package com.dankscripts.sgjdestiny_dhd.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public final class DestinyArmorMaterial implements ArmorMaterial {
    public static final DestinyArmorMaterial INSTANCE = new DestinyArmorMaterial();
    private static final int[] HEALTH = {13, 15, 16, 11};
    private static final int[] DEFENSE = {3, 6, 8, 3};

    private DestinyArmorMaterial() {}

    @Override public int getDurabilityForType(ArmorItem.Type type) { return HEALTH[type.getSlot().getIndex()] * 32; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return DEFENSE[type.getSlot().getIndex()]; }
    @Override public int getEnchantmentValue() { return 10; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    @Override public String getName() { return "sgjdestiny_dhd:destiny_environmental"; }
    @Override public float getToughness() { return 2.0F; }
    @Override public float getKnockbackResistance() { return 0.1F; }
}
