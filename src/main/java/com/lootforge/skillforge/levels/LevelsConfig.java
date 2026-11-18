package com.lootforge.skillforge.levels;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LevelsConfig {
    private LevelsConfig() {
    }

    public static class EffectData {
        public int chance = 100;
        public int amount = 0;
        public int time = 10;
    }

    public static class ImproveData {
        public int chance = 100;
        public int chanceSkipLevel = 0;
        public boolean breakOnFail = false;
        public boolean decreaseLevelOnFail = false;
    }

    public static class ArmorEntry {
        public List<String> key = new ArrayList<>();
        public Map<Integer, Map<String, EffectData>> value = new HashMap<>();
    }

    public static class RawConfig {
        public Map<String, Map<Integer, Map<String, EffectData>>> weapons = new HashMap<>();
        public List<ArmorEntry> armors = new ArrayList<>();
        public Map<String, Map<Integer, ImproveData>> improves = new HashMap<>();
        public Map<String, Integer> stabilizers = new HashMap<>();
    }

    public static class ResolvedConfig {
        public final Map<Item, Map<Integer, Map<String, EffectData>>> weapons;
        public final List<ArmorSetRule> armors;
        public final Map<String, Map<Integer, ImproveData>> improves;
        public final Map<String, Integer> stabilizers;

        public ResolvedConfig(
                Map<Item, Map<Integer, Map<String, EffectData>>> weapons,
                List<ArmorSetRule> armors,
                Map<String, Map<Integer, ImproveData>> improves,
                Map<String, Integer> stabilizers
        ) {
            this.weapons = weapons;
            this.armors = armors;
            this.improves = improves;
            this.stabilizers = stabilizers;
        }

        public static ResolvedConfig empty() {
            return new ResolvedConfig(
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }

        public static ResolvedConfig fromRaw(RawConfig raw) {
            if (raw == null) {
                return empty();
            }

            Map<Item, Map<Integer, Map<String, EffectData>>> resolvedWeapons = new HashMap<>();
            if (raw.weapons != null) {
                for (Map.Entry<String, Map<Integer, Map<String, EffectData>>> entry : raw.weapons.entrySet()) {
                    Item item = resolveItem(entry.getKey());
                    if (item != null && entry.getValue() != null) {
                        resolvedWeapons.put(item, entry.getValue());
                    }
                }
            }

            List<ArmorSetRule> resolvedArmors = new ArrayList<>();
            if (raw.armors != null) {
                for (ArmorEntry entry : raw.armors) {
                    if (entry == null || entry.key == null || entry.value == null) {
                        continue;
                    }
                    Set<Item> setItems = new LinkedHashSet<>();
                    for (String itemId : entry.key) {
                        Item item = resolveItem(itemId);
                        if (item != null) {
                            setItems.add(item);
                        }
                    }
                    if (!setItems.isEmpty()) {
                        resolvedArmors.add(new ArmorSetRule(setItems, entry.value));
                    }
                }
            }

            Map<String, Map<Integer, ImproveData>> resolvedImproves =
                    raw.improves != null ? raw.improves : Collections.emptyMap();
            Map<String, Integer> resolvedStabilizers =
                    raw.stabilizers != null ? raw.stabilizers : Collections.emptyMap();

            return new ResolvedConfig(resolvedWeapons, resolvedArmors, resolvedImproves, resolvedStabilizers);
        }

        private static Item resolveItem(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            ResourceLocation rl = ResourceLocation.tryParse(id.trim());
            if (rl == null) {
                return null;
            }
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            return (item == null || item == Items.AIR) ? null : item;
        }
    }

    public static final class ArmorSetRule {
        public final Set<Item> items;
        public final Map<Integer, Map<String, EffectData>> levelEffects;

        public ArmorSetRule(Set<Item> items, Map<Integer, Map<String, EffectData>> levelEffects) {
            this.items = items;
            this.levelEffects = levelEffects;
        }
    }
}
