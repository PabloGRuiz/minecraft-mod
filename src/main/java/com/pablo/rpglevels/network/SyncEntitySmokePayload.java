package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.renderer.HumoEffectManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncEntitySmokePayload(int entityId, boolean active, long durationTicks) implements CustomPacketPayload {

    public static final Type<SyncEntitySmokePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_entity_smoke"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntitySmokePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncEntitySmokePayload::entityId,
            ByteBufCodecs.BOOL, SyncEntitySmokePayload::active,
            ByteBufCodecs.VAR_LONG, SyncEntitySmokePayload::durationTicks,
            SyncEntitySmokePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncEntitySmokePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.durationTicks() > 0) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    HumoEffectManager.setSmokeDuration(payload.entityId(), mc.level.getGameTime() + payload.durationTicks());
                }
            } else {
                HumoEffectManager.setSmokeActive(payload.entityId(), payload.active());
            }
        });
    }
}
