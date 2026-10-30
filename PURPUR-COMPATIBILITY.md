# Purpur compatibility — RandomTeleportPlugin 1.1.0

Author: **SakuraZ**. Test date: 2026-10-31 (Asia/Bangkok).

**The same universal JAR supports Paper and Purpur. No Purpur-specific plugin
binary, runtime dependency, or code change was needed.** Paper results remain in
[COMPATIBILITY.md](COMPATIBILITY.md).

| Purpur version | Build | Java | Checks | Result |
| --- | --- | --- | --- | --- |
| 1.21 | 2284 | 21 | 32 | PASS |
| 1.21.1 | 2329 | 21 | 32 | PASS |
| 1.21.3 | 2358 | 21 | 32 | PASS |
| 1.21.4 | 2416 | 21 | 32 | PASS |
| 1.21.5 | 2450 | 21 | 32 | PASS |
| 1.21.6 | 2465 | 21 | 32 | PASS |
| 1.21.7 | 2477 | 21 | 32 | PASS |
| 1.21.8 | 2497 | 21 | 32 | PASS |
| 1.21.9 | 2505 | 21 | 32 | PASS |
| 1.21.10 | 2535 | 21 | 32 | PASS |
| 1.21.11 | 2568 | 21 | 32 | PASS |
| 26.1.2 | 2592 | 25 | 32 | PASS |
| 26.2 | 2633 | 25 | 32 | PASS |

All 13 Purpur release versions published by the official API within
the requested 1.21–26.2 range passed. Prereleases were excluded. The API does not
list Purpur 1.21.2 or 26.1.1, so those version numbers are not claimed as tested.

## Validation

The existing 32-check probe ran inside native Purpur servers, not Paper with
Purpur API libraries. It checked loading/entrypoint, commands and permissions,
27-slot GUI/icons, shift-click and drag protection, Adventure Actionbar, reload,
hidden-coordinate defaults, async player teleport API signature, cooldown YAML
round trip, movement cancellation and rotation-only behavior. Safe-location
search, lava/headroom rejection and world borders were checked against actual
Overworld, Nether and End blocks.

All test servers were isolated, bound to localhost, and shut down through their
own console. Every run exited with code 0 and saved all dimensions. Production
server files and player data were not changed.

Player/GUI event tests use simulated players and inventory views; they do not
include a real client's complete selection → countdown → teleport sequence.
Purpur's default settings were used. Custom Purpur configurations, performance
under load and all combinations of other plugins are not covered. Folia and
Spigot are still not supported by these results.

## Artifact and evidence

Use **`RandomTeleportPlugin-1.1.0.jar`** for either Paper or Purpur.

SHA-256: `d4cabdef09c2e8b14a5a9de80e3df9a56a07c2628cea6e11461fac2fab6dcccc`

- [purpur-compatibility-results.json](purpur-compatibility-results.json): raw result matrix.
- Detailed console logs and probe results remain in the original workspace at
  `test-servers/RandomTeleportPurpurCompatibility/<version>/`.
- `purpur-download.py`: download fixed successful Purpur builds; verify the
  API-provided MD5 for transfer integrity and record SHA-256 locally.
- `compat-tests/run.py --platform purpur --server-root <lab-root> --port-base 25701`:
  run the same probe in a separate Purpur lab. Java 21/25 and EULA paths can be
  supplied with `--java21`, `--java25`, and `--eula-file`.
- `compat-tests/purpur-report.py --server-root <lab-root>`: regenerate this report.

Official references: [Purpur documentation](https://purpurmc.org/docs/purpur/),
[published server versions](https://api.purpurmc.org/v2/purpur).
