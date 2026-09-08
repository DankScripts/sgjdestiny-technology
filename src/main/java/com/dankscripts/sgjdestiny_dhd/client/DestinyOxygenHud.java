package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Compact Destiny-style life-support readout. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DestinyOxygenHud {
    private DestinyOxygenHud() {}

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        if (!minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DESTINY_ENVIRONMENTAL_HELMET.get())) return;
        ItemStack tank = ItemStack.EMPTY;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack.is(ModItems.DESTINY_OXYGEN_TANK.get())) {
                tank = stack;
                if (stack.getDamageValue() < stack.getMaxDamage()) break;
            }
        }
        int max = tank.isEmpty() ? 3600 : tank.getMaxDamage();
        int remaining = tank.isEmpty() ? 0 : Math.max(0, max - tank.getDamageValue());
        int percent = max == 0 ? 0 : remaining * 100 / max;
        int seconds = remaining / 2;
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
        GuiGraphics graphics = event.getGuiGraphics();
        int width = 132, height = 38;
        int x = graphics.guiWidth() - width - 8;
        int y = graphics.guiHeight() - height - 8;
        graphics.fill(x, y, x + width, y + height, 0xC6101717);
        graphics.renderOutline(x, y, width, height, 0xFF9A6532);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0x481C302D);
        graphics.renderItem(tank.isEmpty() ? new ItemStack(ModItems.DESTINY_OXYGEN_TANK.get()) : tank, x + 6, y + 10);
        graphics.drawString(minecraft.font, "SUIT OXYGEN", x + 28, y + 5, 0xFFE8B968, false);
        String reading = percent + "%  " + time;
        graphics.drawString(minecraft.font, reading, x + 28, y + 16, 0xFFB9D5CC, false);
        int barLeft = x + 28;
        int barRight = x + width - 7;
        int barTop = y + 28;
        graphics.fill(barLeft, barTop, barRight, barTop + 5, 0xFF172725);
        int fill = (barRight - barLeft) * remaining / Math.max(1, max);
        int color = percent > 25 ? 0xFF58B8AA : percent > 10 ? 0xFFE5A33E : 0xFFD34A35;
        graphics.fill(barLeft, barTop, barLeft + fill, barTop + 5, color);
    }
}
