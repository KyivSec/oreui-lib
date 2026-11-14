package com.kyivsec.oreui.client.widget;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class OreBlockPreview extends OreModelPreview {
    private BlockState blockState;

    public OreBlockPreview(Component label, BlockState blockState, int height) {
        super(label, height);
        setBlockState(blockState);
        setRotation(25, -35, 0);
    }

    public void setBlockState(BlockState state) {
        blockState = Objects.requireNonNull(state);
    }

    public BlockState getBlockState() {
        return blockState;
    }

    @Override
    protected boolean hasModel() {
        return !blockState.isAir();
    }

    @Override
    protected float pixelsPerUnit() {
        return Math.max(1, Math.min(width, height) - 16) * 0.6F;
    }

    @Override
    protected void renderModel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, graphics.pose(), graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
    }
}
