package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.events.ModAttributesHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UnlockSkillPayload(String skillId) implements CustomPacketPayload {

    public static final Type<UnlockSkillPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "unlock_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockSkillPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    UnlockSkillPayload::skillId,
                    UnlockSkillPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UnlockSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (player.isSpectator() || player.isDeadOrDying()) {
                return;
            }

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) {
                return;
            }

            boolean unlocked = data.unlockPhysicalSkill(payload.skillId());
            if (unlocked) {
                ModAttributesHandler.updatePlayerAttributes(player);
                ModNetworking.syncToPlayer(player);

                player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS,
                        0.9f,
                        1.2f
                );

                Component skillName = Component.translatable("tenseicraft.skill." + payload.skillId());
                player.displayClientMessage(
                        Component.translatable("tenseicraft.skill.unlocked_chat", skillName),
                        false
                );
            }
        });
    }
}
