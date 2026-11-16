package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.renderer.KiAuraRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncEntityAuraPayload(int entityId, long durationTicks, float auraPower, int colorRgb) implements CustomPacketPayload {

    public static final Type<SyncEntityAuraPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_entity_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntityAuraPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncEntityAuraPayload::entityId,
            ByteBufCodecs.VAR_LONG, SyncEntityAuraPayload::durationTicks,
            ByteBufCodecs.FLOAT, SyncEntityAuraPayload::auraPower,
            ByteBufCodecs.INT, SyncEntityAuraPayload::colorRgb,
            SyncEntityAuraPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncEntityAuraPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) {
                long expireTick = mc.level.getGameTime() + payload.durationTicks();
                float r = ((payload.colorRgb() >> 16) & 0xFF) / 255.0f;
                float g = ((payload.colorRgb() >> 8) & 0xFF) / 255.0f;
                float b = (payload.colorRgb() & 0xFF) / 255.0f;
                KiAuraRenderer.setEntityAura(payload.entityId(), expireTick, payload.auraPower(), r, g, b);
            }
        });
    }
}
