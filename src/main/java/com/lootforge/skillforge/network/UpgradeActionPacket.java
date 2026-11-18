package com.lootforge.skillforge.network;

import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.common.PlayerUpgradeData;
import com.lootforge.skillforge.common.SkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpgradeActionPacket {
    public static final int ACTION_UPGRADE = 0;
    public static final int ACTION_DOWNGRADE = 1;
    public static final int ACTION_RESET_ALL = 2;

    private final int action;
    private final int skillOrdinal;

    public UpgradeActionPacket(int action, int skillOrdinal) {
        this.action = action;
        this.skillOrdinal = skillOrdinal;
    }

    public static void encode(UpgradeActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.skillOrdinal);
    }

    public static UpgradeActionPacket decode(FriendlyByteBuf buf) {
        return new UpgradeActionPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(UpgradeActionPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            SkillType[] skills = SkillType.values();
            if (msg.action == ACTION_UPGRADE && msg.skillOrdinal >= 0 && msg.skillOrdinal < skills.length) {
                if (PlayerUpgradeData.upgradeSkill(player, skills[msg.skillOrdinal])) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
            } else if (msg.action == ACTION_DOWNGRADE && msg.skillOrdinal >= 0 && msg.skillOrdinal < skills.length) {
                if (PlayerUpgradeData.downgradeSkill(player, skills[msg.skillOrdinal])) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 0.85F);
                }
            } else if (msg.action == ACTION_RESET_ALL) {
                if (PlayerUpgradeData.resetAll(player)) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.65F, 1.15F);
                }
            }

            SkillForgeMod.sync(player);
        });
        ctx.setPacketHandled(true);
    }
}
