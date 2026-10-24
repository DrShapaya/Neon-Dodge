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
}
