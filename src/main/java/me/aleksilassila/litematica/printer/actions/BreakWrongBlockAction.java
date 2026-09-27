package me.aleksilassila.litematica.printer.actions;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.implementation.WrongBlockMining;
import me.aleksilassila.litematica.printer.implementation.WrongBlockPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BreakWrongBlockAction extends BlockMiningAction {
    private final BlockState originalTarget;

    public BreakWrongBlockAction(SchematicBlockState state, Item tool, boolean easyPlace) {
        super(state, tool, easyPlace);
        originalTarget = state.schematic.getBlockState(state.blockPos);
    }

    @Override protected int timeoutTicks() { return 1200; }

    @Override protected boolean targetValid(Minecraft client) {
        var fresh = new SchematicBlockState(state.world, state.schematic, state.blockPos);
        return WrongBlockPolicy.unchanged(originalTarget, state.currentState,
                        state.schematic.getBlockState(state.blockPos), fresh.currentState)
                && WrongBlockMining.eligible(fresh)
                && WrongBlockMining.usableTool(owner.getMainHandItem(), fresh.currentState, owner.getAbilities().instabuild)
                && owner.getMainHandItem().canDestroyBlock(fresh.currentState, state.world, state.blockPos, owner);
    }

    @Override protected BlockHitResult hit() { return WrongBlockMining.hit(owner, state); }
}
