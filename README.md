# Stargate Journey: Destiny Technology

A standalone Forge 1.20.1 addon for Stargate Journey that recreates Destiny's
shipboard dialing console from *Stargate Universe*.

The addon includes the Destiny dialing console, lore-based seven- and
nine-chevron lighting, floor-chevron and shutdown-steam effects, a handheld
remote, deployable Kinos, telemetry, cross-dimensional flight, and live video.

## Documentation

The [searchable documentation site](https://dankscripts.github.io/sgjdestiny-technology/)
contains installation, setup, controls, recipes, configuration, and
troubleshooting instructions. Its source is in [`docs`](docs/index.md).

## Requirements

- Minecraft 1.20.1
- Forge 47.4.0 or newer 47.x build
- Stargate Journey 0.6.49 or newer (0.6.50 recommended)
- Immersive Portals 3.0.7
- Cloth Config for Immersive Portals
- Simply Light 1.4.6+, GTCEu 7.2.0+, Macaw's Bridges 3.1.0+, and
  SGJourney Deco 1.2.0+

Use a diamond-tier Stargate Journey graver on the Destiny console or its screen
to open the Universe DHD engraving menu. Normal clicks still open the custom
Destiny dialer.

Forge checks all listed dependencies before opening a world and displays its
normal missing-mods screen if any are absent. The packaged ship itself remains
disabled until explicitly enabled for a world.

## Optional Destiny ship

The packaged ship is disabled by default to protect existing Destiny builds.
Start the world once with the mod installed, then stop the game or server and
set `destinyShip.installCustomDestinyShip = true` in
`saves/<world name>/serverconfig/sgjdestiny_dhd-server.toml` for single-player,
or `<server>/<level-name>/serverconfig/sgjdestiny_dhd-server.toml` for a
dedicated server. It is a per-world server config, not a file in the main
`config` folder. Start the world again to install the ship.
Back up the world first: opting in replaces the original Destiny room and
installs the captured ship in `sgjourney:destiny`. The existing Stargate and its
address are preserved. The Destiny dialing console is included, and the ship's
Kino Dispenser starts with two Kino/handheld kits. Installation runs once per
world.
Leave the setting `true` after installation; a saved completion marker prevents
the ship from being replaced again on later starts. Keep the ship-block mods
installed on the server and every client. Missing dependencies are reported by
Forge before a world begins loading.
Destiny's weather is always clear, independent of this setting.

## Development

Run `gradlew build` on Windows or `./gradlew build` on Linux.

The accepted model generator is `tools/destiny_console_generator_v4.py`. Run it
with Blender 5.2, then run `tools/prepare_minecraft_obj.py` with Blender to
regenerate the scaled, UV-mapped OBJ and Minecraft texture-backed MTL resources.
The conversion preserves the accepted Blender output and writes only to this
project's model resource directory.
