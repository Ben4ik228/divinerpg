package com.lootforge.skillforge.network;

import com.lootforge.skillforge.common.PlayerUpgradeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncUpgradesPacket {
    private final int[] levels;
    private final int spentXp;

    public SyncUpgradesPacket(int[] levels, int spentXp) {
        this.levels = levels;
        this.spentXp = spentXp;
    }

    public static void encode(SyncUpgradesPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.levels.length);
        for (int lvl : msg.levels) {
            buf.writeVarInt(lvl);
        }
        buf.writeVarInt(msg.spentXp);
    }

    public static SyncUpgradesPacket decode(FriendlyByteBuf buf) {
        int len = Math.min(32, Math.max(0, buf.readVarInt()));
        int[] lvls = new int[len];
        for (int i = 0; i < len; i++) {
            lvls[i] = buf.readVarInt();
        }
        int spent = buf.readVarInt();
        return new SyncUpgradesPacket(lvls, spent);
    }

    public static void handle(SyncUpgradesPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                PlayerUpgradeData.applyClientSync(msg.levels, msg.spentXp)
        ));
        ctx.setPacketHandled(true);
    }
}
