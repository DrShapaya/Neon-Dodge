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
        assertEquals(1, GameRules.rankForNeon(4999L));
        assertEquals(2, GameRules.rankForNeon(5000L));
        assertEquals(3, GameRules.rankForNeon(15000L));
        assertEquals(4, GameRules.rankForNeon(38044L));
        assertEquals(5, GameRules.rankForNeon(50000L));
    }

    @Test
    public void rankProgressIsShareOfCurrentStep() {
        assertEquals(0f, GameRules.rankProgress(5000L), 1e-6f);
        assertEquals(0.5f, GameRules.rankProgress(10000L), 1e-6f);
        assertTrue(GameRules.rankProgress(14999L) < 1f);
    }
}
