package com.dex.myboxmobs.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void legacyColoursBecomeMiniMessage() {
        assertEquals("<reset><green>Hi", Text.toMini("&aHi"));
        assertEquals("<reset><#FFD166>Hi", Text.toMini("&#FFD166Hi"));
        assertEquals("<bold><reset><red>x", Text.toMini("&l&cx"));
    }

    @Test
    void doubleAmpersandIsLiteral() {
        assertEquals("&a", Text.plain("&&a"));
        assertEquals("&#RRGGBB", Text.plain("&&#RRGGBB"));
    }

    @Test
    void mixedFormatsParse() {
        assertEquals("Rồng Băng", Text.plain("&a<gradient:#A0E7E5:#B4F8C8>Rồng</gradient> &#FFD166Băng"));
        assertTrue(Text.legacy("&aX").contains("§a") || Text.legacy("&aX").contains("§x"));
    }

    @Test
    void escapedTagStaysVisible() {
        assertEquals("<gradient>", Text.plain("\\<gradient>"));
    }

    @Test
    void numbersInVietnameseFormat() {
        assertEquals("1.250", Format.number(1250));
        assertEquals("12,5", Format.decimal(12.5));
        assertEquals(1000, Format.parse("1.000"));
        assertEquals(2.5, Format.parse("2,5"));
        assertEquals(0.25, Format.parse("0.25"));
        assertTrue(Double.isNaN(Format.parse("abc")));
    }

    @Test
    void durations() {
        assertEquals("45 giây", Format.duration(45));
        assertEquals("5 phút", Format.duration(300));
        assertEquals("1 giờ 30 phút", Format.duration(5400));
    }

    @Test
    void loreWrapsAtAboutThirtyCharacters() {
        for (String line : Lore.wrap("Người chơi cầm vật phẩm yêu cầu, chuột phải vào block để triệu hồi boss")) {
            assertTrue(line.length() <= 34, line);
        }
    }
}
