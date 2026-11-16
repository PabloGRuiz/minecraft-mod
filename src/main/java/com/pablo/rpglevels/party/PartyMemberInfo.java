package com.pablo.rpglevels.party;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record PartyMemberInfo(
        UUID uuid,
        String name,
        float health,
        float maxHealth,
        float mana,
        float maxMana,
        boolean isNpc,
        String roleOrRank
) {

    public static final StreamCodec<FriendlyByteBuf, PartyMemberInfo> STREAM_CODEC = StreamCodec.of(
            (buf, info) -> {
                buf.writeUUID(info.uuid);
                buf.writeUtf(info.name);
                buf.writeFloat(info.health);
                buf.writeFloat(info.maxHealth);
                buf.writeFloat(info.mana);
                buf.writeFloat(info.maxMana);
                buf.writeBoolean(info.isNpc);
                buf.writeUtf(info.roleOrRank);
            },
            buf -> new PartyMemberInfo(
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readUtf()
            )
    );

    public float getHealthPercentage() {
        return maxHealth > 0.0f ? (health / maxHealth) : 0.0f;
    }

    public float getManaPercentage() {
        return maxMana > 0.0f ? (mana / maxMana) : 0.0f;
    }
}

