package com.kyivsec.oreui.client.widget;

import com.mojang.blaze3d.platform.Lighting;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class OreItemPreview extends OreModelPreview {
    private ItemStack item;
    private ItemDisplayContext displayContext = ItemDisplayContext.GROUND;
    private boolean floating;
    private float bobAmplitude = 3;
    private float bobSpeed = 0.5F;
    private float rotationSpeed = 30;
    private BakedModel model;
    private ItemTransform displayTransform;

    public OreItemPreview(Component label, ItemStack item, int height) {
        super(label, height);
        setItem(item);
        setRotation(20, 35, 0);
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item).copy();
    }

    public ItemStack getItem() {
        return item.copy();
    }

    public void setDisplayContext(ItemDisplayContext context) {
        displayContext = Objects.requireNonNull(context);
    }

    public void setFloating(boolean floating) {
        this.floating = floating;
    }

    public boolean isFloating() {
        return floating;
    }

    public void setBobAmplitude(float pixels) {
        if (finite(pixels) < 0) throw new IllegalArgumentException("Bob amplitude cannot be negative");
        bobAmplitude = pixels;
    }

    public void setBobSpeed(float cyclesPerSecond) {
        if (finite(cyclesPerSecond) < 0) throw new IllegalArgumentException("Bob speed cannot be negative");
        bobSpeed = cyclesPerSecond;
    }

    public void setRotationSpeed(float degreesPerSecond) {
        rotationSpeed = finite(degreesPerSecond);
    }

    @Override
    protected boolean hasModel() {
        if (item.isEmpty()) {
            model = null;
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        model = minecraft.getItemRenderer().getModel(item, minecraft.level, null, 0);
        displayTransform = model.getTransforms().getTransform(displayContext);
        return true;
    }

    @Override
    protected float animatedOffsetY(float seconds) {
        return floating ? (float) Math.sin(seconds * bobSpeed * Math.PI * 2) * bobAmplitude : 0;
    }

    @Override
    protected float animatedYaw(float seconds) {
        return floating ? seconds * rotationSpeed % 360 : 0;
    }

    @Override
    protected float pixelsPerUnit() {
        float sourceScale = Math.max(Math.abs(displayTransform.scale.x()),
                Math.max(Math.abs(displayTransform.scale.y()), Math.abs(displayTransform.scale.z())));
        return Math.max(1, Math.min(width, height) - 16) * 0.6F / Math.max(0.01F, sourceScale);
    }

    @Override
    protected void renderModel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        var renderer = minecraft.getItemRenderer();
        if (!model.usesBlockLight()) Lighting.setupForFlatItems();
        graphics.pose().translate(-displayTransform.translation.x(), -displayTransform.translation.y(), -displayTransform.translation.z());
        renderer.render(item, displayContext, false, graphics.pose(), graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
    }
}
