package com.lootforge.skillforge.levels;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class RuneItem extends Item {
    public enum RuneType {
        RED(Rarity.UNCOMMON),
        BLUE(Rarity.RARE),
        GREEN(Rarity.COMMON),
        PURPLE(Rarity.EPIC),
        ORANGE(Rarity.EPIC);

        private final Rarity rarity;

        RuneType(Rarity rarity) {
            this.rarity = rarity;
        }

        public String configKey() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Rarity rarity() {
            return rarity;
        }
    }

    private final RuneType runeType;

    public RuneItem(RuneType runeType) {
        super(new Item.Properties().rarity(runeType.rarity()));
        this.runeType = runeType;
    }

    public RuneType getRuneType() {
        return runeType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        switch (runeType) {
            case RED -> tooltip.add(Component.translatable("tooltip.skillforge.broke").withStyle(ChatFormatting.DARK_RED));
            case BLUE -> tooltip.add(Component.translatable("tooltip.skillforge.blue").withStyle(ChatFormatting.BLUE));
            case GREEN -> {
                tooltip.add(Component.translatable("tooltip.skillforge.green").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.translatable("tooltip.skillforge.green1").withStyle(ChatFormatting.GREEN));
            }
            case PURPLE -> tooltip.add(Component.translatable("tooltip.skillforge.purple").withStyle(ChatFormatting.DARK_PURPLE));
            case ORANGE -> tooltip.add(Component.translatable("tooltip.skillforge.orange").withStyle(ChatFormatting.RED));
        }
    }
}
