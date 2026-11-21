package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftStops;
import com.dankscripts.sgjdestiny_dhd.network.DestinyLiftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DestinyLiftScreen extends Screen {
    private final int entity, current;
    private final String[] names;
    private DestinyLiftScreen(int entity, int current, String[] names) {
        super(Component.literal("Destiny elevator")); this.entity = entity; this.current = current; this.names = names;
    }
    public static void open(int entity, int current, String[] names) {
        Minecraft.getInstance().setScreen(new DestinyLiftScreen(entity, current, names));
    }
    @Override protected void init() {
        int top = height / 2 - 120;
        for (int row = 0; row < 8; row++) {
            int index = 7 - row, floor = DestinyLiftStops.HEIGHTS[index];
            String full = names[index];
            String label = font.plainSubstrByWidth(full, 182);
            if (!label.equals(full)) label = font.plainSubstrByWidth(full, 170) + "...";
            Button button = addRenderableWidget(Button.builder(Component.literal(label + "  [" + floor + "]"), b -> {
                DestinyLiftNetwork.select(entity, floor); onClose();
            }).bounds(width / 2 - 120, top + 27 + row * 20, 240, 18)
                    .tooltip(Tooltip.create(Component.literal(full + " — Y " + floor))).build());
            button.active = current != -2 && floor != current;
        }
        addRenderableWidget(Button.builder(Component.literal("EMERGENCY STOP / RELEASE DOORS"), b -> {
            DestinyLiftNetwork.control(entity, true);
        }).bounds(width / 2 - 120, top + 191, 240, 18).build()).active = current != -1;
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(width / 2 - 50, top + 215, 100, 18).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g, mx, my, partialTick);
        int top = height / 2 - 120;
        g.fill(width / 2 - 132, top - 7, width / 2 + 132, top + 240, 0xF01A2E26);
        g.fill(width / 2 - 132, top - 7, width / 2 + 132, top - 5, 0xFFD2B579);
        g.drawCenteredString(font, current == -1 ? "RELEASED / SELECT FLOOR TO RESUME" : "DESTINY / SELECT FLOOR", width / 2, top + 7, 0xFFE2D3A0);
        super.render(g, mx, my, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
}
