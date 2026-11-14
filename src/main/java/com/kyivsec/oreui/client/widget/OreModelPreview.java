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
    private long animationTicks;
    private final OrePreviewAnimationClock animationClock = new OrePreviewAnimationClock();
    private final LongSupplier nanoTime;
    private float animationPartialTick;

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
        offsetX = x;
        offsetY = y;
        offsetZ = z;
    }

    public void setRotation(float pitch, float yaw, float roll) {
        finite(pitch);
        finite(yaw);
        finite(roll);
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

    public final void tick() {
        advanceAnimation(nanoTime.getAsLong());
    }

    private void advanceAnimation(long now) {
        var step = animationClock.advance(now, animationRunning && visible);
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
        return false;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
