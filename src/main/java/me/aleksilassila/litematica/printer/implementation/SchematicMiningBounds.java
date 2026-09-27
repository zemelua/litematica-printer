package me.aleksilassila.litematica.printer.implementation;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import net.minecraft.core.BlockPos;

/** Only actual enabled subregions, never the enclosing box or an entire touched chunk. */
public final class SchematicMiningBounds {
    private SchematicMiningBounds() {}

    public static boolean contains(BlockPos pos) {
        for (var placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            if (!placement.isEnabled() || !placement.isRenderingEnabled()) continue;
            for (var box : placement.getSubRegionBoxes(RequiredEnabled.RENDERING_ENABLED).values()) {
                if (contains(box, pos)) return true;
            }
        }
        return false;
    }

    public static boolean contains(Box box, BlockPos pos) {
        var a = box.getPos1();
        var b = box.getPos2();
        return a != null && b != null
                && pos.getX() >= Math.min(a.getX(), b.getX()) && pos.getX() <= Math.max(a.getX(), b.getX())
                && pos.getY() >= Math.min(a.getY(), b.getY()) && pos.getY() <= Math.max(a.getY(), b.getY())
                && pos.getZ() >= Math.min(a.getZ(), b.getZ()) && pos.getZ() <= Math.max(a.getZ(), b.getZ());
    }
}
