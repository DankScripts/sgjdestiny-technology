package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.menu.DestinyControlHubMenu;
import com.dankscripts.sgjdestiny_dhd.registry.ModMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

/** Amber-on-black readout for ship reserves, tank water, and live atmosphere. */
public final class DestinyControlHubScreen extends AbstractContainerScreen<DestinyControlHubMenu> {
    private static final int BG = 0xFF0E1415;
    private static final int FRAME = 0xFF5D5140;
    private static final int BRIGHT = 0xFFEAD9A8;
    private static final int WARM = 0xFFC49354;
    private static final int DIM = 0xFF8A836F;
    private Button scrubberSwitch;

    public DestinyControlHubScreen(DestinyControlHubMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 242;
    }

    @Override
    protected void init() {
        super.init();
        scrubberSwitch = addRenderableWidget(Button.builder(Component.literal("RESET"), b -> press(6))
                .bounds(leftPos + 215, topPos + 183, 86, 20).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF080B0C);
        graphics.fill(x + 3, y + 3, x + 317, y + 239, FRAME);
        graphics.fill(x + 6, y + 6, x + 314, y + 236, BG);
        graphics.fill(x + 14, y + 37, x + 306, y + 39, WARM);
        graphics.fill(x + 14, y + 214, x + 306, y + 215, FRAME);
        for (int row = 0; row < 3; row++) {
            int value = menu.reading(row);
            int top = y + 54 + row * 44;
            graphics.fill(x + 18, top, x + 302, top + 34, 0xFF1B2222);
            graphics.fill(x + 106, top + 21, x + 292, top + 27, 0xFF322C25);
            int color = row == 2
                    ? (value >= 80 ? 0xFFAA4A39 : value >= 60 ? 0xFFD29851 : 0xFFDCCF9A)
                    : (value <= 20 ? 0xFFAA4A39 : value <= 45 ? 0xFFD29851 : 0xFFDCCF9A);
            graphics.fill(x + 106, top + 21, x + 106 + value * 186 / 100, top + 27, color);
            graphics.fill(x + 106, top + 28, x + 292, top + 29, FRAME);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "DESTINY / CONTROL INTERFACE", 17, 15, BRIGHT, false);
        graphics.drawString(font, menu.waterTankPresent() ? "WATER LIVE / POWER FIXED"
                : "NO TANK / POWER FIXED", 142, 27, DIM, false);
        String[] labels = {"POWER RESERVES", "WATER", "CO2 BUILDUP"};
        for (int row = 0; row < 3; row++) {
            int top = 54 + row * 44;
            graphics.drawString(font, labels[row], 24, top + 6, BRIGHT, false);
            graphics.drawString(font, menu.reading(row) + "%", 266, top + 6, WARM, false);
        }
        graphics.drawString(font, menu.waterTankPresent()
                ? menu.waterBuckets() + "/" + menu.waterCapacityBuckets() + " buckets"
                : "NO LOADED TANK", 108, 104, DIM, false);
        graphics.drawString(font, "AIR SCRUBBERS (" + menu.scrubberCount() + ")", 24, 189, BRIGHT, false);
        graphics.drawString(font, menu.liveAtmosphere() ? switch (menu.scrubberStatus()) {
                    case 1 -> "ONLINE";
                    case 2 -> "RESET NEEDED";
                    case 3 -> "SERVICING";
                    default -> "NO UNITS";
                } : "UNAVAILABLE",
                140, 189, menu.scrubberStatus() == 1 ? WARM : DIM, false);
        graphics.drawString(font, menu.liveAtmosphere()
                        ? "Refill dry scrubbers, then reset them here."
                        : "Scrubber reset is available aboard Destiny.",
                17, 223, DIM, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (scrubberSwitch != null) {
            scrubberSwitch.active = menu.liveAtmosphere();
        }
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private static boolean registered;
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                if (registered) return;
                registered = true;
                try {
                    MenuScreens.register(ModMenus.DESTINY_CONTROL_HUB.get(), DestinyControlHubScreen::new);
                } catch (IllegalStateException duplicate) {
                    if (!duplicate.getMessage().startsWith("Duplicate registration for sgjdestiny_dhd:destiny_control_hub"))
                        throw duplicate;
                }
            });
        }
    }
}
