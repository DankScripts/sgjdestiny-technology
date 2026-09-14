# Accepted Kino Dispenser baseline: 0.0.3-dev

Accepted on 2026-09-14.

Version 0.0.3-dev is the accepted reference point for the Ancient Kino Dispenser. Preserve the following unless the user explicitly requests a change:

- The approved ancient, dark oxidized, weathered-metal appearance and final five-block total height.
- The complete model geometry, including the enclosed black Kino compartment, raised lower bezel, sealed sides and back, attached panels, control box, cable routing, pedestal, and shortened vertical pipes.
- The dispenser's full clickable coverage across its base block and both complete blocks above the base.
- Automatic restoration of missing invisible upper interaction targets.
- The purpose-built interface that resembles the dispenser compartment rather than a normal inventory window.
- The interface's real Kino model previews, Deposit and Pickup controls, and synchronized `Kinos stored: N / 64` counter.
- One-Kino-at-a-time deposits and withdrawals, persistent stored quantity, inventory-space checks, and dropping stored Kinos when the dispenser is broken.
- The Forge OBJ material and UV fixes required for correct in-game rendering, including sealed double-sided geometry and the corrected solid pedestal caps.
- The `dev` version suffix for continued development builds.

This baseline supersedes the earlier Kino Dispenser preview and in-game test iterations. The whole-project 0.0.2-alpha-hotfix2 baseline below remains authoritative for systems unrelated to the Kino Dispenser.

# Accepted whole-project baseline: 0.0.2-alpha-hotfix2

Accepted on 2026-09-13.

Version 0.0.2-alpha-hotfix2 is the current reference point for the entire project. Preserve all working behavior, visuals, assets, recipes, configuration, dimensions, addresses, gate integration, Kino systems, and documentation present in this build unless the user explicitly requests a change. New work must be additive and must not regress an accepted system.

This baseline adds the complete field-tested Hotfix 2 behavior:

- Dedicated ATM9 servers launch with Immersive Portals installed on both client and server.
- The high-quality Immersive Portals Kino feed renders at the client's frame rate, with tick-rate camera updates and smooth movement between updates. Shader packs remain unsupported for Kino video.
- Kino flight uses W/A/S/D, Space, Shift, and Left Control; mouse pitch matches normal first-person movement.
- Manual Kino controls, video, and camera placement survive Stargate travel in both directions.
- A Kino holds SGJourney's wormhole open through its native connection lifecycle until the player crosses or the connection is manually closed.
- Cross-dimensional Kino control and video require an active wormhole. Closing it suspends the link and feed; redialing the same destination restores the same Kino.
- Each active Kino has one persistent handheld binding. A replacement can be deployed after abandoning a Kino, and the dormant older unit cannot reclaim the handheld.
- Right-clicking a newly deployed Kino while holding its handheld cannot create a fake handheld entity.
- Custom destination gates retain valid addresses, register reliably, and can dial outward after generation or import repair.
- Nine-chevron bearing illumination and electrical discharge use the accepted lock, connection, failure, shutdown, and steam timing.

This whole-project baseline also preserves:

- Destiny console seven- and nine-chevron dialing, bearing and floor-chevron lighting, electrical discharge, steam effects, and continuous steam audio.
- The handheld controller design, remote dialing, Kino controls, smooth flight, Stargate traversal, high-quality live video, camera controls, and multiplayer Kino limit.
- Earth’s nine-chevron destination appearing only on Destiny’s ship console and never on the handheld controller.
- Destination rows showing the planet name and SGJourney Universe glyph address without numeric symbols or a textual `U 7C` prefix.
- The accepted Calcite, Water/Ice, and Jungle planet implementations and their registered seven-chevron destinations.
- The complete Jungle Planet and Squiggler behavior recorded below, including scattered individual nests, climbing and leaf pursuit, venom-sac loot, daylight retreat, and sulfur fume venting.
- All existing crafting recipes and required dependency declarations.
- Eden under the canonical `sgjourney:eden` dimension identifier with its accepted land-dominant mountainous terrain and passive animal life.
- Eden's single continuous 108-block obelisk, night-only beacon beam, guaranteed nearby lake and food, and absence of hostile mobs.
- Widely spaced natural ponds and lakes with no global surface ocean generation.
- Short shallow hillside caves without deep cave or canyon carvers, plus a continuous three-layer bedrock seal generated through the normal world-generation pipeline.
- The launch-safe original Mixin configuration; failed fixed-seed injection experiments are excluded from this baseline.

The original 0.0.41-dev Water Planet baseline and later subsystem baselines below remain historical detail within this complete 0.0.114-dev reference.

This baseline preserves all previously accepted Destiny console, dialing,
9-chevron discharge, steam vent, Earth Gate Designator, Calcite Planet, suit,
and oxygen HUD behavior.

The Water Planet is complete and locked at this baseline. Preserve its:

- SGJourney-owned `sgjourney:water_planet` dimension and Universe Stargate.
- Stargate placement anchored at Y=64.
- Pre-generated low landing plain around the gate with a broad natural mountain transition.
- Distant mountains, sparse frozen waterfalls, permanent snowfall, and no mobs.
- Nitrogen-rich unbreathable atmosphere and Destiny suit protection.
- Absence of lava lakes and underwater magma generation.
- Optional Distant Horizons compatibility and startup terrain preparation.

Do not change Water Planet terrain, atmosphere, weather, gate placement, ice,
waterfalls, or landing-zone behavior unless the user explicitly requests it.

## Accepted Jungle Planet generation baseline: 0.0.105-dev

Accepted on 2026-09-09.

Preserve the Jungle Planet generation introduced and accepted in 0.0.105-dev:

- Naturally generated dense jungle surrounding the Universe Stargate.
- No artificial clearing, vegetation removal, or terrain-leveling pass around the gate.
- No oceans; retain the existing jungle terrain profile, cliffs, and shallow caves.
- No passive or ambient animal spawns.
- Squigglers are the planet's only natural hostile spawn and use normal darkness-based monster spawning rules.

Do not change Jungle Planet terrain, vegetation, gate surroundings, biome spawn list,
or Squiggler darkness spawning unless the user explicitly requests it.

## Accepted Squiggler and Jungle Planet baseline: 0.0.111-dev

Accepted on 2026-09-09. The user marked this planet complete.

Preserve the complete Squiggler system accepted in 0.0.108-dev:

- Individual visible hollow organic nest mounds distributed broadly through the Jungle Planet without clusters.
- Safe deferred nest placement only after nearby chunks and players are fully loaded.
- No synchronous chunk-load edits, terrain clearing, terrain reshaping, or tree removal.
- Squiggler packs emerge from nearby nests at night when the area is dark enough.
- Emergence packs contain 2–4 Squigglers, with per-nest cooldowns and a local population cap of 16.
- Squigglers remain fast, aggressive pack predators whose attacks apply a short poison effect.
- Squigglers climb trunks and pass through leaves to reach exposed players in tree canopies; solid wood and stone remain protective barriers.
- Player-killed Squigglers drop venom sacs for lore-compatible quests and cures.
- Peaceful difficulty prevents nest emergence.

Do not change nest generation, nest appearance, emergence timing, pack size, cooldowns,
population limits, combat behavior, or placement scheduling unless the user explicitly requests it.
