package me.aleksilassila.litematica.printer.guides;

import static org.junit.jupiter.api.Assertions.*;
import fi.dy.masa.litematica.selection.Box;
import me.aleksilassila.litematica.printer.implementation.SchematicMiningBounds;
import me.aleksilassila.litematica.printer.implementation.WrongBlockPolicy;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExtraBlockTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void extraModeIncludesPlantsAndSnowButOnlyInsideAirRegions() {
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.STONE, Blocks.DIRT,
                Blocks.SPRUCE_PLANKS, Blocks.SHORT_GRASS, Blocks.SNOW}) {
            var current = block.defaultBlockState();
            assertTrue(WrongBlockPolicy.shouldBreakExtra(Blocks.AIR.defaultBlockState(), current, true));
            assertTrue(WrongBlockPolicy.shouldBreakExtra(Blocks.CAVE_AIR.defaultBlockState(), current, true));
            assertFalse(WrongBlockPolicy.shouldBreakExtra(Blocks.AIR.defaultBlockState(), current, false));
            assertFalse(WrongBlockPolicy.shouldBreakExtra(Blocks.GLASS.defaultBlockState(), current, true));
        }
    }
    @Test void protectsFluidsAirAndBlockEntities() {
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.AIR, Blocks.WATER,
                Blocks.LAVA, Blocks.CHEST, Blocks.FURNACE, Blocks.SPRUCE_SIGN})
            assertFalse(WrongBlockPolicy.shouldBreakExtra(Blocks.AIR.defaultBlockState(), block.defaultBlockState(), true));
    }
    @Test void inclusiveBoundsHandleReversedCornersAndNegativeCoordinates() {
        var a = new BlockPos(-7, 60, -3); var b = new BlockPos(-2, 63, 4);
        for (var box : new Box[]{new Box(a, b, "test"), new Box(b, a, "test")}) {
            assertTrue(SchematicMiningBounds.contains(box, a));
            assertTrue(SchematicMiningBounds.contains(box, b));
            assertTrue(SchematicMiningBounds.contains(box, new BlockPos(-5, 62, 0)));
            for (var p : new BlockPos[]{a.west(), a.below(), a.north(), b.east(), b.above(), b.south()})
                assertFalse(SchematicMiningBounds.contains(box, p));
        }
    }
    @Test void disjointSubregionsDoNotIncludeTheGapOrRemainderOfChunk() {
        var a = new Box(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2), "a");
        var b = new Box(new BlockPos(8, 0, 0), new BlockPos(10, 2, 2), "b");
        for (var p : new BlockPos[]{new BlockPos(5, 1, 1), new BlockPos(15, 1, 1)}) {
            assertFalse(SchematicMiningBounds.contains(a, p));
            assertFalse(SchematicMiningBounds.contains(b, p));
        }
        assertFalse(SchematicMiningBounds.contains(new Box(null, null, "incomplete"), BlockPos.ZERO));
    }
}
