package me.aleksilassila.litematica.printer.actions;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.Hotkeys;
import me.aleksilassila.litematica.printer.guides.interaction.WaterloggingGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Buckets ray-trace in Item.use; an ordinary useItemOn hit alone cannot aim a bucket. */
public final class WaterlogAction extends Action {
    private final SchematicBlockState state;

    public WaterlogAction(SchematicBlockState state) { this.state = state; }

    @Override public void send(Minecraft client, LocalPlayer player) {
        if (client.player != player || client.level != state.world || client.gameMode == null
                || client.screen != null || !client.isWindowActive() || player.isUsingItem()
                || !player.isAlive() || player.isSpectator() || !player.getAbilities().mayBuild
                || (!Configs.PRINT_MODE.getBooleanValue() && !Hotkeys.PRINT.getKeybind().isPressed())
                || SchematicWorldHandler.getSchematicWorld() != state.schematic
                || !DataManager.getRenderLayerRange().isPositionWithinRange(state.blockPos)) return;
        var fresh = new SchematicBlockState(state.world, state.schematic, state.blockPos);
        if (!fresh.targetState.equals(state.targetState)) return;
        var guide = new WaterloggingGuide(fresh);
        if (!guide.canExecute(player)) return;
        var hit = WaterloggingGuide.hit(player, fresh);
        if (hit == null) return;
        guide.preparation(player, hit).send(client, player);
        if (!player.getMainHandItem().is(Items.WATER_BUCKET)) return;
        Vec3 delta = hit.getLocation().subtract(player.getEyePosition());
        float oldYaw = player.getYRot(), oldPitch = player.getXRot();
        try {
            player.setYRot((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
            player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
            // Match the exact ray used by BucketItem, including float-angle rounding and full reach.
            var end = player.getEyePosition().add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
            var actualHit = state.world.clip(new ClipContext(player.getEyePosition(), end,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (actualHit.getType() != HitResult.Type.BLOCK || !actualHit.getBlockPos().equals(state.blockPos)) return;
            // Exactly once: a second use with the resulting empty bucket would pick the water back up.
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        } finally {
            player.setYRot(oldYaw);
            player.setXRot(oldPitch);
        }
    }
}
