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
