package me.aleksilassila.litematica.printer.implementation;

import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.actions.Action;
import me.aleksilassila.litematica.printer.actions.BlockMiningAction;
import me.aleksilassila.litematica.printer.actions.BreakWrongBlockAction;
import me.aleksilassila.litematica.printer.actions.PrepareAction;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.guides.interaction.LogStrippingGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WrongBlockMining {
    private WrongBlockMining() {}
    private static long nextDiagnostic;

    private static void diagnostic(SchematicBlockState state, String reason) {
        if (!Configs.PRINT_DEBUG.getBooleanValue()) return;
        long now = System.nanoTime();
        if (now < nextDiagnostic) return;
        nextDiagnostic = now + 3_000_000_000L;
        me.aleksilassila.litematica.printer.Printer.printDebug(
                "Mining {} at {} (actual {}, target {})", reason, state.blockPos, state.currentState, state.targetState);
    }

    public static boolean eligible(SchematicBlockState state) {
        if (!Configs.INTERACT_BLOCKS.getBooleanValue()) return false;
        boolean permitted;
        if (state.targetState.isAir()) {
            permitted = Configs.BREAK_EXTRA_BLOCKS.getBooleanValue()
                    && state.schematic.hasChunk(state.blockPos.getX() >> 4, state.blockPos.getZ() >> 4)
                    && WrongBlockPolicy.shouldBreakExtra(state.targetState, state.currentState,
                            SchematicMiningBounds.contains(state.blockPos));
        } else {
            permitted = Configs.BREAK_WRONG_BLOCKS.getBooleanValue()
                    && WrongBlockPolicy.shouldBreak(state.targetState, state.currentState,
                            Configs.EASY_PLACE_DIRT_FOR_GRASS.getBooleanValue(),
                            LogStrippingGuide.STRIPPED_BLOCKS.get(state.currentState.getBlock()) == state.targetState.getBlock());
        }
        return permitted && state.currentState.getDestroySpeed(state.world, state.blockPos) >= 0
                && state.world.getBlockEntity(state.blockPos) == null;
    }

    public static BlockHitResult hit(LocalPlayer player, SchematicBlockState state) {
        var shape = state.world.getBlockState(state.blockPos).getShape(state.world, state.blockPos);
        double range = Math.min(Configs.PRINTING_RANGE.getDoubleValue(), player.blockInteractionRange());
        return MiningHit.find(shape, state.blockPos, player.getEyePosition(), range,
                end -> state.world.clip(new ClipContext(player.getEyePosition(), end,
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)));
    }

    public static boolean usableTool(ItemStack stack, BlockState block, boolean creative) {
        return creative || ((!stack.isDamageableItem() || !stack.nextDamageWillBreak())
                && (!block.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(block)));
    }

    private static int toolSlot(LocalPlayer player, SchematicBlockState state) {
        var inventory = player.getInventory();
        boolean creative = player.getAbilities().instabuild;
        int best = -1;
        float bestSpeed = -1;
        // Retain the current item on ties; use the fastest suitable item otherwise.
        int selected = inventory.getSelectedSlot();
        var items = inventory.getNonEquipmentItems();
        for (int n = -1; n < items.size(); n++) {
            int slot = n == -1 ? selected : n;
            var item = items.get(slot);
            if (item.isEmpty() && slot >= 9) continue;
            if (!usableTool(item, state.currentState, creative)
                    || !item.canDestroyBlock(state.currentState, state.world, state.blockPos, player)) continue;
            float speed = item.getDestroySpeed(state.currentState);
            if (speed > bestSpeed) { best = slot; bestSpeed = speed; }
        }
        return best;
    }

    public static List<Action> actions(SchematicBlockState state, LocalPlayer player, boolean easyPlace) {
        if ((easyPlace && state.targetState.isAir()) || !eligible(state)) return List.of();
        var hit = hit(player, state);
        int slot = toolSlot(player, state);
        if (hit == null || slot < 0) {
            diagnostic(state, hit == null ? "waiting: no visible surface in reach" : "waiting: no usable mining tool");
            return List.of();
        }
        diagnostic(state, "queued");
        var stack = player.getInventory().getNonEquipmentItems().get(slot);
        // PrepareAction does not switch to an empty slot; do that via a non-destructive select action.
        Action prepare;
        if (stack.isEmpty()) {
            prepare = new Action() {
                @Override public void send(Minecraft client, LocalPlayer owner) {
                    if (owner == player && owner.getInventory().getNonEquipmentItems().get(slot).isEmpty())
                        owner.getInventory().setSelectedSlot(slot);
                }
            };
            // Only hotbar empty slots can be selected directly.
            if (slot >= 9) return List.of();
        } else {
            Vec3 delta = hit.getLocation().subtract(player.getEyePosition());
            var context = new PrinterPlacementContext(player, hit, stack.copy(), slot);
            prepare = new PrepareAction(context,
                    (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90,
                    (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        }
        return List.of(prepare, new BreakWrongBlockAction(state, stack.getItem(), easyPlace));
    }

    /** Invoked only by Litematica's Easy Place operation; the actual crosshair block must be wrong. */
    public static boolean handleEasyPlace(Minecraft client) {
        if (BlockMiningAction.isActive()) return true;
        var schematic = SchematicWorldHandler.getSchematicWorld();
        if (client.player == null || client.level == null || schematic == null || client.screen != null
                || !client.options.keyUse.isDown() || !Configs.BREAK_WRONG_BLOCKS.getBooleanValue()
                || !(client.hitResult instanceof BlockHitResult looking)
                || looking.getType() != HitResult.Type.BLOCK
                || !DataManager.getRenderLayerRange().isPositionWithinRange(looking.getBlockPos())) return false;
        if (LitematicaMixinMod.printer != null && !LitematicaMixinMod.printer.actionHandler.acceptsActions()) return true;
        var state = new SchematicBlockState(client.level, schematic, looking.getBlockPos());
        var actions = actions(state, client.player, true);
        if (actions.isEmpty()) return false;
        for (var action : actions) action.send(client, client.player);
        return BlockMiningAction.isActive();
    }
}
