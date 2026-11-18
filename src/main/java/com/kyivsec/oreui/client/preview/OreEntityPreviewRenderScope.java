package com.kyivsec.oreui.client.preview;

import com.kyivsec.oreui.client.mixin.EntityRenderDispatcherAccessor;
import org.joml.Quaternionf;

/** Internal, nestable dispatcher state guard. Never assumes world shadows/debug boxes were enabled. */
public final class OreEntityPreviewRenderScope implements AutoCloseable {
    private final EntityRenderDispatcherAccessor state;
    private final Quaternionf camera;
    private final boolean shadow, hitBoxes;

    public OreEntityPreviewRenderScope(EntityRenderDispatcherAccessor state, Quaternionf camera) {
        this.state = state;
        this.camera = state.oreui$getCameraOrientation();
        shadow = state.oreui$getRenderShadow();
        hitBoxes = state.oreui$getRenderHitBoxes();
        state.oreui$setCameraOrientation(new Quaternionf(camera));
        state.oreui$setRenderShadow(false);
        state.oreui$setRenderHitBoxes(false);
    }

    @Override public void close() {
        state.oreui$setCameraOrientation(camera);
        state.oreui$setRenderShadow(shadow);
        state.oreui$setRenderHitBoxes(hitBoxes);
    }
}
