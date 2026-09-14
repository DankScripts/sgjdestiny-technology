package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.menu.KinoDispenserMenu;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import com.dankscripts.sgjdestiny_dhd.registry.ModMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

public final class KinoDispenserScreen extends AbstractContainerScreen<KinoDispenserMenu> {
    private static final int PANEL = 0xFF302C27;
    private static final int PANEL_EDGE = 0xFF827763;
    private static final int DARK_EDGE = 0xFF171513;
    private static final int NICHE = 0xFF050606;
    private static final int TEXT = 0xFFE1D1A7;

    public KinoDispenserScreen(KinoDispenserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 244;
        imageHeight = 202;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.sgjdestiny_dhd.kino_dispenser.deposit"),
                button -> press(KinoDispenserMenu.DEPOSIT)).bounds(leftPos + 24, topPos + 164, 92, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.sgjdestiny_dhd.kino_dispenser.pickup"),
                button -> press(KinoDispenserMenu.PICKUP)).bounds(leftPos + 128, topPos + 164, 92, 22).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x + 4, y + 4, x + 240, y + 198, DARK_EDGE);
        graphics.fill(x + 8, y + 8, x + 236, y + 194, PANEL_EDGE);
        graphics.fill(x + 12, y + 12, x + 232, y + 190, PANEL);

        // Recessed compartment, shaped like the opening on the dispenser model.
        graphics.fill(x + 31, y + 48, x + 213, y + 139, 0xFF0E0D0C);
        graphics.fill(x + 37, y + 42, x + 207, y + 145, 0xFF0E0D0C);
        graphics.fill(x + 43, y + 48, x + 201, y + 139, NICHE);
        graphics.fill(x + 31, y + 68, x + 43, y + 124, NICHE);
        graphics.fill(x + 201, y + 68, x + 213, y + 124, NICHE);

        int count = menu.getStoredKinos();
        if (count == 1) {
            drawKino(graphics, x + 122, y + 99, 3.7F);
        } else if (count >= 2) {
            drawKino(graphics, x + 83, y + 101, 3.45F);
            drawKino(graphics, x + 161, y + 101, 3.45F);
        }

        graphics.fill(x + 25, y + 137, x + 219, y + 150, PANEL_EDGE);
        graphics.fill(x + 31, y + 137, x + 213, y + 146, PANEL);
    }

    private static void drawKino(GuiGraphics graphics, int cx, int cy, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx - 8.0F * scale, cy - 8.0F * scale, 100.0F);
        graphics.pose().scale(scale, scale, scale);
        graphics.renderItem(new ItemStack(ModItems.KINO.get()), 0, 0);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 20, TEXT);
        Component count = Component.translatable("gui.sgjdestiny_dhd.kino_dispenser.count",
                menu.getStoredKinos(), 64);
        graphics.drawCenteredString(font, count, imageWidth / 2, 151, TEXT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void registerScreens(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(ModMenus.KINO_DISPENSER.get(), KinoDispenserScreen::new));
        }
    }
}
