package me.aleksilassila.litematica.printer.implementation;

import java.util.List;
import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared by Printer and both Easy Place implementations; never changes the saved schematic. */
public final class PlacementTargets {
    private PlacementTargets() {}

    public static BlockState forPlayer(BlockState schematic, BlockState actual, LocalPlayer player) {
        return resolve(schematic, actual, Configs.EASY_PLACE_DIRT_FOR_GRASS.getBooleanValue(),
                Configs.IGNORE_CROP_AGE.getBooleanValue(),
                player == null ? ItemStack.EMPTY : player.getMainHandItem(),
                player == null ? ItemStack.EMPTY : player.getOffhandItem(),
                player != null && player.getAbilities().instabuild,
                player == null ? List.of() : player.getInventory().getNonEquipmentItems());
    }

    public static BlockState resolve(BlockState schematic, BlockState actual, boolean dirtForGrass,
            boolean ignoreCropAge, ItemStack main, ItemStack off, boolean creative, Iterable<ItemStack> inventory) {
        if (dirtForGrass && schematic.is(Blocks.GRASS_BLOCK)
                && (actual.is(Blocks.DIRT) || actual.is(Blocks.GRASS_BLOCK))) return actual;
        var target = EasyPlaceGrassSubstitution.placementTarget(schematic, dirtForGrass,
                main, off, creative, inventory);
        return CropPlacement.placementTarget(target, actual, ignoreCropAge);
    }
}
