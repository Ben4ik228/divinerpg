package com.lootforge.skillforge.client;

import com.lootforge.skillforge.levels.LevelsHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public final class LevelAuraParticles {
    private static final DustParticleOptions RED_AURA = new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.05F), 0.9F);
    private static final DustParticleOptions PURPLE_AURA = new DustParticleOptions(new Vector3f(0.55F, 0.05F, 0.78F), 0.9F);
    private static final DustParticleOptions WHITE_AURA = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 0.85F);
    private static final DustParticleOptions YELLOW_AURA = new DustParticleOptions(new Vector3f(1.0F, 0.92F, 0.10F), 1.0F);

    private LevelAuraParticles() {
    }

    public static void spawnArmorSetParticles(Player player) {
        if (player == null) {
            return;
        }
        Level level = player.level();
        if (!level.isClientSide) {
            return;
        }
        int setLevel = LevelsHelper.getUniformArmorSetLevel(player);
        if (setLevel <= 0) {
            return;
        }

        RandomSource rand = player.getRandom();
        switch (setLevel) {
            case 1 -> {
                if (rand.nextInt(15) == 0) {
                    spawnAroundPlayer(level, player, rand, ParticleTypes.FIREWORK, 0.0D);
                }
            }
            case 2 -> {
                if (rand.nextInt(3) == 0) {
                    spawnAroundPlayer(level, player, rand, ParticleTypes.SPLASH, 0.0D);
                }
            }
            case 3 -> {
                if (rand.nextInt(12) == 0) {
                    spawnAroundPlayer(level, player, rand, ParticleTypes.HAPPY_VILLAGER, 0.0D);
                }
            }
            case 4 -> {
                if (rand.nextInt(7) == 0) {
                    spawnAroundPlayer(level, player, rand, RED_AURA, 0.04D);
                }
            }
            case 5 -> {
                if (rand.nextInt(8) == 0) {
                    spawnAroundPlayer(level, player, rand, PURPLE_AURA, 0.04D);
                    spawnAroundPlayer(level, player, rand, ParticleTypes.PORTAL, 0.08D);
                }
            }
            case 6 -> {
                if (rand.nextInt(8) == 0) {
                    spawnAroundPlayer(level, player, rand, WHITE_AURA, 0.06D);
                    spawnAroundPlayer(level, player, rand, ParticleTypes.ENCHANT, 0.08D);
                }
            }
            default -> {
                if (rand.nextInt(8) == 0) {
                    spawnAroundPlayer(level, player, rand, YELLOW_AURA, 0.05D);
                }
            }
        }
    }

    private static void spawnAroundPlayer(
            Level level,
            Player player,
            RandomSource rand,
            ParticleOptions particle,
            double speedScale
    ) {
        double spread = player.getBbWidth() + 1.5D;
        double x = player.getX() + (rand.nextDouble() - 0.5D) * spread;
        double y = player.getY() + rand.nextDouble() * (player.getBbHeight() + 0.5D);
        double z = player.getZ() + (rand.nextDouble() - 0.5D) * spread;
        double vx = speedScale > 0.0D ? rand.nextGaussian() * speedScale : 0.0D;
        double vy = speedScale > 0.0D ? rand.nextGaussian() * speedScale : 0.0D;
        double vz = speedScale > 0.0D ? rand.nextGaussian() * speedScale : 0.0D;
        level.addParticle(particle, x, y, z, vx, vy, vz);
    }
}
