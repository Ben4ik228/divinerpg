package com.lootforge.skillforge.levels;

import com.lootforge.skillforge.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class LevelStationBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int TARGET_PROGRESS = 80;
    public static final int SLOT_RUNE = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_STABILIZER = 2;
    public static final int SLOT_OUTPUT = 3;

    private static final int[] SLOTS_TOP = new int[]{SLOT_RUNE, SLOT_STABILIZER};
    private static final int[] SLOTS_BOTTOM = new int[]{SLOT_OUTPUT};
    private static final int[] SLOTS_SIDES = new int[]{SLOT_TARGET};

    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private boolean startImprove = false;
    private int progress = 0;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public LevelStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.LEVEL_STATION_BE.get(), pos, state);
    }

    public void triggerImprove() {
        if (getChance() > 0 && items.get(SLOT_OUTPUT).isEmpty()) {
            this.startImprove = true;
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LevelStationBlockEntity station) {
        if (level.isClientSide) {
            return;
        }

        boolean active = station.progress > 0;
        if (station.startImprove) {
            active = true;
            station.startImprove = false;
        }

        ItemStack rune = station.items.get(SLOT_RUNE);
        ItemStack target = station.items.get(SLOT_TARGET);
        ItemStack output = station.items.get(SLOT_OUTPUT);

        if (active && !rune.isEmpty() && !target.isEmpty() && output.isEmpty() && LevelsHelper.isValidUpgradeTarget(target)) {
            LevelsConfig.ImproveData improveData = getImproveData(rune, target);
            if (improveData != null) {
                station.progress++;
                if (station.progress >= TARGET_PROGRESS) {
                    station.finishImprove(level, pos, improveData);
                } else {
                    station.setChanged();
                }
                return;
            }
        }

        if (station.progress != 0) {
            station.progress = 0;
            station.setChanged();
        }
    }

    private void finishImprove(Level level, BlockPos pos, LevelsConfig.ImproveData improveData) {
        ItemStack rune = removeItem(SLOT_RUNE, 1);
        ItemStack target = removeItem(SLOT_TARGET, 1);
        ItemStack stabilizer = items.get(SLOT_STABILIZER).isEmpty() ? ItemStack.EMPTY : removeItem(SLOT_STABILIZER, 1);

        if (!rune.isEmpty() && !target.isEmpty()) {
            int currentLevel = LevelsHelper.getItemLevel(target);
            int maxLevel = LevelsHelper.getItemMaxLevel(target);
            int effectiveChance = getChance(improveData, stabilizer);

            if (LevelsHelper.isChance(effectiveChance, level.random)) {
                int newLevel = currentLevel + 1;
                if (currentLevel < 4
                        && newLevel < Math.min(5, maxLevel)
                        && LevelsHelper.isChance(improveData.chanceSkipLevel, level.random)) {
                    newLevel++;
                }
                LevelsHelper.setItemLevel(target, Math.min(newLevel, maxLevel));
                setItem(SLOT_OUTPUT, target);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.9F, 1.15F);
            } else if (improveData.breakOnFail) {
                setItem(SLOT_OUTPUT, ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.9F, 0.85F);
            } else {
                if (improveData.decreaseLevelOnFail) {
                    LevelsHelper.setItemLevel(target, Math.max(0, currentLevel - 1));
                    level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.45F, 0.75F);
                } else {
                    level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8F, 0.9F);
                }
                setItem(SLOT_OUTPUT, target);
            }
        }

        this.progress = 0;
        setChanged();
    }

    @Nullable
    public static LevelsConfig.ImproveData getImproveData(ItemStack runeStack, ItemStack targetStack) {
        if (runeStack.isEmpty() || targetStack.isEmpty()) {
            return null;
        }
        if (!(runeStack.getItem() instanceof RuneItem runeItem)) {
            return null;
        }
        if (!LevelsHelper.isValidUpgradeTarget(targetStack)) {
            return null;
        }
        LevelsConfig.ResolvedConfig cfg = LevelsConfigManager.getConfig();
        if (cfg == null || cfg.improves == null) {
            return null;
        }
        Map<Integer, LevelsConfig.ImproveData> runeImproves = cfg.improves.get(runeItem.getRuneType().configKey());
        if (runeImproves == null) {
            return null;
        }
        int nextLevel = LevelsHelper.getItemLevel(targetStack) + 1;
        RuneItem.RuneType runeType = runeItem.getRuneType();
        if (nextLevel <= 5) {
            if (runeType == RuneItem.RuneType.PURPLE || runeType == RuneItem.RuneType.ORANGE) {
                return null;
            }
        } else if (nextLevel == 6) {
            if (runeType != RuneItem.RuneType.PURPLE) {
                return null;
            }
        } else if (nextLevel == 7) {
            if (runeType != RuneItem.RuneType.ORANGE) {
                return null;
            }
        } else {
            return null;
        }
        return runeImproves.get(nextLevel);
    }

    public static int getChance(LevelsConfig.ImproveData improveData, ItemStack stabilizerStack) {
        if (improveData == null) {
            return 0;
        }
        int bonus = 0;
        if (!stabilizerStack.isEmpty() && stabilizerStack.getItem() instanceof StabilizerItem stab) {
            bonus = stab.getBonusChance();
        }
        return Math.max(0, Math.min(100, improveData.chance + bonus));
    }

    public int getChance() {
        LevelsConfig.ImproveData data = getImproveData(items.get(SLOT_RUNE), items.get(SLOT_TARGET));
        return getChance(data, items.get(SLOT_STABILIZER));
    }

    public int getChanceSkipLevel() {
        LevelsConfig.ImproveData data = getImproveData(items.get(SLOT_RUNE), items.get(SLOT_TARGET));
        return data != null ? Math.max(0, Math.min(100, data.chanceSkipLevel)) : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("Progress", this.progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(4, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items);
        this.progress = tag.getInt("Progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.skillforge.level_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LevelStationMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                (double) this.worldPosition.getX() + 0.5D,
                (double) this.worldPosition.getY() + 0.5D,
                (double) this.worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case SLOT_RUNE -> stack.getItem() instanceof RuneItem;
            case SLOT_TARGET -> LevelsHelper.isValidUpgradeTarget(stack);
            case SLOT_STABILIZER -> stack.getItem() instanceof StabilizerItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_BOTTOM;
        }
        if (side == Direction.UP) {
            return SLOTS_TOP;
        }
        return SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }
}
