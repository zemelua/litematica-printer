package me.aleksilassila.litematica.printer.actions;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.guides.interaction.SoilSnowGuide;
import me.aleksilassila.litematica.printer.guides.interaction.TillingGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

public class ClearSoilSnowAction extends BlockMiningAction {
    public ClearSoilSnowAction(SchematicBlockState state, Item shovel) {
        super(state, shovel, false);
    }

    @Override protected boolean targetValid(Minecraft client) {
        var fresh = new SchematicBlockState(state.world, state.schematic, state.blockPos);
        return SoilSnowGuide.enabled(fresh.targetState)
                && SoilSnowGuide.shouldClear(fresh.targetState, fresh.currentState,
                        state.world.getBlockState(state.blockPos.above()),
                        state.schematic.getBlockState(state.blockPos.above()))
                && (!fresh.targetState.is(Blocks.FARMLAND) || new TillingGuide(fresh).hasHoe(owner));
    }

    @Override protected BlockHitResult hit() { return SoilSnowGuide.snowHit(owner, state); }
}
