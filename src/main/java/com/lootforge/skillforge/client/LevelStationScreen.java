package com.lootforge.skillforge.client;

import com.lootforge.skillforge.SkillForgeMod;
import com.lootforge.skillforge.levels.LevelStationBlockEntity;
import com.lootforge.skillforge.levels.LevelStationMenu;
import com.lootforge.skillforge.network.ImproveStationPacket;
import com.lootforge.skillforge.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class LevelStationScreen extends AbstractContainerScreen<LevelStationMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SkillForgeMod.MOD_ID, "textures/gui/container/level_station.png");

    public LevelStationScreen(LevelStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private boolean isMouseOverButton(int mouseX, int mouseY) {
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        return relX > 121 && relX <= 145 && relY > 28 && relY <= 52;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Не рисуем стандартные подписи, чтобы не перекрывать текст шанса усиления
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.getProgress();
        int progressWidth = (int) (46.0D * ((double) progress / (double) LevelStationBlockEntity.TARGET_PROGRESS));
        if (progressWidth > 0) {
            graphics.blit(TEXTURE, x + 68, y + 32, 176, 0, progressWidth, 14);
        }

        int chance = this.menu.getChance();
        if (chance > 0) {
            int buttonU = this.isMouseOverButton(mouseX, mouseY) ? 200 : 176;
            graphics.blit(TEXTURE, x + 121, y + 28, buttonU, 14, 24, 24);
        }

        Component skipText = Component.translatable("gui.skillforge.chance.double")
                .append(" " + this.menu.getChanceSkipLevel() + "%");
        Component chanceText = Component.translatable("gui.skillforge.chance.text")
                .append(" " + chance + "%");

        graphics.drawString(this.font, skipText, x + 42, y + 13, 0xFFFFFF, true);
        graphics.drawString(this.font, chanceText, x + 65, y + 62, 0xFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.getChance() > 0 && this.isMouseOverButton((int) mouseX, (int) mouseY)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            ModNetwork.sendToServer(new ImproveStationPacket(this.menu.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
