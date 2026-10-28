package com.dex.myboxmobs.gui;

/** Test access to the package-private range parser of {@link ThiefMenu}. */
public final class ThiefMenuAccess {

    private ThiefMenuAccess() {
    }

    public static double[] range(String text) {
        return ThiefMenu.percentRange(text);
    }
}
