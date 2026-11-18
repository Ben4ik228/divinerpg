package com.lootforge.skillforge;

import com.lootforge.skillforge.common.PlayerUpgradeData;
import com.lootforge.skillforge.common.SkillType;
import com.lootforge.skillforge.levels.LevelsConfigManager;
import com.lootforge.skillforge.network.DodgeParticlePacket;
import com.lootforge.skillforge.network.ModNetwork;
import com.lootforge.skillforge.network.SyncUpgradesPacket;
import com.lootforge.skillforge.network.TargetHitPacket;
import com.lootforge.skillforge.registry.ModRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SkillForgeMod.MOD_ID)
public class SkillForgeMod {
    public static final String MOD_ID = "skillforge";

    public SkillForgeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistries.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(this::onAttackEntity);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingKnockBack);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            LevelsConfigManager.loadOrCreateDefault();
        });
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerUpgradeData.applyModifiers(player);
            sync(player);
        }
    }

    private void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerUpgradeData.applyModifiers(player);
            player.setHealth(player.getMaxHealth());
            sync(player);
        }
    }

    private void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerUpgradeData.applyModifiers(player);
            sync(player);
        }
    }

    private void onPlayerClone(final PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        CompoundTag origPersisted = original.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (origPersisted.contains(PlayerUpgradeData.NBT_KEY)) {
            CompoundTag clonePersisted = clone.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            clonePersisted.put(PlayerUpgradeData.NBT_KEY, origPersisted.getCompound(PlayerUpgradeData.NBT_KEY).copy());
            clone.getPersistentData().put(Player.PERSISTED_NBT_TAG, clonePersisted);
        }

        PlayerUpgradeData.applyModifiers(clone);
    }

    // Allow rapid hits by reducing target invulnerability frames before damage check
    private void onAttackEntity(final AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (player.level().isClientSide) return;

        int aspdLvl = PlayerUpgradeData.getLevel(player, SkillType.ATTACK_SPEED);
        if (aspdLvl > 0 && target instanceof LivingEntity living) {
            int maxInvuln = Math.max(11, 20 - aspdLvl);
            if (living.invulnerableTime > maxInvuln) {
                living.invulnerableTime = 0;
            }
        }
    }

    // Dodge player attacks with configured chance and spawn white "Уклон" text particle
    private void onLivingAttack(final LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer defender)) {
            return;
        }
        if (defender.level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker) || attacker == defender) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (defender.invulnerableTime > 10) {
            return;
        }

        double dodgeChance = PlayerUpgradeData.getDodgeChance(defender);
        if (dodgeChance <= 0.0D) {
            return;
        }

        if (defender.getRandom().nextDouble() < dodgeChance) {
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

    // Shorten post-hit i-frames, apply gentle upward pop, and notify client HUD when hitting any mob/player
    private void onLivingHurt(final LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        if (event.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, new TargetHitPacket(target.getId()));
        }

        if (event.getSource().getEntity() instanceof Player player) {
            int aspdLvl = PlayerUpgradeData.getLevel(player, SkillType.ATTACK_SPEED);
            if (aspdLvl > 0) {
                int newInvuln = Math.max(11, 20 - aspdLvl);
                target.invulnerableTime = newInvuln;

                Vec3 motion = target.getDeltaMovement();
                double lift = 0.03 + (aspdLvl * 0.005);
                double newY = Math.min(0.25, Math.max(motion.y, 0.0) + lift);
                target.setDeltaMovement(motion.x * 0.5, newY, motion.z * 0.5);
                target.hurtMarked = true;
            }
        }
    }

    // Reduce knockback strength when player hits targets so they stay close in melee
    private void onLivingKnockBack(final LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity().getLastHurtByMob() instanceof Player) {
            event.setStrength(event.getStrength() * 0.4f);
        }
    }

    public static void sync(ServerPlayer player) {
        int[] levels = PlayerUpgradeData.getAllLevels(player);
        int spentXp = PlayerUpgradeData.getTotalSpentXp(player);
        ModNetwork.sendToPlayer(player, new SyncUpgradesPacket(levels, spentXp));
    }
}
