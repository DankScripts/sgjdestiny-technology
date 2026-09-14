package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.menu.KinoDispenserMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DestinyDHD.MOD_ID);

    public static final RegistryObject<MenuType<KinoDispenserMenu>> KINO_DISPENSER =
            MENUS.register("kino_dispenser", () -> IForgeMenuType.create(KinoDispenserMenu::fromNetwork));

    private ModMenus() {}
}
