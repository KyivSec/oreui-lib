package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.host.OreWidgetSupport;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class OrePreviewInteractionTest {
    private static final class Preview extends OreModelPreview {
        Preview() {
            this(new AtomicLong());
        }
        Preview(AtomicLong time) {
            super(Component.literal("Preview"), 240, time::get);
            setPosition(10, 20);
            setWidth(200);
            setRotation(-8, -25, 0);
        }
        @Override protected float pixelsPerUnit() { return 1; }
        @Override protected void renderModel(GuiGraphics graphics, int x, int y, float partial) {}
        void frame() { renderWidget(null, 0, 0, 0); }
    }

    @Test void interactionIsOptInAndStartsOnlyInsideThePreview() {
        Preview preview = new Preview();
        assertFalse(preview.mouseClicked(100, 100, 0));
        assertFalse(preview.mouseScrolled(100, 100, 0, 1));
        preview.setInteractive(true);
        assertFalse(preview.mouseClicked(9, 100, 0));
        assertFalse(preview.mouseClicked(210, 100, 0));
        assertFalse(preview.mouseClicked(100, 100, 1));
        assertFalse(preview.mouseScrolled(300, 100, 0, 1));
        assertTrue(preview.mouseClicked(100, 100, 0));
        assertTrue(preview.isOrbitDragging());
    }

    @Test void capturedRotationContinuesOutsideAndStopsOnRelease() {
        Preview preview = new Preview();
        preview.setInteractive(true);
        preview.mouseClicked(100, 100, 0);
        assertTrue(preview.mouseDragged(500, 500, 0, 50, 20));
        assertEquals(5, preview.getYaw(), 1e-4);
        assertEquals(4, preview.getPitch(), 1e-4);
        assertTrue(preview.mouseReleased(500, 500, 0));
        assertFalse(preview.mouseDragged(500, 500, 0, 50, 20));
        assertFalse(preview.isOrbitDragging());
    }

    @Test void tiltAndZoomAreBoundedAndInvalidDeltasAreIgnored() {
        Preview preview = new Preview();
        preview.setInteractive(true);
        preview.mouseClicked(100, 100, 0);
        preview.mouseDragged(100, 100, 0, 10000, 10000);
        assertEquals(60, preview.getPitch());
        assertTrue(preview.getYaw() >= -180 && preview.getYaw() < 180);
        preview.mouseDragged(100, 100, 0, 0, -10000);
        assertEquals(-60, preview.getPitch());
        assertFalse(preview.mouseDragged(100, 100, 0, Double.NaN, 1));
        assertFalse(preview.mouseScrolled(100, 100, 0, Double.POSITIVE_INFINITY));
        preview.mouseScrolled(110, 140, 0, 10000);
        assertEquals(4, preview.getModelScale());
        preview.mouseScrolled(110, 140, 0, -10000);
        assertEquals(0.75, preview.getModelScale());
    }

    @Test void zoomKeepsThePointUnderTheCursorStationary() {
        Preview preview = new Preview();
        preview.setInteractive(true);
        preview.mouseScrolled(125, 115, 0, 2);
        assertEquals(125, 110 + preview.getModelOffsetX() + 15 * preview.getModelScale(), 1e-4);
        assertEquals(115, 140 + preview.getModelOffsetY() - 25 * preview.getModelScale(), 1e-4);
        preview.mouseScrolled(125, 115, 0, -2);
        assertEquals(1, preview.getModelScale(), 1e-4);
        assertEquals(0, preview.getModelOffsetX(), 1e-4);
        assertEquals(0, preview.getModelOffsetY(), 1e-4);
    }

    @Test void focusPresetsPreserveOrientationAndResetRestoresOnlyTheCamera() {
        Preview preview = new Preview();
        preview.setInteractive(true);
        preview.mouseClicked(100, 100, 0);
        preview.mouseDragged(100, 100, 0, 30, 10);
        float yaw = preview.getYaw();
        preview.focusView(0.75F, 3);
        assertEquals(yaw, preview.getYaw());
        assertEquals(3, preview.getModelScale());
        assertEquals(0.75, preview.getFocusY());
        assertFalse(preview.isOrbitDragging());
        preview.resetView();
        assertEquals(-25, preview.getYaw());
        assertEquals(-8, preview.getPitch());
        assertEquals(1, preview.getModelScale());
        assertEquals(0, preview.getFocusY());
        assertThrows(IllegalArgumentException.class, () -> preview.focusView(Float.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> preview.focusView(0, 0));
    }

    @Test void focusLossHidingAndHostCancellationReleaseCapture() {
        Preview preview = new Preview();
        preview.setInteractive(true);
        preview.mouseClicked(100, 100, 0);
        OreWidgetSupport.cancel(preview);
        assertFalse(preview.isOrbitDragging());
        preview.mouseClicked(100, 100, 0);
        preview.setFocused(false);
        assertFalse(preview.isOrbitDragging());
        preview.mouseClicked(100, 100, 0);
        preview.setAnimationHostVisible(false);
        assertFalse(preview.isOrbitDragging());
        assertFalse(preview.mouseClicked(100, 100, 0));
        preview.setAnimationHostVisible(true);
        preview.mouseClicked(100, 100, 0);
        preview.visible = false;
        preview.tick();
        assertFalse(preview.isOrbitDragging());
        preview.visible = true;
        preview.mouseClicked(100, 100, 0);
        preview.active = false;
        assertFalse(preview.mouseDragged(100, 100, 0, 1, 1));
        assertFalse(preview.isOrbitDragging());
    }

    @Test void keyboardCameraInputRequiresFocusAndCanReset() {
        Preview preview = new Preview();
        preview.setInteractive(true);
        assertFalse(preview.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        preview.setFocused(true);
        assertTrue(preview.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertEquals(-20, preview.getYaw(), 1e-4);
        preview.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_MOD_SHIFT);
        assertEquals(-5, preview.getYaw(), 1e-4);
        preview.keyPressed(GLFW.GLFW_KEY_EQUAL, 0, 0);
        assertTrue(preview.getModelScale() > 1);
        preview.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
        assertEquals(-25, preview.getYaw());
        assertEquals(1, preview.getModelScale());
    }

    @Test void cameraStateDoesNotLeakAcrossPreviews() {
        Preview first = new Preview(), second = new Preview();
        first.setInteractive(true); second.setInteractive(true);
        first.focusView(0.75F, 3);
        assertEquals(1, second.getModelScale());
        assertEquals(0, second.getFocusY());
        first.setInteractive(false);
        assertFalse(first.mouseScrolled(100, 100, 0, 1));
    }

    @Test void centeredZoomNeverDriftsWithTheCursorAndPreservesTheFocus() {
        Preview preview = new Preview();
        preview.setInteractive(true);
        preview.setZoomToCursor(false);
        for (float focus : new float[] {0, 0.75F}) {
            preview.focusView(focus, 1);
            for (int[] cursor : new int[][] {{11, 21}, {209, 259}, {30, 230}}) {
                preview.mouseScrolled(cursor[0], cursor[1], 0, 2);
                assertEquals(0, preview.getModelOffsetX());
                assertEquals(0, preview.getModelOffsetY());
                assertEquals(focus, preview.getFocusY());
            }
        }
        preview.setFocused(true);
        preview.keyPressed(GLFW.GLFW_KEY_MINUS, 0, 0);
        assertEquals(0, preview.getModelOffsetX());
        assertEquals(0, preview.getModelOffsetY());
    }

    @Test void presetsEaseOverThreeHundredMillisecondsAndFinishExactly() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.setInteractive(true);
        preview.setCameraTransitionDuration(0.3);
        preview.focusView(0.75F, 3);
        assertEquals(1, preview.getModelScale());
        time.set(75_000_000);
        preview.tick();
        assertEquals(1.3125, preview.getModelScale(), 1e-5);
        time.set(150_000_000);
        preview.tick();
        assertEquals(2, preview.getModelScale());
        assertEquals(0.375, preview.getFocusY());
        assertEquals(-25, preview.getYaw());
        time.set(300_000_000);
        preview.tick();
        assertEquals(3, preview.getModelScale());
        assertEquals(0.75, preview.getFocusY());
        time.set(900_000_000);
        preview.tick();
        assertEquals(3, preview.getModelScale());
    }

    @Test void retargetingStartsFromTheCurrentCameraWithoutSnapping() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.setCameraTransitionDuration(0.3);
        preview.focusView(0.75F, 3);
        time.set(150_000_000);
        preview.focusView(0, 1);
        assertEquals(2, preview.getModelScale());
        assertEquals(0.375, preview.getFocusY());
        time.set(300_000_000);
        preview.tick();
        assertEquals(1.5, preview.getModelScale());
        assertEquals(0.1875, preview.getFocusY());
        time.set(450_000_000);
        preview.tick();
        assertEquals(1, preview.getModelScale());
        assertEquals(0, preview.getFocusY());
    }

    @Test void resetTweensOffsetsAndTakesTheShortestRotationPath() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.setRotation(-8, 179, 0);
        preview.setInteractive(true);
        preview.setRotation(12, -179, 0);
        preview.setModelOffset(20, 40, 0);
        preview.setModelScale(3);
        preview.setCameraTransitionDuration(0.3);
        preview.resetView();
        time.set(150_000_000);
        preview.tick();
        assertEquals(-180, preview.getYaw());
        assertEquals(2, preview.getPitch());
        assertEquals(10, preview.getModelOffsetX());
        assertEquals(20, preview.getModelOffsetY());
        assertEquals(2, preview.getModelScale());
        time.set(300_000_000);
        preview.tick();
        assertEquals(179, preview.getYaw());
        assertEquals(-8, preview.getPitch());
        assertEquals(0, preview.getModelOffsetX());
        assertEquals(1, preview.getModelScale());
    }

    @Test void directPointerAndKeyboardInputInterruptTweensAtTheCurrentView() {
        for (int input = 0; input < 3; input++) {
            AtomicLong time = new AtomicLong();
            Preview preview = new Preview(time);
            preview.setInteractive(true);
            preview.setCameraTransitionDuration(0.3);
            preview.focusView(0.75F, 3);
            time.set(150_000_000);
            if (input == 0) preview.mouseClicked(100, 100, 0);
            else if (input == 1) preview.mouseScrolled(110, 140, 0, 1);
            else { preview.setFocused(true); preview.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0); }
            float scale = preview.getModelScale();
            float yaw = preview.getYaw();
            assertEquals(0.375, preview.getFocusY());
            time.set(600_000_000);
            preview.tick();
            assertEquals(scale, preview.getModelScale());
            assertEquals(yaw, preview.getYaw());
            assertEquals(0.375, preview.getFocusY());
        }
    }

    @Test void cameraTweensAdvancePerFrameEvenWithPoseAnimationStopped() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.setWidth(2); // Avoid a graphics context while exercising the actual render clock.
        preview.setAnimationRunning(false);
        preview.setCameraTransitionDuration(0.3);
        preview.focusView(0.75F, 3);
        time.set(150_000_000);
        preview.frame();
        assertEquals(2, preview.getModelScale());
        preview.tick();
        assertEquals(2, preview.getModelScale());
        time.set(300_000_000);
        preview.frame();
        assertEquals(3, preview.getModelScale());
        assertThrows(IllegalArgumentException.class, () -> preview.setCameraTransitionDuration(-1));
        assertThrows(IllegalArgumentException.class, () -> preview.setCameraTransitionDuration(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> preview.setCameraTransitionDuration(Double.POSITIVE_INFINITY));
    }
}
