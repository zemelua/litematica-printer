# Automatic dirt paths for Minecraft 26.1.2

This fork of sakura-ryoko/litematica-printer's `LTS/26.1` branch adds soil placement and shovel flattening to Litematica Printer 3.2.2. The custom release is `3.2.2-paths.6` and retains the `litematica_printer` mod ID: **replace the original Printer JAR, do not install both**.

## Use

1. Close Minecraft before changing the instance's mods. Back up the original Printer JAR outside the `mods` folder.
2. Replace it with `litematica-printer-26.1.2-3.2.2-paths.6.jar`.
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

## Easy Place substitutions and crop age

`easyPlaceDirtForGrass` (default true) allows dirt at schematic grass-block positions in Printer and both Litematica Easy Place implementations. Printer does not require Easy Place mode. Already placed dirt remains accepted after switching tools or exhausting the dirt stack; finished grass remains accepted as well. Dirt in the main hand takes priority, then the offhand. Held grass blocks retain normal placement. In survival, when neither hand contains either material, dirt is selected from inventory only if grass blocks are unavailable. Creative keeps normal grass pick-block unless dirt is explicitly held. Placement restriction uses the same effective target, retaining the existing reach, layer, replaceability and placement checks.

`ignoreCropAge` (default true) applies to Easy Place and Printer. Wheat, carrots, potatoes, beetroots, melon stems, pumpkin stems, nether wart, cocoa and sweet berry bushes are planted at age zero using their normal planting item (seeds, carrots, potatoes, etc.). A crop of the same type already in the world is accepted at any age. Cocoa facing and all non-age properties remain significant. Attached stems, multi-block pitcher crops and unrelated age-bearing blocks are not substituted. Normal farmland/support conditions still apply.

These are operation-local placement targets. The saved schematic, rendered schematic, verifier and material list are unchanged; the verifier can still show grass/dirt and crop-age differences. This is not forced growth or replacement of an existing solid block. Disable each setting to restore the corresponding original placement behavior.

Manual checks: with Printer off and Easy Place on, place dirt into an empty grass target (held dirt, inventory-only dirt, grass also available, placement restriction on/off, creative). Plant mature schematic wheat using seeds, then retry when actual wheat is at an intermediate age; it must not be broken/replanted. Repeat for carrots, beetroot and stems, and ensure wrong crop types and wrong cocoa facing are not accepted. Repeat planting using Printer and test both feature toggles off.

## Wrong block removal

`breakWrongBlocks` defaults to **false**. Enable it with `interactBlocks` to mine a different block type occupying a non-air schematic position. Printer removes the obstruction, then normal placement resumes after the local world reflects the break. Easy Place supports the same removal when holding the use/right-click button while aiming at the actual wrong block. Keep Easy Place hold mode enabled for repeated placement after removal, or click again to place. Releasing use or moving the crosshair away cancels Easy Place mining.

Scope is deliberately limited to **different block types at requested placement positions**. It does not clear schematic air or rebuild same-type blocks for facing, moisture, growth, connectivity or other property differences. It preserves permitted dirt-for-grass substitutes, soil awaiting shovel/hoe transformation, matching log stripping pairs, and naturally attached melon/pumpkin stems. Block entities (including chests, furnaces, signs and spawners), unbreakable blocks, fluids and blocks that can be replaced by ordinary placement are excluded.

Tools are selected from inventory by base destroy speed, preferring the held item on ties. Survival requires a harvest-capable tool where vanilla requires one, and avoids a tool whose next damage would break it. Bare-hand mining is allowed for blocks that do not require a harvest tool. Vanilla handles actual speed, effects, enchantments, durability and server permission. No instant-break packet shortcut is used.

A shared mining controller serializes snow removal and wrong-block removal. It revalidates world/player identity, reach, visibility, settings, layer range and held item each client tick. Wrong-block jobs additionally require both the original schematic state and the actual state to remain unchanged; removal stops if the block becomes air, correct, or another block. Attempts time out after 1200 ticks and may be retried by later enabled scans. The replacement material is needed for placement, not for removal.

Manual checks: Printer on with stone at a schematic plank position; Easy Place only with use held/released; correct/missing mining tool; setting off during mining; view/range/layer/schematic changes; protected chest and bedrock; schematic air; crop age and dirt-for-grass substitutes; existing snow clearing and original log stripping. Live client/multiplayer behavior remains unverified.

## paths.5 fix

Earlier versions applied grass-to-dirt substitution only to the legacy Easy Place path, leaving Printer to request grass items. The shared placement-target resolver is now used by Printer, legacy Easy Place and rewritten Easy Place, including their placement restrictions. Existing dirt is kept complete after inventory or hand changes. Tests now check the Printer call site, dirt material resolution, completed dirt/grass, and hook locations in both Easy Place implementations. No instance config or installed mod is modified by building the artifact.

## Waterlogged blocks (paths.6)

`waterlogBlocks` defaults to true. With Printer and `interactBlocks` enabled, supply water buckets in inventory. The normal guide places the slab/block first; a dedicated waterlogging guide then uses one water bucket after the correct dry block exists. Both top and bottom spruce slabs are supported, along with other vanilla liquid containers with a `waterlogged` property. All properties other than waterlogged must already match. Double slabs and water-evaporating environments are skipped.

Unlike ordinary block interactions, buckets use `Item.use` and calculate their own ray from the player's view. The action revalidates the schematic and actual block at send time, equips a water bucket, temporarily aims at the block, checks the full vanilla bucket ray, calls normal `gameMode.useItem` exactly once, and restores the view in a `finally` block. The normal use-item packet carries the target rotation. It does not call `useItemOn` first, collect water sources, refill buckets or drain completed waterlogged blocks. Survival consumes the supplied water; placement waits when no full bucket remains.

Manual checks: empty position -> bottom spruce slab -> waterlogged slab; existing top slab; already wet slab; missing water bucket; changed/removed block before the queued action; obstacle in the line of sight; water evaporating dimension; survival and creative, with Printer rotation on and off. Confirm no adjacent water spill and no automatic water pickup. Live gameplay remains unverified.

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
