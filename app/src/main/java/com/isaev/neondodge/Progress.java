package com.isaev.neondodge;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Долговременный прогресс: рекорд, суммарная статистика, достижения,
 * ежедневные задания и выбранные скины. Всё хранится только на устройстве.
 */
final class Progress {

    static final int MODE_CLASSIC = 0;
    static final int MODE_HARDCORE = 1;
    static final int MODE_ZEN = 2;
    static final int MODE_COUNT = 3;
    static final int RUN_DAILY = 3;
    static final int HISTORY_LIMIT = 10;

    static final int QUEST_STARS_RUN = 0;
    static final int QUEST_SCORE_RUN = 1;
    static final int QUEST_COMBO_RUN = 2;
    static final int QUEST_DODGE_RUN = 3;
    static final int QUEST_RUNS_DAY = 4;
    static final int QUEST_STARS_DAY = 5;
    static final int QUEST_TIME_DAY = 6;
    static final int QUEST_COUNT = 7;
    static final int[] QUEST_TARGETS = {10, 1500, 8, 80, 3, 30, 300};
    static final int QUEST_REWARD = 300;

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
    static final int ACH_COUNT = 10;

    static final int SPHERE_COUNT = 6;
    static final int TRAIL_COUNT = 5;

    private final SharedPreferences prefs;

    int best;
    final int[] bestByMode = new int[MODE_COUNT];
    int runs;
    int dodged;
    int stars;
    int bestCombo;
    int bestTime;
    int shieldBlocks;
    int questsDone;
    int pointsTotal;
    float totalTime;

    int dailyStamp;
    int dailyRuns;
    int dailyStars;
    float dailyTime;
    final int[] questIds = new int[3];
    final int[] questProgress = new int[3];
    final boolean[] questDone = new boolean[3];

    int challengeDate;
    boolean challengeDone;
    int challengeBest;
    int challengeScore;

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
        // Миграция со старой версии: единственный рекорд считаем рекордом
        // «Классики». Проверяем именно наличие ключей, иначе рекорд дневного
        // испытания при следующем запуске ошибочно попал бы в Classic.
        if (!hasModeBestKeys && best > 0) {
            bestByMode[MODE_CLASSIC] = best;
        }
        runs = prefs.getInt("stat_runs", 0);
        dodged = prefs.getInt("stat_dodged", 0);
        stars = prefs.getInt("stat_stars", 0);
        bestCombo = prefs.getInt("stat_best_combo", 0);
        bestTime = prefs.getInt("stat_best_time", 0);
        shieldBlocks = prefs.getInt("stat_shields", 0);
        questsDone = prefs.getInt("stat_quests", 0);
        pointsTotal = prefs.getInt("points_total", 0);
        totalTime = prefs.getFloat("stat_time", 0f);
        achMask = prefs.getInt("ach_mask", 0);

        int savedMode = prefs.getInt("mode", MODE_CLASSIC);
        mode = (savedMode >= 0 && savedMode < MODE_COUNT) ? savedMode : MODE_CLASSIC;
        sphereSkin = clamp(prefs.getInt("skin_sphere", 0), 0, SPHERE_COUNT - 1);
        trailSkin = clamp(prefs.getInt("skin_trail", 0), 0, TRAIL_COUNT - 1);

        dailyStamp = prefs.getInt("quest_date", 0);
        int today = todayStamp();
        boolean needSave = false;
        if (dailyStamp == today) {
            dailyRuns = prefs.getInt("quest_runs", 0);
            dailyStars = prefs.getInt("quest_stars", 0);
            dailyTime = prefs.getFloat("quest_time", 0f);
            for (int i = 0; i < 3; i++) {
                questProgress[i] = prefs.getInt("quest_p" + i, 0);
                questDone[i] = prefs.getBoolean("quest_d" + i, false);
            }
            // Раньше задания выбирались только по дате. Если набор уже
            // сохранён, не пересчитываем его: добавление нового типа задания
            // больше не сдвинет прогресс текущего дня.
            if (!loadQuestIds()) {
                selectQuests(dailyStamp);
                needSave = true;
            }
        } else {
            dailyStamp = today;
            resetDaily();
            selectQuests(today);
            needSave = true;
        }
        if (needSave) {
            saveDaily();
        }

        challengeDate = prefs.getInt("challenge_date", 0);
        if (challengeDate == today) {
            challengeDone = prefs.getBoolean("challenge_done", false);
            challengeBest = prefs.getInt("challenge_best", 0);
            challengeScore = prefs.getInt("challenge_score", 0);
        } else {
            resetChallenge(today);
            saveChallenge();
        }

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
                if (type < 0 || type > RUN_DAILY || score <= 0) {
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
        if (type < 0 || type > RUN_DAILY) {
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

    private boolean loadQuestIds() {
        for (int i = 0; i < 3; i++) {
            int id = prefs.getInt("quest_id_" + i, -1);
            if (id < 0 || id >= QUEST_COUNT) {
                return false;
            }
            for (int j = 0; j < i; j++) {
                if (questIds[j] == id) {
                    return false;
                }
            }
            questIds[i] = id;
        }
        return true;
    }

    /** Сбрасывает дневные задания, если наступил новый день. */
    boolean ensureDay() {
        int today = todayStamp();
        if (today == dailyStamp) {
            return false;
        }
        dailyStamp = today;
        resetDaily();
        selectQuests(today);
        saveDaily();
        resetChallenge(today);
        saveChallenge();
        return true;
    }

    private void resetChallenge(int stamp) {
        challengeDate = stamp;
        challengeDone = false;
        challengeBest = 0;
        challengeScore = 0;
    }

    boolean canPlayChallenge() {
        return !challengeDone;
    }

    /** Отмечает попытку использованной ещё до начала забега. */
    void startChallenge() {
        challengeDone = true;
        challengeScore = 0;
        saveChallenge();
    }

    /** Записывает результат испытания дня. */
    void recordChallengeScore(int score) {
        challengeScore = score;
        if (score > challengeBest) {
            challengeBest = score;
        }
        if (score > best) {
            best = score;
        }
    }

    private void resetDaily() {
        dailyRuns = 0;
        dailyStars = 0;
        dailyTime = 0f;
        for (int i = 0; i < 3; i++) {
            questProgress[i] = 0;
            questDone[i] = false;
        }
    }

    private void selectQuests(long stamp) {
        int[] pool = new int[QUEST_COUNT];
        for (int i = 0; i < QUEST_COUNT; i++) {
            pool[i] = i;
        }
        Random r = new Random(stamp * 0x9E3779B97F4A7C15L);
        for (int i = 0; i < 3; i++) {
            int j = i + r.nextInt(QUEST_COUNT - i);
            int t = pool[i];
            pool[i] = pool[j];
            pool[j] = t;
            questIds[i] = pool[i];
        }
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
            default:
                return 1;
        }
    }

    boolean sphereUnlocked(int id) {
        switch (id) {
            case 0:
                return true;
            case 1:
                return pointsTotal >= 4000;
            case 2:
                return best >= 2500;
            case 3:
                return bestCombo >= 12;
            case 4:
                return dodged >= 300;
            case 5:
                return totalTime >= 900f;
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
                return questsDone >= 5;
            default:
                return false;
        }
    }

    /** Доля пути до открытия сферы (0..1) — для полоски на закрытой карточке. */
    float sphereRatio(int id) {
        switch (id) {
            case 1:
                return ratio(pointsTotal, 4000);
            case 2:
                return ratio(best, 2500);
            case 3:
                return ratio(bestCombo, 12);
            case 4:
                return ratio(dodged, 300);
            case 5:
                return ratio((int) totalTime, 900);
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
                return ratio(questsDone, 5);
            default:
                return 1f;
        }
    }

    private static float ratio(int value, int target) {
        return Math.max(0f, Math.min(1f, (float) value / target));
    }

    int bestForMode(int m) {
        return bestByMode[m < 0 || m >= MODE_COUNT ? MODE_CLASSIC : m];
    }

    /** Пишет результат в рекорд режима и в общий рекорд. */
    void recordScore(int m, int score) {
        if (m >= 0 && m < MODE_COUNT && score > bestByMode[m]) {
            bestByMode[m] = score;
        }
        if (score > best) {
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
                .putInt("stat_quests", questsDone)
                .putInt("points_total", pointsTotal)
                .putFloat("stat_time", totalTime)
                .putInt("ach_mask", achMask)
                .putInt("challenge_date", challengeDate)
                .putBoolean("challenge_done", challengeDone)
                .putInt("challenge_best", challengeBest)
                .putInt("challenge_score", challengeScore);
        for (int i = 0; i < MODE_COUNT; i++) {
            e.putInt("best_mode_" + i, bestByMode[i]);
        }
        e.apply();
    }

    private void saveChallenge() {
        prefs.edit()
                .putInt("challenge_date", challengeDate)
                .putBoolean("challenge_done", challengeDone)
                .putInt("challenge_best", challengeBest)
                .putInt("challenge_score", challengeScore)
                .apply();
    }

    void saveDaily() {
        SharedPreferences.Editor e = prefs.edit()
                .putInt("quest_date", dailyStamp)
                .putInt("quest_runs", dailyRuns)
                .putInt("quest_stars", dailyStars)
                .putFloat("quest_time", dailyTime);
        for (int i = 0; i < 3; i++) {
            e.putInt("quest_id_" + i, questIds[i]);
            e.putInt("quest_p" + i, questProgress[i]);
            e.putBoolean("quest_d" + i, questDone[i]);
        }
        e.apply();
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

    private static int todayStamp() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) * 10000
                + (cal.get(Calendar.MONTH) + 1) * 100
                + cal.get(Calendar.DAY_OF_MONTH);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
