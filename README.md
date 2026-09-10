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
- Stargate Journey 0.6.48-hotfix1
- Immersive Portals 3.0.7
- Cloth Config for Immersive Portals

## Development

Run `gradlew build` on Windows or `./gradlew build` on Linux.

The accepted model generator is `tools/destiny_console_generator_v4.py`. Run it
with Blender 5.2, then run `tools/prepare_minecraft_obj.py` with Blender to
regenerate the scaled, UV-mapped OBJ and Minecraft texture-backed MTL resources.
The conversion preserves the accepted Blender output and writes only to this
project's model resource directory.
