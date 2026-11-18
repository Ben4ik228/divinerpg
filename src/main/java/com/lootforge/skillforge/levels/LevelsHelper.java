package com.lootforge.skillforge.levels;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;

public final class LevelsHelper {
    public static final String NBT_LEVEL_KEY = "LsLevel";

    private LevelsHelper() {
    }

    public record EffectsLookup(Map<String, LevelsConfig.EffectData> effects, boolean isArmor) {
        public static final EffectsLookup EMPTY = new EffectsLookup(Collections.emptyMap(), false);

        public boolean isEmpty() {
            return effects == null || effects.isEmpty();
        }
    }

    public static boolean isChance(int chance, RandomSource random) {
        if (chance <= 0) {
            return false;
        }
        if (chance >= 100) {
            return true;
        }
        return random.nextDouble() <= (double) chance / 100.0D;
    }

    public static boolean isChance(LevelsConfig.EffectData data, RandomSource random) {
        return data != null && isChance(data.chance, random);
    }

    public static int getItemLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_LEVEL_KEY)) {
            return Math.max(0, tag.getInt(NBT_LEVEL_KEY));
        }
        return 0;
    }

    public static void setItemLevel(ItemStack stack, int level) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (level <= 0) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(NBT_LEVEL_KEY)) {
                tag.remove(NBT_LEVEL_KEY);
                if (tag.isEmpty()) {
                    stack.setTag(null);
                }
            }
        } else {
            stack.getOrCreateTag().putInt(NBT_LEVEL_KEY, level);
        }
    }

    public static boolean isValidUpgradeTarget(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        LevelsConfig.ResolvedConfig cfg = LevelsConfigManager.getConfig();
        if (cfg == null) {
            return false;
        }
        Item item = stack.getItem();
        boolean inWeapons = cfg.weapons.containsKey(item);
        boolean inArmors = cfg.armors.stream().anyMatch(rule -> rule.items.contains(item));
        return (inWeapons || inArmors) && !isItemMaxedOut(stack);
    }

    public static int getItemMaxLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        LevelsConfig.ResolvedConfig cfg = LevelsConfigManager.getConfig();
        if (cfg == null) {
            return 0;
        }
        Item item = stack.getItem();
        int maxLevel = 0;

        Map<Integer, Map<String, LevelsConfig.EffectData>> weaponMap = cfg.weapons.get(item);
        if (weaponMap != null) {
            for (Integer lvl : weaponMap.keySet()) {
                if (lvl != null && lvl > maxLevel) {
                    maxLevel = lvl;
                }
            }
        }

        for (LevelsConfig.ArmorSetRule rule : cfg.armors) {
            if (rule.items.contains(item)) {
                for (Integer lvl : rule.levelEffects.keySet()) {
                    if (lvl != null && lvl > maxLevel) {
                        maxLevel = lvl;
                    }
                }
            }
        }
        return maxLevel;
    }

    public static boolean isItemMaxedOut(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        int current = getItemLevel(stack);
        int max = getItemMaxLevel(stack);
        return max <= 0 || current >= max;
    }

    public static Map<String, LevelsConfig.EffectData> getWeaponEffects(Player player) {
        if (player == null) {
            return Collections.emptyMap();
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return Collections.emptyMap();
        }
        int lvl = getItemLevel(held);
        if (lvl <= 0) {
            return Collections.emptyMap();
        }
        LevelsConfig.ResolvedConfig cfg = LevelsConfigManager.getConfig();
        if (cfg == null) {
            return Collections.emptyMap();
        }
        Map<Integer, Map<String, LevelsConfig.EffectData>> levelMap = cfg.weapons.get(held.getItem());
        if (levelMap == null) {
            return Collections.emptyMap();
        }
        Map<String, LevelsConfig.EffectData> effects = levelMap.get(lvl);
        return effects != null ? effects : Collections.emptyMap();
    }

    public static Map<String, LevelsConfig.EffectData> getArmorEffects(Player player) {
        if (player == null) {
            return Collections.emptyMap();
        }
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);

        if (feet.isEmpty() || legs.isEmpty() || chest.isEmpty() || head.isEmpty()) {
            return Collections.emptyMap();
        }

        int lvl = getItemLevel(feet);
        if (lvl <= 0 || getItemLevel(legs) != lvl || getItemLevel(chest) != lvl || getItemLevel(head) != lvl) {
            return Collections.emptyMap();
        }

        LevelsConfig.ResolvedConfig cfg = LevelsConfigManager.getConfig();
        if (cfg == null) {
            return Collections.emptyMap();
        }

        for (LevelsConfig.ArmorSetRule rule : cfg.armors) {
            if (rule.items.contains(feet.getItem())
                    && rule.items.contains(legs.getItem())
                    && rule.items.contains(chest.getItem())
                    && rule.items.contains(head.getItem())) {
                Map<String, LevelsConfig.EffectData> effects = rule.levelEffects.get(lvl);
                if (effects != null) {
                    return effects;
                }
            }
        }
        return Collections.emptyMap();
    }

    public static EffectsLookup getTooltipEffects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EffectsLookup.EMPTY;
        }
        int lvl = getItemLevel(stack);
        if (lvl <= 0) {
            return EffectsLookup.EMPTY;
        }
        LevelsConfig.ResolvedConfig cfg = LevelsConfigManager.getConfig();
        if (cfg == null) {
            return EffectsLookup.EMPTY;
        }

        Map<Integer, Map<String, LevelsConfig.EffectData>> weaponMap = cfg.weapons.get(stack.getItem());
        if (weaponMap != null && weaponMap.containsKey(lvl)) {
            Map<String, LevelsConfig.EffectData> effects = weaponMap.get(lvl);
            return new EffectsLookup(effects != null ? effects : Collections.emptyMap(), false);
        }

        for (LevelsConfig.ArmorSetRule rule : cfg.armors) {
            if (rule.items.contains(stack.getItem()) && rule.levelEffects.containsKey(lvl)) {
                Map<String, LevelsConfig.EffectData> effects = rule.levelEffects.get(lvl);
                return new EffectsLookup(effects != null ? effects : Collections.emptyMap(), true);
            }
        }

        return EffectsLookup.EMPTY;
    }

    public static int getUniformArmorSetLevel(Player player) {
        if (player == null) {
            return 0;
        }
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (feet.isEmpty() || legs.isEmpty() || chest.isEmpty() || head.isEmpty()) {
            return 0;
        }
        int lvl = getItemLevel(feet);
        if (lvl > 0 && getItemLevel(legs) == lvl && getItemLevel(chest) == lvl && getItemLevel(head) == lvl) {
            return lvl;
        }
        return 0;
    }
}
