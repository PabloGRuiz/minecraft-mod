package com.pablo.tenseicraft.items;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.network.OpenAdventurerCardPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public class AdventurerCardItem extends Item {

    public static final String TAG_OWNER_NAME = "OwnerName";
    public static final String TAG_OWNER_UUID = "OwnerUUID";
    public static final String TAG_RANK = "Rank";
    public static final String TAG_RANK_INDEX = "RankIndex";
    public static final String TAG_XP_LEVEL = "XpLevel";
    public static final String TAG_REP_SCORE = "RepScore";
    public static final String TAG_REP_DISPLAY = "RepDisplay";
    public static final String TAG_FORTITUDE = "Fortitude";
    public static final String TAG_DEXTERITY = "Dexterity";
    public static final String TAG_SPIRIT = "Spirit";
    public static final String TAG_LUCK = "Luck";
    public static final String TAG_HEALTH = "Health";
    public static final String TAG_MAX_HEALTH = "MaxHealth";
    public static final String TAG_MANA = "Mana";
    public static final String TAG_MAX_MANA = "MaxMana";
    public static final String TAG_HAS_SNAPSHOT = "HasSnapshot";
    public static final String TAG_IS_ONLINE = "IsOwnerOnline";

    public AdventurerCardItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createPlayerCard(Player player, PlayerRpgData data) {
        ItemStack stack = new ItemStack(ModItems.ADVENTURER_CARD.get());
        CompoundTag tag = buildOwnerSnapshot(player, data, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Tarjeta de Aventurero de " + player.getName().getString())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );
        return stack;
    }

    public static CompoundTag buildOwnerSnapshot(Player owner, PlayerRpgData data, boolean online) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_OWNER_NAME, owner.getName().getString());
        tag.putString(TAG_OWNER_UUID, owner.getUUID().toString());
        tag.putString(TAG_RANK, data.getRank().getName());
        tag.putInt(TAG_RANK_INDEX, data.getRank().getIndex());
        tag.putInt(TAG_XP_LEVEL, data.getRpgLevel());
        tag.putInt(TAG_REP_SCORE, data.getReputationScore());
        tag.putString(TAG_REP_DISPLAY, data.getReputationTier().getDisplayName().getString());
        tag.putInt(TAG_FORTITUDE, data.getTotalStatPoints(owner, StatType.FORTITUDE));
        tag.putInt(TAG_DEXTERITY, data.getTotalStatPoints(owner, StatType.DEXTERITY));
        tag.putInt(TAG_SPIRIT, data.getTotalStatPoints(owner, StatType.SPIRIT));
        tag.putInt(TAG_LUCK, data.getTotalStatPoints(owner, StatType.LUCK));
        tag.putInt(TAG_HEALTH, Math.round(owner.getHealth()));
        tag.putInt(TAG_MAX_HEALTH, Math.round(owner.getMaxHealth()));
        tag.putInt(TAG_MANA, Math.round(data.getCurrentMana(owner)));
        tag.putInt(TAG_MAX_MANA, Math.round(data.getMaxMana(owner)));
        tag.putBoolean(TAG_HAS_SNAPSHOT, true);
        tag.putBoolean(TAG_IS_ONLINE, online);
        return tag;
    }

    public static void updateStackWithOwnerData(ItemStack stack, Player owner, PlayerRpgData data) {
        CompoundTag tag = buildOwnerSnapshot(owner, data, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Tarjeta de Aventurero de " + owner.getName().getString())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );
    }

    public static String getOwnerName(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.contains(TAG_OWNER_NAME) ? tag.getString(TAG_OWNER_NAME) : "Aventurero Anónimo";
    }

    public static String getRankName(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.contains(TAG_RANK) ? tag.getString(TAG_RANK) : "F";
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() || !(entity instanceof ServerPlayer holder)) {
            return;
        }
        if (holder.tickCount % 40 != 0) {
            return;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_OWNER_UUID)) {
            return;
        }
        if (!holder.getUUID().toString().equals(tag.getString(TAG_OWNER_UUID))) {
            return;
        }

        PlayerRpgData data = holder.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            updateStackWithOwnerData(stack, holder, data);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer viewer) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            CompoundTag payloadTag;
            if (!tag.contains(TAG_OWNER_UUID)) {
                // Unlinked card (e.g., obtained via creative): bind to whoever registers it first
                PlayerRpgData viewerData = viewer.getData(ModAttachments.PLAYER_RPG_DATA);
                if (viewerData != null) {
                    updateStackWithOwnerData(stack, viewer, viewerData);
                    payloadTag = buildOwnerSnapshot(viewer, viewerData, true);
                } else {
                    payloadTag = tag;
                }
            } else {
                UUID ownerUuid = null;
                try {
                    ownerUuid = UUID.fromString(tag.getString(TAG_OWNER_UUID));
                } catch (IllegalArgumentException ignored) {
                }

                ServerPlayer onlineOwner = (ownerUuid != null && viewer.getServer() != null)
                        ? viewer.getServer().getPlayerList().getPlayer(ownerUuid)
                        : null;

                if (onlineOwner != null) {
                    PlayerRpgData ownerData = onlineOwner.getData(ModAttachments.PLAYER_RPG_DATA);
                    if (ownerData != null) {
                        updateStackWithOwnerData(stack, onlineOwner, ownerData);
                        payloadTag = buildOwnerSnapshot(onlineOwner, ownerData, true);
                    } else {
                        payloadTag = tag.copy();
                        payloadTag.putBoolean(TAG_IS_ONLINE, true);
                    }
                } else {
                    payloadTag = tag.copy();
                    payloadTag.putBoolean(TAG_IS_ONLINE, false);
                }
            }

            PacketDistributor.sendToPlayer(viewer, new OpenAdventurerCardPayload(payloadTag));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.2f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6══════ [ Tarjeta de Aventurero ] ══════"));
        tooltip.add(Component.literal("§7Titular: §f" + getOwnerName(stack)));
        tooltip.add(Component.literal("§7Rango de Gremio: §eRango " + getRankName(stack)));
        tooltip.add(Component.literal("§8Clic derecho para inspeccionar la ficha oficial."));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

