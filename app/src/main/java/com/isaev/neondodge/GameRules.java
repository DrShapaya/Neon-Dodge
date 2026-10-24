package com.isaev.neondodge;

/** Pure gameplay rules kept outside the Canvas view so they can be tested cheaply. */
final class GameRules {
    static final int MAX_RANK = 100;
    static final int TITLES_COUNT = 11;

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

    /** Неон, заработанный за всё время, с которого начинается ранг (ранг 1 — с нуля, макс. ранг 100). */
    static long rankThreshold(int rank) {
        if (rank <= 1) {
            return 0L;
        }
        long r = Math.min(MAX_RANK, rank);
        return Math.round(2000.0 * (r - 1) * Math.pow(r, 1.42));
    }

    /** Ранг от 1 до MAX_RANK (100). */
    static int rankForNeon(long earned) {
        if (earned <= 0L) {
            return 1;
        }
        int rank = 1;
        while (rank < MAX_RANK && earned >= rankThreshold(rank + 1)) {
            rank++;
        }
        return rank;
    }

    /** Доля пути от текущего ранга к следующему, 0..1 (на 100-м ранге — 1.0). */
    static float rankProgress(long earned) {
        int rank = rankForNeon(earned);
        if (rank >= MAX_RANK) {
            return 1f;
        }
        long lo = rankThreshold(rank);
        long hi = rankThreshold(rank + 1);
        if (hi <= lo) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, (float) (earned - lo) / (float) (hi - lo)));
    }

    /** Индекс звания (0..10) по рангу. */
    static int titleIndexForRank(int rank) {
        if (rank >= 100) return 10;
        if (rank >= 90) return 9;
        if (rank >= 80) return 8;
        if (rank >= 70) return 7;
        if (rank >= 60) return 6;
        if (rank >= 50) return 5;
        if (rank >= 40) return 4;
        if (rank >= 30) return 3;
        if (rank >= 20) return 2;
        if (rank >= 10) return 1;
        return 0;
    }

    /** Диапазон уровней для звания (мин и макс). */
    static int titleMinRank(int index) {
        switch (index) {
            case 0: return 1;
            case 1: return 10;
            case 2: return 20;
            case 3: return 30;
            case 4: return 40;
            case 5: return 50;
            case 6: return 60;
            case 7: return 70;
            case 8: return 80;
            case 9: return 90;
            case 10: return 100;
            default: return 1;
        }
    }

    static int titleMaxRank(int index) {
        switch (index) {
            case 0: return 9;
            case 1: return 19;
            case 2: return 29;
            case 3: return 39;
            case 4: return 49;
            case 5: return 59;
            case 6: return 69;
            case 7: return 79;
            case 8: return 89;
            case 9: return 99;
            case 10: return 100;
            default: return 100;
        }
    }
}
