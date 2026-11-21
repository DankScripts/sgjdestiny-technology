package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.menu.KinoDispenserMenu;
import com.dankscripts.sgjdestiny_dhd.menu.DestinyControlHubMenu;
import com.dankscripts.sgjdestiny_dhd.menu.DestinyAirScrubberMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DestinyDHD.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<KinoDispenserMenu>> KINO_DISPENSER =
            MENUS.register("kino_dispenser", () -> IMenuTypeExtension.create(KinoDispenserMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<DestinyControlHubMenu>> DESTINY_CONTROL_HUB =
            MENUS.register("destiny_control_hub", () -> IMenuTypeExtension.create(DestinyControlHubMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<DestinyAirScrubberMenu>> DESTINY_AIR_SCRUBBER =
            MENUS.register("destiny_air_scrubber", () -> IMenuTypeExtension.create(DestinyAirScrubberMenu::fromNetwork));

    private ModMenus() {}
}
