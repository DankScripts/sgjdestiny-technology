package com.dankscripts.sgjdestiny_dhd.item;

import com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class DestinyLiftRepairKitItem extends Item {
    public DestinyLiftRepairKitItem() { super(new Properties().stacksTo(16)); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player && !player.isSpectator()) {
            if (player.isShiftKeyDown()) DestinyLiftEvents.rescue(player, context.getClickedPos());
            else if (DestinyLiftEvents.repair(player, context.getClickedPos()) && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.sgjdestiny_dhd.elevator_repair_kit"));
        lines.add(Component.translatable("tooltip.sgjdestiny_dhd.elevator_repair_kit_consumption"));
        lines.add(Component.translatable("tooltip.sgjdestiny_dhd.elevator_emergency_release"));
    }
}
