package com.lootforge.skillforge.levels;

import com.lootforge.skillforge.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LevelStationMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;
    private final ContainerLevelAccess levelAccess;

    public LevelStationMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                resolveBlockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(1)
        );
    }

    private static Container resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof LevelStationBlockEntity station) {
            return station;
        }
        return new SimpleContainer(4);
    }

    public LevelStationMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModRegistries.LEVEL_STATION_MENU.get(), containerId);
        checkContainerSize(container, 4);
        checkContainerDataCount(data, 1);

        this.container = container;
        this.data = data;

        if (container instanceof LevelStationBlockEntity station && station.getLevel() != null) {
            this.blockPos = station.getBlockPos();
            this.levelAccess = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        } else {
            this.blockPos = BlockPos.ZERO;
            this.levelAccess = ContainerLevelAccess.NULL;
        }

        // Slot 0: Rune
        this.addSlot(new Slot(container, LevelStationBlockEntity.SLOT_RUNE, 23, 11) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RuneItem;
            }
        });

        // Slot 1: Weapon / Armor target
        this.addSlot(new Slot(container, LevelStationBlockEntity.SLOT_TARGET, 45, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LevelsHelper.isValidUpgradeTarget(stack);
            }
        });

        // Slot 2: Stabilizer
        this.addSlot(new Slot(container, LevelStationBlockEntity.SLOT_STABILIZER, 23, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof StabilizerItem;
            }
        });

        // Slot 3: Output (take only)
        this.addSlot(new Slot(container, LevelStationBlockEntity.SLOT_OUTPUT, 125, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public BlockPos getBlockPos() {
        if (container instanceof LevelStationBlockEntity station) {
            return station.getBlockPos();
        }
        return this.blockPos;
    }

    public int getChance() {
        ItemStack rune = this.slots.get(LevelStationBlockEntity.SLOT_RUNE).getItem();
        ItemStack target = this.slots.get(LevelStationBlockEntity.SLOT_TARGET).getItem();
        ItemStack stabilizer = this.slots.get(LevelStationBlockEntity.SLOT_STABILIZER).getItem();
        LevelsConfig.ImproveData improveData = LevelStationBlockEntity.getImproveData(rune, target);
        return LevelStationBlockEntity.getChance(improveData, stabilizer);
    }

    public int getChanceSkipLevel() {
        ItemStack rune = this.slots.get(LevelStationBlockEntity.SLOT_RUNE).getItem();
        ItemStack target = this.slots.get(LevelStationBlockEntity.SLOT_TARGET).getItem();
        LevelsConfig.ImproveData improveData = LevelStationBlockEntity.getImproveData(rune, target);
        return improveData != null ? Math.max(0, Math.min(100, improveData.chanceSkipLevel)) : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (levelAccess != ContainerLevelAccess.NULL) {
            return stillValid(levelAccess, player, ModRegistries.LEVEL_STATION.get());
        }
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            copy = stackInSlot.copy();

            if (index < 4) {
                if (!this.moveItemStackTo(stackInSlot, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;
                for (int targetSlot = 0; targetSlot < 3; targetSlot++) {
                    if (this.slots.get(targetSlot).mayPlace(stackInSlot)) {
                        if (this.moveItemStackTo(stackInSlot, targetSlot, targetSlot + 1, false)) {
                            moved = true;
                            break;
                        }
                    }
                }
                if (!moved) {
                    if (index < 31) {
                        if (!this.moveItemStackTo(stackInSlot, 31, 40, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stackInSlot, 4, 31, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stackInSlot);
        }
        return copy;
    }
}
