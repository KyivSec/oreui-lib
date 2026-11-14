package com.kyivsec.oreui.client.hud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.MouseHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreHudMixinTest {
    @Test
    void inputHooksAreClientOnlyAndMatchTheTargetMinecraftCallbacks() throws Exception {
        try (var stream = getClass().getResourceAsStream("/oreui_lib.mixins.json")) {
            assertNotNull(stream);
            var config = new Gson().fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            assertEquals("com.kyivsec.oreui.client.mixin", config.get("package").getAsString());
            assertFalse(config.has("mixins"));
            assertEquals(2, config.getAsJsonArray("client").size());
            assertTrue(config.get("required").getAsBoolean());
        }
        assertNotNull(KeyboardHandler.class.getDeclaredMethod("keyPress", long.class, int.class, int.class, int.class, int.class));
        assertNotNull(KeyboardHandler.class.getDeclaredMethod("charTyped", long.class, int.class, int.class));
        assertNotNull(MouseHandler.class.getDeclaredMethod("onPress", long.class, int.class, int.class, int.class));
        assertNotNull(MouseHandler.class.getDeclaredMethod("onScroll", long.class, double.class, double.class));
        assertNotNull(MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class));
    }
}
