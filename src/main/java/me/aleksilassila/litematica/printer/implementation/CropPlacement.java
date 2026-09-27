package me.aleksilassila.litematica.printer.implementation;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Ignore only age on supported single-block crops, retaining block type and facing. */
public final class CropPlacement {
    private CropPlacement() {}

    public static boolean supported(BlockState state) {
        return state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES)
                || state.is(Blocks.BEETROOTS) || state.is(Blocks.MELON_STEM)
                || state.is(Blocks.PUMPKIN_STEM) || state.is(Blocks.NETHER_WART)
                || state.is(Blocks.COCOA) || state.is(Blocks.SWEET_BERRY_BUSH);
    }

    public static BlockState young(BlockState state) {
        if (supported(state)) {
            for (var property : state.getProperties()) {
                if (property instanceof IntegerProperty age && age.getName().equals("age"))
                    return state.setValue(age, 0);
            }
        }
        return state;
    }

    public static BlockState placementTarget(BlockState schematic, BlockState actual, boolean enabled) {
        if (!enabled || !supported(schematic)) return schematic;
        var young = young(schematic);
        // Preserve the actual growth stage so ordinary equality treats this position as complete.
        return young.equals(young(actual)) ? actual : young;
    }
}
