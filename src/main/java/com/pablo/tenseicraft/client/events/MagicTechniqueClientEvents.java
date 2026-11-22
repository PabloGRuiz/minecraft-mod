package com.pablo.tenseicraft.client.events;

import com.mojang.blaze3d.platform.InputConstants;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.ModKeyMappings;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.magic.SpiritualTechnique;
import com.pablo.tenseicraft.network.ModNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class MagicTechniqueClientEvents {

    private static final List<Integer> currentSeals = new ArrayList<>();
    private static long lastSealTime = 0;
    private static final long SEAL_TIMEOUT_MS = 3000;

    private static boolean wasAltDown = false;
    private static int lockedHotbarSlot = -1;

    // Buffer for prefix between 3-2-1 (Low Purification) and 3-2-1-2 (Fast Healing)
    private static long pendingPurificationUntil = 0;
    // Buffer for prefix between 2-1-3 (Low Paralize) and 2-1-3-2 (Connect Realities)
    private static long pendingParalizeUntil = 0;

    private static boolean isMagicModifierDown(Minecraft mc) {
        KeyMapping key = ModKeyMappings.MAGIC_MODIFIER_KEY.get();
        if (key.isUnbound()) return false;
        if (key.getKey().getType() == InputConstants.Type.KEYSYM && mc != null && mc.getWindow() != null) {
            return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getKey().getValue());
        }
        return key.isDown();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || com.pablo.tenseicraft.client.cinematics.ReimuIntroCinematicClient.isActive()) {
            currentSeals.clear();
            wasAltDown = false;
            lockedHotbarSlot = -1;
            pendingPurificationUntil = 0;
            pendingParalizeUntil = 0;
            return;
        }

        boolean isAltDown = isMagicModifierDown(mc);

        // Alt key press transition
        if (isAltDown && !wasAltDown) {
            wasAltDown = true;
            lockedHotbarSlot = mc.player.getInventory().selected;
        } else if (!isAltDown && wasAltDown) {
            wasAltDown = false;
            lockedHotbarSlot = -1;
            if (pendingPurificationUntil > 0) {
                // If Alt released while waiting on 3-2-1, cast Low Purification!
                pendingPurificationUntil = 0;
                castTechnique(mc, SpiritualTechnique.LOW_PURIFICATION);
            } else if (pendingParalizeUntil > 0) {
                // If Alt released while waiting on 2-1-3, cast Low Paralize!
                pendingParalizeUntil = 0;
                castTechnique(mc, SpiritualTechnique.LOW_PARALIZE);
            } else if (!currentSeals.isEmpty()) {
                currentSeals.clear();
            }
        }

        // Check pending timers
        if (pendingPurificationUntil > 0 && System.currentTimeMillis() >= pendingPurificationUntil) {
            pendingPurificationUntil = 0;
            castTechnique(mc, SpiritualTechnique.LOW_PURIFICATION);
        }
        if (pendingParalizeUntil > 0 && System.currentTimeMillis() >= pendingParalizeUntil) {
            pendingParalizeUntil = 0;
            castTechnique(mc, SpiritualTechnique.LOW_PARALIZE);
        }

        // Lock hotbar slot while holding Alt so casting doesn't switch items
        if (isAltDown && lockedHotbarSlot != -1) {
            if (mc.player.getInventory().selected != lockedHotbarSlot) {
                mc.player.getInventory().selected = lockedHotbarSlot;
            }
        }

        // Timeout check for incantation sequence
        if (!currentSeals.isEmpty() && pendingPurificationUntil == 0 && (System.currentTimeMillis() - lastSealTime > SEAL_TIMEOUT_MS)) {
            currentSeals.clear();
            if (mc.player != null && mc.screen == null) {
                mc.player.displayClientMessage(Component.literal("§c✦ Cántico disipado por falta de concentración."), true);
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null
                || com.pablo.tenseicraft.client.cinematics.ReimuIntroCinematicClient.isActive()) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        boolean isAltDown = isMagicModifierDown(mc);
        if (!isAltDown) {
            return;
        }

        int key = event.getKey();
        int scanCode = event.getScanCode();

        int sealPressed = -1;
        if (key == GLFW.GLFW_KEY_1 || mc.options.keyHotbarSlots[0].matches(key, scanCode)) {
            sealPressed = 1;
        } else if (key == GLFW.GLFW_KEY_2 || mc.options.keyHotbarSlots[1].matches(key, scanCode)) {
            sealPressed = 2;
        } else if (key == GLFW.GLFW_KEY_3 || mc.options.keyHotbarSlots[2].matches(key, scanCode)) {
            sealPressed = 3;
        }

        if (sealPressed == -1) {
            return;
        }

        if (lockedHotbarSlot != -1) {
            mc.player.getInventory().selected = lockedHotbarSlot;
        }

        PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasSkill("magic_affinity")) {
            mc.player.displayClientMessage(Component.literal("§c¡Requieres despertar tu vínculo con Alice para entonar Técnicas Espirituales!"), true);
            mc.player.playSound(SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.5f);
            return;
        }

        handleSealInput(mc, sealPressed, data);
    }

    private static void handleSealInput(Minecraft mc, int seal, PlayerRpgData data) {
        lastSealTime = System.currentTimeMillis();

        // If waiting on prefix 3-2-1
        if (pendingPurificationUntil > 0) {
            pendingPurificationUntil = 0;
            if (seal == 2) {
                // Completed: 3 -> 2 -> 1 -> 2 ! Fast Healing
                castTechnique(mc, SpiritualTechnique.FAST_HEALING);
                return;
            } else {
                // Pressed something else: cast Low Purification and start new sequence or fail
                castTechnique(mc, SpiritualTechnique.LOW_PURIFICATION);
                return;
            }
        }

        if (currentSeals.isEmpty()) {
            if (seal == 1) {
                currentSeals.add(1);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                mc.player.displayClientMessage(Component.literal("§6✧ Sello [1]: §f«Corpus...» §7[Físico / Curación]"), true);
            } else if (seal == 2) {
                currentSeals.add(2);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                mc.player.displayClientMessage(Component.literal("§e✧ Sello [2]: §f«Fulgur...» §7[Parálisis / Afinidad]"), true);
            } else if (seal == 3) {
                currentSeals.add(3);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                mc.player.displayClientMessage(Component.literal("§d✧ Sello [3]: §f«Anima...» §7[Elemental / Purificación]"), true);
            } else {
                failSeals(mc);
            }
        } else if (currentSeals.size() == 1) {
            int first = currentSeals.get(0);
            if (first == 1 && seal == 2) {
                // 1 -> 2 (leads to 1-2-3 Physical Boost or 1-2-1-3 Oppressive Aura)
                currentSeals.add(2);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                mc.player.displayClientMessage(Component.literal("§6✧ Cántico: §b«Corpus» §7-> §6«Vigor...» §7(Pulsa 3: Boost | 1: Opresiva)"), true);
            } else if (first == 1 && seal == 3) {
                // 1 -> 3 (leads to 1-3-2 Low Healing or 1-3-1 Summon Lesser Soul)
                currentSeals.add(3);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                mc.player.displayClientMessage(Component.literal("§a✧ Cántico: §b«Corpus» §7-> §a«Vitae...» §7(Pulsa 2: Curar | 1: Invocar)"), true);
            } else if (first == 2 && seal == 1) {
                // 2 -> 1 (leads to 2-1-3 Low Paralize)
                currentSeals.add(1);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                mc.player.displayClientMessage(Component.literal("§e✧ Cántico: §e«Fulgur» §7-> §b«Vinculum...» §7(Pulsa 3)"), true);
            } else if (first == 2 && seal == 3) {
                // 2 -> 3 (leads to 2-3-1 Afinidad Mágica)
                currentSeals.add(3);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                mc.player.displayClientMessage(Component.literal("§d✧ Cántico: §d«Aether» §7-> §b«Fluctuo...» §7(Pulsa 1)"), true);
            } else if (first == 3 && seal == 1) {
                // 3 -> 1 (leads to 3-1-2 Elemental Ball or 3-1-3-1 Ha-ku-rei)
                currentSeals.add(1);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                String nextHint = data.hasSkill(SpiritualTechnique.HAKUREI.getId())
                        ? "§7(Pulsa 2: Esfera | 3: Ha-ku-rei)"
                        : "§7(Pulsa 2)";
                mc.player.displayClientMessage(Component.literal("§c✧ Cántico: §d«Anima» §7-> §c«Elementum...» " + nextHint), true);
            } else if (first == 3 && seal == 2) {
                // 3 -> 2 (leads to 3-2-1 Low Purification or 3-2-1-2 Fast Healing)
                currentSeals.add(2);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                mc.player.displayClientMessage(Component.literal("§b✧ Cántico: §d«Anima» §7-> §b«Purgatio...» §7(Pulsa 1)"), true);
            } else {
                failSeals(mc);
            }
        } else if (currentSeals.size() == 2) {
            int first = currentSeals.get(0);
            int second = currentSeals.get(1);

            if (first == 1 && second == 2 && seal == 3) {
                // 1 -> 2 -> 3 ! (Physical Boost)
                castTechnique(mc, SpiritualTechnique.PHYSICAL_BOOST);
            } else if (first == 1 && second == 2 && seal == 1) {
                // 1 -> 2 -> 1 ! (leads to 1-2-1-3 Oppressive Aura)
                currentSeals.add(1);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.6f);
                mc.player.displayClientMessage(Component.literal("§5✧ Cántico [1-2-1]: §d«Anima» §7-> §5«Gravitas...» §7(Pulsa 3 para Aura Opresiva)"), true);
            } else if (first == 1 && second == 3 && seal == 2) {
                // 1 -> 3 -> 2 ! (Low Healing)
                castTechnique(mc, SpiritualTechnique.LOW_HEALING);
            } else if (first == 1 && second == 3 && seal == 1) {
                // 1 -> 3 -> 1 ! (Summon Lesser Soul)
                castTechnique(mc, SpiritualTechnique.SUMMON_LESSER_SOUL);
            } else if (first == 2 && second == 1 && seal == 3) {
                // 2 -> 1 -> 3 ! (Low Paralize)
                // If player has Connect Realities unlocked, wait 500ms for possible 4th seal (2)
                if (data.hasSkill(SpiritualTechnique.CONNECT_REALITIES.getId())) {
                    currentSeals.add(3); // Now [2, 1, 3]
                    pendingParalizeUntil = System.currentTimeMillis() + 500;
                    mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f, 1.7f);
                    mc.player.displayClientMessage(
                            Component.literal("§5✧ Cántico [2-1-3]: §d«Fulgur Vinculum» §e(¡Pulsa 2 para Conectar Realidades o espera!)"),
                            true
                    );
                } else {
                    castTechnique(mc, SpiritualTechnique.LOW_PARALIZE);
                }
            } else if (first == 2 && second == 3 && seal == 1) {
                // 2 -> 3 -> 1 ! (Afinidad Mágica)
                castTechnique(mc, SpiritualTechnique.MAGIC_AFFINITY);
            } else if (first == 3 && second == 1 && seal == 2) {
                // 3 -> 1 -> 2 ! (Elemental Ball)
                castTechnique(mc, SpiritualTechnique.ELEMENTAL_BALL);
            } else if (first == 3 && second == 1 && seal == 3) {
                // 3 -> 1 -> 3 (leads to 3-1-3-1 Ha-ku-rei)
                if (data.hasSkill(SpiritualTechnique.HAKUREI.getId())) {
                    currentSeals.add(3);
                    mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f, 1.7f);
                    mc.player.displayClientMessage(
                            Component.literal("§c✧ Cántico [3-1-3]: §f«Hakurei...» §e(¡Pulsa 1 para Ha-ku-rei!)"),
                            true
                    );
                } else {
                    failSeals(mc);
                }
            } else if (first == 3 && second == 2 && seal == 1) {
                // 3 -> 2 -> 1 !
                // If player has Fast Healing unlocked, wait 500ms for possible 4th seal (2)
                if (data.hasSkill(SpiritualTechnique.FAST_HEALING.getId())) {
                    currentSeals.add(1); // Now [3, 2, 1]
                    pendingPurificationUntil = System.currentTimeMillis() + 500;
                    mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f, 1.7f);
                    mc.player.displayClientMessage(
                            Component.literal("§a✧ Cántico [3-2-1]: §f«Anima Purgatio» §e(¡Pulsa 2 para Fast Healing o espera!)"),
                            true
                    );
                } else {
                    castTechnique(mc, SpiritualTechnique.LOW_PURIFICATION);
                }
            } else {
                failSeals(mc);
            }
        } else if (currentSeals.size() == 3) {
            // [3, 2, 1] + 2 -> Fast Healing
            if (currentSeals.get(0) == 3 && currentSeals.get(1) == 2 && currentSeals.get(2) == 1 && seal == 2) {
                pendingPurificationUntil = 0;
                castTechnique(mc, SpiritualTechnique.FAST_HEALING);
            }
            // [2, 1, 3] + 2 -> Connect Realities
            else if (currentSeals.get(0) == 2 && currentSeals.get(1) == 1 && currentSeals.get(2) == 3 && seal == 2) {
                pendingParalizeUntil = 0;
                castTechnique(mc, SpiritualTechnique.CONNECT_REALITIES);
            }
            // [1, 2, 1] + 3 -> Oppressive Aura (Técnica Espiritual Nº 25)
            else if (currentSeals.get(0) == 1 && currentSeals.get(1) == 2 && currentSeals.get(2) == 1 && seal == 3) {
                castTechnique(mc, SpiritualTechnique.OPPRESSIVE_AURA);
            }
            // [3, 1, 3] + 1 -> Ha-ku-rei (Técnica Espiritual Nro. 890)
            else if (currentSeals.get(0) == 3 && currentSeals.get(1) == 1 && currentSeals.get(2) == 3 && seal == 1) {
                castTechnique(mc, SpiritualTechnique.HAKUREI);
            } else {
                failSeals(mc);
            }
        }
    }

    private static void castTechnique(Minecraft mc, SpiritualTechnique tech) {
        currentSeals.clear();
        pendingPurificationUntil = 0;
        pendingParalizeUntil = 0;

        if (mc.player == null) return;
        PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasSkill(tech.getId())) {
            mc.player.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7f, 1.4f);
            mc.player.displayClientMessage(
                    Component.literal("§c✦ Aún no has aprendido " + tech.getDisplayName() + ". Alice te la enseñará por 5 esmeraldas."),
                    true
            );
            return;
        }

        mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.9f);
        String name = tech == SpiritualTechnique.ELEMENTAL_BALL
                ? SpiritualTechnique.getDynamicElementalBallName(mc.player)
                : tech.getDisplayName();

        mc.player.displayClientMessage(
                Component.literal("§e§l✦ ¡" + name.toUpperCase() + "! (" + (int)tech.getManaCost() + " MP) ✦"),
                true
        );
        ModNetworking.sendCastSpell(tech.getId());
    }

    private static void failSeals(Minecraft mc) {
        currentSeals.clear();
        pendingPurificationUntil = 0;
        mc.player.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7f, 1.6f);
        mc.player.displayClientMessage(Component.literal("§c✦ Cántico interrumpido. Las palabras de poder pierden armonía."), true);
    }
}
