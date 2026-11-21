# Changelog

## 1.0.0-beta.1-watchdog-test.1

- Request Eden shuttle landing chunks with temporary tickets and wait across
  server ticks instead of synchronously loading the footprint. A timed-out
  search reports the problem and preserves its current candidate for retry.

## 0.0.3-alpha-hotfix1-neoforge-dev6

- Fix the DHD dial-delay mixin callbacks for SGJourney 0.6.55's
  return-valued dialing methods.

## 0.0.3-alpha-hotfix1-neoforge-dev5

- Consume SGJourney 0.6.55's native Universe DHD adapter through
  `DHDControlledStargate` for dialer state, route seeding, bearing effects, and
  Kino connection handling.
- Retain the Universe DHD block-type registration needed by the custom console.

## 0.0.3-alpha-hotfix1

- Locate SGJourney's existing Destiny Stargate before installing or resetting the bundled ship. Align the ship to that gate's position and rotation, preserving its blocks and address. This supports both the original ATM9 Stargates layout and the 90-degree-rotated ATM9 No Frills layout.
- Apply the same alignment to the ship reset volume, protected blocks, elevator, landing doors, curved stairs, airlocks, and ship-bound environmental effects.
- Keep the hotfix1 dependency corrections, handheld layout adjustment, and Destiny snow protection.
- Rotate the invisible corridor collision pieces with the ship and repair their facing in already-installed corridors, clearing the stray side collisions in ATM9 No Frills.

## Unreleased

- Match the Forge Destiny handheld route: select an idle handheld destination
  once to start dialing, normalize the point of origin, and map logical
  Universe symbols to valid physical ring inputs without changing saved routes.
- Locate the nearest registered Universe gate in the current dimension regardless
  of distance, and hold its chunk loaded while a remote dial sequence completes.
- Auto-close handheld-started wormholes after a traveler has crossed and the
  native idle timeout elapses, while retaining Kino hold-open behavior.
- Audit physical gate records against their exact controller position and
  address owner. Remove only confirmed stale records; hold on unresolved owners,
  conflicting live gates, or mismatched saved primary addresses.
- Relocate unambiguous buried gates while retaining their nine-chevron identity
  and existing seven-chevron region mappings. Survey Jungle's broad clearing
  before placement and preserve the existing Water Planet landing circle.
- Grade only around a primary Water Planet gate inside the landing clearing,
  at most one chunk per tick. Avoid loading neighboring chunks for frozen waterfalls.
- Survey fully generated chunk surfaces when placing a missing planet gate;
  reject void-height sites and use the highest sampled terrain for support.
- Place new Water Planet gates at the center of the original landing circle
  after loading the completed terrain. Keep the existing clearing, mountain
  transition, and mountain terrain generation unchanged.
- Let SGJourney engage a programmed Destiny dial after the final buffered
  symbol locks, rather than requesting engagement while the gate is rotating.
- Preserve rapid symbol and engage inputs during the Destiny console's
  pre-dial delay, in the order received.
- Forward SGJourney DHD auto-close queries to the block entity so Kino
  connections can retain their hold-open behavior.
- Stop a programmed DHD dial when SGJourney rejects a symbol.
- Keep the Kino handheld's movement controls and video view inside the visible screen at smaller GUI sizes. Button hit areas now follow the displayed layout.
- Keep Destiny's ship free of snowfall even when another mod overrides snow-placement checks. Clear exposed snow layers already accumulated on the ship as its chunks load.

## 0.0.3-alpha

Changes since 0.0.2-alpha-hotfix2:

### Original Destiny build credit

- The bundled Destiny ship is based on the original **Stargate Universe - The Destiny MC 1.12.2** map created by **Rchanroux**. The ship has since been ported, expanded, rebuilt, and integrated with this mod, but the original structure and creative foundation belong to Rchanroux. Please visit and support the original project: https://www.planetminecraft.com/project/stargate-universe-the-destiny-mc-1-12-2/

### Destiny ship and construction

- Add an optional bundled Destiny ship and refresh it from the October 17 ATM9 Stargates world. The captured block states and inventories match the source at 221,006 non-air positions, including mixed weathered and oxidized floor and ceiling patterns. The original Stargate and parked shuttle are excluded.
- Add a batched ship reset that removes obsolete blocks across the old ship volume before restoring the bundled copy, while preserving the Stargate, its support, and the command player's standing block.
- Add placeable four-bay Destiny corridors, three-by-three sliding airlocks, neighboring door control panels, curved gate-room stairs, safety railings, and a decorative Destiny DHD.
- Add weathered and oxidized always-lit floor lamps, straight, diagonal, left and right 45-degree turns, 90-degree corners, and small corner fillers. Add matching diagonal wall fillers and ceiling-mounted redstone wire, with placement orientations, inventory models, and crafting or stonecutting recipes.

### Life support and equipment

- Make Destiny's exterior unbreathable while enclosed rooms inside the ship supply oxygen. Survival occupants produce shared CO2; high buildup causes nausea, weakness, and eventually damage.
- Add individually serviceable Air Scrubbers that consume water and calcite to remove CO2. The control interface reports the loaded scrubber count and can reset dry units after refilling.
- Add three-by-three-by-three water holding tanks. Face-adjacent tanks share their water and capacity; the bundled ship has three connected tanks containing 52 buckets. Tanks remain manual storage and do not automatically feed scrubbers.
- Add a control interface with live water and CO2 readings and fixed 27% Power Reserves. Add always-full oxygen refill tanks to the suit room.
- Give the Destiny environmental suit built-in gloves. Incomplete suits show an unsealed warning, leak carried oxygen faster outside breathable rooms, and do not prevent exposure. The bundled suit-room chest contains two complete suits and two oxygen tanks.

### Elevator and shuttle

- Replace the rear ship's piston elevator with a smooth eight-stop lift, inside floor-selection panel, outside call buttons, configurable floor names, synchronized cab and landing doors, and emergency release and repair controls. The taller outer doors match the cab doors; boarding doors close automatically, and riders are stabilized during travel.
- Add a placeable Destiny shuttle with a walkable cockpit and cabin, pilot seat, 16 passenger seats, animated rear hatch, hull collision, and docking against a Destiny airlock.
- Add pilot-seat shuttle navigation between Destiny and Eden with a short simulated flight and group arrival. Eden's landing site is selected from natural terrain away from the obelisk.

### Kino, dialing, and stability

- Add the five-block-tall Kino Dispenser with stored Kino and handheld pairs, pickup and deposit controls, and a synchronized inventory display.
- Improve Kino flight collision and step climbing, retain control away from a gate, and keep deployed Kinos active when their owner moves away.
- Repair SGJourney destination registration and saved route indexes so seeded addresses appear and dialing Earth no longer triggers the reported server crash. Keep the existing open-wormhole requirement for cross-dimensional Kino video.
- Reduce render and memory pressure by caching large shuttle and airlock meshes, refining corridor rendering, and cleaning captured armor-stand spawners and surface ore markers from the ship.

## 0.0.3-dev-167

- Refresh the bundled Destiny ship from the October 15 ATM9 Stargates Flatland save. Preserve every captured block type, property, position, floor/ceiling pattern, and block-entity inventory. Include 37 airlocks, 25 corridors, 4 Air Scrubbers, and 3 water tanks with their saved 52 buckets of water. Exclude the original Stargate and parked shuttle.
- Add independent source-to-bundle verification: all 219,945 non-air ship cells and 169 block entities match the source exactly across 70 occupied height layers, including weathered/oxidized finishes and lamp facing.

## 0.0.3-dev-166

- Restore the original polished Naquadah-Copper square border on diagonal lamps, 45-degree turns, and corner fillers in both finishes. Keep the rim aligned to the block edges without drawing over or adding trim to the glowing rectangles. Preserve continuous light patterns and recipes.

## 0.0.3-dev-165

- Match the diagonal lamps, 45-degree transition lamps, and lamp corner fillers to the surrounding smooth Naquadah-Copper flooring. Their top metal surfaces now use the smooth texture instead of the polished texture's square bevel border, in both finishes. Preserve lens placement, recipes, and side textures.

## 0.0.3-dev-164

- Add always-lit weathered and oxidized diagonal lamp corner fillers. A small light patch at one corner completes the rectangular lenses clipped by adjacent diagonal lamps, with four placement orientations, inventory models, and plain sides. Stonecut one matching diagonal lamp into two fillers. Static top geometry uses the existing lens spacing without overlapping faces.

## 0.0.3-dev-163

- Reverse diagonal wall filler placement so builders can face the wall. New fillers copy the facing of a filler immediately below (or above when building downward), including when placed against the backing wall. Existing placed fillers retain their orientation.

## 0.0.3-dev-162

- Add full-height diagonal wall fillers in weathered and oxidized smooth Naquadah-Copper. A flat 45-degree face covers stepped diagonal walls; four placement directions and cached wedge collision shapes keep the empty half accessible. Stonecut one matching block into two fillers.
- Add always-lit left and right 45-degree corner lamps in both finishes to connect straight rows to corner-to-corner diagonal rows. Craft a straight lamp above a diagonal lamp for two left turns, or reverse their order for two right turns. Static models include inventory icons and a single partitioned top surface to prevent overlap flicker.

## 0.0.3-dev-161

- Add always-lit weathered and oxidized diagonal floor lamps. The warm lens row crosses the top corner to opposite corner at 45 degrees; placement facing selects either diagonal. Include inventory models and two-for-two recipes using matching straight lamps.

## 0.0.3-dev-160

- Make `/sgjdestiny ship reset` clear the entire bundled ship capture volume before rebuilding, so blocks deleted from older ship layouts no longer remain. Preserve the original Stargate, its required support block, and the command player's standing block. Clear in bounded batches to avoid a long single-tick stall.

## 0.0.3-dev-159

- Refresh the bundled Destiny ship from the ATM9 Stargates Flatland save: 34 complete airlocks, 25 corridors, 3 Air Scrubbers, 3 connected water tanks, the control hub, door panels, floor lamps, and the updated wall and floor blocks. Keep the parked shuttle out of the capture and preserve the original Stargate assembly.
- Match the packaged Kino Dispenser check to its current position. Keep Destiny's exterior unbreathable while enclosed spaces inside the captured hull supply oxygen; ship occupants still produce CO2 for the Air Scrubbers to remove.

## 0.0.3-dev-148

- Limit Destiny door controllers to airlocks whose 3×3 doorway center is within three blocks of the panel, including status lights and lock controls.

## 0.0.3-dev-147

- Rotate both always-lit Naquadah-Copper lamp top patterns with the player's horizontal facing during normal block placement.

## 0.0.3-dev-146

- Add underside-mounted ceiling redstone wire for horizontal ceiling signal runs; ordinary dust places it on block undersides, and a one-for-one dust recipe also exposes it as an item.
- Add always-lit weathered and oxidized Naquadah-Copper lamps with SGJourney's four-bar top lens and plain polished metal on every other face.

## 0.0.3-dev-145

- Move the far corridor controller socket into the block occupied by its visible panel, migrating placed controllers without changing their visible position or state.
- Let the adjacent helper cell forward clicks on the small overhanging edge of either corridor controller.

## 0.0.3-dev-144

- Align the far-end corridor controller backing with its moved controller socket; preserve the correctly aligned near-end backing.

## 0.0.3-dev-143

- Make all four corridor floor-edge lamp bars uniform rectangles while keeping Stargate Journey's lit Weathered Naquadah-Copper Lamp colors.

## 0.0.3-dev-142

- Replace the corridor's custom amber floor-edge dashes with insets using Stargate Journey's lit Weathered Naquadah-Copper Lamp texture and its four-bar pattern.

## 0.0.3-dev-117

- Resize the supplied Destiny corridor mesh and collision footprint to fit the embedded airlock opening more closely.
- Back open pinholes in the print mesh, improve its nighttime visibility, and stop placing door controllers automatically. A separately placed controller can still operate the corridor door.

## 0.0.3-dev-116

- Place the corridor by clicking the center of its entrance instead of an unseen corner. If its footprint is obstructed, identify the blocking block and position in game.

## 0.0.3-dev-115

- Widen and raise the clear airlock passage inside the corridor's original circular end frame. Resize only the embedded moving leaves to cover the larger opening; the separate ship-door frame remains hidden.
- Preserve and package the corridor's rust, bronze, dark-metal, and warm-light palette with the updated mesh.

## 0.0.3-dev-114

- Rebuild the Destiny corridor from the supplied STL around its actual centerline. Preserve the original four-bay shell and mirrored details, with finer vertex clustering than dev-113.
- Remove the fixed front panel from the original corridor opening and fit the animated Destiny airlock leaves to that opening without adding the airlock's separate frame.
- Extend the corridor render range to match the other large ship structures.

## 0.0.3-dev-113

- Add a placeable, four-bay Destiny corridor from the supplied STL. Mirror its intact side to close the cutaway, remove the display apron, and add a weathered metal color palette.
- Keep the corridor's molded end frame and fit the animated ship airlock leaves inside it without drawing a second frame. Floor, wall, and roof collision follows the corridor; controls on both sides operate the door.
- Add a compact inventory icon and an iron, copper, and airlock crafting recipe. The previous dev-112 jar and source remain frozen separately.

## 0.0.3-dev-112

- Add separate clockwise and counterclockwise curved stair items with distinct iron-and-copper recipes. Each item places only its own flight, and broken sections return the matching item.

## 0.0.3-dev-111

- Scale and center the curved stair and safety railing inventory previews so their icons stay within their slots.

## 0.0.3-dev-110

- Level the narrow stair tread braces and seat them beneath the treads instead of letting their uphill ends protrude through the walking surface.

## 0.0.3-dev-109

- Add placeable Destiny safety railing matching the curved stair rail. Aim at a floor block's edge to place it; additional uses add the other sides in the same space, up to all four. Each edge has a safety collision and drops its own item when broken.
- Give curved stair sections and safety railing separate, simple iron-and-copper crafting recipes, items, and creative inventory entries.

## 0.0.3-dev-108

- Curve both gate-room stair flights toward the illuminated Stargate walkway at their feet, bow them outward through the middle, and extend their top landings into the sides of the upper balcony. Walking collision follows the revised tread paths.

## 0.0.3-dev-107

- Move both curved gate-room stairs to the marked floor and balcony edges. Each stair now rises from the floor near Z −313 and turns inward to meet the side of the center overlook at Z −317.
- Safely migrate either the original Macaw stairs or the earlier native curved stairs in existing Destiny worlds, without resetting the ship.

## 0.0.3-dev-106

- Keep the gate-room stair flights straight at the bottom and concentrate their mirrored inward curves near the upper overlook landings.

## 0.0.3-dev-105

- Replace the two Macaw's Bridges gate-room stairs with mirrored native curved, open-tread metal stairs, using the room's rust-toned Naquadah copper textures.
- Include the new stairs in fresh Destiny ship installs and safely upgrade the ten stair positions in existing worlds without resetting the ship. `/sgjdestiny ship retrofitStairs` is also available for a manual retry.

## 0.0.3-dev-104

- Simplify the decorative Destiny DHD recipe to six iron ingots and two copper ingots; no glass or redstone is needed.

## 0.0.3-dev-103

- Add a decorative Destiny DHD with the same model and collision shape as the working console, but no gate link or dialing behavior. Craft it with common iron, copper, redstone, and a black glass pane.

## 0.0.3-dev-102

- Remove GregTech surface-rock ore indicators from loaded Destiny ship chunks, including existing indicators in saved worlds and those placed during new chunk generation. Other dimensions and ship blocks are unaffected.

## 0.0.3-dev-101

- Extend the shuttle's block-entity rendering range from 128 to 512 blocks. The model remains frustum culled when off screen; actual visibility still depends on the client's chunk draw distance.

## 0.0.3-dev-100

- Prepare Eden's natural shuttle berth in the background when the world starts, so the first launch can use a saved landing site.
- Fully generate distant terrain before judging its height and scan neighboring positions in each area. Keep the berth 2,500–5,000 blocks from the obelisk without clearing trees or reshaping terrain.
- Wrap shuttle navigation status text inside the holographic screen.

## 0.0.3-dev-99

- Open shuttle navigation only by right-clicking the forward helm computer while seated in the pilot chair; sitting no longer opens the screen.

## 0.0.3-dev-98

- Add pilot-seat shuttle navigation between Destiny and Eden with a short simulated flight and group arrival.
- Find and save a natural Eden landing site 2,500–5,000 blocks from the obelisk without clearing or reshaping terrain.

## 0.0.3-dev-97

- Lowers the shuttle passenger mount point to the modeled cushion height so riders sit in the seats instead of hovering above them. The pilot seat, shuttle model, and paint are unchanged.

## 0.0.3-dev-96

- Closes the inner wall collision gap immediately to the right of the shuttle's rear airlock, without changing the doorway or shuttle appearance.

## 0.0.3-dev-95

- Extends the closed shuttle airlock's collision to its visible inner face and across the full door width in all four orientations. The open passage, model, and paint are unchanged.

## 0.0.3-dev-94

- Extends the right passenger-cabin wall collision through the two blocks beside the rear airlock. The previously fixed seat-side collision, shuttle model, and paint are unchanged.

## 0.0.3-dev-93

- Aligns collision with both visible passenger-cabin inner walls behind and between the seats, closing the remaining walk-through strips while retaining the floor and seat interactions. The shuttle model and paint are unchanged.

## 0.0.3-dev-92

- Closes the collision gap along the right wall of the shuttle passenger cabin without changing the model or paint.
- Makes all 16 passenger seats clickable on their cushions and backrests, with dismounts onto the cabin aisle. Existing placed shuttles gain the collision and seats when loaded.

## 0.0.3-dev-91

- Removes the dark liner triangles that crossed the white upper surround of the shuttle's center cockpit window when viewed from inside the cabin. The outer hull, its paint, and the glass are unchanged.

## 0.0.3-dev-90

- Replaces the detached cockpit gap covers with a continuous, curved graphite inner wall. Its window bezels and perimeter returns close the openings between the front windows and side lining while retaining the shuttle's existing exterior finish, glass, and outer window frames.

## 0.0.3-dev-89

- Fills exposed hull slivers around the shuttle cockpit's inner window wall with inward-facing graphite lining. Existing exterior geometry, paint, glass, and window seals are unchanged.

## 0.0.3-dev-88

- Extends the pilot seat's click target over the modeled cushion in all four shuttle orientations, so clicking the seat itself mounts the chair.
- Places a dismounting player's feet on top of the cockpit deck collision instead of inside it.

## 0.0.3-dev-87

- Gives Kino flight a 0.6-block collision box around its visible model instead of using the nearly two-block-tall armor-stand carrier. Movement still clips against Minecraft block and entity shapes, but the Kino can now rise over slabs and dip into a clear one-block tunnel without being pulled back to the carrier's old hover height.

## 0.0.3-dev-86

- Lets the flying Kino climb slab and one-block rises when its horizontal movement meets an obstacle and there is a clear path above it. The climb checks both the upward and forward space, retaining solid-block collision for floors, walls, and ceilings.

## 0.0.3-dev-85

- Restores block collision on deployed Kinos, including ones loaded from existing saves. Minecraft sets a no-gravity armor stand's `noPhysics` flag when it reloads; the Kino now clears that flag before every controlled move while remaining gravity-free, so manual flight, follow mode, floors, ceilings, and walls use Minecraft's normal block collision.

## 0.0.3-dev-84

- Keeps Kino flight, follow, and hover correction inside Minecraft block boundaries. Forward movement now checks for ground below the Kino instead of mistaking a hallway ceiling for the floor, and the height-reset control descends in collision-checked steps.

## 0.0.3-dev-83

- Opens Kino controls when the handheld is used in a dimension without a Universe gate. Within a gate-bearing dimension, the handheld can dial and disconnect the nearest registered Universe gate from any distance. Kino deployment and flight have no gate-proximity limit, and deployed Kinos keep their own moving chunk loaded when their owner travels away.

## 0.0.3-dev-82

- Adds dark cabin-facing lining around the shuttle's front cockpit windows to cover the exposed pale outer hull visible from the pilot seat. Keeps the existing outer hull geometry, paint, window glass, and seals unchanged.

## 0.0.3-dev-81

- Removes the two redundant outer cockpit nose skins that created a second, uneven frame around the front side windows. Trims only the inner-wall triangles visible from outside, retaining the dark interior lining and the narrow black window seals.

## 0.0.3-dev-80

- Restores the two aft cockpit windows by removing the hull sheets that covered their openings and fitting tinted glass to the original aperture edges.
- Adds narrow black seals along both outer front cockpit panes and gives the round nose fasteners a consistent dark backing and finish.

## 0.0.3-dev-79

- Recesses the forward cockpit side wall and its matching rib behind the shuttle hull on both sides, removing the black triangular bleed beside each round exterior fitting without changing the hull paint or fittings.

## 0.0.3-dev-78

- Recesses the narrow cockpit liner that flickered through the hull beside the aft oval recess. Covers the jagged exterior strut ends on both sides with tapered weathered-metal fairings and slim replacement rods, and blends three pale fragments into the dark oval recess on the left.

## 0.0.3-dev-77

- Seals the two small gaps in the shuttle's outer hull beside the aft cockpit windows and recesses the inner window surrounds and shoulder lining that showed through them. Preserves the exterior hull texture, window seals, and existing paint assignments.

## 0.0.3-dev-76

- Closes the open floor at the very front of the shuttle cockpit with a curved dark deck section and matching lower nose lining. Extends floor collision into the new section while keeping it within the hull.

## 0.0.3-dev-75

- Restores worn gray and bronze tones to the shuttle's pipes, couplings, fasteners, engine cases, and weapon barrels after dev73 rendered several paint regions nearly black in game.
- Corrects the cockpit window wall layers so the visible outer returns use the weathered hull finish while the cabin-facing wall and narrow window seals remain dark. Covers the black patches beside the exterior glass without widening the seals.

## 0.0.3-dev-74

- Fixes the missing shuttle model by defining the machinery material used by the dev73 exterior paint restoration. Validates shuttle material names and texture references during every build.

## 0.0.3-dev-73

- Covers the jagged exposed skin above the cockpit windows with a smooth, dark interior brow joining the window wall to the raised headliner. Gives the exposed outer sides and bottoms of the window returns the weathered hull finish while retaining the narrow black seals and dark interior wall.
- Restores the approved Blender exterior paint face for face, including dark engine casings and throats, oxidized engine bodies, black weapon barrels, fasteners, vents, pipe fittings, and capsule details. The weathered hull and wing oxide textures remain. Glass, cockpit floor, and existing exterior geometry are unchanged.

## 0.0.3-dev-72

- Finishes the front cockpit's separate interior wall in dark graphite and brings it almost flush with the three black window seals. The cavity behind it remains hidden; the exterior shell, glazing, and seal shapes are unchanged.

## 0.0.3-dev-71

- Keeps the shuttle rendered whenever the player is nearby, including the front cockpit when facing away from the rear hatch. Distance culling resumes outside the shuttle's immediate area.

## 0.0.3-dev-70

- Adds thin collision along the visible cabin and cockpit decks, including the aisle, while keeping the pilot chair's interaction space clear. Existing placed shuttles receive the corrected floor when loaded.
- Raises the cockpit headliner and its lights to meet the cabin ceiling. Extends the adjacent interior supports and side lining without moving the exterior hull or windows.

## 0.0.3-dev-69

- Recognizes the extended Destiny airlock tunnel up to three blocks behind the shuttle hatch. The docked rear rim may meet the fixed solid tunnel frame, while the ship door, wall blocks, and unrelated placement checks remain intact.

## 0.0.3-dev-68

- Lets a shuttle dock against a matching Destiny ship airlock even when its rear rim meets the fixed wall surround. The wall blocks and their collision stay in place; other placement obstructions are still rejected.

## 0.0.3-dev-67

- Refreshes the optional Destiny ship from the updated Farlands Test world. Preserves the original Stargate, dialing console, and stocked Kino dispenser while excluding any parked shuttle blocks.
- Completes 42 missing airlock sections in the packaged copy, restoring all 14 captured ship doors without changing the source world.
- The Farlands capture contains no Ad Astra airlocks, temporary Ad Astra lamps, or mob spawners.

## 0.0.3-dev-66

- Reduces the door controller's overall face to 84% of its former size and its front-to-back depth to 42%, keeping the rear flush against the wall. The clickable outline now follows the slimmer model.

## 0.0.3-dev-65

- Shrinks the door controller's domed copper button and surrounding socket, and lowers both status lights so the entire indicator channel sits clear of the arched grille.

## 0.0.3-dev-64

- Rebuilds the wall controller with a curved arched housing, recessed grille, crescent indicators, and a domed copper button. The inventory icon now matches the controller.
- The controller button opens the ship airlock on one press and closes it on the next. The door stays in that state until another controller press. Clicking its leaves, applying redstone, and old automatic-close ticks no longer operate it.

## 0.0.3-dev-63

- Extends the bounded ship reset cleanup through the old entrance corridor. This removes obsolete copper stairs, trim, and the remaining wooden door that sit outside SGJourney's smaller default gate-room template. Keeps the existing dialing console and Stargate support intact.
- A requested reset is consumed after one attempt. If it fails, it stops and requires another explicit reset command rather than repeating at every launch.

## 0.0.3-dev-62

- Keeps the support under SGJourney's front gate chevron intact while clearing the old room. A reset also restores that exact chevron if dev-61 displaced it and the other 25 gate blocks and controller remain intact.

## 0.0.3-dev-61

- Clears SGJourney's exact original gate-room footprint one chunk at a time immediately before placing the corresponding ship tiles. The 26 live Stargate blocks are preserved, while obsolete stairs, wooden doors, and other original-room blocks can no longer remain inside the replacement ship.

## 0.0.3-dev-60

- Keeps ship and shuttle airlock meshes cached per facing instead of repeatedly rebuilding and uploading their large models as doors with different states enter view. This removes one source of GPU and native-memory churn in the replacement ship.
- Includes the dev-59 removal of packaged armor-stand spawners and one-time cleanup for already installed ships.

## 0.0.3-dev-59

- Replaces 662 captured armor-stand spawner blocks with air in the packaged Destiny ship. Already installed ships clean only those exact spawner positions once, without resetting the ship.
- Leaves the separate shuttle out of the ship deployment, as before.

## 0.0.3-dev-58

- Replaces the two remaining Ad Astra ship airlocks with the native Destiny airlocks and removes seven temporary Ad Astra lamps from the packaged ship.
- Removes Ad Astra as a required dependency. Existing Ad Astra airlocks can still be upgraded in older worlds with the retrofit command.
- Adds a wall-mounted Destiny door controller with a pushable copper button, white open indicator, red hard-lock indicator, and a simple copper crafting recipe.

## 0.0.3-dev-57

- Declares all packaged Destiny ship block mods as required Forge dependencies so Minecraft shows its normal missing-mods screen before opening a world.

## 0.0.3-dev-56

- Refreshes the optional Destiny ship deployment from the current Flatland Destiny dimension, including updated interior blocks and ship airlocks.
- Preserves the original 26 Stargate blocks and excludes parked test shuttles from the packaged structure.

## 0.0.3-dev-55

- Packages the approved curved grey cockpit nose wall and lower liner so the interior meets the three front windows.
- Keeps the narrow black window seals, reshapes the side bridges to meet the new wall, and preserves the original exterior hull and window glass.

## 0.0.3-dev-54

- Restores the original textured grey cockpit nose interior and keeps only the narrow black seals at the three front windows. Removes the overlapping added nose skins that made the window surround look smeared.
- Tucks the cockpit side bridges inside the exterior hull so they no longer poke through either side. The original hull and window geometry remain intact.

## 0.0.3-dev-53

- Rebuilds the cockpit's forward interior wall against the original three panes, removes the broad raised returns, and keeps the slim black seals. The exterior hull and glass geometry are unchanged.
- Replaces the shuttle's full-cube hull collision with quarter-block shapes sampled from the visible model, clears the rear entrance, and updates collision on already placed shuttles.
- Allows placement over low surface rocks on otherwise clear ground while still rejecting walls and other substantial obstructions.

## 0.0.3-dev-52

- Keeps the cockpit's original three front panes and exterior hull while joining the interior nose wall to the ceiling.
- Finishes the window returns in the nose-wall color and preserves slim black seals along the glass edges, without the raised cockpit collars.

## 0.0.3-dev-51

- Removes the raised interior cockpit window collars and the leftover side-window sill fragments.
- Extends the dark nosecone interior wall to the ceiling and fits flat black seals around the original three front panes. The exterior hull, glass, and pilot seat geometry are unchanged.

## 0.0.3-dev-50

- Replaces the oversized cockpit window seals from dev49 with narrow gaskets that preserve the original glass area.
- Removes the two detached interior strips above the front side windows; the exterior hull and window openings are unchanged.

## 0.0.3-dev-49

- Fits dark interior seals to the cockpit's three front windows and bridges the exposed gaps between their frames.
- Replaces the shuttle item's iron-door placeholder with an icon rendered from the shuttle model.
- Allows the lower collision shell to rest against solid ground and reuses orphaned invisible collision cells from older shuttle tests. Blocked placement now names the obstructing block and position.

## 0.0.3-dev-48

- Adds collision and a right-click target to the shuttle's modeled pilot chair. Players can sit in it and dismount into the cockpit aisle.
- Backfills the pilot seat in already placed shuttles and removes it with the shuttle.

## 0.0.3-dev-47

- Keeps the shuttle visible while a player is anywhere inside its full hull bounds, including the front cockpit when looking away from the rear hatch.

## 0.0.3-dev-46

- Packages the v33 shuttle cockpit review into the in-game model: fitted front-window surrounds, sealed front and side interior walls, the finished pilot station, and individual passenger seats.
- Opens the second rear-facing cockpit window while preserving the previously approved hull textures, front glass, and moving rear hatch.

## 0.0.3-dev-45

- Makes the handheld Back/Home button close the dialer from the address page, while keeping its video-to-Kino and Kino-to-address navigation.

## 0.0.3-dev-44

- Silences the Universe DHD button and Enter sounds while a stored address is submitted from the seed-ship database, leaving the gate's Universe dialing sounds and manual DHD sounds intact.

## 0.0.3-dev-43

- Restores the last Universe address glyphs in the seed-ship database with Stargate Journey 0.6.50 while keeping the 0.6.49 texture names compatible.

## 0.0.3-dev-42

- Updates Stargate Journey compatibility to 0.6.49 or newer and targets the current 0.6.50 release for builds.
- Opens SGJourney's Universe DHD engraving menu when a diamond-tier graver is used on the Destiny console or its screen, while normal clicks keep the custom dialer.

## 0.0.3-dev-41

- Finishes the dark-metal color across both molded side-pipe runs and their curved ends on both sides of the shuttle.
- Removes the broad, misplaced dark color around the small upper cable clusters; those parts remain in hull metal until their individual faces can be separated cleanly.
- Keeps the repaired shuttle door and rear frame geometry from dev-40.

## 0.0.3-dev-40

- Removes the rear surround's floor face where it coincided with the cabin deck, eliminating the striped overlapping surfaces at the threshold.
- Adds a recessed metal backing behind small triangular gaps between the rear surround and the shuttle's original molded hull, without removing the original rear wall.

## 0.0.3-dev-39

- Replaces the unfinished printed wall around the shuttle's rear opening with a closed, two-sided metal surround instead of leaving a hole when the old wall is removed.
- Raises the black and gold hatch seal to meet the taller cabin, while preserving the floor and moving door leaves.
- Removes the overlapping rear liner and gives the hull light weathered metal with muted copper-red wing panels, based on the supplied shuttle references.
- Gives the paired molded side hoses and raised upper cable clusters their own dark metal material so they read separately from the body.

## 0.0.3-dev-38

- Maps the printed shuttle hull continuously across adjacent triangles, removing the per-triangle rust-texture seams seen throughout the exterior.
- Disables per-face directional shading on the shuttle model so the decimated print's triangle boundaries do not create alternating bright and dark patches.
- Caps the printed hull's two exposed lower hatch lips with slim metal returns that recede toward the side walls, outside the moving doors and clear floor.

## 0.0.3-dev-37

- Restores the shuttle's warm rusted-metal exterior and smooths the printed hull's per-triangle lighting without removing its molded details.
- Removes the 386 coincident reverse faces added in dev-36, which could expose a triangular patch on the rear hull.
- Raises the passenger-cabin walls, arches, ceiling, and ceiling fixtures by up to 0.46 blocks while keeping the deck and seating at their original height.
- Closes the exposed join above the rear hatch with a matching metal ceiling return.

## 0.0.3-dev-36

- Restores the cabin-facing surfaces on the shuttle's printed lower right rear-frame brace. The added faces follow the existing hull and reuse its texture, leaving the gold trim, door pocket, and walkway unchanged.
- Checks the rear frame in a preview with back-face culling enabled so missing interior-facing surfaces are visible during review.

## 0.0.3-dev-35

- Finishes the cabin-facing lower returns of the shuttle rear door frame on both sides, ending them flush with the deck instead of in pointed tips.
- Removes the liner strip beneath the doorway so the floor remains a clear, level threshold.

## 0.0.3-dev-34

- Closes the exposed lower ends of the shuttle hatch's black and gold trim while leaving the threshold open.
- Removes leftover print facets and shelf pieces at the lower hatch corners, and backs the small resulting voids with matching shuttle metal.
- Smooths the rear hatch liner's corner shading so its sides meet the sill without harsh triangular bands.

## 0.0.3-dev-33

- Rebuilds the shuttle rear hatch liner as a closed transition between the larger exterior opening and the original, smaller cabin-side gold and black frame.
- Removes the oversized cabin-side backing that projected past the hatch trim and closes the triangular gaps around the frame.

## 0.0.3-dev-32

- Gives the shuttle and standalone door-frame openings a narrow, even bevel while preserving their clear doorway profiles.
- Maps the exposed jambs and lintels along their actual surfaces, removing stretched/banded metal texture and keeping every UV inside its texture-atlas tile.

## 0.0.3-dev-31

- Fixes invalid UV coordinates on the shuttle's rear frame liner that sampled unrelated blue and green texture-atlas tiles in-game.
- Slims the original dark octagonal interior hatch trim while preserving its doorway and gold inner rim.
- Extends the weathered-metal interior backing symmetrically into the cabin wall to cover the untextured right-side and upper seams, without changing the accepted exterior face or door leaves.

## 0.0.3-dev-30

- Clears false full-block collision obstacles from the shuttle cabin aisle, including those left in already placed shuttles.
- Keeps the shuttle visible while a player is inside looking toward its nose, without disabling exterior frustum culling globally.
- Extends the rear airlock frame's interior return past the decorative rim to cover exposed world-color seams, and removes 18 print fragments projecting through the threshold.
- Groups the rear liner OBJ data in standard vertex/UV/normal/face order for reliable game loading.

## 0.0.3-dev-29

- Maps the shuttle's original rear bulkhead to the same weathered metal as the exterior instead of its pale gray palette cell.
- Seats the rear hatch liner against that bulkhead to close the colored gaps around the frame.
- Gives only the shuttle hatch's two gray metal tones warm hull-matched colors, preserving its black panels, dark outlines, and standalone Destiny interior door finish.
- Keeps the shuttle door and renderer geometry reductions from dev-28.

## 0.0.3-dev-28

- Adds a recessed metal lining behind the shuttle's original rear bulkhead and around the hatch cut, closing exposed frame gaps while leaving the doorway open.
- Removes source-print triangles buried inside the solid shuttle hatch leaves, reducing their geometry from about 70,500 to 43,000 triangles. Draws the closed hatch before the much larger static shuttle mesh and culls the directional hull bounds when out of view.
- Hides selection outlines on the hull's geometry-derived collision cells and gives them an empty model resource to avoid the missing-model warning. Physical collision remains in place.

## 0.0.3-dev-27

- Stops drawing the standalone Destiny interior airlock when its actual frame and sliding-door bounds are outside the camera view, and shortens its render range from 192 to 96 blocks.
- Omits source-print triangles fully buried inside the solid door leaves, reducing each interior door from about 85,800 to 53,000 rendered triangles while keeping both visible faces and the shuttle hatch unchanged.

## 0.0.3-dev-26

- Confines the X braces to the dark upper and lower panels on both faces of the standalone Destiny airlock. The diagonals still align toward the geared lock, while their ends meet the panel borders without crossing the lighter door surface.

## 0.0.3-dev-25

- Aligns the four braces on both faces of the standalone Destiny airlock as one uniform X centered on the geared lower lock.
- Extends the lower braces through the dark panel borders into the solid leaves and reverses the upper diagonals to match the reference.

## 0.0.3-dev-24

- Thickens both standalone Destiny airlock leaves so the solid metal core reaches the front and rear detail skins, removing the visible air gap.
- Seats the inset panel rims, fields, and diagonal braces against the thicker core while retaining raised lock hardware and the curved center seam. The shuttle hatch remains unchanged.

## 0.0.3-dev-23

- Seats the upper plain lock and curved seam border close to the Destiny interior door face instead of leaving them standing proud.
- Shortens and slims the upper and lower diagonal braces so their ends remain inside their recessed panels on both door faces.

## 0.0.3-dev-22

- Cuts the original raised door detail to the moving curved seam, exposing the upper half-circle clearance in the left leaf instead of leaving stray metal behind the right-hand lock.
- Brings the upper plain lock and matching cutout closer to the lower geared lock on both faces of the standalone interior door.

## 0.0.3-dev-21

- Raises the start of the interior airlock frame's gentle lower taper and seats the moving leaves to the new outline.
- Moves the upper plain lock closer to the geared lower lock. The upper socket stays exclusively on the right leaf and slides with it; the lower lock remains on the left.

## 0.0.3-dev-20

- Gives the standalone Destiny interior doors a second plain circular lock on the opposite leaf and diagonal metal braces in the upper and lower panels, matching the supplied on-screen references. The shuttle hatch keeps its single rotating lock.
- Shapes the lower door jambs into the slight inward bend shown in the reference and keeps the moving leaves aligned to the opening.

## 0.0.3-dev-19

- Replaces the shuttle's rough invisible collision outline with cells sampled directly from the exported hull, cockpit glass, and rear bulkhead. The cabin and operating rear hatch remain open.
- Refreshes the matching shuttle and Destiny airlock doors with six recessed panels on both faces while retaining the moving halves, visible curved seam, and hollow frame pockets.

## 0.0.3-dev-18

- Gives the Destiny ship and shuttle airlock leaves solid metal cores, matching rear faces, and a visible curved center seam around the original lock.
- Adds hollow side sleeves to the ship door frame so the leaves disappear into the frame when opened, and replaces the cloudy airlock finish with restrained brushed, weathered metal.
- Ports the same finished two-piece door and rotating lock to the shuttle at its original one-unit-to-one-block scale.
- Expands the shuttle exterior collision shell to match the fuselage and wings and restores missing collision blocks on previously placed shuttles when their anchor loads.

## 0.0.3-dev-17

- Seats the original Destiny airlock detail close to both door surfaces, removing the protruding circular locks while preserving their design and opening behavior.

## 0.0.3-dev-16

- Centers the airlock star/gear on its measured hub and derives its animation pivot from the baked model for every door facing.
- Mirrors the original raised hatch detail and console lock emblem onto the back of both door leaves, so the doorway has the same design from either side.

## 0.0.3-dev-15

- Levels the console lock badge against the Destiny airlock's circular socket and spins its starburst one full turn before opening, then reverses it after closing.
- Adds collision that follows both moving door leaves and makes the inner doorway frame extend straight to the floor.
- Removes stray shuttle geometry from the ported airlock and gives its door and the placeable shuttle exterior a weathered copper metal finish while retaining the approved faded-red wing paint.

## 0.0.3-dev-14

- Seats the Destiny console starburst in the ship airlock lock opening, flush with the surrounding lip rather than floating in front of it.

## 0.0.3-dev-13

- Aligns the shuttle's centerline with the center of the selected Destiny docking block.
- Adds a craftable three-by-three Destiny ship airlock using the approved original shuttle hatch leaves and the Destiny console's circular starburst detail on the lock.
- Replaces the 11 packaged Ad Astra airlocks with the Destiny door while preserving the rest of the ship tiles.
- Adds `/sgjdestiny ship retrofitAirlocks` to upgrade complete airlocks in an existing Destiny ship without resetting the world.

## 0.0.3-dev-12

- Rebuilds the shuttle's rear hatch from the original source mesh with a precise ten-sided opening and a curved center-lock split.
- Moves both original door leaves fully into the side pockets over 34 ticks, leaving the complete doorway clear.
- Aligns the cabin floor and inner wall bottoms to the original hatch sill.
- Preserves the approved aged-metal and faded-red face assignments from the painted Blender baseline in the game export.

## 0.0.3-dev-11

- Restores the complete original rear hatch, including the lower octagonal section removed by the earlier cabin carve.
- Splits all recovered original hatch faces at the center seam so the entire two-piece door moves aside when opened.
- Lowers the cockpit and cabin floor, floor lights, seams, and aisle rails to the bottom of the rear doorway.
- Retains the invisible exterior collision shell and overlap-safe shuttle placement from the previous test build.

## 0.0.3-dev-10

- Replaces the dark cloudy dev-9 finish with a higher-resolution, low-contrast aged-metal texture that lets the modeled panel and mechanical detail remain readable.
- Changes the wing inset finish from saturated maroon to subdued, faded oxidized red-brown with sparse directional abrasion.
- Restores the purchased shuttle mesh's original rear hatch faces, divides them at the original center seam, and slides the two original halves apart when opened.
- Adds an invisible exterior collision shell around the fuselage, wings, and rear fin so players can no longer walk through the shuttle skin.
- Rejects placement when any part of the shuttle collision shell would overlap an existing structure.

## 0.0.3-dev-9

- Expands the shuttle block entity's frustum bounds to cover the complete craft, preventing it from disappearing when the rear placement anchor leaves the camera view.
- Replaces flat triangle paint with projected aged-metal and faded-red texture maps.
- Restricts red to broad, upward-facing recessed wing-panel surfaces so piping, ribs, fasteners, edges, and underside geometry retain their metal finish.

## 0.0.3-dev-8

- Replaces the triangle-by-triangle hull color variation with one consistent cool aged-metal finish, removing the unintended desert-camouflage appearance.
- Uses a consistent muted faded red on the rear wing inset regions while retaining the two-sided hull and GPU mesh cache.

## 0.0.3-dev-7

- Renders the opaque shuttle skin two-sided so reversed triangles in the source mesh no longer make whole hull panels disappear at certain viewing angles.
- Retains the static GPU mesh cache from dev-6 and leaves the stable glass render path unchanged.

## 0.0.3-dev-6

- Uploads each shuttle orientation to a static GPU vertex buffer once instead of rebuilding and resubmitting roughly 100,000 triangles every rendered frame.
- Rebuilds the cached shuttle mesh automatically after a resource/model reload.

## 0.0.3-dev-5

- Fixes the multipart definition so Minecraft always renders both the solid shuttle and its glass instead of randomly choosing one, which caused the invisible hull and flickering windows.
- Removes the temporary rectangular replacement door panels; the rear airlock must use the shuttle model's own geometry.

## 0.0.3-dev-4

- Sends the separated solid hull and translucent windows through block-entity-compatible atlas buffers, restoring the missing hull and stopping the rapid render-buffer flicker.

## 0.0.3-dev-3

- Separates the shuttle's hull and interior from its glass so the outer skin renders as fully opaque solid geometry.
- Keeps only the three cockpit panes on the transparent two-sided render layer.

## 0.0.3-dev-2

- Moves the oversized shuttle OBJ out of Minecraft's chunk mesh path and into a direct block-entity renderer, preventing ATM9's optimized renderer from wrapping and scrambling the model across chunk sections.
- Keeps the complete shuttle visible when its rear placement anchor is outside the camera view.
- Uses only the standard Forge 1.20.1 rendering API so the same shuttle build works in both ATM9 and the smaller Farlands Test instance.

## 0.0.3-dev-1

- Adds the placeable Destiny shuttle using the approved hollow-cabin exterior and three curved glass windows.
- Anchors placement at the rear airlock and adds two sliding door panels with manual and redstone controls.
- Leaves the shuttle-to-Destiny docking tunnel for a later build after the in-game shuttle test.

## 0.0.3-dev

- Corrects packaged Destiny structure NBT so the opt-in installer can place its sections.
- Removes captured snow layers from the packaged ship; an operator can reapply it with `/sgjdestiny ship reset` while preserving the original Stargate.
- Requires Ad Astra 1.15.20+, Simply Light 1.4.6+, GTCEu 7.2.0+, Macaw's Bridges 3.1.0+, and SGJourney Deco 1.2.0+ only when the packaged Destiny ship is enabled; missing ship mods stop world startup before installation.
- Initializes every newly placed Kino Dispenser with two complete Kino kits; each pickup supplies one Kino and one handheld, and each deposit returns a complete pair.
- Adds a disabled-by-default server setting for explicitly opting into installation of the packaged Destiny ship, protecting existing player-built ships.
- Documents the per-world config locations for single-player and dedicated servers; this setting is not in the instance's main `config` folder.
- Keeps the `sgjourney:destiny` dimension permanently clear without changing weather in any other dimension.
- Adds the accepted five-block-tall Ancient Kino Dispenser with dark oxidized and weathered metal materials.
- Adds persistent storage for up to 64 Kinos with one-at-a-time deposit and pickup controls.
- Adds a custom dispenser-style interface with real Kino model previews and a synchronized inventory counter.
- Makes the base block and both blocks above it fully clickable, with automatic restoration of the invisible upper interaction targets.
- Corrects OBJ UV mapping, back-face visibility, compartment sealing, pedestal caps, and in-game material rendering.

## 0.0.2-alpha-hotfix2

- Fixes dedicated ATM9 server startup while retaining Immersive Portals as a required client-and-server dependency.
- Restores the high-quality Immersive Portals Kino video renderer and smooths the feed at the client's rendered frame rate.
- Keeps manual Kino controls and camera placement active after Stargate travel, including travel back through the gate.
- Uses W/A/S/D for horizontal flight, Space and Shift for vertical flight, Left Control for default hover height, and first-person-style mouse pitch.
- Holds an active Stargate connection through SGJourney's native lifecycle while a Kino is scouting, then releases it when the player crosses or the connection is manually closed.
- Requires an open wormhole for cross-dimensional Kino control and video, ends the feed when that route closes, and restores the same link when the destination is redialed.
- Gives each active Kino one persistent handheld binding and allows a replacement to be deployed after abandoning an older Kino. The abandoned unit remains dormant and cannot steal the replacement's link.
- Prevents right-clicking a newly deployed Kino with its handheld from creating a fake handheld entity and fixes missing handheld model faces.
- Repairs imported and generated destination-gate registration, retained addresses, outward dialing, and safer surface placement.
- Prevents malformed legacy SGJourney gate records in large modpacks from blocking the Destiny console and handheld address screens.
- Corrects the nine-chevron bearing so its white lamp flashes on each lock and remains lit only after a successful connection.
- Starts the nine-chevron electrical discharge after the seventh lock, keeps it active through chevrons eight and nine and a successful connection, and stops it immediately on failure or disconnect before the accepted idle rotation and steam sequence.
- Restricts Destiny Bearing illumination to the white lamp element.
- Documents that shader packs are unsupported with the Kino video feed.

## 0.0.2-alpha

- Promotes the accepted 0.0.141 development build to the second public alpha baseline.
- Includes the complete accepted Destiny console, handheld controller, Kino, gate-network, planet, creature, recipe, configuration, and documentation systems.
- Locks Eden with land-dominant mountainous terrain, widely spaced ponds and lakes, shallow hillside caves, a sealed three-layer bedrock floor, passive wildlife, no hostile mobs, and the accepted obelisk behavior.
- Produces the release artifact as `sgjdestiny-tech-1.20.1-0.0.2-alpha.jar`.

## 0.0.141-dev

- Removes Eden's large underground cave and canyon carvers.
- Adds occasional short, shallow cave openings specifically to natural hillsides.
- Retains the normal-pipeline three-layer bedrock seal beneath the cave-free deep terrain.
- Adds widely spaced natural ponds and small lakes, averaging about 90 blocks apart, without introducing a global sea level or deep surface oceans.

## 0.0.140-dev

- Adds Eden's bottom seal as a normal world-generation feature instead of a chunk-load repair.
- Generates three continuous bedrock layers at the bottom of every new Eden chunk without blocking player movement during chunk loading.

## 0.0.139-dev

- Removes the synchronous Eden chunk-load block rewriting that froze players at newly generated terrain.
- Returns bottom sealing to the noise generator so chunks generate through Minecraft's normal pipeline and remain compatible with Distant Horizons.

## 0.0.138-dev

- Seals every loaded Eden chunk with four continuous deep deepslate layers and a complete bedrock layer at the world bottom.
- Repairs previously generated Eden chunks when they load and prevents all terrain or cave density rules from leaving openings into the void.
- Restores Eden's normal cave-density generator above the guaranteed bottom seal.

## 0.0.137-dev

- Adds a guaranteed solid terrain-density foundation from Y=-64 through Y=-48 on Eden.
- Prevents cave carving from punching holes through the bedrock floor while preserving the cave system above the sealed foundation.

## 0.0.136-dev

- Prevents Eden's deep terrain cavities from exposing frightening void-like openings.
- Uses lava as the below-ground fallback fluid while retaining a zero sea level and disabled aquifers, filling deep void-like openings without creating surface oceans.

## 0.0.135-dev

- Prevents Eden from generating global seas by disabling its terrain aquifers, using air as its default fluid, and restoring a zero sea level.
- Keeps Eden's intentional event-built lake and shoreline food as its controlled source of surface water.

## 0.0.134-dev

- Removes the failed fixed-seed mixins and restores the original launch-safe Mixin build configuration.
- Retains Eden's taller obelisk, night-only beacon, nearby lake and food, and no-hostile-mob behavior.
- Defers deterministic generation for finalized planets until it can be implemented without fragile launch-time injections.

## 0.0.133-dev

- Fixes the 0.0.132 client launch failure by removing a redundant fixed-seed injection from an inherited `ServerLevel` method.
- Retains the valid terrain-noise and world-generation-region seed hooks.

## 0.0.132-dev

- Extends Eden's existing obelisk from 36 blocks to 108 blocks as one continuous monument.
- Adds an integrated beacon cap that emits a skyward beam only during Eden's night and switches off at sunrise.
- Adds water-filled lowlands plus a guaranteed lake near the future shuttle landing area and the obelisk.
- Grows harvestable melons and mature sweet berries around the guaranteed lake.
- Preserves Eden's accepted passive animal life while preventing hostile mobs from spawning or entering the dimension.
- Locks Calcite, Water, Jungle, and Justice to the seed used by their accepted development-world terrain so their terrain, features, and structures reproduce on every server.
- Leaves Eden seed-dependent until its design is finished and accepted.

## 0.0.131-dev

- Completes the Eden rename by removing the temporary `sgjourney:eden_planet` registry alias after migrating the test save to `sgjourney:eden`.
- Redirects the saved player from the obsolete ocean-backed Eden dimension into the canonical land-heavy Eden dimension where the obelisk is generated.

## 0.0.130-dev

- Restores a hidden `sgjourney:eden_planet` biome and dimension compatibility alias so saves created before Eden was renamed can still load.
- Keeps all new Eden generation and runtime behavior on the canonical `sgjourney:eden` identifier.

## 0.0.118-dev

- Added Mediora as a native SGJourney galaxy for Destiny's current route.
- Migrates existing custom destinations from their former Kaliem mapping into Mediora.

## 0.0.117-dev

- Fixed dead Squigglers freezing indefinitely during their daylight retreat window.
- Explicitly registers startup-generated Universe gate controllers with SGJourney so the Barren Planet is immediately dialable.

## 0.0.116-dev

- Centralizes every custom Universe destination in one startup preload list.
- Forces and retains each destination's Stargate anchor chunk for the life of the server.
- Detects a destination with no valid Universe gate record and deterministically places the SGJourney Universe pedestal template before routing becomes available.
- Applies the destination-specific pedestal processor and reports the repaired gate count in the server log.

## 0.0.115-dev

- Adds the unnamed Barren Planet visited in SGU's "Justice" as a registered seven-chevron destination.
- Generates a dry gray-brown gravel-and-stone valley without vegetation or natural animals.
- Places a half-buried, non-Ancient alien wreck in a rocky area away from the Universe Stargate.

## Accepted project baseline: 0.0.114-dev

- The user accepted 0.0.114-dev as the new baseline for the entire project before development of the next destination begins.
- Future work must preserve every accepted system in this build unless the user explicitly requests a change.

## 0.0.114-dev

- Removes the textual address-type prefix so destination rows show only the planet name and Universe glyph address.

## 0.0.113-dev

- Corrects Universe address glyph rendering to scale each complete SGJourney 32-by-32 symbol instead of sampling its empty corner.

## 0.0.112-dev

- Makes Squigglers retreat into the nearest nest and despawn as daylight approaches.
- Adds visible sulfur-colored fumes venting from Squiggler nest openings.
- Replaces numeric handheld address strings with SGJourney's Universe glyph textures.
- Keeps Earth's nine-chevron address exclusive to the Destiny ship console and removes it from the handheld network list.
- Records the accepted 0.0.111 Jungle Planet and Squiggler behavior as the completed baseline.

## 0.0.97-dev

- Corrects the Jungle Planet's unique Stargate placement field from the invalid
  `y` coordinate to a deterministic `z` chunk coordinate.
- Moves its anchor to an unexplored chunk and generates that chunk on startup,
  repairing worlds opened with the earlier Jungle Planet builds.

## 0.0.96-dev

- Generates and retains each custom destination's unique Stargate anchor chunk
  before validating its gate records, ensuring a newly added Jungle Planet has
  a registered destination gate before the first dial.

## 0.0.95-dev

- Imports the Jungle Planet address region into SGJourney's saved Universe map
  so it appears in the handheld destination list in both new and existing worlds.
- Includes the Jungle Planet in destination-gate validation during server startup.

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
## 0.0.98-dev
- Rebuild the Jungle Planet gate arrival area as a broad, wooded jungle island instead of an ocean landing.
- Transfer a following Kino with its owner when the player travels between dimensions.
- Detect Kino crossings explicitly so manual and live-video flight can pass through connected Stargates.
## 0.0.99-dev
- Make the entire loaded Jungle Planet continuous land with dense jungle growth and low cliff terrain.
- Move Kino follow-dimension recovery into the regular server tick so it reliably follows its owner through a gate.
## 0.0.100-dev
- Restore the proven smooth Kino movement baseline.
- Let a following Kino physically continue through the event horizon after its owner while holding the connection open for SGJourney's normal wormhole traversal.
## 0.0.101-dev
- Aim a following Kino at the source gate's real event-horizon center after its owner crosses, allowing physical follow-through in either direction.
## 0.0.102-dev
- Generate the Jungle Planet without oceans or aquifers instead of rewriting every loaded chunk at runtime.
- Level the gate's immediate surroundings and grow close jungle undergrowth around it without leaving the gate in a pit.
## 0.0.103-dev
- Widen and fully level the gate landing area, filling low ground and blending gradually into nearby jungle hills.
- Remove passive and vanilla hostile Jungle Planet spawns so only darkness-spawned Squigglers appear naturally.
## 0.0.104-dev
- Remove complete trees and hanging foliage before leveling the Jungle gate clearing, preventing floating chopped trunks and leaf layers.
- Preserve an intact dense jungle perimeter with low undergrowth inside the clearing.

## 0.0.105-dev
- Remove the Jungle Planet gate clearing and terrain-leveling pass so natural jungle terrain and vegetation remain around the gate.

## 0.0.106-dev
- Add sparse hollow Squiggler nest mounds to Jungle Planet generation.
- Spawn controlled Squiggler packs from nearby nests at night, with cooldown and population limits.

## 0.0.109-dev
- Distribute Squiggler nests as common colonies of 2–4 mounds across the Jungle Planet.
- Let Squigglers climb trees and vegetation and drop toward nearby prey from above.
- Add player-kill Squiggler Venom Sac drops with Looting support for quests and future waterborne-microbe cure content.

## 0.0.110-dev
- Add active vertical pursuit so Squigglers climb trunks and pass through canopy leaves to reach players in treetops without damaging vegetation.

## 0.0.111-dev
- Spread individual Squiggler nests broadly across the jungle instead of grouping them into clusters.
- Use wall-climber navigation and forward canopy detection so Squigglers can route up trunks and enter leaves to reach treetop players.
# 0.0.129-dev

- Renames Eden's internal dimension, biome, address-region, and space-location identifier from `eden_planet` to `eden`.

# 0.0.128-dev

- Changes Eden to land-dominant terrain with meadows and sparse woodland instead of an ocean broken by small islands.
- Rebuilds the obelisk as one continuous tapered monolith while preserving its accepted height.
- Anchors the obelisk completion marker underground so repeated server starts cannot stack additional obelisks vertically.

# 0.0.127-dev

- Handles SGJourney's absent saved-galaxy map when introducing Eden to an existing world, preventing a server-start crash.

# 0.0.126-dev

- Adds Eden from SGU's “Faith” as a peaceful, Earth-like Mediora world with forests, meadows, water, and a monumental alien obelisk.
- Keeps Eden outside the Stargate destination list because its travel route will use the upcoming Destiny shuttle system.

# 0.0.125-dev

- Embeds the Justice Planet pedestal three blocks into the selected terrain so its built-in base and arrival ramp meet the ground naturally instead of forming a raised rectangular mound.

# 0.0.124-dev

- Supports the Justice Planet gate pedestal with a shallow gravel-and-stone terrace that follows the selected terrain and tapers into the surrounding barren landscape.
- Keeps gate placement additive so it does not excavate or erase nearby terrain.
- Moves the crashed alien ship roughly 300 blocks from the Justice Planet arrival gate so it must be discovered during exploration.

# 0.0.123-dev

- Surveys nearby Justice Planet terrain before initial gate construction and places the full pedestal on the flattest suitable footprint.

# 0.0.122-dev

- Makes the lowest existing Justice Planet gate authoritative when earlier development builds left stacked duplicates.
- Gives newly generated Justice Planet terrain broader and taller barren hills.

# 0.0.121-dev

- Registers Mediora into existing SGJourney Universe saves before migrating its planets, making their displayed seven-symbol addresses routable.

# 0.0.120-dev

- Made fixed Mediora dialer entries resolve directly from their authoritative datapack definitions when SGJourney's saved reverse index is unavailable.

# 0.0.119-dev

- Restored all seeded Mediora destinations in the Destiny dialer even while the source gate cache is loading.
- Reattached migrated space locations to SGJourney's saved Mediora address-region instances.
- Initialized newly placed destination gates through SGJourney so they receive a valid nine-chevron identity before network registration.
