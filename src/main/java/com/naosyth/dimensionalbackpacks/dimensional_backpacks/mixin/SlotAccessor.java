package com.naosyth.dimensionalbackpacks.dimensional_backpacks.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the storage screen lay its slots out for the current view size. */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void dimensionalbackpacks$setX(int x);

    @Mutable
    @Accessor("y")
    void dimensionalbackpacks$setY(int y);
}
