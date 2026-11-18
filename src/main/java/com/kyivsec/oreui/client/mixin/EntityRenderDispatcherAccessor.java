package com.kyivsec.oreui.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access only the dispatcher state temporarily changed by an inventory-style preview. */
@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @Accessor("cameraOrientation") Quaternionf oreui$getCameraOrientation();
    @Accessor("cameraOrientation") void oreui$setCameraOrientation(Quaternionf orientation);
    @Accessor("shouldRenderShadow") boolean oreui$getRenderShadow();
    @Accessor("shouldRenderShadow") void oreui$setRenderShadow(boolean enabled);
    @Accessor("renderHitBoxes") boolean oreui$getRenderHitBoxes();
    @Accessor("renderHitBoxes") void oreui$setRenderHitBoxes(boolean enabled);
}
