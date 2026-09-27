package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.actions.ClearSoilSnowAction;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class SnowMiningMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void printer$validateSnowMining(CallbackInfo ci) {
        ClearSoilSnowAction.validateTick((Minecraft) (Object) this);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void printer$mineSnow(boolean attacking, CallbackInfo ci) {
        if (ClearSoilSnowAction.continueMining((Minecraft) (Object) this, attacking)) ci.cancel();
    }
}
