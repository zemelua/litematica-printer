package me.aleksilassila.litematica.printer.implementation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Try the actual shape pieces, not only a bounding-box center which may be empty or occluded. */
public final class MiningHit {
    private MiningHit() {}
    public static BlockHitResult find(VoxelShape shape, BlockPos pos, Vec3 eye, double range,
                                     Function<Vec3, BlockHitResult> trace) {
        var points = new ArrayList<Vec3>();
        Vec3 offset = Vec3.atLowerCornerOf(pos);
        for (var box : shape.toAabbs()) {
            var c = box.getCenter();
            double e = 0.001;
            points.add(c.add(offset));
            points.add(new Vec3(box.minX + e, c.y, c.z).add(offset));
            points.add(new Vec3(box.maxX - e, c.y, c.z).add(offset));
            points.add(new Vec3(c.x, box.minY + e, c.z).add(offset));
            points.add(new Vec3(c.x, box.maxY - e, c.z).add(offset));
            points.add(new Vec3(c.x, c.y, box.minZ + e).add(offset));
            points.add(new Vec3(c.x, c.y, box.maxZ - e).add(offset));
        }
        points.sort(Comparator.comparingDouble(eye::distanceToSqr));
        for (var point : points) {
            var hit = trace.apply(point);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)
                    && eye.distanceToSqr(hit.getLocation()) <= range * range) return hit;
        }
        return null;
    }
}
