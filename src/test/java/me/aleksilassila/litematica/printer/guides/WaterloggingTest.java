package me.aleksilassila.litematica.printer.guides;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import me.aleksilassila.litematica.printer.guides.interaction.WaterloggingGuide;
import me.aleksilassila.litematica.printer.guides.placement.SlabGuide;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class WaterloggingTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @ParameterizedTest @EnumSource(value = SlabType.class, names = {"BOTTOM", "TOP"})
    void placesSlabFirstThenAddsWaterOnce(SlabType half) {
        var dry = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, half);
        var wet = dry.setValue(BlockStateProperties.WATERLOGGED, true);
        assertFalse(WaterloggingGuide.needsWater(wet, Blocks.AIR.defaultBlockState()));
        assertTrue(WaterloggingGuide.needsWater(wet, dry));
        assertTrue(((LiquidBlockContainer) dry.getBlock()).canPlaceLiquid(null, null, BlockPos.ZERO, dry, Fluids.WATER));
        assertFalse(WaterloggingGuide.needsWater(wet, wet));
        assertFalse(WaterloggingGuide.needsWater(dry, wet));
        assertFalse(WaterloggingGuide.needsWater(dry, dry));
    }

    static Stream<Block> waterloggableBlocks() {
        return Stream.of(Blocks.OAK_SLAB, Blocks.STONE_SLAB, Blocks.SPRUCE_STAIRS,
                Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR, Blocks.IRON_BARS);
    }

    @ParameterizedTest @MethodSource("waterloggableBlocks")
    void supportsOtherVanillaWaterloggableBlocksWithoutChangingTheirShape(Block block) {
        var dry = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
        var wet = dry.setValue(BlockStateProperties.WATERLOGGED, true);
        assertTrue(WaterloggingGuide.needsWater(wet, dry));
        assertTrue(((LiquidBlockContainer) block).canPlaceLiquid(null, null, BlockPos.ZERO, dry, Fluids.WATER));
        assertFalse(WaterloggingGuide.needsWater(wet, wet));
    }

    @Test void rejectsWrongHalfWrongBlockAndInvalidDoubleSlabs() {
        var bottom = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        var wet = bottom.setValue(BlockStateProperties.WATERLOGGED, true);
        assertFalse(WaterloggingGuide.needsWater(wet, bottom.setValue(SlabBlock.TYPE, SlabType.TOP)));
        assertFalse(WaterloggingGuide.needsWater(wet, Blocks.OAK_SLAB.defaultBlockState()));
        var doubled = bottom.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        assertFalse(WaterloggingGuide.needsWater(doubled.setValue(BlockStateProperties.WATERLOGGED, true), doubled));
        assertFalse(((LiquidBlockContainer) doubled.getBlock()).canPlaceLiquid(null, null, BlockPos.ZERO, doubled, Fluids.WATER));
    }

    @Test void staleOrObstructedTargetCannotCauseWaterPlacementOnAnotherBlock() {
        var wet = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        for (Block actual : new Block[]{Blocks.AIR, Blocks.STONE, Blocks.WATER, Blocks.LAVA,
                Blocks.DIRT, Blocks.SNOW, Blocks.SPRUCE_PLANKS})
            assertFalse(WaterloggingGuide.needsWater(wet, actual.defaultBlockState()));
        assertFalse(WaterloggingGuide.needsWater(Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()));
    }

    @Test void facingAndOtherPropertiesMustAlreadyMatchBeforeWaterIsAdded() {
        var dry = Blocks.SPRUCE_STAIRS.defaultBlockState();
        var wet = dry.setValue(BlockStateProperties.WATERLOGGED, true);
        var rotated = dry.setValue(BlockStateProperties.HORIZONTAL_FACING,
                dry.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        assertFalse(WaterloggingGuide.needsWater(wet, rotated));
    }

    @Test void dedicatedWaterloggingGuideRunsBeforeSlabPlacement() {
        var registered = new Guides().getGuides().stream().map(g -> g.getA()).toList();
        int water = registered.indexOf(WaterloggingGuide.class);
        assertTrue(water >= 0 && water < registered.indexOf(SlabGuide.class));
    }
}
