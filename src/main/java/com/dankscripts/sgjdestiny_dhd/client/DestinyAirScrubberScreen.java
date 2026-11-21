package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.menu.DestinyAirScrubberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Service panel for water/calcite input, empty buckets, and scrubber status. */
public final class DestinyAirScrubberScreen extends AbstractContainerScreen<DestinyAirScrubberMenu> {
    private static final int PANEL = 0xFF202521;
    private static final int FRAME = 0xFF665945;
    private static final int TEXT = 0xFFE7D6AB;
    private static final int DIM = 0xFFB1A58C;
    private Button powerSwitch;

    public DestinyAirScrubberScreen(DestinyAirScrubberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 197;
    }

    @Override protected void init() {
        super.init();
        powerSwitch = addRenderableWidget(Button.builder(Component.literal("TURN OFF"), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 105, topPos + 22, 61, 18).build());
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + 176, y + 197, 0xFF0C0E0D);
        graphics.fill(x + 3, y + 3, x + 173, y + 194, FRAME);
        graphics.fill(x + 6, y + 6, x + 170, y + 191, PANEL);
        graphics.fill(x + 10, y + 41, x + 166, y + 91, 0xFF111514);
        for (int slotX : new int[] {34, 79, 124}) {
            graphics.fill(x + slotX - 2, y + 54, x + slotX + 20, y + 76, FRAME);
            graphics.fill(x + slotX, y + 56, x + slotX + 18, y + 74, 0xFF090B0A);
        }
        graphics.fill(x + 14, y + 100, x + 162, y + 101, FRAME);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slotBackground(graphics, x + 8 + col * 18, y + 111 + row * 18);
        for (int col = 0; col < 9; col++) slotBackground(graphics, x + 8 + col * 18, y + 169);
    }

    private static void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF463D31);
        graphics.fill(x, y, x + 16, y + 16, 0xFF111514);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 13, TEXT, false);
        graphics.drawString(font, menu.dryFault() ? "DRY / RESET" : menu.poweredOn() ? "UNIT ONLINE" : "UNIT OFF",
                10, 28, menu.enabled() ? DIM : 0xFFDD7359, false);
        graphics.drawString(font, "WATER", 23, 44, DIM, false);
        graphics.drawString(font, "CALCITE", 66, 44, DIM, false);
        graphics.drawString(font, "EMPTY", 115, 44, DIM, false);
        graphics.drawString(font, "Stored: " + menu.waterBuckets() + " buckets / " + menu.calcite() + " calcite", 12, 80, TEXT, false);
        graphics.drawString(font, "Mix " + menu.mixPercent() + "%", 12, 92, DIM, false);
        if (menu.co2Percent() >= 0)
            graphics.drawString(font, "Ship CO2 " + menu.co2Percent() + "%", 92, 92,
                    menu.co2Percent() >= 60 ? 0xFFDD7359 : DIM, false);
        graphics.drawString(font, "Inventory", 8, 102, DIM, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (powerSwitch != null)
            powerSwitch.setMessage(Component.literal(menu.poweredOn() ? "TURN OFF" : "TURN ON"));
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

}
