package com.lootforge.skillforge.network;

import com.lootforge.skillforge.levels.LevelsConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncLevelsConfigPacket {
    private static final int MAX_JSON_LENGTH = 262144;
    private final String jsonString;

    public SyncLevelsConfigPacket(String jsonString) {
        this.jsonString = jsonString != null ? jsonString : "{}";
    }

    public static void encode(SyncLevelsConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.jsonString, MAX_JSON_LENGTH);
    }

    public static SyncLevelsConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncLevelsConfigPacket(buf.readUtf(MAX_JSON_LENGTH));
    }

    public static void handle(SyncLevelsConfigPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                LevelsConfigManager.applyJsonFromNetwork(msg.jsonString)
        ));
        ctx.setPacketHandled(true);
    }
}
