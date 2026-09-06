# Accepted Baseline

## SGJourney Destiny DHD — Forge 1.20.1

- Accepted version: `0.0.1-alpha`
- Accepted on: 2026-09-05
- Physical console model: `tools/destiny_console_generator_v4.py`
- Screen: dense three-band Ancient-style display using the alpha.25 dual-island UV mapping
- Placement: front/operator side faces the placing player

This is the accepted visual and functional baseline. Preserve its model geometry, OBJ UV
layout, collision and screen interaction targets, placement orientation, Ancient display,
SGJourney Universe DHD dialer/crystal/power behavior, and optional SGJ Deco Destiny Bearing
integration unless a later change is explicitly requested.

The accepted bearing behavior illuminates only the bearing bulb before the first dialing
symbol reaches the gate, holds it continuously through the first-chevron rotation gap, and
returns control to normal gate-state tracking once dialing activity is visible.

Version progression: development/test builds use the `0.0.x-dev` line. Public alpha
builds use the `0.0.x-alpha` line, beta builds begin with `0.1.0-beta.1`, and the
first full release will be `1.0.0`.
