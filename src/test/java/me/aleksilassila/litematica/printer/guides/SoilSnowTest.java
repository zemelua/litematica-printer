package me.aleksilassila.litematica.printer.guides;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.guides.interaction.*;
import me.aleksilassila.litematica.printer.guides.placement.FarmlandGuide;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SoilSnowTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Block> pathSoils() { return DirtPathTest.soils(); }
    static Stream<Block> farmSoils() { return Stream.of(FarmlandGuide.TILLABLE_BLOCKS); }
    static Stream<Block> nonSnow() {
        return Stream.of(Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.STONE,
                Blocks.SHORT_GRASS, Blocks.WATER, Blocks.WHEAT, Blocks.AIR);
    }

    private boolean clear(Block target, Block soil, Block above, Block plannedAbove) {
        return SoilSnowGuide.shouldClear(target.defaultBlockState(), soil.defaultBlockState(),
                above.defaultBlockState(), plannedAbove.defaultBlockState());
    }

    @ParameterizedTest @MethodSource("pathSoils")
    void clearsSnowBeforeFlatteningAllSupportedSoils(Block soil) {
        assertTrue(clear(Blocks.DIRT_PATH, soil, Blocks.SNOW, Blocks.AIR));
        assertFalse(PathFlatteningGuide.canFlatten(Blocks.DIRT_PATH.defaultBlockState(),
                soil.defaultBlockState(), Blocks.SNOW.defaultBlockState()));
        assertTrue(PathFlatteningGuide.canFlatten(Blocks.DIRT_PATH.defaultBlockState(),
                soil.defaultBlockState(), Blocks.AIR.defaultBlockState()));
    }

    @ParameterizedTest @MethodSource("farmSoils")
    void clearsSnowBeforeTillingIncludingTheIntermediateDirtStep(Block soil) {
        assertTrue(clear(Blocks.FARMLAND, soil, Blocks.SNOW, Blocks.WHEAT));
        assertFalse(TillingGuide.canTill(Blocks.FARMLAND.defaultBlockState(),
                soil.defaultBlockState(), Blocks.SNOW.defaultBlockState()));
        assertTrue(TillingGuide.canTill(Blocks.FARMLAND.defaultBlockState(),
                soil.defaultBlockState(), Blocks.AIR.defaultBlockState()));
    }

    @ParameterizedTest @ValueSource(ints = {1,2,3,4,5,6,7,8})
    void handlesEveryAccumulatedSnowDepth(int layers) {
        var snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
        for (Block target : new Block[]{Blocks.DIRT_PATH, Blocks.FARMLAND}) {
            assertTrue(SoilSnowGuide.shouldClear(target.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
                    snow, Blocks.AIR.defaultBlockState()));
        }
    }

    @ParameterizedTest @MethodSource("nonSnow")
    void neverMinesOtherBlocksOrFluids(Block above) {
        for (Block target : new Block[]{Blocks.DIRT_PATH, Blocks.FARMLAND})
            assertFalse(clear(target, Blocks.DIRT, above, Blocks.AIR));
    }

    @Test
    void respectsTheSchematicAndLeavesFinishedOrUnconvertibleGroundAlone() {
        for (Block target : new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.AIR, Blocks.STONE})
            assertFalse(clear(target, Blocks.DIRT, Blocks.SNOW, Blocks.AIR));
        for (Block target : new Block[]{Blocks.DIRT_PATH, Blocks.FARMLAND}) {
            assertFalse(clear(target, target, Blocks.SNOW, Blocks.AIR));
            for (Block ground : new Block[]{Blocks.STONE, Blocks.WATER, Blocks.AIR, Blocks.SPRUCE_LOG})
                assertFalse(clear(target, ground, Blocks.SNOW, Blocks.AIR));
            for (Block planned : new Block[]{Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.SPRUCE_FENCE})
                assertFalse(clear(target, Blocks.DIRT, Blocks.SNOW, planned));
        }
        assertFalse(clear(Blocks.FARMLAND, Blocks.PODZOL, Blocks.SNOW, Blocks.AIR));
        assertFalse(clear(Blocks.DIRT_PATH, Blocks.DIRT, Blocks.SNOW, Blocks.WHEAT));
    }

    @Test
    void supportsFarmSchematicsWithCropsAboveSoil() {
        for (Block crop : new Block[]{Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES,
                Blocks.BEETROOTS, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM})
            assertTrue(clear(Blocks.FARMLAND, Blocks.DIRT, Blocks.SNOW, crop));
    }

    @Test
    void settingsCanDisableRemovalAndPathsIndependently() {
        boolean clear = Configs.CLEAR_SOIL_SNOW.getBooleanValue();
        boolean paths = Configs.MAKE_PATHS.getBooleanValue();
        boolean interact = Configs.INTERACT_BLOCKS.getBooleanValue();
        try {
            Configs.CLEAR_SOIL_SNOW.setBooleanValue(true);
            Configs.INTERACT_BLOCKS.setBooleanValue(true);
            Configs.MAKE_PATHS.setBooleanValue(false);
            assertFalse(SoilSnowGuide.enabled(Blocks.DIRT_PATH.defaultBlockState()));
            assertTrue(SoilSnowGuide.enabled(Blocks.FARMLAND.defaultBlockState()));
            Configs.MAKE_PATHS.setBooleanValue(true);
            assertTrue(SoilSnowGuide.enabled(Blocks.DIRT_PATH.defaultBlockState()));
            Configs.CLEAR_SOIL_SNOW.setBooleanValue(false);
            assertFalse(SoilSnowGuide.enabled(Blocks.FARMLAND.defaultBlockState()));
            Configs.CLEAR_SOIL_SNOW.setBooleanValue(true);
            Configs.INTERACT_BLOCKS.setBooleanValue(false);
            assertFalse(SoilSnowGuide.enabled(Blocks.FARMLAND.defaultBlockState()));
        } finally {
            Configs.CLEAR_SOIL_SNOW.setBooleanValue(clear);
            Configs.MAKE_PATHS.setBooleanValue(paths);
            Configs.INTERACT_BLOCKS.setBooleanValue(interact);
        }
    }

    @Test
    void registersSnowRemovalBeforeSoilPlacementAndInteractions() {
        var classes = new Guides().getGuides().stream().map(g -> g.getA()).toList();
        int snow = classes.indexOf(SoilSnowGuide.class);
        assertTrue(snow >= 0);
        assertTrue(snow < classes.indexOf(FarmlandGuide.class));
        assertTrue(snow < classes.indexOf(TillingGuide.class));
        assertTrue(snow < classes.indexOf(PathFlatteningGuide.class));
    }

    @Test
    void vanillaMiningHooksExistInTheTargetMinecraftVersion() throws Exception {
        assertEquals(void.class, Minecraft.class.getDeclaredMethod("continueAttack", boolean.class).getReturnType());
        assertEquals(void.class, Minecraft.class.getDeclaredMethod("tick").getReturnType());
    }
}
