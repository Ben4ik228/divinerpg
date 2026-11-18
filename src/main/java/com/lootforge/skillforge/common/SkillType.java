package com.lootforge.skillforge.common;

import java.util.Locale;
import java.util.UUID;

public enum SkillType {
    HEALTH(
            "health",
            "I",
            "\u0417\u0434\u043e\u0440\u043e\u0432\u044c\u0435",
            "\u0416\u0438\u0432\u0443\u0447\u0435\u0441\u0442\u044c \u0441\u0435\u0440\u0434\u0446\u0430",
            3,
            2,
            1,
            2.0D,
            0xFFFB7185,
            UUID.fromString("9e74a110-4b88-4c11-9b10-000000000001")
    ),
    DAMAGE(
            "damage",
            "II",
            "\u0421\u0438\u043b\u0430 \u0443\u0434\u0430\u0440\u0430",
            "\u042f\u0440\u043e\u0441\u0442\u044c \u043a\u043b\u0438\u043d\u043a\u0430",
            5,
            2,
            1,
            1.0D,
            0xFFF59E0B,
            UUID.fromString("9e74a110-4b88-4c11-9b10-000000000002")
    ),
    ATTACK_SPEED(
            "attack_speed",
            "III",
            "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0430\u0442\u0430\u043a\u0438",
            "\u0412\u0438\u0445\u0440\u044c \u0437\u0430\u043c\u0430\u0445\u0430",
            8,
            2,
            1,
            0.40D,
            0xFF38BDF8,
            UUID.fromString("9e74a110-4b88-4c11-9b10-000000000003")
    ),
    MOVE_SPEED(
            "move_speed",
            "IV",
            "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0431\u0435\u0433\u0430",
            "\u041f\u043e\u0441\u0442\u0443\u043f\u044c \u0432\u0435\u0442\u0440\u0430",
            4,
            2,
            1,
            0.08D,
            0xFF34D399,
            UUID.fromString("9e74a110-4b88-4c11-9b10-000000000004")
    ),
    ARMOR(
            "armor",
            "V",
            "\u0411\u0440\u043e\u043d\u044f \u0438 \u0449\u0438\u0442",
            "\u0411\u0430\u0441\u0442\u0438\u043e\u043d \u0442\u0438\u0442\u0430\u043d\u0430",
            3,
            2,
            1,
            1.5D,
            0xFFEAB308,
            UUID.fromString("9e74a110-4b88-4c11-9b10-000000000005")
    ),
    DODGE(
            "dodge",
            "VI",
            "\u0423\u043a\u043b\u043e\u043d",
            "\u041f\u0440\u0438\u0437\u0440\u0430\u0447\u043d\u044b\u0439 \u0448\u0430\u0433",
            1,
            2,
            1,
            0.005D,
            0xFFC084FC,
            UUID.fromString("9e74a110-4b88-4c11-9b10-000000000006")
    );

    public static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("9e74a110-4b88-4c11-9b10-000000000015");
    public static final UUID LUCK_BONUS_UUID = UUID.fromString("9e74a110-4b88-4c11-9b10-000000000016");

    public final String id;
    public final String roman;
    public final String title;
    public final String subtitle;
    public final int maxLevel;
    public final int baseCost;
    public final int costStep;
    public final double perLevel;
    public final int color;
    public final UUID modifierUuid;

    SkillType(
            String id,
            String roman,
            String title,
            String subtitle,
            int maxLevel,
            int baseCost,
            int costStep,
            double perLevel,
            int color,
            UUID modifierUuid
    ) {
        this.id = id;
        this.roman = roman;
        this.title = title;
        this.subtitle = subtitle;
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.costStep = costStep;
        this.perLevel = perLevel;
        this.color = color;
        this.modifierUuid = modifierUuid;
    }

    public int getCostForNextLevel(int currentLevel) {
        return baseCost + currentLevel * costStep;
    }

    public String formatBonus(int level) {
        return switch (this) {
            case HEALTH -> String.format(Locale.US, "%.0f HP", 20.0D + level * perLevel);
            case DAMAGE -> String.format(Locale.US, "+%.1f \u0443\u0440.", level * perLevel);
            case ATTACK_SPEED -> String.format(Locale.US, "+%.0f%%", level * perLevel * 100.0D);
            case MOVE_SPEED -> String.format(Locale.US, "+%.0f%%", level * perLevel * 100.0D);
            case ARMOR -> String.format(Locale.US, "+%.1f \u0437\u0449.", level * perLevel);
            case DODGE -> String.format(Locale.US, "%.1f%%", level * perLevel * 100.0D);
        };
    }

    public String formatStepDelta() {
        return switch (this) {
            case HEALTH -> "+2 HP";
            case DAMAGE -> "+1.0 \u0443\u0440.";
            case ATTACK_SPEED -> "+40%";
            case MOVE_SPEED -> "+8%";
            case ARMOR -> "+1.5 \u0437\u0449.";
            case DODGE -> "+0.5%";
        };
    }
}
