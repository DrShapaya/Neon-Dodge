package com.isaev.neondodge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameRulesTest {
    @Test
    public void spawnLaneKeepsSafeGap() {
        assertTrue(GameRules.spawnLaneIsSafe(100f, 300f, 150f));
        assertFalse(GameRules.spawnLaneIsSafe(180f, 300f, 150f));
    }

    @Test
    public void shortRunsDoNotFarmDailyRuns() {
        assertFalse(GameRules.meaningfulRun(2f, 20));
        assertTrue(GameRules.meaningfulRun(10f, 20));
        assertTrue(GameRules.meaningfulRun(2f, 300));
    }

    @Test
    public void armorLosesOneStagePerImpact() {
        assertEquals(1, GameRules.nextArmorHits(2));
        assertEquals(0, GameRules.nextArmorHits(0));
    }

    @Test
    public void rankStartsAtOneAndGrowsByThresholds() {
        assertEquals(1, GameRules.rankForNeon(0L));
        long t2 = GameRules.rankThreshold(2);
        long t3 = GameRules.rankThreshold(3);
        assertTrue(t2 > 4000L);
        assertEquals(1, GameRules.rankForNeon(t2 - 1L));
        assertEquals(2, GameRules.rankForNeon(t2));
        assertEquals(2, GameRules.rankForNeon(t3 - 1L));
        assertEquals(3, GameRules.rankForNeon(t3));
        assertEquals(GameRules.MAX_RANK, GameRules.rankForNeon(500_000_000L));
    }

    @Test
    public void rankCurveIsQuadraticAndMonotonic() {
        assertEquals(0L, GameRules.rankThreshold(1));
        assertEquals(12000L, GameRules.rankThreshold(2));
        assertEquals(12000L * 4, GameRules.rankThreshold(3));
        assertEquals(12000L * 49L * 49L, GameRules.rankThreshold(50));
        assertEquals(12000L * 99L * 99L, GameRules.rankThreshold(100));
        long prev = 0L;
        for (int r = 2; r <= GameRules.MAX_RANK; r++) {
            long t = GameRules.rankThreshold(r);
            assertTrue("threshold must grow: rank " + r, t > prev);
            prev = t;
        }
    }

    @Test
    public void neonMultiplierIsCapped() {
        assertEquals(1f, GameRules.neonMult(1f), 1e-6f);
        assertEquals(2.7f, GameRules.neonMult(2.7f), 1e-6f);
        assertEquals(3f, GameRules.neonMult(9f), 1e-6f);
        assertEquals(3f, GameRules.neonMult(5000f), 1e-6f);
    }

    @Test
    public void workshopCostGrowsModeratelyAndStopsAtCap() {
        assertEquals(2500, Progress.workshopCost(0, 2500));
        assertEquals(4125, Progress.workshopCost(1, 2500));
        // Каждая ступень растёт плавно по полиномиальной формуле.
        long c4 = Progress.workshopCost(4, 2500);
        long c5 = Progress.workshopCost(5, 2500);
        assertTrue(c5 > c4);
        assertTrue(c5 * 100 < c4 * 135);
        // Максимальный уровень недостижим для покупок — цену не называют.
        assertEquals(0, Progress.workshopCost(Progress.WORKSHOP_MAX_10, 2500));
        assertEquals(0, Progress.workshopCost(Progress.WORKSHOP_MAX_5, Progress.WORKSHOP_MAX_5, 2500));
        assertEquals(0, Progress.workshopCost(Progress.WORKSHOP_MAX_8, Progress.WORKSHOP_MAX_8, 1200));
        // Хвост остаётся достижимым: самый дорогой 10-й уровень в пределах десятков тысяч.
        long top = Progress.workshopCost(Progress.WORKSHOP_MAX_10 - 1, 4000);
        assertTrue(top > 0 && top < 100_000);
    }

    @Test
    public void rankProgressIsShareOfCurrentStep() {
        long t2 = GameRules.rankThreshold(2);
        long t3 = GameRules.rankThreshold(3);
        assertEquals(0f, GameRules.rankProgress(t2), 1e-6f);
        float mid = GameRules.rankProgress((t2 + t3) / 2);
        assertTrue(mid > 0.4f && mid < 0.6f);
        assertEquals(1f, GameRules.rankProgress(GameRules.rankThreshold(100)), 1e-6f);
    }

    @Test
    public void rankTitlesCoverLadder() {
        assertEquals(0, GameRules.titleIndexForRank(1));
        assertEquals(1, GameRules.titleIndexForRank(10));
        assertEquals(5, GameRules.titleIndexForRank(50));
        assertEquals(10, GameRules.titleIndexForRank(100));
    }
}
