package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.menu.KinoDispenserMenu;
import com.dankscripts.sgjdestiny_dhd.menu.DestinyControlHubMenu;
import com.dankscripts.sgjdestiny_dhd.menu.DestinyAirScrubberMenu;
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

    public static final RegistryObject<MenuType<DestinyControlHubMenu>> DESTINY_CONTROL_HUB =
            MENUS.register("destiny_control_hub", () -> IForgeMenuType.create(DestinyControlHubMenu::fromNetwork));

    public static final RegistryObject<MenuType<DestinyAirScrubberMenu>> DESTINY_AIR_SCRUBBER =
            MENUS.register("destiny_air_scrubber", () -> IForgeMenuType.create(DestinyAirScrubberMenu::fromNetwork));

    private ModMenus() {}
}
