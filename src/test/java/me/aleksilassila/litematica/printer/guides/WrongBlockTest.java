package me.aleksilassila.litematica.printer.guides;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import me.aleksilassila.litematica.printer.implementation.WrongBlockPolicy;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WrongBlockTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private boolean breaks(Block target, Block current) {
        return WrongBlockPolicy.shouldBreak(target.defaultBlockState(), current.defaultBlockState(), true, false);
    }

    static Stream<Block> wrongBlocks() {
        return Stream.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.SPRUCE_LOG,
                Blocks.DIRT, Blocks.GLASS, Blocks.SNOW_BLOCK, Blocks.SPRUCE_STAIRS);
    }

    @ParameterizedTest @MethodSource("wrongBlocks")
    void removesDifferentBlockTypesOnlyWhereTheSchematicWantsABlock(Block current) {
        assertTrue(breaks(Blocks.OAK_PLANKS, current));
        assertFalse(breaks(Blocks.AIR, current));
        assertFalse(breaks(Blocks.CAVE_AIR, current));
        assertFalse(breaks(current, current));
    }

    static Stream<Block> protectedBlocks() {
        return Stream.of(Blocks.CHEST, Blocks.BARREL, Blocks.SHULKER_BOX,
                Blocks.FURNACE, Blocks.SPRUCE_SIGN, Blocks.SPAWNER,
                Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.SHORT_GRASS);
    }

    @ParameterizedTest @MethodSource("protectedBlocks")
    void leavesContainersBlockEntitiesFluidsAndReplaceableBlocksAlone(Block current) {
        assertFalse(breaks(Blocks.STONE, current));
    }

    @Test void preservesIntermediateSoilsAndPermittedGrassSubstitution() {
        for (Block soil : new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
                Blocks.ROOTED_DIRT, Blocks.PODZOL, Blocks.MYCELIUM})
            assertFalse(breaks(Blocks.DIRT_PATH, soil));
        for (Block soil : new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
                Blocks.ROOTED_DIRT, Blocks.DIRT_PATH})
            assertFalse(breaks(Blocks.FARMLAND, soil));
        assertFalse(breaks(Blocks.GRASS_BLOCK, Blocks.DIRT));
        assertTrue(WrongBlockPolicy.shouldBreak(Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(), false, false));
        assertTrue(breaks(Blocks.DIRT, Blocks.GRASS_BLOCK));
        assertTrue(breaks(Blocks.FARMLAND, Blocks.STONE));
    }

    @Test void preservesStripPairsButNotUnrelatedLogs() {
        assertFalse(WrongBlockPolicy.shouldBreak(Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
                Blocks.SPRUCE_LOG.defaultBlockState(), true, true));
        assertTrue(breaks(Blocks.STRIPPED_SPRUCE_LOG, Blocks.OAK_LOG));
    }

    @Test void doesNotDestroySameTypeForAgeMoistureOrOrientationDifferences() {
        for (Block block : new Block[]{Blocks.WHEAT, Blocks.FARMLAND, Blocks.SPRUCE_STAIRS}) {
            var initial = block.defaultBlockState();
            for (var property : initial.getProperties()) {
                var changed = initial.cycle(property);
                assertFalse(WrongBlockPolicy.shouldBreak(initial, changed, true, false));
            }
        }
        assertFalse(breaks(Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM));
        assertFalse(breaks(Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM));
        assertTrue(breaks(Blocks.WHEAT, Blocks.CARROTS));
    }

    @Test void cancelsAQueuedOrActiveBreakAfterAnyTargetOrActualStateChange() {
        var target = Blocks.OAK_PLANKS.defaultBlockState();
        var actual = Blocks.STONE.defaultBlockState();
        assertTrue(WrongBlockPolicy.unchanged(target, actual, target, actual));
        for (var replacement : new Block[]{Blocks.AIR, Blocks.OAK_PLANKS, Blocks.COBBLESTONE})
            assertFalse(WrongBlockPolicy.unchanged(target, actual, target, replacement.defaultBlockState()));
        assertFalse(WrongBlockPolicy.unchanged(target, actual, Blocks.SPRUCE_PLANKS.defaultBlockState(), actual));
        var grass = Blocks.GRASS_BLOCK.defaultBlockState();
        assertFalse(WrongBlockPolicy.unchanged(target, grass, target,
                grass.setValue(BlockStateProperties.SNOWY, true)));
    }
}
