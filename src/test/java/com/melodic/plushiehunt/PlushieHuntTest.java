package com.melodic.plushiehunt;

import com.hypixel.hytale.builtin.triggervolumes.effect.builtin.conditions.PlayerCountCondition.Comparison;
import com.hypixel.hytale.codec.EmptyExtraInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlushieHuntTest {
    @Test
    void addingTheSamePlushieTwiceCountsOnce() {
        var progress = new PlushiesFound();
        assertTrue(progress.add("attic"));
        assertFalse(progress.add("attic"));
        assertTrue(progress.add("cellar"));
        assertEquals(2, progress.count());
    }

    @Test
    void progressSurvivesCodecRoundTrip() {
        var progress = new PlushiesFound();
        progress.add("attic");
        progress.add("cellar");
        var decoded = PlushiesFound.CODEC.decode(PlushiesFound.CODEC.encode(progress, EmptyExtraInfo.EMPTY), EmptyExtraInfo.EMPTY);
        assertEquals(2, decoded.count());
        assertTrue(decoded.has("attic"));
        assertTrue(decoded.has("cellar"));
    }

    @Test
    void messagePlaceholdersAreReplaced() {
        assertEquals("Found attic! (3/10) and earned 5 coins", FindPlushieEffect.format("Found {plushie}! ({found}/{total}) and earned {coins} coins", "attic", 3, 10, 5));
    }

    @Test
    void conditionComparisons() {
        assertTrue(PlushiesFoundCondition.matches(Comparison.AT_LEAST, 10, 10));
        assertFalse(PlushiesFoundCondition.matches(Comparison.AT_LEAST, 9, 10));
        assertTrue(PlushiesFoundCondition.matches(Comparison.LESS_THAN, 0, 1));
        assertTrue(PlushiesFoundCondition.matches(null, 1, 1));
    }
}
