package com.dex.myboxmobs.text;

import java.text.Normalizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Centres a chat line with spaces, using the pixel widths of Minecraft's default font (chat is 320 px wide, centre
 * at 154 px). Vietnamese letters count as their base letter; bold adds 1 px per character.
 */
public final class ChatCenter {

    public static final String TAG = "<center>";
    private static final int CENTER_PX = 154;
    private static final int SPACE_PX = 4;

    private ChatCenter() {
    }

    /** Spaces to put before {@code line} so it is centred. */
    public static String padding(Component line) {
        int width = width(line, false);
        int toCompensate = CENTER_PX - width / 2;
        if (toCompensate <= 0) return "";
        return " ".repeat(toCompensate / SPACE_PX);
    }

    static int width(Component c, boolean parentBold) {
        TextDecoration.State state = c.decoration(TextDecoration.BOLD);
        boolean bold = state == TextDecoration.State.TRUE || (state == TextDecoration.State.NOT_SET && parentBold);
        int w = 0;
        if (c instanceof TextComponent t) {
            for (int i = 0; i < t.content().length(); ) {
                int cp = t.content().codePointAt(i);
                i += Character.charCount(cp);
                w += charWidth(cp) + (bold && cp != ' ' ? 1 : 0) + 1;
            }
        }
        for (Component child : c.children()) w += width(child, bold);
        return w;
    }

    static int charWidth(int cp) {
        if (cp == 'Đ') cp = 'D';
        if (cp == 'đ') cp = 'd';
        if (cp > 127) {
            String base = Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFD);
            if (!base.isEmpty() && base.charAt(0) < 128) cp = base.charAt(0);
        }
        return switch (cp) {
            case '!', '.', ',', ':', ';', 'i', '|', '\'' -> 1;
            case 'l', '`' -> 2;
            case 'I', 't', '[', ']', ' ' -> 3;
            case 'f', 'k', '(', ')', '{', '}', '<', '>', '"', '*' -> 4;
            case '@', '~' -> 6;
            default -> 5;
        };
    }
}
