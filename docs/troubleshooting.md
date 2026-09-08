---
layout: default
title: Troubleshooting
nav_order: 7
---

# Troubleshooting

## Minecraft reports a missing dependency

Install matching Forge 1.20.1 builds of Stargate Journey, Immersive Portals, and Cloth Config on both client and server. Remove duplicate or incompatible versions.

## The Kino video page is blank

- Confirm Immersive Portals 3.0.7 is installed and loaded.
- Confirm a Kino is deployed and the player still holds the paired remote.
- Wait briefly for the destination chunks to load after crossing dimensions.
- Check `latest.log` for errors mentioning `immersive_portals`, `sgjourney`, or `sgjdestiny_dhd`.

## The Kino will not deploy

- Carry at least one Kino in the inventory.
- Recall the player's existing Kino, or press Deploy to switch it into Follow mode.
- Check whether the server-wide Kino limit has been reached.

## The floor chevron does not illuminate

Use Stargate Journey's **Universe Stargate Chevron**, place it horizontally in the floor directly in front of the gate, and confirm the dial reaches a successful final lock. It should remain dark during incomplete or failed dialing.

## The handheld cannot dial

The remote still needs a usable Universe gate nearby. It does not require the Destiny console. Move within approximately 32 horizontal and 16 vertical blocks of the gate and try again.

## Reporting a problem

Include the addon version, Forge version, Stargate Journey version, Immersive Portals version, a short reproduction sequence, and the relevant `latest.log` section.
