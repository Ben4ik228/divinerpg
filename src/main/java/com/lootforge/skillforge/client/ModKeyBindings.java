package com.lootforge.skillforge.client;

import com.lootforge.skillforge.SkillForgeMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SkillForgeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyBindings {
    public static final KeyMapping OPEN_UPGRADE_MENU = new KeyMapping(
            "key.skillforge.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.skillforge"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_UPGRADE_MENU);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("rpg_hud", RPGGuiIngame.INSTANCE);
    }

    @Mod.EventBusSubscriber(modid = SkillForgeMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null && mc.options.attackIndicator().get() != AttackIndicatorStatus.OFF) {
                mc.options.attackIndicator().set(AttackIndicatorStatus.OFF);
            }
            if (mc.player == null) {
                return;
            }
            while (OPEN_UPGRADE_MENU.consumeClick()) {
                if (mc.screen instanceof UpgradeScreen) {
                    mc.setScreen(null);
                } else if (mc.screen == null) {
                    mc.setScreen(new UpgradeScreen());
                }
            }
        }

        // Immediate client-side trigger when hitting a mob or player in melee
        @SubscribeEvent
        public static void onClientAttackEntity(AttackEntityEvent event) {
            if (event.getEntity().level().isClientSide && event.getTarget() instanceof LivingEntity living) {
                RPGGuiIngame.onHitTarget(living);
            }
        }

        // Hide vanilla health, armor, food, and air bars since they are rendered by the custom top-left HUD
        @SubscribeEvent
        public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()
                    || event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()
                    || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()
                    || event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
                event.setCanceled(true);
            }
        }
    }
}
