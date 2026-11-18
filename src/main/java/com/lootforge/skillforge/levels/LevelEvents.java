package com.lootforge.skillforge.levels;

import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.client.LevelAuraParticles;
import com.lootforge.skillforge.network.DodgeParticlePacket;
import com.lootforge.skillforge.network.FloatingTextPacket;
import com.lootforge.skillforge.network.ModNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SkillForgeMod.MOD_ID)
public final class LevelEvents {
    private static final UUID LEVEL_HEALTH_UUID = UUID.fromString("c90a97d0-8d1e-11e9-bc42-526af7264f64");
    private static final UUID LEVEL_DAMAGE_UUID = UUID.fromString("fcb925a8-aaea-11e9-a2a3-2a2ae2dbcce4");
    private static final UUID LEVEL_REACH_UUID = UUID.fromString("d4b2a190-5f3c-4e8a-9b11-1a2b3c4d5e6f");

    private static final ThreadLocal<Boolean> IS_REFLECTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private LevelEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LevelsConfigManager.loadOrCreateDefault();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("syncLvl")
                        .requires(source -> source.hasPermission(3))
                        .executes(ctx -> {
                            LevelsConfigManager.reloadAndSyncAll(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("\u00a79\u0421\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0430\u0446\u0438\u044f \u043f\u0440\u043e\u043a\u0430\u0447\u0435\u043a..."),
                                    true
                            );
                            return 1;
                        })
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LevelsConfigManager.syncToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    LevelAuraParticles.spawnArmorSetParticles(player)
            );
            return;
        }

        Map<String, LevelsConfig.EffectData> weaponEffects = LevelsHelper.getWeaponEffects(player);
        Map<String, LevelsConfig.EffectData> armorEffects = LevelsHelper.getArmorEffects(player);

        double bonusDamage = getAmount(weaponEffects, "damage");
        double bonusReach = getAmount(weaponEffects, "distance");
        double bonusHealth = getAmount(armorEffects, "health");

        updateModifier(player, Attributes.ATTACK_DAMAGE, LEVEL_DAMAGE_UUID, "skillforge_level_damage", bonusDamage);
        updateModifier(player, ForgeMod.ENTITY_REACH.get(), LEVEL_REACH_UUID, "skillforge_level_reach", bonusReach);
        updateModifier(player, Attributes.MAX_HEALTH, LEVEL_HEALTH_UUID, "skillforge_level_health", bonusHealth);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        LevelsConfig.EffectData speedBreak = weaponEffects.get("speedbreak");
        if (speedBreak != null && speedBreak.amount > 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SPEED,
                    25,
                    Math.max(0, speedBreak.amount - 1),
                    true,
                    false
            ));
        }

        LevelsConfig.EffectData moveSpeed = armorEffects.get("speed");
        if (moveSpeed != null && moveSpeed.amount > 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    25,
                    Math.max(0, moveSpeed.amount - 1),
                    true,
                    false
            ));
        }

        LevelsConfig.EffectData regen = armorEffects.get("regeneration");
        if (regen != null && regen.amount > 0 && player.tickCount % 10 == 0) {
            player.heal((float) regen.amount / 4.0F);
        }

        LevelsConfig.EffectData saturation = armorEffects.get("saturation");
        if (saturation != null && saturation.amount > 0) {
            int intervalTicks = Math.max(1, saturation.time) * 20;
            if (player.tickCount % intervalTicks == 0 && player.getFoodData().needsFood()) {
                player.getFoodData().eat(Math.max(1, saturation.amount / 2), 0.1F);
                ModNetwork.sendToTrackingAndSelf(
                        player,
                        new FloatingTextPacket(
                                player.getX(),
                                player.getY() + player.getBbHeight() * 0.78D,
                                player.getZ(),
                                "\u041d\u0430\u0441\u044b\u0449\u0435\u043d\u0438\u0435",
                                0x10E87A
                        )
                );
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer defender) || defender.level().isClientSide) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || attacker == defender) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || defender.invulnerableTime > 10) {
            return;
        }

        Map<String, LevelsConfig.EffectData> armorEffects = LevelsHelper.getArmorEffects(defender);
        LevelsConfig.EffectData evasion = armorEffects.get("evasion");
        if (evasion == null) {
            evasion = armorEffects.get("unique.evasion");
        }
        if (evasion != null && LevelsHelper.isChance(evasion, defender.getRandom())) {
            event.setCanceled(true);
            defender.level().playSound(
                    null,
                    defender.getX(),
                    defender.getY(),
                    defender.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    0.7F,
                    1.65F
            );
            ModNetwork.sendToTrackingOnly(
                    defender,
                    new DodgeParticlePacket(
                            defender.getX(),
                            defender.getY() + defender.getBbHeight() * 0.74D,
                            defender.getZ()
                    )
            );
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getSource() == null) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity == null) {
            return;
        }

        // Armor defense effects
        if (target instanceof ServerPlayer defender) {
            Map<String, LevelsConfig.EffectData> armorEffects = LevelsHelper.getArmorEffects(defender);

            LevelsConfig.EffectData returnEffect = armorEffects.get("unique.return");
            if (returnEffect != null
                    && !IS_REFLECTING.get()
                    && sourceEntity instanceof LivingEntity livingAttacker
                    && livingAttacker != defender
                    && LevelsHelper.isChance(returnEffect, defender.getRandom())) {
                float reflected = event.getAmount() * (float) returnEffect.amount / 100.0F;
                if (reflected > 0.0F) {
                    try {
                        IS_REFLECTING.set(Boolean.TRUE);
                        livingAttacker.hurt(defender.damageSources().thorns(defender), reflected);
                    } finally {
                        IS_REFLECTING.set(Boolean.FALSE);
                    }
                }
            }

            LevelsConfig.EffectData restore = armorEffects.get("restore");
            if (restore != null && restore.amount > 0 && LevelsHelper.isChance(restore, defender.getRandom())) {
                defender.heal((float) restore.amount);
            }
        }

        // Weapon offense effects
        if (sourceEntity instanceof Player attacker && !IS_REFLECTING.get()) {
            Map<String, LevelsConfig.EffectData> weaponEffects = LevelsHelper.getWeaponEffects(attacker);
            if (weaponEffects.isEmpty()) {
                return;
            }

            if (event.getSource().isIndirect()) {
                LevelsConfig.EffectData dmg = weaponEffects.get("damage");
                if (dmg != null && dmg.amount > 0) {
                    event.setAmount(event.getAmount() + (float) dmg.amount);
                }
            }

            LevelsConfig.EffectData crit = weaponEffects.get("unique.critical");
            if (crit != null && crit.amount > 0 && LevelsHelper.isChance(crit, attacker.getRandom())) {
                event.setAmount(event.getAmount() * (float) crit.amount / 100.0F);
                ModNetwork.sendToTrackingAndSelf(
                        target,
                        new FloatingTextPacket(
                                target.getX(),
                                target.getY() + target.getBbHeight() * 0.78D,
                                target.getZ(),
                                "\u041a\u0440\u0438\u0442!",
                                0xFF4B2B
                        )
                );
            }

            LevelsConfig.EffectData speedAttack = weaponEffects.get("unique.speedAttack");
            if (speedAttack != null && speedAttack.amount > 0) {
                target.invulnerableTime = Math.max(0, target.invulnerableTime - speedAttack.amount);
            }

            LevelsConfig.EffectData fire = weaponEffects.get("fire");
            if (fire != null && fire.time > 0 && LevelsHelper.isChance(fire, attacker.getRandom())) {
                target.setSecondsOnFire(fire.time);
            }

            LevelsConfig.EffectData slow = weaponEffects.get("slow");
            if (slow != null && slow.time > 0 && LevelsHelper.isChance(slow, attacker.getRandom())) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        slow.time * 20,
                        Math.max(0, slow.amount - 1),
                        false,
                        true
                ));
            }

            LevelsConfig.EffectData poison = weaponEffects.get("poison");
            if (poison != null && poison.time > 0 && LevelsHelper.isChance(poison, attacker.getRandom())) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.POISON,
                        poison.time * 20,
                        Math.max(0, poison.amount - 1),
                        false,
                        true
                ));
            }
        }
    }

    private static double getAmount(Map<String, LevelsConfig.EffectData> effects, String key) {
        LevelsConfig.EffectData data = effects.get(key);
        return data != null ? (double) data.amount : 0.0D;
    }

    private static void updateModifier(Player player, Attribute attribute, UUID uuid, String name, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(uuid);
        if (amount <= 0.0D) {
            if (existing != null) {
                instance.removeModifier(uuid);
            }
            return;
        }
        if (existing != null) {
            if (Double.compare(existing.getAmount(), amount) == 0) {
                return;
            }
            instance.removeModifier(uuid);
        }
        instance.addTransientModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
    }
}
