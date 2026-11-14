package com.kyivsec.oreui.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestScreenLayoutTest {
    @Test
    void narrowScreensKeepNavigationAndChromeOutsideContent() {
        TestScreenLayout layout = TestScreenLayout.of(320, 240);
        assertEquals(88, layout.navigationWidth());
        assertEquals(196, layout.width());
        assertEquals(54, layout.top());
        assertEquals(210, layout.top() + layout.height());
        assertTrue(layout.x() > layout.navigationX() + layout.navigationWidth());
        assertTrue(layout.x() + layout.width() < 320);
    }

    @Test
    void wideScreensCenterTheNavigationAndBoundedContentTogether() {
        TestScreenLayout layout = TestScreenLayout.of(1920, 1080);
        assertEquals(132, layout.navigationWidth());
        assertEquals(960, layout.width());
        assertEquals(552, layout.x());
        assertEquals(1920, layout.navigationX() * 2 + layout.navigationWidth() + 12 + layout.width());
    }

}
