package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.entities.MeguNpcEntity;
import com.pablo.tenseicraft.events.ModAttributesHandler;
import com.pablo.tenseicraft.init.ModBlocks;
import com.pablo.tenseicraft.items.ModItems;
import com.pablo.tenseicraft.reforge.ReforgeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PerformReforgePayload(
        int inventorySlot,
        int lesserCount,
        int resonantCount,
        int radiantCount,
        int primordialCount,
        boolean isMagicAnvil,
        int meguEntityId,
        BlockPos anvilPos
) implements CustomPacketPayload {

    public static final Type<PerformReforgePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "perform_reforge"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PerformReforgePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PerformReforgePayload decode(RegistryFriendlyByteBuf buf) {
                    return new PerformReforgePayload(
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readBoolean(),
                            buf.readVarInt(),
                            buf.readBlockPos()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, PerformReforgePayload val) {
                    buf.writeVarInt(val.inventorySlot());
                    buf.writeVarInt(val.lesserCount());
                    buf.writeVarInt(val.resonantCount());
                    buf.writeVarInt(val.radiantCount());
                    buf.writeVarInt(val.primordialCount());
                    buf.writeBoolean(val.isMagicAnvil());
                    buf.writeVarInt(val.meguEntityId());
                    buf.writeBlockPos(val.anvilPos());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PerformReforgePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // 1. Validar proximidad a Megu o al Yunque Mágico
            if (payload.isMagicAnvil()) {
                BlockPos pos = payload.anvilPos();
                if (!player.level().getBlockState(pos).is(ModBlocks.MAGIC_ANVIL.get())
                        || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) {
                    return;
                }
            } else {
                Entity ent = player.serverLevel().getEntity(payload.meguEntityId());
                if (!(ent instanceof MeguNpcEntity megu) || megu.distanceToSqr(player) > 64.0) {
                    return;
                }
            }

            // 2. Validar slot e ítem compatible (excluye Espada Espiritual)
            int slot = payload.inventorySlot();
            if (slot < 0 || slot >= player.getInventory().items.size()) {
                return;
            }
            ItemStack target = player.getInventory().items.get(slot);
            if (ReforgeManager.isSpiritualSword(target)) {
                player.sendSystemMessage(Component.literal("§c⚠ Tu Espada Espiritual no puede arriesgarse en este proceso."));
                return;
            }
            if (!ReforgeManager.canReforge(target)) {
                return;
            }

            ThreatLevel nextRank = ReforgeManager.getNextTargetRank(target);
            if (nextRank == null) {
                player.sendSystemMessage(Component.literal("§e✦ Este equipamiento ya alcanzó el rango supremo §l[Génesis]§e."));
                return;
            }

            PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (rpgData == null) {
                return;
            }

            // 3. Si es con Megu y el siguiente rango supera Rango B, reconocer límite y desbloquear venta de Yunque Mágico
            if (!payload.isMagicAnvil() && nextRank.ordinal() > ReforgeManager.MEGU_MAX_RANK.ordinal()) {
                rpgData.setMeguAnvilUnlocked(true);
                ModNetworking.syncToPlayer(player);
                player.sendSystemMessage(Component.literal(
                        "§6[Megu]: §e«¡Esta pieza ya es de Rango B! Mis manos mortales no tienen la habilidad suficiente para forjarla a Rango A sin romperla. ¡Adquiere mi Yunque Mágico para continuar mejorándola tú mismo!»"));
                return;
            }

            // 4. Validar y consumir Cristales de Alma del inventario
            int lesser = Math.max(0, payload.lesserCount());
            int resonant = Math.max(0, payload.resonantCount());
            int radiant = Math.max(0, payload.radiantCount());
            int primordial = Math.max(0, payload.primordialCount());
            if (lesser + resonant + radiant + primordial <= 0) {
                return;
            }

            if (countItem(player, ModItems.LESSER_SOUL_FRAGMENT.get()) < lesser
                    || countItem(player, ModItems.RESONANT_SOUL_CRYSTAL.get()) < resonant
                    || countItem(player, ModItems.RADIANT_SOUL_GEM.get()) < radiant
                    || countItem(player, ModItems.PRIMORDIAL_SOUL_CORE.get()) < primordial) {
                player.sendSystemMessage(Component.literal("§c¡No tienes suficientes Cristales de Alma en tu inventario!"));
                return;
            }

            consumeItem(player, ModItems.LESSER_SOUL_FRAGMENT.get(), lesser);
            consumeItem(player, ModItems.RESONANT_SOUL_CRYSTAL.get(), resonant);
            consumeItem(player, ModItems.RADIANT_SOUL_GEM.get(), radiant);
            consumeItem(player, ModItems.PRIMORDIAL_SOUL_CORE.get(), primordial);

            // 5. Calcular probabilidad y tirar ruleta
            int luckPoints = rpgData.getTotalStatPoints(player, StatType.LUCK);
            float chance = ReforgeManager.calculateSuccessChance(nextRank, lesser, resonant, radiant, primordial, luckPoints);
            float roll = player.getRandom().nextFloat() * 100.0f;
            boolean success = roll <= chance;

            float winArcDeg = Math.max(4.0f, Math.min(350.0f, chance * 3.6f));
            float finalAngle;
            String summary;

            if (success) {
                String perksSummary = ReforgeManager.applySuccessfulReforge(target, player.getRandom());
                finalAngle = 2.0f + player.getRandom().nextFloat() * Math.max(1.0f, winArcDeg - 4.0f);
                summary = "§a✔ ¡REFORJA EXITOSA! §f" + perksSummary;

                if (!payload.isMagicAnvil()) {
                    int count = rpgData.getMeguReforgeCount() + 1;
                    rpgData.setMeguReforgeCount(count);
                    if (ReforgeManager.getReforgeRankOrdinal(target) >= ThreatLevel.B.ordinal() || count >= 3) {
                        boolean newlyUnlocked = !rpgData.isMeguAnvilUnlocked();
                        rpgData.setMeguAnvilUnlocked(true);
                        if (newlyUnlocked) {
                            player.sendSystemMessage(Component.literal(
                                    "§6[Megu]: §e«Tu equipo y destreza han puesto a prueba mis límites como herrera... Ya puedes comprarme tu propio §dYunque Mágico§e en mi taller.»"));
                        }
                    }
                }
            } else {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                float failArcSpan = Math.max(2.0f, 356.0f - winArcDeg);
                finalAngle = winArcDeg + 2.0f + player.getRandom().nextFloat() * failArcSpan;
                summary = "§c✘ ¡FALLO! El equipamiento no soportó la presión anímica y se destruyó.";

                if (!payload.isMagicAnvil()) {
                    int count = rpgData.getMeguReforgeCount() + 1;
                    rpgData.setMeguReforgeCount(count);
                    if (count >= 3) {
                        rpgData.setMeguAnvilUnlocked(true);
                    }
                }
            }

            player.getInventory().setChanged();
            ModAttributesHandler.updatePlayerAttributes(player);
            ModNetworking.syncToPlayer(player);

            PacketDistributor.sendToPlayer(player, new ReforgeResultPayload(success, chance, finalAngle, summary));
        });
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}
