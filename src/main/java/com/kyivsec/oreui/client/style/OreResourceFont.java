package com.kyivsec.oreui.client.style;

import java.util.List;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import org.joml.Matrix4f;

/** Delegates glyph ownership/reloads to Minecraft while sharing styled metrics across drawing and editing. */
final class OreResourceFont extends Font {
    private final Font base;
    private final ResourceLocation fontId;
    private final StringSplitter metrics;

    OreResourceFont(Font base, ResourceLocation fontId) {
        // All glyph-dependent Font entry points delegate below; this view never owns a FontSet/texture.
        super(id -> { throw new IllegalStateException("Resource font glyphs are owned by the base font"); }, false);
        this.base = base;
        this.fontId = fontId;
        metrics = new StringSplitter((codepoint, style) -> base.getSplitter().stringWidth(
                (FormattedCharSequence) sink -> sink.accept(0, OreFonts.resolveStyle(style, fontId), codepoint)));
    }

    private FormattedCharSequence styled(FormattedCharSequence text) {
        return sink -> text.accept((index, style, codepoint) ->
                sink.accept(index, OreFonts.resolveStyle(style, fontId), codepoint));
    }

    @Override
    public int width(String text) { return (int) Math.ceil(metrics.stringWidth(text)); }

    @Override
    public int width(FormattedText text) { return (int) Math.ceil(metrics.stringWidth(text)); }

    @Override
    public int width(FormattedCharSequence text) { return (int) Math.ceil(metrics.stringWidth(text)); }

    @Override
    public String plainSubstrByWidth(String text, int width, boolean tail) {
        return tail ? metrics.plainTailByWidth(text, width, Style.EMPTY) : metrics.plainHeadByWidth(text, width, Style.EMPTY);
    }

    @Override
    public String plainSubstrByWidth(String text, int width) { return plainSubstrByWidth(text, width, false); }

    @Override
    public FormattedText substrByWidth(FormattedText text, int width) { return metrics.headByWidth(text, width, Style.EMPTY); }

    @Override
    public int wordWrapHeight(String text, int width) { return lineHeight * metrics.splitLines(text, width, Style.EMPTY).size(); }

    @Override
    public int wordWrapHeight(FormattedText text, int width) { return lineHeight * metrics.splitLines(text, width, Style.EMPTY).size(); }

    @Override
    public List<FormattedCharSequence> split(FormattedText text, int width) {
        return Language.getInstance().getVisualOrder(metrics.splitLines(text, width, Style.EMPTY)).stream().map(this::styled).toList();
    }

    @Override
    public StringSplitter getSplitter() { return metrics; }

    @Override
    public int drawInBatch(String text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                           MultiBufferSource buffers, DisplayMode mode, int background, int light) {
        return drawInBatch(text, x, y, color, shadow, matrix, buffers, mode, background, light, isBidirectional());
    }

    @Override
    public int drawInBatch(String text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                           MultiBufferSource buffers, DisplayMode mode, int background, int light, boolean bidi) {
        String shaped = bidi ? bidirectionalShaping(text) : text;
        return drawInBatch((FormattedCharSequence) sink -> StringDecomposer.iterateFormatted(shaped, Style.EMPTY, sink),
                x, y, color, shadow, matrix, buffers, mode, background, light);
    }

    @Override
    public int drawInBatch(Component text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                           MultiBufferSource buffers, DisplayMode mode, int background, int light) {
        return drawInBatch(text.getVisualOrderText(), x, y, color, shadow, matrix, buffers, mode, background, light);
    }

    @Override
    public int drawInBatch(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                           MultiBufferSource buffers, DisplayMode mode, int background, int light) {
        return base.drawInBatch(styled(text), x, y, color, shadow, matrix, buffers, mode, background, light);
    }

    @Override
    public void drawInBatch8xOutline(FormattedCharSequence text, float x, float y, int color, int outline,
                                     Matrix4f matrix, MultiBufferSource buffers, int light) {
        base.drawInBatch8xOutline(styled(text), x, y, color, outline, matrix, buffers, light);
    }
}
