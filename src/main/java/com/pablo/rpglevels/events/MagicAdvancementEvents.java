package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class MagicAdvancementEvents {

    public static final String SKILL_ID = "magic_affinity";
    private static final ResourceLocation ENCHANT_ADVANCEMENT = ResourceLocation.withDefaultNamespace("story/enchant_item");

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AdvancementHolder advancement = event.getAdvancement();
        if (advancement != null && advancement.id().equals(ENCHANT_ADVANCEMENT)) {
            unlockMagicAffinity(player);
        }
    }

    public static void checkRetroactiveAdvancement(ServerPlayer player) {
        if (player == null || player.server == null) {
            return;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null && !data.hasSkill(SKILL_ID)) {
            AdvancementHolder adv = player.server.getAdvancements().get(ENCHANT_ADVANCEMENT);
            if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
                unlockMagicAffinity(player);
            }
        }
    }

    private static void unlockMagicAffinity(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || data.hasSkill(SKILL_ID)) {
            return;
        }

        data.unlockSkill(SKILL_ID);
        ModNetworking.syncToPlayer(player);

        // Tensura Voice of the World Screen Toast + Fanfare
        ModNetworking.sendSkillUnlocked(
                player,
                SKILL_ID,
                "Afinidad Mágica",
                "Habilidad Extra",
                "Desbloquea los Cánticos Arcanos en Lengua Antigua y canalización de maná"
        );

        // Tensura Chat Announcement
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§6« ANUNCIO DE LA VOZ DEL MUNDO »").withStyle(ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("§e«Condición cumplida: Primer encantamiento realizado. Se ha otorgado la Habilidad Extra: §d[Afinidad Mágica]§e.»"));
        player.sendSystemMessage(Component.literal("§7«Efecto: Permite entonar cánticos en Lengua Antigua combinando palabras de poder con la mente y el habla (Alt Izq + 1, 2, 3).»"));
        player.sendSystemMessage(Component.literal("§b«Cánticos iniciales descubiertos:»"));
        player.sendSystemMessage(Component.literal("§6 • [Refuerzo Físico]: §fAlt Izq + §e1 -> 2 -> 3 §7(«Corpus Vigor Eruptio!») §8- §7Buffs de fuerza y resistencia."));
        player.sendSystemMessage(Component.literal("§3 • [Potenciar Agilidad]: §fAlt Izq + §b1 -> 3 -> 1 §7(«Corpus Zephyrus Celeritas!») §8- §7Velocidad, salto y nado ágil."));
        player.sendSystemMessage(Component.literal("§c • [Bola de Fuego]: §fAlt Izq + §62 -> 1 -> 3 §7(«Ignis Aether Eruptio!») §8- §7Proyectil ígneo destructivo."));
        player.sendSystemMessage(Component.literal("§d • [Canalizar Magia]: §fAlt Izq + §b3 -> 1 -> 2 §7(«Anima Aether Fluctuo!») §8- §7Surge de espíritu y despertar de armas."));
        player.sendSystemMessage(Component.empty());
    }
}
