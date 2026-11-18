package com.lootforge.skillforge.client;

import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.common.PlayerUpgradeData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Matrix4f;

public class RPGGuiIngame implements IGuiOverlay {

    public static final RPGGuiIngame INSTANCE = new RPGGuiIngame();

    public static final ResourceLocation BACK_TEXTURE = new ResourceLocation(
            SkillForgeMod.MOD_ID,
            "textures/gui/back.png"
    );

    public static final ResourceLocation HEART_ICON = new ResourceLocation(
            SkillForgeMod.MOD_ID,
            "textures/gui/hud_heart_cyan.png"
    );

    public static final ResourceLocation ARMOR_ICON = new ResourceLocation(
            SkillForgeMod.MOD_ID,
            "textures/gui/hud_armor.png"
    );

    private static final int BACK_TEX_W = 1348;
    private static final int BACK_TEX_H = 285;

    // Target HUD state (triggered when hitting a mob or player)
    private static LivingEntity lastHitTarget = null;
    private static long lastHitTimestamp = 0L;
    private static float animatedTargetHp = -1.0f;

    public static void onHitTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        if (lastHitTarget != target) {
            lastHitTarget = target;
            animatedTargetHp = target.getHealth();
        }
        lastHitTimestamp = System.currentTimeMillis();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 1. Compact Top-Left Player HUD built on back.png
        renderTopLeftPlayerHud(guiGraphics, mc, mc.player);

        // 2. Compact Top-Center Target Health Bar HUD (shown when hitting a mob or player)
        renderHitTargetHud(guiGraphics, mc, screenWidth);

        RenderSystem.disableBlend();
    }

    private void renderTopLeftPlayerHud(GuiGraphics guiGraphics, Minecraft mc, Player player) {
        Font fr = mc.font;

        int startX = 5;
        int startY = 5;

        // A. Compact 18x18 Player Head Avatar on the far left
        int faceSize = 18;
        guiGraphics.fill(startX - 1, startY - 1, startX + faceSize + 1, startY + faceSize + 1, 0x880D0D10);
        if (mc.player != null) {
            PlayerFaceRenderer.draw(guiGraphics, mc.player.getSkinTextureLocation(), startX, startY, faceSize);
        }

        // B. back.png scaled down to compact size (128 x 27 GUI px) right next to the face
        float scale = 0.095f;
        int backX = startX + faceSize + 3;
        int backY = startY;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(backX, backY, 0.0f);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(BACK_TEXTURE, 0, 0, 0, 0, BACK_TEX_W, BACK_TEX_H, BACK_TEX_W, BACK_TEX_H);

        // C. Top Slanted Red Health Bar inside back.png (y = 10..128)
        float currHp = Math.max(0.0f, player.getHealth());
        float absorb = Math.max(0.0f, player.getAbsorptionAmount());
        float maxHp = Math.max(1.0f, player.getMaxHealth());
        float hpRatio = Mth.clamp(currHp / maxHp, 0.0f, 1.0f);

        float hpTopLeftX = 11.0f;
        float hpBotLeftX = 55.0f;
        float hpTopY = 10.0f;
        float hpBotY = 128.0f;
        float hpFullW = 1272.0f;

        // Dark crimson backing for missing health
        drawSlantedQuad(guiGraphics,
                hpTopLeftX, hpTopY,
                hpTopLeftX + hpFullW, hpTopY,
                hpBotLeftX + hpFullW, hpBotY,
                hpBotLeftX, hpBotY,
                0x77260606);

        // Filled red health bar
        float hpFillW = hpFullW * hpRatio;
        if (hpFillW > 1.0f) {
            int mainRedColor = 0xECDC1E1E;
            int highlightRedColor = 0x44FF8A8A;
            if (player.hasEffect(MobEffects.POISON)) {
                mainRedColor = 0xEC689F38;
                highlightRedColor = 0x44C5E1A5;
            } else if (player.hasEffect(MobEffects.WITHER)) {
                mainRedColor = 0xEC373737;
                highlightRedColor = 0x44757575;
            }

            drawSlantedQuad(guiGraphics,
                    hpTopLeftX, hpTopY,
                    hpTopLeftX + hpFillW, hpTopY,
                    hpBotLeftX + hpFillW, hpBotY,
                    hpBotLeftX, hpBotY,
                    mainRedColor);

            // Subtle top highlight
            drawSlantedQuad(guiGraphics,
                    hpTopLeftX, hpTopY,
                    hpTopLeftX + hpFillW, hpTopY,
                    hpTopLeftX + 8.0f + hpFillW, hpTopY + 22.0f,
                    hpTopLeftX + 8.0f, hpTopY + 22.0f,
                    highlightRedColor);
        }

        // Absorption golden bar overlay if player has golden hearts
        if (absorb > 0.0f) {
            float absRatio = Mth.clamp(absorb / maxHp, 0.0f, 1.0f);
            float absW = hpFullW * absRatio;
            drawSlantedQuad(guiGraphics,
                    hpBotLeftX - 6.0f, hpBotY - 18.0f,
                    hpBotLeftX - 6.0f + absW, hpBotY - 18.0f,
                    hpBotLeftX + absW, hpBotY,
                    hpBotLeftX, hpBotY,
                    0xEEFFB300);
        }

        // D. Middle-Left Thin Slanted Bar: Yellow Slanted Stripes (Food Level)
        float foodTopLeftX = 55.0f;
        float foodBotLeftX = 37.0f;
        float foodTopY = 156.0f;
        float foodBotY = 206.0f;
        float foodFullW = 860.0f;

        int totalSegments = 20;
        float stepW = foodFullW / totalSegments;
        float segW = stepW * 0.68f;
        int filledSegments = player.getFoodData().getFoodLevel();

        int activeColor = player.hasEffect(MobEffects.HUNGER) ? 0xF2799C36 : 0xF2F2C94C;

        for (int s = 0; s < totalSegments; s++) {
            float sxTop = foodTopLeftX + s * stepW;
            float sxBot = foodBotLeftX + s * stepW;
            int segColor = (s < filledSegments) ? activeColor : 0x551A160C;
            drawSlantedQuad(guiGraphics,
                    sxTop, foodTopY,
                    sxTop + segW, foodTopY,
                    sxBot + segW, foodBotY,
                    sxBot, foodBotY,
                    segColor);
        }

        // E. Small Cyan Heart Icon inside left of Red Health Bar
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(HEART_ICON, 88, 26, 0, 0, 84, 84, 9, 9);

        // F. White HP Number inside right of Red Health Bar
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(8.8f, 8.8f, 1.0f);
        String hpNumStr = String.valueOf((int) Math.ceil(currHp + absorb));
        int hpNumW = fr.width(hpNumStr);
        guiGraphics.drawString(fr, hpNumStr, 144 - hpNumW, 4, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();

        guiGraphics.pose().popPose();

        // G. Compact Armor Icon + Value directly below back.png on the left
        int armorY = backY + Math.round(BACK_TEX_H * scale) - 3;
        int armorX = backX + 4;
        int armorVal = player.getArmorValue();

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(ARMOR_ICON, armorX, armorY, 0, 0, 9, 9, 9, 9);
        guiGraphics.drawString(fr, String.valueOf(armorVal), armorX + 12, armorY + 1, 0xFFFFFFFF, true);

        // Show underwater air value next to armor when submerged
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            int airPct = Math.max(0, (int) Math.ceil((player.getAirSupply() * 100.0f) / player.getMaxAirSupply()));
            guiGraphics.drawString(fr, "§b" + airPct + "%", armorX + 32, armorY + 1, 0xFF4FC3F7, true);
        }
    }

    private void renderHitTargetHud(GuiGraphics guiGraphics, Minecraft mc, int screenWidth) {
        if (lastHitTarget == null || !lastHitTarget.isAlive()) {
            lastHitTarget = null;
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastHitTimestamp;
        long showDuration = 3500L;
        long fadeDuration = 500L;

        if (elapsed > showDuration + fadeDuration) {
            lastHitTarget = null;
            return;
        }

        float alpha = 1.0f;
        if (elapsed > showDuration) {
            alpha = 1.0f - (float) (elapsed - showDuration) / (float) fadeDuration;
        }
        alpha = Mth.clamp(alpha, 0.05f, 1.0f);

        LivingEntity target = lastHitTarget;
        float actualHp = Math.max(0.0f, target.getHealth() + target.getAbsorptionAmount());
        float maxHp = Math.max(1.0f, target.getMaxHealth() + target.getAbsorptionAmount());

        if (animatedTargetHp < 0.0f) {
            animatedTargetHp = actualHp;
        }
        animatedTargetHp += (actualHp - animatedTargetHp) * 0.25f;
        float hpRatio = Mth.clamp(animatedTargetHp / maxHp, 0.0f, 1.0f);

        Font fr = mc.font;

        int boxW = 118;
        int boxH = 22;
        int boxX = (screenWidth - boxW) / 2;
        int boxY = 6;

        int bgAlpha = ((int) (0x88 * alpha)) << 24;
        int textAlpha = ((int) (0xFF * alpha)) << 24;

        // Translucent dark slate background card
        guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, bgAlpha | 0x16161A);
        guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 1, (((int) (0x44 * alpha)) << 24) | 0xFFFFFF);

        // Target Level on top-right
        int targetLvl;
        if (target instanceof Player targetPlayer) {
            int sum = 0;
            if (targetPlayer == mc.player) {
                sum = PlayerUpgradeData.getClientTotalLevels();
            } else {
                for (int lvl : PlayerUpgradeData.getAllLevels(targetPlayer)) {
                    sum += lvl;
                }
            }
            targetLvl = Math.max(1, sum > 0 ? sum : targetPlayer.experienceLevel);
        } else {
            targetLvl = Math.max(1, Math.round(target.getMaxHealth() / 20.0f));
        }

        String lvlStr = String.valueOf(targetLvl);
        int lvlW = fr.width(lvlStr);
        guiGraphics.drawString(fr, lvlStr, boxX + boxW - lvlW - 4, boxY + 2, textAlpha | 0xFFFFFF, true);

        // Target Name on top-left
        String rawName = target.getDisplayName().getString();
        String name = fr.plainSubstrByWidth(rawName, boxW - lvlW - 12);
        guiGraphics.drawString(fr, name, boxX + 4, boxY + 2, textAlpha | 0xFFFFFF, true);

        // Bottom Red Health Bar inside the card
        int barX = boxX + 2;
        int barY = boxY + 12;
        int barW = boxW - 4;
        int barH = 8;

        // Dark track
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, (((int) (0x77 * alpha)) << 24) | 0x2E1010);

        // Red health fill
        int fillW = (int) (barW * hpRatio);
        if (fillW > 0) {
            int redCol = (((int) (0xEC * alpha)) << 24) | 0xD61A1A;
            guiGraphics.fill(barX, barY, barX + fillW, barY + barH, redCol);
        }

        // Centered HP text inside the red bar
        String hpStr = (actualHp % 1.0f == 0 && maxHp % 1.0f == 0)
                ? String.format("%.0f/%.0f", actualHp, maxHp)
                : String.format("%.1f/%.0f", actualHp, maxHp);
        int hpStrW = fr.width(hpStr);
        guiGraphics.drawString(fr, hpStr, barX + (barW - hpStrW) / 2, barY, textAlpha | 0xFFFFFF, true);
    }

    private void drawSlantedQuad(
            GuiGraphics gui,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4,
            int color
    ) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Matrix4f matrix = gui.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x4, y4, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x3, y3, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
    }
}
