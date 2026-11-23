package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.events.PrimordialFuryEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Paquete enviado desde el cliente al servidor para ejecutar combos destructivos
 * del estilo Furia Primordial (Golpe Sísmico, Caída Destructiva, Tajo Ascendente).
 */
public record PrimordialFuryComboPayload(int comboType) implements CustomPacketPayload {

    public static final int COMBO_SEISMIC_SLAM = 1;
    public static final int COMBO_AERIAL_DROP = 2;
    public static final int COMBO_ASCENDING_SLASH = 3;
    public static final int COMBO_CELESTIAL_SLASH = 4;

    public static final Type<PrimordialFuryComboPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "primordial_fury_combo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrimordialFuryComboPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PrimordialFuryComboPayload::comboType,
            PrimordialFuryComboPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PrimordialFuryComboPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PrimordialFuryEvents.executeCombo(player, payload.comboType());
            }
        });
    }
}
