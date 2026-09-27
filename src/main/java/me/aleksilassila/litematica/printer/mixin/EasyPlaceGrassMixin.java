package me.aleksilassila.litematica.printer.mixin;

import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.implementation.EasyPlaceGrassSubstitution;
import me.aleksilassila.litematica.printer.implementation.CropPlacement;
import me.aleksilassila.litematica.printer.implementation.WrongBlockMining;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WorldUtils.class, remap = false)
public class EasyPlaceGrassMixin {
    @Inject(method = "doEasyPlaceAction", at = @At("HEAD"), cancellable = true)
    private static void printer$repairWrongBlock(Minecraft client, CallbackInfoReturnable<InteractionResult> cir) {
        if (WrongBlockMining.handleEasyPlace(client)) cir.setReturnValue(InteractionResult.SUCCESS);
    }
    // Both the automatic pick/placement and its placement restriction must see the same target.
    // Leave rendering, verification, material lists and ordinary schematic pick-block unchanged.
    @Redirect(method = {"doEasyPlaceAction", "placementRestrictionInEffect"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 2, allow = 2)
    private static BlockState printer$allowDirtForGrass(Level level, BlockPos pos) {
        BlockState original = level.getBlockState(pos);
        var player = Minecraft.getInstance().player;
        if (!(level instanceof WorldSchematic) || player == null) return original;
        boolean enabled = Configs.EASY_PLACE_DIRT_FOR_GRASS.getBooleanValue()
                && fi.dy.masa.litematica.config.Configs.Generic.EASY_PLACE_MODE.getBooleanValue();
        var target = EasyPlaceGrassSubstitution.placementTarget(original, enabled,
                player.getMainHandItem(), player.getOffhandItem(), player.getAbilities().instabuild,
                player.getInventory().getNonEquipmentItems());
        return CropPlacement.placementTarget(target, player.level().getBlockState(pos),
                Configs.IGNORE_CROP_AGE.getBooleanValue()
                        && fi.dy.masa.litematica.config.Configs.Generic.EASY_PLACE_MODE.getBooleanValue());
    }
}
