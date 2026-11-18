package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.Objects;
import java.util.function.LongSupplier;
import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.glfw.GLFW;

public abstract class OreModelPreview extends AbstractWidget {
    private float offsetX;
    private float offsetY;
    private float offsetZ;
    private float pitch;
    private float yaw;
    private float roll;
    private float modelScale = 1;
    private boolean background = true;
    private boolean animationRunning = true;
    private boolean animationHostVisible = true;
    private long animationTicks;
    private final OrePreviewAnimationClock animationClock = new OrePreviewAnimationClock();
    private final LongSupplier nanoTime;
    private float animationPartialTick;
    private boolean interactive;
    private boolean orbitDragging;
    private boolean hasHomeView;
    private float focusY;
    private float homePitch, homeYaw, homeRoll, homeScale = 1;
    private float homeX, homeY, homeZ, homeFocusY;
    private boolean zoomToCursor = true;
    private long cameraTransitionNanos;
    private long cameraTransitionStart;
    private CameraView cameraFrom, cameraTarget;

    private record CameraView(float x, float y, float z, float pitch, float yaw, float roll, float scale, float focusY) {}

    /** Defaults to immediate camera presets; positive durations use a smooth ease-in/out. */
    public void setCameraTransitionDuration(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Transition duration must be finite and nonnegative");
        stopCameraTransition();
        cameraTransitionNanos = (long) (seconds * 1_000_000_000.0);
    }

    /** False keeps wheel/key zoom centered on the model's current focus instead of the cursor. */
    public void setZoomToCursor(boolean enabled) { zoomToCursor = enabled; }

    private CameraView cameraView() {
        return new CameraView(offsetX, offsetY, offsetZ, pitch, yaw, roll, modelScale, focusY);
    }

    private void applyCameraView(CameraView view) {
        offsetX = view.x; offsetY = view.y; offsetZ = view.z;
        pitch = view.pitch; yaw = view.yaw; roll = view.roll;
        modelScale = view.scale; focusY = view.focusY;
    }

    private void transitionTo(CameraView target) {
        long now = nanoTime.getAsLong();
        advanceCamera(now);
        cancelDrag();
        if (cameraTransitionNanos == 0) {
            applyCameraView(target);
            cameraTarget = cameraFrom = null;
            return;
        }
        cameraFrom = cameraView();
        cameraTarget = target;
        cameraTransitionStart = now;
    }

    private void advanceCamera(long now) {
        if (cameraTarget == null) return;
        float t = (float) Mth.clamp((now - cameraTransitionStart) / (double) cameraTransitionNanos, 0, 1);
        if (t >= 1) {
            applyCameraView(cameraTarget);
            cameraTarget = cameraFrom = null;
            return;
        }
        float eased = t * t * (3 - 2 * t);
        applyCameraView(new CameraView(
                Mth.lerp(eased, cameraFrom.x, cameraTarget.x), Mth.lerp(eased, cameraFrom.y, cameraTarget.y),
                Mth.lerp(eased, cameraFrom.z, cameraTarget.z), Mth.lerp(eased, cameraFrom.pitch, cameraTarget.pitch),
                cameraFrom.yaw + eased * Mth.wrapDegrees(cameraTarget.yaw - cameraFrom.yaw),
                cameraFrom.roll + eased * Mth.wrapDegrees(cameraTarget.roll - cameraFrom.roll),
                Mth.lerp(eased, cameraFrom.scale, cameraTarget.scale), Mth.lerp(eased, cameraFrom.focusY, cameraTarget.focusY)));
    }

    private void stopCameraTransition() {
        advanceCamera(nanoTime.getAsLong());
        cameraTarget = cameraFrom = null;
    }

    /** Opt-in orbit/zoom input. Enabling records the current transforms as the reset view. */
    public void setInteractive(boolean interactive) {
        if (this.interactive == interactive) return;
        stopCameraTransition();
        cancelDrag();
        this.interactive = interactive;
        if (interactive) {
            homePitch = pitch; homeYaw = yaw; homeRoll = roll; homeScale = modelScale;
            homeX = offsetX; homeY = offsetY; homeZ = offsetZ; homeFocusY = focusY;
            hasHomeView = true;
        }
    }

    public boolean isInteractive() { return interactive; }
    public boolean isOrbitDragging() { return orbitDragging; }
    public float getFocusY() { return focusY; }
    public float getModelOffsetX() { return offsetX; }
    public float getModelOffsetY() { return offsetY; }

    public void cancelDrag() { orbitDragging = false; }

    /** Model-space vertical target, with bounded zoom; orientation is preserved. */
    public void focusView(float targetY, float zoom) {
        finite(targetY); finite(zoom);
        if (zoom <= 0) throw new IllegalArgumentException("Zoom must be positive");
        stopCameraTransition();
        transitionTo(new CameraView(0, 0, offsetZ, pitch, yaw, roll, Mth.clamp(zoom, 0.75F, 4), targetY));
    }

    public void resetView() {
        cancelDrag();
        if (!hasHomeView) return;
        transitionTo(new CameraView(homeX, homeY, homeZ, homePitch, homeYaw, homeRoll, homeScale, homeFocusY));
    }

    private boolean acceptsCameraInput() {
        return interactive && active && visible && animationHostVisible;
    }

    private void orbit(double dx, double dy) {
        stopCameraTransition();
        setRotation(Mth.clamp(pitch + (float) dy * 0.6F, -60, 60),
                Mth.wrapDegrees(yaw + (float) dx * 0.6F), roll);
    }

    private void zoom(double amount, double mouseX, double mouseY) {
        stopCameraTransition();
        float previous = Mth.clamp(modelScale, 0.75F, 4);
        float next = Mth.clamp((float) (previous * Math.exp(Mth.clamp(amount, -40, 40) * 0.14)), 0.75F, 4);
        float ratio = next / previous;
        if (zoomToCursor) {
            offsetX = (float) Mth.clamp(offsetX + (mouseX - getX() - width / 2.0 - offsetX) * (1 - ratio), -width * 4.0, width * 4.0);
            offsetY = (float) Mth.clamp(offsetY + (mouseY - getY() - height / 2.0 - offsetY) * (1 - ratio), -height * 4.0, height * 4.0);
        } else {
            offsetX = offsetY = 0;
        }
        setModelScale(next);
    }

    protected OreModelPreview(Component label, int height) {
        this(label, height, Util::getNanos);
    }

    OreModelPreview(Component label, int height, LongSupplier nanoTime) {
        super(0, 0, 100, height, label);
        if (height <= 0) throw new IllegalArgumentException("Preview height must be positive");
        this.nanoTime = Objects.requireNonNull(nanoTime);
    }

    protected static float finite(float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Preview transforms must be finite");
        return value;
    }

    public void setModelOffset(float x, float y, float z) {
        finite(x);
        finite(y);
        finite(z);
        stopCameraTransition();
        offsetX = x;
        offsetY = y;
        offsetZ = z;
    }

    public void setRotation(float pitch, float yaw, float roll) {
        finite(pitch);
        finite(yaw);
        finite(roll);
        stopCameraTransition();
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }

    public void setModelScale(float scale) {
        if (finite(scale) <= 0) throw new IllegalArgumentException("Model scale must be positive");
        stopCameraTransition();
        modelScale = scale;
    }

    public float getModelScale() {
        return modelScale;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public void setAnimationRunning(boolean running) {
        if (animationRunning != running) {
            long now = nanoTime.getAsLong();
            advanceAnimation(now);
            animationRunning = running;
            advanceAnimation(now);
        }
    }

    public boolean isAnimationRunning() {
        return animationRunning;
    }

    /** Lets a screen-independent host suspend its entire layer without changing the widget's own visibility. */
    public final void setAnimationHostVisible(boolean visible) {
        if (!visible) cancelDrag();
        if (animationHostVisible != visible) {
            long now = nanoTime.getAsLong();
            advanceAnimation(now);
            animationHostVisible = visible;
            advanceAnimation(now);
        }
    }

    public final void tick() {
        if (!acceptsCameraInput()) cancelDrag();
        advanceAnimation(nanoTime.getAsLong());
    }

    private void advanceAnimation(long now) {
        // Camera presets use wall time independently of walking/pose animation or world partial ticks.
        advanceCamera(now);
        var step = animationClock.advance(now, animationRunning && visible && animationHostVisible);
        for (int i = 0; i < step.ticks(); i++) {
            animationTicks++;
            tickModel();
        }
        animationPartialTick = step.partialTick();
    }

    protected void tickModel() {
    }

    protected final float animationSeconds(float partialTick) {
        return (animationTicks % 1_000_000L + Mth.clamp(partialTick, 0, 1)) / 20.0F;
    }

    protected final float animationSeconds() {
        return animationSeconds(animationPartialTick);
    }

    protected float animatedOffsetY(float seconds) {
        return 0;
    }

    protected float animatedYaw(float seconds) {
        return 0;
    }

    protected boolean hasModel() {
        return true;
    }

    protected Component emptyMessage() {
        return Component.translatable("oreui.preview.empty");
    }

    protected abstract float pixelsPerUnit();

    protected abstract void renderModel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    @Override
    protected final void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen partialTick belongs to the world and can be frozen while a menu is open.
        // Both tick() and rendering service the same clock, so a frame never double-ticks a pose.
        advanceAnimation(nanoTime.getAsLong());
        if (width <= 2 || height <= 2) return;
        if (background) OreDrawing.inset(graphics, getX(), getY(), width, height);
        if (!hasModel()) {
            OreDrawing.scrollingText(graphics, net.minecraft.client.Minecraft.getInstance().font, emptyMessage(),
                    getX() + 6, getY() + 4, getRight() - 6, getBottom() - 4, OreDrawing.MUTED);
            return;
        }
        graphics.flush();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        float[] color = RenderSystem.getShaderColor().clone();
        graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        graphics.pose().pushPose();
        try {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.enableDepthTest();
            Lighting.setupFor3DItems();
            float seconds = animationSeconds();
            graphics.pose().translate(getX() + width / 2.0F + offsetX,
                    getY() + height / 2.0F + offsetY + animatedOffsetY(seconds), 150 + offsetZ);
            float scale = Math.max(1, pixelsPerUnit()) * modelScale;
            graphics.pose().scale(scale, -scale, scale);
            graphics.pose().mulPose(Axis.XP.rotationDegrees(pitch));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(yaw + animatedYaw(seconds)));
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(roll));
            graphics.pose().translate(0, -focusY, 0);
            renderModel(graphics, mouseX, mouseY, animationPartialTick);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            graphics.disableScissor();
            Lighting.setupFor3DItems();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            graphics.setColor(color[0], color[1], color[2], color[3]);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!acceptsCameraInput() || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        stopCameraTransition();
        orbitDragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!acceptsCameraInput()) { cancelDrag(); return false; }
        if (!orbitDragging || button != 0 || !Double.isFinite(dx) || !Double.isFinite(dy)) return false;
        orbit(Mth.clamp(dx, -10000, 10000), Mth.clamp(dy, -10000, 10000));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean captured = orbitDragging && button == 0;
        if (button == 0) cancelDrag();
        return captured;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!acceptsCameraInput() || !isMouseOver(mouseX, mouseY) || !Double.isFinite(vertical) || vertical == 0) return false;
        zoom(vertical, mouseX, mouseY);
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) cancelDrag();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (!acceptsCameraInput() || !isFocused()) return false;
        double step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 15 : 5;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> orbit(-step / 0.6, 0);
            case GLFW.GLFW_KEY_RIGHT -> orbit(step / 0.6, 0);
            case GLFW.GLFW_KEY_UP -> orbit(0, -step / 0.6);
            case GLFW.GLFW_KEY_DOWN -> orbit(0, step / 0.6);
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> zoom(1, getX() + width / 2.0, getY() + height / 2.0);
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> zoom(-1, getX() + width / 2.0, getY() + height / 2.0);
            case GLFW.GLFW_KEY_HOME -> resetView();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return acceptsCameraInput() ? super.nextFocusPath(event) : null;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
