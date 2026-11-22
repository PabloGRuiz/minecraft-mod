package com.pablo.tenseicraft.items;

import com.pablo.tenseicraft.entities.TownswomanNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * Bastón del Patrón (Overseer's Cane)
 * Magical configuration tool used to delimit work boundaries, assign chests, and set beds
 * for base workers.
 */
public class OverseerCaneItem extends Item {

    public static final String TAG_WORKER_UUID = "WorkerUUID";
    public static final String TAG_WORKER_NAME = "WorkerName";
    public static final String TAG_CENTER_X = "CenterX";
    public static final String TAG_CENTER_Y = "CenterY";
    public static final String TAG_CENTER_Z = "CenterZ";
    public static final String TAG_RADIUS = "Radius";
    public static final String TAG_HAS_AREA = "HasArea";
    public static final String TAG_DEP_X = "DepositX";
    public static final String TAG_DEP_Y = "DepositY";
    public static final String TAG_DEP_Z = "DepositZ";
    public static final String TAG_HAS_DEP = "HasDeposit";
    public static final String TAG_FOOD_X = "FoodX";
    public static final String TAG_FOOD_Y = "FoodY";
    public static final String TAG_FOOD_Z = "FoodZ";
    public static final String TAG_HAS_FOOD = "HasFood";
    public static final String TAG_BED_X = "BedX";
    public static final String TAG_BED_Y = "BedY";
    public static final String TAG_BED_Z = "BedZ";
    public static final String TAG_HAS_BED = "HasBed";
    public static final String TAG_IS_EDIT = "IsEditMode";

    public OverseerCaneItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createCaneForWorker(TownswomanNpcEntity worker) {
        return createCaneForWorker(worker, false);
    }

    public static ItemStack createCaneForWorker(TownswomanNpcEntity worker, boolean isEditMode) {
        ItemStack stack = new ItemStack(ModItems.OVERSEER_CANE.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6Bastón del Patrón (" + worker.getName().getString() + ")"));

        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_WORKER_UUID, worker.getUUID().toString());
        tag.putString(TAG_WORKER_NAME, worker.getName().getString());
        tag.putInt(TAG_RADIUS, Math.max(6, worker.getWorkRadius()));
        tag.putBoolean(TAG_IS_EDIT, isEditMode);

        if (isEditMode) {
            // In Edit Mode, pause worker and reset assignment booleans so single clicks won't auto-finish
            worker.setWorkerStatus(TownswomanNpcEntity.WorkerStatus.WAITING_SETUP);
            tag.putBoolean(TAG_HAS_AREA, false);
            tag.putBoolean(TAG_HAS_DEP, false);
            tag.putBoolean(TAG_HAS_FOOD, false);
            tag.putBoolean(TAG_HAS_BED, false);

            if (worker.getBaseCenterPos() != null) {
                tag.putInt("PrevCenterX", worker.getBaseCenterPos().getX());
                tag.putInt("PrevCenterY", worker.getBaseCenterPos().getY());
                tag.putInt("PrevCenterZ", worker.getBaseCenterPos().getZ());
            }
        } else {
            if (worker.getBaseCenterPos() != null) {
                tag.putInt(TAG_CENTER_X, worker.getBaseCenterPos().getX());
                tag.putInt(TAG_CENTER_Y, worker.getBaseCenterPos().getY());
                tag.putInt(TAG_CENTER_Z, worker.getBaseCenterPos().getZ());
            }
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Enchanted glint effect
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TownswomanNpcEntity worker && player instanceof ServerPlayer serverPlayer) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            if (tag.getString(TAG_WORKER_UUID).equals(worker.getUUID().toString())) {
                boolean hasArea = tag.getBoolean(TAG_HAS_AREA);
                boolean isEdit = tag.getBoolean(TAG_IS_EDIT);

                // If editing or has at least area assigned, clicking worker confirms/saves configuration!
                if (isEdit || hasArea) {
                    if (!tag.getBoolean(TAG_HAS_AREA) && worker.getBaseCenterPos() != null) {
                        tag.putInt(TAG_CENTER_X, worker.getBaseCenterPos().getX());
                        tag.putInt(TAG_CENTER_Y, worker.getBaseCenterPos().getY());
                        tag.putInt(TAG_CENTER_Z, worker.getBaseCenterPos().getZ());
                        tag.putBoolean(TAG_HAS_AREA, true);
                    }
                    if (!tag.getBoolean(TAG_HAS_DEP) && worker.getDepositChestPos() != null) {
                        tag.putInt(TAG_DEP_X, worker.getDepositChestPos().getX());
                        tag.putInt(TAG_DEP_Y, worker.getDepositChestPos().getY());
                        tag.putInt(TAG_DEP_Z, worker.getDepositChestPos().getZ());
                        tag.putBoolean(TAG_HAS_DEP, true);
                    }
                    if (!tag.getBoolean(TAG_HAS_FOOD) && worker.getFoodChestPos() != null) {
                        tag.putInt(TAG_FOOD_X, worker.getFoodChestPos().getX());
                        tag.putInt(TAG_FOOD_Y, worker.getFoodChestPos().getY());
                        tag.putInt(TAG_FOOD_Z, worker.getFoodChestPos().getZ());
                        tag.putBoolean(TAG_HAS_FOOD, true);
                    }
                    if (!tag.getBoolean(TAG_HAS_BED) && worker.getBedPos() != null) {
                        tag.putInt(TAG_BED_X, worker.getBedPos().getX());
                        tag.putInt(TAG_BED_Y, worker.getBedPos().getY());
                        tag.putInt(TAG_BED_Z, worker.getBedPos().getZ());
                        tag.putBoolean(TAG_HAS_BED, true);
                    }

                    checkAndCompleteSetup(serverPlayer, stack, tag);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(clickedPos);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        boolean isChest = state.is(BlockTags.SHULKER_BOXES)
                || state.getBlock() instanceof net.minecraft.world.level.block.AbstractChestBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock;

        boolean isBed = state.getBlock() instanceof BedBlock;

        // 1. Shift + Right Click on Chest -> Assign Deposit Chest
        if (player.isShiftKeyDown() && isChest) {
            tag.putInt(TAG_DEP_X, clickedPos.getX());
            tag.putInt(TAG_DEP_Y, clickedPos.getY());
            tag.putInt(TAG_DEP_Z, clickedPos.getZ());
            tag.putBoolean(TAG_HAS_DEP, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            serverPlayer.sendSystemMessage(Component.literal("§a[Bastón del Patrón] §fCofre de Depósito asignado en §e" + clickedPos.toShortString() + "§a.").withStyle(ChatFormatting.GREEN));
            level.playSound(null, clickedPos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.9F, 1.2F);
            checkAndCompleteSetup(serverPlayer, stack, tag);
            return InteractionResult.SUCCESS;
        }

        // 2. Normal Right Click on Bed -> Assign Bed
        if (isBed) {
            tag.putInt(TAG_BED_X, clickedPos.getX());
            tag.putInt(TAG_BED_Y, clickedPos.getY());
            tag.putInt(TAG_BED_Z, clickedPos.getZ());
            tag.putBoolean(TAG_HAS_BED, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            serverPlayer.sendSystemMessage(Component.literal("§a[Bastón del Patrón] §fCama de descanso asignada en §e" + clickedPos.toShortString() + "§a.").withStyle(ChatFormatting.GREEN));
            level.playSound(null, clickedPos, SoundEvents.VILLAGER_WORK_CLERIC, SoundSource.BLOCKS, 0.9F, 1.0F);
            checkAndCompleteSetup(serverPlayer, stack, tag);
            return InteractionResult.SUCCESS;
        }

        // 3. Shift + Right Click on Ground -> RE-ESTABLECER NUEVO CENTRO
        int currentRadius = tag.getInt(TAG_RADIUS);
        if (currentRadius <= 0) currentRadius = 6;

        if (player.isShiftKeyDown()) {
            tag.putInt(TAG_CENTER_X, clickedPos.getX());
            tag.putInt(TAG_CENTER_Y, clickedPos.getY());
            tag.putInt(TAG_CENTER_Z, clickedPos.getZ());
            tag.putInt(TAG_RADIUS, currentRadius);
            tag.putBoolean(TAG_HAS_AREA, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            serverPlayer.sendSystemMessage(Component.literal("§b[Bastón del Patrón] §a¡Nuevo centro fijado en §e" + clickedPos.toShortString() + "§a! §7(Radio: " + currentRadius + "). Clic Der. para expandir, Clic Izq. para reducir.").withStyle(ChatFormatting.AQUA));
            spawnPerimeterParticles((ServerLevel) level, clickedPos, currentRadius);
            level.playSound(null, clickedPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.4F);
            checkAndCompleteSetup(serverPlayer, stack, tag);
            return InteractionResult.SUCCESS;
        }

        // 4. Normal Right Click on Ground -> Set or Expand Work Area
        boolean hasArea = tag.getBoolean(TAG_HAS_AREA);

        if (!hasArea) {
            tag.putInt(TAG_CENTER_X, clickedPos.getX());
            tag.putInt(TAG_CENTER_Y, clickedPos.getY());
            tag.putInt(TAG_CENTER_Z, clickedPos.getZ());
            tag.putInt(TAG_RADIUS, 6);
            tag.putBoolean(TAG_HAS_AREA, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            serverPlayer.sendSystemMessage(Component.literal("§b[Bastón del Patrón] §fCentro del área de tala fijado en §e" + clickedPos.toShortString() + " §7(Radio: 6). Clic para expandir, Shift+Clic para mover centro.").withStyle(ChatFormatting.AQUA));
            spawnPerimeterParticles((ServerLevel) level, clickedPos, 6);
            level.playSound(null, clickedPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.3F);
        } else {
            int newRadius = Math.min(24, currentRadius + 2);
            tag.putInt(TAG_RADIUS, newRadius);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            BlockPos center = new BlockPos(tag.getInt(TAG_CENTER_X), tag.getInt(TAG_CENTER_Y), tag.getInt(TAG_CENTER_Z));
            serverPlayer.sendSystemMessage(Component.literal("§b[Bastón del Patrón] §fÁrea de tala expandida a radio: §e" + newRadius + " bloques§f.").withStyle(ChatFormatting.AQUA));
            spawnPerimeterParticles((ServerLevel) level, center, newRadius);
            level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 0.8F, 1.4F);
        }

        checkAndCompleteSetup(serverPlayer, stack, tag);
        return InteractionResult.SUCCESS;
    }

    /**
     * Called when player left clicks with the cane:
     * Shift + Left Click on Chest -> Assign Food Chest
     * Left Click on Ground -> Shrink Area
     */
    public static void handleLeftClick(ServerPlayer player, ItemStack stack, BlockPos clickedPos, BlockState state) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        ServerLevel level = player.serverLevel();

        boolean isChest = state.is(BlockTags.SHULKER_BOXES)
                || state.getBlock() instanceof net.minecraft.world.level.block.AbstractChestBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock;

        if (player.isShiftKeyDown() && isChest) {
            tag.putInt(TAG_FOOD_X, clickedPos.getX());
            tag.putInt(TAG_FOOD_Y, clickedPos.getY());
            tag.putInt(TAG_FOOD_Z, clickedPos.getZ());
            tag.putBoolean(TAG_HAS_FOOD, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            player.sendSystemMessage(Component.literal("§a[Bastón del Patrón] §fCofre de Comida / Suministros asignado en §e" + clickedPos.toShortString() + "§a.").withStyle(ChatFormatting.GREEN));
            level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.9F, 1.2F);
            checkAndCompleteSetup(player, stack, tag);
            return;
        }

        // Normal left click on ground -> shrink radius
        if (tag.getBoolean(TAG_HAS_AREA)) {
            int currentRadius = tag.getInt(TAG_RADIUS);
            int newRadius = Math.max(4, currentRadius - 2);
            tag.putInt(TAG_RADIUS, newRadius);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            BlockPos center = new BlockPos(tag.getInt(TAG_CENTER_X), tag.getInt(TAG_CENTER_Y), tag.getInt(TAG_CENTER_Z));
            player.sendSystemMessage(Component.literal("§e[Bastón del Patrón] §fÁrea de tala reducida a radio: §6" + newRadius + " bloques§f.").withStyle(ChatFormatting.YELLOW));
            spawnPerimeterParticles(level, center, newRadius);
            level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
            checkAndCompleteSetup(player, stack, tag);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;

        // Render boundary grid every 20 ticks (1 second) when held
        if (isSelected || player.getOffhandItem() == stack) {
            if (level.getGameTime() % 20 == 0) {
                CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = customData.copyTag();
                if (tag.getBoolean(TAG_HAS_AREA)) {
                    BlockPos center = new BlockPos(tag.getInt(TAG_CENTER_X), tag.getInt(TAG_CENTER_Y), tag.getInt(TAG_CENTER_Z));
                    int radius = tag.getInt(TAG_RADIUS);
                    spawnPerimeterParticles((ServerLevel) level, center, radius);
                }
            }
        }
    }

    public static void spawnPerimeterParticles(ServerLevel level, BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int y = center.getY() + 1;

        // Draw boundary rectangle with particles
        for (int x = minX; x <= maxX; x += 2) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.1, minZ + 0.5, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.1, maxZ + 0.5, 1, 0, 0, 0, 0);
        }
        for (int z = minZ; z <= maxZ; z += 2) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, minX + 0.5, y + 0.1, z + 0.5, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, maxX + 0.5, y + 0.1, z + 0.5, 1, 0, 0, 0, 0);
        }
    }

    public static void checkAndCompleteSetup(ServerPlayer player, ItemStack stack, CompoundTag tag) {
        boolean hasArea = tag.getBoolean(TAG_HAS_AREA);
        boolean hasDep = tag.getBoolean(TAG_HAS_DEP);
        boolean hasFood = tag.getBoolean(TAG_HAS_FOOD);
        boolean hasBed = tag.getBoolean(TAG_HAS_BED);

        if (hasArea && hasDep && hasFood && hasBed) {
            String workerUuidStr = tag.getString(TAG_WORKER_UUID);
            if (workerUuidStr.isEmpty()) return;

            UUID workerUuid = UUID.fromString(workerUuidStr);
            TownswomanNpcEntity worker = findWorkerByUuid(player.serverLevel(), workerUuid);

            if (worker != null) {
                BlockPos center = new BlockPos(tag.getInt(TAG_CENTER_X), tag.getInt(TAG_CENTER_Y), tag.getInt(TAG_CENTER_Z));
                int radius = tag.getInt(TAG_RADIUS);
                BlockPos depChest = new BlockPos(tag.getInt(TAG_DEP_X), tag.getInt(TAG_DEP_Y), tag.getInt(TAG_DEP_Z));
                BlockPos foodChest = new BlockPos(tag.getInt(TAG_FOOD_X), tag.getInt(TAG_FOOD_Y), tag.getInt(TAG_FOOD_Z));
                BlockPos bedPos = new BlockPos(tag.getInt(TAG_BED_X), tag.getInt(TAG_BED_Y), tag.getInt(TAG_BED_Z));

                worker.setBaseCenterPos(center);
                worker.setWorkRadius(radius);
                worker.setDepositChestPos(depChest);
                worker.setFoodChestPos(foodChest);
                worker.setBedPos(bedPos);
                worker.setWorkRole(TownswomanNpcEntity.WorkRole.LUMBERJACK);
                worker.setWorkerStatus(TownswomanNpcEntity.WorkerStatus.WORKING);

                // Consume cane tool upon completion
                stack.shrink(1);

                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);

                player.sendSystemMessage(Component.literal("§6§l========================================"));
                player.sendSystemMessage(Component.literal("§a§l✔ ¡CONFIGURACIÓN DE TRABAJADOR COMPLETADA!"));
                player.sendSystemMessage(Component.literal("§f" + worker.getName().getString() + " §eha comenzado sus labores como §6Leñadora§e."));
                player.sendSystemMessage(Component.literal("§7El bastón ha cumplido su función y ha desaparecido."));
                player.sendSystemMessage(Component.literal("§7(Habla con ella y selecciona 'Editar Trabajo' si deseas reconfigurarla)."));
                player.sendSystemMessage(Component.literal("§6§l========================================"));
            }
        }
    }

    private static TownswomanNpcEntity findWorkerByUuid(ServerLevel level, UUID uuid) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof TownswomanNpcEntity tw && tw.getUUID().equals(uuid)) {
                return tw;
            }
        }
        return null;
    }

    public static void sendSetupGuide(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6§l========================================"));
        player.sendSystemMessage(Component.literal("§6§l    BASTÓN DEL PATRÓN - GUÍA DE INICIO"));
        player.sendSystemMessage(Component.literal("§6§l========================================"));
        player.sendSystemMessage(Component.literal("§e¡Tu trabajadora ha llegado a la base! Usa el bastón en mano:"));
        player.sendSystemMessage(Component.literal("§b1. Clic Derecho en Suelo: §fFija centro o expande el área (+2 radio)."));
        player.sendSystemMessage(Component.literal("§b2. Shift + Clic Derecho en Suelo: §fCambia el centro del área al bloque seleccionado."));
        player.sendSystemMessage(Component.literal("§b3. Clic Izquierdo en Suelo: §fReduce el radio del área (-2 radio)."));
        player.sendSystemMessage(Component.literal("§b4. Shift + Clic Derecho en Cofre: §fAsigna el cofre de depósito (donde guardará la madera)."));
        player.sendSystemMessage(Component.literal("§b5. Shift + Clic Izquierdo en Cofre: §fAsigna el cofre de comida / suministros."));
        player.sendSystemMessage(Component.literal("§b6. Clic Derecho en Cama: §fAsigna su cama para descansar por la noche."));
        player.sendSystemMessage(Component.literal("§a¡Al completar las 4 tareas, el bastón desaparecerá y comenzará a trabajar!"));
        player.sendSystemMessage(Component.literal("§7(También puedes hacer Clic Derecho sobre la aldeana con el bastón para confirmar)."));
        player.sendSystemMessage(Component.literal("§6§l========================================"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        String workerName = tag.getString(TAG_WORKER_NAME);
        if (!workerName.isEmpty()) {
            tooltip.add(Component.literal("§7Vinculado a: §e" + workerName));
        }

        boolean isEdit = tag.getBoolean(TAG_IS_EDIT);
        if (isEdit) {
            tooltip.add(Component.literal("§6[Modo Edición]: §eReasignando tareas..."));
        }

        boolean hasArea = tag.getBoolean(TAG_HAS_AREA);
        boolean hasDep = tag.getBoolean(TAG_HAS_DEP);
        boolean hasFood = tag.getBoolean(TAG_HAS_FOOD);
        boolean hasBed = tag.getBoolean(TAG_HAS_BED);

        tooltip.add(Component.literal("§e--- Tareas de Asignación ---"));
        tooltip.add(Component.literal((hasArea ? "§a[✔] " : "§c[✘] ") + "§fÁrea de Trabajo: " + (hasArea ? "§bRadio " + tag.getInt(TAG_RADIUS) : "§7Sin definir (Shift+Clic Der: mover centro)")));
        tooltip.add(Component.literal((hasDep ? "§a[✔] " : "§c[✘] ") + "§fCofre de Depósito: " + (hasDep ? "§eAsignado" : "§7Shift+Clic Der en Cofre")));
        tooltip.add(Component.literal((hasFood ? "§a[✔] " : "§c[✘] ") + "§fCofre de Comida: " + (hasFood ? "§eAsignado" : "§7Shift+Clic Izq en Cofre")));
        tooltip.add(Component.literal((hasBed ? "§a[✔] " : "§c[✘] ") + "§fCama de Descanso: " + (hasBed ? "§eAsignada" : "§7Clic Der en Cama")));

        tooltip.add(Component.literal("§e--- Controles ---"));
        tooltip.add(Component.literal("§f• Shift + Clic Der. en Suelo: §bFijar Nuevo Centro"));
        tooltip.add(Component.literal("§f• Clic Der. en Suelo: §7Expandir radio (+2)"));
        tooltip.add(Component.literal("§f• Clic Izq. en Suelo: §7Reducir radio (-2)"));
        tooltip.add(Component.literal("§f• Clic Der. en Aldeana: §aGuardar / Confirmar"));
    }
}
