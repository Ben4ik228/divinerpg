package com.lootforge.skillforge.client;

import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.common.PlayerUpgradeData;
import com.lootforge.skillforge.common.SkillType;
import com.lootforge.skillforge.network.ModNetwork;
import com.lootforge.skillforge.network.UpgradeActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UpgradeScreen extends Screen {
    private static final ResourceLocation UI_FONT = new ResourceLocation(SkillForgeMod.MOD_ID, "ui");

    private static final int BASE_PANEL_WIDTH = 486;
    private static final int BASE_PANEL_HEIGHT = 296;

    private final List<SparkParticle> sparks = new ArrayList<>();
    private final List<AmbientEmber> embers = new ArrayList<>();
    private final Random random = new Random();

    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;
    private int coreCenterX;
    private int coreCenterY;

    private float pulseTimer = 0.0F;
    private int hoveredSkillIndex = -1;
    private int recentFlashSkillIndex = -1;
    private float recentFlashTimer = 0.0F;

    private final CardBounds[] cardBounds = new CardBounds[SkillType.values().length];
    private RectBounds resetButtonBounds;
    private RectBounds closeButtonBounds;

    public UpgradeScreen() {
        super(Component.literal("\u0410\u0441\u0442\u0440\u0430\u043b\u044c\u043d\u044b\u0439 \u0430\u043b\u0442\u0430\u0440\u044c \u043f\u0440\u043e\u043a\u0430\u0447\u043a\u0438"));
    }

    private Component uiText(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(UI_FONT));
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = Math.min(BASE_PANEL_WIDTH, this.width - 14);
        this.panelHeight = Math.min(BASE_PANEL_HEIGHT, this.height - 14);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;
        this.coreCenterX = this.panelLeft + this.panelWidth / 2;
        this.coreCenterY = this.panelTop + this.panelHeight / 2 - 4;

        initEmbers();
        computeCardBounds();
    }

    private void initEmbers() {
        if (!embers.isEmpty()) {
            return;
        }
        int[] palette = {0xFFEAB308, 0xFF38BDF8, 0xFFFB7185, 0xFF34D399, 0xFFC084FC};
        for (int i = 0; i < 24; i++) {
            AmbientEmber e = new AmbientEmber();
            e.x = this.panelLeft + 16 + random.nextFloat() * Math.max(40, this.panelWidth - 32);
            e.y = this.panelTop + 20 + random.nextFloat() * Math.max(40, this.panelHeight - 40);
            e.vy = -(0.14F + random.nextFloat() * 0.24F);
            e.vx = (random.nextFloat() - 0.5F) * 0.14F;
            e.size = random.nextBoolean() ? 2 : 1;
            e.color = palette[i % palette.length];
            e.phase = random.nextFloat() * 6.28F;
            embers.add(e);
        }
    }

    private void computeCardBounds() {
        int marginX = 14;
        int cardW = Math.min(156, (this.panelWidth - 172) / 2);
        int cardH = 66;
        int startY = this.panelTop + 42;
        int gapY = 8;

        int leftX = this.panelLeft + marginX;
        int rightX = this.panelLeft + this.panelWidth - marginX - cardW;

        for (int i = 0; i < 3; i++) {
            int y = startY + i * (cardH + gapY);
            cardBounds[i] = createCardBounds(leftX, y, cardW, cardH, true);
            cardBounds[i + 3] = createCardBounds(rightX, y, cardW, cardH, false);
        }

        int btnY = this.panelTop + this.panelHeight - 28;
        int centerBtnW = 92;
        int resetX = this.coreCenterX - centerBtnW / 2;
        this.resetButtonBounds = new RectBounds(resetX, btnY, centerBtnW, 18);
        this.closeButtonBounds = new RectBounds(this.panelLeft + this.panelWidth - 96, btnY, 82, 18);
    }

    private CardBounds createCardBounds(int x, int y, int w, int h, boolean isLeft) {
        int upW = 68;
        int downW = 18;
        int btnH = 16;
        int btnY = y + h - btnH - 6;
        int upX = x + w - upW - 6;
        int downX = upX - downW - 4;
        int anchorX = isLeft ? x + w : x;
        int anchorY = y + h / 2;
        return new CardBounds(
                x, y, w, h,
                new RectBounds(upX, btnY, upW, btnH),
                new RectBounds(downX, btnY, downW, btnH),
                anchorX, anchorY
        );
    }

    @Override
    public void tick() {
        super.tick();
        pulseTimer += 0.06F;
        if (recentFlashTimer > 0.0F) {
            recentFlashTimer = Math.max(0.0F, recentFlashTimer - 0.08F);
        }

        for (AmbientEmber e : embers) {
            e.x += e.vx;
            e.y += e.vy;
            e.phase += 0.08F;
            if (e.y < this.panelTop + 14) {
                e.y = this.panelTop + this.panelHeight - 16;
                e.x = this.panelLeft + 14 + random.nextFloat() * Math.max(40, this.panelWidth - 28);
            }
        }

        for (int i = sparks.size() - 1; i >= 0; i--) {
            SparkParticle p = sparks.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.07F;
            p.life--;
            if (p.life <= 0) {
                sparks.remove(i);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        SkillType[] skills = SkillType.values();
        for (int i = 0; i < skills.length; i++) {
            CardBounds cb = cardBounds[i];
            if (cb == null) continue;

            if (cb.upgradeBtn.contains(mouseX, mouseY)) {
                int level = PlayerUpgradeData.getClientLevel(skills[i]);
                int cost = skills[i].getCostForNextLevel(level);
                boolean canAfford = mc.player.isCreative() || mc.player.experienceLevel >= cost;
                if (level < skills[i].maxLevel && canAfford) {
                    ModNetwork.sendToServer(new UpgradeActionPacket(UpgradeActionPacket.ACTION_UPGRADE, i));
                    recentFlashSkillIndex = i;
                    recentFlashTimer = 1.0F;
                    spawnBurst(cb.upgradeBtn.centerX(), cb.upgradeBtn.centerY(), skills[i].color, 18);
                    spawnBurst(this.coreCenterX, this.coreCenterY, skills[i].color, 14);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.25F, 0.75F));
                } else {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 0.4F));
                }
                return true;
            }

            if (cb.downgradeBtn.contains(mouseX, mouseY)) {
                int level = PlayerUpgradeData.getClientLevel(skills[i]);
                if (level > 0) {
                    ModNetwork.sendToServer(new UpgradeActionPacket(UpgradeActionPacket.ACTION_DOWNGRADE, i));
                    spawnBurst(cb.downgradeBtn.centerX(), cb.downgradeBtn.centerY(), 0xFFD8B25E, 8);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.85F, 0.65F));
                }
                return true;
            }
        }

        if (resetButtonBounds != null && resetButtonBounds.contains(mouseX, mouseY)) {
            if (PlayerUpgradeData.getClientTotalLevels() > 0) {
                ModNetwork.sendToServer(new UpgradeActionPacket(UpgradeActionPacket.ACTION_RESET_ALL, -1));
                spawnBurst(this.coreCenterX, this.coreCenterY, 0xFFEAB308, 28);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_USE, 1.1F, 0.55F));
            }
            return true;
        }

        if (closeButtonBounds != null && closeButtonBounds.contains(mouseX, mouseY)) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void spawnBurst(int x, int y, int color, int count) {
        for (int i = 0; i < count; i++) {
            SparkParticle p = new SparkParticle();
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 1.0D + random.nextDouble() * 2.8D;
            p.x = x;
            p.y = y;
            p.vx = (float) (Math.cos(angle) * speed);
            p.vy = (float) (Math.sin(angle) * speed - 0.8D);
            p.color = color;
            p.life = 14 + random.nextInt(14);
            p.size = 2 + random.nextInt(2);
            sparks.add(p);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        int x0 = this.panelLeft;
        int y0 = this.panelTop;
        int w = this.panelWidth;
        int h = this.panelHeight;

        // Outer Obsidian Altar Slab & Burnished Gold Trim
        g.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, 0xF5040307);
        g.fill(x0, y0, x0 + w, y0 + h, 0xFF0A0812);
        g.fill(x0 + 2, y0 + 2, x0 + w - 2, y0 + h - 2, 0xFF100D1B);

        // Header Ribbon
        g.fill(x0 + 2, y0 + 2, x0 + w - 2, y0 + 34, 0xFF161124);
        g.fill(x0 + 2, y0 + 34, x0 + w - 2, y0 + 35, 0xFF7A5D24);

        renderBorder(g, x0, y0, w, h, 0xFF9C762F);
        renderCornerFiligree(g, x0, y0, w, h, 0xFFEAB308);
        renderEmbers(g, partialTick);

        // Determine Hovered Skill
        hoveredSkillIndex = -1;
        SkillType[] skills = SkillType.values();
        for (int i = 0; i < skills.length; i++) {
            if (cardBounds[i] != null && cardBounds[i].contains(mouseX, mouseY)) {
                hoveredSkillIndex = i;
                break;
            }
        }

        // Top Header Texts
        Minecraft mc = Minecraft.getInstance();
        int playerXp = mc.player != null ? mc.player.experienceLevel : 0;
        int powerScore = PlayerUpgradeData.getClientPowerScore();

        g.drawCenteredString(this.font, uiText("\u0410\u0421\u0422\u0420\u0410\u041b\u042c\u041d\u042b\u0419 \u0410\u041b\u0422\u0410\u0420\u042c \u041f\u0420\u041e\u041a\u0410\u0427\u041a\u0418"), x0 + w / 2, y0 + 9, 0xFFFBF7EE);
        g.drawString(this.font, uiText("\u041c\u043e\u0449\u044c: " + powerScore), x0 + 16, y0 + 13, 0xFFF4D078, false);
        String xpLabel = "\u041e\u043f\u044b\u0442: " + playerXp + " \u0443\u0440.";
        int xpW = this.font.width(uiText(xpLabel));
        g.drawString(this.font, uiText(xpLabel), x0 + w - 16 - xpW, y0 + 13, 0xFF34D399, false);

        // Energy Tethers from Core to 6 Nodes
        renderEnergyBeams(g, skills, partialTick);

        // Central Astral Rings + Live 3D Player Entity
        renderAstralCore(g, mouseX, mouseY, partialTick);

        // 6 Interactive Attribute Cards & Buttons
        for (int i = 0; i < skills.length; i++) {
            renderSkillCard(g, skills[i], i, cardBounds[i], mouseX, mouseY, mc);
        }

        // Footer Controls
        renderFooterButtons(g, mouseX, mouseY);

        // Sparks above everything
        renderSparks(g);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderEmbers(GuiGraphics g, float partialTick) {
        for (AmbientEmber e : embers) {
            int alpha = (int) ((0.22F + 0.32F * Mth.sin(e.phase)) * 255.0F);
            alpha = Mth.clamp(alpha, 25, 160);
            int col = (alpha << 24) | (e.color & 0x00FFFFFF);
            int px = (int) e.x;
            int py = (int) (e.y + e.vy * partialTick);
            g.fill(px, py, px + e.size, py + e.size, col);
        }
    }

    private void renderEnergyBeams(GuiGraphics g, SkillType[] skills, float partialTick) {
        for (int i = 0; i < skills.length; i++) {
            CardBounds cb = cardBounds[i];
            if (cb == null) continue;

            int level = PlayerUpgradeData.getClientLevel(skills[i]);
            boolean isHovered = (i == hoveredSkillIndex);
            boolean isFlashing = (i == recentFlashSkillIndex && recentFlashTimer > 0.0F);

            int alpha = 45 + Math.min(130, level * 9);
            if (isHovered) alpha = 215;
            if (isFlashing) alpha = 255;

            int color = (alpha << 24) | (skills[i].color & 0x00FFFFFF);
            int midX = (this.coreCenterX + cb.anchorX) / 2;

            drawLine(g, this.coreCenterX, this.coreCenterY, midX, cb.anchorY, color);
            drawLine(g, midX, cb.anchorY, cb.anchorX, cb.anchorY, color);
            if (isHovered || isFlashing || level >= 5) {
                drawLine(g, this.coreCenterX, this.coreCenterY + 1, midX, cb.anchorY + 1, color);
                drawLine(g, midX, cb.anchorY + 1, cb.anchorX, cb.anchorY + 1, color);
            }

            // Moving energy pulse node along tether
            float t = ((pulseTimer * 0.35F) + i * 0.17F + partialTick * 0.01F) % 1.0F;
            int px = (int) Mth.lerp(t, this.coreCenterX, cb.anchorX);
            int py = (int) Mth.lerp(t, this.coreCenterY, cb.anchorY);
            g.fill(px - 1, py - 1, px + 2, py + 2, (0xCC << 24) | (skills[i].color & 0x00FFFFFF));
        }
    }

    private void renderAstralCore(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = this.coreCenterX;
        int cy = this.coreCenterY;
        float angle = (pulseTimer + partialTick * 0.06F) * 0.45F;

        int ringColorOuter = 0xAA9C762F;
        int ringColorInner = 0xCC38BDF8;
        if (hoveredSkillIndex >= 0) {
            ringColorInner = (0xDD << 24) | (SkillType.values()[hoveredSkillIndex].color & 0x00FFFFFF);
        }

        // Core Backglow Disc
        drawFilledCircle(g, cx, cy, 48, 0x45181228);
        drawCircleOutline(g, cx, cy, 52, ringColorOuter, 44, angle);
        drawCircleOutline(g, cx, cy, 38, ringColorInner, 32, -angle * 1.4F);
        drawPolygonOutline(g, cx, cy, 45, 8, 0x88EAB308, angle * 0.8F);

        // Live 3D Player Avatar Inside the Rotating Astral Core
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int entityY = cy + 34;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g,
                    cx,
                    entityY,
                    34,
                    (float) (cx - mouseX),
                    (float) ((cy - 8) - mouseY),
                    mc.player
            );
        }

        String totalBadge = "\u0423\u0440\u043e\u0432\u043d\u0435\u0439 \u0432\u043b\u043e\u0436\u0435\u043d\u043e: " + PlayerUpgradeData.getClientTotalLevels();
        g.drawCenteredString(this.font, uiText(totalBadge), cx, cy + 58, 0xFFD8B25E);
    }

    private void renderSkillCard(
            GuiGraphics g,
            SkillType skill,
            int index,
            CardBounds cb,
            int mouseX,
            int mouseY,
            Minecraft mc
    ) {
        if (cb == null) return;

        int level = PlayerUpgradeData.getClientLevel(skill);
        int cost = skill.getCostForNextLevel(level);
        boolean maxed = level >= skill.maxLevel;
        boolean freeMode = mc.player != null && mc.player.isCreative();
        boolean canAfford = !maxed && (freeMode || (mc.player != null && mc.player.experienceLevel >= cost));
        boolean cardHover = cb.contains(mouseX, mouseY);

        int bgColor = cardHover ? 0xFF1A152B : 0xFF130F20;
        int borderColor = cardHover ? skill.color : ((0x70 << 24) | (skill.color & 0x00FFFFFF));

        g.fill(cb.x, cb.y, cb.x + cb.w, cb.y + cb.h, bgColor);
        renderBorder(g, cb.x, cb.y, cb.w, cb.h, borderColor);
        // Top Element Accent Strip
        g.fill(cb.x + 2, cb.y + 1, cb.x + cb.w - 2, cb.y + 3, skill.color);

        // Title + Level Badge
        g.drawString(this.font, uiText(skill.roman + " \u00b7 " + skill.title), cb.x + 6, cb.y + 6, 0xFFFBF7EE, false);
        String lvlText = level + "/" + skill.maxLevel;
        int lvlW = this.font.width(uiText(lvlText));
        g.drawString(this.font, uiText(lvlText), cb.x + cb.w - 6 - lvlW, cb.y + 6, skill.color, false);

        // Current Value & Next Delta
        String statLine = skill.formatBonus(level) + (maxed ? " [MAX]" : " (" + skill.formatStepDelta() + ")");
        g.drawString(this.font, uiText(statLine), cb.x + 6, cb.y + 19, 0xFFD4CBB8, false);

        // 15 Crystal Progress Pips
        int pipCount = skill.maxLevel;
        int barX = cb.x + 6;
        int barY = cb.y + 32;
        int totalBarW = cb.w - 12;
        int gap = 1;
        int pipW = Math.max(3, (totalBarW - (pipCount - 1) * gap) / pipCount);
        for (int p = 0; p < pipCount; p++) {
            int px = barX + p * (pipW + gap);
            int pipCol = p < level ? skill.color : 0xFF28213A;
            g.fill(px, barY, px + pipW, barY + 5, pipCol);
        }

        // Downgrade [-] button
        boolean downHover = cb.downgradeBtn.contains(mouseX, mouseY);
        int downBg = level > 0 ? (downHover ? 0xFF33284A : 0xFF211A33) : 0xFF14101E;
        int downBorder = level > 0 ? 0xFF6B598A : 0xFF2B233D;
        g.fill(cb.downgradeBtn.x, cb.downgradeBtn.y, cb.downgradeBtn.x + cb.downgradeBtn.w, cb.downgradeBtn.y + cb.downgradeBtn.h, downBg);
        renderBorder(g, cb.downgradeBtn.x, cb.downgradeBtn.y, cb.downgradeBtn.w, cb.downgradeBtn.h, downBorder);
        g.drawCenteredString(this.font, uiText("-"), cb.downgradeBtn.centerX(), cb.downgradeBtn.y + 4, level > 0 ? 0xFFEAE3D5 : 0xFF5A506D);

        // Upgrade [+] button
        boolean upHover = cb.upgradeBtn.contains(mouseX, mouseY);
        int upBg;
        int upBorder;
        int upTextCol;
        String upLabel;

        if (maxed) {
            upBg = 0xFF1D192B;
            upBorder = skill.color;
            upTextCol = skill.color;
            upLabel = "\u041c\u0410\u041a\u0421.";
        } else if (canAfford) {
            upBg = upHover ? 0xFFDDB152 : 0xFFC89B3C;
            upBorder = 0xFFFDE68A;
            upTextCol = 0xFF0A0810;
            upLabel = freeMode ? "+1 FREE" : "+ " + cost + " XP";
        } else {
            upBg = 0xFF1A1526;
            upBorder = 0xFF3B304D;
            upTextCol = 0xFF7B718E;
            upLabel = cost + " XP";
        }

        g.fill(cb.upgradeBtn.x, cb.upgradeBtn.y, cb.upgradeBtn.x + cb.upgradeBtn.w, cb.upgradeBtn.y + cb.upgradeBtn.h, upBg);
        renderBorder(g, cb.upgradeBtn.x, cb.upgradeBtn.y, cb.upgradeBtn.w, cb.upgradeBtn.h, upBorder);
        g.drawCenteredString(this.font, uiText(upLabel), cb.upgradeBtn.centerX(), cb.upgradeBtn.y + 4, upTextCol);
    }

    private void renderFooterButtons(GuiGraphics g, int mouseX, int mouseY) {
        // Reset Build
        if (resetButtonBounds != null) {
            boolean hasLevels = PlayerUpgradeData.getClientTotalLevels() > 0;
            boolean hover = resetButtonBounds.contains(mouseX, mouseY);
            int bg = hasLevels ? (hover ? 0xFF3D1D29 : 0xFF28131B) : 0xFF15121F;
            int br = hasLevels ? 0xFFFB7185 : 0xFF322945;
            int tx = hasLevels ? 0xFFFDA4AF : 0xFF675D7A;
            g.fill(resetButtonBounds.x, resetButtonBounds.y, resetButtonBounds.x + resetButtonBounds.w, resetButtonBounds.y + resetButtonBounds.h, bg);
            renderBorder(g, resetButtonBounds.x, resetButtonBounds.y, resetButtonBounds.w, resetButtonBounds.h, br);
            g.drawCenteredString(this.font, uiText("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u0431\u0438\u043b\u0434"), resetButtonBounds.centerX(), resetButtonBounds.y + 5, tx);
        }

        // Close Button
        if (closeButtonBounds != null) {
            boolean hover = closeButtonBounds.contains(mouseX, mouseY);
            g.fill(closeButtonBounds.x, closeButtonBounds.y, closeButtonBounds.x + closeButtonBounds.w, closeButtonBounds.y + closeButtonBounds.h, hover ? 0xFF2A223D : 0xFF1B1628);
            renderBorder(g, closeButtonBounds.x, closeButtonBounds.y, closeButtonBounds.w, closeButtonBounds.h, 0xFF8B6A2B);
            g.drawCenteredString(this.font, uiText("\u0417\u0430\u043a\u0440\u044b\u0442\u044c (L)"), closeButtonBounds.centerX(), closeButtonBounds.y + 5, 0xFFEAE3D5);
        }
    }

    private void renderSparks(GuiGraphics g) {
        for (SparkParticle p : sparks) {
            int alpha = Mth.clamp(p.life * 14, 0, 255);
            int col = (alpha << 24) | (p.color & 0x00FFFFFF);
            int px = (int) p.x;
            int py = (int) p.y;
            g.fill(px - p.size / 2, py - p.size / 2, px + p.size, py + p.size, col);
        }
    }

    private void renderBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void renderCornerFiligree(GuiGraphics g, int x, int y, int w, int h, int color) {
        int len = 12;
        g.fill(x + 4, y + 4, x + 4 + len, y + 5, color);
        g.fill(x + 4, y + 4, x + 5, y + 4 + len, color);

        g.fill(x + w - 4 - len, y + 4, x + w - 4, y + 5, color);
        g.fill(x + w - 5, y + 4, x + w - 4, y + 4 + len, color);

        g.fill(x + 4, y + h - 5, x + 4 + len, y + h - 4, color);
        g.fill(x + 4, y + h - 4 - len, x + 5, y + h - 4, color);

        g.fill(x + w - 4 - len, y + h - 5, x + w - 4, y + h - 4, color);
        g.fill(x + w - 5, y + h - 4 - len, x + w - 4, y + h - 4, color);
    }

    private void drawFilledCircle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
        }
    }

    private void drawCircleOutline(GuiGraphics g, int cx, int cy, int r, int color, int segments, float offsetAngle) {
        for (int i = 0; i < segments; i++) {
            float a1 = offsetAngle + (float) (i * 2.0D * Math.PI / segments);
            float a2 = offsetAngle + (float) ((i + 1) * 2.0D * Math.PI / segments);
            int x1 = cx + Math.round(Mth.cos(a1) * r);
            int y1 = cy + Math.round(Mth.sin(a1) * r);
            int x2 = cx + Math.round(Mth.cos(a2) * r);
            int y2 = cy + Math.round(Mth.sin(a2) * r);
            drawLine(g, x1, y1, x2, y2, color);
        }
    }

    private void drawPolygonOutline(GuiGraphics g, int cx, int cy, int r, int sides, int color, float offsetAngle) {
        drawCircleOutline(g, cx, cy, r, color, sides, offsetAngle);
    }

    private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.OPEN_UPGRADE_MENU.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class RectBounds {
        final int x;
        final int y;
        final int w;
        final int h;

        RectBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        int centerX() {
            return x + w / 2;
        }

        int centerY() {
            return y + h / 2;
        }
    }

    private static final class CardBounds {
        final int x;
        final int y;
        final int w;
        final int h;
        final RectBounds upgradeBtn;
        final RectBounds downgradeBtn;
        final int anchorX;
        final int anchorY;

        CardBounds(
                int x, int y, int w, int h,
                RectBounds upgradeBtn,
                RectBounds downgradeBtn,
                int anchorX,
                int anchorY
        ) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.upgradeBtn = upgradeBtn;
            this.downgradeBtn = downgradeBtn;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private static final class SparkParticle {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        int life;
        int size;
    }

    private static final class AmbientEmber {
        float x;
        float y;
        float vx;
        float vy;
        float phase;
        int color;
        int size;
    }
}
