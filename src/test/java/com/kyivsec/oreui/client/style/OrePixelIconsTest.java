package com.kyivsec.oreui.client.style;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.BitSet;
import java.util.HashSet;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrePixelIconsTest {
    private static byte[] resource(String path) throws Exception {
        try (var stream = OrePixelIconsTest.class.getResourceAsStream("/assets/oreui_lib/" + path)) {
            assertNotNull(stream, path);
            return stream.readAllBytes();
        }
    }

    private static BufferedImage image(String path) throws Exception {
        try (var stream = new ByteArrayInputStream(resource(path))) {
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path);
            return image;
        }
    }

    private static BitSet mask(BufferedImage strip, int frame) {
        BitSet mask = new BitSet(225);
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
                if (strip.getRGB(x, frame * 15 + y) >>> 24 != 0) mask.set(y * 15 + x);
            }
        }
        return mask;
    }

    @Test
    void suppliedStripsArePackagedUnchangedWithNativeDimensionsAndProvenance() throws Exception {
        JsonObject manifest;
        try (var stream = new ByteArrayInputStream(resource("animation-attribution.json"))) {
            manifest = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
        }
        assertEquals(15, OrePixelIcons.FRAME_SIZE);
        assertEquals(16, OrePixelIcons.FRAME_COUNT);
        var entries = manifest.getAsJsonArray("animations");
        assertEquals(2, entries.size());
        for (var item : entries) {
            var entry = item.getAsJsonObject();
            String path = entry.get("resource").getAsString();
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(resource(path)));
            assertEquals(entry.get("sha256").getAsString(), hash);
            BufferedImage image = image(path);
            assertEquals(15, image.getWidth());
            assertEquals(240, image.getHeight());
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    if (pixel >>> 24 != 0) assertEquals(0xFFFFFFFF, pixel, path);
                }
            }
        }
        assertEquals("331bbc6759058b02b5de37315a440f9d5bccb4d0d58eb4a1b7dbbc1aa1442b14",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(resource(OrePixelIcons.LOADING_TEXTURE.getPath()))));
        assertEquals("80f94355bbc87c560ea7433df462a327c7c5973f5b7db99903bf5acb0c006fd2",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(resource(OrePixelIcons.PROGRESS_TEXTURE.getPath()))));
        assertNull(OrePixelIconsTest.class.getResource("/assets/oreui_lib/textures/gui/community-icons.png"));
        assertNull(OrePixelIconsTest.class.getResource("/assets/oreui_lib/community-icon-attribution.json"));
    }

    @Test
    void framesStayOnTheSameRingAndFormASeamlessLoadingLoopAndIncreasingProgress() throws Exception {
        BufferedImage loading = image(OrePixelIcons.LOADING_TEXTURE.getPath());
        BufferedImage progress = image(OrePixelIcons.PROGRESS_TEXTURE.getPath());
        BitSet ring = mask(image(OreNativeIcons.CIRCLE.texture().getPath()), 0);
        assertEquals(ring, mask(progress, 15));
        var unique = new HashSet<BitSet>();
        BitSet previous = new BitSet(225);
        for (int frame = 0; frame < 16; frame++) {
            BitSet arc = mask(loading, frame);
            assertEquals(9, arc.cardinality());
            assertTrue(unique.add(arc));
            BitSet outside = (BitSet) arc.clone();
            outside.andNot(ring);
            assertTrue(outside.isEmpty());
            BitSet movement = (BitSet) arc.clone();
            movement.xor(mask(loading, (frame + 1) % 16));
            assertEquals(2, movement.cardinality(), "Exactly one arc pixel moves, including across the loop seam");
            BitSet grown = mask(progress, frame);
            assertEquals(frame + 1, grown.cardinality());
            BitSet missing = (BitSet) previous.clone();
            missing.andNot(grown);
            assertTrue(missing.isEmpty());
            previous = grown;
        }
    }

    @Test
    void timingAndDeterminateFrameSelectionHandleBoundariesAndEmptyProgress() {
        assertEquals(0, OrePixelIcons.loadingFrame(0));
        assertEquals(0, OrePixelIcons.loadingFrame(99));
        assertEquals(1, OrePixelIcons.loadingFrame(100));
        assertEquals(15, OrePixelIcons.loadingFrame(1599));
        assertEquals(0, OrePixelIcons.loadingFrame(1600));
        assertEquals(15, OrePixelIcons.loadingFrame(-1));
        for (double empty : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertEquals(-1, OrePixelIcons.progressFrame(empty));
        }
        assertEquals(0, OrePixelIcons.progressFrame(Double.MIN_VALUE));
        for (int step = 1; step <= 16; step++) {
            assertEquals(step - 1, OrePixelIcons.progressFrame(step / 16.0));
            if (step < 16) assertEquals(step, OrePixelIcons.progressFrame(Math.nextUp(step / 16.0)));
        }
        assertEquals(7, OrePixelIcons.progressFrame(0.5));
        assertEquals(15, OrePixelIcons.progressFrame(1));
        assertEquals(15, OrePixelIcons.progressFrame(2));
    }

    @Test
    void allFramesUseTheSameIntegerFittingInsteadOfRecroppingMovingArcs() {
        for (int slot : new int[] {7, 8, 15, 16, 20, 24, 30, 32, 48}) {
            var placement = OrePixelIcons.animationPlacement(slot);
            int scale = Math.max(1, slot / 16);
            assertEquals(15 * scale, placement.size());
            assertEquals(Math.floorDiv(slot - 15 * scale, 2), placement.offset());
            assertEquals(0, placement.sourceInset());
            assertEquals(15, placement.sourceSize());
        }
        var tiny = OrePixelIcons.animationPlacement(4);
        assertEquals(4, tiny.size());
        assertEquals(4, tiny.sourceInset());
        assertEquals(7, tiny.sourceSize());
        assertThrows(IllegalArgumentException.class, () -> OrePixelIcons.animationPlacement(0));
    }
}
