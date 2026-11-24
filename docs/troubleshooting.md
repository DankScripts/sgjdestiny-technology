---
layout: default
title: Troubleshooting
nav_order: 9
---

# Troubleshooting

## Minecraft reports a missing dependency

Install the matching Forge 1.20.1 or NeoForge 1.21.1 releases and required dependencies on both client and server. The loader lists missing dependencies and versions on its error screen. Remove duplicate or incompatible versions. See [Installation]({{ '/getting-started/installation.html' | relative_url }}).

## The Kino video page is blank

- Confirm the appropriate Immersive Portals release is installed and loaded.
- Confirm a Kino is deployed and the player still holds the paired remote.
- Wait briefly for the destination chunks to load after crossing dimensions.
- For cross-dimensional video, keep the connecting wormhole open.
- Check `latest.log` for errors mentioning `immersive_portals`, `sgjourney`, or `sgjdestiny_dhd`.

## The Kino will not deploy

- Carry at least one Kino in the inventory.
- Recall the player's existing Kino, or press Deploy to switch it into Follow mode.
- Check whether the server-wide Kino limit has been reached.

## The floor chevron does not illuminate

Use Stargate Journey's **Universe Stargate Chevron**, place it horizontally in the floor directly in front of the gate, and confirm the dial reaches the final lock or establishes a connection.

## The handheld cannot dial

The remote needs a usable registered Universe gate in the **current dimension**, but it does not require a nearby console or have a fixed distance limit. Check the message in game and confirm that the gate is registered and available. The remote cannot select a non Universe gate.

If a nearby Universe DHD is selected, check that it is linked to a usable gate. On NeoForge, an interface opened directly to Kino control has an empty gate list: close it and check the remote's tooltip. If it is in Kino Control mode, sneak-use it to select gate mode, then reopen it.

## The packaged Destiny console cannot find its gate

Verify the gate and console are present, the gate is powered and within the console's connection range, and neither is already linked to a different controller. On NeoForge, the hardwired console discovers eligible gates in loaded chunks; make sure both chunks are loaded and allow discovery to retry. Check the server log for link diagnostics. Do not reset the ship or replace the gate solely to troubleshoot a link.

## Ship installation or reset failed

Confirm the per-world `installCustomDestinyShip` setting is enabled and read the server log for the reported cause. Back up the world before installing or resetting; reset clears the ship capture area. See [Destiny ship]({{ '/guides/destiny-ship.html' | relative_url }}).

## Reporting a problem

Include the addon version, Minecraft and Forge or NeoForge version, Stargate Journey version, Immersive Portals version, a short reproduction sequence, and the relevant `latest.log` section.
