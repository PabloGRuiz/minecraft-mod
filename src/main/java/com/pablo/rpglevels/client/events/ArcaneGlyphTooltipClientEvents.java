package com.pablo.rpglevels.client.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.events.EnchantedItemRestrictionEvents;
import com.pablo.rpglevels.events.MagicAdvancementEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class ArcaneGlyphTooltipClientEvents {

    private static final ResourceLocation SGA_FONT = ResourceLocation.withDefaultNamespace("alt");
    private static final TextColor RUNE_TITLE_COLOR = TextColor.fromRgb(0xC455F5);
    private static final TextColor RUNE_BODY_COLOR = TextColor.fromRgb(0x8B35BA);

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!EnchantedItemRestrictionEvents.isMagicallySealed(stack)) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null && data.hasSkill(MagicAdvancementEvents.SKILL_ID)) {
            // Player has Magic Affinity unlocked -> everything is deciphered and readable
            return;
        }

        List<Component> tooltips = event.getToolTip();
        if (tooltips.isEmpty()) {
            return;
        }

        // 1. Transform the Item Title (Index 0) into Standard Galactic Alphabet glyphs
        Component originalTitle = tooltips.get(0);
        String titleText = originalTitle.getString();
        Component arcaneTitle = Component.literal(titleText)
                .withStyle(Style.EMPTY
                        .withFont(SGA_FONT)
                        .withColor(RUNE_TITLE_COLOR)
                );
        tooltips.set(0, arcaneTitle);

        // 2. Transform the remaining tooltip lines (enchantments, descriptions) into arcane glyphs
        for (int i = 1; i < tooltips.size(); i++) {
            Component line = tooltips.get(i);
            String lineText = line.getString();
            if (!lineText.trim().isEmpty()) {
                tooltips.set(i, Component.literal(lineText)
                        .withStyle(Style.EMPTY
                                .withFont(SGA_FONT)
                                .withColor(RUNE_BODY_COLOR)
                        ));
            }
        }

        // 3. Add decipher requirement warning in readable text
        tooltips.add(1, Component.literal("§8[ §5ᔑꖎℸ ̣ §7Runas Arcanas Incomprensibles §5!¡⚍∷ §8]"));
        tooltips.add(2, Component.literal("§c§oRequiere [Afinidad Mágica] para descifrar y canalizar este artefacto."));
    }
}
