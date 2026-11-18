package com.lootforge.skillforge.network;

import com.lootforge.skillforge.client.RPGGuiIngame;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TargetHitPacket {
    private final int entityId;

    public TargetHitPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(TargetHitPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static TargetHitPacket decode(FriendlyByteBuf buf) {
        return new TargetHitPacket(buf.readInt());
    }

    public static void handle(TargetHitPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity entity = mc.level.getEntity(packet.entityId);
                if (entity instanceof LivingEntity living) {
                    RPGGuiIngame.onHitTarget(living);
                }
            }
        }));
        context.setPacketHandled(true);
    }
}
