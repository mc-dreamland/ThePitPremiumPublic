package cn.charlotte.pit.nametag.hiding;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringDecomposer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameHidingMinecraftTextTest {
    @Test void minecraft1206FormattedTextParserEmitsNoGlyphsForTheWholeIdentifierRange() {
        for (int value : new int[]{1, 15, 16, 150, 10000, 0xfffffff}) {
            StringBuilder visible = new StringBuilder();
            assertTrue(StringDecomposer.iterateFormatted(NameHidingAliases.encode(value), Style.EMPTY, (index, style, codepoint) -> {
                visible.appendCodePoint(codepoint);
                return true;
            }));
            assertEquals("", visible.toString());
        }
    }
    @Test void invisibleNamesDoNotEatThePrefixSuffixOrTheirColours() {
        var tag = Component.empty().withStyle(ChatFormatting.WHITE)
                .append(Component.literal("[VIP] ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(NameHidingAliases.encode(150)))
                .append(Component.literal(" suffix").withStyle(ChatFormatting.BLUE));
        StringBuilder visible = new StringBuilder();
        assertTrue(StringDecomposer.iterateFormatted(tag, Style.EMPTY, (index, style, codepoint) -> {
            visible.appendCodePoint(codepoint);
            if (codepoint == '[') assertEquals(ChatFormatting.GOLD.getColor(), style.getColor().getValue());
            if (codepoint == 's') assertEquals(ChatFormatting.BLUE.getColor(), style.getColor().getValue());
            return true;
        }));
        assertEquals("[VIP]  suffix", visible.toString());
    }
}
