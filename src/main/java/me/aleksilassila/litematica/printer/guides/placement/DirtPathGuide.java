package me.aleksilassila.litematica.printer.guides.placement;

import java.util.List;
import javax.annotation.Nonnull;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.guides.interaction.PathFlatteningGuide;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Places ordinary dirt first; PathFlatteningGuide handles the subsequent scan. */
public class DirtPathGuide extends GeneralPlacementGuide {
    public DirtPathGuide(SchematicBlockState state) {
        super(state);
    }

    public static boolean canPrepare(BlockState target, BlockState current, BlockState above) {
        return target.is(Blocks.DIRT_PATH) && current.canBeReplaced() && above.isAir();
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        return Configs.MAKE_PATHS.getBooleanValue()
                && canPrepare(targetState, currentState, state.world.getBlockState(state.blockPos.above()))
                && new PathFlatteningGuide(state).hasShovel(player)
                && super.canExecute(player);
    }

    @Override
    protected @Nonnull List<ItemStack> getRequiredItems() {
        // No dependency on the schematic pick-block cache mapping paths to an item.
        return List.of(new ItemStack(Items.DIRT));
    }

    @Override
    public boolean skipOtherGuides() {
        // Do not let generic placement leave unfinished dirt when disabled or missing a shovel.
        return true;
    }
}
