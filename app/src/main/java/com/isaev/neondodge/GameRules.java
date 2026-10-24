package com.isaev.neondodge;

/** Pure gameplay rules kept outside the Canvas view so they can be tested cheaply. */
final class GameRules {
    private GameRules() {
    }

    static boolean spawnLaneIsSafe(float obstacleX, float playerX, float minGap) {
        return Math.abs(obstacleX - playerX) >= minGap;
    }

    static boolean meaningfulRun(float elapsed, int score) {
        return elapsed >= 10f || score >= 300;
    }

    static int nextArmorHits(int currentHits) {
        return Math.max(0, currentHits - 1);
    }

    /** Неон, заработанный за всё время, с которого начинается ранг (ранг 1 — с нуля). */
    static long rankThreshold(int rank) {
        long r = Math.max(1, rank);
        return 2500L * (r - 1) * r;
    }

    /** Ранг растёт всё медленнее: 5 000, 15 000, 30 000, 50 000… неона. */
    static int rankForNeon(long earned) {
        int rank = 1;
        while (earned >= rankThreshold(rank + 1)) {
            rank++;
        }
        return rank;
    }

    /** Доля пути от текущего ранга к следующему, 0..1. */
    static float rankProgress(long earned) {
        int rank = rankForNeon(earned);
        long lo = rankThreshold(rank);
        long hi = rankThreshold(rank + 1);
        return (float) (earned - lo) / (float) (hi - lo);
    }
}
