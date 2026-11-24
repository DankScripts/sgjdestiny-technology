---
layout: default
title: Destiny Ship
parent: Guides
nav_order: 5
---

# Destiny ship

The addon includes an optional Destiny ship built around Stargate Journey's existing Destiny gate. Installation is **off by default** and is a one-time operation per world; leaving the setting enabled does not rebuild it at every start. The installer preserves the existing gate and its address.

{: .warning }
Back up the entire world first. Installing the ship changes blocks around the gate. A ship reset clears and rebuilds the captured ship area, including player changes there; do not use it as a routine repair.

1. Install the matching addon and dependencies, start the world once, and stop the game or server.
2. Open `saves/<world>/serverconfig/sgjdestiny_dhd-server.toml` in singleplayer, or `<server>/<level-name>/serverconfig/sgjdestiny_dhd-server.toml` on a server.
3. Set `installCustomDestinyShip = true` under `[destinyShip]`, save, and restart.
4. Allow the installation to complete. If it does not, check the server log for the reported cause.

See [Configuration]({{ '/configuration.html' | relative_url }}) for the reset command and further precautions.

## On board

- Use the gate-room console or [handheld]({{ '/guides/handheld.html' | relative_url }}) to dial. A Kino Dispenser supplies two starter Kino/remote pairs; see [Kino operation]({{ '/guides/kino.html' | relative_url }}).
- Manage the atmosphere and equipment in [Life support]({{ '/destiny-life-support.html' | relative_url }}).
- Travel between decks with the [Destiny elevator]({{ '/DESTINY_ELEVATOR.html' | relative_url }}) and visit Eden with the [shuttle]({{ '/guides/shuttle.html' | relative_url }}).
- Explore the addon's creative tab and recipe viewer for ship construction blocks, doors, vents, railings, and other fixtures.

The life-support system uses a simplified room detector, not a full airtightness simulation. Read the life-support guide before venturing outside.
