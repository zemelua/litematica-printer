package me.aleksilassila.litematica.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.implementation.CropPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SchematicBlockState {
    public final Level world;
    public final WorldSchematic schematic;
    public final BlockPos blockPos;
    public final BlockState targetState;
    public final BlockState currentState;

    public SchematicBlockState(Level world, WorldSchematic schematic, BlockPos blockPos) {
        this.world = world;
        this.schematic = schematic;
        this.blockPos = blockPos;
        this.currentState = world.getBlockState(blockPos);
        this.targetState = CropPlacement.placementTarget(schematic.getBlockState(blockPos), currentState,
                Configs.IGNORE_CROP_AGE.getBooleanValue());
    }

    public SchematicBlockState offset(Direction direction) {
        return new SchematicBlockState(world, schematic, blockPos.relative(direction));
    }

    @Override
    public String toString() {
        return "SchematicBlockState{" +
                "world=" + world +
                ", schematic=" + schematic +
                ", blockPos=" + blockPos +
                ", targetState=" + targetState +
                ", currentState=" + currentState +
                '}';
    }
}
