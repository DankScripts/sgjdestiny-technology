# Destiny elevator

The existing rear elevator can be upgraded in place. Right-click one of its original landing buttons or doors, or run `/sgjdestiny ship retrofitElevator` while in the Destiny dimension. Upgrade requires the original piston/slime car to be parked at a landing. The installer checks its complete 15-block footprint and the shaft before making changes. It preserves the surrounding floors, walls and wiring, and records replaced blocks in the world's `sgjdestiny_elevator.dat` saved data.

Each landing gets an outside call button. Calls made during a journey wait their turn; they do not interrupt that journey. Step into the cab and right-click its panel to select a destination. Players can walk around on the moving floor during travel; they are not mounted or locked to passenger positions. Use an empty hand on the control panel to open it, including during a journey. The cab eases into motion and stops directly at the selected landing, with a maximum speed of three blocks per second. Landing doors open only at the docked cab. After arrival there is a five-second boarding interval before another queued call is served. Choosing a destination inside the cab takes priority over waiting calls.

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

The visible floor, walls, roof and sliding cab doors are rendered together throughout the trip and while parked. Travel progress comes from the server, independent of the client's dimension clock. Invisible blocks supply collision and lighting at the dock; they cannot show a second platform ahead of the cab. Existing dev168 parked floors are converted automatically when the cab loads. No ship reset or elevator retrofit is needed for this update.

After reaching a landing, the cab settles for half a second with both its own doors and the landing doors closed. Doors unlock only when the cab is stopped, aligned with the landing, and all four support blocks are present. During travel, riders attach to the cab; the floor supports normal walking at the dock. Clients apply the stopped position before later door updates; offhand clicks cannot bypass the landing controls. A saved cab retains its docking progress.

## Landing doors and repairs

As of dev173, the outer doors and frame are 2.5 blocks tall to match the cab's door panels. Existing upper door cells extend upward by half a block; no extra blocks or world conversion are required. A top slab in the block above the doorway meets the frame at its top. Both open and closed collision shapes follow the taller frame. The extension uses explicit texture coordinates to avoid sampling adjacent textures.

Existing spruce landing doors automatically become paired metal sliding panels when the elevator loads. They use the same polished-deepslate finish as the cab doors, retract toward the sides of the opening, and follow the elevator's docking controls during normal operation. An emergency release can override the interlock. They do not respond to redstone or vanilla door toggling. Missing door cells are left for repair; the automatic upgrade does not replace player-added blocks.

Search for **Destiny Elevator Repair Kit** in the inventory or recipe viewer. Craft one with this pattern (I = iron ingot, C = copper ingot, R = redstone dust, P = piston):

```text
I C I
R P R
I C I
```

Use it on a doorway, call button or shaft wall of the existing Destiny elevator while the cab is stopped. Occupants do not prevent servicing. One successful use repairs missing outer door cells at all eight landings, outside call buttons and their supports, the docked platform supports, and the pit marker. Occupied positions and unrelated replacement blocks are skipped. The kit reports incomplete repairs and is consumed only if at least one part was repaired (never in Creative). If the lift is moving, unavailable, or already intact, the kit is kept. It does not reset the ship or create a second elevator. Its cab itself remains invulnerable; this kit does not recreate an entity removed through administrative commands.

A hidden marker in the original obsidian pit records the landing for future ship captures. This lets a bundled structure recreate its cab even though the ship exporter excludes entities. The marker has no inventory item and does not alter the visible floor patterns.

## Emergency rescue

Inside the cab, right-click the control panel with an empty hand and choose **EMERGENCY STOP / RELEASE DOORS**. This stops the floor at its current height, opens the cab doors, and opens the landing doors too if the cab is aligned with a landing. From outside, **crouch-use a repair kit on a landing doorway or adjacent wall** to stop the lift and release that landing's outer doors, even if the cab is elsewhere or unavailable. Emergency use never consumes the kit. A release between floors exposes the shaft for rescue; the platform stays at its current position.

Automatic calls remain disabled during an emergency. Once everyone is clear of the doorway and the shaft is unobstructed, select a destination inside the cab to close the release and resume. Emergency position and released landing persist through saving. Passengers saved by earlier builds are detached automatically on load.

## Validation

The dependency-free `LiftMotionTest` exercises all 64 combinations of the eight stops, including fractional-tick motion, endpoint bounds, speed, acceleration/deceleration, deterministic resumption, late-tracking progress snapshots and door timing. `tools/verify_destiny_elevator.py` checks the bundled ship's retrofit footprint and route clearance. These checks do not substitute for an in-game client/multiplayer ride test.


## dev173 boarding and travel

Parked doors default closed. Calling the current floor opens both sets of doors for 160 ticks (eight seconds at 20 TPS). Arrival opens them only after the existing docking interlock completes. A living entity in the doorway resets the boarding timer, preventing automatic closure on occupants. Choosing a destination closes the doors before movement; a trip started with closed doors does not briefly reopen them. Pending calls wait until boarding finishes. The remaining boarding time is saved.

For this build, riders attach to the cab during travel to avoid upward collision/gravity camera jitter. They keep their standing positions and can look around; walking resumes on arrival or emergency release. Sneak-dismount requests an emergency stop on the next server tick. The console emergency release and external rescue kit remain available. In-game multiplayer camera behavior still requires testing.
