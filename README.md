# RandomTeleportPlugin

A configurable random teleport plugin for **Paper 1.21.11**, featuring a world-selection GUI, terrain safety checks, asynchronous chunk loading, and persistent player cooldowns.

## Requirements

- Paper **1.21.11**
- Java **21**
- No client mods or required companion plugins

Other Minecraft versions, Spigot, and Folia are not verified by this project.

## Features

- **World selector:** choose Overworld, Nether, or The End from a 27-slot GUI.
- **Configurable destinations:** customize worlds, display names, icons, menu slots, area sizes, and center coordinates.
- **Warmup countdown:** five seconds by default, with configurable sounds.
- **Movement cancellation:** changing position cancels a pending teleport; looking around does not.
- **Persistent cooldown:** the default 60-second cooldown is shared across destinations and survives reconnects and restarts.
- **Cooldown after success:** failed or cancelled requests do not start a new cooldown.
- **Asynchronous loading:** load candidate chunks and teleport through Paper's asynchronous APIs; block inspection stays on the server thread.
- **Safe-location checks:** reject hazardous terrain, water, unsafe headroom, and positions outside the world border.
- **Actionbar feedback:** show countdowns, search status, success, and normal errors above the hotbar.
- **Hidden coordinates:** the default success notification displays the world name without X/Y/Z.
- **Protected menu:** prevent players from taking or moving selector items.
- **Duplicate-request protection:** allow only one pending RTP request per player.

## Installation

1. Stop your Paper server.
2. Place `RandomTeleportPlugin-1.0.0.jar` in the server's `plugins/` directory.
3. Start the server to generate `plugins/RandomTeleportPlugin/config.yml`.
4. Update the destination world names and settings as needed.
5. Run `/rtp reload` **in-game** as an administrator after editing the configuration.

Destination worlds must already be loaded. RandomTeleportPlugin does not create worlds.

## Commands and permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/rtp` | Open the random teleport menu. | `randomteleportplugin.use` |
| `/randomteleport` | Alias for `/rtp`. | `randomteleportplugin.use` |
| `/rtp reload` | Reload the configuration. | `randomteleportplugin.admin` |

| Permission | Default |
| --- | --- |
| `randomteleportplugin.use` | Everyone |
| `randomteleportplugin.admin` | Operators |

Commands are player-only. Running them from the console prints an informational message; it does not open a menu or reload the configuration. There is currently no separate cooldown-bypass permission.

## Default destinations

| Destination | World | Icon | Slot | Area: X × Z | Block-coordinate range per axis |
| --- | --- | --- | --- | --- | --- |
| Overworld | `world` | `GRASS_BLOCK` | `11` | 1,250 × 1,250 | −625 to 624 |
| Nether | `world_nether` | `NETHERRACK` | `13` | 156 × 156 | −78 to 77 |
| The End | `world_the_end` | `END_STONE` | `15` | 2,500 × 2,500 | −1,250 to 1,249 |

Menu slots are **zero-based**, from `0` to `26`. Unused slots remain empty.

**`size-x` and `size-z` are full side lengths, not radii.** Areas default to a center of X=0, Z=0. Landing locations use block centers, so their X/Z values include `.5`.

## Configuration

Edit `plugins/RandomTeleportPlugin/config.yml`. This partial example shows the general settings and one destination; the generated file also includes Nether, End, messages, and sounds.

```yaml
countdown-seconds: 5
cooldown-seconds: 60

search:
  max-attempts: 80
  timeout-seconds: 30

menu:
  title: Random Teleport
  item-lore:
    - '&eClick to teleport'

destinations:
  overworld:
    world: world
    name: '&aOverworld'
    icon: GRASS_BLOCK
    slot: 11
    size-x: 1250
    size-z: 1250
    center-x: 0
    center-z: 0
```

Add or remove destination entries as needed. Each entry must use a unique menu slot and a valid item material.

### Accepted settings

| Setting | Default | Accepted range |
| --- | --- | --- |
| `countdown-seconds` | `5` | 1–60 seconds |
| `cooldown-seconds` | `60` | 1–86,400 seconds |
| `search.max-attempts` | `80` | 1–500 candidates |
| `search.timeout-seconds` | `30` | 5–120 seconds |

Area side lengths must be 1–1,000,000 blocks and satisfy the plugin's world-coordinate bounds. Nether floor limits default to Y=32–110. `nether-min-y` and `nether-max-y` must be between 1 and 125, with the minimum no greater than the maximum.

### Messages

Messages and menu text support legacy `&` color codes.

```yaml
messages:
  countdown: '&aTeleporting to {world} in {seconds}s...'
  searching: '&eFinding a safe location...'
  success: '&aTeleported to {world}'
  moved: '&cTeleport cancelled because you moved.'
```

| Context | Placeholders |
| --- | --- |
| Countdown | `{world}`, `{seconds}` |
| Cooldown | `{seconds}` |
| Success | `{world}`, `{x}`, `{y}`, `{z}` |
| Menu item lore | `{size_x}`, `{size_z}` |

Coordinates are omitted from the default success message, but can be added using the optional placeholders. This hides coordinates only in the notification, not in Minecraft's debug screen or client mods.

### Sounds

```yaml
sounds:
  teleport-success:
    enabled: true
    sound: entity.enderman.teleport
    volume: 1.0
    pitch: 1.0
  countdown:
    enabled: true
    sound: block.note_block.hat
    volume: 1.0
    pitch: 1.0
```

Sounds play only to the affected player. Set `enabled: false` to disable a sound.

## How location checks work

- **Overworld:** check the highest `MOTION_BLOCKING_NO_LEAVES` surface instead of scanning down into caves.
- **Nether:** scan down for a safe floor within the configured Y range, excluding bedrock and the roof.
- **The End:** require a solid surface and reject the void.
- **All dimensions:** require an occluding floor and two air blocks above it. Reject hazardous floors and nearby hazards, including lava, water, magma, fire, cactus, campfires, powder snow, and pointed dripstone.
- Reject chunk-edge candidates to avoid synchronously loading neighboring chunks during safety checks.
- Require the landing position and a surrounding margin to be inside the world border.

If no suitable location is found within the attempt or search-time limit, RTP fails without applying a cooldown. Terrain checks reduce unsafe landings; they do not guarantee protection from mobs, traps, or changes made after the check.

## Cancellation and combat integration

Moving, disconnecting, or being teleported elsewhere cancels a pending request. Closing the selector before choosing a destination starts no request.

Optional **IceCurrencies** integration blocks RTP when that plugin's `icesmp-combat-until` metadata marks the player as in combat, and cancels pending requests. Its combat-block message appears in chat rather than the Actionbar. Other combat-tag plugins are not automatically supported.

## Stored files

```text
plugins/RandomTeleportPlugin/
├── config.yml       # Worlds, GUI, timing, messages, and sounds
└── cooldowns.yml    # Active cooldown expiry timestamps by player UUID
```

Back up both files when moving the server. Cooldown saves use a temporary file and atomic replacement where supported.

## Development and testing

The implementation consists of `src/RandomTeleportPlugin.java` and `src/RtpRules.java`, using the Paper 1.21.11 API.

Build with Java 21's `javac.exe` available on PATH:

```powershell
./build.ps1 -LibrariesPath 'C:/path/to/paper-server/libraries'
```

The library directory must contain the Paper 1.21.11 API and its dependencies. In the original server workspace the script finds the `libraries/` folder automatically. Use `-JdkBin 'C:/path/to/jdk-21/bin'` to select a JDK explicitly. Resources are packaged from `classes/`, which must contain `plugin.yml` and `config.yml`.

Output: `dist/RandomTeleportPlugin-1.0.0.jar`.

When replacing an earlier installation, stop the server, back up its data, remove the previous plugin JAR, and copy the previous configuration and cooldown files into `plugins/RandomTeleportPlugin/`. Update permission assignments to `randomteleportplugin.use` and `randomteleportplugin.admin`, then install the new JAR. Do not run both versions together: they register the same `/rtp` command.

- `TestRtpRules.java`: sampling boundaries, cooldown rounding, and hazard exclusions.
- `TestRtpConfig.java`: original default destinations, timing, and required message templates.

These focused tests do not establish compatibility with every Minecraft version or server plugin.

## License

See the repository's [LICENSE](LICENSE) file.
