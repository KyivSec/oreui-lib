package com.kyivsec.oreui.client.preview;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/** Optional owner of a registered dynamic 64x64 skin. Share appearances, but close this handle once. */
public final class OrePreviewTexture implements AutoCloseable {
    private final TextureManager manager;
    private final ResourceLocation location;
    private boolean closed;

    private OrePreviewTexture(TextureManager manager, ResourceLocation location) {
        this.manager = manager; this.location = location;
    }

    /** On success transfers the image to Minecraft; on validation failure the image still belongs to the caller. */
    public static OrePreviewTexture register(NativeImage image) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(image);
        if (image.getWidth() != 64 || image.getHeight() != 64) throw new IllegalArgumentException("Standalone player skins must be 64x64");
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        DynamicTexture texture;
        try { texture = new DynamicTexture(image); }
        catch (RuntimeException | Error error) { image.close(); throw error; }
        try {
            return new OrePreviewTexture(manager, manager.register("oreui_preview_skin", texture));
        } catch (RuntimeException | Error error) {
            texture.close();
            throw error;
        }
    }

    public ResourceLocation location() {
        if (closed) throw new IllegalStateException("Preview texture is closed");
        return location;
    }
    public boolean isClosed() { return closed; }

    /** Release the manager registration, GPU texture and retained NativeImage. Render thread only; idempotent. */
    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        if (!closed) { closed = true; manager.release(location); }
    }
}
