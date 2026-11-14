package com.kyivsec.oreui.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreUiCommandsTest {
    @Test
    void registersPermissionlessClientTestCommand() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        OreUiCommands.registerCommands(new RegisterClientCommandsEvent(dispatcher, null));
        var root = dispatcher.getRoot().getChild("oreui");
        assertNotNull(root);
        assertTrue(root.canUse(null));
        assertNotNull(root.getChild("test").getCommand());
        var parsed = dispatcher.parse("oreui test", null);
        assertTrue(parsed.getExceptions().isEmpty());
        assertFalse(parsed.getReader().canRead());
        for (String command : new String[] {"oreui hud", "oreui hud on", "oreui hud off", "oreui hud edit"}) {
            var hud = dispatcher.parse(command, null);
            assertTrue(hud.getExceptions().isEmpty(), command);
            assertFalse(hud.getReader().canRead(), command);
        }
    }
}
