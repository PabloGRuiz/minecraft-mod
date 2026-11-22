package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.gui.SkillUnlockedToastClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SkillUnlockedPayload(String skillId, String skillName, String skillCategory, String description) implements CustomPacketPayload {

    public static final Type<SkillUnlockedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "skill_unlocked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillUnlockedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SkillUnlockedPayload::skillId,
                    ByteBufCodecs.STRING_UTF8,
                    SkillUnlockedPayload::skillName,
                    ByteBufCodecs.STRING_UTF8,
                    SkillUnlockedPayload::skillCategory,
                    ByteBufCodecs.STRING_UTF8,
                    SkillUnlockedPayload::description,
                    SkillUnlockedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillUnlockedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SkillUnlockedToastClient.triggerSkillToast(
                payload.skillId(),
                payload.skillName(),
                payload.skillCategory(),
                payload.description()
        ));
    }
}
