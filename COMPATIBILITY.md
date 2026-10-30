# Compatibility â€” RandomTeleportPlugin 1.1.0

Target: **Paper 1.21â€“26.2**. Built against the **Paper 1.21 API**, with
`api-version: '1.21'` and Java 21 bytecode. The same JAR was used in every test.

Test date: 2026-10-31 (Asia/Bangkok).

| Paper version | Build | Java | Checks | Result |
| --- | --- | --- | --- | --- |
| 1.21 | 130 | 21 | 32 | PASS |
| 1.21.1 | 133 | 21 | 32 | PASS |
| 1.21.3 | 83 | 21 | 32 | PASS |
| 1.21.4 | 232 | 21 | 32 | PASS |
| 1.21.5 | 114 | 21 | 32 | PASS |
| 1.21.6 | 48 | 21 | 32 | PASS |
| 1.21.7 | 32 | 21 | 32 | PASS |
| 1.21.8 | 60 | 21 | 32 | PASS |
| 1.21.9 | 59 | 21 | 32 | PASS |
| 1.21.10 | 130 | 21 | 32 | PASS |
| 1.21.11 | 132 | 21 | 32 | PASS |
| 26.1.1 | 29 | 25 | 32 | PASS |
| 26.1.2 | 74 | 25 | 32 | PASS |
| 26.2 | 135 | 25 | 32 | PASS |

All 14 published release versions passed 32 checks each. Servers ran
in separate disposable directories bound to localhost, then shut down through
their own console. Every run exited with code 0 and saved all three dimensions.
These are native Paper server tests, not newer clients connecting through ViaVersion.
Prereleases and version numbers without a published Paper build are not listed.

## What was checked

- Plugin loading, the 1.1.0 entrypoint, command/alias and permission registration.
- A 27-slot GUI, three destination icons, shift-click and drag cancellation.
- Adventure Actionbar, the player reload path, config parsing and hidden-coordinate defaults.
- The async player teleport API signature.
- Cooldown YAML write/load round trip keyed by UUID.
- Rotation-only movement keeps a pending request; position movement cancels it.
- Real worlds in Overworld, Nether and End: safe floor search, lava rejection,
  blocked headroom rejection and world-border enforcement.

The separate unit tests also passed 120,000 range samples, boundary checks,
cooldown rounding, hazard exclusions and default-config checks.

## Limits

GUI/player event tests use simulated players and inventory views inside the real
server. Terrain and border tests use real Bukkit worlds and blocks. This does
not include a real client's full GUI-selection â†’ countdown â†’ completed teleport
sequence, manual gameplay, load testing, or every possible server-plugin combination.
This report records Paper tests. Additional Purpur validation is documented in
[PURPUR-COMPATIBILITY.md](PURPUR-COMPATIBILITY.md). Spigot, Folia and modded
hybrids were not independently tested.

## Runtime and build

- Paper 1.21.x: use Java 21.
- Paper 26.1.x / 26.2: use Java 25 for the server. The plugin JAR still has Java 21 bytecode.
- Compile against Paper 1.21, not a newer API, using `build.ps1`.
- `test.ps1` runs the focused rule/config tests.
- `compat-tests/build.ps1` packages the test-only probe; never install it on a live server.
- `compat-download.py` downloads official Paper release builds and verifies their SHA-256.
- `compat-tests/run.py` supports explicit `--server-root`, `--java21`, `--java25`
  and `--eula-file` paths. It uses a copy of an existing EULA file supplied by the operator.
- `compat-tests/report.py` validates the matrix and generates this report.

## Artifact

`dist/RandomTeleportPlugin-1.1.0.jar`

Tested JAR SHA-256: `6b23e09ae31991a0dd11c5ace251658397fbfdddeeda95fc722c25fff291335b`

Machine-readable evidence: [compatibility-results.json](compatibility-results.json).
Detailed per-server console logs and probe reports are retained in the original
test workspace under `test-servers/RandomTeleportCompatibility/<version>/`.

## References

- [Paper plugin.yml API version rules](https://docs.papermc.io/paper/dev/plugin-yml/)
- [Paper Java requirements](https://docs.papermc.io/paper/getting-started/)
- [Official Paper downloads service](https://docs.papermc.io/misc/downloads-service/)

## Author metadata update

The distributed JAR now lists **SakuraZ** as its author. Only `plugin.yml` changed; every other ZIP entry, including all class bytecode, is identical to the 14-version tested JAR above. The full server matrix was not rerun for this metadata-only edit.

Current distributed JAR SHA-256: `d4cabdef09c2e8b14a5a9de80e3df9a56a07c2628cea6e11461fac2fab6dcccc`
