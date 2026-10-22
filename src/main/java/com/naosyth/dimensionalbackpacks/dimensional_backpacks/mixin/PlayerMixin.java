package com.naosyth.dimensionalbackpacks.dimensional_backpacks.mixin;

import com.naosyth.dimensionalbackpacks.dimensional_backpacks.plot.HotspotSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    // Every menu's stillValid distance check goes through this method.
    @Inject(method = "canInteractWithBlock", at = @At("HEAD"), cancellable = true)
    private void dimensionalbackpacks$allowHotspotMenus(BlockPos pos, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (HotspotSessions.allowsInteraction((Player) (Object) this, pos)) {
            cir.setReturnValue(true);
        }
    }
}
