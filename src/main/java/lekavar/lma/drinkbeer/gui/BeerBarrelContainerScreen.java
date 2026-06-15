package lekavar.lma.drinkbeer.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.awt.*;

public class BeerBarrelContainerScreen extends AbstractContainerScreen<BeerBarrelContainer> {

    private static final ResourceLocation BEER_BARREL_CONTAINER_RESOURCE = ResourceLocation.fromNamespaceAndPath("drinkbeer", "textures/gui/container/beer_barrel.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;
    private static final int OUTPUT_SLOT_X = 128;
    private static final int OUTPUT_SLOT_Y = 34;
    private final Inventory inventory;

    public BeerBarrelContainerScreen(BeerBarrelContainer screenContainer, Inventory inv, Component title) {
        super(screenContainer, inv, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
        this.inventory = inv;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderLockedOutputOverlay(guiGraphics);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(BEER_BARREL_CONTAINER_RESOURCE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, this.titleLabelY, 4210752);
        guiGraphics.drawString(this.font, this.inventory.getDisplayName(), this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        String str = menu.getIsBrewing() ? convertTickToTime(menu.getRemainingBrewingTime()) : convertTickToTime(menu.getStandardBrewingTime());
        guiGraphics.drawString(this.font, str, 128, 54, new Color(64, 64, 64, 255).getRGB(), false);
    }

    public String convertTickToTime(int tick) {
        if (tick > 0) {
            int totalSeconds = (tick + 19) / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
        return "";
    }

    private void renderLockedOutputOverlay(GuiGraphics guiGraphics) {
        if (menu.getIsBrewing()) {
            int x = this.leftPos + OUTPUT_SLOT_X;
            int y = this.topPos + OUTPUT_SLOT_Y;
            guiGraphics.fill(x, y, x + 16, y + 16, 0x99000000);
        }
    }
}
