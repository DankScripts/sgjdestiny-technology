# Destiny routing hotfix — dev172

The supplied 2026-10-17 02:10:50 server report identifies a null galaxy key in SGJourney 0.6.51's `AddressRegion.findCommonGalaxy` while Destiny's Universe gate at (-8, 74, -299) dials a nine-chevron destination. The 02:12:52 report is a later watchdog timeout. The two October 16 loading reports instead show Lantea requiring Forge >=47.4.13 while the server had 47.4.0.

Our existing-world Mediora migration passed a registry template straight to `Universe.addGalaxy`. Registry galaxy templates have no internal key. When SGJourney attaches route regions to that object, it adds a null galaxy key to their address maps. This can both prevent the seeded seven-chevron routes from resolving and crash connection classification for Earth.

dev172 registers a keyed galaxy copy, repairs null-key residue and reverse address indexes for Destiny's route regions at startup, and preserves valid saved addresses. The address menu now also recognizes SGJourney's nine-chevron fallback as unsuitable for a seeded seven-chevron route and consults the route registry instead. The repair is repeatable; no ship reset or deletion of universe/network data is required.

## Hosted server installation

1. Stop the server and back up the world and current Destiny Technology jar.
2. Replace the existing `sgjdestiny-tech` jar in `mods` with `sgjdestiny-tech-1.20.1-0.0.3-dev-172.jar`. Keep exactly one Destiny Technology jar.
3. Restart the server. Startup repairs the affected route records automatically.
4. Use the same build on clients. Open the Destiny dialer and check the seeded destinations; test one planet and then Earth. If either still fails, retain the new server log/report for diagnosis.

The hosted server has not been updated remotely by this task. Its full modpack/world has not been run locally.

## Verification

- Build and existing checks passed.
- Regression test against real SGJourney 0.6.51 classes reproduced the original null-key exception, then verified the repair, reverse lookup, saved-address preservation, idempotence, and serialization.
- Packaged ship manifest and all 210 structure tiles remain identical to the bundled source.

Regression runner: `tools/test_destiny_route_repair.py` (requires an installed Minecraft 1.20.1/Forge library set, SGJourney jar, and Java 17).
