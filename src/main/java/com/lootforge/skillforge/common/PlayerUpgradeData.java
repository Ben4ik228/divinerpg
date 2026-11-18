package com.lootforge.skillforge.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class PlayerUpgradeData {
    public static final String NBT_KEY = "skillforge_data";
    private static final String SPENT_XP_KEY = "spent_xp";

    // Реальный шанс уклона: 0.5% на 1 уровне (поставь true только если снова понадобится 100% тест)
    public static final boolean TEST_100_PERCENT_DODGE = false;

    private static int[] clientLevels = new int[SkillType.values().length];
    private static int clientSpentXp = 0;

    private PlayerUpgradeData() {
    }

    public static void applyClientSync(int[] levels, int spentXp) {
        int count = SkillType.values().length;
        int[] copy = new int[count];
        for (int i = 0; i < count && i < levels.length; i++) {
            copy[i] = Math.max(0, Math.min(SkillType.values()[i].maxLevel, levels[i]));
        }
        clientLevels = copy;
        clientSpentXp = Math.max(0, spentXp);
    }

    public static int getClientLevel(SkillType type) {
        return clientLevels[type.ordinal()];
    }

    public static int getClientSpentXp() {
        return clientSpentXp;
    }

    public static int getClientTotalLevels() {
        int sum = 0;
        for (int lvl : clientLevels) {
            sum += lvl;
        }
        return sum;
    }

    public static int getClientPowerScore() {
        return 100 + getClientTotalLevels() * 35;
    }

    private static CompoundTag getOrCreateDataTag(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag data = persisted.getCompound(NBT_KEY);
        persisted.put(NBT_KEY, data);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return data;
    }

    public static int[] getAllLevels(Player player) {
        CompoundTag data = getOrCreateDataTag(player);
        SkillType[] values = SkillType.values();
        int[] levels = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            levels[i] = Math.max(0, Math.min(values[i].maxLevel, data.getInt(values[i].id)));
        }
        return levels;
    }

    public static int getLevel(Player player, SkillType type) {
        return getAllLevels(player)[type.ordinal()];
    }

    public static double getDodgeChance(Player player) {
        int level = getLevel(player, SkillType.DODGE);
        if (level <= 0) {
            return 0.0D;
        }
        if (TEST_100_PERCENT_DODGE) {
            return 1.0D;
        }
        return level * SkillType.DODGE.perLevel;
    }

    public static int getTotalSpentXp(Player player) {
        return Math.max(0, getOrCreateDataTag(player).getInt(SPENT_XP_KEY));
    }

    public static boolean upgradeSkill(ServerPlayer player, SkillType type) {
        CompoundTag data = getOrCreateDataTag(player);
        int current = Math.max(0, data.getInt(type.id));
        if (current >= type.maxLevel) {
            return false;
        }
        int cost = type.getCostForNextLevel(current);
        boolean freeMode = player.isCreative();
        if (!freeMode && player.experienceLevel < cost) {
            return false;
        }
        if (!freeMode) {
            player.giveExperienceLevels(-cost);
            data.putInt(SPENT_XP_KEY, data.getInt(SPENT_XP_KEY) + cost);
        }
        data.putInt(type.id, current + 1);
        applyModifiers(player);
        if (type == SkillType.HEALTH) {
            player.heal((float) type.perLevel);
        }
        return true;
    }

    public static boolean downgradeSkill(ServerPlayer player, SkillType type) {
        CompoundTag data = getOrCreateDataTag(player);
        int current = Math.max(0, data.getInt(type.id));
        if (current <= 0) {
            return false;
        }
        int prevLevel = current - 1;
        int refund = type.getCostForNextLevel(prevLevel);
        data.putInt(type.id, prevLevel);
        int spent = data.getInt(SPENT_XP_KEY);
        int actualRefund = Math.min(spent, refund);
        if (actualRefund > 0) {
            data.putInt(SPENT_XP_KEY, spent - actualRefund);
            if (!player.isCreative()) {
                player.giveExperienceLevels(actualRefund);
            }
        }
        applyModifiers(player);
        return true;
    }

    public static boolean resetAll(ServerPlayer player) {
        CompoundTag data = getOrCreateDataTag(player);
        int spent = Math.max(0, data.getInt(SPENT_XP_KEY));
        boolean anyChanged = spent > 0;
        for (SkillType type : SkillType.values()) {
            if (data.getInt(type.id) > 0) {
                anyChanged = true;
            }
            data.putInt(type.id, 0);
        }
        data.putInt(SPENT_XP_KEY, 0);
        if (spent > 0 && !player.isCreative()) {
            player.giveExperienceLevels(spent);
        }
        applyModifiers(player);
        return anyChanged;
    }

    public static void applyModifiers(Player player) {
        int[] levels = getAllLevels(player);
        for (SkillType type : SkillType.values()) {
            int level = levels[type.ordinal()];
            double amount = level * type.perLevel;
            switch (type) {
                case HEALTH -> setModifier(player, Attributes.MAX_HEALTH, type.modifierUuid, "skillforge_health", amount, AttributeModifier.Operation.ADDITION);
                case DAMAGE -> setModifier(player, Attributes.ATTACK_DAMAGE, type.modifierUuid, "skillforge_damage", amount, AttributeModifier.Operation.ADDITION);
                case ATTACK_SPEED -> setModifier(player, Attributes.ATTACK_SPEED, type.modifierUuid, "skillforge_attack_speed", amount, AttributeModifier.Operation.MULTIPLY_TOTAL);
                case MOVE_SPEED -> setModifier(player, Attributes.MOVEMENT_SPEED, type.modifierUuid, "skillforge_move_speed", amount, AttributeModifier.Operation.MULTIPLY_BASE);
                case ARMOR -> {
                    setModifier(player, Attributes.ARMOR, type.modifierUuid, "skillforge_armor", amount, AttributeModifier.Operation.ADDITION);
                    setModifier(player, Attributes.ARMOR_TOUGHNESS, SkillType.ARMOR_TOUGHNESS_UUID, "skillforge_toughness", level * 0.5D, AttributeModifier.Operation.ADDITION);
                }
                case DODGE -> {
                    removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, type.modifierUuid);
                    removeModifier(player, Attributes.LUCK, SkillType.LUCK_BONUS_UUID);
                }
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void setModifier(
            Player player,
            Attribute attribute,
            UUID uuid,
            String name,
            double amount,
            AttributeModifier.Operation operation
    ) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
        if (amount > 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(uuid, name, amount, operation));
        }
    }

    private static void removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }
}
