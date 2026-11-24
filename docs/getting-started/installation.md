---
layout: default
title: Installation
parent: Getting Started
nav_order: 2
---

# Installation

## Requirements

| Component | Forge release | NeoForge release |
|:--|:--|:--|
| Minecraft | 1.20.1 | 1.21.1 |
| Mod loader | Forge 47.4.0 or newer compatible 47.x | NeoForge 21.1.252 or newer compatible 21.1.x |
| Java | 17 | 21 |
| Stargate Journey | 0.6.55-alpha or newer compatible 1.20.1 release | 0.6.55-alpha or newer compatible 1.21.1 release |
| Immersive Portals | 3.0.7 for Forge 1.20.1 | Compatible NeoForge 1.21.1 port, 6.0.7 or newer |

Install any additional dependencies required by your chosen Immersive Portals release. SGJourney Deco 1.2.0 or newer is optional; the NeoForge addon includes a fallback Destiny bearing. Distant Horizons is optional.

A diamond-tier SGJourney graver opens the Universe DHD engraving menu from the Destiny console
or its screen; normal clicks still open the Destiny dialer.

The loader displays its normal missing-mods screen before opening a world if a
required dependency is absent. The packaged ship itself remains opt-in. See
[Configuration]({{ '/configuration.html' | relative_url }}) for the ship
setting and its single-player and dedicated-server file locations.

## Client and server

Install matching releases of Stargate Journey: Destiny Technology and its required dependencies on the server and every connecting client. Use the correct loader and Minecraft version for each mod. Immersive Portals provides the Kino's remote world view.

1. Stop Minecraft or the server.
2. Remove older Destiny Technology jars from the `mods` folder.
3. Add the current addon jar and required dependencies.
4. Start the game and confirm the addon appears in the Mods screen.

{: .warning }
Do not keep multiple versions of this addon in the same `mods` folder.
