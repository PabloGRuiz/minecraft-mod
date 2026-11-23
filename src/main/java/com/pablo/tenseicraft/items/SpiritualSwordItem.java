package com.pablo.tenseicraft.items;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.pablo.tenseicraft.party.PartyManager;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;

public class SpiritualSwordItem extends SwordItem implements IPresetSpellContainer {

    public static final String TAG_INITIALIZED = "SoulImprinted";
    public static final String TAG_SOUL_BOUND = "SoulBound";
    public static final String TAG_FIRST_RELEASE = "FirstRelease";
    public static final String TAG_AFFINITY = "SoulBladeAffinity";
    public static final String TAG_TRIAL_READY = "SwordSpiritTrialReady";
    public static final String TAG_FIRST_RELEASE_UNLOCKED = "FirstReleaseUnlocked";
    public static final String TAG_OWNER_NAME = "SoulOwnerName";
    public static final String TAG_OWNER_UUID = "SoulOwnerUUID";
    public static final String TAG_STYLE = "BladeStyle";
    public static final String TAG_STYLE_VARIANT = "BladeStyleVariant";
    public static final String TAG_ELEMENT = "BladeElement";
    public static final String TAG_RELEASE = "BladeRelease";
    public static final String TAG_RESONANCE = "BladeResonance";
    public static final String TAG_SOUL_SEED = "SoulSeed";
    public static final String TAG_DENSE_SOUL_BLADE = "DenseSoulBlade";

    public static boolean isValidCombatTarget(Entity attacker, Entity target) {
        if (target == null || !target.isAlive() || target == attacker) return false;
        if (attacker != null && PartyManager.isAllied(attacker, target)) return false;
        if (attacker != null && attacker.isAlliedTo(target)) return false;
        return true;
    }

    // =========================================================================
    // 1. ESTILO DE COMBATE (4 Diseños Visuales + Pasiva de Combate)
    // =========================================================================
    public enum BladeStyle {
        FRENZY("frenzy", 1, ChatFormatting.AQUA, "tenseicraft.spiritual_sword.style.frenzy", "tenseicraft.spiritual_sword.style_desc.frenzy"),
        DUELIST("duelist", 2, ChatFormatting.GOLD, "tenseicraft.spiritual_sword.style.duelist", "tenseicraft.spiritual_sword.style_desc.duelist"),
        BULWARK("bulwark", 3, ChatFormatting.BLUE, "tenseicraft.spiritual_sword.style.bulwark", "tenseicraft.spiritual_sword.style_desc.bulwark"),
        PHANTOM("phantom", 4, ChatFormatting.DARK_AQUA, "tenseicraft.spiritual_sword.style.phantom", "tenseicraft.spiritual_sword.style_desc.phantom");

        private final String id;
        private final int customModelData;
        private final ChatFormatting color;
        private final String translationKey;
        private final String descriptionKey;

        BladeStyle(String id, int customModelData, ChatFormatting color, String translationKey, String descriptionKey) {
            this.id = id;
            this.customModelData = customModelData;
            this.color = color;
            this.translationKey = translationKey;
            this.descriptionKey = descriptionKey;
        }

        public String getId() { return id; }
        public int getCustomModelData() { return customModelData; }
        public ChatFormatting getColor() { return color; }
        public String getTranslationKey() { return translationKey; }
        public String getDescriptionKey() { return descriptionKey; }

        public static BladeStyle byId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT).trim();
            for (BladeStyle style : values()) {
                if (style.id.equals(normalized)) return style;
            }
            return switch (normalized) {
                case "duelista" -> DUELIST;
                case "frenesi", "frenesí" -> FRENZY;
                case "baluarte" -> BULWARK;
                case "fantasma", "filo_fantasma", "katana" -> PHANTOM;
                default -> null;
            };
        }
    }

    // =========================================================================
    // 2. AFINIDAD ELEMENTAL (4 Elementos + Partículas + Efecto de Impacto)
    // =========================================================================
    public enum BladeElement {
        NONE("none", ChatFormatting.AQUA, "tenseicraft.spiritual_sword.element.none", "tenseicraft.spiritual_sword.element_desc.none"),
        IGNIS("ignis", ChatFormatting.GOLD, "tenseicraft.spiritual_sword.element.ignis", "tenseicraft.spiritual_sword.element_desc.ignis"),
        FULGUR("fulgur", ChatFormatting.YELLOW, "tenseicraft.spiritual_sword.element.fulgur", "tenseicraft.spiritual_sword.element_desc.fulgur"),
        GLACIES("glacies", ChatFormatting.AQUA, "tenseicraft.spiritual_sword.element.glacies", "tenseicraft.spiritual_sword.element_desc.glacies"),
        UMBRA("umbra", ChatFormatting.DARK_PURPLE, "tenseicraft.spiritual_sword.element.umbra", "tenseicraft.spiritual_sword.element_desc.umbra");

        public static final BladeElement[] ACTIVE_ELEMENTS = {IGNIS, FULGUR, GLACIES, UMBRA};

        private final String id;
        private final ChatFormatting color;
        private final String translationKey;
        private final String descriptionKey;

        BladeElement(String id, ChatFormatting color, String translationKey, String descriptionKey) {
            this.id = id;
            this.color = color;
            this.translationKey = translationKey;
            this.descriptionKey = descriptionKey;
        }

        public String getId() { return id; }
        public ChatFormatting getColor() { return color; }
        public String getTranslationKey() { return translationKey; }
        public String getDescriptionKey() { return descriptionKey; }

        public static BladeElement byId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT).trim();
            for (BladeElement element : values()) {
                if (element.id.equals(normalized)) return element;
            }
            return switch (normalized) {
                case "fuego", "fire" -> IGNIS;
                case "rayo", "trueno", "lightning" -> FULGUR;
                case "hielo", "escarcha", "ice" -> GLACIES;
                case "sombra", "oscuridad", "shadow", "dark" -> UMBRA;
                case "ninguno", "clear" -> NONE;
                default -> null;
            };
        }
    }

    // =========================================================================
    // 3. PRIMERA LIBERACIÓN ACTIVA (4 Habilidades con Agacharse + Clic Derecho)
    // =========================================================================
    public enum BladeRelease {
        SLASH("slash", 30.0f, 60, ChatFormatting.RED, "tenseicraft.spiritual_sword.release.slash", "tenseicraft.spiritual_sword.release_desc.slash"),
        VORTEX("vortex", 35.0f, 100, ChatFormatting.LIGHT_PURPLE, "tenseicraft.spiritual_sword.release.vortex", "tenseicraft.spiritual_sword.release_desc.vortex"),
        STEP("step", 25.0f, 60, ChatFormatting.AQUA, "tenseicraft.spiritual_sword.release.step", "tenseicraft.spiritual_sword.release_desc.step"),
        OVERDRIVE("overdrive", 45.0f, 240, ChatFormatting.GOLD, "tenseicraft.spiritual_sword.release.overdrive", "tenseicraft.spiritual_sword.release_desc.overdrive"),
        JUDGEMENT("judgement", 40.0f, 80, ChatFormatting.DARK_PURPLE, "tenseicraft.spiritual_sword.release.judgement", "tenseicraft.spiritual_sword.release_desc.judgement"),
        BARRAGE("barrage", 30.0f, 60, ChatFormatting.LIGHT_PURPLE, "tenseicraft.spiritual_sword.release.barrage", "tenseicraft.spiritual_sword.release_desc.barrage"),
        DOMINION("dominion", 45.0f, 120, ChatFormatting.BLUE, "tenseicraft.spiritual_sword.release.dominion", "tenseicraft.spiritual_sword.release_desc.dominion"),
        PIERCE("pierce", 35.0f, 80, ChatFormatting.YELLOW, "tenseicraft.spiritual_sword.release.pierce", "tenseicraft.spiritual_sword.release_desc.pierce");

        private final String id;
        private final float manaCost;
        private final int cooldownTicks;
        private final ChatFormatting color;
        private final String translationKey;
        private final String descriptionKey;

        BladeRelease(String id, float manaCost, int cooldownTicks, ChatFormatting color, String translationKey, String descriptionKey) {
            this.id = id;
            this.manaCost = manaCost;
            this.cooldownTicks = cooldownTicks;
            this.color = color;
            this.translationKey = translationKey;
            this.descriptionKey = descriptionKey;
        }

        public String getId() { return id; }
        public float getManaCost() { return manaCost; }
        public int getCooldownTicks() { return cooldownTicks; }
        public ChatFormatting getColor() { return color; }
        public String getTranslationKey() { return translationKey; }
        public String getDescriptionKey() { return descriptionKey; }

        public static BladeRelease byId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT).trim();
            for (BladeRelease rel : values()) {
                if (rel.id.equals(normalized)) return rel;
            }
            return switch (normalized) {
                case "corte", "slash", "getsuga" -> SLASH;
                case "vortice", "vórtice", "vortex" -> VORTEX;
                case "paso", "teleport", "dash", "step", "shunpo" -> STEP;
                case "trance", "buff", "overdrive" -> OVERDRIVE;
                case "juicio", "judgement", "dimensional" -> JUDGEMENT;
                case "danza", "barrage", "senbonzakura", "rafaga", "ráfaga" -> BARRAGE;
                case "dominio", "dominion", "gravedad" -> DOMINION;
                case "lanza", "pierce", "perforar", "cero" -> PIERCE;
                default -> null;
            };
        }
    }

    // =========================================================================
    // 4. RESONANCIA DEL ALMA (4 Sinergias con Estadísticas RPG)
    // =========================================================================
    public enum BladeResonance {
        SPIRIT("spirit", StatType.SPIRIT, ChatFormatting.LIGHT_PURPLE, "tenseicraft.spiritual_sword.resonance.spirit", "tenseicraft.spiritual_sword.resonance_desc.spirit"),
        FORTITUDE("fortitude", StatType.FORTITUDE, ChatFormatting.RED, "tenseicraft.spiritual_sword.resonance.fortitude", "tenseicraft.spiritual_sword.resonance_desc.fortitude"),
        DEXTERITY("dexterity", StatType.DEXTERITY, ChatFormatting.AQUA, "tenseicraft.spiritual_sword.resonance.dexterity", "tenseicraft.spiritual_sword.resonance_desc.dexterity"),
        LUCK("luck", StatType.LUCK, ChatFormatting.GREEN, "tenseicraft.spiritual_sword.resonance.luck", "tenseicraft.spiritual_sword.resonance_desc.luck");

        private final String id;
        private final StatType statType;
        private final ChatFormatting color;
        private final String translationKey;
        private final String descriptionKey;

        BladeResonance(String id, StatType statType, ChatFormatting color, String translationKey, String descriptionKey) {
            this.id = id;
            this.statType = statType;
            this.color = color;
            this.translationKey = translationKey;
            this.descriptionKey = descriptionKey;
        }

        public String getId() { return id; }
        public StatType getStatType() { return statType; }
        public ChatFormatting getColor() { return color; }
        public String getTranslationKey() { return translationKey; }
        public String getDescriptionKey() { return descriptionKey; }

        public static BladeResonance byId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT).trim();
            for (BladeResonance res : values()) {
                if (res.id.equals(normalized)) return res;
            }
            return switch (normalized) {
                case "espiritu", "espíritu" -> SPIRIT;
                case "fortaleza" -> FORTITUDE;
                case "destreza" -> DEXTERITY;
                case "suerte" -> LUCK;
                default -> null;
            };
        }
    }

    public SpiritualSwordItem(Properties properties) {
        super(Tiers.IRON, properties
                .attributes(SwordItem.createAttributes(Tiers.IRON, 3.5F, -2.4F))
                .component(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true))
                .rarity(Rarity.RARE));
    }

    // =========================================================================
    // INMUNIDAD TOTAL A ENCANTAMIENTOS (Mesa de encantamientos y Libros en Yunques)
    // =========================================================================
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    // =========================================================================
    // ESTADO DE VÍNCULO DE ALMA Y PRIMERA LIBERACIÓN
    // =========================================================================
    @SuppressWarnings("deprecation")
    private static CompoundTag getReadOnlyTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.getUnsafe() : null;
    }

    public static boolean isSoulBound(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.getBoolean(TAG_SOUL_BOUND);
    }

    public static boolean isSenbonzakura(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            if (custom != null) {
                String raw = custom.getString();
                if (raw != null) {
                    String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                    String norm = clean.toLowerCase(Locale.ROOT);
                    if (norm.contains("senbonzakura") || norm.contains("senbon zakura")) {
                        return true;
                    }
                }
            }
        }
        CompoundTag tag = getReadOnlyTag(stack);
        if (tag != null) {
            if (tag.contains("display", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                CompoundTag disp = tag.getCompound("display");
                if (disp.contains("Name", net.minecraft.nbt.Tag.TAG_STRING)) {
                    String nameStr = disp.getString("Name");
                    String clean = nameStr.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                    String norm = clean.toLowerCase(Locale.ROOT);
                    if (norm.contains("senbonzakura") || norm.contains("senbon zakura")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isFirstRelease(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.getBoolean(TAG_FIRST_RELEASE);
    }

    public static void setFirstRelease(ItemStack stack, boolean active) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_FIRST_RELEASE, active);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        if (active) {
            BladeStyle style = getBladeStyle(stack);
            int variant = getBladeVariant(stack);
            int cmd = style.getCustomModelData() + (variant == 1 ? 10 : 0);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(cmd));
        } else {
            if (isSenbonzakura(stack)) {
                stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(BladeStyle.PHANTOM.getCustomModelData()));
            } else {
                stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            }
        }
    }

    public static int getMaxAffinity(ItemStack stack) {
        return (isFirstReleaseUnlocked(stack) || isFirstRelease(stack)) ? 200 : 100;
    }

    public static int getAffinity(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        if (tag == null) return 0;
        int max = getMaxAffinity(stack);
        return Math.min(max, Math.max(0, tag.getInt(TAG_AFFINITY)));
    }

    public static void setAffinity(ItemStack stack, int affinity) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        int max = getMaxAffinity(stack);
        tag.putInt(TAG_AFFINITY, Math.min(max, Math.max(0, affinity)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        syncAwakenedSpell(stack);
    }

    public static boolean isTrialReady(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.getBoolean(TAG_TRIAL_READY);
    }

    public static void setTrialReady(ItemStack stack, boolean ready) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_TRIAL_READY, ready);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isFirstReleaseUnlocked(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.getBoolean(TAG_FIRST_RELEASE_UNLOCKED);
    }

    public static void setFirstReleaseUnlocked(ItemStack stack, boolean unlocked) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_FIRST_RELEASE_UNLOCKED, unlocked);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        syncAwakenedSpell(stack);
    }

    public static boolean isDenseSoulBlade(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.getBoolean(TAG_DENSE_SOUL_BLADE);
    }

    public static void setDenseSoulBlade(ItemStack stack, boolean dense) {
        if (isDenseSoulBlade(stack) == dense) {
            return;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_DENSE_SOUL_BLADE, dense);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void bindSoulVassal(ItemStack stack, Player player) {
        imprintSoulBlade(stack, player);

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_SOUL_BOUND, true);
        tag.putString(TAG_OWNER_NAME, player.getScoreboardName());
        tag.putString(TAG_OWNER_UUID, player.getUUID().toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        PlayerRpgData pData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        boolean denseSoul = pData != null && pData.isDenseSoul();
        setDenseSoulBlade(stack, denseSoul);
        if (denseSoul) {
            setFirstRelease(stack, true);
            syncAwakenedSpell(stack, false);
        } else {
            // En estado base recién enlazado se mantiene la textura base spiritual_sword00 (sin CustomModelData)
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            syncAwakenedSpell(stack, true);
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.pablo.tenseicraft.advancements.ModAdvancements.award(sp, com.pablo.tenseicraft.advancements.ModAdvancements.FORGE_SOUL_BOND);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    sp,
                    new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
            );
        }
    }

    // =========================================================================
    // SISTEMA IRROMPIBLE (ESPADAS VÍRGENES Y VINCULADAS INDESTRUCTIBLES)
    // =========================================================================
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        super.setDamage(stack, 0);
    }

    public static boolean isTemporarilyExhausted(ItemStack stack) {
        return false;
    }

    // =========================================================================
    // IMPRONTA DEL ALMA 4x4x4x4 (256 COMBINACIONES ÚNICAS POR JUGADOR)
    // =========================================================================
    public static boolean isInitialized(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return tag != null && tag.getBoolean(TAG_INITIALIZED);
    }

    /**
     * Imprime las 4 características únicas del alma del jugador.
     * Es aleatoria al descubrirse por primera vez en la partida/mundo, pero se almacena
     * en los datos del jugador para que en toda la partida conserve siempre esa misma combinación única.
     */
    public static void imprintSoulBlade(ItemStack stack, Player player) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        BladeStyle style;
        BladeElement element;
        BladeRelease release;
        BladeResonance resonance;
        int variant;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            if (!data.hasRolledSoulBlade()) {
                // Generación aleatoria al forjar/despertar su espada por primera vez en esta partida
                var random = player.getRandom();
                BladeStyle rolledStyle = BladeStyle.values()[random.nextInt(BladeStyle.values().length)];
                BladeElement rolledElement = BladeElement.ACTIVE_ELEMENTS[random.nextInt(BladeElement.ACTIVE_ELEMENTS.length)];
                BladeRelease rolledRelease = BladeRelease.values()[random.nextInt(BladeRelease.values().length)];
                BladeResonance rolledResonance = BladeResonance.values()[random.nextInt(BladeResonance.values().length)];
                int rolledVariant = random.nextInt(2);

                data.setSoulBladeTraits(rolledStyle.getId(), rolledElement.getId(), rolledRelease.getId(), rolledResonance.getId(), rolledVariant);
            }

            style = BladeStyle.byId(data.getSoulBladeStyle());
            element = BladeElement.byId(data.getSoulBladeElement());
            release = BladeRelease.byId(data.getSoulBladeRelease());
            resonance = BladeResonance.byId(data.getSoulBladeResonance());
            variant = data.getSoulBladeVariant();

            if (style == null) style = BladeStyle.FRENZY;
            if (element == null) element = BladeElement.IGNIS;
            if (release == null) release = BladeRelease.SLASH;
            if (resonance == null) resonance = BladeResonance.SPIRIT;
        } else {
            var random = player.getRandom();
            style = BladeStyle.values()[random.nextInt(BladeStyle.values().length)];
            element = BladeElement.ACTIVE_ELEMENTS[random.nextInt(BladeElement.ACTIVE_ELEMENTS.length)];
            release = BladeRelease.values()[random.nextInt(BladeRelease.values().length)];
            resonance = BladeResonance.values()[random.nextInt(BladeResonance.values().length)];
            variant = random.nextInt(2);
        }

        tag.putBoolean(TAG_INITIALIZED, true);
        tag.putString(TAG_OWNER_NAME, player.getScoreboardName());
        tag.putString(TAG_OWNER_UUID, player.getUUID().toString());
        tag.putString(TAG_STYLE, style.getId());
        tag.putString(TAG_ELEMENT, element.getId());
        tag.putString(TAG_RELEASE, release.getId());
        tag.putString(TAG_RESONANCE, resonance.getId());
        tag.putInt(TAG_STYLE_VARIANT, variant);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        syncAwakenedSpell(stack);
    }

    public static ItemStack createRandomExecutorSword(net.minecraft.util.RandomSource random, String ownerName) {
        ItemStack stack = new ItemStack(ModItems.SPIRITUAL_SWORD.get());
        BladeStyle style = BladeStyle.values()[random.nextInt(BladeStyle.values().length)];
        BladeElement element = BladeElement.ACTIVE_ELEMENTS[random.nextInt(BladeElement.ACTIVE_ELEMENTS.length)];
        BladeRelease release = BladeRelease.values()[random.nextInt(BladeRelease.values().length)];
        BladeResonance resonance = BladeResonance.values()[random.nextInt(BladeResonance.values().length)];
        int variant = random.nextInt(2);

        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_INITIALIZED, true);
        tag.putString(TAG_OWNER_NAME, ownerName);
        tag.putString(TAG_OWNER_UUID, UUID.randomUUID().toString());
        tag.putString(TAG_STYLE, style.getId());
        tag.putString(TAG_ELEMENT, element.getId());
        tag.putString(TAG_RELEASE, release.getId());
        tag.putString(TAG_RESONANCE, resonance.getId());
        tag.putInt(TAG_STYLE_VARIANT, variant);
        tag.putInt(TAG_AFFINITY, 100);
        tag.putBoolean(TAG_FIRST_RELEASE_UNLOCKED, true);
        tag.putBoolean(TAG_FIRST_RELEASE, true);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        setFirstRelease(stack, true);
        syncAwakenedSpell(stack);
        return stack;
    }

    public static String getOwnerUUID(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return (tag != null && tag.contains(TAG_OWNER_UUID)) ? tag.getString(TAG_OWNER_UUID) : "";
    }

    public static String getOwnerName(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return (tag != null && tag.contains(TAG_OWNER_NAME)) ? tag.getString(TAG_OWNER_NAME) : "";
    }

    public static BladeStyle getBladeStyle(ItemStack stack) {
        if (isSenbonzakura(stack)) return BladeStyle.PHANTOM;
        CompoundTag tag = getReadOnlyTag(stack);
        if (tag != null && tag.contains(TAG_STYLE)) {
            BladeStyle parsed = BladeStyle.byId(tag.getString(TAG_STYLE));
            if (parsed != null) return parsed;
        }
        return BladeStyle.FRENZY;
    }

    public static void setBladeStyle(ItemStack stack, BladeStyle style) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString(TAG_STYLE, style.getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (isFirstRelease(stack)) {
            int variant = getBladeVariant(stack);
            int cmd = style.getCustomModelData() + (variant == 1 ? 10 : 0);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(cmd));
        }
    }

    public static int getBladeVariant(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        return (tag != null && tag.contains(TAG_STYLE_VARIANT)) ? Math.max(0, tag.getInt(TAG_STYLE_VARIANT) % 2) : 0;
    }

    public static void setBladeVariant(ItemStack stack, int variant) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        int safeVariant = Math.max(0, variant % 2);
        tag.putInt(TAG_STYLE_VARIANT, safeVariant);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (isFirstRelease(stack)) {
            BladeStyle style = getBladeStyle(stack);
            int cmd = style.getCustomModelData() + (safeVariant == 1 ? 10 : 0);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(cmd));
        }
    }

    public static BladeElement getBladeElement(ItemStack stack) {
        if (isSenbonzakura(stack)) return BladeElement.NONE;
        CompoundTag tag = getReadOnlyTag(stack);
        if (tag != null && tag.contains(TAG_ELEMENT)) {
            BladeElement parsed = BladeElement.byId(tag.getString(TAG_ELEMENT));
            if (parsed != null) return parsed;
        }
        return BladeElement.IGNIS;
    }

    public static void setBladeElement(ItemStack stack, BladeElement element) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString(TAG_ELEMENT, element.getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static BladeRelease getBladeRelease(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        if (tag != null && tag.contains(TAG_RELEASE)) {
            BladeRelease parsed = BladeRelease.byId(tag.getString(TAG_RELEASE));
            if (parsed != null) return parsed;
        }
        return BladeRelease.SLASH;
    }

    public static void setBladeRelease(ItemStack stack, BladeRelease release) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString(TAG_RELEASE, release.getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static BladeResonance getBladeResonance(ItemStack stack) {
        CompoundTag tag = getReadOnlyTag(stack);
        if (tag != null && tag.contains(TAG_RESONANCE)) {
            BladeResonance parsed = BladeResonance.byId(tag.getString(TAG_RESONANCE));
            if (parsed != null) return parsed;
        }
        return BladeResonance.SPIRIT;
    }

    public static void setBladeResonance(ItemStack stack, BladeResonance resonance) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString(TAG_RESONANCE, resonance.getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ItemStack createCustomSword(ServerPlayer owner, BladeStyle style, BladeElement element, BladeRelease release, BladeResonance resonance) {
        return createCustomSword(owner, style, element, release, resonance, 0);
    }

    public static ItemStack createCustomSword(ServerPlayer owner, BladeStyle style, BladeElement element, BladeRelease release, BladeResonance resonance, int variant) {
        ItemStack stack = new ItemStack(ModItems.SPIRITUAL_SWORD.get());
        if (owner != null) {
            imprintSoulBlade(stack, owner);
        }
        setBladeStyle(stack, style);
        setBladeElement(stack, element);
        setBladeRelease(stack, release);
        setBladeResonance(stack, resonance);
        setBladeVariant(stack, variant);
        return stack;
    }

    // =========================================================================
    // BLOQUEO CLÁSICO (Clic Derecho) vs PRIMERA LIBERACIÓN (Agachado + Clic Derecho)
    // =========================================================================
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Bloqueo clásico con la espada (tanto normal como agachado)
        // Las técnicas liberadas ahora se lanzan a través de los controles y barra de hechizos del ISB
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);

        if (player != null && player.isShiftKeyDown() && (state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL) || state.is(Blocks.GRINDSTONE))) {
            if (!level.isClientSide()) {
                int curVariant = getBladeVariant(stack);
                int nextVariant = (curVariant == 0) ? 1 : 0;
                setBladeVariant(stack, nextVariant);

                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null && isSoulBound(stack)) {
                    data.setSoulBladeVariant(nextVariant);
                }

                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.4f);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 18, 0.3, 0.2, 0.3, 0.1);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
                }

                String variantName = (nextVariant == 0) ? "§bVariante I (Clásica)" : "§6Variante II (Alternativa)";
                player.displayClientMessage(Component.literal("§5✦ ¡Aspecto de tu Espada Espiritual Reforjado! ➔ " + variantName), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }

    public static int getAwakenedSpellLevel(ItemStack stack) {
        int aff = getAffinity(stack);
        if (aff >= 150) return 5;
        if (aff >= 100) return 4;
        if (aff >= 50) return 3;
        if (aff >= 25) return 2;
        return 1;
    }

    public static io.redspace.ironsspellbooks.api.spells.AbstractSpell getPrimaryAwakenedSpell(BladeRelease release) {
        if (release == null) return com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_SLASH.get();
        return switch (release) {
            case SLASH -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_SLASH.get();
            case VORTEX -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SOUL_VORTEX.get();
            case STEP -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SHADOW_STEP.get();
            case OVERDRIVE -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_OVERDRIVE.get();
            case JUDGEMENT -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.DIMENSIONAL_JUDGEMENT.get();
            case BARRAGE -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.THOUSAND_BLADES.get();
            case DOMINION -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRIT_DOMINION.get();
            case PIERCE -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.CELESTIAL_PIERCER.get();
        };
    }

    public static io.redspace.ironsspellbooks.api.spells.AbstractSpell getSecondaryAwakenedSpell(BladeRelease release) {
        if (release == null) return com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.DIMENSIONAL_JUDGEMENT.get();
        return switch (release) {
            case SLASH -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.DIMENSIONAL_JUDGEMENT.get();
            case VORTEX -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRIT_DOMINION.get();
            case STEP -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.CELESTIAL_PIERCER.get();
            case OVERDRIVE -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.THOUSAND_BLADES.get();
            case JUDGEMENT -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_SLASH.get();
            case BARRAGE -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_OVERDRIVE.get();
            case DOMINION -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SOUL_VORTEX.get();
            case PIERCE -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SHADOW_STEP.get();
        };
    }

    public static io.redspace.ironsspellbooks.api.spells.AbstractSpell getAwakenedSpell(BladeRelease release) {
        return getPrimaryAwakenedSpell(release);
    }

    private static void spawnElementalWavePoint(ServerLevel level, double x, double y, double z, BladeElement element) {
        switch (element) {
            case IGNIS -> {
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.25, 0.25, 0.25, 0.03);
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.2, 0.2, 0.2, 0.01);
            }
            case FULGUR -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 8, 0.3, 0.3, 0.3, 0.08);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.15, 0.15, 0.15, 0.02);
            }
            case GLACIES -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 6, 0.25, 0.25, 0.25, 0.03);
                level.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 4, 0.25, 0.25, 0.25, 0.02);
            }
            case UMBRA -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 6, 0.25, 0.25, 0.25, 0.03);
                level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
            }
            default -> level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void applyElementalStatusEffect(ServerLevel level, @org.jetbrains.annotations.Nullable LivingEntity attacker, LivingEntity target, BladeElement element, float hitDamage) {
        switch (element) {
            case IGNIS -> target.igniteForSeconds(5);
            case FULGUR -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, true));
                // Chain Lightning a hasta 3 enemigos cercanos (4.5 bloques)
                AABB chainArea = target.getBoundingBox().inflate(4.5);
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, chainArea,
                        e -> e != target && isValidCombatTarget(attacker, e));
                int jumps = 0;
                DamageSource source = (attacker != null) ? attacker.damageSources().magic() : level.damageSources().magic();
                for (LivingEntity chained : nearby) {
                    if (jumps >= 3) break;
                    chained.hurt(source, Math.max(1.5f, hitDamage * 0.35f));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, chained.getX(), chained.getY() + 0.8, chained.getZ(), 12, 0.25, 0.35, 0.25, 0.1);
                    jumps++;
                }
            }
            case GLACIES -> {
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 90));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
            }
            case UMBRA -> {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, true));
                if (attacker != null && attacker.getHealth() < attacker.getMaxHealth()) {
                    attacker.heal(Math.min(4.0f, hitDamage * 0.12f));
                }
            }
            default -> {}
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        boolean soulBound = isSoulBound(stack);
        boolean firstRelease = isFirstRelease(stack);

        if (isSenbonzakura(stack)) {
            return Component.literal("✦ Senbonzakura ✦").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        }

        if (stack.has(DataComponents.CUSTOM_NAME)) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            String nameStr = custom != null ? custom.getString() : "";
            if (soulBound) {
                // Conserva el nombre del yunque pero cambia el color a Morado
                return Component.literal(nameStr).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
            } else {
                return Component.literal(nameStr).withStyle(ChatFormatting.WHITE);
            }
        }

        if (firstRelease) {
            BladeStyle style = getBladeStyle(stack);
            BladeElement element = getBladeElement(stack);
            return Component.translatable("item.tenseicraft.spiritual_sword")
                    .withStyle(element.getColor(), ChatFormatting.BOLD)
                    .append(Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(style.getTranslationKey()).withStyle(style.getColor()))
                    .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (soulBound) {
            return Component.translatable("item.tenseicraft.spiritual_sword")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
        }

        return Component.translatable("item.tenseicraft.spiritual_sword_base")
                .withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        syncAwakenedSpell(stack);
    }

    public static boolean syncAwakenedSpell(ItemStack stack) {
        return syncAwakenedSpell(stack, !isDenseSoulBlade(stack));
    }

    public static boolean syncAwakenedSpell(ItemStack stack, boolean allowResonance) {
        int maxSlots = allowResonance ? 4 : 3;
        // Regla: Las técnicas de la Espada Espiritual SOLO aparecen en la interfaz/rueda del ISB al tener la espada liberada (Primera Liberación activa).
        if (!isFirstRelease(stack)) {
            ISpellContainer cur = ISpellContainer.get(stack);
            if (cur == null || !cur.isEmpty() || !cur.isSpellWheel() || cur.mustEquip() || cur.getMaxSpellCount() != maxSlots) {
                ISpellContainer.set(stack, ISpellContainer.create(maxSlots, true, false));
                return true;
            }
            return false;
        }

        boolean senbon = isSenbonzakura(stack);
        BladeRelease release = getBladeRelease(stack);
        io.redspace.ironsspellbooks.api.spells.AbstractSpell primarySpell = senbon
                ? com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SENBONZAKURA_CHIRE.get()
                : getPrimaryAwakenedSpell(release);
        if (primarySpell == null) return false;

        int affinity = getAffinity(stack);
        int targetLevel = getAwakenedSpellLevel(stack);
        boolean hasSecondary = senbon || (affinity >= 150);
        boolean hasTertiary = affinity >= 200;
        boolean hasQuaternary = allowResonance && (affinity >= 200);
        io.redspace.ironsspellbooks.api.spells.AbstractSpell secondarySpell = hasSecondary
                ? (senbon ? com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SENBONZAKURA_VORTEX.get() : getSecondaryAwakenedSpell(release))
                : null;
        io.redspace.ironsspellbooks.api.spells.AbstractSpell tertiarySpell = hasTertiary ? com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_BEAM.get() : null;
        io.redspace.ironsspellbooks.api.spells.AbstractSpell quaternarySpell = hasQuaternary ? com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_RESONANCE.get() : null;

        ISpellContainer container = ISpellContainer.get(stack);
        boolean needsUpdate = true;
        if (container != null && container.getMaxSpellCount() == maxSlots && container.isSpellWheel() && !container.mustEquip()) {
            io.redspace.ironsspellbooks.api.spells.SpellData slot0 = container.getSpellAtIndex(0);
            io.redspace.ironsspellbooks.api.spells.SpellData slot1 = container.getSpellAtIndex(1);
            io.redspace.ironsspellbooks.api.spells.SpellData slot2 = container.getSpellAtIndex(2);
            io.redspace.ironsspellbooks.api.spells.SpellData slot3 = (maxSlots >= 4)
                    ? container.getSpellAtIndex(3)
                    : io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY;

            boolean slot0Ok = (slot0 != null && slot0 != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                    && slot0.getSpell() == primarySpell && slot0.getLevel() == targetLevel && slot0.isLocked());
            boolean slot1Ok = hasSecondary
                    ? (slot1 != null && slot1 != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                            && slot1.getSpell() == secondarySpell && slot1.getLevel() == targetLevel && slot1.isLocked())
                    : (slot1 == null || slot1 == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY);
            boolean slot2Ok = hasTertiary
                    ? (slot2 != null && slot2 != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                            && slot2.getSpell() == tertiarySpell && slot2.getLevel() == targetLevel && slot2.isLocked())
                    : (slot2 == null || slot2 == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY);
            boolean slot3Ok = !allowResonance || (hasQuaternary
                    ? (slot3 != null && slot3 != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                            && slot3.getSpell() == quaternarySpell && slot3.getLevel() == targetLevel && slot3.isLocked())
                    : (slot3 == null || slot3 == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY));

            if (slot0Ok && slot1Ok && slot2Ok && slot3Ok) {
                needsUpdate = false;
            }
        }

        if (needsUpdate) {
            // maxSpells=4 (o 3 en almas densas sin Resonancia Espiritual), spellWheel=true, mustEquip=false
            // Slot 0: Técnica de liberación principal
            // Slot 1: Segunda técnica suprema desbloqueada al alcanzar 150 de Afinidad
            // Slot 2: Tercera técnica universal (Spiritual Beam) desbloqueada al alcanzar 200 de Afinidad
            // Slot 3: Cuarta técnica universal suprema (Spiritual Resonance) solo en almas Menor/Moderada (200 Afinidad)
            ISpellContainer newContainer = ISpellContainer.create(maxSlots, true, false);
            io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable mutable = newContainer.mutableCopy();
            mutable.addSpellAtIndex(primarySpell, targetLevel, 0, true);
            if (hasSecondary && secondarySpell != null) {
                mutable.addSpellAtIndex(secondarySpell, targetLevel, 1, true);
            }
            if (hasTertiary && tertiarySpell != null) {
                mutable.addSpellAtIndex(tertiarySpell, targetLevel, 2, true);
            }
            if (hasQuaternary && quaternarySpell != null && maxSlots >= 4) {
                mutable.addSpellAtIndex(quaternarySpell, targetLevel, 3, true);
            }
            ISpellContainer.set(stack, mutable.toImmutable());
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (!(entity instanceof LivingEntity living)) return;

        boolean denseSoul = (entity instanceof ServerPlayer p
                && p.getData(ModAttachments.PLAYER_RPG_DATA) != null
                && p.getData(ModAttachments.PLAYER_RPG_DATA).isDenseSoul());
        boolean denseFlagChanged = false;
        if (entity instanceof ServerPlayer && isSoulBound(stack) && isDenseSoulBlade(stack) != denseSoul) {
            setDenseSoulBlade(stack, denseSoul);
            denseFlagChanged = true;
        }
        boolean allowResonance = !denseSoul;

        // Optimización: Solo sincronizar el contenedor de hechizos si aún no está inicializado, si cambió DenseSoulBlade o periódicamente cada 20 ticks (1s)
        if (denseFlagChanged || ISpellContainer.get(stack) == null || entity.tickCount % 20 == 0) {
            boolean updated = syncAwakenedSpell(stack, allowResonance);
            if ((updated || denseFlagChanged) && entity instanceof ServerPlayer serverPlayer) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                );
            }
        }

        // En una misma partida el jugador solo puede tener espadas con sus habilidades únicas de alma (a excepción de Senbonzakura).
        // Optimización: Verificar rasgos de NBT y mutación solo cada 20 ticks (1s) en lugar de cada tick.
        if (!isSenbonzakura(stack) && entity.tickCount % 20 == 0 && entity instanceof ServerPlayer serverPlayer) {
            PlayerRpgData data = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null && data.hasRolledSoulBlade()) {
                String ownerUuid = getOwnerUUID(stack);
                if (ownerUuid.isEmpty() || ownerUuid.equals(serverPlayer.getUUID().toString())) {
                    BladeStyle soulStyle = BladeStyle.byId(data.getSoulBladeStyle());
                    BladeElement soulElement = BladeElement.byId(data.getSoulBladeElement());
                    BladeRelease soulRelease = BladeRelease.byId(data.getSoulBladeRelease());
                    BladeResonance soulResonance = BladeResonance.byId(data.getSoulBladeResonance());

                    if (soulStyle != null && soulElement != null && soulRelease != null && soulResonance != null) {
                        BladeStyle curStyle = getBladeStyle(stack);
                        BladeElement curElem = getBladeElement(stack);
                        BladeRelease curRel = getBladeRelease(stack);
                        BladeResonance curRes = getBladeResonance(stack);

                        if (curStyle != soulStyle || curElem != soulElement || curRel != soulRelease || curRes != soulResonance) {
                            setBladeStyle(stack, soulStyle);
                            setBladeElement(stack, soulElement);
                            setBladeRelease(stack, soulRelease);
                            setBladeResonance(stack, soulResonance);
                            syncAwakenedSpell(stack, allowResonance);
                            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                                    serverPlayer,
                                    new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                            );
                        }
                    }
                }
            }
        }

        // Las Espadas Espirituales (vírgenes y vinculadas) son indestructibles: limpiar daño previo sin consumir maná
        if (stack.getDamageValue() > 0) {
            stack.setDamageValue(0);
        }

        if (living instanceof ServerPlayer sp && isSoulBound(stack)) {
            PlayerRpgData denseCheck = sp.getData(ModAttachments.PLAYER_RPG_DATA);
            if (denseCheck != null && denseCheck.isDenseSoul()) {
                if (!isFirstRelease(stack)) {
                    setFirstRelease(stack, true);
                    syncAwakenedSpell(stack, false);
                    com.pablo.tenseicraft.events.ModAttributesHandler.updatePlayerAttributes(sp);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            sp,
                            new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                    );
                }
                // Almas densas (Alta/Irregular): la espada permanece siempre liberada visualmente
                // y con sus técnicas activas, pero SIN saturación, SIN +50% y SIN drenaje de maná.
                return;
            }
        }

        boolean heldInOffhand = living.getOffhandItem() == stack;
        if (!isSelected && !heldInOffhand) {
            if (isFirstRelease(stack)) {
                setFirstRelease(stack, false);
                syncAwakenedSpell(stack);
                if (living instanceof ServerPlayer sp) {
                    com.pablo.tenseicraft.events.ModAttributesHandler.updatePlayerAttributes(sp);
                    ModNetworking.syncToPlayer(sp);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            sp,
                            new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                    );
                    sp.displayClientMessage(Component.literal("§7Has enfundado la Espada Espiritual. La liberación se disipó."), true);
                }
            }
            return;
        }

        // Si la hoja aún no ha despertado su Primera Liberación, solo emite destellos sutiles si está vinculada
        if (!isFirstRelease(stack)) {
            if (isSenbonzakura(stack)) {
                if (entity.tickCount % 20 == 0 && living instanceof ServerPlayer serverPlayer) {
                    serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 1, 0.2, 0.3, 0.2, 0.02);
                }
                CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
                if (cmd == null || cmd.value() != 4) {
                    stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(4));
                }
            } else if (isSoulBound(stack) && entity.tickCount % 30 == 0 && living instanceof ServerPlayer serverPlayer) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 2, 0.2, 0.3, 0.2, 0.05);
            }
            return;
        }

        // =========================================================================
        // PRIMERA LIBERACIÓN ACTIVA:
        // 1. Saturación infinita para regenerar vida continuamente (aplicada cada 20 ticks para evitar spam de paquetes de efectos)
        // 2. Drenaje continuo de Maná espiritual (alto en afinidad 100, mínimo en 200)
        // 3. Desactivación automática al agotarse el Maná
        // =========================================================================
        if (entity.tickCount % 20 == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.SATURATION, 45, 0, false, false, true));
        }

        if (living instanceof ServerPlayer serverPlayer) {
            PlayerRpgData data = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                // Supresión absoluta de regeneración: el maná jamás puede subir por encima del tope fijado
                float curMana = data.getCurrentMana(serverPlayer);
                float cap = data.getLastReleasedManaCap();
                if (cap >= 0.0f && curMana > cap) {
                    data.setCurrentMana(serverPlayer, cap);
                } else if (cap < 0.0f) {
                    data.setLastReleasedManaCap(curMana);
                }

                if (entity.tickCount % 10 == 0) {
                    int aff = getAffinity(stack);
                    float progress = Math.min(1.0f, Math.max(0.0f, (aff - 100) / 100.0f));
                    // Escala de drenaje bestial:
                    // Afinidad 100: 8.0 MP/s -> 4.0 MP cada 10 ticks
                    // Afinidad 200 (Dominada): 1.5 MP/s -> 0.75 MP cada 10 ticks
                    float drainPerSecond = 8.0f - (progress * 6.5f);
                    float drain = drainPerSecond * 0.5f;

                    if (curMana <= 0.5f || !data.consumeMana(serverPlayer, drain)) {
                        setFirstRelease(stack, false);
                        syncAwakenedSpell(stack);
                        data.setLastReleasedManaCap(-1.0f);
                        com.pablo.tenseicraft.events.ModAttributesHandler.updatePlayerAttributes(serverPlayer);
                        ModNetworking.syncToPlayer(serverPlayer);
                        ModNetworking.syncMana(serverPlayer);
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                                serverPlayer,
                                new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                        );

                        serverPlayer.displayClientMessage(
                                Component.literal("§c✦ Tu energía espiritual se ha agotado. La Primera Liberación se disipa..."),
                                true
                        );
                        serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                        return;
                    } else {
                        data.setLastReleasedManaCap(data.getCurrentMana(serverPlayer));
                        ModNetworking.syncMana(serverPlayer);
                    }
                }
            }
        }

        boolean senbon = isSenbonzakura(stack);
        BladeElement element = getBladeElement(stack);
        BladeStyle style = getBladeStyle(stack);

        // 1. Arco de partículas elementales al dar un tajo al aire o en combate + Onda de Filo Fantasma
        if (isSelected && living.swinging && living.swingTime == 1) {
            if (senbon) {
                spawnSenbonzakuraSlashArc(serverLevel, living);
            } else if (element != BladeElement.NONE) {
                spawnElementalSlashArc(serverLevel, living, element);
            }
            // Si el estilo es FILO FANTASMA (Katana), proyecta una onda de corte a 4.8 bloques
            if (style == BladeStyle.PHANTOM && living instanceof ServerPlayer serverPlayer && !isTemporarilyExhausted(stack)) {
                Vec3 look = serverPlayer.getLookAngle().normalize();
                Vec3 wavePos = serverPlayer.getEyePosition().add(look.scale(3.8));
                if (senbon) {
                    serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, wavePos.x, wavePos.y - 0.2, wavePos.z, 8, 0.2, 0.2, 0.2, 0.05);
                }
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, wavePos.x, wavePos.y - 0.2, wavePos.z, 1, 0.1, 0.1, 0.1, 0.0);
                AABB waveBox = new AABB(wavePos.x - 1.1, wavePos.y - 1.0, wavePos.z - 1.1, wavePos.x + 1.1, wavePos.y + 1.0, wavePos.z + 1.1);
                for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, waveBox,
                        e -> isValidCombatTarget(serverPlayer, e) && e.distanceToSqr(serverPlayer) > 6.25)) {
                    if (target.hurt(serverPlayer.damageSources().playerAttack(serverPlayer), 4.0f)) {
                        if (senbon) {
                            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + 0.6, target.getZ(), 15, 0.3, 0.4, 0.3, 0.08);
                        } else {
                            spawnElementalImpactBurst(serverLevel, target, element);
                            applyElementalStatusEffect(serverLevel, serverPlayer, target, element, 4.0f);
                        }
                    }
                }
            }
        }

        // 2. Aura pasiva de partículas alrededor de la hoja cada 4 ticks (o cada 2 ticks en Overdrive)
        boolean overdrive = living.getPersistentData().getLong("RpgSwordOverdriveUntil") > level.getGameTime();
        if (entity.tickCount % (overdrive ? 2 : 4) == 0) {
            if (senbon) {
                spawnSenbonzakuraHeldAura(serverLevel, living);
            } else if (element != BladeElement.NONE) {
                spawnHeldElementalAura(serverLevel, living, element);
            }
        }

        // 3. Pulso elemental de Trance Espiritual (Overdrive) cada segundo alrededor del portador
        if (overdrive && entity.tickCount % 20 == 0 && living instanceof ServerPlayer serverPlayer) {
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle);
                spawnElementalWavePoint(serverLevel, serverPlayer.getX() + Math.cos(rad) * 2.5, serverPlayer.getY() + 0.3, serverPlayer.getZ() + Math.sin(rad) * 2.5, element);
            }
            for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, serverPlayer.getBoundingBox().inflate(3.5),
                    e -> isValidCombatTarget(serverPlayer, e))) {
                switch (element) {
                    case IGNIS -> nearby.igniteForSeconds(4);
                    case FULGUR -> {
                        nearby.hurt(serverPlayer.damageSources().magic(), 2.0f);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, nearby.getX(), nearby.getY() + 0.8, nearby.getZ(), 8, 0.2, 0.3, 0.2, 0.08);
                    }
                    case GLACIES -> {
                        nearby.setTicksFrozen(nearby.getTicksFrozen() + 60);
                        nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
                    }
                    case UMBRA -> {
                        nearby.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, true));
                        if (nearby.hurt(serverPlayer.damageSources().magic(), 1.5f)) {
                            serverPlayer.heal(0.75f);
                        }
                    }
                    default -> {}
                }
            }
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!target.level().isClientSide() && target.level() instanceof ServerLevel serverLevel) {
            BladeElement element = getBladeElement(stack);
            boolean soulBound = isSoulBound(stack);
            boolean released = isFirstRelease(stack);
            int affinity = getAffinity(stack);

            // Progresión de Afinidad con 10% de probabilidad por golpe a mobs vivos (hasta 100 base, o hasta 200 si la liberación fue desbloqueada)
            int maxAff = getMaxAffinity(stack);
            if (soulBound && affinity < maxAff && attacker instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.getRandom().nextFloat() < 0.10f) {
                    int newAffinity = affinity + 1;
                    setAffinity(stack, newAffinity);
                    affinity = newAffinity;

                    if (newAffinity == 100 && !isFirstReleaseUnlocked(stack)) {
                        serverPlayer.sendSystemMessage(Component.empty());
                        serverPlayer.sendSystemMessage(Component.literal("§d✦ §l[ESPADA ESPIRITUAL] §f¡Tu hoja emana una inmensa concentración de energía espiritual!"));
                        serverPlayer.sendSystemMessage(Component.literal("§eHas alcanzado el 100% de Afinidad. El poder elemental se ha manifestado por completo."));
                        serverPlayer.sendSystemMessage(Component.literal("§bConsulta con Alice para descubrir el camino hacia la Primera Liberación."));
                        serverPlayer.sendSystemMessage(Component.empty());

                        serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                        serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 40, 0.4, 0.6, 0.4, 0.15);
                    } else if (newAffinity == 150) {
                        serverPlayer.sendSystemMessage(Component.empty());
                        serverPlayer.sendSystemMessage(Component.literal("§6✦ §l[ESPADA ESPIRITUAL] §f¡Has alcanzado §e150 de Afinidad §fcon tu arma!"));
                        serverPlayer.sendSystemMessage(Component.literal("§e¡Se ha desbloqueado la §6Segunda Técnica Suprema §een tu hoja liberada!"));
                        serverPlayer.sendSystemMessage(Component.empty());

                        serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.4f);
                        serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.8f);
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 45, 0.4, 0.6, 0.4, 0.15);
                    } else if (newAffinity == 200) {
                        serverPlayer.sendSystemMessage(Component.empty());
                        serverPlayer.sendSystemMessage(Component.literal("§d✦ §l[ESPADA ESPIRITUAL] §6¡Has dominado por completo tu Primera Liberación!"));
                        serverPlayer.sendSystemMessage(Component.literal("§eAfinidad Máxima (200/200) alcanzada. El drenaje espiritual ahora es mínimo (0.8 MP/s)."));
                        serverPlayer.sendSystemMessage(Component.empty());

                        com.pablo.tenseicraft.advancements.ModAdvancements.award(serverPlayer, com.pablo.tenseicraft.advancements.ModAdvancements.MASTER_SWORD);

                        serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 50, 0.5, 0.8, 0.5, 0.2);
                    }

                    syncAwakenedSpell(stack);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            serverPlayer,
                            new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                    );
                }
            }

            // Solo se activa el elemento en golpe si tiene afinidad al máximo (100) o está liberada
            if (isSenbonzakura(stack)) {
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 1.2F, 1.3F);
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.4, 0.4, 0.4, 0.08);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0, 0, 0, 0);

                // Ataque en área característico: Pétalos cortantes a todos los enemigos en 4.0 bloques
                AABB cleaveArea = target.getBoundingBox().inflate(4.0, 1.5, 4.0);
                float cleaveDamage = 4.0f + (affinity * 0.04f);
                if (attacker instanceof Player p) {
                    PlayerRpgData pData = p.getData(ModAttachments.PLAYER_RPG_DATA);
                    if (pData != null) {
                        int sp = pData.getTotalStatPoints(p, StatType.SPIRIT);
                        int dx = pData.getTotalStatPoints(p, StatType.DEXTERITY);
                        cleaveDamage += (float) (sp * 0.35 + dx * 0.20);
                    }
                }
                List<LivingEntity> nearbyTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, cleaveArea,
                        e -> e != target && e != attacker && isValidCombatTarget(attacker, e));
                for (LivingEntity nearby : nearbyTargets) {
                    DamageSource src = (attacker instanceof Player p) ? attacker.damageSources().playerAttack(p) : serverLevel.damageSources().magic();
                    nearby.hurt(src, cleaveDamage);
                    nearby.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
                    serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, nearby.getX(), nearby.getY() + nearby.getBbHeight() * 0.5, nearby.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
            } else if (element != BladeElement.NONE && (affinity >= 100 || released)) {
                spawnElementalImpactBurst(serverLevel, target, element);
                if (attacker instanceof ServerPlayer serverPlayer) {
                    applyElementalStatusEffect(serverLevel, serverPlayer, target, element, 4.0f);
                }
            }
        }
        return result;
    }

    public static void spawnSenbonzakuraHeldAura(ServerLevel level, LivingEntity wielder) {
        Vec3 look = wielder.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        double bx = wielder.getX() + look.x * 0.65 + right.x * 0.38;
        double by = wielder.getY() + wielder.getEyeHeight() - 0.35 + look.y * 0.45;
        double bz = wielder.getZ() + look.z * 0.65 + right.z * 0.38;

        level.sendParticles(ParticleTypes.CHERRY_LEAVES, bx, by, bz, 2, 0.12, 0.18, 0.12, 0.02);
        if (wielder.getRandom().nextFloat() < 0.25f) {
            level.sendParticles(ParticleTypes.CRIT, bx, by, bz, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    public static void spawnSenbonzakuraSlashArc(ServerLevel level, LivingEntity wielder) {
        Vec3 look = wielder.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }

        double centerX = wielder.getX() + look.x * 1.45;
        double centerY = wielder.getY() + wielder.getEyeHeight() - 0.2 + look.y * 1.1;
        double centerZ = wielder.getZ() + look.z * 1.45;

        for (int i = -3; i <= 3; i++) {
            double offset = i * 0.32;
            double curveBack = -Math.abs(i) * 0.08;
            double px = centerX + right.x * offset + look.x * curveBack;
            double py = centerY - i * 0.06;
            double pz = centerZ + right.z * offset + look.z * curveBack;

            level.sendParticles(ParticleTypes.CHERRY_LEAVES, px, py, pz, 3, 0.08, 0.08, 0.08, 0.03);
        }
    }

    public static void spawnHeldElementalAura(ServerLevel level, LivingEntity wielder, BladeElement element) {
        Vec3 look = wielder.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        double bx = wielder.getX() + look.x * 0.65 + right.x * 0.38;
        double by = wielder.getY() + wielder.getEyeHeight() - 0.35 + look.y * 0.45;
        double bz = wielder.getZ() + look.z * 0.65 + right.z * 0.38;

        switch (element) {
            case IGNIS -> {
                level.sendParticles(ParticleTypes.FLAME, bx, by, bz, 2, 0.12, 0.18, 0.12, 0.01);
                if (wielder.getRandom().nextFloat() < 0.35f) {
                    level.sendParticles(ParticleTypes.SMOKE, bx, by + 0.1, bz, 1, 0.08, 0.1, 0.08, 0.01);
                }
            }
            case FULGUR -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, bx, by, bz, 3, 0.16, 0.22, 0.16, 0.04);
                if (wielder.getRandom().nextFloat() < 0.3f) {
                    level.sendParticles(ParticleTypes.END_ROD, bx, by, bz, 1, 0.08, 0.12, 0.08, 0.01);
                }
            }
            case GLACIES -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, bx, by, bz, 2, 0.15, 0.2, 0.15, 0.01);
                level.sendParticles(ParticleTypes.WHITE_ASH, bx, by, bz, 2, 0.18, 0.2, 0.18, 0.01);
            }
            case UMBRA -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, bx, by, bz, 2, 0.12, 0.18, 0.12, 0.01);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, bx, by, bz, 3, 0.15, 0.2, 0.15, 0.02);
                if (wielder.getRandom().nextFloat() < 0.35f) {
                    level.sendParticles(ParticleTypes.SCULK_SOUL, bx, by, bz, 1, 0.1, 0.15, 0.1, 0.01);
                }
            }
            default -> {}
        }
    }

    public static void spawnElementalSlashArc(ServerLevel level, LivingEntity wielder, BladeElement element) {
        Vec3 look = wielder.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }

        double centerX = wielder.getX() + look.x * 1.45;
        double centerY = wielder.getY() + wielder.getEyeHeight() - 0.2 + look.y * 1.1;
        double centerZ = wielder.getZ() + look.z * 1.45;

        for (int i = -3; i <= 3; i++) {
            double offset = i * 0.32;
            double curveBack = -Math.abs(i) * 0.08;
            double px = centerX + right.x * offset + look.x * curveBack;
            double py = centerY - i * 0.06;
            double pz = centerZ + right.z * offset + look.z * curveBack;

            switch (element) {
                case IGNIS -> {
                    level.sendParticles(ParticleTypes.FLAME, px, py, pz, 3, 0.06, 0.06, 0.06, 0.02);
                    if (i == 0) {
                        level.sendParticles(ParticleTypes.LAVA, px, py, pz, 2, 0.1, 0.1, 0.1, 0.0);
                    }
                }
                case FULGUR -> {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 4, 0.08, 0.08, 0.08, 0.08);
                    level.sendParticles(ParticleTypes.WAX_OFF, px, py, pz, 1, 0.05, 0.05, 0.05, 0.02);
                }
                case GLACIES -> {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 3, 0.06, 0.06, 0.06, 0.02);
                    level.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.04, 0.04, 0.04, 0.01);
                }
                case UMBRA -> {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 3, 0.06, 0.06, 0.06, 0.02);
                    level.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 1, 0.04, 0.04, 0.04, 0.01);
                    if (i % 2 == 0) {
                        level.sendParticles(ParticleTypes.SCULK_SOUL, px, py, pz, 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }
                default -> {}
            }
        }
    }

    public static void spawnElementalImpactBurst(ServerLevel level, LivingEntity target, BladeElement element) {
        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() * 0.55;
        double tz = target.getZ();

        switch (element) {
            case IGNIS -> {
                level.sendParticles(ParticleTypes.FLAME, tx, ty, tz, 22, 0.35, 0.45, 0.35, 0.07);
                level.sendParticles(ParticleTypes.LAVA, tx, ty, tz, 6, 0.25, 0.3, 0.25, 0.0);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, tx, ty, tz, 8, 0.3, 0.4, 0.3, 0.03);
                level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
            }
            case FULGUR -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tx, ty, tz, 30, 0.4, 0.55, 0.4, 0.15);
                level.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 10, 0.3, 0.4, 0.3, 0.08);
                level.sendParticles(ParticleTypes.WAX_OFF, tx, ty, tz, 12, 0.35, 0.45, 0.35, 0.1);
                level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.35F, 1.8F);
            }
            case GLACIES -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, tx, ty, tz, 25, 0.4, 0.5, 0.4, 0.06);
                level.sendParticles(ParticleTypes.WHITE_ASH, tx, ty, tz, 18, 0.4, 0.5, 0.4, 0.05);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, tx, ty, tz, 12, 0.3, 0.4, 0.3, 0.08);
                level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.5F);
            }
            case UMBRA -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tx, ty, tz, 22, 0.35, 0.45, 0.35, 0.06);
                level.sendParticles(ParticleTypes.SCULK_SOUL, tx, ty, tz, 12, 0.35, 0.45, 0.35, 0.05);
                level.sendParticles(ParticleTypes.SQUID_INK, tx, ty, tz, 10, 0.3, 0.4, 0.3, 0.04);
                level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.6F, 1.4F);
            }
            default -> {}
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return itemAbility == net.neoforged.neoforge.common.ItemAbilities.SHIELD_BLOCK
                || super.canPerformAction(stack, itemAbility);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        boolean soulBound = isSoulBound(stack);
        boolean firstRelease = isFirstRelease(stack);
        String ownerName = getOwnerName(stack);

        tooltipComponents.add(Component.empty());

        if (isSenbonzakura(stack)) {
            tooltipComponents.add(Component.literal("§d✦ Zanpakuto: Senbonzakura (Byakuya Kuchiki) ✦").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            tooltipComponents.add(Component.literal("§7« Dispersa... Senbonzakura »").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            tooltipComponents.add(Component.literal("§f• Hoja Desintegrada: §dTodos los ataques y habilidades son en Área.").withStyle(ChatFormatting.WHITE));
            tooltipComponents.add(Component.literal("§f• Elemento: §bNeutro §7(Reiryoku puro sin elemento)").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("§f• Pétalos Cortantes: §dLos tajos normales dañan en área a enemigos cercanos.").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltipComponents.add(Component.empty());
        }

        if (!soulBound) {
            tooltipComponents.add(Component.literal("§7[Espada Espiritual Base - Sin Vincular]").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltipComponents.add(Component.literal("§e1. Grábale un nombre en un yunque.").withStyle(ChatFormatting.YELLOW));
            tooltipComponents.add(Component.literal("§d2. Canaliza magia con al menos 200 de Maná.").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltipComponents.add(Component.literal("§7El vínculo maestro-arma forjará su esencia espiritual.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("§7Durabilidad: §bIrrompible §8(Indestructible)").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("§8(Inmune a encantamientos de mesa y libros)").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltipComponents.add(Component.literal("§5✦ Vínculo Maestro-Arma Forjado ✦").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        if (!ownerName.isEmpty()) {
            tooltipComponents.add(Component.literal("§7Portador de Alma: §d" + ownerName));
        }
        tooltipComponents.add(Component.literal("§7Durabilidad: §bIrrompible §8(Indestructible)").withStyle(ChatFormatting.GRAY));

        int affinity = getAffinity(stack);
        int maxAffinity = getMaxAffinity(stack);
        boolean trialReady = isTrialReady(stack);
        boolean trialPassed = isFirstReleaseUnlocked(stack);
        boolean denseSoulBlade = isDenseSoulBlade(stack);

        int filledBlocks = Math.min(10, Math.max(0, (affinity * 10) / maxAffinity));
        String bar = "§a" + "■".repeat(filledBlocks) + "§8" + "■".repeat(10 - filledBlocks);
        String affTag = (affinity >= 200) ? " §6[Dominada]" : "";
        tooltipComponents.add(Component.literal("§6Afinidad Espiritual: §e" + affinity + "/" + maxAffinity + affTag + " §8[" + bar + "§8]"));

        if (denseSoulBlade && firstRelease) {
            tooltipComponents.add(Component.literal("§f✦ Forma Liberada Constante §8(Sin bono +50%, sin Saturación ni drenaje de maná)"));
        } else if (!trialPassed) {
            if (affinity < 100) {
                tooltipComponents.add(Component.literal("§7(Requiere 100 de afinidad para despertar sus efectos elementales)").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                tooltipComponents.add(Component.literal("§b✦ Afinidad Despierta: Habla con Alice para desafiar al Espíritu de tu Espada.").withStyle(ChatFormatting.AQUA));
            }
        } else {
            float progress = Math.min(1.0f, Math.max(0.0f, (affinity - 100) / 100.0f));
            float activationCost = 80.0f - (progress * 55.0f);
            float drainRate = 8.0f - (progress * 6.5f);
            if (firstRelease) {
                tooltipComponents.add(Component.literal("§a✦ Primera Liberación: §a[ACTIVA]"));
                tooltipComponents.add(Component.literal("§6• Poder General: §e+50% a Daño y Atributos RPG").withStyle(ChatFormatting.GOLD));
                tooltipComponents.add(Component.literal("§7• Drenaje Espiritual: §c-" + String.format(java.util.Locale.ROOT, "%.1f", drainRate) + " MP/s §8(Sin regeneración)").withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(Component.literal("§7• Saturación Continua: §6Activa §8(Regenera vida continuamente)").withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(Component.literal("§7• Hechizos Espirituales: §bCoste 0 MP §8(Solo cooldown)").withStyle(ChatFormatting.GRAY));
            } else {
                tooltipComponents.add(Component.literal("§a✦ Primera Liberación: §7[INACTIVA] §8([Z] para activar)"));
                tooltipComponents.add(Component.literal("§7• Coste de Activación: §e" + (int) Math.ceil(activationCost) + " MP §8(Mín. 25 MP en 200/200)").withStyle(ChatFormatting.DARK_GRAY));
                tooltipComponents.add(Component.literal("§7• Drenaje proyectado: §e" + String.format(java.util.Locale.ROOT, "%.1f", drainRate) + " MP/s §8(Mín. 1.5 MP/s en 200/200)").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (!firstRelease) {
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("§c[Primera Liberación Inactiva]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            if (!trialPassed) {
                tooltipComponents.add(Component.literal("§8Supera la prueba del Espíritu de tu Espada para despertarla.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                tooltipComponents.add(Component.literal("§8Presiona [Z] para desatar su forma liberada y activar sus hechizos en el HUD.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("§8(Inmune a encantamientos)").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        // Si la Primera Liberación está despierta:
        BladeStyle style = getBladeStyle(stack);
        BladeElement element = getBladeElement(stack);
        BladeRelease release = getBladeRelease(stack);
        BladeResonance resonance = getBladeResonance(stack);

        // 1. Estilo de Combate
        tooltipComponents.add(Component.translatable("tenseicraft.spiritual_sword.style_label")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": "))
                .append(Component.translatable(style.getTranslationKey()).withStyle(style.getColor(), ChatFormatting.BOLD)));
        tooltipComponents.add(Component.translatable(style.getDescriptionKey()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        int variant = getBladeVariant(stack);
        String variantStr = (variant == 1) ? "§6Variante II (Alternativa)" : "§bVariante I (Clásica)";
        tooltipComponents.add(Component.literal("§7• Aspecto de Hoja: ").append(Component.literal(variantStr)));

        // 2. Elemento Espiritual
        tooltipComponents.add(Component.translatable("tenseicraft.spiritual_sword.element_label")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": "))
                .append(Component.translatable(element.getTranslationKey()).withStyle(element.getColor(), ChatFormatting.BOLD)));
        tooltipComponents.add(Component.translatable(element.getDescriptionKey()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        // 3. Resonancia del Alma
        tooltipComponents.add(Component.translatable("tenseicraft.spiritual_sword.resonance_label")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": "))
                .append(Component.translatable(resonance.getTranslationKey()).withStyle(resonance.getColor(), ChatFormatting.BOLD)));
        tooltipComponents.add(Component.translatable(resonance.getDescriptionKey()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        // 4. Técnicas de la Hoja Liberada (ISB HUD)
        if (isSenbonzakura(stack)) {
            tooltipComponents.add(Component.literal("§d✦ Técnica Shikai I: ")
                    .append(com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SENBONZAKURA_CHIRE.get().getDisplayName(null).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                    .append(Component.literal(" §7[HUD ISB Ranura 1]")));

            tooltipComponents.add(Component.literal("§d✦ Técnica Shikai II: ")
                    .append(com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SENBONZAKURA_VORTEX.get().getDisplayName(null).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                    .append(Component.literal(" §7[HUD ISB Ranura 2]")));

            if (affinity >= 200) {
                io.redspace.ironsspellbooks.api.spells.AbstractSpell tertSpell = com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_BEAM.get();
                tooltipComponents.add(Component.literal("§b✦ Técnica Definitiva: ")
                        .append(tertSpell.getDisplayName(null).copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                        .append(Component.literal(" §7[HUD ISB Ranura 3]")));
                tooltipComponents.add(Component.literal("   §3↳ §o(Haz continuo destructivo. ¡Desencadena Choques de Poderes!)"));

                if (denseSoulBlade) {
                    tooltipComponents.add(Component.literal("§f✦ Camino Primordial: §7Resonancia bloqueada §8(Usa Furia Primordial [Z])"));
                } else {
                    io.redspace.ironsspellbooks.api.spells.AbstractSpell quatSpell = com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_RESONANCE.get();
                    tooltipComponents.add(Component.literal("§5✦ Forma Suprema Universal: ")
                            .append(quatSpell.getDisplayName(null).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                            .append(Component.literal(" §7[HUD ISB Ranura 4]")));
                    tooltipComponents.add(Component.literal("   §d↳ §o(Resonancia Espiritual: x2 a todas las stats durante 30s. Aura exaltada)"));
                }
            } else {
                tooltipComponents.add(Component.literal("§8✦ Técnica Definitiva: §7(Bloqueada - Requiere 200 de Afinidad)"));
                if (denseSoulBlade) {
                    tooltipComponents.add(Component.literal("§f✦ Camino Primordial: §7Resonancia bloqueada §8(Usa Furia Primordial [Z])"));
                } else {
                    tooltipComponents.add(Component.literal("§8✦ Forma Suprema: §7(Bloqueada - Requiere 200 de Afinidad)"));
                }
            }
        } else {
            io.redspace.ironsspellbooks.api.spells.AbstractSpell primarySpell = getPrimaryAwakenedSpell(release);
            tooltipComponents.add(Component.literal("§d✦ Técnica Principal: ")
                    .append(primarySpell.getDisplayName(null).copy().withStyle(release.getColor(), ChatFormatting.BOLD))
                    .append(Component.literal(" §7[HUD ISB Ranura 1]")));

            if (affinity >= 150) {
                io.redspace.ironsspellbooks.api.spells.AbstractSpell secSpell = getSecondaryAwakenedSpell(release);
                tooltipComponents.add(Component.literal("§6✦ Segunda Técnica Suprema: ")
                        .append(secSpell.getDisplayName(null).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .append(Component.literal(" §7[HUD ISB Ranura 2]")));
            } else {
                tooltipComponents.add(Component.literal("§8✦ Segunda Técnica Suprema: §7(Bloqueada - Requiere 150 de Afinidad)"));
            }

            if (affinity >= 200) {
                io.redspace.ironsspellbooks.api.spells.AbstractSpell tertSpell = com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_BEAM.get();
                tooltipComponents.add(Component.literal("§b✦ Técnica Definitiva: ")
                        .append(tertSpell.getDisplayName(null).copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                        .append(Component.literal(" §7[HUD ISB Ranura 3]")));
                tooltipComponents.add(Component.literal("   §3↳ §o(Haz continuo destructivo. ¡Desencadena Choques de Poderes!)"));

                if (denseSoulBlade) {
                    tooltipComponents.add(Component.literal("§f✦ Camino Primordial: §7Resonancia bloqueada §8(Usa Furia Primordial [Z])"));
                } else {
                    io.redspace.ironsspellbooks.api.spells.AbstractSpell quatSpell = com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPIRITUAL_RESONANCE.get();
                    tooltipComponents.add(Component.literal("§5✦ Forma Suprema Universal: ")
                            .append(quatSpell.getDisplayName(null).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                            .append(Component.literal(" §7[HUD ISB Ranura 4]")));
                    tooltipComponents.add(Component.literal("   §d↳ §o(Resonancia Espiritual: x2 a todas las stats durante 30s. Aura exaltada)"));
                }
            } else {
                tooltipComponents.add(Component.literal("§8✦ Técnica Definitiva: §7(Bloqueada - Requiere 200 de Afinidad)"));
                if (denseSoulBlade) {
                    tooltipComponents.add(Component.literal("§f✦ Camino Primordial: §7Resonancia bloqueada §8(Usa Furia Primordial [Z])"));
                } else {
                    tooltipComponents.add(Component.literal("§8✦ Forma Suprema: §7(Bloqueada - Requiere 200 de Afinidad)"));
                }
            }
        }

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("§8(Shift + Clic Derecho en Yunque/Mesa de Herrería para alternar aspecto)").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal("§e[Tecla Z] Desatar / Enfundar Primera Liberación").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.literal("§8(Inmune a encantamientos)").withStyle(ChatFormatting.DARK_GRAY));
    }
}
