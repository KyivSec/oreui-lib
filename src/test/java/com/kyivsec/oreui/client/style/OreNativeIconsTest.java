package com.kyivsec.oreui.client.style;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreNativeIconsTest {
    private static byte[] resource(String path) throws Exception {
        try (var stream = OreNativeIconsTest.class.getResourceAsStream("/assets/oreui_lib/" + path)) {
            assertNotNull(stream, path);
            return stream.readAllBytes();
        }
    }

    @Test
    void all59IconsHaveCorrectCanvasBoundsAndHardWhitePixels() throws Exception {
        assertEquals(59, OreNativeIcons.values().length);
        int odd = 0, even = 0;
        for (OreNativeIcons icon : OreNativeIcons.values()) {
            BufferedImage image;
            try (var stream = new java.io.ByteArrayInputStream(resource(icon.texture().getPath()))) {
                image = ImageIO.read(stream);
            }
            assertNotNull(image, icon.name());
            assertEquals(icon.canvasSize(), image.getWidth(), icon.name());
            assertEquals(icon.canvasSize(), image.getHeight(), icon.name());
            if (icon.canvasSize() == 15) odd++; else { assertEquals(16, icon.canvasSize()); even++; }
            int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    assertTrue(pixel == 0 || pixel == 0xFFFFFFFF, icon.name() + " pixel " + x + "," + y);
                    if (pixel != 0) {
                        minX = Math.min(minX, x); minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                    }
                }
            }
            assertTrue(maxX >= minX, icon.name());
            assertEquals(new OreNativeIcons.Bounds(minX, minY, maxX - minX + 1, maxY - minY + 1), icon.ink(), icon.name());
            assertSame(icon, OreNativeIcons.fromTexture(icon.texture()));
        }
        assertEquals(42, odd);
        assertEquals(17, even);
    }

    @Test
    void mixedCanvasFamiliesUseTheSameIntegerPixelPitchAndKeepTheirPadding() {
        for (OreNativeIcons icon : OreNativeIcons.values()) {
            for (int slot : new int[] {16, 20, 24, 30, 32, 48}) {
                var placement = icon.placement(slot);
                int scale = Math.max(1, slot / 16);
                assertEquals(icon.ink().width() * scale, placement.width(), icon.name());
                assertEquals(icon.ink().height() * scale, placement.height(), icon.name());
                assertTrue(placement.x() >= 0 && placement.x() + placement.width() <= slot);
                assertTrue(placement.y() >= 0 && placement.y() + placement.height() <= slot);
            }
        }
        for (OrePixelIcons.Icon glyph : OrePixelIcons.Icon.values()) {
            var icon = glyph.nativeIcon();
            var placement = icon.placement(8);
            assertEquals(icon.ink().width(), placement.width());
            assertEquals(icon.ink().height(), placement.height());
        }
        assertThrows(IllegalArgumentException.class, () -> OreNativeIcons.PLUS.placement(0));
        assertEquals(new OreNativeIcons.Bounds(1, 1, 14, 14), OreNativeIcons.BAG.ink());
        assertEquals(new OreNativeIcons.Bounds(4, 3, 8, 10), OreNativeIcons.HASHTAG_LARGE.ink());
    }

    @Test
    void nativeDefaultsPreserveOldEnumNamesAndResourceIdentifiers() {
        int migrated = 0;
        for (OreIcons icon : OreIcons.values()) {
            if (icon == OreIcons.PACKAGE) { assertNull(icon.nativeIcon()); continue; }
            assertNotNull(icon.nativeIcon(), icon.name());
            assertEquals(icon.nativeIcon().texture(), icon.texture());
            migrated++;
        }
        assertEquals(46, migrated);
        assertSame(OreNativeIcons.PLUS, OreIcons.ADD.nativeIcon());
        assertSame(OreNativeIcons.CHECKMARK, OrePixelIcons.Icon.TICK.nativeIcon());
        assertSame(OreNativeIcons.ARROW_RIGHT, OreIcons.CHEVRON_RIGTH_FILL.nativeIcon());
        assertNull(OreNativeIcons.fromTexture(ResourceLocation.withDefaultNamespace("textures/icons/native/plus.png")));
    }

    @Test
    void everyImportedIconHasMatchingProvenance() throws Exception {
        JsonObject manifest;
        try (var stream = OreNativeIconsTest.class.getResourceAsStream("/assets/oreui_lib/native-icon-attribution.json")) {
            assertNotNull(stream);
            manifest = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
        }
        var icons = manifest.getAsJsonArray("icons");
        assertEquals(59, icons.size());
        int removed = 0;
        for (var element : icons) {
            var entry = element.getAsJsonObject();
            var icon = OreNativeIcons.valueOf(entry.get("name").getAsString().toUpperCase(java.util.Locale.ROOT));
            assertEquals(icon.texture().getPath(), entry.get("texture").getAsString());
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(resource(icon.texture().getPath())));
            assertEquals(entry.get("sha256").getAsString(), hash);
            removed += entry.get("removedOffCanvasPixels").getAsInt();
        }
        assertEquals(10, removed);
    }
}
