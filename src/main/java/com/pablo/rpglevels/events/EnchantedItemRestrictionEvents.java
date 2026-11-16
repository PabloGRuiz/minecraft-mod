package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class EnchantedItemRestrictionEvents {

    public static boolean isMagicallySealed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // Check vanilla enchantments component
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) {
            return true;
        }

        // Check stored enchantments (enchanted books)
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            return true;
        }

        // Check Iron's Spells scrolls, spellbooks or spell items
        String creatorMod = stack.getItem().getCreatorModId(stack);
        if ("irons_spellbooks".equals(creatorMod)) {
            String path = stack.getItem().toString().toLowerCase();
            if (path.contains("scroll") || path.contains("spell_book") || path.contains("staff") || path.contains("wand")) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasMagicAffinity(Player player) {
        if (player == null) return true;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        return data != null && data.hasSkill(MagicAdvancementEvents.SKILL_ID);
    }

    // 1. Bloqueo de combate con armas/herramientas encantadas
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null || player.isCreative()) return;

        ItemStack weapon = player.getMainHandItem();
        if (isMagicallySealed(weapon) && !hasMagicAffinity(player)) {
            event.setCanceled(true);

            player.displayClientMessage(
                    Component.literal("§5§k??? §c¡El encantamiento rechaza tu mano sin [Afinidad Mágica]! §5§k???")
                            .withStyle(ChatFormatting.RED),
                    true
            );

            player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS,
                    0.9f,
                    0.6f
            );
        }
    }

    // 2. Bloqueo de minado y velocidad de excavación con herramientas encantadas
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player == null || player.isCreative()) return;

        ItemStack tool = player.getMainHandItem();
        if (isMagicallySealed(tool) && !hasMagicAffinity(player)) {
            event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.isCreative()) return;

        ItemStack tool = player.getMainHandItem();
        if (isMagicallySealed(tool) && !hasMagicAffinity(player)) {
            event.setCanceled(true);

            player.displayClientMessage(
                    Component.literal("§5§k??? §cNo puedes canalizar una herramienta encantada sin [Afinidad Mágica]. §5§k???")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    // 3. Bloqueo de uso por clic derecho (arcos, tridentes, libros, pergaminos arcanos)
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player == null || player.isCreative()) return;

        ItemStack stack = event.getItemStack();
        if (isMagicallySealed(stack) && !hasMagicAffinity(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            player.displayClientMessage(
                    Component.literal("§5§k??? §cLas runas mágicas están selladas para ti sin [Afinidad Mágica]. §5§k???")
                            .withStyle(ChatFormatting.RED),
                    true
            );

            player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS,
                    0.8f,
                    0.6f
            );
        }
    }

    // 4. Rechazo de armaduras encantadas
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }

        if (event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            ItemStack toStack = event.getTo();
            if (isMagicallySealed(toStack) && !hasMagicAffinity(player)) {
                // Eject armor from slot
                player.setItemSlot(event.getSlot(), ItemStack.EMPTY);

                // Give back to player inventory or drop
                if (!player.getInventory().add(toStack.copy())) {
                    player.drop(toStack.copy(), false);
                }

                player.displayClientMessage(
                        Component.literal("§5§k??? §c¡Tu cuerpo no puede soportar el flujo arcano de esta armadura sin [Afinidad Mágica]! §5§k???")
                                .withStyle(ChatFormatting.RED),
                        true
                );

                player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_GENERIC,
                        SoundSource.PLAYERS,
                        1.0f,
                        0.5f
                );
            }
        }
    }
}
