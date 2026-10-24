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
}
