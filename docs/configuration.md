---
layout: default
title: Configuration
nav_order: 6
---

# Configuration

Forge creates `sgjdestiny_dhd-server.toml` in the world's `serverconfig` directory after the world is loaded with the addon installed.

## Kino server limit

```toml
[kino]
maxDeployedKinos = 16
```

| Setting | Default | Range | Purpose |
|:--|--:|--:|:--|
| `kino.maxDeployedKinos` | 16 | 1–24 | Maximum number of simultaneously deployed Kinos across all loaded server dimensions |

When the limit is reached, additional deployment is refused before a Kino item is consumed. Recalling an active Kino frees a slot.

{: .note }
The main server cost comes from active remote chunk loading. Live-video rendering itself primarily affects the client viewing the feed.
