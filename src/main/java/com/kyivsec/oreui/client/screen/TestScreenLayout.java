package com.kyivsec.oreui.client.screen;

record TestScreenLayout(int navigationX, int navigationWidth, int x, int width, int top, int height) {
    static TestScreenLayout of(int screenWidth, int screenHeight) {
        int groupWidth = Math.max(1, Math.min(1104, screenWidth - 24));
        int navigationWidth = Math.max(88, Math.min(132, screenWidth / 5));
        int navigationX = (screenWidth - groupWidth) / 2;
        int x = navigationX + navigationWidth + 12;
        return new TestScreenLayout(navigationX, navigationWidth, x, Math.max(1, groupWidth - navigationWidth - 12),
                54, Math.max(1, screenHeight - 84));
    }

}
