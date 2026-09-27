package me.aleksilassila.litematica.printer.guides;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import me.aleksilassila.litematica.printer.guides.interaction.PathFlatteningGuide;
import me.aleksilassila.litematica.printer.guides.placement.DirtPathGuide;
import me.aleksilassila.litematica.printer.guides.placement.GuesserGuide;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirtPathBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DirtPathTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Block> soils() {
        return Stream.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
                Blocks.ROOTED_DIRT, Blocks.PODZOL, Blocks.MYCELIUM);
    }

    static Stream<Block> obstructions() {
        return Stream.of(Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW,
                Blocks.SHORT_GRASS, Blocks.STONE, Blocks.WATER);
    }

    static Stream<Block> otherBlocks() {
        return Stream.of(Blocks.FARMLAND, Blocks.STONE, Blocks.GRAVEL,
                Blocks.SPRUCE_LOG, Blocks.DIRT_PATH, Blocks.AIR);
    }

    @ParameterizedTest
    @MethodSource("soils")
    void convertsExistingVanillaSoilsWithoutReplacingThem(Block soil) {
        assertTrue(PathFlatteningGuide.canFlatten(Blocks.DIRT_PATH.defaultBlockState(),
                soil.defaultBlockState(), Blocks.AIR.defaultBlockState()));
        assertFalse(DirtPathGuide.canPrepare(Blocks.DIRT_PATH.defaultBlockState(),
                soil.defaultBlockState(), Blocks.AIR.defaultBlockState()));
    }

    @ParameterizedTest
    @MethodSource("obstructions")
    void leavesSnowPlantsAndOtherHeadroomObstructionsAlone(Block above) {
        assertFalse(PathFlatteningGuide.canFlatten(Blocks.DIRT_PATH.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(), above.defaultBlockState()));
        assertFalse(DirtPathGuide.canPrepare(Blocks.DIRT_PATH.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), above.defaultBlockState()));
    }

    @ParameterizedTest
    @MethodSource("otherBlocks")
    void doesNotFlattenUnrelatedOrAlreadyFinishedBlocks(Block block) {
        assertFalse(PathFlatteningGuide.canFlatten(Blocks.DIRT_PATH.defaultBlockState(),
                block.defaultBlockState(), Blocks.AIR.defaultBlockState()));
    }

    @Test
    void onlyActsWhereSchematicRequestsAPath() {
        for (Block target : new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.FARMLAND, Blocks.AIR}) {
            assertFalse(PathFlatteningGuide.canFlatten(target.defaultBlockState(),
                    Blocks.DIRT.defaultBlockState(), Blocks.AIR.defaultBlockState()));
            assertFalse(DirtPathGuide.canPrepare(target.defaultBlockState(),
                    Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState()));
        }
    }

    @Test
    void waitsForPlacedDirtBeforeFlattening() {
        var path = Blocks.DIRT_PATH.defaultBlockState();
        var air = Blocks.AIR.defaultBlockState();
        assertTrue(DirtPathGuide.canPrepare(path, air, air));
        assertFalse(PathFlatteningGuide.canFlatten(path, air, air));
        assertTrue(PathFlatteningGuide.canFlatten(path, Blocks.DIRT.defaultBlockState(), air));
        assertFalse(DirtPathGuide.canPrepare(path, path, air));
        assertFalse(PathFlatteningGuide.canFlatten(path, path, air));
    }

    @Test
    void usesExistingGuidePipelineBeforeGenericPlacement() {
        var guides = new Guides().getGuides();
        int flatten = -1, place = -1, generic = -1;
        for (int i = 0; i < guides.size(); i++) {
            var entry = guides.get(i);
            if (entry.getA() == PathFlatteningGuide.class) {
                flatten = i;
                assertArrayEquals(new Class<?>[]{DirtPathBlock.class}, entry.getB());
            }
            if (entry.getA() == DirtPathGuide.class) {
                place = i;
                assertArrayEquals(new Class<?>[]{DirtPathBlock.class}, entry.getB());
            }
            if (entry.getA() == GuesserGuide.class) generic = i;
        }
        assertTrue(flatten >= 0 && place > flatten && generic > place);
    }
}
