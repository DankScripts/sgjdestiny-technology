---
layout: default
title: Kino Operation
parent: Guides
nav_order: 3
---

# Kino Operation


## Deploy

The player must carry a Kino in their inventory. Press **Deploy** on the Kino page to consume and place it in front of the player. If the player's Kino is already deployed, Deploy changes it to Follow mode instead of creating another probe.

The Kino Dispenser stores up to 64 Kino/remote pairs. Right-click it and use **Pickup** to take a pair, or **Deposit** while carrying both items to return one. Pickup in survival requires two empty inventory slots. The bundled ship's dispenser starts with two pairs.

## Flight modes

| Control | Effect |
|:--|:--|
| Follow | Follows behind its owner |
| Hold | Hovers at its current position |
| Recall | Removes the deployed Kino and returns its item |
| Forward / Back | Moves relative to the Kino camera heading |
| Left / Right | Strafes and turns the Kino |
| Up / Down | Changes altitude |
| Scan | Reports dimension, atmosphere, temperature index, altitude, and biome |

When moving horizontally, the Kino adjusts its hover height as terrain rises or falls.

## Keyboard flight

While holding the remote with no screen open, use the arrow keys to control the Kino. In live video, use W/A/S/D to steer, Space to rise, Left Shift to descend, and Left Control to restore automatic hover height.

## Live video

Press the video control on the Kino page. Mouse movement rotates the camera through 360 degrees and looks up or down. Leaving video mode returns the camera to its stored forward-facing home direction.

The video is a full client-side world render supplied through Immersive Portals. The camera receives server-authoritative position samples at 10 Hz and interpolates between them for smooth motion. The Kino hides only from its own video render; it remains visible in the world to other players.

## Through a Stargate

Kinos may travel through an open Stargate. Cross-dimensional live video requires an open wormhole; relevant destination chunks are loaded for the controlling player.


