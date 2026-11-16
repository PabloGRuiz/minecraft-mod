package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.entities.ai.TownswomanEscortGoal;
import com.pablo.rpglevels.entities.ai.TownswomanLumberjackGoal;
import com.pablo.rpglevels.entities.ai.TownswomanOffDutyGoal;
import com.pablo.rpglevels.entities.ai.TownswomanSleepGoal;
import com.pablo.rpglevels.network.OpenNpcDialoguePayload;
import com.pablo.rpglevels.social.NpcSocialManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TownswomanNpcEntity extends PathfinderMob {

    public enum WorkerStatus {
        CIVILIAN,
        ESCORTING,
        WAITING_SETUP,
        WORKING
    }

    public enum WorkRole {
        NONE,
        LUMBERJACK
    }

    private static final EntityDataAccessor<Integer> DATA_SKIN_VARIANT =
            SynchedEntityData.defineId(TownswomanNpcEntity.class, EntityDataSerializers.INT);

    public static final int TOTAL_VARIANTS = 3;

    private static final String[] FEMALE_NAMES = {
            "Elena", "Rosalía", "Lyra", "Celia", "Evelyn",
            "Maeve", "Beatriz", "Yvaine", "Clara", "Aria", "Silvia", "Gwendolyn"
    };

    private long lastGiftDay = -1;

    // Social & Relationships
    private final Map<UUID, Integer> affinityMap = new HashMap<>();
    private final Map<UUID, Long> lastChatGameTime = new HashMap<>();

    // Base Worker System
    private UUID hiredOwnerUuid = null;
    private WorkerStatus workerStatus = WorkerStatus.CIVILIAN;
    private WorkRole workRole = WorkRole.NONE;
    private BlockPos baseCenterPos = null;
    private int workRadius = 8;
    private BlockPos depositChestPos = null;
    private BlockPos foodChestPos = null;
    private BlockPos bedPos = null;

    // Devotion & Vassal System (Tensura Style)
    private boolean isDevoted = false;
    private int vassalRank = 0; // 0 = None, 1 = Rango C, 2 = Rango B, 3 = Rango A, 4 = Rango S
    private String rawName = null;

    // 3-slot inventory: Slot 0 = Tool (Axe), Slots 1 & 2 = Storage Buffer
    private final SimpleContainer workerInventory = new SimpleContainer(3);

    public TownswomanNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation groundNav) {
            groundNav.setCanOpenDoors(true);
            groundNav.setCanPassDoors(true);
        }
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DOOR_OPEN, 0.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DOOR_WOOD_CLOSED, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 0.0);
    }

    public static boolean checkTownswomanSpawnRules(EntityType<? extends PathfinderMob> type, net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType, net.minecraft.core.BlockPos pos, RandomSource random) {
        return level.getRawBrightness(pos, 0) > 8 && level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN_VARIANT, 0);
    }

    public int getSkinVariant() {
        return this.entityData.get(DATA_SKIN_VARIANT);
    }

    public void setSkinVariant(int variant) {
        this.entityData.set(DATA_SKIN_VARIANT, Math.max(0, variant % TOTAL_VARIANTS));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TownswomanEscortGoal(this));
        this.goalSelector.addGoal(2, new TownswomanSleepGoal(this));
        this.goalSelector.addGoal(3, new TownswomanOffDutyGoal(this));
        this.goalSelector.addGoal(4, new TownswomanLumberjackGoal(this));
        this.goalSelector.addGoal(5, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(6, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(7, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 1.2, 1.4));
        this.goalSelector.addGoal(7, new AvoidEntityGoal<>(this, Raider.class, 10.0F, 1.2, 1.4));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    public int getAffinity(Player player) {
        if (player == null) return 0;
        return affinityMap.getOrDefault(player.getUUID(), 0);
    }

    public void addAffinity(Player player, int amount) {
        if (player == null) return;
        int current = getAffinity(player);
        int newScore = Math.max(0, Math.min(100, current + amount));
        affinityMap.put(player.getUUID(), newScore);
    }

    public boolean canChat(Player player) {
        if (player == null) return false;
        long last = lastChatGameTime.getOrDefault(player.getUUID(), -24000L);
        return (this.level().getGameTime() - last) >= 2400L;
    }

    public void recordChat(Player player) {
        if (player != null) {
            lastChatGameTime.put(player.getUUID(), this.level().getGameTime());
        }
    }

    // --- Worker Status & Management ---

    public boolean isHiredWorker() {
        return this.hiredOwnerUuid != null && this.workerStatus != WorkerStatus.CIVILIAN;
    }

    public boolean isHired() {
        return isHiredWorker();
    }

    public UUID getHiringPlayerUUID() {
        return this.hiredOwnerUuid;
    }

    public boolean isHiredBy(Player player) {
        return isHiredWorker() && player != null && player.getUUID().equals(this.hiredOwnerUuid);
    }

    @Nullable
    public Player getHiringPlayer() {
        if (this.hiredOwnerUuid == null) return null;
        return this.level().getPlayerByUUID(this.hiredOwnerUuid);
    }

    public WorkerStatus getWorkerStatus() {
        return this.workerStatus;
    }

    public void setWorkerStatus(WorkerStatus status) {
        this.workerStatus = status;
    }

    public WorkRole getWorkRole() {
        return this.workRole;
    }

    public void setWorkRole(WorkRole role) {
        this.workRole = role;
    }

    @Nullable
    public BlockPos getBaseCenterPos() {
        return this.baseCenterPos;
    }

    public void setBaseCenterPos(@Nullable BlockPos pos) {
        this.baseCenterPos = pos;
    }

    public int getWorkRadius() {
        return this.workRadius;
    }

    public void setWorkRadius(int radius) {
        this.workRadius = Math.max(4, Math.min(24, radius));
    }

    @Nullable
    public BlockPos getDepositChestPos() {
        return this.depositChestPos;
    }

    public void setDepositChestPos(@Nullable BlockPos pos) {
        this.depositChestPos = pos;
    }

    @Nullable
    public BlockPos getFoodChestPos() {
        return this.foodChestPos;
    }

    public void setFoodChestPos(@Nullable BlockPos pos) {
        this.foodChestPos = pos;
    }

    @Nullable
    public BlockPos getBedPos() {
        return this.bedPos;
    }

    public void setBedPos(@Nullable BlockPos pos) {
        this.bedPos = pos;
    }

    public SimpleContainer getWorkerInventory() {
        return this.workerInventory;
    }

    // --- Devotion & Vassal System (Tensura Style) ---

    public boolean isDevoted() {
        return this.isDevoted;
    }

    public void setDevoted(boolean devoted) {
        this.isDevoted = devoted;
        if (devoted && this.vassalRank == 0) {
            this.vassalRank = 1;
        }
        applyRankAttributes();
        updateCustomNameWithRank();
    }

    public int getVassalRank() {
        return this.vassalRank;
    }

    public void setVassalRank(int rank) {
        this.vassalRank = Math.max(0, Math.min(4, rank));
        applyRankAttributes();
        updateCustomNameWithRank();
    }

    public void promoteVassalRank() {
        if (this.vassalRank < 4) {
            this.vassalRank++;
            applyRankAttributes();
            updateCustomNameWithRank();
        }
    }

    public String getVassalRankName() {
        return switch (this.vassalRank) {
            case 4 -> "Rango S (Pilar de la Fortaleza)";
            case 3 -> "Rango A (Guardiana de Élite)";
            case 2 -> "Rango B (Vasalla Fiel)";
            case 1 -> "Rango C (Subordinada Consagrada)";
            default -> "Civil";
        };
    }

    public void applyRankAttributes() {
        if (!isDevoted) return;

        double maxHp = switch (vassalRank) {
            case 4 -> 65.0;
            case 3 -> 50.0;
            case 2 -> 40.0;
            case 1 -> 30.0;
            default -> 20.0;
        };

        double armor = switch (vassalRank) {
            case 4 -> 16.0;
            case 3 -> 12.0;
            case 2 -> 8.0;
            case 1 -> 4.0;
            default -> 0.0;
        };

        double speed = switch (vassalRank) {
            case 4 -> 0.33;
            case 3 -> 0.31;
            case 2 -> 0.29;
            case 1 -> 0.27;
            default -> 0.25;
        };

        var hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(maxHp);
            this.setHealth((float) maxHp);
        }

        var armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(armor);
        }

        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speed);
        }
    }

    public void updateCustomNameWithRank() {
        if (this.rawName == null) {
            if (this.hasCustomName()) {
                String current = this.getCustomName().getString();
                current = current.replaceAll("§[0-9a-fk-or]", "");
                current = current.replaceAll("\\[.*?\\]", "").replace("★", "").trim();
                this.rawName = current.isEmpty() ? "Elena" : current;
            } else {
                this.rawName = "Elena";
            }
        }

        if (!isDevoted) {
            this.setCustomName(Component.literal("§e" + this.rawName));
        } else {
            String title = switch (vassalRank) {
                case 4 -> "§6★ [Rango S] §e";
                case 3 -> "§d★ [Rango A] §e";
                case 2 -> "§b[Rango B] §e";
                default -> "§a[Rango C] §e";
            };
            this.setCustomName(Component.literal(title + this.rawName + (vassalRank == 4 ? " §6★" : "")));
        }
        this.setCustomNameVisible(true);
    }

    public void hireAsWorker(Player player) {
        this.hiredOwnerUuid = player.getUUID();
        this.workerStatus = WorkerStatus.ESCORTING;
        this.workRole = WorkRole.LUMBERJACK;
        this.setPersistenceRequired();
    }

    public void arrivedAtBase(Player player) {
        this.workerStatus = WorkerStatus.WAITING_SETUP;
        this.baseCenterPos = this.blockPosition();
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public void setHeldAxeVisible(boolean visible) {
        if (visible) {
            ItemStack tool = this.workerInventory.getItem(0);
            if (!tool.isEmpty() && tool.getItem() instanceof AxeItem) {
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, tool);
            }
        } else {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    public void setHeldFood(ItemStack food) {
        if (!food.isEmpty()) {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, food.copy());
        } else {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Nullable
    public BlockPos findChestWithAxe() {
        BlockPos[] chests = { depositChestPos, foodChestPos };
        for (BlockPos chestPos : chests) {
            if (chestPos == null) continue;
            if (this.level().getBlockEntity(chestPos) instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack st = container.getItem(i);
                    if (!st.isEmpty() && st.getItem() instanceof AxeItem) {
                        return chestPos;
                    }
                }
            }
        }
        return null;
    }

    public boolean takeAxeFromChest(BlockPos chestPos) {
        if (chestPos == null) return false;
        if (this.level().getBlockEntity(chestPos) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack st = container.getItem(i);
                if (!st.isEmpty() && st.getItem() instanceof AxeItem) {
                    ItemStack taken = st.split(1);
                    if (st.isEmpty()) container.setItem(i, ItemStack.EMPTY);
                    container.setChanged();
                    this.workerInventory.setItem(0, taken);
                    this.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.2F);
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public BlockPos findChestWithFood() {
        BlockPos[] chests = { foodChestPos, depositChestPos };
        for (BlockPos chestPos : chests) {
            if (chestPos == null) continue;
            if (this.level().getBlockEntity(chestPos) instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack st = container.getItem(i);
                    if (!st.isEmpty() && st.has(DataComponents.FOOD)) {
                        return chestPos;
                    }
                }
            }
        }
        return null;
    }

    public ItemStack takeFoodFromChest(BlockPos chestPos) {
        if (chestPos == null) return ItemStack.EMPTY;
        if (this.level().getBlockEntity(chestPos) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack st = container.getItem(i);
                if (!st.isEmpty() && st.has(DataComponents.FOOD)) {
                    ItemStack taken = st.split(1);
                    if (st.isEmpty()) container.setItem(i, ItemStack.EMPTY);
                    container.setChanged();
                    return taken;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public void dismissWorkerContract(@Nullable Player player, String reason) {
        this.hiredOwnerUuid = null;
        this.workerStatus = WorkerStatus.CIVILIAN;
        this.workRole = WorkRole.NONE;
        this.baseCenterPos = null;
        this.depositChestPos = null;
        this.foodChestPos = null;
        this.bedPos = null;
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("§c[Trabajadora] El contrato con " + this.getName().getString() + " ha sido cancelado (" + reason + ").").withStyle(ChatFormatting.RED));
        }
    }

    public boolean hasAxeInToolSlot() {
        ItemStack tool = this.workerInventory.getItem(0);
        if (!tool.isEmpty() && tool.getItem() instanceof AxeItem) {
            return true;
        }
        for (int i = 1; i < 3; i++) {
            ItemStack st = this.workerInventory.getItem(i);
            if (!st.isEmpty() && st.getItem() instanceof AxeItem) {
                ItemStack current0 = this.workerInventory.getItem(0);
                this.workerInventory.setItem(0, st);
                this.workerInventory.setItem(i, current0);
                return true;
            }
        }
        return false;
    }

    public void damageAxe(int amount) {
        ItemStack tool = this.workerInventory.getItem(0);
        if (!tool.isEmpty() && tool.isDamageableItem()) {
            tool.setDamageValue(tool.getDamageValue() + amount);
            if (tool.getDamageValue() >= tool.getMaxDamage()) {
                this.workerInventory.setItem(0, ItemStack.EMPTY);
                this.playSound(SoundEvents.ITEM_BREAK, 1.0F, 1.0F);
            }
        }
    }

    public ItemStack addToWorkerStorage(ItemStack stack) {
        for (int s = 1; s <= 2; s++) {
            ItemStack current = this.workerInventory.getItem(s);
            if (current.isEmpty()) {
                this.workerInventory.setItem(s, stack.copy());
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
                int canAdd = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
                current.grow(canAdd);
                stack.shrink(canAdd);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    public boolean isWorkerStorageFull() {
        ItemStack s1 = this.workerInventory.getItem(1);
        ItemStack s2 = this.workerInventory.getItem(2);
        return !s1.isEmpty() && s1.getCount() >= s1.getMaxStackSize()
                && !s2.isEmpty() && s2.getCount() >= s2.getMaxStackSize();
    }

    public boolean hasLumberInStorage() {
        return !this.workerInventory.getItem(1).isEmpty() || !this.workerInventory.getItem(2).isEmpty();
    }

    public ItemStack findAndConsumeSapling() {
        for (int s = 1; s <= 2; s++) {
            ItemStack st = this.workerInventory.getItem(s);
            if (!st.isEmpty() && (st.is(ItemTags.SAPLINGS) || (st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SaplingBlock))) {
                ItemStack single = st.split(1);
                if (st.isEmpty()) this.workerInventory.setItem(s, ItemStack.EMPTY);
                return single;
            }
        }
        return ItemStack.EMPTY;
    }

    public void openWorkerInventory(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((containerId, playerInventory, p) -> {
            SimpleContainer displayContainer = new SimpleContainer(9) {
                @Override
                public ItemStack getItem(int slot) {
                    if (slot < 3) return workerInventory.getItem(slot);
                    ItemStack locked = new ItemStack(Blocks.GRAY_STAINED_GLASS_PANE);
                    locked.set(DataComponents.CUSTOM_NAME, Component.literal("§8[Ranura Bloqueada]").withStyle(ChatFormatting.GRAY));
                    return locked;
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    if (slot < 3) {
                        workerInventory.setItem(slot, stack);
                        setChanged();
                    }
                }

                @Override
                public boolean canPlaceItem(int slot, ItemStack stack) {
                    if (slot == 0) {
                        return stack.getItem() instanceof AxeItem;
                    }
                    return slot < 3;
                }

                @Override
                public ItemStack removeItem(int slot, int amount) {
                    if (slot < 3) {
                        ItemStack res = workerInventory.removeItem(slot, amount);
                        setChanged();
                        return res;
                    }
                    return ItemStack.EMPTY;
                }

                @Override
                public ItemStack removeItemNoUpdate(int slot) {
                    if (slot < 3) {
                        ItemStack res = workerInventory.removeItemNoUpdate(slot);
                        setChanged();
                        return res;
                    }
                    return ItemStack.EMPTY;
                }
            };

            return new ChestMenu(
                    MenuType.GENERIC_9x1,
                    containerId,
                    playerInventory,
                    displayContainer,
                    1
            );
        }, Component.literal(this.getName().getString() + " §7(Ranura 1: Hacha | 2 y 3: Carga)")));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        RandomSource random = level.getRandom();

        setSkinVariant(random.nextInt(TOTAL_VARIANTS));

        if (!this.hasCustomName()) {
            String randomName = FEMALE_NAMES[random.nextInt(FEMALE_NAMES.length)];
            this.rawName = randomName;
            updateCustomNameWithRank();
        } else {
            String nameStr = this.getCustomName().getString().replaceAll("§[0-9a-fk-or]", "");
            this.rawName = nameStr.replaceAll("\\[.*?\\]", "").replace("★", "").trim();
            updateCustomNameWithRank();
        }

        return data;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Sneak + Right Click opens worker inventory directly if player is the owner!
            if (serverPlayer.isShiftKeyDown() && isHiredBy(serverPlayer)) {
                openWorkerInventory(serverPlayer);
                return InteractionResult.SUCCESS;
            }

            String villagerName = this.hasCustomName() ? this.getCustomName().getString() : "Aldeana";
            int affinity = getAffinity(player);

            String greeting;
            if (this.isDevoted) {
                greeting = "§6\"¡Mi amo! Es un absoluto honor recibiros. La fortaleza prospera bajo vuestro liderazgo. ¿En qué puedo serviros hoy?\"";
            } else if (this.workerStatus == WorkerStatus.ESCORTING) {
                greeting = "§a\"Te sigo el paso. Avísame en cuanto lleguemos a tu base para comenzar a trabajar.\"";
            } else if (this.workerStatus == WorkerStatus.WAITING_SETUP) {
                greeting = "§b\"Estoy esperando que asignes el área y los cofres con el Bastón del Patrón para comenzar a trabajar.\"";
            } else if (this.workerStatus == WorkerStatus.WORKING) {
                greeting = "§6\"¡Saludos, jefe! Mis labores de leñadora van viento en popa. ¿Necesitas ajustar algo de mi trabajo?\"";
            } else {
                greeting = NpcSocialManager.getGreeting(
                        "townswoman", affinity, AdventurerRank.F, villagerName, player.getName().getString(),
                        false, false
                );
            }

            // Open MCA-style dialogue screen with workerStatus
            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    villagerName,
                    "townswoman",
                    this.getSkinVariant(),
                    0,
                    affinity,
                    isHiredWorker(),
                    isHiredBy(player),
                    0,
                    false,
                    this.workerStatus.ordinal(),
                    this.isDevoted(),
                    this.getVassalRank(),
                    false,
                    0,
                    false,
                    greeting
            ));

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.25F);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SkinVariant", getSkinVariant());
        compound.putLong("LastGiftDay", lastGiftDay);

        // Affinities
        CompoundTag affTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : affinityMap.entrySet()) {
            affTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        compound.put("Affinities", affTag);

        // Worker Data
        compound.putInt("WorkerStatus", workerStatus.ordinal());
        compound.putInt("WorkRole", workRole.ordinal());
        if (hiredOwnerUuid != null) {
            compound.putUUID("HiredOwner", hiredOwnerUuid);
        }
        compound.putInt("WorkRadius", workRadius);
        if (baseCenterPos != null) {
            compound.putInt("BaseCenterX", baseCenterPos.getX());
            compound.putInt("BaseCenterY", baseCenterPos.getY());
            compound.putInt("BaseCenterZ", baseCenterPos.getZ());
        }
        if (depositChestPos != null) {
            compound.putInt("DepChestX", depositChestPos.getX());
            compound.putInt("DepChestY", depositChestPos.getY());
            compound.putInt("DepChestZ", depositChestPos.getZ());
        }
        if (foodChestPos != null) {
            compound.putInt("FoodChestX", foodChestPos.getX());
            compound.putInt("FoodChestY", foodChestPos.getY());
            compound.putInt("FoodChestZ", foodChestPos.getZ());
        }
        if (bedPos != null) {
            compound.putInt("BedPosX", bedPos.getX());
            compound.putInt("BedPosY", bedPos.getY());
            compound.putInt("BedPosZ", bedPos.getZ());
        }

        compound.putBoolean("IsDevoted", isDevoted);
        compound.putInt("VassalRank", vassalRank);
        if (rawName != null) {
            compound.putString("RawName", rawName);
        }

        // 3-slot inventory
        ListTag invList = new ListTag();
        for (int i = 0; i < workerInventory.getContainerSize(); i++) {
            ItemStack stack = workerInventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                invList.add(stack.save(this.registryAccess(), itemTag));
            }
        }
        compound.put("WorkerInventory", invList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SkinVariant")) {
            setSkinVariant(compound.getInt("SkinVariant"));
        }
        if (compound.contains("LastGiftDay")) {
            this.lastGiftDay = compound.getLong("LastGiftDay");
        }

        if (compound.contains("Affinities", Tag.TAG_COMPOUND)) {
            CompoundTag affTag = compound.getCompound("Affinities");
            for (String key : affTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    affinityMap.put(uuid, affTag.getInt(key));
                } catch (Exception ignored) {}
            }
        }

        // Worker Data
        if (compound.contains("WorkerStatus")) {
            int ws = compound.getInt("WorkerStatus");
            WorkerStatus[] statuses = WorkerStatus.values();
            this.workerStatus = (ws >= 0 && ws < statuses.length) ? statuses[ws] : WorkerStatus.CIVILIAN;
        }
        if (compound.contains("WorkRole")) {
            int wr = compound.getInt("WorkRole");
            WorkRole[] roles = WorkRole.values();
            this.workRole = (wr >= 0 && wr < roles.length) ? roles[wr] : WorkRole.NONE;
        }
        if (compound.hasUUID("HiredOwner")) {
            this.hiredOwnerUuid = compound.getUUID("HiredOwner");
        }
        if (compound.contains("WorkRadius")) {
            this.workRadius = compound.getInt("WorkRadius");
        }
        if (compound.contains("BaseCenterX")) {
            this.baseCenterPos = new BlockPos(compound.getInt("BaseCenterX"), compound.getInt("BaseCenterY"), compound.getInt("BaseCenterZ"));
        }
        if (compound.contains("DepChestX")) {
            this.depositChestPos = new BlockPos(compound.getInt("DepChestX"), compound.getInt("DepChestY"), compound.getInt("DepChestZ"));
        }
        if (compound.contains("FoodChestX")) {
            this.foodChestPos = new BlockPos(compound.getInt("FoodChestX"), compound.getInt("FoodChestY"), compound.getInt("FoodChestZ"));
        }
        if (compound.contains("BedPosX")) {
            this.bedPos = new BlockPos(compound.getInt("BedPosX"), compound.getInt("BedPosY"), compound.getInt("BedPosZ"));
        }

        if (compound.contains("IsDevoted")) {
            this.isDevoted = compound.getBoolean("IsDevoted");
        }
        if (compound.contains("VassalRank")) {
            this.vassalRank = compound.getInt("VassalRank");
        }
        if (compound.contains("RawName")) {
            this.rawName = compound.getString("RawName");
        }
        applyRankAttributes();
        updateCustomNameWithRank();

        // Inventory
        if (compound.contains("WorkerInventory", Tag.TAG_LIST)) {
            ListTag invList = compound.getList("WorkerInventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < invList.size(); i++) {
                CompoundTag itemTag = invList.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < workerInventory.getContainerSize()) {
                    workerInventory.setItem(slot, ItemStack.parse(this.registryAccess(), itemTag).orElse(ItemStack.EMPTY));
                }
            }
        }
    }
}
