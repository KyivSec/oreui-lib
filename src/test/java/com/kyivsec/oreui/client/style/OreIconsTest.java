package com.kyivsec.oreui.client.style;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreIconsTest {
    private static BufferedImage image(String path) throws IOException {
        try (var stream = OreIconsTest.class.getResourceAsStream("/assets/oreui_lib/" + path)) {
            assertNotNull(stream, path);
            return ImageIO.read(stream);
        }
    }

    @Test
    void oldEnumNamesAndSizeOverloadsResolveToPackagedArtworkWithoutDuplicateRasters() throws IOException {
        assertEquals(47, OreIcons.values().length);
        for (OreIcons icon : OreIcons.values()) {
            for (int size : new int[] {8, 16, 24, 32, 48}) {
                BufferedImage image = image(icon.texture(size).getPath());
                int canvas = icon.nativeIcon() == null ? 16 : icon.nativeIcon().canvasSize();
                assertEquals(icon.texture(), icon.texture(size));
                assertEquals(canvas, image.getWidth());
                assertEquals(canvas, image.getHeight());
                int visible = 0;
                for (int y = 0; y < canvas; y++) {
                    for (int x = 0; x < canvas; x++) {
                        int pixel = image.getRGB(x, y);
                        if (pixel >>> 24 != 0) {
                            visible++;
                            assertEquals(0xFFFFFF, pixel & 0xFFFFFF, icon.name());
                        }
                    }
                }
                assertTrue(visible > 0, icon.name());
            }
        }
    }

    @Test
    void checkIsAnUprightWideTickRatherThanASidewaysArrow() throws IOException {
        BufferedImage image = image(OreIcons.CHECK.texture().getPath());
        int minX = image.getWidth();
        int maxX = 0;
        int minY = image.getHeight();
        int maxY = 0;
        int lowestX = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 > 127) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    if (y > maxY) {
                        maxY = y;
                        lowestX = x;
                    }
                }
            }
        }
        assertTrue(maxX - minX > maxY - minY);
        assertTrue(lowestX > minX && lowestX < (minX + maxX) / 2.0);
    }

    @Test
    void oldAddAndSearchAliasesResolveInHelpersWithoutBundledDuplicateFiles() throws IOException {
        for (OreIcons icon : new OreIcons[] {OreIcons.ADD, OreIcons.SEARCH}) {
            String path = "textures/icons/" + icon.name().toLowerCase(java.util.Locale.ROOT) + ".png";
            ResourceLocation alias = ResourceLocation.fromNamespaceAndPath("oreui_lib", path);
            assertEquals(icon.texture(), OreIcons.raster(alias, 32).texture());
            assertNull(OreIconsTest.class.getResource("/assets/oreui_lib/" + path));
            assertNotNull(image(icon.texture().getPath()));
        }
    }

    @Test
    void unsupportedSizesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> OreIcons.CHECK.texture(20));
    }

    @Test
    void legacyRasterIdentifiersRedirectToActualNativeCanvasDimensions() {
        for (OreIcons icon : OreIcons.values()) {
            for (int size : new int[] {8, 16, 24, 32, 48}) {
                OreIcons.Raster raster = OreIcons.raster(icon.texture(), size);
                assertNotNull(raster);
                if (icon.nativeIcon() == null) {
                    assertEquals(icon.texture(), raster.texture());
                    assertEquals(16, raster.size());
                } else {
                    assertEquals(icon.nativeIcon().texture(), raster.texture());
                    assertEquals(icon.nativeIcon().canvasSize(), raster.size());
                }
                ResourceLocation oldPath = ResourceLocation.fromNamespaceAndPath("oreui_lib",
                        "textures/icons/" + icon.name().toLowerCase(java.util.Locale.ROOT) + "_" + size + ".png");
                OreIcons.Raster legacy = OreIcons.raster(oldPath, size);
                assertEquals(icon.texture(), legacy.texture());
                assertEquals(raster.size(), legacy.size());
            }
        }
        assertEquals(OreIcons.SEARCH.texture(24), OreIcons.raster(OreIcons.SEARCH.texture(16), 20).texture());
        assertEquals(OreIcons.SEARCH.texture(48), OreIcons.raster(OreIcons.SEARCH.texture(16), 96).texture());
        ResourceLocation alias = ResourceLocation.fromNamespaceAndPath("oreui_lib", "textures/icons/search.png");
        assertEquals(OreIcons.SEARCH.texture(8), OreIcons.raster(alias, 8).texture());
        assertNull(OreIcons.raster(ResourceLocation.withDefaultNamespace("textures/block/stone.png"), 32));
        assertNull(OreIcons.raster(ResourceLocation.fromNamespaceAndPath("other", "textures/icons/search_16.png"), 32));
    }
}
