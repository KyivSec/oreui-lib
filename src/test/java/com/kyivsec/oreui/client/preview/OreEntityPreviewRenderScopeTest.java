package com.kyivsec.oreui.client.preview;

import com.kyivsec.oreui.client.mixin.EntityRenderDispatcherAccessor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreEntityPreviewRenderScopeTest {
    private static final class State implements EntityRenderDispatcherAccessor {
        Quaternionf camera = new Quaternionf().rotationY(0.7F);
        boolean shadow, hitBoxes;
        @Override public Quaternionf oreui$getCameraOrientation() { return camera; }
        @Override public void oreui$setCameraOrientation(Quaternionf value) { camera = value; }
        @Override public boolean oreui$getRenderShadow() { return shadow; }
        @Override public void oreui$setRenderShadow(boolean value) { shadow = value; }
        @Override public boolean oreui$getRenderHitBoxes() { return hitBoxes; }
        @Override public void oreui$setRenderHitBoxes(boolean value) { hitBoxes = value; }
    }

    @Test void accessorTargetsMatchMinecraft1211DispatcherFields() throws Exception {
        assertEquals(Quaternionf.class, EntityRenderDispatcher.class.getDeclaredField("cameraOrientation").getType());
        assertEquals(boolean.class, EntityRenderDispatcher.class.getDeclaredField("shouldRenderShadow").getType());
        assertEquals(boolean.class, EntityRenderDispatcher.class.getDeclaredField("renderHitBoxes").getType());
    }

    @Test void previewSuppressesWorldDecorationsAndRestoresEveryOriginalCombination() {
        for (boolean shadow : new boolean[] {false, true}) {
            for (boolean boxes : new boolean[] {false, true}) {
                State state = new State();
                state.shadow = shadow; state.hitBoxes = boxes;
                Quaternionf original = state.camera, requested = new Quaternionf().rotationX(0.2F);
                try (var ignored = new OreEntityPreviewRenderScope(state, requested)) {
                    assertFalse(state.shadow); assertFalse(state.hitBoxes);
                    assertEquals(requested, state.camera);
                    assertNotSame(requested, state.camera);
                    state.camera.rotateZ(1); // A renderer cannot mutate the caller's requested camera.
                }
                assertSame(original, state.camera);
                assertEquals(new Quaternionf().rotationX(0.2F), requested);
                assertEquals(shadow, state.shadow); assertEquals(boxes, state.hitBoxes);
            }
        }
    }

    @Test void exceptionRestoresDispatcherState() {
        State state = new State();
        state.shadow = state.hitBoxes = true;
        Quaternionf original = state.camera;
        assertThrows(IllegalStateException.class, () -> {
            try (var ignored = new OreEntityPreviewRenderScope(state, new Quaternionf())) {
                throw new IllegalStateException("Renderer failed");
            }
        });
        assertSame(original, state.camera);
        assertTrue(state.shadow); assertTrue(state.hitBoxes);
    }

    @Test void nestedPreviewsRestoreTheOuterPreviewBeforeRestoringTheWorld() {
        State state = new State();
        state.shadow = true;
        Quaternionf original = state.camera;
        try (var outer = new OreEntityPreviewRenderScope(state, new Quaternionf().rotationX(0.2F))) {
            Quaternionf outerCamera = state.camera;
            try (var inner = new OreEntityPreviewRenderScope(state, new Quaternionf().rotationX(0.8F))) {
                assertNotEquals(outerCamera, state.camera);
            }
            assertSame(outerCamera, state.camera);
            assertFalse(state.shadow);
        }
        assertSame(original, state.camera);
        assertTrue(state.shadow);
    }
}
