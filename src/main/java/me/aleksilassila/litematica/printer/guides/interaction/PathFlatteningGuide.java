package me.aleksilassila.litematica.printer.guides.interaction;

import java.util.List;
import javax.annotation.Nonnull;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Uses the normal upward-facing block interaction, after the server confirms the soil exists. */
public class PathFlatteningGuide extends InteractionGuide {
    private static final List<Item> SHOVELS = List.of(
            Items.NETHERITE_SHOVEL, Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL,
            Items.COPPER_SHOVEL, Items.GOLDEN_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL);

    public PathFlatteningGuide(SchematicBlockState state) {
        super(state);
    }

    public static boolean isFlattenable(BlockState block) {
        return block.is(Blocks.DIRT) || block.is(Blocks.GRASS_BLOCK)
                || block.is(Blocks.COARSE_DIRT) || block.is(Blocks.ROOTED_DIRT)
                || block.is(Blocks.PODZOL) || block.is(Blocks.MYCELIUM);
    }

    public boolean hasShovel(LocalPlayer player) {
        return playerHasRightItem(player);
    }

    public static boolean canFlatten(BlockState target, BlockState current, BlockState above) {
        return target.is(Blocks.DIRT_PATH) && isFlattenable(current) && above.isAir();
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        return Configs.MAKE_PATHS.getBooleanValue()
                // Vanilla shovel flattening requires actual air, not replaceable snow/grass.
                && canFlatten(targetState, currentState, state.world.getBlockState(state.blockPos.above()))
                && super.canExecute(player);
    }

    @Override
    protected @Nonnull List<ItemStack> getRequiredItems() {
        return SHOVELS.stream().map(ItemStack::new).toList();
    }
}
