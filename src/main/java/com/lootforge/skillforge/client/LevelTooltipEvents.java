package com.lootforge.skillforge.client;

import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.levels.LevelsConfig;
import com.lootforge.skillforge.levels.LevelsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SkillForgeMod.MOD_ID, value = Dist.CLIENT)
public final class LevelTooltipEvents {
    private LevelTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        LevelsHelper.EffectsLookup lookup = LevelsHelper.getTooltipEffects(stack);
        if (lookup.isEmpty()) {
            return;
        }

        int level = LevelsHelper.getItemLevel(stack);
        if (level <= 0) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());

        if (LevelsHelper.isItemMaxedOut(stack)) {
            tooltip.add(Component.translatable("tooltip.skillforge.maxedout").withStyle(ChatFormatting.GOLD));
        }

        ChatFormatting levelColor = switch (level) {
            case 1 -> ChatFormatting.WHITE;
            case 2 -> ChatFormatting.BLUE;
            case 3 -> ChatFormatting.GREEN;
            case 4 -> ChatFormatting.DARK_RED;
            case 5 -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.GOLD;
        };
        tooltip.add(Component.translatable("tooltip.skillforge.level", level).withStyle(levelColor));

        if (lookup.isArmor()) {
            tooltip.add(Component.translatable("tooltip.skillforge.armoreffects").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.skillforge.effects").withStyle(ChatFormatting.GRAY));
        }

        List<Component> uniqueLines = new ArrayList<>();
        for (Map.Entry<String, LevelsConfig.EffectData> entry : lookup.effects().entrySet()) {
            String effectName = entry.getKey();
            LevelsConfig.EffectData data = entry.getValue();
            if (data == null) {
                continue;
            }
            String template = Language.getInstance().getOrDefault("tooltip.skillforge.effect." + effectName);
            String formatted = ">" + template
                    .replace("${chance}", Integer.toString(data.chance))
                    .replace("${amount}", Integer.toString(data.amount))
                    .replace("${time}", Integer.toString(data.time));

            Component line = Component.literal(formatted).withStyle(ChatFormatting.GRAY);
            if (effectName.contains("unique")) {
                uniqueLines.add(line);
            } else {
                tooltip.add(line);
            }
        }

        if (!uniqueLines.isEmpty()) {
            tooltip.add(Component.empty());
            if (lookup.isArmor()) {
                tooltip.add(Component.translatable("tooltip.skillforge.hasUniqueArmor"));
            } else {
                tooltip.add(Component.translatable("tooltip.skillforge.hasUnique"));
            }
            tooltip.addAll(uniqueLines);
        }
    }
}
