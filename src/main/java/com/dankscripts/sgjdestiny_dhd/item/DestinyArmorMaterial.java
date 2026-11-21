package com.dankscripts.sgjdestiny_dhd.item;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** Native 1.21 armor-material record used by the Destiny environmental suit. */
public final class DestinyArmorMaterial {
    public static final Holder<ArmorMaterial> INSTANCE = Holder.direct(create());

    private DestinyArmorMaterial() {}

    private static ArmorMaterial create() {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 8);
        return new ArmorMaterial(defense, 10, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                        DestinyDHD.MOD_ID, "destiny_environmental"))), 2.0F, 0.1F);
    }
}
