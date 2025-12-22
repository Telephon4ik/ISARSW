package org.example.isarsw.service;

public class ConflictChecker {
    public static final long DEFAULT_BUFFER_SECONDS = 10 * 60L;
    public static final long BOARDING_BUFFER_SECONDS = 5 * 60L;

    public static boolean intervalsOverlap(long aStart, long aEnd, long bStart, long bEnd, long bufferSeconds) {
        long aStartB = aStart - bufferSeconds;
        long aEndB = aEnd + bufferSeconds;
        long bStartB = bStart - bufferSeconds;
        long bEndB = bEnd + bufferSeconds;
        return aStartB < bEndB && bStartB < aEndB;
    }

    public static boolean intervalsOverlap(long aStart, long aEnd, long bStart, long bEnd) {
        return intervalsOverlap(aStart, aEnd, bStart, bEnd, DEFAULT_BUFFER_SECONDS);
    }

    public static boolean boardingConflictExists(long newBoardingStart, long newBoardingEnd,
                                                 long existingBoardingStart, long existingBoardingEnd,
                                                 long bufferSeconds) {
        long newStartWithBuffer = newBoardingStart - bufferSeconds;
        long existingStartWithBuffer = existingBoardingStart - bufferSeconds;

        return newStartWithBuffer < existingBoardingEnd &&
                existingStartWithBuffer < newBoardingEnd;
    }

    public static boolean boardingConflictExists(long newBoardingStart, long newBoardingEnd,
                                                 long existingBoardingStart, long existingBoardingEnd) {
        return boardingConflictExists(newBoardingStart, newBoardingEnd,
                existingBoardingStart, existingBoardingEnd,
                BOARDING_BUFFER_SECONDS);
    }
}