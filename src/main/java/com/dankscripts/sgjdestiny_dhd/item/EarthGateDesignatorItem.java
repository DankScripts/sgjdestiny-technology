package com.dankscripts.sgjdestiny_dhd.item;

import com.dankscripts.sgjdestiny_dhd.dialer.EarthGateData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.block_entities.stargate.MilkyWayStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.Address;

import java.util.List;

public final class EarthGateDesignatorItem extends Item {
    public EarthGateDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return designate(context);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return designate(context);
    }

    private InteractionResult designate(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel) || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof MilkyWayStargateEntity gate)) {
            context.getPlayer().displayClientMessage(
                    Component.translatable("message.sgjdestiny_dhd.earth_gate_invalid"), true);
            return InteractionResult.CONSUME;
        }
        gate.generate();
        Address.Immutable address = gate.get9ChevronAddress();
        if (address == null || address.getType() != Address.Type.ADDRESS_9_CHEVRON) {
            context.getPlayer().displayClientMessage(
                    Component.translatable("message.sgjdestiny_dhd.earth_gate_invalid"), true);
            return InteractionResult.CONSUME;
        }
        EarthGateData.get(serverLevel.getServer()).assign(address);
        context.getPlayer().displayClientMessage(Component.translatable(
                "message.sgjdestiny_dhd.earth_gate_assigned", context.getClickedPos().toShortString()), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sgjdestiny_dhd.earth_gate_designator")
                .withStyle(ChatFormatting.GRAY));
    }
}
