package me.aleksilassila.litematica.printer.actions;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.Hotkeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;

/** One vanilla mining operation shared by snow clearing, Printer and Easy Place. */
public abstract class BlockMiningAction extends Action {
    private static BlockMiningAction active;
    protected final SchematicBlockState state;
    protected LocalPlayer owner;
    private final Item tool;
    private final boolean easyPlace;
    private boolean started;
    private int age;

    protected BlockMiningAction(SchematicBlockState state, Item tool, boolean easyPlace) {
        this.state = state;
        this.tool = tool;
        this.easyPlace = easyPlace;
    }

    public static boolean isActive() { return active != null; }

    @Override public final void send(Minecraft client, LocalPlayer player) {
        owner = player;
        if (active == null && valid(client)) active = this;
    }

    protected int timeoutTicks() { return 200; }
    protected abstract boolean targetValid(Minecraft client);
    protected abstract BlockHitResult hit();

    private boolean valid(Minecraft client) {
        if (client.player != owner || client.level != state.world || client.gameMode == null
                || client.screen != null || !client.isWindowActive() || client.isPaused()
                || owner == null || !owner.isAlive() || owner.isSpectator() || !owner.getAbilities().mayBuild
                || owner.isUsingItem() || client.options.keyAttack.isDown()
                || owner.getMainHandItem().getItem() != tool
                || SchematicWorldHandler.getSchematicWorld() != state.schematic
                || !DataManager.getRenderLayerRange().isPositionWithinRange(state.blockPos)) return false;
        if (easyPlace) {
            if (!fi.dy.masa.litematica.config.Configs.Generic.EASY_PLACE_MODE.getBooleanValue()
                    || !client.options.keyUse.isDown()
                    || !(client.hitResult instanceof BlockHitResult looking)
                    || !looking.getBlockPos().equals(state.blockPos)) return false;
        } else if (client.options.keyUse.isDown()
                || (!Configs.PRINT_MODE.getBooleanValue() && !Hotkeys.PRINT.getKeybind().isPressed())) return false;
        return targetValid(client) && hit() != null;
    }

    public static void validateTick(Minecraft client) {
        if (active != null && (++active.age > active.timeoutTicks() || !active.valid(client))) cancel(client);
    }

    public static boolean continueMining(Minecraft client, boolean attacking) {
        if (active == null) return false;
        if (attacking || !active.valid(client)) {
            cancel(client);
            return false;
        }
        var job = active;
        var hit = job.hit();
        if (hit == null) {
            cancel(client);
            return false;
        }
        boolean accepted = job.started
                ? client.gameMode.continueDestroyBlock(hit.getBlockPos(), hit.getDirection())
                : client.gameMode.startDestroyBlock(hit.getBlockPos(), hit.getDirection());
        job.started = true;
        if (accepted) job.owner.swing(InteractionHand.MAIN_HAND);
        if (!accepted || !job.targetValid(client)) cancel(client);
        return true;
    }

    private static void cancel(Minecraft client) {
        var job = active;
        active = null;
        if (job != null && job.started && client.gameMode != null
                && client.player == job.owner && client.level == job.state.world)
            client.gameMode.stopDestroyBlock();
    }
}
