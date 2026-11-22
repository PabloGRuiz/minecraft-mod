package com.pablo.tenseicraft.data;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TenseiCraftMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerRpgData>> PLAYER_RPG_DATA =
            ATTACHMENT_TYPES.register("player_rpg_data", () -> AttachmentType.serializable(PlayerRpgData::new)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<Integer>> MOB_RANK =
            ATTACHMENT_TYPES.register("mob_rank", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    public static final Supplier<AttachmentType<com.pablo.tenseicraft.quests.PlayerQuestData>> PLAYER_QUEST_DATA =
            ATTACHMENT_TYPES.register("player_quest_data", () -> AttachmentType.serializable(com.pablo.tenseicraft.quests.PlayerQuestData::new)
                    .copyOnDeath()
                    .build());
}
