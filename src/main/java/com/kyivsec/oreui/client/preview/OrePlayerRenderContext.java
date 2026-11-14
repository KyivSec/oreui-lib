package com.kyivsec.oreui.client.preview;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Frame-local hook context. Pose/scissor/full-bright lighting are already set up; do not retain it. */
public record OrePlayerRenderContext(GuiGraphics graphics, OrePlayerModel model, OrePlayerAppearance appearance,
                                    OrePlayerPose pose, int packedLight) {
    /** Draw a registered 64x64 skin-space overlay with the current model/pose (ARGB tint). */
    public void renderSkinLayer(ResourceLocation texture, int tint) {
        model.model().renderToBuffer(graphics.pose(), graphics.bufferSource().getBuffer(RenderType.entityTranslucent(texture)),
                packedLight, OverlayTexture.NO_OVERLAY, tint);
    }
}
