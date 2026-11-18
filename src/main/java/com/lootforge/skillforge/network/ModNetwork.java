package com.lootforge.skillforge.network;

import com.lootforge.skillforge.SkillForgeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SkillForgeMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(UpgradeActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpgradeActionPacket::encode)
                .decoder(UpgradeActionPacket::decode)
                .consumerMainThread(UpgradeActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncUpgradesPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncUpgradesPacket::encode)
                .decoder(SyncUpgradesPacket::decode)
                .consumerMainThread(SyncUpgradesPacket::handle)
                .add();

        CHANNEL.messageBuilder(TargetHitPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TargetHitPacket::encode)
                .decoder(TargetHitPacket::decode)
                .consumerMainThread(TargetHitPacket::handle)
                .add();

        CHANNEL.messageBuilder(DodgeParticlePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DodgeParticlePacket::encode)
                .decoder(DodgeParticlePacket::decode)
                .consumerMainThread(DodgeParticlePacket::handle)
                .add();

        CHANNEL.messageBuilder(ImproveStationPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ImproveStationPacket::encode)
                .decoder(ImproveStationPacket::decode)
                .consumerMainThread(ImproveStationPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncLevelsConfigPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLevelsConfigPacket::encode)
                .decoder(SyncLevelsConfigPacket::decode)
                .consumerMainThread(SyncLevelsConfigPacket::handle)
                .add();

        CHANNEL.messageBuilder(FloatingTextPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FloatingTextPacket::encode)
                .decoder(FloatingTextPacket::decode)
                .consumerMainThread(FloatingTextPacket::handle)
                .add();
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingOnly(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    public static void sendToTrackingAndSelf(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
