---
layout: default
title: Destiny Elevator
nav_order: 8
---

# Destiny elevator

The existing rear elevator can be upgraded in place. Right-click one of its original landing buttons or doors, or run `/sgjdestiny ship retrofitElevator` while in the Destiny dimension. Upgrade requires the original piston/slime car to be parked at a landing. The installer checks its complete 15-block footprint and the shaft before making changes. It preserves the surrounding floors, walls and wiring, and records replaced blocks in the world's `sgjdestiny_elevator.dat` saved data.

Each landing gets an outside call button. Calls made during a journey wait their turn; they do not interrupt that journey. Step into the cab and right-click its panel with an empty hand to select a destination. Riders attach to the cab during travel and can walk again at a landing. Landing doors open at the docked cab, and queued calls wait for boarding. Choosing a destination inside the cab takes priority over waiting calls.

Stop heights are fixed to the existing ship: **70, 75, 80, 85, 93, 99, 105, 111**. Horizontal travel is not implemented.

## Custom floor names

In the world's `serverconfig/sgjdestiny_dhd-server.toml`, edit:

```toml
[destinyElevator]
floorNameY70 = "Lower landing"
floorNameY75 = "Gate Room"
floorNameY80 = "Landing Y80"
floorNameY85 = "Landing Y85"
floorNameY93 = "Landing Y93"
floorNameY99 = "Landing Y99"
floorNameY105 = "Command Crew Quarters"
floorNameY111 = "Bridge"
```

Names may contain up to 48 characters and must not be blank. They are sent by the server each time a rider opens the destination panel. The Y coordinate remains visible to distinguish identically named stops. These settings rename stops; they do not move them.

The cab, pending calls and trip progress persist with the world. Logging out during a trip places that player outside the departure landing. A ship reset retires the previous cab and upgrades the newly rebuilt shaft, preventing duplicate elevators.

After reaching a landing, the cab settles before its doors unlock. Doors open when the cab is stopped, aligned with the landing, and its supports are present. A living entity in the doorway delays automatic closure.

## Landing doors and repairs

Existing spruce landing doors automatically become paired metal sliding panels when the elevator loads. They use the same polished-deepslate finish as the cab doors, retract toward the sides of the opening, and follow the elevator's docking controls during normal operation. An emergency release can override the interlock. They do not respond to redstone or vanilla door toggling. Missing door cells are left for repair; the automatic upgrade does not replace player-added blocks.

Search for **Destiny Elevator Repair Kit** in the inventory or recipe viewer. Craft one with this pattern (I = iron ingot, C = copper ingot, R = redstone dust, P = piston):

```text
I C I
R P R
I C I
```

Use it on a doorway, call button or shaft wall of the existing Destiny elevator while the cab is stopped. Occupants do not prevent servicing. One successful use repairs missing outer door cells at all eight landings, outside call buttons and their supports, the docked platform supports, and the pit marker. Occupied positions and unrelated replacement blocks are skipped. The kit reports incomplete repairs and is consumed only if at least one part was repaired (never in Creative). If the lift is moving, unavailable, or already intact, the kit is kept. It does not reset the ship or create a second elevator. Its cab itself remains invulnerable; this kit does not recreate an entity removed through administrative commands.

## Emergency rescue

Inside the cab, right-click the control panel with an empty hand and choose **EMERGENCY STOP / RELEASE DOORS**. This stops the floor at its current height, opens the cab doors, and opens the landing doors too if the cab is aligned with a landing. From outside, **crouch-use a repair kit on a landing doorway or adjacent wall** to stop the lift and release that landing's outer doors, even if the cab is elsewhere or unavailable. Emergency use never consumes the kit. A release between floors exposes the shaft for rescue; the platform stays at its current position.

Automatic calls remain disabled during an emergency. Once everyone is clear of the doorway and the shaft is unobstructed, select a destination inside the cab to close the release and resume. Emergency position and released landing persist through saving. Passengers saved by earlier builds are detached automatically on load.

Sneak-dismount during travel also requests an emergency stop.
