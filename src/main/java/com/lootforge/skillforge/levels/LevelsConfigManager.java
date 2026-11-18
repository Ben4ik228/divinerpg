package com.lootforge.skillforge.levels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.network.ModNetwork;
import com.lootforge.skillforge.network.SyncLevelsConfigPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LevelsConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = SkillForgeMod.MOD_ID + "_levels.json";
    private static final String DEFAULT_RESOURCE_PATH = "/data/" + SkillForgeMod.MOD_ID + "/default_levels_config.json";

    private static volatile LevelsConfig.ResolvedConfig currentConfig = LevelsConfig.ResolvedConfig.empty();
    private static volatile String currentJsonString = "{}";

    private LevelsConfigManager() {
    }

    public static LevelsConfig.ResolvedConfig getConfig() {
        return currentConfig;
    }

    public static String getJsonString() {
        return currentJsonString;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
    }

    public static void loadOrCreateDefault() {
        Path configPath = getConfigPath();
        try {
            if (!Files.exists(configPath) || Files.size(configPath) == 0L) {
                Files.createDirectories(configPath.getParent());
                String defaultJson = readBundledDefaultJson();
                Files.writeString(configPath, defaultJson, StandardCharsets.UTF_8);
                LOGGER.info("Created default OmegaLevels config at {}", configPath);
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            applyJson(json);
        } catch (Exception ex) {
            LOGGER.error("Failed to load {}, falling back to bundled defaults", configPath, ex);
            applyJson(readBundledDefaultJson());
        }
    }

    public static void reloadAndSyncAll(MinecraftServer server) {
        loadOrCreateDefault();
        if (server != null) {
            ModNetwork.sendToAll(new SyncLevelsConfigPacket(currentJsonString));
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player != null) {
            ModNetwork.sendToPlayer(player, new SyncLevelsConfigPacket(currentJsonString));
        }
    }

    public static void applyJsonFromNetwork(String json) {
        applyJson(json);
    }

    private static void applyJson(String json) {
        if (json == null || json.isBlank()) {
            currentJsonString = "{}";
            currentConfig = LevelsConfig.ResolvedConfig.empty();
            return;
        }
        try {
            LevelsConfig.RawConfig raw = GSON.fromJson(json, LevelsConfig.RawConfig.class);
            currentConfig = LevelsConfig.ResolvedConfig.fromRaw(raw);
            currentJsonString = json;
        } catch (Exception ex) {
            LOGGER.error("Failed to parse OmegaLevels JSON config", ex);
        }
    }

    private static String readBundledDefaultJson() {
        try (InputStream stream = LevelsConfigManager.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            LOGGER.error("Could not read bundled default config {}", DEFAULT_RESOURCE_PATH, ex);
        }
        return "{\n  \"stabilizers\": {\"common\": 14, \"good\": 30},\n  \"improves\": {},\n  \"weapons\": {},\n  \"armors\": []\n}\n";
    }
}
