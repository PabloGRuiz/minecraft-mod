package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RenameCompanionPayload(int entityId, String newName) implements CustomPacketPayload {

    public static final Type<RenameCompanionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "rename_companion"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameCompanionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RenameCompanionPayload::entityId,
                    ByteBufCodecs.STRING_UTF8, RenameCompanionPayload::newName,
                    RenameCompanionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenameCompanionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Entity entity = player.serverLevel().getEntity(payload.entityId());
            if (entity instanceof AdventurerNpcEntity adv && adv.isHiredBy(player)) {
                String cleanName = payload.newName().trim();
                if (!cleanName.isEmpty() && cleanName.length() <= 24) {
                    adv.setCustomName(Component.literal(cleanName));
                    adv.setCustomNameVisible(true);

                    player.serverLevel().playSound(null, adv.getX(), adv.getY(), adv.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.sendSystemMessage(Component.literal("§6[Hermandad] §fHas bautizado a tu fiel camarada como: §e" + cleanName + "§f. ¡Ahora es formalmente parte de tu linaje y familia!"));

                    com.pablo.tenseicraft.advancements.ModAdvancements.award(player, com.pablo.tenseicraft.advancements.ModAdvancements.RENAME_COMRADE);

                    PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                            adv.getId(),
                            cleanName,
                            "adventurer",
                            adv.getSkinVariant(),
                            adv.getRank().getIndex(),
                            adv.getAffinity(player),
                            adv.isHired(),
                            true,
                            adv.getHireRemainingTicks(player.level().getGameTime()),
                            adv.isStaying(),
                            0,
                            false,
                            0,
                            adv.isEternalComrade(),
                            adv.getStance().getId(),
                            adv.isReadyToAscend(),
                            "§a\"¡Es un gran honor llevar el nombre de §6" + cleanName + "§a! Lo portaré con orgullo defendiendo nuestro honor familiar.\""
                    ));
                }
            }
        });
    }
}

