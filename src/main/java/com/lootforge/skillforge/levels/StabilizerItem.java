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

public class StabilizerItem extends Item {
    public enum StabilizerType {
        COMMON(14, Rarity.UNCOMMON),
        GOOD(30, Rarity.RARE);

        private final int fallbackBonus;
        private final Rarity rarity;

        StabilizerType(int fallbackBonus, Rarity rarity) {
            this.fallbackBonus = fallbackBonus;
            this.rarity = rarity;
        }

        public String configKey() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int fallbackBonus() {
            return fallbackBonus;
        }

        public Rarity rarity() {
            return rarity;
        }
    }

    private final StabilizerType stabilizerType;

    public StabilizerItem(StabilizerType stabilizerType) {
        super(new Item.Properties().rarity(stabilizerType.rarity()));
        this.stabilizerType = stabilizerType;
    }

    public StabilizerType getStabilizerType() {
        return stabilizerType;
    }

    public int getBonusChance() {
        LevelsConfig.ResolvedConfig cfg = LevelsConfigManager.getConfig();
        if (cfg != null && cfg.stabilizers != null) {
            Integer configured = cfg.stabilizers.get(stabilizerType.configKey());
            if (configured != null) {
                return configured;
            }
        }
        return stabilizerType.fallbackBonus();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(
                Component.translatable("tooltip.skillforge.chanceup")
                        .append(" " + getBonusChance() + "%")
                        .withStyle(ChatFormatting.WHITE)
        );
    }
}
