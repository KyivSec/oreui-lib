package com.kyivsec.oreui.client.hud;

import java.util.HashSet;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

/** Keeps held UI keys from becoming gameplay presses/repeats immediately after cursor mode exits. */
final class OreHudInputGuard {
    private final Set<Integer> held = new HashSet<>();
    void consumed(int key, int action) {
        if (action == GLFW.GLFW_RELEASE) held.remove(key); else held.add(key);
    }
    boolean suppressAfterExit(int key, int action) {
        boolean suppress = held.contains(key);
        if (action == GLFW.GLFW_RELEASE) held.remove(key);
        return suppress;
    }
    void clear() { held.clear(); }
}
