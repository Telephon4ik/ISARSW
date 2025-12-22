package org.example.isarsw.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConflictCheckerTest {

    @Test
    void intervalsOverlap_OverlappingIntervals_ReturnsTrue() {
        assertTrue(ConflictChecker.intervalsOverlap(1000, 2000, 1500, 2500));
    }

    @Test
    void intervalsOverlap_NonOverlappingIntervals_ReturnsFalse() {
        // С буфером 600: [400-2600] и [3600-4600] → [3000-5200]
        // 3000 > 2600, не пересекаются
        assertFalse(ConflictChecker.intervalsOverlap(1000, 2000, 3600, 4600));
    }

    @Test
    void boardingConflictExists_WithBufferConflict_ReturnsTrue() {
        assertTrue(ConflictChecker.boardingConflictExists(1000, 2000, 1800, 2800, 300));
    }
}