package me.aleksilassila.litematica.printer.implementation;

import me.aleksilassila.litematica.printer.guides.interaction.PathFlatteningGuide;
import me.aleksilassila.litematica.printer.guides.interaction.TillingGuide;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class WrongBlockPolicy {
    private WrongBlockPolicy() {}

    public static boolean shouldBreak(BlockState target, BlockState current,
                                      boolean allowDirtForGrass, boolean strippingPair) {
        // Air is deliberately never a removal target. State-only differences use existing guides.
        if (target.isAir() || current.isAir() || target.getBlock() == current.getBlock()
                || current.canBeReplaced() || current.getBlock() instanceof LiquidBlock
                || current.hasBlockEntity()) return false;
        if (allowDirtForGrass && target.is(Blocks.GRASS_BLOCK) && current.is(Blocks.DIRT)) return false;
        if (target.is(Blocks.DIRT_PATH) && PathFlatteningGuide.isFlattenable(current)) return false;
        if (target.is(Blocks.FARMLAND) && TillingGuide.isTillable(current)) return false;
        if (strippingPair) return false;
        // Fruiting naturally changes the stem's block type; do not repeatedly destroy healthy stems.
        if ((target.is(Blocks.MELON_STEM) && current.is(Blocks.ATTACHED_MELON_STEM))
                || (target.is(Blocks.PUMPKIN_STEM) && current.is(Blocks.ATTACHED_PUMPKIN_STEM))) return false;
        return true;
    }

    public static boolean shouldBreakExtra(BlockState target, BlockState current, boolean insideRegion) {
        // Replaceable plants and snow are extras too; fluids and block entities remain protected.
        return insideRegion && target.isAir() && !current.isAir()
                && !(current.getBlock() instanceof LiquidBlock) && !current.hasBlockEntity();
    }

    public static boolean unchanged(BlockState initialTarget, BlockState initialActual,
                                     BlockState targetNow, BlockState actualNow) {
        return initialTarget.equals(targetNow) && initialActual.equals(actualNow);
    }
}
