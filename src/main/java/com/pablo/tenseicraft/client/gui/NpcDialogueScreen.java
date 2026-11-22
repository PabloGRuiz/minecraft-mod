package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.network.NpcDialogueActionPayload;
import com.pablo.tenseicraft.network.OpenNpcDialoguePayload;
import com.pablo.tenseicraft.social.NpcSocialManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class NpcDialogueScreen extends Screen {

    private final int GUI_WIDTH = 340;
    private final int GUI_HEIGHT = 220;
    private int leftPos;
    private int topPos;

    private int entityId;
    private String npcName;
    private String npcType;
    private int skinVariant;
    private int rankIndex;
    private int affinity;
    private boolean isHired;
    private boolean isHiredByPlayer;
    private long hireRemainingTicks;
    private boolean isStaying;
    private int workerStatus;
    private boolean isDevoted;
    private int vassalRank;
    private boolean isEternalComrade;
    private int stance;
    private boolean companionReadyToAscend;
    private String dialogueText;
    private int dialogueScrollOffset = 0;
    private static final int MAX_VISIBLE_LINES = 6;

    private LivingEntity cachedEntity;

    public NpcDialogueScreen(OpenNpcDialoguePayload payload) {
        super(Component.literal(payload.npcName()));
        updateState(payload);
    }

    public String getNpcType() {
        return this.npcType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void updateState(OpenNpcDialoguePayload payload) {
        this.entityId = payload.entityId();
        this.npcName = payload.npcName();
        this.npcType = payload.npcType();
        this.skinVariant = payload.skinVariant();
        this.rankIndex = payload.rankIndex();
        this.affinity = payload.affinity();
        this.isHired = payload.isHired();
        this.isHiredByPlayer = payload.isHiredByPlayer();
        this.hireRemainingTicks = payload.hireRemainingTicks();
        this.isStaying = payload.isStaying();
        this.workerStatus = payload.workerStatus();
        this.isDevoted = payload.isDevoted();
        this.vassalRank = payload.vassalRank();
        this.isEternalComrade = payload.isEternalComrade();
        this.stance = payload.stance();
        this.companionReadyToAscend = payload.companionReadyToAscend();
        this.dialogueText = payload.dialogueText();
        this.dialogueScrollOffset = 0;

        if (this.minecraft != null) {
            this.rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        if (this.minecraft != null && this.minecraft.level != null) {
            Entity ent = this.minecraft.level.getEntity(this.entityId);
            if (ent instanceof LivingEntity living) {
                this.cachedEntity = living;
            }
        }

        // Action Buttons
        int btnWidth = 100;
        int btnHeight = 20;
        int rightColumnX = leftPos + 120;
        int actionY = topPos + 126;

        // 1. Chat button
        boolean hasAliceTrialEvent = false;
        if ("alice".equals(npcType)) {
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null) {
                ItemStack sword = findSpiritualSword(localPlayer);
                if (sword != null && com.pablo.tenseicraft.items.SpiritualSwordItem.getAffinity(sword) >= 100
                        && !com.pablo.tenseicraft.items.SpiritualSwordItem.isFirstReleaseUnlocked(sword)) {
                    hasAliceTrialEvent = true;
                }
            }
        }

        boolean hasComradeBondEvent = "adventurer".equals(npcType) && isHiredByPlayer && !isEternalComrade && affinity >= 100;
        boolean hasExclamation = hasAliceTrialEvent || hasComradeBondEvent;

        Component chatLabel = hasExclamation
                ? Component.literal("§6💬 Charlar (!)")
                : Component.translatable("tenseicraft.social.chat");

        Tooltip chatTooltip;
        if (hasComradeBondEvent) {
            chatTooltip = Tooltip.create(Component.literal("§6[¡Vínculo Eterno Desbloqueado!]\n§eTu compañero desea hablar contigo sobre su futuro juntos.\n§7Sellará una hermandad eterna y pasará a ser parte de tu familia."));
        } else if (hasAliceTrialEvent) {
            chatTooltip = Tooltip.create(Component.literal("§6[¡Prueba Espiritual Disponible!]\n§eTu espada alcanzó 100 de afinidad.\n§7Alice te revelará el camino para despertar su Primera Liberación."));
        } else {
            chatTooltip = Tooltip.create(Component.translatable("tenseicraft.social.chat_tooltip", npcName));
        }

        this.addRenderableWidget(Button.builder(chatLabel, b -> {
            ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_CHAT);
        }).bounds(rightColumnX, actionY, btnWidth, btnHeight)
        .tooltip(chatTooltip)
        .build());

        // 2. Gift button
        this.addRenderableWidget(Button.builder(Component.translatable("tenseicraft.social.gift"), b -> {
            ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_GIFT);
        }).bounds(rightColumnX + 106, actionY, btnWidth, btnHeight)
        .tooltip(Tooltip.create(Component.translatable("tenseicraft.social.gift_tooltip")))
        .build());

        // 3. Adventurer specific: Hire / Stay / Stances / Rename buttons
        if ("adventurer".equals(npcType)) {
            int secondRowY = actionY + 24;

            if (isEternalComrade) {
                // 3 Stance Buttons: Follow / Base / Guard
                Component followLabel = (stance == 0) ? Component.literal("§a§l✔ Seguir") : Component.literal("§7🚶 Seguir");
                Component baseLabel = (stance == 1) ? Component.literal("§b§l✔ En Casa") : Component.literal("§7🏡 En Casa");
                Component guardLabel = (stance == 2) ? Component.literal("§e§l✔ Guardia") : Component.literal("§7🛡 Guardia");

                this.addRenderableWidget(Button.builder(followLabel, b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_STANCE_FOLLOW);
                }).bounds(rightColumnX, secondRowY, 68, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§a[Postura: Seguir]\n§7El aventurero te acompañará y combatirá a tu lado.")))
                .build());

                this.addRenderableWidget(Button.builder(baseLabel, b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_STANCE_BASE);
                }).bounds(rightColumnX + 71, secondRowY, 70, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§b[Postura: Esperar en Casa]\n§7El aventurero deambulará pacíficamente por tu base dentro de 16 bloques.")))
                .build());

                this.addRenderableWidget(Button.builder(guardLabel, b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_STANCE_GUARD);
                }).bounds(rightColumnX + 144, secondRowY, 65, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§e[Postura: Quedarse Quieto]\n§7El aventurero mantendrá su posición fija montando guardia.")))
                .build());

                // Row 3: Card button + Rename button + Backpack
                int thirdRowY = actionY + 48;
                this.addRenderableWidget(Button.builder(Component.literal("§e🪪 Tarjeta"), b -> {
                    if (this.minecraft != null && this.minecraft.level != null) {
                        Entity entity = this.minecraft.level.getEntity(entityId);
                        if (entity instanceof LivingEntity living) {
                            this.minecraft.setScreen(new AdventurerCardScreen(living, this));
                        }
                    }
                }).bounds(rightColumnX, thirdRowY, 68, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§e[Tarjeta Oficial de Aventurero]\n§7Inspecciona el nivel, estadísticas y rango de este aventurero.")))
                .build());

                this.addRenderableWidget(Button.builder(Component.literal("§6✏ Nombrar"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new CompanionNamingScreen(entityId, npcName, this));
                    }
                }).bounds(rightColumnX + 71, thirdRowY, 68, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§6[Asignar Nombre Propio]\n§7Dale un nombre de familia a tu compañero para acogerlo en tu linaje.")))
                .build());

                this.addRenderableWidget(Button.builder(Component.literal("§6🎒 Mochila"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_OPEN_COMPANION_INVENTORY);
                }).bounds(rightColumnX + 142, thirdRowY, 68, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§6[Mochila de Camarada]\n§7Abre el inventario de 18 casillas del aventurero.\n§e- Guarda comida y manzanas doradas para que se cure solo.\n§e- Revisa los minerales y botín recolectado del suelo.")))
                .build());

            } else if (!isHired) {
                AdventurerRank rank = AdventurerRank.fromIndex(rankIndex);
                int hireCost = rank.getHireCost();
                this.addRenderableWidget(Button.builder(Component.literal("§6⚔ Contratar (" + hireCost + " 🟢)"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_HIRE);
                }).bounds(rightColumnX, secondRowY, 130, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§6[Contratar Escolta de Aventurero]\n§eCosto: §a" + hireCost + " Esmeralda" + (hireCost > 1 ? "s" : "") + " §epor 1 día Minecraft.\n§7Acompaña al jugador y combate fieramente a su lado.")))
                .build());

                int thirdRowY = actionY + 48;
                this.addRenderableWidget(Button.builder(Component.literal("§e🪪 Ver Tarjeta"), b -> {
                    if (this.minecraft != null && this.minecraft.level != null) {
                        Entity entity = this.minecraft.level.getEntity(entityId);
                        if (entity instanceof LivingEntity living) {
                            this.minecraft.setScreen(new AdventurerCardScreen(living, this));
                        }
                    }
                }).bounds(rightColumnX, thirdRowY, 110, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§e[Tarjeta Oficial de Aventurero]\n§7Inspecciona la credencial oficial, rango, miniatura 3D y estadísticas de combate de este aventurero.")))
                .build());

            } else if (isHiredByPlayer) {
                long totalSeconds = hireRemainingTicks / 20L;
                long minutes = totalSeconds / 60L;
                long seconds = totalSeconds % 60L;
                String timeFormatted = String.format("%02d:%02d", minutes, seconds);

                // Toggle Stay / Follow
                Component stayLabel = isStaying
                        ? Component.translatable("tenseicraft.social.follow")
                        : Component.translatable("tenseicraft.social.guard");
                this.addRenderableWidget(Button.builder(stayLabel, b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_TOGGLE_STAY);
                }).bounds(rightColumnX, secondRowY, 105, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§a[Tiempo Restante: " + timeFormatted + "]\n§7" + (isStaying ? "Haz que el aventurero reanude su guardia siguiéndote." : "Ordena al aventurero que mantenga su posición aquí."))))
                .build());

                // Dismiss contract early
                this.addRenderableWidget(Button.builder(Component.translatable("tenseicraft.social.dismiss"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_DISMISS);
                }).bounds(rightColumnX + 110, secondRowY, 90, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§c[Finalizar Contrato]\n§7Tiempo restante: §a" + timeFormatted + "\n§7Termina el contrato de escolta anticipadamente.")))
                .build());

                int thirdRowY = actionY + 48;
                this.addRenderableWidget(Button.builder(Component.literal("§e🪪 Tarjeta"), b -> {
                    if (this.minecraft != null && this.minecraft.level != null) {
                        Entity entity = this.minecraft.level.getEntity(entityId);
                        if (entity instanceof LivingEntity living) {
                            this.minecraft.setScreen(new AdventurerCardScreen(living, this));
                        }
                    }
                }).bounds(rightColumnX, thirdRowY, 100, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§e[Tarjeta Oficial de Aventurero]\n§7Inspecciona la credencial oficial, rango, miniatura 3D y estadísticas de combate de este aventurero.")))
                .build());

                this.addRenderableWidget(Button.builder(Component.literal("§6🎒 Mochila"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_OPEN_COMPANION_INVENTORY);
                }).bounds(rightColumnX + 104, thirdRowY, 95, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§6[Mochila de Camarada]\n§7Abre el inventario de 18 casillas del aventurero.\n§e- Guarda comida y manzanas doradas para que se cure solo.\n§e- Revisa los minerales y botín recolectado del suelo.")))
                .build());
            }
        }

        // 4. Townswoman specific: Hiring disabled for Alpha testing (preserved for future update)
        // Townswomen remain as social/friendly village NPCs (chat, gifts, affinity)

        // 5. Guild Master specific: Register in Guild / View Quests
        if ("guild_master".equals(npcType)) {
            Player localPlayer = Minecraft.getInstance().player;
            com.pablo.tenseicraft.data.PlayerRpgData clientData = localPlayer != null ? localPlayer.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA) : null;
            boolean isRegistered = clientData != null && clientData.isGuildRegistered();
            boolean isCriminal = clientData != null && clientData.getReputationScore() < 0;

            int secondRowY = actionY + 24;

            if (!isRegistered && !isCriminal) {
                this.addRenderableWidget(Button.builder(Component.literal("§6⚔ Inscribirse al Gremio"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_REGISTER_GUILD);
                }).bounds(rightColumnX, secondRowY, 170, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§6[Inscripción en el Gremio de Aventureros]\n§eÚnete formalmente al gremio como §7Aventurero de Rango F§e.\n§7- Recibe tu tag oficial sobre la cabeza.\n§7- Desbloquea el acceso a misiones de aventurero.\n§7- Multiplicador de reputación oficial.")))
                .build());
            } else if (isRegistered) {
                this.addRenderableWidget(Button.builder(Component.literal("§e📜 Tablón de Misiones"), b -> {
                    Minecraft.getInstance().setScreen(new RpgQuestsScreen(this));
                }).bounds(rightColumnX, secondRowY, 110, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§eConsulta el tablón de misiones disponibles para tu rango de aventurero.")))
                .build());

                if (!clientData.getRank().isMaxRank()) {
                    com.pablo.tenseicraft.data.AdventurerRank nextRank = clientData.getRank().getNextRank();
                    int reqLvl = clientData.getCurrentMilestoneCap();
                    boolean ready = clientData.isAtMilestoneCap();
                    Component btnLabel = ready
                            ? Component.literal("§d⚡ Hito Alcanzado")
                            : Component.literal("§b⚡ Hito Nivel " + reqLvl);
                    this.addRenderableWidget(Button.builder(btnLabel, b -> {
                        ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_GUILD_ASCEND);
                    }).bounds(rightColumnX + 114, secondRowY, 100, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§d[Ascensión Divina en el Plano Astral]\n§eHito requerido para " + nextRank.getBadge().getString() + "§e: §fNivel " + reqLvl + " §7(Actual: " + clientData.getRpgLevel() + ")\n§7Al alcanzar el Hito y dormir en una cama, tu Dios te otorgará la Ascensión en el Espacio Astral.")))
                    .build());
                }

                if (companionReadyToAscend) {
                    int thirdRowY = actionY + 48;
                    this.addRenderableWidget(Button.builder(Component.literal("§6⚡ Ascender Camarada"), b -> {
                        ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_GUILD_ASCEND_COMPANION);
                    }).bounds(rightColumnX, thirdRowY, 150, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§6[Ascensión de Rango para tu Camarada]\n§eRequisitos de experiencia cumplidos.\n§7Asciende formalmente a tu aventurero aliado a su siguiente rango en el gremio.")))
                    .build());
                }
            }
        }

        // 6. Alice specific: Start Quest / Show Progress / Claim Reward
        if ("alice".equals(npcType)) {
            Player localPlayer = Minecraft.getInstance().player;
            com.pablo.tenseicraft.data.PlayerRpgData clientData = localPlayer != null ? localPlayer.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA) : null;
            boolean questActive = clientData != null && clientData.isAliceQuestActive();
            boolean questCompleted = clientData != null && clientData.isAliceQuestCompleted();
            boolean receivedSword = clientData != null && clientData.hasReceivedSpiritualSword();
            int kills = clientData != null ? clientData.getAliceDMonstersKilled() : 0;

            int secondRowY = actionY + 24;

            if (!questActive && !questCompleted) {
                this.addRenderableWidget(Button.builder(Component.literal("§d⚔ Aceptar Encargo"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_START_QUEST);
                }).bounds(rightColumnX, secondRowY, 150, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§d[Misión: Purificación de Rango D]\n§eDerrota a 5 monstruos de Rango D (Brujas, Hoglins o Drowneds con tridente).\n§7- Recompensa: Espada Espiritual virgen y Técnica de Canalizar Magia.")))
                .build());
            } else if (questActive && !questCompleted) {
                this.addRenderableWidget(Button.builder(Component.literal("§e📜 Progreso: " + kills + "/5 Rango D"), b -> {})
                .bounds(rightColumnX, secondRowY, 160, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§eElimina 5 monstruos de Rango D para demostrar tu temple espiritual a los Segadores.")))
                .build());
            } else if (questCompleted && !receivedSword) {
                this.addRenderableWidget(Button.builder(Component.literal("§6⚡ Reclamar Espada"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_CLAIM_REWARD);
                }).bounds(rightColumnX, secondRowY, 170, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§6[Forja Espiritual Completada]\n§eReclama tu Espada Espiritual virgen y despierta tu alma para aprender técnicas.")))
                .build());
            } else if (receivedSword) {
                boolean hasSwordInInventory = false;
                if (localPlayer != null) {
                    if (localPlayer.getMainHandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem
                            || localPlayer.getOffhandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
                        hasSwordInInventory = true;
                    } else {
                        for (ItemStack item : localPlayer.getInventory().items) {
                            if (!item.isEmpty() && item.getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
                                hasSwordInInventory = true;
                                break;
                            }
                        }
                    }
                }

                if (hasSwordInInventory) {
                    this.addRenderableWidget(Button.builder(Component.literal("§b✨ Tienda"), b -> {
                        Minecraft.getInstance().setScreen(new AliceTechniquesShopScreen(this));
                    }).bounds(rightColumnX, secondRowY, 95, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§b[Tienda de Técnicas Espirituales]\n§eAprende cánticos espirituales de nivel Principiante e Intermedio a cambio de 5 Esmeraldas cada uno.")))
                    .build());

                    boolean shadowbladeUnlocked = isDevoted || affinity >= 50;
                    if (shadowbladeUnlocked) {
                        if (isHiredByPlayer) {
                            long totalSeconds = hireRemainingTicks / 20L;
                            long minutes = totalSeconds / 60L;
                            long seconds = totalSeconds % 60L;
                            String timeFormatted = String.format("%02d:%02d", minutes, seconds);
                            this.addRenderableWidget(Button.builder(Component.literal("§a✔ Apoyo (" + timeFormatted + ")"), b -> {})
                                    .bounds(rightColumnX + 99, secondRowY, 110, btnHeight)
                                    .tooltip(Tooltip.create(Component.literal("§a[Asistencia Espiritual de Alice Activa]\n§7Alice combate a tu lado y te asiste con Fortificar Aliado y Sanación.\n§eTiempo restante: " + timeFormatted)))
                                    .build());
                        } else {
                            this.addRenderableWidget(Button.builder(Component.literal("§5✨ Apoyo (20m)"), b -> {
                                ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_HIRE_SUPPORT);
                            }).bounds(rightColumnX + 99, secondRowY, 110, btnHeight)
                            .tooltip(Tooltip.create(Component.literal("§5[Pedir Asistencia a Alice]\n§eAlice te acompañará durante 20 minutos en combate.\n§7- Lanza 'Técnica Espiritual N° 17: Fortificar Aliado' (Fuerza II y Resistencia II).\n§7- Te cura cuando estás malherido.\n§7- Desata cortes mágicos contra monstruos.")))
                            .build());
                        }
                    } else {
                        this.addRenderableWidget(Button.builder(Component.literal("§8🔒 Apoyo"), b -> {})
                                .bounds(rightColumnX + 99, secondRowY, 110, btnHeight)
                                .tooltip(Tooltip.create(Component.literal("§8[Asistencia Espiritual Bloqueada]\n§7Para ganar la lealtad y el apoyo en combate de Alice, debes purificar y entregarle como ofrenda la §dShadowblade del Cultista§7.")))
                                .build());
                    }
                } else {
                    // When player lost their sword: show Tienda (60), Apoyo (65), and Reemplazar (82)
                    this.addRenderableWidget(Button.builder(Component.literal("§b✨ Tienda"), b -> {
                        Minecraft.getInstance().setScreen(new AliceTechniquesShopScreen(this));
                    }).bounds(rightColumnX, secondRowY, 60, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§b[Tienda de Técnicas Espirituales]\n§eAprende cánticos espirituales a cambio de 5 Esmeraldas cada uno.")))
                    .build());

                    boolean shadowbladeUnlocked = isDevoted || affinity >= 50;
                    if (shadowbladeUnlocked) {
                        if (isHiredByPlayer) {
                            long totalSeconds = hireRemainingTicks / 20L;
                            long minutes = totalSeconds / 60L;
                            long seconds = totalSeconds % 60L;
                            String timeFormatted = String.format("%02d:%02d", minutes, seconds);
                            this.addRenderableWidget(Button.builder(Component.literal("§a✔ Apoyo"), b -> {})
                                    .bounds(rightColumnX + 64, secondRowY, 65, btnHeight)
                                    .tooltip(Tooltip.create(Component.literal("§a[Asistencia de Alice Activa]\n§7Alice combate a tu lado.\n§eTiempo restante: " + timeFormatted)))
                                    .build());
                        } else {
                            this.addRenderableWidget(Button.builder(Component.literal("§5✨ Apoyo"), b -> {
                                ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_HIRE_SUPPORT);
                            }).bounds(rightColumnX + 64, secondRowY, 65, btnHeight)
                            .tooltip(Tooltip.create(Component.literal("§5[Pedir Asistencia a Alice (20m)]\n§eAlice te acompañará en combate con técnicas de sanación y fortificación.")))
                            .build());
                        }
                    } else {
                        this.addRenderableWidget(Button.builder(Component.literal("§8🔒 Apoyo"), b -> {})
                                .bounds(rightColumnX + 64, secondRowY, 65, btnHeight)
                                .tooltip(Tooltip.create(Component.literal("§8[Asistencia Bloqueada]\n§7Ofrece la §dShadowblade del Cultista§7 para desbloquear.")))
                                .build());
                    }

                    this.addRenderableWidget(Button.builder(Component.literal("§6⚔ Espada (20🟢)"), b -> {
                        ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_BUY_REPLACEMENT_SWORD);
                    }).bounds(rightColumnX + 133, secondRowY, 82, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§6[Reemplazar Espada Perdida]\n§e¿Extraviaste tu espada espiritual? Alice forjará una nueva hoja virgen por 20 esmeraldas.\n§7Deberás nombrarla en un yunque y canalizar maná [X] para forjar un nuevo vínculo.")))
                    .build());
                }
            }

            // Misión de la Orden: El Segador Corrupto (Desbloqueado tras recibir la Espada Espiritual)
            if (receivedSword) {
                com.pablo.tenseicraft.quests.PlayerQuestData clientQuestData = localPlayer != null ? localPlayer.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_QUEST_DATA) : null;
                boolean isCultistActive = clientQuestData != null && clientQuestData.isQuestActive("alice_cultist_hunt");
                boolean isCultistClaimed = clientQuestData != null && clientQuestData.isQuestClaimed("alice_cultist_hunt");
                boolean isCultistKilled = (localPlayer != null && localPlayer.getPersistentData().getBoolean("RpgKilledCultistBoss"))
                        || (clientQuestData != null && clientQuestData.isCompleted("alice_cultist_hunt"));

                int thirdRowY = actionY + 48;

                if (!isCultistActive && !isCultistClaimed && !isCultistKilled) {
                    this.addRenderableWidget(Button.builder(Component.literal("§5📜 Misión: Cultista"), b -> {
                        ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_START_CULTIST_QUEST);
                    }).bounds(rightColumnX, thirdRowY, 110, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§5[Misión Rango A: El Segador Corrupto]\n§eAlice te encomendará la caza del Cultista que busca resucitar a los Demonios en el Abismo.")))
                    .build());
                } else if (isCultistKilled && !isCultistClaimed) {
                    this.addRenderableWidget(Button.builder(Component.literal("§6🏆 Entregar"), b -> {
                        ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_CLAIM_CULTIST_REWARD);
                    }).bounds(rightColumnX, thirdRowY, 110, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§6¡Has derrotado al Cultista!\n§eHabla con Alice para entregar la misión, recibir tus recompensas y concluir el contenido de la Alpha.")))
                    .build());
                } else {
                    this.addRenderableWidget(Button.builder(Component.literal("§5🌀 Viajar (Abismo)"), b -> {
                        ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_TELEPORT_ABYSS);
                        this.onClose();
                    }).bounds(rightColumnX, thirdRowY, 110, btnHeight)
                    .tooltip(Tooltip.create(Component.literal("§5[Expedición al Abismo]\n§eAlice abrirá una brecha dimensional para ti y tus aliados hacia el Abismo (e imbuirá a todos con su manto protector).")))
                    .build());
                }

                // Botón para recibir o renovar la técnica protectora de 20 minutos
                this.addRenderableWidget(Button.builder(Component.literal("§b🛡️ Manto (20m)"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_REQUEST_WARD);
                }).bounds(rightColumnX + 114, thirdRowY, 96, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§b[Protección del Abismo (20 min)]\n§eAlice imbuye tu alma con un manto protector.\n§7- Evita que el Vacío del Abismo te rechace y expulse.\n§7- El efecto dura exactamente 20 minutos.\n§7- Deja de ser necesario si tu Maná Máximo alcanza los 500 MP.")))
                .build());

                // Botón para Reroll Completo de la Espada Espiritual (25 esmeraldas)
                this.addRenderableWidget(Button.builder(Component.literal("§d🎲 Reroll Espada (25❇)"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_ALICE_REROLL_SOUL_BLADE);
                }).bounds(rightColumnX, topPos + GUI_HEIGHT - 24, 132, 18)
                .tooltip(Tooltip.create(Component.literal("§d[Reroll Espiritual de Hoja (25 Esmeraldas)]\n§eAlice rearmonizará tu alma con el cosmos para forjar un nuevo destino para tu espada:\n§7- Reroll completo: Estilo, Elemento, 1ª Liberación, Resonancia y Skin.\n§a- ¡Conservas el despertar! Al presionar [Z] mutará a su nueva forma sin repetir la prueba.")))
                .build());
            }
        }

        // 7. Megu specific: Reforge & Buy Magic Anvil
        if ("megu".equals(npcType)) {
            Player localPlayer = Minecraft.getInstance().player;
            com.pablo.tenseicraft.data.PlayerRpgData clientData = localPlayer != null ? localPlayer.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA) : null;
            boolean anvilUnlocked = isDevoted || (clientData != null && clientData.isMeguAnvilUnlocked());
            int reforgeCount = Math.max(workerStatus, clientData != null ? clientData.getMeguReforgeCount() : 0);
            int secondRowY = actionY + 24;

            this.addRenderableWidget(Button.builder(Component.literal("§6🔨 Reforjar Armas"), b -> {
                ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_MEGU_REFORGE);
            }).bounds(rightColumnX, secondRowY, 106, btnHeight)
            .tooltip(Tooltip.create(Component.literal("§6[Mesa de Reforja de Megu]\n§7Abre la ruleta de reforja con Cristales de Alma para subir de rango tu equipo (hasta Rango B).")))
            .build());

            if (anvilUnlocked) {
                this.addRenderableWidget(Button.builder(Component.literal("§d⚒ Yunque (30🟢)"), b -> {
                    ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_MEGU_BUY_MAGIC_ANVIL);
                }).bounds(rightColumnX + 110, secondRowY, 100, btnHeight)
                .tooltip(Tooltip.create(Component.literal("§d[Comprar Yunque Mágico Propio]\n§eCosto: §a30 Esmeraldas\n§7Megu ha reconocido que tu equipo supera su habilidad de herrera y te permite adquirir tu propio Yunque Mágico.")))
                .build());
            } else {
                Button lockedBtn = Button.builder(Component.literal("§8🔒 Yunque Mágico"), b -> {})
                        .bounds(rightColumnX + 110, secondRowY, 100, btnHeight)
                        .tooltip(Tooltip.create(Component.literal("§8[Yunque Mágico Bloqueado]\n§7Pídele a Megu que examine/reforje tu equipo (" + Math.min(3, reforgeCount) + "/3) o acude con equipo de alto nivel hasta que reconozca que has superado su habilidad.")))
                        .build();
                lockedBtn.active = false;
                this.addRenderableWidget(lockedBtn);
            }
        }

        // 8. Reimu specific: Rematch Challenge
        if ("reimu".equals(npcType)) {
            int secondRowY = actionY + 24;
            this.addRenderableWidget(Button.builder(Component.literal("§c⛩ Volver a Desafiar"), b -> {
                ModNetworking.sendNpcDialogueAction(entityId, NpcDialogueActionPayload.ACTION_REIMU_CHALLENGE);
                this.onClose();
            }).bounds(rightColumnX, secondRowY, 150, btnHeight)
            .tooltip(Tooltip.create(Component.literal("§c[Desafío del Santuario Hakurei]\n§eReimu volverá a desplegar sus reglas de Spell Cards para combatir contra ti.\n§7- Rango Calamidad (5000 HP)\n§7- Proyectiles mortales al instante\n§7- Aturdida 5s por golpe antes de empujarte")))
            .build());
        }

        // Close / Farewell button
        this.addRenderableWidget(Button.builder(Component.translatable("tenseicraft.social.farewell"), b -> {
            this.onClose();
        }).bounds(leftPos + GUI_WIDTH - 85, topPos + GUI_HEIGHT - 24, 75, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intencionalmente vacío para evitar processBlurEffect de Minecraft 1.21
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Oscurecer el fondo sin shader de desenfoque
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        // 1. Background Frames (MCA / Dark RPG style)
        renderBackgroundFrames(guiGraphics);

        // 2. 3D Model of the NPC
        renderNpcPortrait(guiGraphics, mouseX, mouseY);

        // 3. NPC Name, Title and Rank
        renderNpcHeader(guiGraphics);

        // 4. Affinity Hearts Bar (MCA Style)
        renderAffinityHearts(guiGraphics);

        // 5. Dialogue Speech Bubble
        renderDialogueBubble(guiGraphics);

        // 6. Render interactive action buttons on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderBackgroundFrames(GuiGraphics guiGraphics) {
        // Main panel
        guiGraphics.fillGradient(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF212141F, 0xF21A1E2C);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFD4AF37);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0xFF35394A);

        // Separator between portrait and dialogue
        guiGraphics.vLine(leftPos + 108, topPos + 8, topPos + GUI_HEIGHT - 8, 0xFF4A4E65);
    }

    private void renderNpcPortrait(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int pX1 = leftPos + 12;
        int pY1 = topPos + 12;
        int pX2 = leftPos + 98;
        int pY2 = topPos + 140;

        guiGraphics.fillGradient(pX1, pY1, pX2, pY2, 0xF00B0D14, 0xF8101420);
        guiGraphics.renderOutline(pX1, pY1, pX2 - pX1, pY2 - pY1, 0xFF555A70);

        if (cachedEntity != null) {
            Component oldName = cachedEntity.getCustomName();
            boolean oldVisible = cachedEntity.isCustomNameVisible();
            try {
                cachedEntity.setCustomName(null);
                cachedEntity.setCustomNameVisible(false);
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        guiGraphics,
                        pX1, pY1, pX2, pY2,
                        46, 0.0625f,
                        mouseX, mouseY,
                        cachedEntity
                );
            } finally {
                cachedEntity.setCustomName(oldName);
                cachedEntity.setCustomNameVisible(oldVisible);
            }
        }
    }

    private void renderNpcHeader(GuiGraphics guiGraphics) {
        // Name & Title
        int textX = leftPos + 120;
        int textY = topPos + 14;

        // Clean redundant titles from name if present (e.g. "Gareth - Maestro del Gremio" -> "Gareth")
        String cleanName = npcName != null ? npcName : "NPC";
        if (cleanName.contains(" - ")) {
            cleanName = cleanName.substring(0, cleanName.indexOf(" - "));
        }

        guiGraphics.drawString(this.font, Component.literal("§6§l" + cleanName), textX, textY, 0xFFFFFFFF, true);

        if ("adventurer".equals(npcType)) {
            AdventurerRank rank = AdventurerRank.fromIndex(rankIndex);
            guiGraphics.drawString(this.font, Component.literal("§7").append(Component.translatable("tenseicraft.social.npc_type_adventurer")).append(" | ").append(rank.getBadge()), textX, textY + 11, 0xFFAAAAAA, false);
        } else if ("guild_master".equals(npcType)) {
            guiGraphics.drawString(this.font, Component.literal("§6★ Maestro del Gremio ★"), textX, textY + 11, 0xFFFFAA00, false);
        } else if ("alice".equals(npcType)) {
            guiGraphics.drawString(this.font, Component.literal("§5✦ Ingeniera de Segadores ✦"), textX, textY + 11, 0xFFDA70D6, false);
        } else if ("megu".equals(npcType)) {
            guiGraphics.drawString(this.font, Component.literal("§6⚒ Artesana y Herrera de Reforjas ⚒"), textX, textY + 11, 0xFFFFAA00, false);
        } else if ("reimu".equals(npcType)) {
            guiGraphics.drawString(this.font, Component.literal("§c⛩ Miko del Santuario Hakurei (Gensokyo) ⛩"), textX, textY + 11, 0xFFFF5555, false);
        } else if ("villager".equals(npcType)) {
            guiGraphics.drawString(this.font, Component.literal("§e").append(Component.translatable("tenseicraft.social.npc_type_villager")), textX, textY + 11, 0xFFAAAAAA, false);
        } else {
            if (isDevoted) {
                String rankStr = switch (vassalRank) {
                    case 4 -> "§6★ Vasalla Suprema (Rango S) ★";
                    case 3 -> "§d★ Guardiana Élite (Rango A) ★";
                    case 2 -> "§b★ Vasalla Fiel (Rango B) ★";
                    default -> "§a★ Subordinada (Rango C) ★";
                };
                guiGraphics.drawString(this.font, Component.literal(rankStr), textX, textY + 11, 0xFFFFAA00, false);
            } else {
                guiGraphics.drawString(this.font, Component.literal("§e").append(Component.translatable("tenseicraft.social.npc_type_townswoman")), textX, textY + 11, 0xFFAAAAAA, false);
            }
        }
    }

    private void renderAffinityHearts(GuiGraphics guiGraphics) {
        // 5 Hearts Meter (each heart represents 20 affinity points)
        int startX = leftPos + 12;
        int startY = topPos + 148;

        NpcSocialManager.RelationshipTier tier = NpcSocialManager.RelationshipTier.fromPoints(affinity);

        // Hearts string: 5 hearts total
        StringBuilder hearts = new StringBuilder();
        int fullHearts = affinity / 20;
        boolean hasHalf = (affinity % 20) >= 10;

        for (int i = 0; i < 5; i++) {
            if (i < fullHearts) {
                hearts.append("§c❤ ");
            } else if (i == fullHearts && hasHalf) {
                hearts.append("§e❤ ");
            } else {
                hearts.append("§8❤ ");
            }
        }

        guiGraphics.drawString(this.font, Component.literal(hearts.toString().trim()), startX, startY, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, Component.literal(tier.getColorCode() + tier.getDisplayName() + " §7(" + affinity + "/100)"), startX, startY + 12, 0xFFAAAAAA, false);
    }

    private void renderDialogueBubble(GuiGraphics guiGraphics) {
        int bubbleX = leftPos + 116;
        int bubbleY = topPos + 38;
        int bubbleW = GUI_WIDTH - 128;
        int bubbleH = 82;

        // Speech bubble container
        guiGraphics.fillGradient(bubbleX, bubbleY, bubbleX + bubbleW, bubbleY + bubbleH, 0xAA080A12, 0xAA121625);
        guiGraphics.renderOutline(bubbleX, bubbleY, bubbleW, bubbleH, 0xFF42475E);

        // Quote symbol
        guiGraphics.drawString(this.font, Component.literal("§e“"), bubbleX + 6, bubbleY + 6, 0xFFFFAA00, false);

        // Wrapped dialogue text (accounting for scrollbar width)
        List<FormattedCharSequence> lines = this.font.split(Component.literal(dialogueText), bubbleW - 32);
        int maxScroll = Math.max(0, lines.size() - MAX_VISIBLE_LINES);
        dialogueScrollOffset = Math.max(0, Math.min(dialogueScrollOffset, maxScroll));

        int startLine = dialogueScrollOffset;
        int endLine = Math.min(lines.size(), startLine + MAX_VISIBLE_LINES);
        int lineY = bubbleY + 8;
        for (int i = startLine; i < endLine; i++) {
            guiGraphics.drawString(this.font, lines.get(i), bubbleX + 18, lineY, 0xFFFFFFFF, false);
            lineY += 11;
        }

        // Scrollbar if there are more lines than MAX_VISIBLE_LINES
        if (maxScroll > 0) {
            int trackX = bubbleX + bubbleW - 8;
            int trackY = bubbleY + 8;
            int trackW = 4;
            int trackH = bubbleH - 16;

            // Track background
            guiGraphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0x55000000);

            // Thumb
            int thumbH = Math.max(12, trackH * MAX_VISIBLE_LINES / lines.size());
            int thumbY = trackY + (int) ((trackH - thumbH) * ((float) dialogueScrollOffset / maxScroll));
            guiGraphics.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, 0xFF6C80A8);

            // Up/Down arrows
            if (dialogueScrollOffset > 0) {
                guiGraphics.drawString(this.font, Component.literal("§e▲"), trackX - 7, trackY - 4, 0xFFFFAA00, false);
            }
            if (dialogueScrollOffset < maxScroll) {
                guiGraphics.drawString(this.font, Component.literal("§e▼"), trackX - 7, trackY + trackH - 5, 0xFFFFAA00, false);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int bubbleX = leftPos + 116;
        int bubbleY = topPos + 38;
        int bubbleW = GUI_WIDTH - 128;
        int bubbleH = 82;

        if (mouseX >= bubbleX && mouseX <= bubbleX + bubbleW && mouseY >= bubbleY && mouseY <= bubbleY + bubbleH) {
            List<FormattedCharSequence> lines = this.font.split(Component.literal(dialogueText), bubbleW - 32);
            int maxScroll = Math.max(0, lines.size() - MAX_VISIBLE_LINES);
            if (maxScroll > 0) {
                if (scrollY > 0) {
                    dialogueScrollOffset = Math.max(0, dialogueScrollOffset - 1);
                } else if (scrollY < 0) {
                    dialogueScrollOffset = Math.min(maxScroll, dialogueScrollOffset + 1);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bubbleX = leftPos + 116;
        int bubbleY = topPos + 38;
        int bubbleW = GUI_WIDTH - 128;
        int bubbleH = 82;

        if (button == 0 && mouseX >= bubbleX + bubbleW - 16 && mouseX <= bubbleX + bubbleW) {
            List<FormattedCharSequence> lines = this.font.split(Component.literal(dialogueText), bubbleW - 32);
            int maxScroll = Math.max(0, lines.size() - MAX_VISIBLE_LINES);
            if (maxScroll > 0) {
                if (mouseY >= bubbleY && mouseY <= bubbleY + 20) {
                    dialogueScrollOffset = Math.max(0, dialogueScrollOffset - 1);
                    return true;
                } else if (mouseY >= bubbleY + bubbleH - 20 && mouseY <= bubbleY + bubbleH) {
                    dialogueScrollOffset = Math.min(maxScroll, dialogueScrollOffset + 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ItemStack findSpiritualSword(Player player) {
        if (player == null) return null;
        if (player.getMainHandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
            return player.getOffhandItem();
        }
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
                return item;
            }
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
