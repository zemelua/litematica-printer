package me.aleksilassila.litematica.printer.mixin;

import fi.dy.masa.litematica.util.EasyPlaceUtils;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.implementation.PlacementTargets;
import me.aleksilassila.litematica.printer.implementation.WrongBlockMining;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EasyPlaceUtils.class, remap = false)
public class EasyPlaceRewriteMixin {
    @Inject(method = "handleEasyPlace", at = @At("HEAD"), cancellable = true)
    private static void printer$repairWrongBlock(CallbackInfoReturnable<InteractionResult> cir) {
        if (WrongBlockMining.handleEasyPlace(Minecraft.getInstance())) cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Redirect(method = {"handleEasyPlace", "placementRestrictionInEffect"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            // One schematic read in placement; two real-world reads plus one schematic read in restriction.
            require = 4, allow = 4)
    private static BlockState printer$substituteTarget(Level level, BlockPos pos) {
        var original = level.getBlockState(pos);
        var player = Minecraft.getInstance().player;
        if (!(level instanceof WorldSchematic) || player == null) return original;
        return PlacementTargets.forPlayer(original, player.level().getBlockState(pos), player);
    }
}
