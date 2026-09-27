package me.aleksilassila.litematica.printer.guides.interaction;

import java.util.List;
import javax.annotation.Nonnull;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.actions.Action;
import me.aleksilassila.litematica.printer.actions.PrepareAction;
import me.aleksilassila.litematica.printer.actions.WaterlogAction;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.guides.Guide;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WaterloggingGuide extends Guide {
    public WaterloggingGuide(SchematicBlockState state) { super(state); }

    public static boolean needsWater(BlockState target, BlockState current) {
        var waterlogged = BlockStateProperties.WATERLOGGED;
        return target.getBlock() == current.getBlock()
                && current.getBlock() instanceof LiquidBlockContainer
                && target.hasProperty(waterlogged) && current.hasProperty(waterlogged)
                && target.getValue(waterlogged) && !current.getValue(waterlogged)
                && (!current.hasProperty(SlabBlock.TYPE) || current.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
                && current.setValue(waterlogged, true).equals(target);
    }

    public static BlockHitResult hit(LocalPlayer player, SchematicBlockState state) {
        var shape = state.world.getBlockState(state.blockPos).getShape(state.world, state.blockPos);
        if (shape.isEmpty()) return null;
        Vec3 end = shape.bounds().getCenter().add(Vec3.atLowerCornerOf(state.blockPos));
        double reach = Math.min(Configs.PRINTING_RANGE.getDoubleValue(), player.blockInteractionRange());
        if (player.getEyePosition().distanceToSqr(end) > reach * reach) return null;
        var hit = state.world.clip(new ClipContext(player.getEyePosition(), end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(state.blockPos) ? hit : null;
    }

    @Override public boolean canExecute(LocalPlayer player) {
        // Guide's default equality intentionally ignores WATERLOGGED for initial block placement.
        return Configs.WATERLOG_BLOCKS.getBooleanValue() && Configs.INTERACT_BLOCKS.getBooleanValue()
                && needsWater(targetState, currentState) && playerHasRightItem(player)
                && !state.world.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, state.blockPos)
                && ((LiquidBlockContainer) currentState.getBlock()).canPlaceLiquid(player, state.world,
                        state.blockPos, currentState, Fluids.WATER)
                && hit(player, state) != null;
    }

    public PrepareAction preparation(LocalPlayer player, BlockHitResult hit) {
        return new PrepareAction(new PrinterPlacementContext(player, hit,
                new ItemStack(Items.WATER_BUCKET), getRequiredItemStackSlot(player)));
    }

    @Override public @Nonnull List<Action> execute(LocalPlayer player) {
        return List.of(new WaterlogAction(state));
    }

    @Override protected @Nonnull List<ItemStack> getRequiredItems() {
        return List.of(new ItemStack(Items.WATER_BUCKET));
    }
}
