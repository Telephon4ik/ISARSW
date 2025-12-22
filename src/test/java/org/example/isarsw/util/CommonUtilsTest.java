package org.example.isarsw.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonUtilsTest {

    @Test
    void formatDateTime_ValidTimestamp_ReturnsFormattedString() {
        // UT-16
        long timestamp = 1705327200L; // 15 января 2024, 14:00 UTC
        String result = CommonUtils.formatDateTime(timestamp);
        assertNotNull(result);
        assertTrue(result.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}"));
    }

    @Test
    void formatTime_ValidTimestamp_ReturnsTimeOnly() {
        // UT-17
        long timestamp = 1705327200L;
        String result = CommonUtils.formatTime(timestamp);
        assertNotNull(result);
        assertTrue(result.matches("\\d{2}:\\d{2}"));
    }
}