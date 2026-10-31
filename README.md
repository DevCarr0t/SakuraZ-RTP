# Sakura Random Teleport

**Sakura Random Teleport**, created by **SakuraZ**, lets players explore your server through a simple random teleport menu. Choose Overworld, Nether, or The End, then teleport to a location checked for suitable terrain.

Built for **Paper and Purpur**, with one universal JAR covering published server releases from **Minecraft 1.21 to 26.2**.

## Features

- **World-selection GUI** — Choose a destination from a 27-slot menu.
- **Configurable teleport areas** — Set each world's area size and center coordinates.
- **Terrain safety checks** — Check floors, headroom, nearby hazards, and world borders.
- **Asynchronous chunk loading** — Prepare candidate chunks using Paper's asynchronous APIs.
- **Warmup countdown** — Five seconds by default, with configurable sounds.
- **Movement cancellation** — Moving cancels a pending teleport; looking around does not.
- **Persistent cooldowns** — A shared per-player cooldown survives reconnects and server restarts.
- **Actionbar notifications** — Display countdowns, status messages, and results above the hotbar.
- **Hidden coordinates** — Destination coordinates are omitted from the default success message.
- **Protected menu items** — Players cannot take or move the destination icons.

Failed or cancelled teleports do not start a new cooldown. Terrain checks reduce unsafe landings but cannot guarantee protection from mobs, traps, or later changes.

## Commands

| Command | Description |
| --- | --- |
| `/rtp` | Open the teleport menu |
| `/randomteleport` | Alias for `/rtp` |
| `/rtp reload` | Reload configuration in-game |

## Permissions

| Permission | Default |
| --- | --- |
| `randomteleportplugin.use` | Everyone |
| `randomteleportplugin.admin` | Operators |

## Installation

1. Stop your server.
2. Place `RandomTeleportPlugin-1.1.1.jar` in the `plugins` folder.
3. Start the server.
4. Edit `plugins/RandomTeleportPlugin/config.yml` as needed.
5. Run `/rtp reload` in-game as an administrator.

Destination worlds must already be loaded. The plugin does not create worlds.

## Compatibility

- **Platforms:** Paper and Purpur
- **Server versions:** Published releases within Minecraft 1.21–26.2
- **Java:** 21 for Minecraft 1.21.x; 25 for Minecraft 26.x
- **Client mods:** None required

Server-side compatibility checks passed on **14 Paper versions and 13 Purpur versions**. GUI tests used simulated players; full client gameplay, load testing, and every plugin combination are not covered.

**Bukkit/CraftBukkit, Spigot, and Folia are not currently supported.**

---

**Author: SakuraZ**
