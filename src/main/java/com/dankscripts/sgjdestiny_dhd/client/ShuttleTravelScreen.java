package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.network.ShuttleTravelNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** A cockpit hologram; the server validates and performs the actual trip. */
public final class ShuttleTravelScreen extends Screen {
    private final BlockPos anchor;
    private final String destination;
    private Button launch;
    private String status = "PILOT LINK READY";
    private boolean inFlight;
    private int ticks;

    ShuttleTravelScreen(BlockPos anchor, String destination) {
        super(Component.literal("Shuttle navigation"));
        this.anchor = anchor;
        this.destination = destination;
    }

    @Override protected void init() {
        int cx = width / 2, cy = height / 2;
        launch = addRenderableWidget(Button.builder(Component.literal("LAUNCH TO " + destination), button -> {
            button.active = false;
            status = "CHECKING ROUTE...";
            ShuttleTravelNetwork.launch(anchor);
        }).bounds(cx - 92, cy + 42, 184, 22).build());
    }

    void setStatus(boolean launched, String message) {
        inFlight = launched && message.startsWith("Launching");
        ticks = 0;
        status = message.toUpperCase(java.util.Locale.ROOT);
        if (launch != null) launch.active = !launched;
    }

    @Override public void tick() { ticks++; }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2, cy = height / 2;
        graphics.fill(cx - 126, cy - 86, cx + 126, cy + 80, 0xCC061B1D);
        graphics.fill(cx - 126, cy - 86, cx + 126, cy - 84, 0xFF5ABFB0);
        graphics.fill(cx - 126, cy + 78, cx + 126, cy + 80, 0xFF5ABFB0);
        graphics.drawCenteredString(font, "DESTINY  /  SHUTTLE NAVIGATION", cx, cy - 70, 0xFFB7EEDB);
        graphics.drawCenteredString(font, "DESTINATION: " + destination, cx, cy - 41, 0xFFD8C184);
        var statusLines = font.split(Component.literal(status), 220);
        int statusY = cy - 17 - Math.max(0, statusLines.size() - 2) * 5;
        for (var line : statusLines) {
            graphics.drawCenteredString(font, line, cx, statusY, 0xFF78D3BF);
            statusY += 10;
        }
        int progress = inFlight ? Math.min(200, ticks * 2) : 0;
        graphics.fill(cx - 100, cy + 14, cx + 100, cy + 20, 0xFF163A3D);
        graphics.fill(cx - 100, cy + 14, cx - 100 + progress, cy + 20, 0xFF58C9B1);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
