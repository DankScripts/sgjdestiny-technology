# SGJourney: Destiny DHD

A Forge 1.20.1 addon for Stargate Journey that recreates Destiny's shipboard
dialing console from *Stargate Universe*.

## Current release

This source matches `sgjdestiny-dhd-1.20.1-0.0.1-alpha`.

- Minecraft 1.20.1
- Forge 47.4.0 or newer 47.x build
- Java 17
- Stargate Journey 0.6.48-hotfix1 or newer compatible 0.6.x build
- SGJ Deco 1.2.0 or newer is optional

## Features

- Placeable, three-block-tall Destiny DHD console using the accepted Blender v4 model
- Correct placement orientation with the operator side facing the player
- Full-height interaction targets, including the monitor
- SGJourney Universe DHD dialer on right-click
- SGJourney crystal and Fusion Core interface on sneak-right-click
- Universe DHD power, address, gate discovery, dialing, sound, and save/load behavior
- Survival crafting recipe plus a liquid-Naquadah Crystallizer alternative
- Shared Forge ingredient and fluid tags for GregTech and Almost Fluidified compatibility
- Optional SGJ Deco Destiny Bearing integration
- Bearing bulb pre-light before dialing, continuous illumination through gate activity,
  and reliable restoration when the gate returns to idle

The Destiny DHD can be found in the Functional Blocks creative tab by searching
for `Destiny DHD`.

## Building

See [BUILDING.md](BUILDING.md). A successful build places the mod JAR in
`build/libs/`.

## Model source

The accepted console generator is `tools/destiny_console_generator_v4.py`.
The generated Blender output is converted for Minecraft with
`tools/prepare_minecraft_obj.py`. The checked-in OBJ, MTL, JSON models, and
textures under `src/main/resources/` are the runtime assets used by the mod.

## Downloads and support

Use the official CurseForge project for supported release downloads. Files
built directly from this repository may contain unfinished development work.

## License

Copyright (c) 2026 DankScripts. All Rights Reserved.

The repository is source-visible for inspection and compatibility work. No
permission is granted to copy, modify, redistribute, or republish the software
or its original assets without prior written permission. See
[LICENSE.txt](LICENSE.txt).

