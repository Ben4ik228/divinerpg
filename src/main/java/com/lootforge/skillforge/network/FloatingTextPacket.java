package com.lootforge.skillforge.network;

import com.lootforge.skillforge.client.DodgeTextParticle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FloatingTextPacket {
    private final double x;
    private final double y;
    private final double z;
    private final String text;
    private final int rgbColor;

    public FloatingTextPacket(double x, double y, double z, String text, int rgbColor) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.text = text;
        this.rgbColor = rgbColor;
    }

    public static void encode(FloatingTextPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeUtf(msg.text, 64);
        buf.writeInt(msg.rgbColor);
    }

    public static FloatingTextPacket decode(FriendlyByteBuf buf) {
        return new FloatingTextPacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readUtf(64),
                buf.readInt()
        );
    }

    public static void handle(FloatingTextPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DodgeTextParticle.spawnText(msg.x, msg.y, msg.z, msg.text, msg.rgbColor)
        ));
        ctx.setPacketHandled(true);
    }
}
