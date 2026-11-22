package com.pablo.tenseicraft.client.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.ModKeyMappings;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.network.PerformDashPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class DashClientEvents {

    private static long clientCooldownUntilTick = 0;

    public static long getRemainingCooldownTicks(long currentTick) {
        return Math.max(0L, clientCooldownUntilTick - currentTick);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null
                || com.pablo.tenseicraft.client.cinematics.ReimuIntroCinematicClient.isActive()) {
            return;
        }

        while (ModKeyMappings.DASH_KEY.get().consumeClick()) {
            tryPerformDash(player, player.input.forwardImpulse, player.input.leftImpulse);
        }
    }

    private static void tryPerformDash(LocalPlayer player, float forward, float left) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasSkill("agility_dash")) {
            return;
        }

        long currentTick = player.level().getGameTime();
        long localRem = Math.max(0L, clientCooldownUntilTick - currentTick);
        long dataRem = data.getDashCooldownRemainingTicks(currentTick, player);
        long maxRem = Math.max(localRem, dataRem);
        if (maxRem > 0) {
            float remainingSec = maxRem / 20.0f;
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(String.format("§c[Dash en enfriamiento: %.1fs]", remainingSec)), true);
            return;
        }

        if (data.getCurrentMana(player) < PlayerRpgData.DASH_MANA_COST) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c¡Maná insuficiente para el Dash! §8(Requiere 20 PM)"), true);
            player.playSound(SoundEvents.VILLAGER_NO, 0.8f, 1.2f);
            return;
        }

        int cooldownTicks = data.getDashCooldownTicks(player);
        clientCooldownUntilTick = currentTick + cooldownTicks;
        data.triggerDash(currentTick, 8);

        // Dash Nivel 1: Evasión horizontal 360 grados (no 3D)
        int dexterity = data.getTotalStatPoints(player, StatType.DEXTERITY);
        double baseSpeed = 1.05 + Math.min(0.20, dexterity * 0.008);

        float dashYaw;
        if (forward == 0.0f && left == 0.0f) {
            dashYaw = player.getYRot();
        } else {
            double moveAngleRad = Math.atan2(-left, forward);
            dashYaw = player.getYRot() + (float) Math.toDegrees(moveAngleRad);
        }

        double rad = Math.toRadians(dashYaw);
        float dirX = (float) -Math.sin(rad);
        float dirY = 0.0f;
        float dirZ = (float) Math.cos(rad);

        double vx = dirX * baseSpeed;
        double vy = 0.16; // Ligero impulso vertical para superar bordes de bloques
        double vz = dirZ * baseSpeed;

        player.setDeltaMovement(new Vec3(vx, vy, vz));
        player.playSound(SoundEvents.WIND_CHARGE_BURST.value(), 0.9f, 1.4f);

        for (int i = 0; i < 6; i++) {
            player.level().addParticle(
                    ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    -dirX * 0.25, 0.05, -dirZ * 0.25
            );
        }

        PacketDistributor.sendToServer(new PerformDashPayload(dirX, dirY, dirZ, false));
    }
}
