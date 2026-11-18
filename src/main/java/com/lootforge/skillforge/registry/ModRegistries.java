package com.lootforge.skillforge.registry;

import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.levels.LevelStationBlock;
import com.lootforge.skillforge.levels.LevelStationBlockEntity;
import com.lootforge.skillforge.levels.LevelStationMenu;
import com.lootforge.skillforge.levels.RuneItem;
import com.lootforge.skillforge.levels.StabilizerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistries {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SkillForgeMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SkillForgeMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SkillForgeMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SkillForgeMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkillForgeMod.MOD_ID);

    public static final RegistryObject<Block> LEVEL_STATION = BLOCKS.register(
            "level_station",
            () -> new LevelStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .sound(SoundType.METAL))
    );

    public static final RegistryObject<Item> LEVEL_STATION_ITEM = ITEMS.register(
            "level_station",
            () -> new BlockItem(LEVEL_STATION.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> GEM_RED = ITEMS.register(
            "gem_red",
            () -> new RuneItem(RuneItem.RuneType.RED)
    );
    public static final RegistryObject<Item> GEM_BLUE = ITEMS.register(
            "gem_blue",
            () -> new RuneItem(RuneItem.RuneType.BLUE)
    );
    public static final RegistryObject<Item> GEM_GREEN = ITEMS.register(
            "gem_green",
            () -> new RuneItem(RuneItem.RuneType.GREEN)
    );
    public static final RegistryObject<Item> GEM_PURPLE = ITEMS.register(
            "gem_purple",
            () -> new RuneItem(RuneItem.RuneType.PURPLE)
    );
    public static final RegistryObject<Item> GEM_ORANGE = ITEMS.register(
            "gem_orange",
            () -> new RuneItem(RuneItem.RuneType.ORANGE)
    );

    public static final RegistryObject<Item> STABILIZER_COMMON = ITEMS.register(
            "stabilizer_common",
            () -> new StabilizerItem(StabilizerItem.StabilizerType.COMMON)
    );
    public static final RegistryObject<Item> STABILIZER_GOOD = ITEMS.register(
            "stabilizer_good",
            () -> new StabilizerItem(StabilizerItem.StabilizerType.GOOD)
    );

    public static final RegistryObject<BlockEntityType<LevelStationBlockEntity>> LEVEL_STATION_BE = BLOCK_ENTITIES.register(
            "level_station",
            () -> BlockEntityType.Builder.of(LevelStationBlockEntity::new, LEVEL_STATION.get()).build(null)
    );

    public static final RegistryObject<MenuType<LevelStationMenu>> LEVEL_STATION_MENU = MENUS.register(
            "level_station",
            () -> IForgeMenuType.create(LevelStationMenu::new)
    );

    public static final RegistryObject<CreativeModeTab> SKILLFORGE_TAB = CREATIVE_TABS.register(
            "skillforge_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.skillforge"))
                    .icon(() -> new ItemStack(LEVEL_STATION_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(LEVEL_STATION_ITEM.get());
                        output.accept(GEM_GREEN.get());
                        output.accept(GEM_BLUE.get());
                        output.accept(GEM_RED.get());
                        output.accept(GEM_PURPLE.get());
                        output.accept(GEM_ORANGE.get());
                        output.accept(STABILIZER_COMMON.get());
                        output.accept(STABILIZER_GOOD.get());
                    })
                    .build()
    );

    private ModRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
