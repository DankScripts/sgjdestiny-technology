# Changelog

## 0.0.94-dev

- Adds the humid Jungle Planet visited after the ice world, with dense jungle,
  bamboo, vines, caves, hostile wildlife, and an overgrown Universe Stargate
  pedestal.
- Registers the Jungle Planet in the handheld address database with a stable
  seven-chevron destination.
- Adds Squigglers: fast, low-profile pack predators inspired by the venomous
  creatures encountered by Destiny's expedition, including a poisonous bite
  and natural jungle spawning.

## 0.0.61-dev

- Locates the floor chevron along the gate-to-console axis, independent of the
  Universe gate's direction and the chevron block's stored orientation.
- Latches the floor chevron on when the final seventh or ninth chevron locks,
  keeps it lit through the wormhole, and clears it when the gate returns idle.

## 0.0.60-dev

- Fixes floor-chevron discovery for Universe gates whose direction has no
  horizontal component. Horizontal chevrons are now selected by face orientation
  and their position below the gate center.

## 0.0.59-dev

- Fixes Destiny floor-chevron discovery by identifying the chevron geometrically
  in front of the gate instead of assuming a particular placement orientation.
- Synchronizes its lit state without neighbor updates so the chevron's normal
  redstone response cannot immediately cancel the connection indicator.

## 0.0.58-dev

- Normal seven-chevron dialing now flashes the Destiny bearing for each lock,
  leaves it dark between locks, and holds it on after a successful connection.
- Known nine-chevron programmed dials keep the bearing powered throughout the
  dialing sequence while retaining the brighter lock pulses and discharge effect.
- A horizontal SGJourney Universe chevron placed in the floor near the gate now
  lights only while the wormhole is successfully connected.
- Retains the exposed polished Naquadah-Copper vent body and working steam grates.

## 0.0.15-dev (post-alpha)

- Added a 20-tick pause after the gate becomes inactive before the first steam
  burst and hiss, matching the slight post-shutdown delay from the show.
- Preserves the accepted steam duration, density, coverage, angle, distance,
  and 24-block sound radius, plus the working 0.0.14 discharge path.

## 0.0.14-dev (post-alpha)

- Consolidated lightning and steam client providers into the single particle
  registration callback already proven active by the visible custom steam.
- Removed the independently discovered nested lightning registration that was
  the only remaining difference between the working 0.0.8 client path and the
  later two-particle setup.
- Added an explicit startup log confirming that both custom providers registered.
- Retains the 0.0.8 discharge renderer and emission bytecode, persistent outgoing
  lifecycle, bearing resolution, perfect steam visuals, and 24-block hiss radius.

## 0.0.13-dev (post-alpha)

- Fixed the silent server-side bearing-cache gate that could start a discharge
  state but return before sending any lightning particles.
- Discharge state now captures its physical bearing, re-resolves it from the
  bounded loaded gate area when necessary, and records its first successful
  particle emission in the log.
- Increased the repeating steam-hiss volume to Minecraft's 24-block attenuation
  radius in every direction without changing the accepted steam visuals.

## 0.0.12-dev (post-alpha)

- Restored the complete field-tested 0.0.8-dev bearing-discharge renderer and
  endpoint geometry exactly, while retaining the current persistent outgoing
  nine-chevron lifecycle through connection close and gate shutdown.
- Added a repeating localized pressure hiss for the full steam-release period;
  the accepted 0.0.11 steam visuals are unchanged.

## 0.0.11-dev (post-alpha)

- Expanded shutdown steam across nearly the full vent grate with parallel
  pressure lanes and front-to-back depth instead of a narrow centerline.
- Greatly increased sustained steam density and sprite size while preserving
  the accepted 45-degree angle, velocity, lifetime, and travel distance.
- Added a server-tick fallback that restores the bearing discharge whenever a
  nine-symbol Destiny address is waiting, even if SGJourney bypasses the normal
  symbol hook or briefly loses the cached gate during the input sequence.
- Strengthened the lightning corona, cyan body, white core, and branches so the
  discharge remains unmistakably visible from normal gate-room distances.
- Fixed the bolt render plane to select the face of the Stargate that points
  toward its linked Destiny DHD, preventing the ring and ceiling from hiding
  an otherwise active discharge on oppositely oriented gates.

## 0.0.10-dev (post-alpha)

- Increased shutdown-steam density, opacity, and plume width while preserving
  the accepted 45-degree angle and travel distance.
- Fixed nine-chevron discharge stopping during the Universe gate's dialing
  sequence when its engaged-chevron count briefly returns to zero.
- Discharge now survives every active outgoing dialing phase and successful
  outgoing connection, then stops when the gate truly returns to idle.

## 0.0.9-dev (post-alpha)

- Changed the bearing discharge to a true 45-degree left/right V by matching
  each bolt's horizontal travel to its vertical drop.
- Reworked the lightning into a fine white-hot filament, cyan body, and softer
  outer corona with tighter natural jitter and less ribbon-like thickness.
- Keeps the discharge active indefinitely after the ninth symbol is entered
  while the console waits for the center engage button.
- A successful outgoing nine-chevron connection keeps discharging until that
  wormhole closes; incoming connections never create the discharge.
- Replaced scattered vanilla steam puffs with smooth, growing pressure plumes
  that leave each floor vent in a true outward 45-degree stream.
- Converted the floor vent from a thin cover into a full solid floor block and
  recessed its grate so the top is flush with neighboring blocks.

## 0.0.8-dev (post-alpha)

- Added a low-profile, directional Destiny Floor Vent block with a metal grille model,
  survival recipe, block drop, item model, translation, and creative inventory entry.
- Nearby floor vents now release dense white steam for several seconds when a
  Destiny-controlled Stargate finishes shutting down and returns to idle.
- Steam emission is bounded to loaded blocks near the gate and does not scan while idle.
- Pulled the nine-chevron discharge forward from the gate plane, widened its
  shoulder strike points, and strengthened its cyan sheath and white core so
  both arcs remain visible with the bearing mounted close to the gate.

## 0.0.7-dev (post-alpha)

- Moved nine-chevron discharge impacts away from the Stargate's top center and
  farther down/outward across its upper-left and upper-right rim.
- Allows the Destiny Bearing to remain close to the gate beneath full ceiling
  blocks while keeping both diagonal energy bolts clearly visible.
- Preserves the accepted `0.0.6-dev` immediate renderer and all bearing timing.

## 0.0.6-dev (post-alpha)

- Fixed the invisible discharge regression in `0.0.5-dev`.
- Restored the particle-relative coordinates required by Minecraft's particle
  camera while moving the bolt draw into an immediate position/color buffer.
- Prevents the shared level buffer from retaining camera-relative bolt geometry
  outside the particle render call.

## 0.0.5-dev (post-alpha)

- Fixed the nine-chevron energy bolts following the player's camera instead of
  remaining anchored between the Destiny Bearing and Stargate.
- Removed the duplicate camera translation from the level-buffer lightning
  renderer while retaining camera-facing bolt width and world-space endpoints.

## 0.0.4-dev (post-alpha)

- Fixed the `0.0.3-dev` server crash caused by development-name SGJourney
  block-entity calls surviving the manual runtime remap.
- Restored the complete proven `0.0.2-dev` bearing compatibility class and
  transplanted only the new lightning-emission method into it.
- Retains the jagged blue-white ribbon lightning renderer introduced for the
  nine-chevron discharge test.

## 0.0.3-dev (post-alpha)

- Replaced the bubble-like nine-chevron discharge particles with continuous,
  jagged blue-white lightning ribbons.
- Added a bright white electrical core, cyan outer glow, and two short branches
  to each rapidly reconnecting main bolt.
- Generates bolt geometry locally on each client from two bounded server events,
  preserving the accepted nine-chevron trigger and Destiny Bearing bulb behavior.

## 0.0.2-dev (post-alpha)

- Added a nine-chevron-only Destiny Bearing discharge prototype.
- The effect begins as the ninth address symbol is submitted, before SGJourney
  evaluates the final dial, with the engage action retained as a fallback.
- Emits two rapidly reconnecting blue-white arcs plus a smaller branch from the
  bearing's lower emitter toward varying points across the Stargate's upper rim.
- Keeps the established bearing bulb behavior independent from the discharge.
- Stops the discharge on connection, cancellation, failed-dial reset, loss of
  DHD range, or a safety timeout.
- Reuses the cached bearing position and emits at a bounded three-tick interval;
  no additional world scan was added to the normal tick path.

## 0.0.1-alpha

- Corrected a packaging/remapping error in the first performance-test artifact
  that renamed Java collection `isEmpty()` calls and crashed the server while
  ticking an existing Destiny Console.
- Performance test revision: cached each discovered SGJ Deco Destiny Bearing so
  normal dial and shutdown transitions update its exact position directly.
- Replaced the 33x33x33 recovery cube with an 11x13x11 upper-gate search that
  inspects loaded chunks only and therefore cannot force chunk loads while dialing.
- Normal active-to-idle shutdown now restores tracked bearings without any
  discovery scan.

- Fixed an SGJ Deco Destiny Bearing recovery edge case where the illuminated
  replacement block could remain in the world after its transient ownership
  tracking was lost.
- The console now re-adopts an already-lit bearing at dial start and performs a
  one-time world-state recovery scan when the gate returns to idle.
- Recovery remains transition-driven, so the fix does not restore the old
  per-tick 35,937-block area scan.

- Promoted the accepted `0.0.7-dev` baseline to the first alpha without changing
  gameplay behavior, model geometry, integrations, or recipes.

- Changed the standard Destiny Console recipe to use shared compatibility tags
  for Iron Ingots, black glass panes, and Pure Naquadah.
- The four Naquadah-Alloy slots continue to use the shared Forge alloy tag.
- This allows correctly tagged GregTech and other mod equivalents while keeping
  the vanilla and SGJourney ingredients valid.

## 0.0.6-dev

- Removed the Destiny Bearing and Universe DHD from all Destiny Console recipes.
- Replaced the two Stone Buttons with two Iron Ingots.
- The standard recipe now uses four Naquadah-Alloy ingots, two Iron Ingots,
  one Black Stained Glass Pane, one Redstone Comparator, and one Pure Naquadah.
- Updated the liquid-Naquadah Crystallizer alternative to use a Comparator,
  two Iron Ingots, four Naquadah-Iron Alloy ingots, and 1,000 mB of liquid
  Naquadah, with no SGJ Deco recipe dependency.

## 0.0.5-dev

- Added a survival crafting recipe for the Destiny DHD Console.
- The recipe uses a Destiny Bearing, an SGJourney Universe DHD, four current
  Naquadah-Alloy ingots, two stone buttons, and Pure Naquadah.
- The recipe is conditionally enabled when SGJ Deco is installed, while the mod
  itself remains compatible with installations that do not include SGJ Deco.
- Added an alternate SGJourney Crystallizer recipe using one Universe DHD, one
  Destiny Bearing, four Naquadah-Iron Alloy ingots, and 1,000 mB of liquid
  Naquadah.
- Added SGJourney liquid Naquadah to `forge:naquadah`, allowing Almost
  Fluidified to substitute GregTech Naquadah fluid when its Naquadah fluid
  unification is enabled.

## 0.0.4-dev

- Added an optional SGJ Deco 1.2.0 compatibility override for the Destiny Bearing recipe.
- Replaced the removed `sgjourney:naquadah_alloy` ingredient tag with the current `forge:ingots/naquadah_alloy` tag.
- Preserved the original recipe layout and Pure Naquadah ingredient.

## 0.0.3-dev

- Prevented the Destiny Bearing from blinking off and back on during a failed
  Universe Stargate dial while the ring begins returning to its idle position.
- Added a five-tick idle debounce across SGJourney's internal failure/reset gap;
  the bearing remains continuously lit through the return rotation and switches
  off after that rotation finishes.

## 0.0.2-dev (pre-alpha)

- Fixed the optional SGJ Deco Destiny Bearing sometimes remaining illuminated after
  a rapid or aborted dial returned the Stargate to idle.
- Bearing activity now follows the Universe Stargate's live dialing buffer, rotation,
  chevrons, and connection state instead of treating a stale encoded-symbol list as
  an active dial.
- Preserved the accepted pre-dial illumination timing and the continuous light through
  the first-chevron transition.

## 0.0.1-alpha.1

- First official public alpha baseline, promoted unchanged from the confirmed `0.1.0-alpha.40-test.10` behavior.
- Provides the placeable Destiny DHD console with SGJourney's Universe DHD dialing, power, and crystal interfaces.
- Supports right-click interaction across the full three-block-tall console, including its monitor.
- Adds optional SGJ Deco Destiny Bearing support: the bulb illuminates before dialing begins, remains lit through the first-chevron rotation gap, and follows gate activity afterward.
- Optimizes Destiny Bearing discovery to scan only when dialing begins instead of checking 35,937 nearby blocks every tick.
- Maintains existing-console interaction targets once per second instead of every tick.

## 0.1.0-alpha.40-test.10

- Keeps the Destiny Bearing bulb continuously lit between the first symbol release and the first chevron lock.
- Transfers light control back to SGJourney's normal active-gate state as soon as gate activity is observed.
- Adds a ten-second safety timeout if a first-symbol attempt never produces gate activity.

## 0.1.0-alpha.40-test.9

- Moved Destiny Bearing pre-lighting from the final engage button to the first dialing symbol.
- Holds the bearing light on during the three-tick pre-dial lead instead of allowing status polling to restore it.
- The first chevron dialing action now begins only after the bearing bulb has been illuminated.

## 0.1.0-alpha.40-test.8

- Hooks SGJourney's actual DHD engage method, including stock block entities restored from existing worlds.
- Gives the bearing bulb a three-tick client-visible lead before releasing the gate dial action.
- Adds invisible, non-colliding interaction targets over the monitor so screen clicks reach the console.

## 0.1.0-alpha.40-test.7

- Separated bearing illumination and gate dialing into distinct server ticks so the bulb visibly activates first.
- Expanded the console interaction shape upward and across the monitor, allowing right-clicks directly on the screen.

## 0.1.0-alpha.40-test.6

- Moved the optional SGJ Deco Destiny Bearing activation into the DHD's engage action.
- The bearing bulb is now switched on before SGJourney is told to begin dialing.
- Preserved the confirmed bulb-only illuminated model and normal polling-based shutoff.

## 0.1.0-alpha.40-test.5

- Removed the five-tick Destiny Bearing polling delay.
- Destiny Bearing bulbs now respond on the first server tick that dialing activity is visible.

## 0.1.0-alpha.40-test.4

- Corrected Destiny Bearing illumination so only the hanging bulb/lens glows.
- Preserved SGJ Deco's original dark housing, braces, stem, and ceiling mount.

## 0.1.0-alpha.40-test.3

- Replaced the non-firing SGJourney connection-event bearing hook with direct
  reconciliation from the Destiny DHD's proven server ticker.
- Nearby SGJ Deco Destiny Bearings now illuminate from the start of dialing and
  return to their original state after the gate becomes idle or disconnects.
- Preserved alpha.39's accepted console model, dialer, crystal controls, power,
  gate discovery, placement, collision, textures, and optional SGJ Deco loading.

## 0.1.0-alpha.39

- Corrected bearing range semantics: communication crystals now determine
    whether the Destiny DHD can read the gate, while the bearing search stays
    local to the gate structure.

## 0.1.0-alpha.38

- Fixed bearing activation being suppressed when the DHD-to-gate distance was
    outside the separate DHD connection-distance field.
- The Destiny DHD is now used to identify the controller, while its installed
    communication crystals determine the gate-centered bearing scan radius.

## 0.1.0-alpha.37

- Fixed bearing range calculation to use the installed communication crystals
    themselves instead of SGJourney's separate fixed DHD connection-distance
    field.
- Added SGJourney's gate-connect event as an activation path alongside the
    established-connection event.

## 0.1.0-alpha.36

- Moved Destiny Bearing activation from the early dial event to SGJourney's
    connection-established event, so the gate and DHD are fully available
    before the bearing scan runs.
- The DHD discovery radius is now 64 blocks; the installed communication
    crystal distance still controls whether the DHD can read and light the
    bearing area.

## 0.1.0-alpha.35

- Bearing activation now uses the Destiny DHD's effective SGJourney
    communication distance, including installed communication crystals, for
    both gate association and bearing visibility.
- The bearing remains anchored to the active gate instead of relying on a
    separate fixed DHD-to-bearing radius.

## 0.1.0-alpha.34

- Improved optional SGJ Deco Destiny Bearing activation by searching a larger
    area around the active gate, preserving shared block properties, and
    explicitly refreshing the light engine after each bearing swap.
- Lit bearings retain full block light (15), making them brighter than the
    Destiny gate's white lights when the gate is active.
- The creative item name remains `Destiny DHD`, so searching `dhd` finds it
    directly.

## 0.1.0-alpha.33

- Fixed the Destiny console block entity being rejected by SGJourney's
    Universe DHD block entity type, which prevented all server-side ticking
    and left the energy buffer at zero.
- Existing placed consoles now retain their inventory and begin ticking after
    the game restarts with this version.

## 0.1.0-alpha.32

- Restored automatic Fusion Core and Universe dialing-crystal installation
    before the normal dialer opens, matching alpha.30's working power behavior.
- Retained sneak-right-click Crystal Controls and optional Destiny Bearing
    illumination from alpha.31.

## 0.1.0-alpha.31

- Restored sneak-right-click access to the Universe DHD crystal interface with
    a Destiny-aware menu that remains open for the custom console block.
- Temporarily removed automatic hardware generation; superseded by alpha.32.
- Added optional SGJ Deco 1.2.0 integration: nearby Destiny Bearing blocks use
    their illuminated texture while a Destiny-controlled gate is active.
- Preserved alpha.30's normal right-click Universe dialer behavior.

## 0.1.0-alpha.30

- Fixed the Universe dialer opening and immediately closing because SGJourney's stock menu only accepts the stock Universe DHD block.
- Added a Destiny-aware server menu while retaining SGJourney's standard Universe DHD menu type and client screen.
- Preserved automatic hidden Fusion Core/crystal initialization and the disabled sneak-right-click crystal interface.
- Preserved the accepted console model, textures, placement orientation, hitbox, and no-water behavior.

## 0.1.0-alpha.29

- Removed the incorrect protected-DHD permission gate from normal right-click dialing.
- Initializes the console's internal Fusion Core and Universe dialing hardware before opening the dialer.
- Players do not need to insert a Fusion Core or removable DHD crystals.
- Shift-right-click remains disabled and no crystal-management interface is exposed.
- Preserved the alpha.28 water fix and the accepted console model and textures.

## 0.1.0-alpha.28

- Fixed the Destiny console incorrectly placing with `WATERLOGGED=true`.
- Normal right-click now opens the Universe dialer from every console face.
- Removed the sneak-right-click crystal interface.
- Destiny dialing no longer requires players to install removable DHD crystals.
- Preserved the accepted alpha.26 model, textures, UV layout, orientation, and collision.

## 0.1.0-alpha.27

- Backed the Destiny console with SGJourney's complete Universe DHD implementation.
- Normal right-click now opens the SGJourney Universe DHD dialer.
- Sneak-right-click now opens the SGJourney Universe DHD crystal interface.
- Inherits Universe DHD dialing, crystals, power, gate discovery, address, sound, save/load, and ticking behavior.
- Preserved the accepted alpha.26 model, textures, UV layout, placement orientation, and collision geometry.

## 0.1.0-alpha.26

- Populated substantially more of the Destiny DHD display with Ancient-style writing.
- Reworked the screen artwork into three fuller horizontal text bands with subtle distressed dividers.
- Preserved the alpha.25 UV atlas mapping so the complete display remains visible without cropping.
- Preserved the accepted table model, placement orientation, collision, OBJ geometry, and compiled behavior.

## 0.1.0-alpha.25

- Repacked the approved Ancient-language artwork into both existing display
  UV-atlas regions so neither side samples a cropped section of the writing.
- The complete glyph composition now fills the display while retaining
  alpha.24's confirmed 90-degree counter-clockwise orientation.
- Preserved alpha.24's corrected player-facing placement, physical geometry,
  OBJ model, collision, dimensions, and all compiled behavior.

## 0.1.0-alpha.24

- Rotated the inactive Ancient glyph screen texture 90 degrees
  counter-clockwise so its writing has the intended in-game orientation.
- Corrected placement facing so the console's operator side faces the player
  who places it.
- Preserved the alpha.23 model, collision geometry, scale, and screen artwork.

## 0.1.0-alpha.23

- Replaced the inactive display's green vertical column grid with a nearly
  black, subtly weathered screen.
- Added dim, irregular ivory-amber Ancient-style glyph notation inspired by
  the Destiny console reference imagery.
- Increased the screen texture from 16x16 to 128x128 so the glyph shapes
  remain legible after Minecraft texture filtering and mipmapping.
- Preserved all alpha.22 model geometry, materials, placement behavior,
  dimensions, rotation, and composite collision.

## 0.1.0-alpha.22

- Gave the dial's white face a dedicated borderless ivory texture, removing
    the gray texture edges that appeared as an up-arrow in Minecraft.
- Promoted the complete console source to `destiny_console_generator_v4.py`.

## 0.1.0-alpha.21

- Rebuilt the bronze selector star with explicit radial triangles, removing
    the two gray diagonal bars that formed an up-arrow behind it in Minecraft.

## 0.1.0-alpha.20

- Replaced the UV-painted selector face with layered navy, ivory, and bronze
    geometry so the emblem remains circular in Minecraft.
- Removed the coarse exposed gear teeth and restored a smooth metal bezel.
- Added complete Minecraft material mappings for the revised Blender export.

## 0.1.0-alpha.19

- Reduced the selector dial's diameter and depth so it sits naturally in the
    monitor corner instead of dominating the display.
- Replaced the bright starburst face with a restrained, higher-resolution
    concentric teal, ivory, and bronze selector texture.
- Regenerated the production OBJ from the revised Blender scene.

## 0.1.0-alpha.18

- Assigned the existing reference-matched Destiny star medallion texture to
    the circular dial face.
- The dial now carries the gold pointed star, pale inset facets, dark rings,
    and center hub visible in the prop references.
- Preserved all alpha.17 geometry, controls, collision, and dimensions.

## 0.1.0-alpha.17

- Removed all nine flat bronze tabs protruding from the curved upper front rail.
- Restored the floor-socket comb pieces mistakenly removed in alpha.16.
- Restored the matching floor-base collision reach while preserving full
    facing-aware collision across both console wings.

## 0.1.0-alpha.16

- Removed all eleven flat slats from the front of the floor socket.
- Replaced the comb-like base front with one clean continuous edge.
- Trimmed the low base collision to the revised edge while preserving
    alpha.15's full left/right deck collision.

## 0.1.0-alpha.15

- Replaced the center-block-only collision cube with a composite shape covering
    the full left and right console wings.
- Added separate collision volumes for the floor base, both supports, upper
    deck, and raised display while leaving the open leg area passable.
- Rotated the collision shape for all four horizontal facings to match the
    rendered blockstate model.

## 0.1.0-alpha.14

- Removed the outermost left-wing round control.
- Shifted the remaining two matching round controls right as a pair.
- Preserved their metal collars, ivory caps, and all other alpha.13 geometry.

## 0.1.0-alpha.13

- Rebuilt the three round controls on the left wing with matching dimensions.
- Added dark metal collars beneath smaller ivory press surfaces.
- Aligned the controls diagonally between the ivory grid and rear keypad,
  removing the previous crowding and inconsistent visual weight.

## 0.1.0-alpha.12

- Shifted the black left keypad assembly right toward the display cable.
- Added a bronze socket at the cable endpoint so the cable visibly terminates
    on the keypad panel.
- Preserved all other console geometry, controls, scale, and topology repairs.

## 0.1.0-alpha.11

- Moved the fitted black left keypad panel and all twelve of its controls
    rearward toward the copper cable.
- Preserved alpha.10's control spacing, console geometry, scale, and repaired
    manifold topology.

## 0.1.0-alpha.10

- Realigned the left-wing controls to follow the hooked panel's taper.
- Replaced the rectangular rear keypad bed with a fitted trapezoidal inset.
- Removed six coincident copies of the auxiliary controls caused by accidental
    loop nesting in the Blender generator.
- Preserved alpha.9's manifold, outward-normalized, fully triangulated mesh.

## 0.1.0-alpha.9

- Replaced alpha.8's ineffective reversed-face workaround with actual mesh
    repair before OBJ export.
- Applied all Blender modifiers, capped curve ends, merged coincident vertices,
    recalculated outward normals, and triangulated every exported surface.
- Added a conversion-time failure if any object retains non-manifold edges.
- Preserved alpha.7 dimensions and fully opaque solid-rendered textures.

## 0.1.0-alpha.8

- Made every exported OBJ surface double-sided by adding a reversed-winding
    copy of each face, addressing see-through surfaces caused by backface culling.
- Preserved alpha.7 dimensions, materials, textures, and placement behavior.

## 0.1.0-alpha.7

- Reduced only the Minecraft model's width by another 15 percent after the
    third Farlands test, preserving alpha.6 height and depth.
- Changed the OBJ from cutout to solid rendering so partially transparent
    texture pixels no longer create see-through holes in opaque console parts.

## 0.1.0-alpha.6

- Reduced only the Minecraft model's width by 20 percent after the second
    Farlands test, preserving alpha.5 height and depth.

## 0.1.0-alpha.5

- Reduced only the Minecraft model's vertical scale by 28 percent after the
    first Farlands in-game test, preserving its accepted width and depth.

## 0.1.0-alpha.4

- Replaced the earlier Blockbench approximation with the accepted Blender v3
    Destiny console model.
- Added Forge OBJ loading with texture-backed materials and generated UVs.
- Scaled and centered the console to approximately 3.6 by 2.6 by 1.3 blocks.
- Added horizontal facing so the console points away from the player when placed.
- Added a repeatable Blender-to-Minecraft conversion tool.

## 0.1.0-alpha.2

- Rebuilt the physical console model around the screen-used prop proportions.
- Reduced and recessed the inactive display into a layered green monitor housing.
- Added a clearer circular upper-left dial, organized control islands, keyboard tray,
  wrapped perimeter bumper, side conduits, and detailed twin-pedestal base.
- Improved held-item scaling so the console no longer fills the player's view.
- Renamed the displayed block to Destiny DHD so creative search for `dhd` finds it.

## 0.1.0-alpha.1

- Added the placeable Destiny Dialing Console.
- Added the first-pass physical console model and Destiny-style textures.
- Added the console item to the Functional Blocks creative-mode tab.
- Added self-dropping block loot behavior.
- No dialing, GUI, power, crystal, or screen functionality is included yet.
