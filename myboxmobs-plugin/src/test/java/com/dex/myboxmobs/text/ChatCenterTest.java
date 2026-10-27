package com.dex.myboxmobs.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatCenterTest {

    @Test
    void vietnameseLettersUseTheirBaseWidth() {
        assertEquals(ChatCenter.charWidth('a'), ChatCenter.charWidth('ạ'));
        assertEquals(ChatCenter.charWidth('D'), ChatCenter.charWidth('Đ'));
        assertEquals(ChatCenter.charWidth('i'), ChatCenter.charWidth('í'));
    }

    @Test
    void shorterLinesGetMorePadding() {
        String shortPad = ChatCenter.padding(Text.parse("Boss"));
        String longPad = ChatCenter.padding(Text.parse("&#FFD166Bảng xếp hạng sát thương"));
        assertTrue(shortPad.length() > longPad.length());
        assertTrue(longPad.length() > 0);
    }

    @Test
    void boldIsWider() {
        assertTrue(ChatCenter.width(Text.parse("&lBoss"), false) > ChatCenter.width(Text.parse("Boss"), false));
    }
}
