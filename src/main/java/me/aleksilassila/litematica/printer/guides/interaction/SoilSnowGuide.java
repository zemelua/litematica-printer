package me.aleksilassila.litematica.printer.guides.interaction;

import java.util.List;
import javax.annotation.Nonnull;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.actions.Action;
import me.aleksilassila.litematica.printer.actions.ClearSoilSnowAction;
import me.aleksilassila.litematica.printer.actions.PrepareAction;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Clears only accumulated snow that prevents a requested soil transformation. */
public class SoilSnowGuide extends InteractionGuide {
    public SoilSnowGuide(SchematicBlockState state) {
        super(state);
    }

    public static boolean shouldClear(BlockState target, BlockState current,
                                      BlockState above, BlockState schematicAbove) {
        if (!above.is(Blocks.SNOW)) return false;
        boolean path = target.is(Blocks.DIRT_PATH) && PathFlatteningGuide.isFlattenable(current);
        boolean farm = target.is(Blocks.FARMLAND) && TillingGuide.isTillable(current);
        // Farm schematics commonly include the crops as well as the soil.
        boolean plannedCrop = schematicAbove.getBlock() instanceof CropBlock
                || schematicAbove.getBlock() instanceof StemBlock;
        return (path && schematicAbove.isAir()) || (farm && (schematicAbove.isAir() || plannedCrop));
    }

    public static boolean enabled(BlockState target) {
        return Configs.CLEAR_SOIL_SNOW.getBooleanValue() && Configs.INTERACT_BLOCKS.getBooleanValue()
                && (!target.is(Blocks.DIRT_PATH) || Configs.MAKE_PATHS.getBooleanValue());
    }

    /** Recomputed at execution time so movement cannot turn this into remote mining. */
    public static BlockHitResult snowHit(LocalPlayer player, SchematicBlockState state) {
        var pos = state.blockPos.above();
        Vec3 end = Vec3.atBottomCenterOf(pos).add(0, 0.0625, 0);
        double reach = Math.min(Configs.PRINTING_RANGE.getDoubleValue(), player.blockInteractionRange());
        if (player.getEyePosition().distanceToSqr(end) > reach * reach) return null;
        var hit = state.world.clip(new ClipContext(player.getEyePosition(), end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) ? hit : null;
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        return enabled(targetState)
                && shouldClear(targetState, currentState, state.world.getBlockState(state.blockPos.above()),
                        state.schematic.getBlockState(state.blockPos.above()))
                && (!targetState.is(Blocks.FARMLAND) || new TillingGuide(state).hasHoe(player))
                && snowHit(player, state) != null && super.canExecute(player);
    }

    @Override
    public @Nonnull List<Action> execute(LocalPlayer player) {
        var hit = snowHit(player, state);
        var item = getRequiredItem(player).orElse(ItemStack.EMPTY);
        int slot = getRequiredItemStackSlot(player);
        if (hit == null || item.isEmpty() || slot < 0) return List.of();
        Vec3 delta = hit.getLocation().subtract(player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        var context = new PrinterPlacementContext(player, hit, item, slot);
        return List.of(new PrepareAction(context, yaw, pitch), new ClearSoilSnowAction(state, item.getItem()));
    }

    @Override
    protected @Nonnull List<ItemStack> getRequiredItems() {
        return PathFlatteningGuide.SHOVELS.stream().map(ItemStack::new).toList();
    }
}
