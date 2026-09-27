package me.aleksilassila.litematica.printer.actions;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.Hotkeys;
import me.aleksilassila.litematica.printer.guides.interaction.SoilSnowGuide;
import me.aleksilassila.litematica.printer.guides.interaction.TillingGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;

/** Owns vanilla mining across real client ticks, not the printer's ten scans per tick. */
public class ClearSoilSnowAction extends Action {
    private static ClearSoilSnowAction active;
    private final SchematicBlockState state;
    private final Item shovel;
    private LocalPlayer owner;
    private boolean started;
    private int age;

    public ClearSoilSnowAction(SchematicBlockState state, Item shovel) {
        this.state = state;
        this.shovel = shovel;
    }

    public static boolean isActive() {
        return active != null;
    }

    @Override
    public void send(Minecraft client, LocalPlayer player) {
        owner = player;
        if (active == null && valid(client)) active = this;
    }

    private boolean valid(Minecraft client) {
        if (client.player != owner || client.level != state.world || client.gameMode == null
                || client.screen != null || !client.isWindowActive() || client.isPaused()
                || owner == null || !owner.isAlive() || owner.isSpectator() || !owner.getAbilities().mayBuild
                || owner.isUsingItem() || client.options.keyAttack.isDown() || client.options.keyUse.isDown()
                || !owner.getMainHandItem().is(shovel)
                || (!Configs.PRINT_MODE.getBooleanValue() && !Hotkeys.PRINT.getKeybind().isPressed())
                || SchematicWorldHandler.getSchematicWorld() != state.schematic
                || !DataManager.getRenderLayerRange().isPositionWithinRange(state.blockPos)) return false;
        var fresh = new SchematicBlockState(state.world, state.schematic, state.blockPos);
        return SoilSnowGuide.enabled(fresh.targetState)
                && SoilSnowGuide.shouldClear(fresh.targetState, fresh.currentState,
                        state.world.getBlockState(state.blockPos.above()),
                        state.schematic.getBlockState(state.blockPos.above()))
                && (!fresh.targetState.is(Blocks.FARMLAND) || new TillingGuide(fresh).hasHoe(owner))
                && SoilSnowGuide.snowHit(owner, fresh) != null;
    }

    /** Runs even when a screen/disconnect prevents continueAttack from being called. */
    public static void validateTick(Minecraft client) {
        if (active != null && (++active.age > 200 || !active.valid(client))) cancel(client);
    }

    /** Called once per vanilla input tick. Returning true suppresses only vanilla idle cancellation. */
    public static boolean continueMining(Minecraft client, boolean attacking) {
        if (active == null) return false;
        if (attacking || !active.valid(client)) {
            cancel(client);
            return false;
        }
        var job = active;
        var hit = SoilSnowGuide.snowHit(job.owner, job.state);
        if (hit == null) {
            cancel(client);
            return false;
        }
        // Let vanilla handle destroy speed, enchantments, fatigue, creative, durability and packets.
        boolean accepted = job.started
                ? client.gameMode.continueDestroyBlock(hit.getBlockPos(), hit.getDirection())
                : client.gameMode.startDestroyBlock(hit.getBlockPos(), hit.getDirection());
        job.started = true;
        if (accepted) job.owner.swing(InteractionHand.MAIN_HAND);
        if (!accepted || !job.state.world.getBlockState(hit.getBlockPos()).is(Blocks.SNOW)) cancel(client);
        return true;
    }

    private static void cancel(Minecraft client) {
        var job = active;
        active = null;
        if (job != null && job.started && client.gameMode != null
                && client.player == job.owner && client.level == job.state.world) {
            client.gameMode.stopDestroyBlock();
        }
    }
}
