package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.renderer.KiAuraRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncOppressiveAuraPayload(int entityId, long durationTicks, float maxRadius, float auraPower, int colorRgb) implements CustomPacketPayload {

    public static final Type<SyncOppressiveAuraPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_oppressive_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOppressiveAuraPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncOppressiveAuraPayload::entityId,
            ByteBufCodecs.VAR_LONG, SyncOppressiveAuraPayload::durationTicks,
            ByteBufCodecs.FLOAT, SyncOppressiveAuraPayload::maxRadius,
            ByteBufCodecs.FLOAT, SyncOppressiveAuraPayload::auraPower,
            ByteBufCodecs.INT, SyncOppressiveAuraPayload::colorRgb,
            SyncOppressiveAuraPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncOppressiveAuraPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) {
                long currentTick = mc.level.getGameTime();
                long expireTick = currentTick + payload.durationTicks();
                float r = ((payload.colorRgb() >> 16) & 0xFF) / 255.0f;
                float g = ((payload.colorRgb() >> 8) & 0xFF) / 255.0f;
                float b = (payload.colorRgb() & 0xFF) / 255.0f;
                KiAuraRenderer.setOppressiveAura(payload.entityId(), currentTick, expireTick, payload.maxRadius(), payload.auraPower(), r, g, b);
            }
        });
    }
}
