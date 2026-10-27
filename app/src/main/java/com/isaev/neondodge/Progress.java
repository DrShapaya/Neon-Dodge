package com.isaev.neondodge;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import java.util.ArrayList;

/**
 * Долговременный прогресс: рекорд, суммарная статистика, достижения,
 * мастерская и выбранные скины. Всё хранится только на устройстве.
 */
final class Progress {

    static final int MODE_CLASSIC = 0;
    static final int MODE_HARDCORE = 1;
    static final int MODE_ZEN = 2;
    static final int MODE_COUNT = 3;
    static final int HISTORY_LIMIT = 10;

    static final int ACH_FIRST_RUN = 0;
    static final int ACH_SCORE_1000 = 1;
    static final int ACH_SCORE_5000 = 2;
    static final int ACH_DODGE_500 = 3;
    static final int ACH_COMBO_10 = 4;
    static final int ACH_COMBO_25 = 5;
    static final int ACH_STARS_100 = 6;
    static final int ACH_TIME_10 = 7;
    static final int ACH_SHIELD = 8;
    static final int ACH_SURVIVE_120 = 9;
    static final int ACH_SCORE_10000 = 10;
    static final int ACH_COMBO_50 = 11;
    static final int ACH_BOSS_DEFEATED = 12;
    static final int ACH_COUNT = 13;

    static final int SPHERE_COUNT = 8;
    static final int TRAIL_COUNT = 7;
    static final int WORKSHOP_MAX_5 = 5;
    static final int WORKSHOP_MAX_8 = 8;
    static final int WORKSHOP_MAX_10 = 10;
    static final int WORKSHOP_MAX_LEVEL = WORKSHOP_MAX_10;
    static final int WORKSHOP_MAX_LEVEL_MULT = WORKSHOP_MAX_10;
    static final int WORKSHOP_MAX_LEVEL_TEMP = WORKSHOP_MAX_5;
    static final int WORKSHOP_TOTAL_MAX_LEVEL = 78;
    static final int WORKSHOP_UNLOCK_LEVEL = 5;

    private final SharedPreferences prefs;

    int best;
    final int[] bestByMode = new int[MODE_COUNT];
    int runs;
    int dodged;
    int stars;
    int bestCombo;
    int bestTime;
    int shieldBlocks;
    int bossesDefeated;
    int pointsTotal;
    /** Весь заработанный неон, без вычета покупок: из него считается ранг. */
    long neonEarned;
    /** Кошелёк: неон, доступный к тратам. Растёт только от начислений. */
    long neon;
    int multiplierUpgradeLevel;
    int closeMultiplierUpgradeLevel;
    int bossMultiplierUpgradeLevel;
    int levelMultiplierUpgradeLevel;
    int bonusDurationUpgradeLevel;
    int tempMultiplierUpgradeLevel;
    int comboTimeUpgradeLevel;
    int shieldCapUpgradeLevel;
    int magnetRadiusUpgradeLevel;
    int emergencyPhaseUpgradeLevel;
    int secondWindUpgradeLevel;
    int conduitBoostUpgradeLevel;
    float totalTime;

    static final class RunRecord {
        final int type;
        final int score;
        final int seconds;
        final long timestamp;
        final String date;

        RunRecord(int type, int score, int seconds, long timestamp) {
            this.type = type;
            this.score = score;
            this.seconds = seconds;
            this.timestamp = timestamp;
            // Форматируем один раз, а не на каждом кадре экрана истории.
            this.date = DateFormat.format("dd.MM HH:mm", timestamp).toString();
        }
    }

    final ArrayList<RunRecord> runHistory = new ArrayList<>();

    int achMask;
    int mode;
    int sphereSkin;
    int trailSkin;

    Progress(Context context) {
        prefs = context.getSharedPreferences("neon_dodge", Context.MODE_PRIVATE);

        best = prefs.getInt("best", 0);
        boolean hasModeBestKeys = prefs.contains("best_mode_" + MODE_CLASSIC);
        for (int i = 0; i < MODE_COUNT; i++) {
            bestByMode[i] = prefs.getInt("best_mode_" + i, 0);
        }
        if (!hasModeBestKeys && best > 0) {
            bestByMode[MODE_CLASSIC] = best;
        }
        runs = prefs.getInt("stat_runs", 0);
        dodged = prefs.getInt("stat_dodged", 0);
        stars = prefs.getInt("stat_stars", 0);
        bestCombo = prefs.getInt("stat_best_combo", 0);
        bestTime = prefs.getInt("stat_best_time", 0);
        shieldBlocks = prefs.getInt("stat_shields", 0);
        bossesDefeated = prefs.getInt("stat_bosses_defeated", 0);
        pointsTotal = prefs.getInt("points_total", 0);
        multiplierUpgradeLevel = clamp(prefs.getInt("mult_upgrade", 0), 0, WORKSHOP_MAX_10);
        closeMultiplierUpgradeLevel = clamp(prefs.getInt("mult_upgrade_close", 0), 0, WORKSHOP_MAX_10);
        bossMultiplierUpgradeLevel = clamp(prefs.getInt("mult_upgrade_boss", 0), 0, WORKSHOP_MAX_5);
        levelMultiplierUpgradeLevel = clamp(prefs.getInt("mult_upgrade_level", 0), 0, WORKSHOP_MAX_5);
        bonusDurationUpgradeLevel = clamp(prefs.getInt("bonus_duration_upgrade", 0), 0, WORKSHOP_MAX_10);
        tempMultiplierUpgradeLevel = clamp(prefs.getInt("temp_multiplier_upgrade", 0), 0, WORKSHOP_MAX_5);
        comboTimeUpgradeLevel = clamp(prefs.getInt("combo_time_upgrade", 0), 0, WORKSHOP_MAX_5);
        shieldCapUpgradeLevel = clamp(prefs.getInt("shield_cap_upgrade", 0), 0, WORKSHOP_MAX_5);
        magnetRadiusUpgradeLevel = clamp(prefs.getInt("magnet_radius_upgrade", 0), 0, WORKSHOP_MAX_8);
        emergencyPhaseUpgradeLevel = clamp(prefs.getInt("emergency_phase_upgrade", 0), 0, WORKSHOP_MAX_5);
        secondWindUpgradeLevel = clamp(prefs.getInt("second_wind_upgrade", 0), 0, WORKSHOP_MAX_5);
        conduitBoostUpgradeLevel = clamp(prefs.getInt("conduit_boost_upgrade", 0), 0, WORKSHOP_MAX_5);
        totalTime = prefs.getFloat("stat_time", 0f);
        achMask = prefs.getInt("ach_mask", 0);
        if (prefs.contains("neon_earned")) {
            neonEarned = prefs.getLong("neon_earned", 0L);
        } else {
            neonEarned = pointsTotal;
        }
        // Миграция кошелька: раньше траты шли из pointsTotal.
        if (prefs.contains("neon")) {
            neon = prefs.getLong("neon", 0L);
        } else {
            neon = pointsTotal;
        }

        int savedMode = prefs.getInt("mode", MODE_CLASSIC);
        mode = (savedMode >= 0 && savedMode < MODE_COUNT) ? savedMode : MODE_CLASSIC;
        sphereSkin = clamp(prefs.getInt("skin_sphere", 0), 0, SPHERE_COUNT - 1);
        trailSkin = clamp(prefs.getInt("skin_trail", 0), 0, TRAIL_COUNT - 1);

        loadHistory();
    }

    private void loadHistory() {
        runHistory.clear();
        String raw = prefs.getString("run_history", "");
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String[] entries = raw.split(";");
        for (String entry : entries) {
            if (runHistory.size() >= HISTORY_LIMIT) {
                break;
            }
            String[] f = entry.split("\\|");
            if (f.length != 4) {
                continue;
            }
            try {
                int type = Integer.parseInt(f[0]);
                int score = Integer.parseInt(f[1]);
                int seconds = Integer.parseInt(f[2]);
                long timestamp = Long.parseLong(f[3]);
                if (type < 0 || type >= MODE_COUNT || score <= 0) {
                    continue;
                }
                runHistory.add(new RunRecord(type, score, Math.max(1, seconds),
                        timestamp));
            } catch (NumberFormatException ignored) {
                // Повреждённую запись просто пропускаем.
            }
        }
    }

    /** Добавляет завершённый забег в историю (последние 10, новые сверху). */
    void addRun(int type, int score, int seconds) {
        if (score <= 0) {
            return;
        }
        if (type < 0 || type >= MODE_COUNT) {
            type = MODE_CLASSIC;
        }
        runHistory.add(0, new RunRecord(type, score, Math.max(1, seconds),
                System.currentTimeMillis()));
        while (runHistory.size() > HISTORY_LIMIT) {
            runHistory.remove(runHistory.size() - 1);
        }
        saveHistory();
    }

    private void saveHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runHistory.size(); i++) {
            RunRecord r = runHistory.get(i);
            if (i > 0) {
                sb.append(';');
            }
            sb.append(r.type).append('|')
                    .append(r.score).append('|')
                    .append(r.seconds).append('|')
                    .append(r.timestamp);
        }
        prefs.edit().putString("run_history", sb.toString()).apply();
    }

    /** Возвращает маску достижений, открывшихся с прошлой проверки. */
    int refreshAchievements() {
        int fresh = 0;
        for (int i = 0; i < ACH_COUNT; i++) {
            int bit = 1 << i;
            if ((achMask & bit) == 0 && achUnlocked(i)) {
                achMask |= bit;
                fresh |= bit;
            }
        }
        if (fresh != 0) {
            saveStats();
        }
        return fresh;
    }

    boolean achUnlocked(int id) {
        switch (id) {
            case ACH_FIRST_RUN:
                return runs >= 1;
            case ACH_SCORE_1000:
                return best >= 1000;
            case ACH_SCORE_5000:
                return best >= 5000;
            case ACH_DODGE_500:
                return dodged >= 500;
            case ACH_COMBO_10:
                return bestCombo >= 10;
            case ACH_COMBO_25:
                return bestCombo >= 25;
            case ACH_STARS_100:
                return stars >= 100;
            case ACH_TIME_10:
                return totalTime >= 600f;
            case ACH_SHIELD:
                return shieldBlocks >= 1;
            case ACH_SURVIVE_120:
                return bestTime >= 120;
            case ACH_SCORE_10000:
                return best >= 10000;
            case ACH_COMBO_50:
                return bestCombo >= 50;
            case ACH_BOSS_DEFEATED:
                return bossesDefeated >= 1;
            default:
                return false;
        }
    }

    int achValue(int id) {
        switch (id) {
            case ACH_FIRST_RUN:
                return Math.min(runs, 1);
            case ACH_SCORE_1000:
            case ACH_SCORE_5000:
                return best;
            case ACH_DODGE_500:
                return dodged;
            case ACH_COMBO_10:
            case ACH_COMBO_25:
                return bestCombo;
            case ACH_STARS_100:
                return stars;
            case ACH_TIME_10:
                return (int) totalTime;
            case ACH_SHIELD:
                return shieldBlocks;
            case ACH_SURVIVE_120:
                return bestTime;
            case ACH_SCORE_10000:
                return best;
            case ACH_COMBO_50:
                return bestCombo;
            case ACH_BOSS_DEFEATED:
                return Math.min(bossesDefeated, 1);
            default:
                return 0;
        }
    }

    int achTarget(int id) {
        switch (id) {
            case ACH_FIRST_RUN:
                return 1;
            case ACH_SCORE_1000:
                return 1000;
            case ACH_SCORE_5000:
                return 5000;
            case ACH_DODGE_500:
                return 500;
            case ACH_COMBO_10:
                return 10;
            case ACH_COMBO_25:
                return 25;
            case ACH_STARS_100:
                return 100;
            case ACH_TIME_10:
                return 600;
            case ACH_SHIELD:
                return 1;
            case ACH_SURVIVE_120:
                return 120;
            case ACH_SCORE_10000:
                return 10000;
            case ACH_COMBO_50:
                return 50;
            case ACH_BOSS_DEFEATED:
                return 1;
            default:
                return 1;
        }
    }

    boolean sphereUnlocked(int id) {
        switch (id) {
            case 0:
                return true;
            case 1:
                return neonEarned >= 4000;
            case 2:
                return best >= 2500;
            case 3:
                return bestCombo >= 12;
            case 4:
                return dodged >= 300;
            case 5:
                return totalTime >= 900f;
            case 6:
                return neonEarned >= 12000;
            case 7:
                return bestCombo >= 30;
            default:
                return false;
        }
    }

    boolean trailUnlocked(int id) {
        switch (id) {
            case 0:
                return true;
            case 1:
                return stars >= 50;
            case 2:
                return best >= 4000;
            case 3:
                return dodged >= 500;
            case 4:
                return stars >= 150;
            case 5:
                return stars >= 350;
            case 6:
                return totalTime >= 1800f;
            default:
                return false;
        }
    }

    /** Доля пути до открытия сферы (0..1) — для полоски на закрытой карточке. */
    float sphereRatio(int id) {
        switch (id) {
            case 1:
                return ratio(neonEarned, 4000);
            case 2:
                return ratio(best, 2500);
            case 3:
                return ratio(bestCombo, 12);
            case 4:
                return ratio(dodged, 300);
            case 5:
                return ratio((int) totalTime, 900);
            case 6:
                return ratio(neonEarned, 12000);
            case 7:
                return ratio(bestCombo, 30);
            default:
                return 1f;
        }
    }

    /** Доля пути до открытия шлейфа (0..1). */
    float trailRatio(int id) {
        switch (id) {
            case 1:
                return ratio(stars, 50);
            case 2:
                return ratio(best, 4000);
            case 3:
                return ratio(dodged, 500);
            case 4:
                return ratio(stars, 150);
            case 5:
                return ratio(stars, 350);
            case 6:
                return ratio((int) totalTime, 1800);
            default:
                return 1f;
        }
    }

    private static float ratio(int value, int target) {
        return Math.max(0f, Math.min(1f, (float) value / target));
    }

    private static float ratio(long value, long target) {
        return Math.max(0f, Math.min(1f, (float) value / (float) target));
    }

    int bestForMode(int m) {
        return bestByMode[m < 0 || m >= MODE_COUNT ? MODE_CLASSIC : m];
    }

    /** Пишет результат в рекорд режима и в общий рекорд. */
    void recordScore(int m, int score) {
        if (m >= 0 && m < MODE_COUNT && score > bestByMode[m]) {
            bestByMode[m] = score;
        }
        if ((m == MODE_CLASSIC || m == MODE_HARDCORE) && score > best) {
            best = score;
        }
    }

    void saveStats() {
        SharedPreferences.Editor e = prefs.edit()
                .putInt("best", best)
                .putInt("stat_runs", runs)
                .putInt("stat_dodged", dodged)
                .putInt("stat_stars", stars)
                .putInt("stat_best_combo", bestCombo)
                .putInt("stat_best_time", bestTime)
                .putInt("stat_shields", shieldBlocks)
                .putInt("stat_bosses_defeated", bossesDefeated)
                .putInt("points_total", pointsTotal)
                .putLong("neon_earned", neonEarned)
                .putLong("neon", neon)
                .putInt("mult_upgrade", multiplierUpgradeLevel)
                .putInt("mult_upgrade_close", closeMultiplierUpgradeLevel)
                .putInt("mult_upgrade_boss", bossMultiplierUpgradeLevel)
                .putInt("mult_upgrade_level", levelMultiplierUpgradeLevel)
                .putInt("bonus_duration_upgrade", bonusDurationUpgradeLevel)
                .putInt("temp_multiplier_upgrade", tempMultiplierUpgradeLevel)
                .putInt("combo_time_upgrade", comboTimeUpgradeLevel)
                .putInt("shield_cap_upgrade", shieldCapUpgradeLevel)
                .putInt("magnet_radius_upgrade", magnetRadiusUpgradeLevel)
                .putInt("emergency_phase_upgrade", emergencyPhaseUpgradeLevel)
                .putInt("second_wind_upgrade", secondWindUpgradeLevel)
                .putInt("conduit_boost_upgrade", conduitBoostUpgradeLevel)
                .putFloat("stat_time", totalTime)
                .putInt("ach_mask", achMask);
        for (int i = 0; i < MODE_COUNT; i++) {
            e.putInt("best_mode_" + i, bestByMode[i]);
        }
        e.apply();
    }

    int levelMultiplierUpgradeCost() {
        return workshopCost(levelMultiplierUpgradeLevel, WORKSHOP_MAX_5, 2000);
    }

    int multiplierUpgradeCost() {
        return workshopCost(multiplierUpgradeLevel, WORKSHOP_MAX_10, 1500);
    }

    int closeMultiplierUpgradeCost() {
        return workshopCost(closeMultiplierUpgradeLevel, WORKSHOP_MAX_10, 1500);
    }

    int comboTimeUpgradeCost() {
        return workshopCost(comboTimeUpgradeLevel, WORKSHOP_MAX_5, 2500);
    }

    int shieldCapUpgradeCost() {
        return workshopCost(shieldCapUpgradeLevel, WORKSHOP_MAX_5, 3000);
    }

    int magnetRadiusUpgradeCost() {
        return workshopCost(magnetRadiusUpgradeLevel, WORKSHOP_MAX_8, 1200);
    }

    int emergencyPhaseUpgradeCost() {
        return workshopCost(emergencyPhaseUpgradeLevel, WORKSHOP_MAX_5, 3500);
    }

    int secondWindUpgradeCost() {
        return workshopCost(secondWindUpgradeLevel, WORKSHOP_MAX_5, 2000);
    }

    int bossMultiplierUpgradeCost() {
        return workshopCost(bossMultiplierUpgradeLevel, WORKSHOP_MAX_5, 3000);
    }

    int conduitBoostUpgradeCost() {
        return workshopCost(conduitBoostUpgradeLevel, WORKSHOP_MAX_5, 2500);
    }

    int bonusDurationUpgradeCost() {
        return workshopCost(bonusDurationUpgradeLevel, WORKSHOP_MAX_10, 1500);
    }

    int tempMultiplierUpgradeCost() {
        return workshopCost(tempMultiplierUpgradeLevel, WORKSHOP_MAX_5, 2500);
    }

    float startMultiplier() {
        return 1.0f + levelMultiplierUpgradeLevel * 0.20f;
    }

    float levelMultiplierStep() {
        return levelMultiplierUpgradeLevel * 0.20f;
    }

    float starMultiplierStep() {
        return 0.10f + multiplierUpgradeLevel * 0.02f;
    }

    float closeMultiplierStep() {
        return 0.10f + closeMultiplierUpgradeLevel * 0.02f;
    }

    float comboExtraTime() {
        return comboTimeUpgradeLevel * 0.15f;
    }

    int maxShieldCharges() {
        return shieldCapUpgradeLevel >= WORKSHOP_MAX_5 ? 2 : 1;
    }

    float shieldInvulnBonus() {
        return shieldCapUpgradeLevel * 0.30f;
    }

    float magnetRadiusScale() {
        return 1.0f + magnetRadiusUpgradeLevel * 0.15f;
    }

    boolean hasEmergencyPhase() {
        return emergencyPhaseUpgradeLevel > 0;
    }

    float emergencyPhaseDuration() {
        return 1.0f + emergencyPhaseUpgradeLevel * 0.40f;
    }

    float secondWindRateScale() {
        return 1.0f + secondWindUpgradeLevel * 0.15f;
    }

    float essenceNeonBonus() {
        return 1.0f + bossMultiplierUpgradeLevel * 0.20f;
    }

    float bossMultiplier() {
        return 2.0f + bossMultiplierUpgradeLevel * 0.20f;
    }

    float conduitDamageScale() {
        return 1.0f + conduitBoostUpgradeLevel * 0.20f;
    }

    float conduitStunBonus() {
        return conduitBoostUpgradeLevel * 0.40f;
    }

    float bonusDurationScale() {
        return 1.0f + bonusDurationUpgradeLevel * 0.10f;
    }

    float temporaryMultiplier() {
        return 2.0f + tempMultiplierUpgradeLevel * 0.60f;
    }

    int workshopTotalLevel() {
        return levelMultiplierUpgradeLevel + multiplierUpgradeLevel + closeMultiplierUpgradeLevel + comboTimeUpgradeLevel
                + shieldCapUpgradeLevel + magnetRadiusUpgradeLevel + emergencyPhaseUpgradeLevel + secondWindUpgradeLevel
                + bossMultiplierUpgradeLevel + conduitBoostUpgradeLevel + bonusDurationUpgradeLevel + tempMultiplierUpgradeLevel;
    }

    boolean isLevelMultiplierUnlocked() {
        return true;
    }

    boolean isBossMultiplierUnlocked() {
        return true;
    }

    boolean isCloseMultiplierUnlocked() {
        return true;
    }

    boolean isStarMultiplierUnlocked() {
        return true;
    }

    boolean canUpgradeLevelMultiplier() {
        int cost = levelMultiplierUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeMultiplier() {
        int cost = multiplierUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeCloseMultiplier() {
        int cost = closeMultiplierUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeComboTime() {
        int cost = comboTimeUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeShieldCap() {
        int cost = shieldCapUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeMagnetRadius() {
        int cost = magnetRadiusUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeEmergencyPhase() {
        int cost = emergencyPhaseUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeSecondWind() {
        int cost = secondWindUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeBossMultiplier() {
        int cost = bossMultiplierUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeConduitBoost() {
        int cost = conduitBoostUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeBonusDuration() {
        int cost = bonusDurationUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeTempMultiplier() {
        int cost = tempMultiplierUpgradeCost();
        return cost > 0 && neon >= cost;
    }

    boolean canUpgradeAnyWorkshop() {
        return canUpgradeLevelMultiplier() || canUpgradeMultiplier() || canUpgradeCloseMultiplier() || canUpgradeComboTime()
                || canUpgradeShieldCap() || canUpgradeMagnetRadius() || canUpgradeEmergencyPhase() || canUpgradeSecondWind()
                || canUpgradeBossMultiplier() || canUpgradeConduitBoost() || canUpgradeBonusDuration() || canUpgradeTempMultiplier();
    }

    boolean upgradeLevelMultiplier() {
        if (!canUpgradeLevelMultiplier()) {
            return false;
        }
        neon -= levelMultiplierUpgradeCost();
        levelMultiplierUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeMultiplier() {
        if (!canUpgradeMultiplier()) {
            return false;
        }
        neon -= multiplierUpgradeCost();
        multiplierUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeCloseMultiplier() {
        if (!canUpgradeCloseMultiplier()) {
            return false;
        }
        neon -= closeMultiplierUpgradeCost();
        closeMultiplierUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeComboTime() {
        if (!canUpgradeComboTime()) {
            return false;
        }
        neon -= comboTimeUpgradeCost();
        comboTimeUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeShieldCap() {
        if (!canUpgradeShieldCap()) {
            return false;
        }
        neon -= shieldCapUpgradeCost();
        shieldCapUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeMagnetRadius() {
        if (!canUpgradeMagnetRadius()) {
            return false;
        }
        neon -= magnetRadiusUpgradeCost();
        magnetRadiusUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeEmergencyPhase() {
        if (!canUpgradeEmergencyPhase()) {
            return false;
        }
        neon -= emergencyPhaseUpgradeCost();
        emergencyPhaseUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeSecondWind() {
        if (!canUpgradeSecondWind()) {
            return false;
        }
        neon -= secondWindUpgradeCost();
        secondWindUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeBossMultiplier() {
        if (!canUpgradeBossMultiplier()) {
            return false;
        }
        neon -= bossMultiplierUpgradeCost();
        bossMultiplierUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeConduitBoost() {
        if (!canUpgradeConduitBoost()) {
            return false;
        }
        neon -= conduitBoostUpgradeCost();
        conduitBoostUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeBonusDuration() {
        if (!canUpgradeBonusDuration()) {
            return false;
        }
        neon -= bonusDurationUpgradeCost();
        bonusDurationUpgradeLevel++;
        saveStats();
        return true;
    }

    boolean upgradeTempMultiplier() {
        if (!canUpgradeTempMultiplier()) {
            return false;
        }
        neon -= tempMultiplierUpgradeCost();
        tempMultiplierUpgradeLevel++;
        saveStats();
        return true;
    }

    /** Начисление неона: пополняет и кошелёк, и счётчик для ранга с защитой от переполнения. */
    void addNeon(int amount) {
        if (amount <= 0) {
            return;
        }
        pointsTotal = (int) Math.min(Integer.MAX_VALUE, (long) pointsTotal + amount);
        long room = Long.MAX_VALUE - amount;
        neonEarned = Math.min(room, neonEarned + amount);
        neon = Math.min(room, neon + amount);
    }

    int rank() {
        return GameRules.rankForNeon(neonEarned);
    }

    static int workshopCost(int level, int maxLevel, int base) {
        if (level >= maxLevel) {
            return 0;
        }
        double value = base * (1.0 + level * 0.55 + level * level * 0.10);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value));
    }

    static int workshopCost(int level, int base) {
        return workshopCost(level, WORKSHOP_MAX_10, base);
    }

    void saveMode() {
        prefs.edit().putInt("mode", mode).apply();
    }

    void saveSkins() {
        prefs.edit()
                .putInt("skin_sphere", sphereSkin)
                .putInt("skin_trail", trailSkin)
                .apply();
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
