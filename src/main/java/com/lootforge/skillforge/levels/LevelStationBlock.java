package com.lootforge.skillforge.levels;

import com.lootforge.skillforge.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class LevelStationBlock extends BaseEntityBlock {
    public LevelStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LevelStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, ModRegistries.LEVEL_STATION_BE.get(), LevelStationBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LevelStationBlockEntity station) {
                NetworkHooks.openScreen(serverPlayer, station, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LevelStationBlockEntity station) {
                Containers.dropContents(level, pos, station);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx > -2 && dx < 2 && dz == -1) {
                    dz = 2;
                }
                if (random.nextInt(16) != 0) {
                    continue;
                }
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos midPos = pos.offset(dx / 2, dy, dz / 2);
                    if (!level.isEmptyBlock(midPos)) {
                        break;
                    }
                    level.addParticle(
                            ParticleTypes.ENCHANT,
                            (double) pos.getX() + 0.5D,
                            (double) pos.getY() + 2.0D,
                            (double) pos.getZ() + 0.5D,
                            (double) ((float) dx + random.nextFloat()) - 0.5D,
                            (double) ((float) dy - random.nextFloat() - 1.0F),
                            (double) ((float) dz + random.nextFloat()) - 0.5D
                    );
                }
            }
        }
    }
}
