---
layout: default
title: Configuration
nav_order: 6
---

# Configuration

This addon uses a per-world **SERVER** config on Forge and NeoForge, so its settings belong to a world.
They are not in the main `config` folder alongside SGJourney's client and common
configs. After a world starts successfully with this addon installed, find the
file here:

| Game | Config file |
|:--|:--|
| Single-player | `<instance>/saves/<world name>/serverconfig/sgjdestiny_dhd-server.toml` |
| Dedicated server | `<server>/<level-name>/serverconfig/sgjdestiny_dhd-server.toml` |

For a dedicated server, `<level-name>` is the value of `level-name` in
`server.properties` (often `world`). Multiplayer uses the server's copy; editing
a client's local copy will not change that server. Exit the world or stop the
server before editing, then restart it.

## Optional packaged Destiny ship

```toml
[destinyShip]
installCustomDestinyShip = false
```

This defaults to `false` to protect existing Destiny ships. To opt in, back up
the world, then set the value to `true` and restart. The
ship is installed only once per world, so the value may remain `true` afterward.
The original Destiny gate and address are preserved; the packaged dialing
console and a Kino Dispenser containing two Kino/handheld kits are included.

An operator can reapply the packaged ship in an existing world with
`/sgjdestiny ship reset`. This requires the option to remain `true`. The command queues a new installation
over the captured ship area. It clears every existing block within the bundled
ship's capture bounds before rebuilding, including removed parts of older ship
layouts and player additions there. The original Stargate, the block supporting
its free-standing chevron, and the block beneath the player who ran the command
are preserved. The packaged dispenser inventory is restored. Blocks outside
the capture bounds are left alone. Back up the world before using it. If a
requested reset fails, it stops and the log reports
the failure; it does not repeat on later launches. Run the command again only
after fixing the cause.

The Destiny Door Controller is a wall panel for the nearest ship airlock.
Place it beside a door, including on the right-hand wall. Click its copper
button to press it briefly and open the door; the pale right indicator glows
white while the door is open. Crouch-click the panel to toggle the hard lock;
the left indicator glows red while locked. A controller crafts from two copper
ingots, one stone button, and one redstone dust, yielding two panels.

## Kino server limit

```toml
[kino]
maxDeployedKinos = 16
```

| Setting | Default | Range | Purpose |
|:--|--:|--:|:--|
| `kino.maxDeployedKinos` | 16 | 1–24 | Maximum number of simultaneously deployed Kinos across all loaded server dimensions |

When the limit is reached, additional deployment is refused before a Kino item is consumed. Recalling an active Kino frees a slot.

{: .note }
Each deployed Kino keeps its current chunk ticking so it can be controlled from any distance. Viewing the live feed temporarily loads additional nearby chunks and primarily affects the client rendering the video.

## Elevator floor names

The `[destinyElevator]` section names the eight existing stops. These settings change labels only, not the stop heights. See [Destiny elevator]({{ '/DESTINY_ELEVATOR.html' | relative_url }}) for the full list and operating instructions.
