package me.aleksilassila.litematica.printer.guides.interaction;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.guides.placement.FarmlandGuide;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class TillingGuide extends InteractionGuide {
    public static final Item[] HOE_ITEMS = new Item[]{
            Items.NETHERITE_HOE,
            Items.DIAMOND_HOE,
            Items.GOLDEN_HOE,
            Items.IRON_HOE,
            Items.COPPER_HOE,
            Items.STONE_HOE,
            Items.WOODEN_HOE
    };

    public TillingGuide(SchematicBlockState state) {
        super(state);
    }

    @Override
    public boolean canExecute(LocalPlayer player) {
        if (!super.canExecute(player))
            return false;

        return canTill(targetState, currentState, state.world.getBlockState(state.blockPos.above()));
    }

    public boolean hasHoe(LocalPlayer player) {
        return playerHasRightItem(player);
    }

    public static boolean isTillable(BlockState current) {
        return Arrays.stream(FarmlandGuide.TILLABLE_BLOCKS).anyMatch(b -> b == current.getBlock());
    }

    public static boolean canTill(BlockState target, BlockState current, BlockState above) {
        // Rooted dirt can be unrooted under an obstruction, but the final till still needs air.
        return target.is(Blocks.FARMLAND) && isTillable(current) && above.isAir();
    }

    @Override
    protected @Nonnull List<ItemStack> getRequiredItems() {
        return Arrays.stream(HOE_ITEMS).map(ItemStack::new).toList();
    }
}
