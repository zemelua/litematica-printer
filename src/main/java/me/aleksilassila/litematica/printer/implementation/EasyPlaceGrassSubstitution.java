package me.aleksilassila.litematica.printer.implementation;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** An operation-local target; never writes to the schematic or its material cache. */
public final class EasyPlaceGrassSubstitution {
    private EasyPlaceGrassSubstitution() {}

    public static BlockState placementTarget(BlockState schematic, boolean enabled,
            ItemStack mainHand, ItemStack offHand, boolean creative, Iterable<ItemStack> inventory) {
        if (!enabled || !schematic.is(Blocks.GRASS_BLOCK)) return schematic;
        if (mainHand.is(Items.GRASS_BLOCK)) return schematic;
        if (mainHand.is(Items.DIRT)) return Blocks.DIRT.defaultBlockState();
        if (offHand.is(Items.GRASS_BLOCK)) return schematic;
        if (offHand.is(Items.DIRT)) return Blocks.DIRT.defaultBlockState();
        // Creative retains normal pick-block unless the user explicitly holds dirt.
        if (creative) return schematic;
        boolean hasDirt = false;
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.GRASS_BLOCK)) return schematic;
            if (stack.is(Items.DIRT)) hasDirt = true;
        }
        return hasDirt ? Blocks.DIRT.defaultBlockState() : schematic;
    }
}
