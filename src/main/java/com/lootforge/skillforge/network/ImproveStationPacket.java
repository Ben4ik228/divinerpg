package com.lootforge.skillforge.network;

import com.lootforge.skillforge.levels.LevelStationBlockEntity;
import com.lootforge.skillforge.levels.LevelStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ImproveStationPacket {
    private final BlockPos pos;

    public ImproveStationPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ImproveStationPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static ImproveStationPacket decode(FriendlyByteBuf buf) {
        return new ImproveStationPacket(buf.readBlockPos());
    }

    public static void handle(ImproveStationPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (!(player.containerMenu instanceof LevelStationMenu menu) || !menu.getBlockPos().equals(msg.pos)) {
                return;
            }
            if (player.distanceToSqr(msg.pos.getX() + 0.5D, msg.pos.getY() + 0.5D, msg.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof LevelStationBlockEntity station) {
                station.triggerImprove();
            }
        });
        ctx.setPacketHandled(true);
    }
}
