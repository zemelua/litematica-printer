package me.aleksilassila.litematica.printer.guides;

import static org.junit.jupiter.api.Assertions.*;
import me.aleksilassila.litematica.printer.implementation.MiningHit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

class MiningHitTest {
    @Test void hitsSeparatedPiecesEvenWhenCombinedBoundsCenterIsEmpty() {
        var shape = Shapes.or(Shapes.box(0, 0, 0, .2, 1, 1), Shapes.box(.8, 0, 0, 1, 1, 1));
        var eye = new Vec3(.5, .5, -2);
        assertNotNull(MiningHit.find(shape, BlockPos.ZERO, eye, 4,
                end -> shape.clip(eye, end, BlockPos.ZERO)));
    }
    @Test void acceptsReachableSurfaceEvenWhenCenterIsBeyondReach() {
        var eye = new Vec3(.5, .5, -4.4);
        assertNotNull(MiningHit.find(Shapes.block(), BlockPos.ZERO, eye, 4.5,
                end -> Shapes.block().clip(eye, end, BlockPos.ZERO)));
        assertNull(MiningHit.find(Shapes.block(), BlockPos.ZERO, eye, 4.3,
                end -> Shapes.block().clip(eye, end, BlockPos.ZERO)));
    }
    @Test void neverMinesOccluderOrAcceptsMiss() {
        var eye = new Vec3(.5, .5, -2);
        assertNull(MiningHit.find(Shapes.block(), BlockPos.ZERO, eye, 4,
                end -> new BlockHitResult(new Vec3(.5, .5, -1), Direction.NORTH, new BlockPos(0, 0, -1), false)));
        assertNull(MiningHit.find(Shapes.block(), BlockPos.ZERO, eye, 4,
                end -> BlockHitResult.miss(end, Direction.NORTH, BlockPos.ZERO)));
    }
    @Test void findsExposedFaceWhenCenterIsOccluded() {
        var eye = new Vec3(.5, .5, -2);
        assertNotNull(MiningHit.find(Shapes.block(), BlockPos.ZERO, eye, 4,
                end -> end.x < .1 ? Shapes.block().clip(eye, end, BlockPos.ZERO) : null));
    }
}
