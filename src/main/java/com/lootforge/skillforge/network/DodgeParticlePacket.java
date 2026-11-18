package com.lootforge.skillforge.network;

import com.lootforge.skillforge.client.DodgeTextParticle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DodgeParticlePacket {
    private final double x;
    private final double y;
    private final double z;

    public DodgeParticlePacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(DodgeParticlePacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static DodgeParticlePacket decode(FriendlyByteBuf buf) {
        return new DodgeParticlePacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(DodgeParticlePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DodgeTextParticle.spawn(msg.x, msg.y, msg.z)
        ));
        ctx.setPacketHandled(true);
    }
}
