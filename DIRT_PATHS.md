# Automatic dirt paths for Minecraft 26.1.2

This fork of sakura-ryoko/litematica-printer's `LTS/26.1` branch adds soil placement and shovel flattening to Litematica Printer 3.2.2. The custom release is `3.2.2-paths.2` and retains the `litematica_printer` mod ID: **replace the original Printer JAR, do not install both**.

## Use

1. Close Minecraft before changing the instance's mods. Back up the original Printer JAR outside the `mods` folder.
2. Replace it with `litematica-printer-26.1.2-3.2.2-paths.2.jar`.
3. In Litematica's Generic settings (`M + C` by default), enable `makePaths` (default: true) and `interactBlocks`.
4. Keep dirt and a shovel in your inventory; using the hotbar is the simplest way to test. All seven vanilla shovel materials, including copper, are supported.
5. Load a schematic containing **dirt path blocks**, enable printing, and move within the configured placement range.

For an empty target, the printer places ordinary dirt. On a subsequent scan, after the placed block appears in the client world, it uses a shovel through the existing interaction/action queue. Existing dirt, grass blocks, coarse dirt, rooted dirt, podzol, and mycelium can be flattened in place without consuming another dirt block.

Only positions whose schematic target is `minecraft:dirt_path` are affected. Finished paths are skipped. Farmland, stone, logs, and unrelated schematic targets are not converted. The optional snow removal described below also supports schematic farmland.

The block above must be **air**, following vanilla shovel behavior. Plants, fluids, snow blocks and other obstructions make the printer wait. Accumulated snow layers can be removed automatically as described below. Missing shovels prevent preparatory dirt placement; missing dirt does not prevent flattening existing soil. Turning `makePaths` off prevents the new path guides and their generic-placement fallback.

## Snow before paths and farmland

`clearSoilSnow` defaults to true. With printing and `interactBlocks` enabled, a shovel removes accumulated `minecraft:snow` (1–8 layers) immediately above existing convertible soil before flattening or tilling. Keep a hoe available too for farmland; all seven vanilla hoe materials including copper are supported. Coarse and rooted dirt proceed through ordinary dirt before the final till. Turn `clearSoilSnow` off to leave snow alone.

Only schematic dirt paths and farmland are eligible. Completed soil, unrelated blocks, snow blocks and powder snow remain untouched. Snow explicitly present in the schematic is preserved. Farm schematics may request crops or melon/pumpkin stems above the soil; snow there is removed before tilling and subsequent crop placement. Surrounding snow is not cleared, and this is not a continuous snow-clearing service for finished construction.

Mining uses vanilla start/continue/stop destruction, once per actual client input tick, so even a wooden shovel or mining fatigue uses normal break speed. The printer pauses placement while mining. The target and tool are rechecked each tick; leaving reach, losing line of sight, changing the tool/schematic/layer range, disabling printing, opening a screen, losing window focus, manual attack/use, or disconnecting cancels the job. A single attempt times out after 200 ticks. Ordinary printer scans may retry remaining obstructions while enabled. The client waits for the local world to report snow removal before the next soil operation; server-side permissions still apply.

## Build and tests

Use JDK 25 or newer and the included Gradle wrapper:

```text
./gradlew clean test build
```

The project targets Minecraft 26.1.2, Litematica 0.27.14, MaLiLib 0.28.12, and Fabric API 0.155.3+26.1.2. Java bytecode targets Java 25. The build was run on JDK 26 / Gradle 9.4.1 / Loom 1.15.5.

The JUnit suite uses actual Minecraft block states and checks the six supported soil types, six overhead obstructions (including snow), unrelated and already-complete blocks, the two-step state transition, schematic-target isolation, and guide registration/order. Compilation and automated tests do not replace an in-game multiplayer/server-interaction test; live gameplay has not been verified.

## Manual acceptance test

Use a small disposable test area, with printing restricted to its schematic:

- Survival, empty path target, dirt + shovel: dirt is placed, then becomes a path.
- Existing soil, shovel but no dirt: soil becomes a path.
- Missing shovel or `makePaths=false`: no preparatory dirt is placed.
- Snow layers (1 and 8) above existing soil: shovel removes snow, then the path/hoe operation follows. Test wooden and diamond shovels in survival and creative mode.
- Farmland schematic with wheat above it: clear snow, till, then plant. Test dirt, grass, coarse dirt, rooted dirt and existing paths, including a copper hoe.
- `clearSoilSnow=false`, missing shovel, missing hoe for farmland: no snow removal.
- Snow blocks, powder snow, snow included in the schematic and unrelated neighboring snow remain intact.
- While a slow mining attempt is active: disable printing, open inventory, walk out of reach, switch tools, change schematic, attack manually, and disconnect. Mining must stop without touching the soil underneath.
- Verify both singleplayer and a server: no accelerated mining under fatigue and no extra operations after a server rejects a break.
- Schematic asks for ordinary dirt next to a path: ordinary dirt stays dirt.
- A finished path remains untouched; a stone obstruction is not excavated.
- Regression: normal placement, original log stripping, and farmland tilling still work.

The source remains under the repository's AGPL-3.0 license. The bundled license filename and JAR metadata now match `LICENSE.md`; upstream's stale CC0 metadata was not used to relicense the source.
