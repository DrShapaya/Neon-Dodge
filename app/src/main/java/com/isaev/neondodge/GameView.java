package com.isaev.neondodge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 * Вся игра нарисована на одном Canvas: фон, астероиды, частицы, HUD и меню.
 * Мир масштабируется по ширине экрана (1080 условных единиц).
 */
public class GameView extends View implements Choreographer.FrameCallback {

    private static final float W = 1080f;
    private static final float PLAYER_R = 36f;

    private static final float OFFSET_MIN = 0f;
    private static final float OFFSET_MAX = 300f;
    private static final float OFFSET_DEFAULT = 110f;

    private static final int ST_MENU = 0;
    private static final int ST_PLAY = 1;
    private static final int ST_PAUSE = 2;
    private static final int ST_OVER = 3;
    private static final int ST_SETTINGS = 4;
    private static final int ST_RULES = 5;
    private static final int ST_STATS = 6;
    private static final int ST_QUESTS = 7;
    private static final int ST_SKINS = 8;
    private static final int ST_HISTORY = 9;

    private static final int C_CYAN = 0xFF5FF3FF;
    private static final int C_MAGENTA = 0xFFFF4FD8;
    private static final int C_VIOLET = 0xFFA46BFF;
    private static final int C_ORANGE = 0xFFFF9E4F;
    private static final int C_LIME = 0xFFB6FF5C;
    private static final int C_GOLD = 0xFFFFD75E;
    private static final int C_RED = 0xFFFF4F6A;
    private static final int C_MINT = 0xFF7CF9D0;
    private static final int C_ICE = 0xFFC9A6FF;
    private static final int C_STEEL = 0xFFC4D2E0;

    // Pickup kinds.
    private static final int P_STAR = 0;
    private static final int P_HEART = 1;
    private static final int P_SHIELD = 2;
    private static final int P_MAGNET = 3;
    private static final int P_SLOW = 4;
    private static final int P_BOMB = 5;
    private static final int P_PHASE = 6;
    private static final int P_DOUBLE = 7;

    private static final float MAGNET_TIME = 8f;
    private static final float SLOW_TIME = 6f;
    private static final float SLOW_SCALE = 0.45f;
    private static final float PHASE_TIME = 6f;
    private static final float DOUBLE_TIME = 8f;
    private static final int MAX_LIVES = 3;

    private static final int CONTROL_ABOVE = 0;
    private static final int CONTROL_AT = 1;

    private static final float MULT_STEP = 0.2f;

    // Sphere / trail skin palettes.
    private static final int[] SPHERE_OUTER = {
            0xFF5FF3FF, 0xFFFF4FD8, 0xFFFFD75E, 0xFFB6FF5C, 0xFFA46BFF, 0xFFEFFBFF,
            0xFFFF7A45, 0xFF65E6C5
    };
    private static final int[] SPHERE_INNER = {
            0xFFBFFAFF, 0xFFFFB8F0, 0xFFFFF0B8, 0xFFEBFFC0, 0xFFDCC7FF, 0xFFFFFFFF,
            0xFFFFD0B8, 0xFFB9FFF0
    };
    private static final int[] TRAIL_COLORS = {
            0xAA5FF3FF, 0xCCFFD75E, 0xCCFF9E4F, 0x88A46BFF, 0xCCFF4FD8,
            0xCCFF6A45, 0xCC65E6C5
    };
    private static final float[] TRAIL_SIZE = {1f, 0.85f, 1.15f, 1.30f, 1f, 1.2f, 0.75f};
    private static final float[] TRAIL_RATE = {0.022f, 0.017f, 0.025f, 0.030f, 0.020f, 0.015f, 0.035f};

    // Asteroid types: colour tells the player which ability it has.
    private static final int T_GREEN = 0;   // bounces off walls
    private static final int T_BLUE = 1;    // increased fall speed
    private static final int T_VIOLET = 2;  // slight horizontal homing
    private static final int T_ORANGE = 3;  // moves in bursts
    private static final int T_RED = 4;     // increased size
    private static final int T_SPLIT = 5;   // breaks into two fragments
    private static final int T_MINE = 6;    // stops, blinks, then dashes down
    private static final int T_GRAVITY = 7; // pulls the player when close
    private static final int T_ARMOR = 8;   // survives a shield hit

    private static final int[] OBS_COLORS = {
            C_LIME, C_CYAN, C_VIOLET, C_ORANGE, C_RED,
            C_GOLD, C_MAGENTA, C_MINT, C_STEEL
    };

    // ---------- world / view ----------
    private float scale = 1f;
    private float worldH = 1920f;
    private int insetTopPx;
    private int insetBottomPx;
    private float insetTop;
    private float insetBottom;

    private final Random rnd = new Random();

    // ---------- state ----------
    private Progress progress;
    private int state = ST_MENU;
    private int rulesReturn = ST_MENU;
    private int skinTab = 0;
    private float time;
    private float elapsed;
    private float difficulty;
    private float latePressure;
    private float score;
    private boolean newRecord;
    private int lives = MAX_LIVES;

    private int mode = Progress.MODE_CLASSIC;
    private int maxLives = MAX_LIVES;
    private boolean hardcoreMode;
    private boolean zenMode;
    private boolean dailyChallenge;
    private Random gameRnd = new Random();
    private Random pickupRnd = new Random();
    private int runStars;
    private int runDodged;
    private int runMaxCombo;
    private int bossMask;
    private boolean scoreBanked;

    private float scoreMult = 1f;
    private float runMaxMult = 1f;
    private float closeMult = 1f;
    private float runMaxCloseMult = 1f;

    private boolean soundOn = true;
    private boolean vibOn = true;
    private boolean musicOn = true;
    private boolean tutorialDone;
    private float fingerOffset = OFFSET_DEFAULT;
    private boolean draggingOffset;
    private int controlMode = CONTROL_ABOVE;
    private int combo;
    private float comboTimer;
    private float comboPop;
    private int lastLevel;
    private float announceTimer;

    private int shieldCharges;
    private float magnetTimer;
    private float slowTimer;
    private float phaseTimer;
    private float doubleTimer;
    private float specialTimer;
    private float checkTimer;
    private float saveTimer;
    private String toastText = "";
    private float toastTimer;
    private float px = W / 2f;
    private float py = 1400f;
    private float targetX = px;
    private float targetY = py;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private float invuln;
    private float shake;
    private float flash;
    private float inputLock;
    private float overTime;

    private float spawnTimer;
    private float coinTimer;
    private float heartTimer;
    private float trailTimer;
    private float ambientTimer;
    private int shownScore = -1;
    private String shownScoreText = "0";

    private final ArrayList<Obs> obstacles = new ArrayList<>();
    private final ArrayList<Pickup> pickups = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();

    // ---------- background ----------
    private final float[] starX = new float[120];
    private final float[] starY = new float[120];
    private final float[] starSpd = new float[120];
    private final float[] starSize = new float[120];
    private final float[] starPh = new float[120];
    private Shader bgShader;
    private Shader nebulaOne;
    private Shader nebulaTwo;

    // ---------- paints / bitmaps ----------
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pGlow = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint pBg = new Paint();
    private final Paint pNebula = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFlash = new Paint();
    private final Paint pScrim = new Paint();
    private Shader hudScrim;
    private float hudScrimH = 240f;

    private Bitmap glowBmp;
    private final Rect srcGlow = new Rect(0, 0, 128, 128);
    private final RectF tmpRect = new RectF();
    private final HashMap<Integer, PorterDuffColorFilter> filters = new HashMap<>();

    private final Path hexPath = new Path();
    private final Path starPath = new Path();
    private final Path heartPath = new Path();
    private final Path shieldPath = new Path();
    private final Path iconPath = new Path();

    private Typeface tfBlack;
    private Typeface tfBold;
    private Typeface tfMed;
    private Typeface tfCond;

    // ---------- strings ----------
    private String sScore;
    private String sBest;
    private String sGameOver;
    private String sNewRecord;
    private String sPlayAgain;
    private String sPause;
    private String sTapContinue;
    private String sTapStart;
    private String sTagline;
    private String sHintMove;
    private String sToMenu;
    private String sSpeedUp;
    private String sCombo;
    private String sSound;
    private String sVibration;
    private String sMusic;
    private String sOn;
    private String sOff;
    private String sTutorial;
    private String sSettings;
    private String sOffset;
    private String sOffsetHint;
    private String sDone;
    private String sVersion;
    private String versionName;
    private String sRules;
    private String sRulesButton;
    private String sLegendAsteroids;
    private String sLegendBonuses;
    private String sRulesHowTo;
    private String sRulesMultiplier;
    private String sRulesMultiplierDesc;
    private String sRulesProgress;
    private String sRulesScrollHint;
    private String sMultiplier;
    private String sControlMode;
    private String sControlAbove;
    private String sControlAt;
    private String sControlAtHint;
    private String sStats;
    private String sAchievements;
    private String sHistory;
    private String sHistoryEmpty;
    private String sQuests;
    private String sSkins;
    private String sBack;
    private String sNeon;
    private String sSelected;
    private String sSelect;
    private String sQuestDone;
    private String sAllQuestsDone;
    private String sQuestReward;
    private String sAchievementUnlocked;
    private String sShieldActive;
    private String sShieldBlocked;
    private String sMagnetActive;
    private String sSlowActive;
    private String sBombActive;
    private String sPhaseActive;
    private String sDoubleActive;
    private String sSphere;
    private String sTrail;
    private String sZen;
    private String sDailyChallenge;
    private String sDailyShort;
    private String sDailyHint;
    private String sDailyDone;
    private String sDailyUsed;
    private String sRecords;
    private String sTotals;
    private String sHistoryCta;
    private String sQuestsSummary;
    private String sQuestsRefresh;
    private String sQuestToast;
    private String sExitConfirm;
    private String sReplayTutorial;
    private String sTutorialReset;
    private String sSoundSection;
    private final String[] sModeNames = new String[Progress.MODE_COUNT];
    private final String[] sModeHints = new String[Progress.MODE_COUNT];
    private final String[] sRuleLine = new String[7];
    private final String[] sRuleProgress = new String[3];
    private final String[] sAstName = new String[9];
    private final String[] sAstDesc = new String[9];
    private final String[] sBonusName = new String[8];
    private final String[] sBonusDesc = new String[8];
    private final String[] sStatName = new String[6];
    private final String[] sAchName = new String[Progress.ACH_COUNT];
    private final String[] sQuestName = new String[Progress.QUEST_COUNT];
    private final String[] sSphereName = new String[Progress.SPHERE_COUNT];
    private final String[] sTrailName = new String[Progress.TRAIL_COUNT];
    private final String[] sSphereCond = new String[Progress.SPHERE_COUNT];
    private final String[] sTrailCond = new String[Progress.TRAIL_COUNT];

    // ---------- audio ----------
    private SharedPreferences prefs;
    private SoundPool soundPool;
    private MediaPlayer musicPlayer;
    private int musicTrack;
    private int sndCoin;
    private int sndHit;
    private int sndPower;
    private Vibrator vibrator;

    // ---------- loop ----------
    private boolean running;
    private long lastNs;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setKeepScreenOn(false);

        prefs = context.getSharedPreferences("neon_dodge", Context.MODE_PRIVATE);
        progress = new Progress(context);
        mode = progress.mode;
        soundOn = prefs.getBoolean("sound", true);
        vibOn = prefs.getBoolean("vibrate", true);
        musicOn = prefs.getBoolean("music", true);
        tutorialDone = prefs.getBoolean("tutorial_done", false);
        fingerOffset = clamp(prefs.getFloat("finger_offset", OFFSET_DEFAULT),
                OFFSET_MIN, OFFSET_MAX);
        controlMode = prefs.getInt("control_mode", CONTROL_ABOVE);
        if (controlMode != CONTROL_ABOVE && controlMode != CONTROL_AT) {
            controlMode = CONTROL_ABOVE;
        }

        loadStrings(context);
        try {
            versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "";
        }

        initPaints();
        initStars();
        initAudio();
        resetMenu();
    }

    private void loadStrings(Context context) {
        sScore = context.getString(R.string.score);
        sBest = context.getString(R.string.best);
        sGameOver = context.getString(R.string.game_over);
        sNewRecord = context.getString(R.string.new_record);
        sPlayAgain = context.getString(R.string.play_again);
        sPause = context.getString(R.string.pause);
        sTapContinue = context.getString(R.string.tap_continue);
        sTapStart = context.getString(R.string.tap_start);
        sTagline = context.getString(R.string.menu_tagline);
        sHintMove = context.getString(R.string.hint_move);
        sToMenu = context.getString(R.string.to_menu);
        sSpeedUp = context.getString(R.string.speed_up);
        sCombo = context.getString(R.string.combo);
        sSound = context.getString(R.string.sound);
        sVibration = context.getString(R.string.vibration);
        sMusic = context.getString(R.string.music);
        sOn = context.getString(R.string.on);
        sOff = context.getString(R.string.off);
        sTutorial = context.getString(R.string.tutorial);
        sSettings = context.getString(R.string.settings);
        sControlMode = context.getString(R.string.control_mode);
        sControlAbove = context.getString(R.string.control_above);
        sControlAt = context.getString(R.string.control_at);
        sControlAtHint = context.getString(R.string.control_at_hint);
        sOffset = context.getString(R.string.sphere_offset);
        sOffsetHint = context.getString(R.string.sphere_offset_hint);
        sDone = context.getString(R.string.done);
        sVersion = context.getString(R.string.version);
        sMultiplier = context.getString(R.string.multiplier);

        sRules = context.getString(R.string.rules_title);
        sRulesButton = context.getString(R.string.rules_button);
        sRulesHowTo = context.getString(R.string.rules_how_to);
        sRulesMultiplier = context.getString(R.string.rules_multiplier);
        sRulesMultiplierDesc = context.getString(R.string.rules_multiplier_desc);
        sRulesProgress = context.getString(R.string.rules_progress);
        sRulesScrollHint = context.getString(R.string.rules_scroll_hint);
        sLegendAsteroids = context.getString(R.string.legend_asteroids);
        sLegendBonuses = context.getString(R.string.legend_bonuses);
        sStats = context.getString(R.string.stats_title);
        sAchievements = context.getString(R.string.achievements);
        sHistory = context.getString(R.string.history_title);
        sHistoryEmpty = context.getString(R.string.history_empty);
        sQuests = context.getString(R.string.quests_title);
        sSkins = context.getString(R.string.skins_title);
        sBack = context.getString(R.string.back);
        sNeon = context.getString(R.string.neon_points);
        sSelected = context.getString(R.string.skin_selected);
        sSelect = context.getString(R.string.skin_select);
        sQuestDone = context.getString(R.string.quest_done);
        sAllQuestsDone = context.getString(R.string.all_quests_done);
        sQuestReward = context.getString(R.string.quest_reward, Progress.QUEST_REWARD);
        sAchievementUnlocked = context.getString(R.string.achievement_unlocked);
        sShieldActive = context.getString(R.string.bonus_shield_active);
        sShieldBlocked = context.getString(R.string.bonus_shield_blocked);
        sMagnetActive = context.getString(R.string.bonus_magnet_active);
        sSlowActive = context.getString(R.string.bonus_slow_active);
        sBombActive = context.getString(R.string.bonus_bomb_active);
        sPhaseActive = context.getString(R.string.bonus_phase_active);
        sDoubleActive = context.getString(R.string.bonus_double_active);
        sSphere = context.getString(R.string.skins_sphere);
        sTrail = context.getString(R.string.skins_trail);
        sZen = context.getString(R.string.zen_hud);
        sDailyChallenge = context.getString(R.string.daily_challenge);
        sDailyShort = context.getString(R.string.daily_short);
        sDailyHint = context.getString(R.string.daily_hint);
        sDailyDone = context.getString(R.string.daily_done);
        sDailyUsed = context.getString(R.string.daily_used);
        sRecords = context.getString(R.string.records);
        sTotals = context.getString(R.string.totals);
        sHistoryCta = context.getString(R.string.history_cta);
        sQuestsSummary = context.getString(R.string.quests_summary);
        sQuestsRefresh = context.getString(R.string.quests_refresh);
        sQuestToast = context.getString(R.string.quest_toast, Progress.QUEST_REWARD);
        sExitConfirm = context.getString(R.string.exit_confirm);
        sReplayTutorial = context.getString(R.string.replay_tutorial);
        sTutorialReset = context.getString(R.string.tutorial_reset);
        sSoundSection = context.getString(R.string.sound_section);

        sModeNames[Progress.MODE_CLASSIC] = context.getString(R.string.mode_classic);
        sModeNames[Progress.MODE_HARDCORE] = context.getString(R.string.mode_hardcore);
        sModeNames[Progress.MODE_ZEN] = context.getString(R.string.mode_zen);
        sModeHints[Progress.MODE_CLASSIC] = context.getString(R.string.mode_classic_hint);
        sModeHints[Progress.MODE_HARDCORE] = context.getString(R.string.mode_hardcore_hint);
        sModeHints[Progress.MODE_ZEN] = context.getString(R.string.mode_zen_hint);

        sRuleLine[0] = context.getString(R.string.rule_1);
        sRuleLine[1] = context.getString(R.string.rule_2);
        sRuleLine[2] = context.getString(R.string.rule_3);
        sRuleLine[3] = context.getString(R.string.rule_4);
        sRuleLine[4] = context.getString(R.string.rule_5);
        sRuleLine[5] = context.getString(R.string.rule_6);
        sRuleLine[6] = context.getString(R.string.rule_7);
        sRuleProgress[0] = context.getString(R.string.rule_progress_1);
        sRuleProgress[1] = context.getString(R.string.rule_progress_2);
        sRuleProgress[2] = context.getString(R.string.rule_progress_3);

        sAstName[0] = context.getString(R.string.ast_green);
        sAstName[1] = context.getString(R.string.ast_blue);
        sAstName[2] = context.getString(R.string.ast_violet);
        sAstName[3] = context.getString(R.string.ast_orange);
        sAstName[4] = context.getString(R.string.ast_red);
        sAstName[5] = context.getString(R.string.ast_split);
        sAstName[6] = context.getString(R.string.ast_mine);
        sAstName[7] = context.getString(R.string.ast_gravity);
        sAstName[8] = context.getString(R.string.ast_armor);
        sAstDesc[0] = context.getString(R.string.ast_green_desc);
        sAstDesc[1] = context.getString(R.string.ast_blue_desc);
        sAstDesc[2] = context.getString(R.string.ast_violet_desc);
        sAstDesc[3] = context.getString(R.string.ast_orange_desc);
        sAstDesc[4] = context.getString(R.string.ast_red_desc);
        sAstDesc[5] = context.getString(R.string.ast_split_desc);
        sAstDesc[6] = context.getString(R.string.ast_mine_desc);
        sAstDesc[7] = context.getString(R.string.ast_gravity_desc);
        sAstDesc[8] = context.getString(R.string.ast_armor_desc);

        sBonusName[0] = context.getString(R.string.bonus_star);
        sBonusName[1] = context.getString(R.string.bonus_heart);
        sBonusName[2] = context.getString(R.string.bonus_shield);
        sBonusName[3] = context.getString(R.string.bonus_magnet);
        sBonusName[4] = context.getString(R.string.bonus_slow);
        sBonusName[5] = context.getString(R.string.bonus_bomb);
        sBonusName[6] = context.getString(R.string.bonus_phase);
        sBonusName[7] = context.getString(R.string.bonus_double);
        sBonusDesc[0] = context.getString(R.string.bonus_star_desc);
        sBonusDesc[1] = context.getString(R.string.bonus_heart_desc);
        sBonusDesc[2] = context.getString(R.string.bonus_shield_desc);
        sBonusDesc[3] = context.getString(R.string.bonus_magnet_desc);
        sBonusDesc[4] = context.getString(R.string.bonus_slow_desc);
        sBonusDesc[5] = context.getString(R.string.bonus_bomb_desc);
        sBonusDesc[6] = context.getString(R.string.bonus_phase_desc);
        sBonusDesc[7] = context.getString(R.string.bonus_double_desc);

        sStatName[0] = context.getString(R.string.stat_dodged);
        sStatName[1] = context.getString(R.string.stat_combo);
        sStatName[2] = context.getString(R.string.stat_time);
        sStatName[3] = context.getString(R.string.stat_stars);
        sStatName[4] = context.getString(R.string.stat_runs);
        sStatName[5] = context.getString(R.string.stat_neon);

        sAchName[Progress.ACH_FIRST_RUN] = context.getString(R.string.ach_first_run);
        sAchName[Progress.ACH_SCORE_1000] = context.getString(R.string.ach_score_1000);
        sAchName[Progress.ACH_SCORE_5000] = context.getString(R.string.ach_score_5000);
        sAchName[Progress.ACH_DODGE_500] = context.getString(R.string.ach_dodge_500);
        sAchName[Progress.ACH_COMBO_10] = context.getString(R.string.ach_combo_10);
        sAchName[Progress.ACH_COMBO_25] = context.getString(R.string.ach_combo_25);
        sAchName[Progress.ACH_STARS_100] = context.getString(R.string.ach_stars_100);
        sAchName[Progress.ACH_TIME_10] = context.getString(R.string.ach_time_10);
        sAchName[Progress.ACH_SHIELD] = context.getString(R.string.ach_shield);
        sAchName[Progress.ACH_SURVIVE_120] = context.getString(R.string.ach_survive_120);

        sQuestName[Progress.QUEST_STARS_RUN] = context.getString(R.string.quest_stars_run);
        sQuestName[Progress.QUEST_SCORE_RUN] = context.getString(R.string.quest_score_run);
        sQuestName[Progress.QUEST_COMBO_RUN] = context.getString(R.string.quest_combo_run);
        sQuestName[Progress.QUEST_DODGE_RUN] = context.getString(R.string.quest_dodge_run);
        sQuestName[Progress.QUEST_RUNS_DAY] = context.getString(R.string.quest_runs_day);
        sQuestName[Progress.QUEST_STARS_DAY] = context.getString(R.string.quest_stars_day);
        sQuestName[Progress.QUEST_TIME_DAY] = context.getString(R.string.quest_time_day);

        sSphereName[0] = context.getString(R.string.skin_cyan);
        sSphereName[1] = context.getString(R.string.skin_magenta);
        sSphereName[2] = context.getString(R.string.skin_gold);
        sSphereName[3] = context.getString(R.string.skin_lime);
        sSphereName[4] = context.getString(R.string.skin_violet);
        sSphereName[5] = context.getString(R.string.skin_white);
        sSphereName[6] = context.getString(R.string.skin_sunset);
        sSphereName[7] = context.getString(R.string.skin_aqua);

        sTrailName[0] = context.getString(R.string.trail_neon);
        sTrailName[1] = context.getString(R.string.trail_sparks);
        sTrailName[2] = context.getString(R.string.trail_flame);
        sTrailName[3] = context.getString(R.string.trail_ghost);
        sTrailName[4] = context.getString(R.string.trail_magnet);
        sTrailName[5] = context.getString(R.string.trail_comet);
        sTrailName[6] = context.getString(R.string.trail_pulse);

        sSphereCond[0] = context.getString(R.string.skin_free);
        sSphereCond[1] = context.getString(R.string.unlock_points, fmt(4000));
        sSphereCond[2] = context.getString(R.string.unlock_best, fmt(2500));
        sSphereCond[3] = context.getString(R.string.unlock_combo, fmt(12));
        sSphereCond[4] = context.getString(R.string.unlock_dodged, fmt(300));
        sSphereCond[5] = context.getString(R.string.unlock_time, formatTime(900));
        sSphereCond[6] = context.getString(R.string.unlock_points, fmt(12000));
        sSphereCond[7] = context.getString(R.string.unlock_combo, fmt(30));
        sTrailCond[0] = context.getString(R.string.skin_free);
        sTrailCond[1] = context.getString(R.string.unlock_stars, fmt(50));
        sTrailCond[2] = context.getString(R.string.unlock_best, fmt(4000));
        sTrailCond[3] = context.getString(R.string.unlock_dodged, fmt(500));
        sTrailCond[4] = context.getString(R.string.unlock_quests, fmt(5));
        sTrailCond[5] = context.getString(R.string.unlock_stars, fmt(250));
        sTrailCond[6] = context.getString(R.string.unlock_time, formatTime(1800));
    }


    private void initPaints() {
        pFill.setStyle(Paint.Style.FILL);
        pStroke.setStyle(Paint.Style.FILL);
        pFlash.setStyle(Paint.Style.FILL);

        tfBlack = Typeface.create("sans-serif-black", Typeface.BOLD);
        tfBold = Typeface.create("sans-serif", Typeface.BOLD);
        tfMed = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        tfCond = Typeface.create("sans-serif-condensed", Typeface.BOLD);

        glowBmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas gc = new Canvas(glowBmp);
        Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
        gp.setShader(new RadialGradient(
                64f, 64f, 64f,
                new int[]{0xFFFFFFFF, 0xB0FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.28f, 1f},
                Shader.TileMode.CLAMP));
        gc.drawCircle(64f, 64f, 64f, gp);

        heartPath.moveTo(0f, 0.78f);
        heartPath.cubicTo(-0.14f, 0.64f, -0.96f, 0.06f, -0.96f, -0.36f);
        heartPath.cubicTo(-0.96f, -0.76f, -0.60f, -1.0f, -0.30f, -1.0f);
        heartPath.cubicTo(-0.12f, -1.0f, -0.02f, -0.90f, 0f, -0.80f);
        heartPath.cubicTo(0.02f, -0.90f, 0.12f, -1.0f, 0.30f, -1.0f);
        heartPath.cubicTo(0.60f, -1.0f, 0.96f, -0.76f, 0.96f, -0.36f);
        heartPath.cubicTo(0.96f, 0.06f, 0.14f, 0.64f, 0f, 0.78f);
        heartPath.close();

        shieldPath.moveTo(0f, -1f);
        shieldPath.cubicTo(0.34f, -0.82f, 0.76f, -0.74f, 0.88f, -0.60f);
        shieldPath.lineTo(0.80f, 0.22f);
        shieldPath.cubicTo(0.70f, 0.68f, 0.34f, 0.90f, 0f, 1f);
        shieldPath.cubicTo(-0.34f, 0.90f, -0.70f, 0.68f, -0.80f, 0.22f);
        shieldPath.lineTo(-0.88f, -0.60f);
        shieldPath.cubicTo(-0.76f, -0.74f, -0.34f, -0.82f, 0f, -1f);
        shieldPath.close();
    }

    private void initStars() {
        for (int i = 0; i < starX.length; i++) {
            starX[i] = rnd.nextFloat() * W;
            starY[i] = rnd.nextFloat() * worldH;
            starSpd[i] = 8f + rnd.nextFloat() * 46f;
            starSize[i] = 1.2f + rnd.nextFloat() * 2.6f;
            starPh[i] = rnd.nextFloat() * 6.283f;
        }
    }

    private void initAudio() {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(attrs)
                    .build();
            sndCoin = soundPool.load(getContext(), R.raw.coin, 1);
            sndHit = soundPool.load(getContext(), R.raw.hit, 1);
            sndPower = soundPool.load(getContext(), R.raw.power, 1);
        } catch (Exception e) {
            soundPool = null;
        }
        try {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            vibrator = null;
        }
    }

    private void initMusic() {
        if (musicPlayer != null) {
            return;
        }
        try {
            musicPlayer = MediaPlayer.create(getContext(),
                    musicTrack == 0 ? R.raw.music : R.raw.music_alt);
            if (musicPlayer != null) {
                musicPlayer.setLooping(true);
                musicPlayer.setVolume(0.45f, 0.45f);
                if (musicOn) {
                    musicPlayer.start();
                }
            }
        } catch (Exception e) {
            musicPlayer = null;
        }
    }

    private void startMusicIfEnabled() {
        if (!musicOn) {
            return;
        }
        if (musicPlayer == null) {
            initMusic();
            return;
        }
        try {
            if (!musicPlayer.isPlaying()) {
                musicPlayer.start();
            }
        } catch (Exception ignored) {
        }
    }

    private void pauseMusic() {
        if (musicPlayer == null) {
            return;
        }
        try {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
            }
        } catch (Exception ignored) {
        }
    }

    private void toggleMusic() {
        musicOn = !musicOn;
        prefs.edit().putBoolean("music", musicOn).apply();
        if (musicOn) {
            startMusicIfEnabled();
        } else {
            pauseMusic();
        }
    }

    private void releaseMusic() {
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                musicPlayer.release();
            } catch (Exception ignored) {
            }
            musicPlayer = null;
        }
    }

    private void releaseAudio() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    // ==================================================================
    //  layout
    // ==================================================================

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        scale = w / W;
        worldH = h / scale;

        bgShader = new LinearGradient(0f, 0f, 0f, worldH,
                0xFF05070F, 0xFF0B1730, Shader.TileMode.CLAMP);
        nebulaOne = new RadialGradient(W * 0.18f, worldH * 0.10f, W * 1.15f,
                new int[]{0x2E5A35FF, 0x00000000}, null, Shader.TileMode.CLAMP);
        nebulaTwo = new RadialGradient(W * 0.92f, worldH * 0.58f, W * 0.95f,
                new int[]{0x2635E0C0, 0x00000000}, null, Shader.TileMode.CLAMP);

        recalcInsets();
        rebuildScrim();
        if (state == ST_MENU || state == ST_OVER || state == ST_SETTINGS
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
                || state == ST_HISTORY) {
            px = W / 2f;
            targetX = px;
            py = groundY();
            targetY = py;
        }
    }    private float touchTargetY(float touchY) {
        return clamp(touchY - effectiveFingerOffset(), playerMinY(), playerMaxY());
    }

    private float effectiveFingerOffset() {
        return controlMode == CONTROL_AT ? 0f : fingerOffset;
    }

    private void recalcInsets() {
        if (scale <= 0f) {
            return;
        }
        insetTop = insetTopPx / scale;
        insetBottom = insetBottomPx / scale;
    }

    public void setSafeInsets(int topPx, int bottomPx) {
        insetTopPx = topPx;
        insetBottomPx = bottomPx;
        recalcInsets();
        rebuildScrim();
        if (state == ST_MENU || state == ST_SETTINGS || state == ST_OVER
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
                || state == ST_HISTORY) {
            py = groundY();
            targetY = py;
        }
    }

    private void rebuildScrim() {
        float h = Math.max(240f, insetTop + 220f);
        hudScrimH = h;
        hudScrim = new LinearGradient(0f, 0f, 0f, h,
                new int[]{0xCC05070F, 0x8005070F, 0x0005070F},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP);
    }

    private float groundY() {
        return clamp(worldH - 250f - insetBottom, playerMinY(), playerMaxY());
    }

    private float playerMinY() {
        return Math.max(PLAYER_R + 24f, insetTop + 210f);
    }

    private float playerMaxY() {
        return Math.max(playerMinY() + 1f,
                worldH - insetBottom - PLAYER_R - 24f);
    }

    // ==================================================================
    //  loop
    // ==================================================================

    private void startLoop() {
        if (running) {
            return;
        }
        running = true;
        lastNs = 0L;
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void stopLoop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        if (lastNs == 0L) {
            lastNs = frameTimeNanos;
        }
        float dt = (frameTimeNanos - lastNs) / 1_000_000_000f;
        lastNs = frameTimeNanos;
        if (dt > 0.05f) {
            dt = 0.05f;
        }
        if (dt < 0f) {
            dt = 0f;
        }
        update(dt);
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (soundPool == null) {
            initAudio();
        }
        startLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        releaseAudio();
        releaseMusic();
        super.onDetachedFromWindow();
    }

    public void onHostPause() {
        if (state == ST_PLAY) {
            pauseGame();
        }
        draggingOffset = false;
        pressedId = B_NONE;
        scrollDragging = false;
        progress.saveStats();
        progress.saveDaily();
        pauseMusic();
        stopLoop();
    }

    public void onHostResume() {
        progress.ensureDay();
        // Не блокируем первый кадр меню декодированием OGG; музыка стартует
        // при начале забега или возвращении из паузы.
        if (state == ST_PLAY || state == ST_PAUSE) {
            startMusicIfEnabled();
        }
        startLoop();
    }

    // ==================================================================
    //  update
    // ==================================================================

    private void update(float dt) {
        time += dt;
        if (inputLock > 0f) {
            inputLock -= dt;
        }
        if (flash > 0f) {
            flash = Math.max(0f, flash - dt * 2.2f);
        }
        if (shake > 0f) {
            shake = Math.max(0f, shake - dt * 95f);
        }
        if (announceTimer > 0f) {
            announceTimer -= dt;
        }
        if (comboPop > 0f) {
            comboPop = Math.max(0f, comboPop - dt * 3.2f);
        }
        if (toastTimer > 0f) {
            toastTimer = Math.max(0f, toastTimer - dt);
        }
        updateUi(dt);
        if (state == ST_PLAY && resumeTimer <= 0f) {
            if (magnetTimer > 0f) {
                magnetTimer = Math.max(0f, magnetTimer - dt);
            }
            if (slowTimer > 0f) {
                slowTimer = Math.max(0f, slowTimer - dt);
            }
        }

        updateStars(dt);
        updateParticles(dt);

        if (state == ST_PLAY) {
            if (resumeTimer > 0f) {
                resumeTimer = Math.max(0f, resumeTimer - dt);
                updateResume(dt);
            } else {
                updatePlay(dt);
            }
        } else if (state == ST_MENU || state == ST_SETTINGS
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
                || state == ST_HISTORY) {
            updateMenu(dt);
        } else if (state == ST_OVER) {
            overTime += dt;
        }
    }

    /** Во время отсчёта мир стоит, а сфера уже слушается пальца. */
    private void updateResume(float dt) {
        targetX = clamp(targetX, PLAYER_R + 6f, W - PLAYER_R - 6f);
        targetY = clamp(targetY, playerMinY(), playerMaxY());
        float k = 1f - (float) Math.exp(-16f * dt);
        followTarget(k);
        px = clamp(px, PLAYER_R + 6f, W - PLAYER_R - 6f);
        py = clamp(py, playerMinY(), playerMaxY());
        invuln = Math.max(invuln, 0.5f);
    }

    private void updateStars(float dt) {
        float extra = state == ST_PLAY && resumeTimer <= 0f ? 40f + difficulty * 90f : 0f;
        for (int i = 0; i < starX.length; i++) {
            starY[i] += (starSpd[i] + extra) * dt;
            if (starY[i] > worldH + 12f) {
                starY[i] = -12f;
                starX[i] = rnd.nextFloat() * W;
            }
        }
    }

    private void updateMenu(float dt) {
        ambientTimer -= dt;
        if (ambientTimer <= 0f) {
            ambientTimer = 0.10f;
            int col = rnd.nextBoolean() ? 0xAA5FF3FF : 0xAAFF4FD8;
            addParticle(rnd.nextFloat() * W, worldH + 16f,
                    (rnd.nextFloat() - 0.5f) * 30f,
                    -(30f + rnd.nextFloat() * 70f),
                    2.4f + rnd.nextFloat() * 2.6f,
                    1.8f + rnd.nextFloat() * 3.4f,
                    col, 0f);
        }
    }

    private void updatePlay(float dt) {
        elapsed += dt;
        float diffCap = zenMode ? 0.60f : 1f;
        float diffTime = zenMode ? 120f : 90f;
        difficulty = Math.min(diffCap, elapsed / diffTime);
        // После достижения потолка сложность не останавливается: давление
        // растёт ещё несколько минут, но медленно и с жёстким лимитом.
        float late = zenMode ? 0f : Math.max(0f, elapsed - diffTime);
        latePressure = Math.min(0.30f, late / 300f);
        score += dt * (70f + 120f * difficulty)
                * (1f + 0.25f * latePressure) * scoreFactor();
        progress.totalTime += dt;
        progress.dailyTime += dt;
        if ((int) elapsed > progress.bestTime) {
            progress.bestTime = (int) elapsed;
        }

        if (comboTimer > 0f) {
            comboTimer -= dt;
            if (comboTimer <= 0f) {
                combo = 0;
            }
        }

        int level = (int) (elapsed / 22.5f);
        if (level > lastLevel) {
            lastLevel = level;
            if (level <= 4) {
                announceTimer = 1.7f;
            }
        }

        if (invuln > 0f) {
            invuln -= dt;
        }
        phaseTimer = Math.max(0f, phaseTimer - dt);
        doubleTimer = Math.max(0f, doubleTimer - dt);

        // Mini-bosses arrive at 2, 4 and 6 minutes and create a clear tempo spike.
        int minute = (int) (elapsed / 60f);
        if (minute >= 2 && minute <= 6 && (minute & 1) == 0) {
            int bit = 1 << (minute / 2 - 1);
            if ((bossMask & bit) == 0) {
                bossMask |= bit;
                spawnBoss(minute);
                announceTimer = 2.4f;
            }
        }

        targetX = clamp(targetX, PLAYER_R + 6f, W - PLAYER_R - 6f);
        targetY = clamp(targetY, playerMinY(), playerMaxY());

        float k = 1f - (float) Math.exp(-16f * dt);
        followTarget(k);
        px = clamp(px, PLAYER_R + 6f, W - PLAYER_R - 6f);
        py = clamp(py, playerMinY(), playerMaxY());

        trailTimer -= dt;
        if (trailTimer <= 0f) {
            int ti = clamp(progress.trailSkin, 0, Progress.TRAIL_COUNT - 1);
            trailTimer = TRAIL_RATE[ti];
            addParticle(px + (rnd.nextFloat() - 0.5f) * 22f, py + PLAYER_R * 0.7f,
                    (rnd.nextFloat() - 0.5f) * 50f,
                    40f + rnd.nextFloat() * 80f,
                    0.35f + rnd.nextFloat() * 0.30f,
                    (3f + rnd.nextFloat() * 5f) * TRAIL_SIZE[ti],
                    TRAIL_COLORS[ti], 0f);
        }

        float sdt = dt * slowFactor();

        spawnTimer -= sdt;
        if (spawnTimer <= 0f && elapsed > 0.7f) {
            spawnObstacle();
            float base;
            if (zenMode) {
                base = 1.05f - 0.30f * difficulty;
            } else if (hardcoreMode) {
                base = 0.72f - 0.42f * difficulty;
            } else {
                base = 0.78f - 0.40f * difficulty;
            }
            spawnTimer = base * (1f - 0.25f * latePressure)
                    * (0.72f + gameRnd.nextFloat() * 0.62f);
        }

        coinTimer -= dt;
        if (coinTimer <= 0f) {
            spawnPickup(P_STAR);
            coinTimer = 3.0f + pickupRnd.nextFloat() * 2.6f;
        }

        if (lives < maxLives) {
            heartTimer -= dt;
            if (heartTimer <= 0f) {
                spawnPickup(P_HEART);
                heartTimer = 24f;
            }
        }

        specialTimer -= dt;
        if (specialTimer <= 0f && elapsed > 3f) {
            float v = pickupRnd.nextFloat();
            if (v < 0.20f) {
                spawnPickup(P_SHIELD);
            } else if (v < 0.38f) {
                spawnPickup(P_MAGNET);
            } else if (v < 0.56f) {
                spawnPickup(P_SLOW);
            } else if (v < 0.72f) {
                spawnPickup(P_BOMB);
            } else if (v < 0.87f) {
                spawnPickup(P_PHASE);
            } else {
                spawnPickup(P_DOUBLE);
            }
            specialTimer = 15f + pickupRnd.nextFloat() * 9f;
        }

        updateObstacles(dt);
        updatePickups(dt);

        checkTimer -= dt;
        if (checkTimer <= 0f) {
            checkTimer = 0.25f;
            checkProgress();
        }
        saveTimer -= dt;
        if (saveTimer <= 0f) {
            saveTimer = 5f;
            progress.saveStats();
            progress.saveDaily();
        }
    }

    private void followTarget(float k) {
        // Гравитационные астероиды смещают точку, к которой стремится сфера.
        // Игрок может компенсировать притяжение пальцем.
        float pullX = 0f;
        float pullY = 0f;
        for (int i = 0; i < obstacles.size(); i++) {
            Obs o = obstacles.get(i);
            if (o.type != T_GRAVITY) {
                continue;
            }
            float dx = o.x - px;
            float dy = o.y - py;
            float d2 = dx * dx + dy * dy;
            float range = 340f;
            if (d2 < 1f || d2 > range * range) {
                continue;
            }
            float d = (float) Math.sqrt(d2);
            float strength = 92f * (1f - d / range);
            pullX += dx / d * strength;
            pullY += dy / d * strength;
        }
        float pullLen = (float) Math.sqrt(pullX * pullX + pullY * pullY);
        if (pullLen > 132f) {
            pullX *= 132f / pullLen;
            pullY *= 132f / pullLen;
        }
        px += (targetX + pullX - px) * k;
        py += (targetY + pullY - py) * k;
    }

    private float slowFactor() {
        return slowTimer > 0f ? SLOW_SCALE : 1f;
    }

    private float scoreFactor() {
        return scoreMult * closeMult * (doubleTimer > 0f ? 2f : 1f);
    }

    private void updateObstacles(float dt) {
        float sdt = dt * slowFactor();
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obs o = obstacles.get(i);

            if (o.boss) {
                o.vy = o.baseVy * (0.72f + 0.28f *
                        (0.5f + 0.5f * (float) Math.sin(time * (1.5f + o.bossStage * 0.35f))));
                o.vx += (float) Math.sin(time * 0.7f + o.bossStage) * sdt * 35f;
            } else if (o.type == T_ORANGE) {
                o.burstPhase += sdt;
                if (o.burstPhase >= 1.05f) {
                    o.burstPhase -= 1.05f;
                }
                o.vy = o.baseVy * (o.burstPhase < 0.30f ? 2.15f : 0.28f);
            } else if (o.type == T_VIOLET) {
                float desired = clamp((px - o.x) * 0.9f, -70f, 70f);
                o.vx += (desired - o.vx) * Math.min(1f, sdt * 1.1f);
                o.vy = o.baseVy;
            } else if (o.type == T_MINE) {
                if (!o.mineArmed) {
                    o.vy = o.baseVy * 0.55f;
                } else {
                    o.fuse += sdt;
                    // Короткая пауза с миганием, затем резкий рывок вниз.
                    o.vy = o.fuse < 0.85f ? 0f : o.baseVy * 2.35f;
                }
            } else {
                o.vy = o.baseVy;
            }

            o.y += o.vy * sdt;
            o.x += o.vx * sdt;

            if (o.type == T_MINE && !o.mineArmed && o.y >= o.stopY) {
                o.y = o.stopY;
                o.mineArmed = true;
                o.fuse = 0f;
                o.vy = 0f;
            }

            if (o.type == T_SPLIT && o.canSplit && o.y >= o.splitY) {
                obstacles.remove(i);
                spawnSplitFragments(o);
                continue;
            }

            if (o.type == T_GREEN || o.boss) {
                if (o.x < o.r + 8f) {
                    o.x = o.r + 8f;
                    o.vx = Math.abs(o.vx);
                    bounceOff(o);
                } else if (o.x > W - o.r - 8f) {
                    o.x = W - o.r - 8f;
                    o.vx = -Math.abs(o.vx);
                    bounceOff(o);
                }
            } else {
                if (o.x < o.r + 8f) {
                    o.x = o.r + 8f;
                    if (o.vx < 0f) {
                        o.vx = 0f;
                    }
                } else if (o.x > W - o.r - 8f) {
                    o.x = W - o.r - 8f;
                    if (o.vx > 0f) {
                        o.vx = 0f;
                    }
                }
            }

            if (o.bounceFlash > 0f) {
                o.bounceFlash = Math.max(0f, o.bounceFlash - dt * 3.5f);
            }
            o.rot += o.vrot * sdt;

            if (!o.passed && o.y > py + PLAYER_R * 0.5f) {
                o.passed = true;
                runDodged++;
                progress.dodged++;
                float dx = Math.abs(o.x - px);
                if (invuln <= 0f && dx < o.r + PLAYER_R + 92f) {
                    combo++;
                    if (combo > runMaxCombo) {
                        runMaxCombo = combo;
                    }
                    if (combo > progress.bestCombo) {
                        progress.bestCombo = combo;
                    }
                    comboTimer = 2.2f;
                    comboPop = 1f;
                    closeMult = Math.min(999f, closeMult + MULT_STEP);
                    runMaxCloseMult = Math.max(runMaxCloseMult, closeMult);
                    score += 5f * combo * scoreFactor();
                    for (int k = 0; k < 3; k++) {
                        addParticle(o.x + (rnd.nextFloat() - 0.5f) * o.r,
                                py - 10f + (rnd.nextFloat() - 0.5f) * 30f,
                                (rnd.nextFloat() - 0.5f) * 130f,
                                -40f - rnd.nextFloat() * 90f,
                                0.35f + rnd.nextFloat() * 0.30f,
                                2f + rnd.nextFloat() * 3f,
                                0xCCFFD75E, 0f);
                    }
                }
            }

            if (invuln <= 0f && state == ST_PLAY) {
                float rr = o.r * 0.80f + PLAYER_R * 0.80f;
                if (dist2(o.x, o.y, px, py) < rr * rr) {
                    // Броня выдерживает удар щита: щит спасает сферу,
                    // но сам астероид продолжает падать.
                    boolean survivesShield = (o.type == T_ARMOR || o.boss)
                            && shieldCharges > 0 && !zenMode;
                    if (o.boss && o.bossHits > 1 && !survivesShield) {
                        o.bossHits--;
                        o.bounceFlash = 1f;
                        explode(o.x, o.y, 18, o.color);
                        invuln = 0.75f;
                        shake = 18f;
                        flash = 0.18f;
                        toast("BOSS · " + o.bossHits + " HP");
                        continue;
                    }
                    if (!survivesShield) {
                        obstacles.remove(i);
                    }
                    hit(o);
                    continue;
                }
            }
            if (o.y - o.r > worldH + 100f) {
                obstacles.remove(i);
            }
        }
    }

    private void updatePickups(float dt) {
        float sdt = dt * slowFactor();
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            boolean pulled = false;
            if (p.kind == P_STAR && magnetTimer > 0f) {
                float dx = px - p.x;
                float dy = py - p.y;
                float d2 = dx * dx + dy * dy;
                if (d2 < 620f * 620f) {
                    float d = (float) Math.sqrt(d2);
                    if (d < 1f) {
                        d = 1f;
                    }
                    float sp = 760f + Math.min(300f, d);
                    p.x += dx / d * sp * dt;
                    p.y += dy / d * sp * dt;
                    pulled = true;
                    if (rnd.nextFloat() < dt * 14f) {
                        addParticle(p.x, p.y, (rnd.nextFloat() - 0.5f) * 70f,
                                (rnd.nextFloat() - 0.5f) * 70f,
                                0.25f + rnd.nextFloat() * 0.25f,
                                2f + rnd.nextFloat() * 2f, C_GOLD, 0f);
                    }
                }
            }
            if (!pulled) {
                p.y += p.vy * sdt;
            }
            p.rot += dt * 150f;

            float rr = p.r + PLAYER_R * 0.85f;
            if (dist2(p.x, p.y, px, py) < rr * rr) {
                pickups.remove(i);
                collect(p);
                continue;
            }
            if (p.y - p.r > worldH + 80f) {
                pickups.remove(i);
            }
        }
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                particles.remove(i);
                continue;
            }
            p.vy += p.grav * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            float drag = Math.max(0f, 1f - dt * 1.7f);
            p.vx *= drag;
            p.vy *= drag;
        }
    }

    private void updateShownScore() {
        int s = (int) score;
        if (s != shownScore) {
            shownScore = s;
            shownScoreText = fmt(s);
        }
    }

    // ==================================================================
    //  spawning / gameplay
    // ==================================================================

    private void spawnObstacle() {
        Obs o = new Obs();
        o.type = pickObstacleType();
        o.color = OBS_COLORS[o.type];

        o.r = 26f + gameRnd.nextFloat() * 20f;
        if (o.type == T_RED) {
            o.r *= 1.55f;
        } else if (o.type == T_SPLIT) {
            o.r *= 1.22f;
        } else if (o.type == T_MINE) {
            o.r *= 0.86f;
        } else if (o.type == T_ARMOR) {
            o.r *= 1.10f;
        }

        o.x = o.r + 30f + gameRnd.nextFloat() * (W - 2f * o.r - 60f);
        o.y = -o.r - 50f;

        float base = (430f + 400f * difficulty) * (1f + 0.30f * latePressure)
                * (0.86f + gameRnd.nextFloat() * 0.5f) * (0.72f + o.r / 60f);
        switch (o.type) {
            case T_BLUE:
                base *= 1.38f;
                break;
            case T_RED:
                base *= 0.82f;
                break;
            case T_VIOLET:
                base *= 0.92f;
                break;
            case T_ORANGE:
                base *= 0.95f;
                break;
            case T_SPLIT:
                base *= 0.86f;
                break;
            case T_MINE:
                base *= 0.78f;
                break;
            case T_GRAVITY:
                base *= 0.90f;
                break;
            case T_ARMOR:
                base *= 0.86f;
                break;
            default:
                break;
        }

        o.baseVy = base;
        o.vy = base;
        o.vx = 0f;
        if (o.type == T_GREEN) {
            o.vx = (gameRnd.nextBoolean() ? 1f : -1f) * (95f + 120f * difficulty);
        } else if (o.type == T_VIOLET) {
            o.vx = (gameRnd.nextFloat() - 0.5f) * 30f;
        }

        if (o.type == T_SPLIT) {
            o.canSplit = true;
            o.splitY = worldH * (0.26f + gameRnd.nextFloat() * 0.18f);
        } else if (o.type == T_MINE) {
            o.stopY = worldH * (0.28f + gameRnd.nextFloat() * 0.30f);
        }

        o.burstPhase = gameRnd.nextFloat() * 1.05f;
        o.rot = gameRnd.nextFloat() * 360f;
        o.vrot = (gameRnd.nextFloat() - 0.5f) * 190f;
        obstacles.add(o);
    }

    private void spawnBoss(int minute) {
        Obs o = new Obs();
        o.boss = true;
        o.bossStage = minute / 2;
        o.bossHits = 3 + o.bossStage;
        o.type = T_ARMOR;
        o.color = minute == 2 ? C_MAGENTA : (minute == 4 ? C_ORANGE : C_GOLD);
        o.r = 82f + o.bossStage * 8f;
        o.x = o.r + 24f + gameRnd.nextFloat() * (W - 2f * o.r - 48f);
        o.y = -o.r - 60f;
        o.baseVy = 250f + o.bossStage * 24f;
        o.vy = o.baseVy;
        o.vx = (gameRnd.nextBoolean() ? 1f : -1f) * (90f + o.bossStage * 20f);
        o.rot = gameRnd.nextFloat() * 360f;
        o.vrot = 60f + o.bossStage * 18f;
        obstacles.add(o);
        toast("MINI-BOSS · " + minute + ":00");
    }

    private void spawnSplitFragments(Obs parent) {
        for (int side = -1; side <= 1; side += 2) {
            Obs f = new Obs();
            f.type = T_SPLIT;
            f.color = parent.color;
            f.r = Math.max(16f, parent.r * 0.58f);
            f.x = clamp(parent.x + side * parent.r * 0.38f,
                    f.r + 8f, W - f.r - 8f);
            f.y = parent.y + parent.r * 0.18f;
            f.baseVy = parent.baseVy * 1.10f;
            f.vy = f.baseVy;
            f.vx = side * (105f + 55f * difficulty);
            f.rot = parent.rot + (side > 0 ? 26f : -26f);
            f.vrot = (side > 0 ? 1f : -1f) * (90f + 40f * difficulty);
            f.canSplit = false;
            f.splitY = Float.MAX_VALUE;
            f.stopY = Float.MAX_VALUE;
            obstacles.add(f);
        }
        explode(parent.x, parent.y, 12, parent.color);
    }

    private int pickObstacleType() {
        // Новые механики вводятся постепенно, чтобы первые секунды оставались читаемыми.
        if (elapsed < 20f) {
            return gameRnd.nextInt(3);
        }
        if (elapsed < 45f) {
            return gameRnd.nextInt(5);
        }
        if (elapsed < 75f) {
            return gameRnd.nextInt(7);
        }
        float v = gameRnd.nextFloat();
        if (v < 0.14f) {
            return T_GREEN;
        }
        if (v < 0.28f) {
            return T_BLUE;
        }
        if (v < 0.41f) {
            return T_VIOLET;
        }
        if (v < 0.54f) {
            return T_ORANGE;
        }
        if (v < 0.67f) {
            return T_RED;
        }
        if (v < 0.76f) {
            return T_SPLIT;
        }
        if (v < 0.85f) {
            return T_MINE;
        }
        if (v < 0.93f) {
            return T_GRAVITY;
        }
        return T_ARMOR;
    }

    private void bounceOff(Obs o) {
        if (o.bounceFlash > 0.5f) {
            return;
        }
        o.bounceFlash = 1f;
        for (int k = 0; k < 4; k++) {
            addParticle(o.x, o.y + (rnd.nextFloat() - 0.5f) * o.r,
                    (o.vx > 0f ? 1f : -1f) * (40f + rnd.nextFloat() * 90f),
                    (rnd.nextFloat() - 0.5f) * 90f,
                    0.25f + rnd.nextFloat() * 0.25f,
                    2f + rnd.nextFloat() * 3f,
                    o.color, 0f);
        }
    }

    private void spawnPickup(int kind) {
        Pickup p = new Pickup();
        p.kind = kind;
        switch (kind) {
            case P_HEART:
                p.r = 27f;
                p.vy = 250f;
                break;
            case P_SHIELD:
                p.r = 29f;
                p.vy = 260f;
                break;
            case P_MAGNET:
            case P_SLOW:
            case P_BOMB:
            case P_PHASE:
            case P_DOUBLE:
                p.r = 29f;
                p.vy = 270f;
                break;
            default:
                p.r = 30f;
                p.vy = 300f + difficulty * 130f;
                break;
        }
        p.x = p.r + 40f + pickupRnd.nextFloat() * (W - 2f * p.r - 80f);
        p.y = -p.r - 40f;
        p.rot = pickupRnd.nextFloat() * 360f;
        pickups.add(p);
    }

    private void hit(Obs o) {
        if (zenMode) {
            combo = 0;
            comboTimer = 0f;
            comboPop = 0f;
            resetMultipliers();
            shake = 18f;
            flash = 0.45f;
            invuln = 1.2f;
            explode(px, py, 10, C_CYAN);
            explode(o.x, o.y, 10, o.color);
            playSound(sndHit, 1.2f);
            buzz(22);
            return;
        }

        if (shieldCharges > 0) {
            shieldCharges = 0;
            progress.shieldBlocks++;
            shake = 24f;
            flash = 0.55f;
            invuln = 1.2f;
            explode(px, py, 22, C_MINT);
            explode(o.x, o.y, 14, o.color);
            playSound(sndPower, 0.85f);
            buzz(35);
            toast(sShieldBlocked);
            checkProgress();
            return;
        }

        combo = 0;
        comboTimer = 0f;
        comboPop = 0f;
        resetMultipliers();

        lives--;
        shake = 30f;
        flash = 1f;
        invuln = 1.7f;
        explode(px, py, 18, C_CYAN);
        explode(o.x, o.y, 16, o.color);
        playSound(sndHit, 0.9f + rnd.nextFloat() * 0.25f);
        buzz(45);
        if (lives <= 0) {
            gameOver();
        }
    }

    private void collect(Pickup p) {
        switch (p.kind) {
            case P_HEART:
                if (lives < maxLives) {
                    lives++;
                    playSound(sndPower, 1f);
                } else {
                    score += 60f * scoreFactor();
                    playSound(sndCoin, 1.25f);
                }
                explode(p.x, p.y, 16, C_RED);
                buzz(18);
                break;

            case P_SHIELD:
                shieldCharges = 1;
                score += 25f * scoreFactor();
                explode(p.x, p.y, 18, C_MINT);
                playSound(sndPower, 1.05f);
                buzz(20);
                toast(sShieldActive);
                break;

            case P_MAGNET:
                magnetTimer = MAGNET_TIME;
                score += 30f * scoreFactor();
                explode(p.x, p.y, 18, C_MAGENTA);
                playSound(sndPower, 1.15f);
                buzz(20);
                toast(sMagnetActive);
                break;

            case P_SLOW:
                slowTimer = SLOW_TIME;
                score += 30f * scoreFactor();
                explode(p.x, p.y, 18, C_ICE);
                playSound(sndPower, 0.85f);
                buzz(20);
                toast(sSlowActive);
                break;

            case P_BOMB:
                int cleared = 0;
                for (int i = obstacles.size() - 1; i >= 0; i--) {
                    Obs o = obstacles.get(i);
                    if (dist2(o.x, o.y, p.x, p.y) < 520f * 520f) {
                        explode(o.x, o.y, 10, o.color);
                        obstacles.remove(i);
                        cleared++;
                    }
                }
                score += (120f + cleared * 80f) * scoreFactor();
                explode(p.x, p.y, 34, C_ORANGE);
                playSound(sndHit, 0.7f);
                toast(sBombActive);
                break;

            case P_PHASE:
                phaseTimer = PHASE_TIME;
                invuln = Math.max(invuln, PHASE_TIME);
                score += 35f * scoreFactor();
                explode(p.x, p.y, 18, C_VIOLET);
                playSound(sndPower, 1.25f);
                toast(sPhaseActive);
                break;

            case P_DOUBLE:
                doubleTimer = DOUBLE_TIME;
                score += 35f * scoreFactor();
                explode(p.x, p.y, 18, C_GOLD);
                playSound(sndPower, 1.4f);
                toast(sDoubleActive);
                break;

            default:
                bumpMultiplier();
                score += 50f * scoreFactor();
                runStars++;
                progress.stars++;
                progress.dailyStars++;
                explode(p.x, p.y, 12, C_GOLD);
                playSound(sndCoin, 0.95f + rnd.nextFloat() * 0.25f);
                break;
        }
        checkProgress();
    }

    private void gameOver() {
        setKeepScreenOn(false);
        newRecord = dailyChallenge
                ? (int) score > progress.challengeBest
                : (int) score > progress.bestForMode(mode);
        bankScore();
        progress.saveStats();
        progress.saveDaily();
        checkProgress();

        state = ST_OVER;
        overTime = 0f;
        explode(px, py, 70, C_CYAN);
        explode(px, py, 24, 0xFFFFFFFF);
        shake = 42f;
        buzz(180);
        playSound(sndHit, 0.6f);
    }

    private void startGame() {
        startGame(false);
    }

    private void startDailyChallenge() {
        progress.ensureDay();
        if (!progress.canPlayChallenge()) {
            toast(sDailyUsed);
            inputLock = 0.15f;
            return;
        }
        // Попытка расходуется сразу: выход в меню не даёт переиграть день.
        progress.startChallenge();
        startGame(true);
    }

    private void startGame(boolean challenge) {
        setKeepScreenOn(true);
        // Чередуем два лупа между забегами, чтобы длинная сессия не звучала одинаково.
        musicTrack = (musicTrack + 1) & 1;
        if (musicPlayer != null) {
            releaseMusic();
            initMusic();
        }
        progress.ensureDay();
        dailyChallenge = challenge;
        mode = challenge ? Progress.MODE_CLASSIC : progress.mode;
        hardcoreMode = !challenge && mode == Progress.MODE_HARDCORE;
        zenMode = !challenge && mode == Progress.MODE_ZEN;
        maxLives = hardcoreMode ? 1 : MAX_LIVES;
        if (challenge) {
            long seed = (long) progress.challengeDate * 0x9E3779B97F4A7C15L;
            gameRnd = new Random(seed);
            pickupRnd = new Random(seed ^ 0x5DEECE66DL);
        } else {
            gameRnd = new Random();
            pickupRnd = new Random();
        }

        obstacles.clear();
        pickups.clear();
        particles.clear();

        elapsed = 0f;
        difficulty = 0f;
        latePressure = 0f;
        score = 0f;
        lives = maxLives;
        invuln = 1.0f;
        shake = 0f;
        flash = 0f;
        overTime = 0f;
        newRecord = false;
        shownScore = -1;
        combo = 0;
        comboTimer = 0f;
        comboPop = 0f;
        lastLevel = 0;
        announceTimer = 0f;

        runStars = 0;
        runDodged = 0;
        runMaxCombo = 0;
        scoreMult = 1f;
        runMaxMult = 1f;
        closeMult = 1f;
        runMaxCloseMult = 1f;
        scoreBanked = false;
        shieldCharges = 0;
        magnetTimer = 0f;
        slowTimer = 0f;
        phaseTimer = 0f;
        doubleTimer = 0f;
        bossMask = 0;
        toastTimer = 0f;
        toastText = "";
        resumeTimer = 0f;
        exitArmed = 0f;
        pressedId = B_NONE;

        progress.runs++;
        progress.saveStats();
        progress.saveDaily();

        px = W / 2f;
        targetX = px;
        py = groundY();
        targetY = py;

        spawnTimer = 1.0f;
        coinTimer = 2.0f;
        heartTimer = 18f;
        trailTimer = 0f;
        specialTimer = 12f + gameRnd.nextFloat() * 6f;
        checkTimer = 0f;
        saveTimer = 5f;

        state = ST_PLAY;
        playSound(sndPower, 1.35f);
        updateQuests();
        int fresh = progress.refreshAchievements();
        toastAchievement(fresh);
    }

    private void resetMenu() {
        setKeepScreenOn(false);
        boolean finishedRun = !scoreBanked && score > 0f;
        if (finishedRun) {
            bankScore();
            progress.saveStats();
        }
        dailyChallenge = false;
        mode = progress.mode;
        state = ST_MENU;
        resumeTimer = 0f;
        exitArmed = 0f;
        pressedId = B_NONE;
        scrollY = 0f;
        scrollVel = 0f;
        obstacles.clear();
        pickups.clear();
        particles.clear();
        elapsed = 0f;
        difficulty = 0f;
        latePressure = 0f;
        score = 0f;
        lives = maxLives;
        invuln = 0f;
        shake = 0f;
        flash = 0f;
        inputLock = 0.15f;
        overTime = 0f;
        newRecord = false;
        combo = 0;
        comboTimer = 0f;
        comboPop = 0f;
        scoreMult = 1f;
        runMaxMult = 1f;
        closeMult = 1f;
        runMaxCloseMult = 1f;
        announceTimer = 0f;
        shieldCharges = 0;
        magnetTimer = 0f;
        slowTimer = 0f;
        phaseTimer = 0f;
        doubleTimer = 0f;
        bossMask = 0;
        toastTimer = 0f;
        px = W / 2f;
        targetX = px;
        py = groundY();
        targetY = py;
        if (finishedRun) {
            checkProgress();
        }
    }

    private void bankScore() {
        if (scoreBanked) {
            return;
        }
        scoreBanked = true;
        int finalScore = (int) score;
        // A run counts toward the daily quest only after meaningful play.
        if (elapsed >= 10f || finalScore >= 300) {
            progress.dailyRuns++;
        }
        if (dailyChallenge) {
            // Испытание дня — отдельный рекорд; забег идёт по правилам
            // «Классики» и даёт неон, но не попадает в рекорд Classic.
            progress.recordChallengeScore(finalScore);
            progress.pointsTotal += finalScore;
        } else {
            progress.recordScore(mode, finalScore);
            // «Дзен» — тренировка: рекорд обновляется, но неон за очки не даётся.
            if (!zenMode) {
                progress.pointsTotal += finalScore;
            }
        }
        progress.addRun(dailyChallenge ? Progress.RUN_DAILY : mode,
                finalScore, Math.max(1, (int) elapsed));
    }

    private void bumpMultiplier() {
        scoreMult += MULT_STEP;
        if (scoreMult > runMaxMult) {
            runMaxMult = scoreMult;
        }
    }

    private void resetMultiplier() {
        scoreMult = 1f;
    }

    private void resetMultipliers() {
        scoreMult = 1f;
        closeMult = 1f;
    }

    private String fmtMult(float m) {
        return String.format(java.util.Locale.getDefault(), "%.1f", m);
    }

    private void checkProgress() {
        if (combo > progress.bestCombo) {
            progress.bestCombo = combo;
        }
        if ((int) elapsed > progress.bestTime) {
            progress.bestTime = (int) elapsed;
        }
        int fresh = progress.refreshAchievements();
        toastAchievement(fresh);
        updateQuests();
    }

    private void toastAchievement(int fresh) {
        if (fresh == 0) {
            return;
        }
        for (int i = 0; i < Progress.ACH_COUNT; i++) {
            if ((fresh & (1 << i)) != 0) {
                toast(sAchievementUnlocked + " · " + sAchName[i]);
                return;
            }
        }
    }

    private void updateQuests() {
        boolean changed = false;
        for (int slot = 0; slot < 3; slot++) {
            int id = progress.questIds[slot];
            int value = questValue(id);
            if (id <= Progress.QUEST_DODGE_RUN) {
                if (value > progress.questProgress[slot]) {
                    progress.questProgress[slot] = value;
                    changed = true;
                }
            } else if (value != progress.questProgress[slot]) {
                progress.questProgress[slot] = value;
                changed = true;
            }
            if (!progress.questDone[slot]
                    && progress.questProgress[slot] >= Progress.QUEST_TARGETS[id]) {
                progress.questDone[slot] = true;
                progress.questsDone++;
                progress.pointsTotal += Progress.QUEST_REWARD;
                toast(sQuestToast);
                changed = true;
            }
        }
        if (changed) {
            progress.saveDaily();
            progress.saveStats();
        }
    }

    private int questValue(int id) {
        switch (id) {
            case Progress.QUEST_STARS_RUN:
                return runStars;
            case Progress.QUEST_SCORE_RUN:
                return (int) score;
            case Progress.QUEST_COMBO_RUN:
                return runMaxCombo;
            case Progress.QUEST_DODGE_RUN:
                return runDodged;
            case Progress.QUEST_RUNS_DAY:
                return progress.dailyRuns;
            case Progress.QUEST_STARS_DAY:
                return progress.dailyStars;
            case Progress.QUEST_TIME_DAY:
                return (int) progress.dailyTime;
            default:
                return 0;
        }
    }

    private void toast(String text) {
        toastText = text;
        toastTimer = 2.2f;
    }

    private void addParticle(float x, float y, float vx, float vy,
                             float life, float size, int color, float grav) {
        if (particles.size() > 420) {
            particles.remove(0);
        }
        Particle p = new Particle();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.maxLife = life;
        p.size = size;
        p.color = color;
        p.grav = grav;
        particles.add(p);
    }

    private void explode(float x, float y, int count, int color) {
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2.0;
            float sp = 70f + rnd.nextFloat() * 640f;
            addParticle(x, y,
                    (float) Math.cos(a) * sp,
                    (float) Math.sin(a) * sp,
                    0.35f + rnd.nextFloat() * 0.75f,
                    2.5f + rnd.nextFloat() * 8f,
                    color, 240f);
        }
    }

    // ==================================================================
    //  drawing
    // ==================================================================

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (scale <= 0f || worldH <= 0f) {
            return;
        }
        c.save();
        c.scale(scale, scale);
        hitCount = 0;
        hitState = state;

        pBg.setShader(bgShader);
        c.drawRect(0f, 0f, W, worldH, pBg);
        if (nebulaOne != null) {
            pNebula.setShader(nebulaOne);
            c.drawRect(0f, 0f, W, worldH, pNebula);
            pNebula.setShader(nebulaTwo);
            c.drawRect(0f, 0f, W, worldH, pNebula);
        }
        drawStars(c);

        c.save();
        if (shake > 0.5f) {
            c.translate((rnd.nextFloat() - 0.5f) * shake,
                    (rnd.nextFloat() - 0.5f) * shake);
        }
        boolean menuSurface = state == ST_MENU || state == ST_SETTINGS
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
                || state == ST_HISTORY
                || (state == ST_RULES && rulesReturn == ST_MENU);
        if (menuSurface) {
            layoutMenu();
            drawMenuDecor(c);
        } else {
            drawWorld(c);
        }
        c.restore();

        if (slowTimer > 0f && state == ST_PLAY) {
            pFill.setColor(0x185FF3FF);
            c.drawRect(0f, 0f, W, worldH, pFill);
        }

        if (flash > 0f) {
            pFlash.setColor(C_RED);
            pFlash.setAlpha((int) (120f * clamp(flash, 0f, 1f)));
            c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFlash);
        }

        if (!menuSurface) {
            if (hudScrim == null) {
                rebuildScrim();
            }
            pScrim.setShader(hudScrim);
            c.drawRect(0f, 0f, W, hudScrimH, pScrim);
            drawHud(c);
        }
        if (state == ST_MENU) {
            drawMenu(c);
        } else if (state == ST_SETTINGS) {
            drawSettings(c);
        } else if (state == ST_STATS) {
            drawStats(c);
        } else if (state == ST_HISTORY) {
            drawHistory(c);
        } else if (state == ST_QUESTS) {
            drawQuests(c);
        } else if (state == ST_SKINS) {
            drawSkins(c);
        } else if (state == ST_RULES) {
            drawRules(c);
        } else if (state == ST_PAUSE) {
            drawPause(c);
        } else if (state == ST_OVER) {
            updateShownScore();
            drawOver(c);
        } else {
            drawTutorial(c);
            drawAnnounce(c);
            drawResumeCountdown(c);
        }
        drawToast(c);

        c.restore();
    }

    private void drawStars(Canvas c) {
        for (int i = 0; i < starX.length; i++) {
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * 1.6f + starPh[i]));
            pFill.setColor(0xFFDDF2FF);
            pFill.setAlpha((int) (190f * tw));
            c.drawCircle(starX[i], starY[i], starSize[i], pFill);
        }
    }

    private void drawWorld(Canvas c) {
        for (int i = 0; i < pickups.size(); i++) {
            drawPickup(c, pickups.get(i));
        }
        for (int i = 0; i < obstacles.size(); i++) {
            drawObs(c, obstacles.get(i));
        }
        drawParticles(c);
        if (state != ST_OVER) {
            drawPlayer(c, px, py);
        }
    }

    private void drawParticles(Canvas c) {
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            float lr = clamp(p.life / p.maxLife, 0f, 1f);
            pFill.setColor(p.color);
            int baseA = Color.alpha(p.color);
            pFill.setAlpha((int) (baseA * lr));
            c.drawCircle(p.x, p.y, p.size * (0.4f + 0.6f * lr), pFill);
        }
    }

    private void drawPlayer(Canvas c, float x, float y) {
        float a = 1f;
        if (invuln > 0f && state == ST_PLAY) {
            a = ((int) (invuln * 14f) % 2 == 0) ? 0.3f : 1f;
        }

        int si = clamp(progress.sphereSkin, 0, Progress.SPHERE_COUNT - 1);
        int outer = SPHERE_OUTER[si];
        int inner = SPHERE_INNER[si];

        if (magnetTimer > 0f && state == ST_PLAY) {
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(C_MAGENTA);
            pStroke.setAlpha(38);
            pStroke.setStrokeWidth(2.5f);
            c.drawCircle(x, y, 330f, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
        }

        drawGlow(c, x, y, 240f, outer, (int) (155f * a));

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(outer);
        pStroke.setAlpha((int) (110f * a));
        pStroke.setStrokeWidth(3f);
        c.drawCircle(x, y, PLAYER_R + 13f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        pFill.setColor(outer);
        pFill.setAlpha((int) (255f * a));
        c.drawCircle(x, y, PLAYER_R, pFill);

        pFill.setColor(inner);
        c.drawCircle(x, y, PLAYER_R * 0.62f, pFill);

        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(x, y, PLAYER_R * 0.27f, pFill);

        if (shieldCharges > 0) {
            float pulse = 0.65f + 0.35f * (float) Math.sin(time * 5.5f);
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(C_MINT);
            pStroke.setAlpha((int) (185f * pulse));
            pStroke.setStrokeWidth(4f);
            c.drawCircle(x, y, PLAYER_R + 23f + 3f * pulse, pStroke);
            pStroke.setColor(0xFFFFFFFF);
            pStroke.setAlpha((int) (90f * pulse));
            pStroke.setStrokeWidth(1.6f);
            c.drawCircle(x, y, PLAYER_R + 31f, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 4; i++) {
                double ang = Math.toRadians(time * 90f + i * 90f);
                pFill.setColor(C_MINT);
                pFill.setAlpha(210);
                c.drawCircle(x + (float) Math.cos(ang) * (PLAYER_R + 27f),
                        y + (float) Math.sin(ang) * (PLAYER_R + 27f), 3.4f, pFill);
            }
        }
    }

    private void drawObs(Canvas c, Obs o) {
        if (o.boss) {
            float pulse = 0.85f + 0.15f * (float) Math.sin(time * 4f);
            drawGlow(c, o.x, o.y, o.r * 3.6f, o.color, 190);
            drawHex(c, o.x, o.y, o.r * pulse, o.rot, o.color);
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(0xFFFFFFFF);
            pStroke.setAlpha(210);
            pStroke.setStrokeWidth(5f);
            c.drawCircle(o.x, o.y, o.r * 0.48f, pStroke);
            c.drawCircle(o.x, o.y, o.r * 0.22f, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
            textC(c, "BOSS", o.x, o.y + 8f, 24f, 0xFFFFFFFF, tfBlack, Paint.Align.CENTER);
            return;
        }
        int glowAlpha = 100;
        if (o.type == T_ORANGE) {
            glowAlpha = o.burstPhase < 0.30f ? 170 : 70;
        } else if (o.type == T_GREEN && o.bounceFlash > 0f) {
            glowAlpha = 100 + (int) (120f * o.bounceFlash);
        } else if (o.type == T_MINE) {
            float blink = 0.5f + 0.5f * (float) Math.sin(time * 18f);
            glowAlpha = o.mineArmed && o.fuse < 0.85f
                    ? 130 + (int) (100f * blink) : 105;
        } else if (o.type == T_GRAVITY) {
            glowAlpha = 105 + (int) (55f * (0.5f + 0.5f * (float) Math.sin(time * 3f)));
        } else if (o.type == T_ARMOR) {
            glowAlpha = 125;
        }
        drawGlow(c, o.x, o.y, o.r * 2.7f, o.color, glowAlpha);

        hexPath.reset();
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(o.rot + i * 60f);
            float hx = o.x + (float) Math.cos(ang) * o.r;
            float hy = o.y + (float) Math.sin(ang) * o.r;
            if (i == 0) {
                hexPath.moveTo(hx, hy);
            } else {
                hexPath.lineTo(hx, hy);
            }
        }
        hexPath.close();

        pFill.setColor(o.color);
        pFill.setAlpha(40);
        c.drawPath(hexPath, pFill);

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(o.color);
        pStroke.setAlpha(235);
        pStroke.setStrokeWidth(o.type == T_RED || o.type == T_ARMOR ? 7.5f : 5.5f);
        c.drawPath(hexPath, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setAlpha(70);
        pStroke.setStrokeWidth(1.8f);
        c.drawPath(hexPath, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        if (o.type == T_RED) {
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(0xFFFFFFFF);
            pStroke.setAlpha(95);
            pStroke.setStrokeWidth(2.2f);
            c.drawCircle(o.x, o.y, o.r * 0.50f, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
        } else {
            drawObsMark(c, o);
        }
    }

    private void drawObsMark(Canvas c, Obs o) {
        pStroke.setStyle(Paint.Style.STROKE);
        switch (o.type) {
            case T_GREEN: {
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(145);
                pStroke.setStrokeWidth(2.6f);
                float off = o.r * 0.40f;
                float len = o.r * 0.20f;
                c.drawLine(o.x - off - len, o.y - len, o.x - off, o.y, pStroke);
                c.drawLine(o.x - off - len, o.y + len, o.x - off, o.y, pStroke);
                c.drawLine(o.x + off + len, o.y - len, o.x + off, o.y, pStroke);
                c.drawLine(o.x + off + len, o.y + len, o.x + off, o.y, pStroke);
                break;
            }
            case T_BLUE: {
                pStroke.setColor(o.color);
                pStroke.setAlpha(160);
                pStroke.setStrokeWidth(2.2f);
                float by = o.y - o.r * 1.18f;
                for (int k = -1; k <= 1; k++) {
                    float sx = o.x + k * o.r * 0.34f;
                    c.drawLine(sx, by, sx, by - o.r * (0.34f - Math.abs(k) * 0.06f), pStroke);
                }
                break;
            }
            case T_VIOLET: {
                float ang = (float) Math.atan2(py - o.y, px - o.x);
                float dx = (float) Math.cos(ang) * o.r * 0.38f;
                float dy = (float) Math.sin(ang) * o.r * 0.38f;
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(65);
                pStroke.setStrokeWidth(1.6f);
                c.drawCircle(o.x, o.y, o.r * 0.50f, pStroke);
                pFill.setColor(0xFFFFFFFF);
                pFill.setAlpha(185);
                c.drawCircle(o.x + dx, o.y + dy, 2.5f + o.r * 0.06f, pFill);
                break;
            }
            case T_ORANGE: {
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(o.burstPhase < 0.30f ? 220 : 90);
                pStroke.setStrokeWidth(3f);
                float u = o.r * 0.38f;
                c.drawLine(o.x - u * 0.45f, o.y - u, o.x + u * 0.25f, o.y - u * 0.10f, pStroke);
                c.drawLine(o.x + u * 0.25f, o.y - u * 0.10f, o.x - u * 0.12f, o.y + u * 0.08f, pStroke);
                c.drawLine(o.x - u * 0.12f, o.y + u * 0.08f, o.x + u * 0.45f, o.y + u, pStroke);
                break;
            }
            case T_SPLIT: {
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(170);
                pStroke.setStrokeWidth(2.4f);
                float u = o.r * 0.28f;
                c.drawLine(o.x - u * 0.85f, o.y - u, o.x - u * 0.10f, o.y, pStroke);
                c.drawLine(o.x - u * 0.10f, o.y, o.x - u * 0.85f, o.y + u, pStroke);
                c.drawLine(o.x + u * 0.85f, o.y - u, o.x + u * 0.10f, o.y, pStroke);
                c.drawLine(o.x + u * 0.10f, o.y, o.x + u * 0.85f, o.y + u, pStroke);
                break;
            }
            case T_MINE: {
                float blink = 0.5f + 0.5f * (float) Math.sin(time * 18f);
                pStroke.setColor(o.color);
                pStroke.setAlpha(o.mineArmed ? 150 + (int) (100f * blink) : 150);
                pStroke.setStrokeWidth(2.8f);
                for (int k = 0; k < 6; k++) {
                    double ang = Math.toRadians(o.rot + k * 60f);
                    float ca = (float) Math.cos(ang);
                    float sa = (float) Math.sin(ang);
                    c.drawLine(o.x + ca * o.r * 0.72f, o.y + sa * o.r * 0.72f,
                            o.x + ca * o.r * 1.02f, o.y + sa * o.r * 1.02f, pStroke);
                }
                pFill.setColor(o.mineArmed ? 0xFFFFFFFF : o.color);
                pFill.setAlpha(o.mineArmed ? 150 + (int) (100f * blink) : 175);
                c.drawCircle(o.x, o.y, o.r * 0.17f, pFill);
                if (o.mineArmed && o.fuse < 0.85f) {
                    pStroke.setColor(0xFFFF4FD8);
                    pStroke.setAlpha(40 + (int) (90f * blink));
                    pStroke.setStrokeWidth(2f);
                    c.drawCircle(o.x, o.y, o.r * 1.34f, pStroke);
                }
                break;
            }
            case T_GRAVITY: {
                float pulse = 0.5f + 0.5f * (float) Math.sin(time * 4f);
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(50 + (int) (55f * pulse));
                pStroke.setStrokeWidth(1.8f);
                c.drawCircle(o.x, o.y, o.r * 0.44f, pStroke);
                pStroke.setAlpha(35 + (int) (45f * pulse));
                c.drawCircle(o.x, o.y, o.r * 0.68f, pStroke);
                pFill.setColor(0xFFFFFFFF);
                pFill.setAlpha(120 + (int) (55f * pulse));
                for (int k = 0; k < 4; k++) {
                    double ang = Math.toRadians(time * 55f + k * 90f);
                    c.drawCircle(o.x + (float) Math.cos(ang) * o.r * 0.55f,
                            o.y + (float) Math.sin(ang) * o.r * 0.55f,
                            2.2f + o.r * 0.03f, pFill);
                }
                break;
            }
            case T_ARMOR: {
                hexPath.reset();
                for (int k = 0; k < 6; k++) {
                    double ang = Math.toRadians(o.rot + k * 60f);
                    float hx = o.x + (float) Math.cos(ang) * o.r * 0.56f;
                    float hy = o.y + (float) Math.sin(ang) * o.r * 0.56f;
                    if (k == 0) {
                        hexPath.moveTo(hx, hy);
                    } else {
                        hexPath.lineTo(hx, hy);
                    }
                }
                hexPath.close();
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(105);
                pStroke.setStrokeWidth(2.6f);
                c.drawPath(hexPath, pStroke);
                pFill.setColor(0xFFFFFFFF);
                pFill.setAlpha(135);
                for (int k = 0; k < 3; k++) {
                    double ang = Math.toRadians(o.rot + 30f + k * 120f);
                    c.drawCircle(o.x + (float) Math.cos(ang) * o.r * 0.34f,
                            o.y + (float) Math.sin(ang) * o.r * 0.34f,
                            2.6f, pFill);
                }
                break;
            }
            default:
                break;
        }
        pStroke.setStyle(Paint.Style.FILL);
    }

    private void drawPickup(Canvas c, Pickup p) {
        if (p.kind == P_HEART) {
            float pulse = 1f + 0.10f * (float) Math.sin(time * 5f + p.rot * 0.05f);
            drawGlow(c, p.x, p.y, p.r * 4.2f, C_RED, 120);
            drawHeart(c, p.x, p.y, p.r * 0.72f * pulse, C_RED, 255);
            drawHeart(c, p.x, p.y, p.r * 0.40f * pulse, 0xFFFFD9E0, 190);
            return;
        }

        if (p.kind != P_STAR) {
            int col = bonusColor(p.kind);
            float pulse = 1f + 0.08f * (float) Math.sin(time * 5f + p.rot * 0.04f);
            drawGlow(c, p.x, p.y, p.r * 4.2f, col, 145);
            drawBonusIcon(c, p.kind, p.x, p.y, p.r * 0.92f * pulse, 255);
            return;
        }

        starPath.reset();
        for (int i = 0; i < 8; i++) {
            float rr = (i % 2 == 0) ? p.r : p.r * 0.34f;
            double ang = Math.toRadians(p.rot + i * 45f);
            float sx = p.x + (float) Math.cos(ang) * rr;
            float sy = p.y + (float) Math.sin(ang) * rr;
            if (i == 0) {
                starPath.moveTo(sx, sy);
            } else {
                starPath.lineTo(sx, sy);
            }
        }
        starPath.close();

        drawGlow(c, p.x, p.y, p.r * 4.0f, C_GOLD, 150);
        pFill.setColor(C_GOLD);
        pFill.setAlpha(240);
        c.drawPath(starPath, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(0xFFFFFFFF);
        pStroke.setAlpha(150);
        pStroke.setStrokeWidth(2f);
        c.drawPath(starPath, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
    }

    private int bonusColor(int kind) {
        switch (kind) {
            case P_HEART:
                return C_RED;
            case P_SHIELD:
                return C_MINT;
            case P_MAGNET:
                return C_MAGENTA;
            case P_SLOW:
                return C_ICE;
            case P_BOMB:
                return C_ORANGE;
            case P_PHASE:
                return C_VIOLET;
            case P_DOUBLE:
                return C_GOLD;
            default:
                return C_GOLD;
        }
    }

    private void drawBonusIcon(Canvas c, int kind, float x, float y, float size, int alpha) {
        int col = bonusColor(kind);
        switch (kind) {
            case P_SHIELD: {
                c.save();
                c.translate(x, y);
                c.scale(size, size);
                pFill.setColor(col);
                pFill.setAlpha(alpha / 4);
                c.drawPath(shieldPath, pFill);
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setColor(col);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(0.17f);
                c.drawPath(shieldPath, pStroke);
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(alpha / 2);
                pStroke.setStrokeWidth(0.07f);
                c.drawPath(shieldPath, pStroke);
                pStroke.setStyle(Paint.Style.FILL);
                c.restore();
                break;
            }
            case P_MAGNET: {
                float sx = size * 0.58f;
                float top = y - size * 0.92f;
                float arcY = y + size * 0.12f;
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setStrokeCap(Paint.Cap.ROUND);
                pStroke.setColor(col);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(size * 0.34f);
                c.drawLine(x - sx, top, x - sx, arcY, pStroke);
                c.drawLine(x + sx, top, x + sx, arcY, pStroke);
                tmpRect.set(x - sx, arcY - sx, x + sx, arcY + sx);
                c.drawArc(tmpRect, 0f, 180f, false, pStroke);
                pStroke.setStrokeCap(Paint.Cap.BUTT);
                pStroke.setStyle(Paint.Style.FILL);
                pFill.setColor(0xFFFFFFFF);
                pFill.setAlpha(alpha);
                c.drawCircle(x - sx, top + size * 0.12f, size * 0.17f, pFill);
                c.drawCircle(x + sx, top + size * 0.12f, size * 0.17f, pFill);
                break;
            }
            case P_SLOW: {
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setStrokeCap(Paint.Cap.ROUND);
                pStroke.setColor(col);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(size * 0.16f);
                c.drawCircle(x, y, size * 0.82f, pStroke);
                c.drawLine(x, y, x, y - size * 0.48f, pStroke);
                c.drawLine(x, y, x + size * 0.42f, y + size * 0.12f, pStroke);
                pStroke.setStrokeCap(Paint.Cap.BUTT);
                pStroke.setStyle(Paint.Style.FILL);
                pFill.setColor(0xFFFFFFFF);
                pFill.setAlpha(alpha);
                c.drawCircle(x, y, size * 0.15f, pFill);
                break;
            }
            case P_BOMB: {
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setColor(col);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(size * 0.16f);
                c.drawCircle(x, y + size * 0.12f, size * 0.56f, pStroke);
                c.drawLine(x, y - size * 0.44f, x + size * 0.26f, y - size * 0.78f, pStroke);
                c.drawLine(x + size * 0.26f, y - size * 0.78f,
                        x + size * 0.50f, y - size * 0.62f, pStroke);
                strokeReset();
                break;
            }
            case P_PHASE: {
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setColor(col);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(size * 0.14f);
                c.drawCircle(x, y, size * 0.74f, pStroke);
                c.drawCircle(x, y, size * 0.34f, pStroke);
                c.drawLine(x - size * 0.95f, y, x + size * 0.95f, y, pStroke);
                strokeReset();
                break;
            }
            case P_DOUBLE: {
                textC(c, "×2", x, y, size * 0.78f, col, tfBlack, Paint.Align.CENTER);
                break;
            }
            default:
                break;
        }
    }

    private void drawHex(Canvas c, float x, float y, float r, float rot, int color) {
        hexPath.reset();
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(rot + i * 60f);
            float hx = x + (float) Math.cos(ang) * r;
            float hy = y + (float) Math.sin(ang) * r;
            if (i == 0) {
                hexPath.moveTo(hx, hy);
            } else {
                hexPath.lineTo(hx, hy);
            }
        }
        hexPath.close();

        drawGlow(c, x, y, r * 2.6f, color, 80);
        pFill.setColor(color);
        pFill.setAlpha(34);
        c.drawPath(hexPath, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(color);
        pStroke.setAlpha(210);
        pStroke.setStrokeWidth(5f);
        c.drawPath(hexPath, pStroke);
        pStroke.setColor(0xFFFFFFFF);
        pStroke.setAlpha(55);
        pStroke.setStrokeWidth(1.6f);
        c.drawPath(hexPath, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
    }

    private void drawHeart(Canvas c, float x, float y, float size, int color, int alpha) {
        c.save();
        c.translate(x, y);
        c.scale(size, size);
        pFill.setColor(color);
        pFill.setAlpha(alpha);
        c.drawPath(heartPath, pFill);
        c.restore();
    }

    private void drawGlow(Canvas c, float x, float y, float size, int color, int alpha) {
        if (alpha <= 0) {
            return;
        }
        tmpRect.set(x - size * 0.5f, y - size * 0.5f, x + size * 0.5f, y + size * 0.5f);
        pGlow.setColorFilter(filter(color));
        pGlow.setAlpha(Math.min(255, alpha));
        c.drawBitmap(glowBmp, srcGlow, tmpRect, pGlow);
    }

    private PorterDuffColorFilter filter(int color) {
        Integer key = color;
        PorterDuffColorFilter f = filters.get(key);
        if (f == null) {
            f = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
            filters.put(key, f);
        }
        return f;
    }

    // ==================================================================
    //  UI kit: палитра, зоны нажатия, прокрутка
    // ==================================================================
    //
    // Цвет в интерфейсе несёт смысл: циан — действие, золото — неон и
    // награды, магента — испытание дня, мята — выполнено, цвет режима —
    // сам режим. Форма тоже: главная кнопка — сплошная «капсула», вторичные —
    // контур, карточки — скругление 28, плитки навигации — 30.

    private static final int UI_INK = 0xFFE8F7FF;
    private static final int UI_FOG = 0xFF8FA6BD;
    private static final int UI_DIM = 0xFF56697E;
    private static final int UI_SURFACE = 0xE00E1630;
    private static final int UI_SURFACE_HI = 0xF0182450;
    private static final int UI_LINE = 0x1CFFFFFF;
    private static final int UI_ON_ACCENT = 0xFF061018;
    private static final float UI_PAD = 48f;

    private static final int B_NONE = -1;
    private static final int B_PLAY = 1;
    private static final int B_DAILY = 2;
    private static final int B_QUESTS = 3;
    private static final int B_SKINS = 4;
    private static final int B_STATS = 5;
    private static final int B_RULES = 6;
    private static final int B_SETTINGS = 7;
    private static final int B_BACK = 8;
    private static final int B_HISTORY = 9;
    private static final int B_PAUSE = 10;
    private static final int B_CONTINUE = 11;
    private static final int B_PAUSE_RULES = 12;
    private static final int B_EXIT = 13;
    private static final int B_MUSIC = 14;
    private static final int B_SOUND = 15;
    private static final int B_VIBRATION = 16;
    private static final int B_AGAIN = 17;
    private static final int B_MENU = 18;
    private static final int B_SLIDER = 19;
    private static final int B_TUTORIAL = 20;
    private static final int B_EMPTY_PLAY = 21;
    private static final int B_MODE = 30;     // + индекс режима
    private static final int B_CONTROL = 40;  // + тип управления
    private static final int B_TAB = 50;      // + вкладка скинов
    private static final int B_SKIN = 60;     // + индекс скина

    private static final float RESUME_TIME = 1.5f;
    private static final float EXIT_CONFIRM_TIME = 3f;

    // Зоны нажатия собираются во время отрисовки кадра, поэтому разметка
    // и обработка касаний не расходятся.
    private static final int MAX_HITS = 48;
    private final RectF[] hitRect = new RectF[MAX_HITS];
    private final int[] hitId = new int[MAX_HITS];
    private int hitCount;
    private int hitState = -1;
    private int pressedId = B_NONE;
    private boolean gestureIgnored;
    private boolean touchMoved;
    private float touchDownX;
    private float touchDownY;

    private float scrollY;
    private float scrollMax;
    private float scrollVel;
    private boolean scrollDragging;
    private float scrollLastY;
    private long scrollLastT;
    private float scrollTop;
    private float scrollBottom = Float.MAX_VALUE;
    private boolean clipping;

    private float sliderX1 = 170f;
    private float sliderX2 = 910f;
    private float exitArmed;
    private float resumeTimer;

    private float mTopBarY;
    private float mLogoY;
    private float mLogoSize;
    private float mHeroY = 1300f;
    private float mSegCy;
    private float mInfoY;
    private float mPlayCy;
    private float mDailyCy;
    private float mNavCy;

    private int hudBestValue = -1;
    private boolean hudBestDaily;
    private String hudBestText = "";
    private int hudMultTenths = -1;
    private String hudMultText = "x1.0";

    {
        for (int i = 0; i < MAX_HITS; i++) {
            hitRect[i] = new RectF();
        }
    }

    private void addHit(int id, float l, float t, float r, float b) {
        if (clipping) {
            t = Math.max(t, scrollTop);
            b = Math.min(b, scrollBottom);
        }
        if (b <= t || hitCount >= MAX_HITS) {
            return;
        }
        hitRect[hitCount].set(l, t, r, b);
        hitId[hitCount] = id;
        hitCount++;
    }

    private int hitAt(float x, float y) {
        if (hitState != state) {
            return B_NONE;
        }
        for (int i = hitCount - 1; i >= 0; i--) {
            RectF r = hitRect[i];
            if (x >= r.left - 6f && x <= r.right + 6f
                    && y >= r.top - 6f && y <= r.bottom + 6f) {
                return hitId[i];
            }
        }
        return B_NONE;
    }

    private boolean pressed(int id) {
        return pressedId == id && !(scrollDragging && touchMoved);
    }

    private boolean isScrollScreen() {
        return state == ST_STATS || state == ST_HISTORY || state == ST_QUESTS
                || state == ST_SKINS || state == ST_RULES || state == ST_SETTINGS;
    }

    private void go(int newState) {
        state = newState;
        scrollY = 0f;
        scrollVel = 0f;
        scrollMax = 0f;
        scrollDragging = false;
        pressedId = B_NONE;
        draggingOffset = false;
        inputLock = 0.12f;
    }

    private void updateUi(float dt) {
        if (exitArmed > 0f) {
            exitArmed = Math.max(0f, exitArmed - dt);
        }
        if (!scrollDragging && scrollVel != 0f) {
            scrollY += scrollVel * dt;
            scrollVel *= (float) Math.exp(-4.5f * dt);
            if (scrollY <= 0f || scrollY >= scrollMax) {
                scrollY = clamp(scrollY, 0f, scrollMax);
                scrollVel = 0f;
            }
            if (Math.abs(scrollVel) < 25f) {
                scrollVel = 0f;
            }
        }
    }

    // ---------- примитивы ----------

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int aa = (a >>> 24);
        return (aa << 24)
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (clamp(alpha, 0, 255) << 24);
    }

    private void box(Canvas c, float l, float t, float r, float b, float rad,
                     int fill, int stroke) {
        tmpRect.set(l, t, r, b);
        if (fill != 0) {
            pFill.setColor(fill);
            c.drawRoundRect(tmpRect, rad, rad, pFill);
        }
        if (stroke != 0) {
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(stroke);
            pStroke.setStrokeWidth(2f);
            c.drawRoundRect(tmpRect, rad, rad, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
        }
    }

    /** Карточка; цветная полоса слева кодирует статус или режим. */
    private void card(Canvas c, float l, float t, float r, float b, int accent,
                      boolean highlight) {
        box(c, l, t, r, b, 28f, highlight ? UI_SURFACE_HI : UI_SURFACE,
                highlight ? 0x995FF3FF : UI_LINE);
        if (accent != 0) {
            box(c, l + 12f, t + 24f, l + 18f, b - 24f, 3f, accent, 0);
        }
    }

    private void textC(Canvas c, String s, float x, float cy, float size,
                       int color, Typeface tf, Paint.Align align) {
        text(c, s, x, cy + size * 0.36f, size, color, tf, align);
    }

    private float measure(String s, float size, Typeface tf) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setLetterSpacing(0f);
        return pText.measureText(s);
    }

    private void primaryButton(Canvas c, int id, float cx, float cy, float w, float h,
                               String label, int color, float breathe) {
        boolean down = pressed(id);
        float in = down ? 5f : 0f;
        c.save();
        c.scale(w / h * 0.62f, 1f, cx, cy);
        drawGlow(c, cx, cy, h * 2.4f, color, (int) ((down ? 50f : 70f) + 40f * breathe));
        c.restore();
        box(c, cx - w / 2f + in, cy - h / 2f + in, cx + w / 2f - in, cy + h / 2f - in,
                (h - 2f * in) / 2f, down ? mix(color, 0xFF000000, 0.22f) : color, 0);
        float size = Math.min(48f, h * 0.34f);
        float tw = measure(label, size, tfBlack);
        float tri = size * 0.42f;
        float tx = cx + tri;
        if (id == B_PLAY || id == B_AGAIN || id == B_EMPTY_PLAY || id == B_CONTINUE) {
            float ix = cx - tw / 2f - tri * 0.4f;
            iconPlay(c, ix, cy, tri, UI_ON_ACCENT);
        } else {
            tx = cx;
        }
        textC(c, label, tx, cy, size, UI_ON_ACCENT, tfBlack, Paint.Align.CENTER);
        addHit(id, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    private void secondaryButton(Canvas c, int id, float cx, float cy, float w, float h,
                                 String label, int accent, int textColor) {
        boolean down = pressed(id);
        float in = down ? 4f : 0f;
        box(c, cx - w / 2f + in, cy - h / 2f + in, cx + w / 2f - in, cy + h / 2f - in,
                (h - 2f * in) / 2f, down ? UI_SURFACE_HI : 0x990E1630,
                withAlpha(accent, down ? 200 : 120));
        textC(c, label, cx, cy, Math.min(34f, h * 0.30f), textColor,
                tfCond, Paint.Align.CENTER);
        addHit(id, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    private void progressBar(Canvas c, float l, float r, float cy, float h,
                             float ratio, int color) {
        box(c, l, cy - h / 2f, r, cy + h / 2f, h / 2f, 0x1FFFFFFF, 0);
        ratio = clamp(ratio, 0f, 1f);
        if (ratio > 0f) {
            float rr = Math.max(l + h, l + (r - l) * ratio);
            box(c, l, cy - h / 2f, rr, cy + h / 2f, h / 2f, color, 0);
        }
    }

    private void switchToggle(Canvas c, float cx, float cy, boolean on) {
        float w = 92f;
        float h = 52f;
        box(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h / 2f,
                on ? 0x665FF3FF : 0x22FFFFFF, on ? 0 : 0x22FFFFFF);
        float kx = on ? cx + w / 2f - h / 2f : cx - w / 2f + h / 2f;
        if (on) {
            drawGlow(c, kx, cy, 90f, C_CYAN, 90);
        }
        pFill.setColor(on ? 0xFFFFFFFF : 0xFF7D90A6);
        c.drawCircle(kx, cy, h / 2f - 6f, pFill);
    }

    private float sectionLabel(Canvas c, String s, float y) {
        text(c, s, UI_PAD + 8f, y + 34f, 27f, UI_FOG, tfMed, Paint.Align.LEFT);
        return y + 56f;
    }

    // ---------- иконки (векторные, на Canvas) ----------

    private void strokeStyle(int color, float width) {
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeCap(Paint.Cap.ROUND);
        pStroke.setStrokeJoin(Paint.Join.ROUND);
        pStroke.setColor(color);
        pStroke.setStrokeWidth(width);
    }

    private void strokeReset() {
        pStroke.setStrokeCap(Paint.Cap.BUTT);
        pStroke.setStrokeJoin(Paint.Join.MITER);
        pStroke.setStyle(Paint.Style.FILL);
    }

    private void iconPlay(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        iconPath.moveTo(x - s * 0.55f, y - s * 0.75f);
        iconPath.lineTo(x + s * 0.75f, y);
        iconPath.lineTo(x - s * 0.55f, y + s * 0.75f);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
    }

    private void iconChevron(Canvas c, float x, float y, float s, int color, boolean left) {
        strokeStyle(color, s * 0.30f);
        float d = left ? 1f : -1f;
        c.drawLine(x + d * s * 0.45f, y - s, x - d * s * 0.45f, y, pStroke);
        c.drawLine(x - d * s * 0.45f, y, x + d * s * 0.45f, y + s, pStroke);
        strokeReset();
    }

    private void iconCheck(Canvas c, float x, float y, float s, int color, float width) {
        strokeStyle(color, width);
        c.drawLine(x - s * 0.55f, y + s * 0.02f, x - s * 0.15f, y + s * 0.42f, pStroke);
        c.drawLine(x - s * 0.15f, y + s * 0.42f, x + s * 0.60f, y - s * 0.40f, pStroke);
        strokeReset();
    }

    private void iconDiamond(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        iconPath.moveTo(x, y - s);
        iconPath.lineTo(x + s * 0.72f, y);
        iconPath.lineTo(x, y + s);
        iconPath.lineTo(x - s * 0.72f, y);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
        pFill.setColor(0x88FFFFFF);
        c.drawCircle(x - s * 0.12f, y - s * 0.30f, s * 0.16f, pFill);
    }

    private void iconGear(Canvas c, float x, float y, float s, int color) {
        pFill.setColor(color);
        for (int i = 0; i < 8; i++) {
            c.save();
            c.rotate(i * 45f, x, y);
            tmpRect.set(x - s * 0.17f, y - s, x + s * 0.17f, y - s * 0.55f);
            c.drawRoundRect(tmpRect, s * 0.06f, s * 0.06f, pFill);
            c.restore();
        }
        strokeStyle(color, s * 0.26f);
        c.drawCircle(x, y, s * 0.58f, pStroke);
        strokeReset();
    }

    private void iconFlag(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.16f);
        c.drawLine(x - s * 0.62f, y - s, x - s * 0.62f, y + s, pStroke);
        strokeReset();
        iconPath.reset();
        iconPath.moveTo(x - s * 0.55f, y - s * 0.92f);
        iconPath.lineTo(x + s * 0.80f, y - s * 0.55f);
        iconPath.lineTo(x - s * 0.55f, y - s * 0.05f);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
    }

    private void iconOrb(Canvas c, float x, float y, float s, int color) {
        // Сфера со шлейфом: точки уходят влево и вниз, как в полёте.
        pFill.setColor(withAlpha(color, 80));
        c.drawCircle(x - s * 1.05f, y + s * 0.58f, s * 0.14f, pFill);
        pFill.setColor(withAlpha(color, 130));
        c.drawCircle(x - s * 0.80f, y + s * 0.30f, s * 0.20f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(s * 0.12f);
        pStroke.setColor(withAlpha(color, 150));
        c.drawCircle(x + s * 0.20f, y - s * 0.10f, s * 0.86f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        pFill.setColor(color);
        c.drawCircle(x + s * 0.20f, y - s * 0.10f, s * 0.56f, pFill);
        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(x + s * 0.20f, y - s * 0.10f, s * 0.22f, pFill);
    }

    private void iconBars(Canvas c, float x, float y, float s, int color) {
        pFill.setColor(color);
        float w = s * 0.40f;
        float base = y + s * 0.85f;
        float[] hs = {0.85f, 1.35f, 1.80f};
        for (int i = 0; i < 3; i++) {
            float bx = x + (i - 1) * s * 0.66f;
            tmpRect.set(bx - w / 2f, base - s * hs[i], bx + w / 2f, base);
            c.drawRoundRect(tmpRect, w * 0.3f, w * 0.3f, pFill);
        }
    }

    private void iconDoc(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.15f);
        tmpRect.set(x - s * 0.72f, y - s, x + s * 0.72f, y + s);
        c.drawRoundRect(tmpRect, s * 0.22f, s * 0.22f, pStroke);
        c.drawLine(x - s * 0.36f, y - s * 0.45f, x + s * 0.36f, y - s * 0.45f, pStroke);
        c.drawLine(x - s * 0.36f, y, x + s * 0.36f, y, pStroke);
        c.drawLine(x - s * 0.36f, y + s * 0.45f, x + s * 0.12f, y + s * 0.45f, pStroke);
        strokeReset();
    }

    private void iconLock(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.20f);
        tmpRect.set(x - s * 0.45f, y - s * 1.0f, x + s * 0.45f, y - s * 0.1f);
        c.drawArc(tmpRect, 180f, 180f, false, pStroke);
        c.drawLine(x - s * 0.45f, y - s * 0.55f, x - s * 0.45f, y - s * 0.1f, pStroke);
        c.drawLine(x + s * 0.45f, y - s * 0.55f, x + s * 0.45f, y - s * 0.1f, pStroke);
        strokeReset();
        box(c, x - s * 0.72f, y - s * 0.15f, x + s * 0.72f, y + s * 0.9f, s * 0.2f,
                color, 0);
    }

    private void iconNote(Canvas c, float x, float y, float s, int color) {
        pFill.setColor(color);
        c.drawCircle(x - s * 0.35f, y + s * 0.6f, s * 0.36f, pFill);
        strokeStyle(color, s * 0.16f);
        c.drawLine(x - s * 0.02f, y + s * 0.6f, x - s * 0.02f, y - s * 0.9f, pStroke);
        c.drawLine(x - s * 0.02f, y - s * 0.9f, x + s * 0.6f, y - s * 0.55f, pStroke);
        strokeReset();
    }

    private void iconSpeaker(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        iconPath.moveTo(x - s * 0.9f, y - s * 0.32f);
        iconPath.lineTo(x - s * 0.5f, y - s * 0.32f);
        iconPath.lineTo(x - s * 0.02f, y - s * 0.8f);
        iconPath.lineTo(x - s * 0.02f, y + s * 0.8f);
        iconPath.lineTo(x - s * 0.5f, y + s * 0.32f);
        iconPath.lineTo(x - s * 0.9f, y + s * 0.32f);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
        strokeStyle(color, s * 0.14f);
        tmpRect.set(x - s * 0.35f, y - s * 0.5f, x + s * 0.55f, y + s * 0.5f);
        c.drawArc(tmpRect, -50f, 100f, false, pStroke);
        tmpRect.set(x - s * 0.55f, y - s * 0.9f, x + s * 0.95f, y + s * 0.9f);
        c.drawArc(tmpRect, -50f, 100f, false, pStroke);
        strokeReset();
    }

    private void iconVibrate(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.15f);
        tmpRect.set(x - s * 0.40f, y - s * 0.85f, x + s * 0.40f, y + s * 0.85f);
        c.drawRoundRect(tmpRect, s * 0.18f, s * 0.18f, pStroke);
        c.drawLine(x - s * 0.72f, y - s * 0.35f, x - s * 0.72f, y + s * 0.35f, pStroke);
        c.drawLine(x + s * 0.72f, y - s * 0.35f, x + s * 0.72f, y + s * 0.35f, pStroke);
        c.drawLine(x - s * 1.0f, y - s * 0.18f, x - s * 1.0f, y + s * 0.18f, pStroke);
        c.drawLine(x + s * 1.0f, y - s * 0.18f, x + s * 1.0f, y + s * 0.18f, pStroke);
        strokeReset();
    }

    private void iconReplay(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.18f);
        tmpRect.set(x - s * 0.75f, y - s * 0.75f, x + s * 0.75f, y + s * 0.75f);
        c.drawArc(tmpRect, -60f, 300f, false, pStroke);
        strokeReset();
        iconPath.reset();
        float ax = x + s * 0.375f;
        float ay = y - s * 0.65f;
        iconPath.moveTo(ax + s * 0.42f, ay - s * 0.05f);
        iconPath.lineTo(ax - s * 0.10f, ay - s * 0.40f);
        iconPath.lineTo(ax - s * 0.05f, ay + s * 0.30f);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
    }

    /** Косая черта поверх иконки выключенной настройки. */
    private void iconSlash(Canvas c, float x, float y, float s, int color) {
        strokeStyle(0xFF0E1630, s * 0.36f);
        c.drawLine(x - s, y - s, x + s, y + s, pStroke);
        strokeStyle(color, s * 0.15f);
        c.drawLine(x - s, y - s, x + s, y + s, pStroke);
        strokeReset();
    }

    private int modeColor(int m) {
        return historyColor(m);
    }

    // ==================================================================
    //  HUD
    // ==================================================================

    private void drawHud(Canvas c) {
        float top = insetTop;

        updateShownScore();
        textGlow(c, shownScoreText, W / 2f, top + 112f, 96f,
                0xFFF2FEFF, 0x885FF3FF, tfBlack);

        int best = dailyChallenge ? progress.challengeBest : progress.bestForMode(mode);
        if (best != hudBestValue || dailyChallenge != hudBestDaily) {
            hudBestValue = best;
            hudBestDaily = dailyChallenge;
            hudBestText = (dailyChallenge ? sDailyChallenge + " · " : "")
                    + sBest + " " + fmt(best);
        }
        text(c, hudBestText, W / 2f, top + 156f, 25f,
                dailyChallenge ? 0xDDFF9FE9 : 0xAA9FB8CC, tfMed, Paint.Align.CENTER);

        int tenths = Math.round(scoreMult * 10f);
        if (tenths != hudMultTenths) {
            hudMultTenths = tenths;
            hudMultText = "x" + fmtMult(scoreMult);
        }
        boolean hot = tenths > 10;
        float mw = measure(hudMultText, 27f, tfBlack) + 48f;
        float my = top + 202f;
        box(c, W / 2f - mw / 2f, my - 24f, W / 2f + mw / 2f, my + 24f, 24f,
                hot ? 0x2EFFD75E : 0x14FFFFFF, hot ? 0x99FFD75E : 0);
        textC(c, hudMultText, W / 2f, my, 27f, hot ? C_GOLD : 0x99A9C4D6,
                tfBlack, Paint.Align.CENTER);
        String closeText = "x" + fmtMult(closeMult);
        float cw = measure(closeText, 25f, tfBlack) + 34f;
        float cx = W / 2f + mw / 2f + 10f;
        box(c, cx, my - 24f, cx + cw, my + 24f, 24f,
                closeMult > 1.01f ? 0x224FD8FF : 0x12FFFFFF,
                closeMult > 1.01f ? 0x884FD8FF : 0);
        textC(c, closeText, cx + cw / 2f, my, 25f,
                closeMult > 1.01f ? C_MAGENTA : 0x889FB8CC, tfBlack, Paint.Align.CENTER);

        if (combo >= 2) {
            float pop = 1f + 0.30f * comboPop;
            float a = Math.min(1f, comboTimer / 0.6f);
            c.save();
            c.scale(pop, pop, W / 2f, top + 262f);
            text(c, sCombo + " x" + combo, W / 2f, top + 272f, 30f,
                    Color.argb((int) (235f * a), 255, 215, 94),
                    tfBlack, Paint.Align.CENTER);
            c.restore();
        }

        float hy = top + 66f;
        if (zenMode) {
            float zw = measure(sZen, 26f, tfBlack) + 44f;
            box(c, UI_PAD, hy - 26f, UI_PAD + zw, hy + 26f, 26f, 0x22B6FF5C, 0x88B6FF5C);
            textC(c, sZen, UI_PAD + zw / 2f, hy, 26f, C_LIME, tfBlack, Paint.Align.CENTER);
        } else {
            for (int i = 0; i < maxLives; i++) {
                float hx = UI_PAD + 22f + i * 54f;
                if (i < lives) {
                    drawGlow(c, hx, hy, 90f, C_RED, 120);
                    drawHeart(c, hx, hy, 21f, C_RED, 255);
                    drawHeart(c, hx - 5f, hy - 5f, 7f, 0xFFFFD9E0, 120);
                } else {
                    drawHeart(c, hx, hy, 21f, C_RED, 40);
                }
            }
        }

        // Активные бонусы: иконка в кольце, кольцо — оставшееся время.
        float ex = UI_PAD + 24f;
        float ey = top + 140f;
        if (shieldCharges > 0) {
            hudEffect(c, P_SHIELD, ex, ey, 1f);
            ex += 68f;
        }
        if (magnetTimer > 0f) {
            hudEffect(c, P_MAGNET, ex, ey, magnetTimer / MAGNET_TIME);
            ex += 68f;
        }
        if (slowTimer > 0f) {
            hudEffect(c, P_SLOW, ex, ey, slowTimer / SLOW_TIME);
            ex += 68f;
        }
        if (phaseTimer > 0f) {
            hudEffect(c, P_PHASE, ex, ey, phaseTimer / PHASE_TIME);
            ex += 68f;
        }
        if (doubleTimer > 0f) {
            hudEffect(c, P_DOUBLE, ex, ey, doubleTimer / DOUBLE_TIME);
        }

        float bx = pauseBtnX();
        float by = pauseBtnY();
        boolean down = state == ST_PLAY && pressed(B_PAUSE);
        pFill.setColor(down ? UI_SURFACE_HI : 0xB30E1630);
        c.drawCircle(bx, by, 42f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(0x33FFFFFF);
        pStroke.setStrokeWidth(2f);
        c.drawCircle(bx, by, 42f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        box(c, bx - 12f, by - 15f, bx - 5f, by + 15f, 3f, 0xEEDFF5FF, 0);
        box(c, bx + 5f, by - 15f, bx + 12f, by + 15f, 3f, 0xEEDFF5FF, 0);
        if (state == ST_PLAY) {
            addHit(B_PAUSE, bx - 70f, by - 70f, bx + 70f, by + 70f);
        }
    }

    private void hudEffect(Canvas c, int kind, float x, float y, float frac) {
        int col = bonusColor(kind);
        pFill.setColor(0xB30E1630);
        c.drawCircle(x, y, 28f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(4f);
        pStroke.setColor(0x22FFFFFF);
        c.drawCircle(x, y, 28f, pStroke);
        pStroke.setColor(col);
        pStroke.setStrokeCap(Paint.Cap.ROUND);
        tmpRect.set(x - 28f, y - 28f, x + 28f, y + 28f);
        c.drawArc(tmpRect, -90f, 360f * clamp(frac, 0f, 1f), false, pStroke);
        strokeReset();
        drawBonusIcon(c, kind, x, y, 14f, 240);
    }

    private void drawResumeCountdown(Canvas c) {
        if (resumeTimer <= 0f) {
            return;
        }
        int n = (int) Math.ceil(resumeTimer / (RESUME_TIME / 3f));
        float local = resumeTimer - (n - 1) * (RESUME_TIME / 3f);
        float t = clamp(local / (RESUME_TIME / 3f), 0f, 1f);
        float sz = 150f + 70f * t;
        textGlow(c, String.valueOf(Math.max(1, n)), W / 2f, worldH * 0.45f + sz * 0.35f, sz,
                Color.argb((int) (255f * Math.min(1f, t * 1.6f)), 255, 255, 255),
                0xCC5FF3FF, tfBlack);
    }

    private void drawTutorial(Canvas c) {
        if (tutorialDone || elapsed > 6.5f) {
            return;
        }
        float a = clamp(elapsed / 0.6f, 0f, 1f)
                * clamp((6.5f - elapsed) / 1.0f, 0f, 1f);
        if (a <= 0.01f) {
            return;
        }

        float x = W / 2f + (float) Math.sin(time * 1.8f) * W * 0.17f;
        float yLo = playerMinY();
        float yHi = Math.max(yLo, worldH - insetBottom - 50f);
        float y = clamp(py + Math.max(effectiveFingerOffset(), 70f), yLo, yHi)
                + (float) Math.sin(time * 2.4f) * 7f;

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(C_CYAN);
        pStroke.setAlpha((int) (70f * a));
        pStroke.setStrokeWidth(2f);
        c.drawLine(px, py + PLAYER_R + 6f, x, y - 40f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        drawGlow(c, x, y, 190f, C_CYAN, (int) (120f * a));
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(C_CYAN);
        pStroke.setAlpha((int) (220f * a));
        pStroke.setStrokeWidth(4f);
        c.drawCircle(x, y, 38f, pStroke);
        pStroke.setColor(0xFFFFFFFF);
        pStroke.setAlpha((int) (120f * a));
        pStroke.setStrokeWidth(1.5f);
        c.drawCircle(x, y, 52f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        pFill.setColor(0xFFFFFFFF);
        pFill.setAlpha((int) (215f * a));
        c.drawCircle(x, y, 11f, pFill);

        float textY = Math.max(insetTop + 340f, py - 262f);
        float tw = measure(sTutorial, 32f, tfMed) + 64f;
        box(c, W / 2f - tw / 2f, textY - 46f, W / 2f + tw / 2f, textY + 24f, 35f,
                Color.argb((int) (200f * a), 14, 22, 48), 0);
        text(c, sTutorial, W / 2f, textY, 32f,
                Color.argb((int) (240f * a), 232, 247, 255), tfMed, Paint.Align.CENTER);
    }

    private void drawAnnounce(Canvas c) {
        if (announceTimer <= 0f) {
            return;
        }
        float a;
        if (announceTimer > 1.15f) {
            a = (1.7f - announceTimer) / 0.55f;
        } else {
            a = announceTimer / 1.15f;
        }
        a = clamp(a, 0f, 1f);
        textGlow(c, sSpeedUp, W / 2f, worldH * 0.32f, 56f,
                Color.argb((int) (255f * a), 255, 255, 255),
                Color.argb((int) (170f * a), 255, 79, 216), tfBlack);
    }

    // ==================================================================
    //  Главное меню
    // ==================================================================

    /** Меню раскладывается снизу вверх: кнопки у большого пальца, сцена сверху. */
    private void layoutMenu() {
        float bottom = worldH - insetBottom - 36f;
        mNavCy = bottom - 78f;
        float navTop = mNavCy - 78f;
        mDailyCy = navTop - 28f - 58f;
        float dailyTop = mDailyCy - 58f;
        mPlayCy = dailyTop - 24f - 74f;
        float playTop = mPlayCy - 74f;
        mInfoY = playTop - 34f;
        mSegCy = mInfoY - 46f - 48f;
        float segTop = mSegCy - 48f;
        mTopBarY = insetTop + 76f;
        float free = segTop - (mTopBarY + 48f);
        mLogoSize = free > 820f ? 150f : (free > 620f ? 126f : 100f);
        float logoBlock = mLogoSize * 1.9f + 60f;
        float heroSpace = Math.max(0f, free - logoBlock);
        mLogoY = mTopBarY + 48f + Math.max(24f, heroSpace * 0.22f) + mLogoSize * 0.80f;
        float logoBottom = mLogoY + mLogoSize * 0.92f + 60f;
        mHeroY = (logoBottom + segTop) / 2f;
    }

    private void drawMenuDecor(Canvas c) {
        float h = mHeroY;
        float spread = Math.max(120f, Math.min(260f, (mSegCy - mLogoY) * 0.30f));
        drawHex(c, W * 0.16f, h - spread * 0.55f + (float) Math.sin(time * 0.8f) * 16f,
                62f, time * 24f, C_MAGENTA);
        drawHex(c, W * 0.86f, h - spread * 1.05f + (float) Math.sin(time * 0.6f + 1.4f) * 20f,
                44f, -time * 18f, C_ORANGE);
        drawHex(c, W * 0.80f, h + spread * 0.55f + (float) Math.sin(time * 0.7f + 2.2f) * 14f,
                30f, time * 30f, C_LIME);
        float sy = h + (float) Math.sin(time * 2f) * 10f;
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(0x285FF3FF);
        tmpRect.set(W / 2f - 190f, sy - 46f, W / 2f + 190f, sy + 46f);
        c.drawOval(tmpRect, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        drawPlayer(c, W / 2f, sy);
        drawParticles(c);
    }

    private void drawMenu(Canvas c) {
        float cx = W / 2f;
        float l = UI_PAD;
        float r = W - UI_PAD;

        // Верхняя строка: неон (ведёт в скины) и настройки.
        String neon = fmt(progress.pointsTotal);
        float nw = measure(neon, 30f, tfBlack) + 104f;
        boolean nDown = pressed(B_SKINS + 1000);
        box(c, l, mTopBarY - 36f, l + nw, mTopBarY + 36f, 36f,
                nDown ? UI_SURFACE_HI : 0xB30E1630, 0x55FFD75E);
        drawGlow(c, l + 40f, mTopBarY, 80f, C_GOLD, 90);
        iconDiamond(c, l + 40f, mTopBarY, 17f, C_GOLD);
        textC(c, neon, l + 70f, mTopBarY, 30f, 0xFFFFE9A6, tfBlack, Paint.Align.LEFT);
        addHit(B_SKINS + 1000, l, mTopBarY - 44f, l + nw, mTopBarY + 44f);

        float gx = r - 40f;
        boolean gDown = pressed(B_SETTINGS);
        pFill.setColor(gDown ? UI_SURFACE_HI : 0xB30E1630);
        c.drawCircle(gx, mTopBarY, 40f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(UI_LINE);
        c.drawCircle(gx, mTopBarY, 40f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        iconGear(c, gx, mTopBarY, 20f, 0xDDDFF5FF);
        addHit(B_SETTINGS, gx - 56f, mTopBarY - 56f, gx + 56f, mTopBarY + 56f);

        // Логотип: два слова, второе — магента, как у сферы и астероида.
        textGlow(c, "NEON", cx, mLogoY, mLogoSize, 0xFFFFFFFF, C_CYAN, tfBlack);
        textGlow(c, "DODGE", cx, mLogoY + mLogoSize * 0.92f, mLogoSize,
                0xFFFFECFB, C_MAGENTA, tfBlack);
        text(c, sTagline, cx, mLogoY + mLogoSize * 0.92f + 58f, 28f,
                0x99CFE8F5, tfMed, Paint.Align.CENTER);

        // Выбор режима — сегменты в цвете режима.
        float segH = 96f;
        box(c, l, mSegCy - segH / 2f, r, mSegCy + segH / 2f, segH / 2f, 0xCC0A1226, UI_LINE);
        float segW = (r - l - 12f) / Progress.MODE_COUNT;
        for (int i = 0; i < Progress.MODE_COUNT; i++) {
            float sl = l + 6f + i * segW;
            float sr = sl + segW;
            boolean sel = i == mode;
            int mc = modeColor(i);
            if (sel) {
                box(c, sl, mSegCy - segH / 2f + 6f, sr, mSegCy + segH / 2f - 6f,
                        (segH - 12f) / 2f, withAlpha(mc, 0x30), withAlpha(mc, 0xD0));
            } else if (pressed(B_MODE + i)) {
                box(c, sl, mSegCy - segH / 2f + 6f, sr, mSegCy + segH / 2f - 6f,
                        (segH - 12f) / 2f, 0x14FFFFFF, 0);
            }
            textC(c, sModeNames[i], (sl + sr) / 2f, mSegCy, 30f,
                    sel ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(B_MODE + i, sl, mSegCy - segH / 2f, sr, mSegCy + segH / 2f);
        }

        text(c, sModeHints[mode], l + 8f, mInfoY, 25f, UI_FOG, tfMed, Paint.Align.LEFT);
        String best = sBest + "  " + fmt(progress.bestForMode(mode));
        text(c, best, r - 8f, mInfoY, 27f, UI_INK, tfBold, Paint.Align.RIGHT);

        float breathe = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
        primaryButton(c, B_PLAY, cx, mPlayCy, r - l, 148f, sTapStart, C_CYAN, breathe);

        drawDailyCard(c, l, r);
        drawNav(c, l, r);
    }

    private void drawDailyCard(Canvas c, float l, float r) {
        boolean done = progress.challengeDone;
        float t = mDailyCy - 58f;
        float b = mDailyCy + 58f;
        boolean down = pressed(B_DAILY);
        card(c, l, t, r, b, done ? 0x66FF4FD8 : C_MAGENTA, false);
        if (down) {
            box(c, l, t, r, b, 28f, 0x18FFFFFF, 0);
        }
        float ix = l + 74f;
        if (!done) {
            drawGlow(c, ix, mDailyCy, 120f, C_MAGENTA, 100);
        }
        // Четырёхлучевая вспышка — «событие дня».
        iconPath.reset();
        float s = 22f;
        iconPath.moveTo(ix, mDailyCy - s);
        iconPath.quadTo(ix, mDailyCy, ix + s, mDailyCy);
        iconPath.quadTo(ix, mDailyCy, ix, mDailyCy + s);
        iconPath.quadTo(ix, mDailyCy, ix - s, mDailyCy);
        iconPath.quadTo(ix, mDailyCy, ix, mDailyCy - s);
        iconPath.close();
        pFill.setColor(done ? 0x88FF4FD8 : C_MAGENTA);
        c.drawPath(iconPath, pFill);

        text(c, sDailyChallenge, l + 124f, mDailyCy - 6f, 30f,
                done ? UI_FOG : UI_INK, tfBold, Paint.Align.LEFT);
        String sub = done
                ? sDailyDone + " · " + fmt(progress.challengeBest)
                : sDailyHint;
        text(c, sub, l + 124f, mDailyCy + 32f, 23f,
                done ? UI_DIM : 0xCCFF9FE9, tfMed, Paint.Align.LEFT);
        if (done) {
            iconCheck(c, r - 50f, mDailyCy, 18f, C_MINT, 5f);
        } else {
            iconChevron(c, r - 48f, mDailyCy, 14f, 0xCCFF9FE9, false);
        }
        addHit(B_DAILY, l, t, r, b);
    }

    private void drawNav(Canvas c, float l, float r) {
        float gap = 18f;
        float tw = (r - l - gap * 3f) / 4f;
        float th = 156f;
        int[] ids = {B_QUESTS, B_SKINS, B_STATS, B_RULES};
        String[] labels = {sQuests, sSkins, sStats, sRulesButton};
        int done = 0;
        for (int i = 0; i < 3; i++) {
            if (progress.questDone[i]) {
                done++;
            }
        }
        for (int i = 0; i < 4; i++) {
            float tl = l + i * (tw + gap);
            float tr = tl + tw;
            float tcx = (tl + tr) / 2f;
            boolean down = pressed(ids[i]);
            float in = down ? 4f : 0f;
            box(c, tl + in, mNavCy - th / 2f + in, tr - in, mNavCy + th / 2f - in, 30f,
                    down ? UI_SURFACE_HI : UI_SURFACE, UI_LINE);
            float iy = mNavCy - 24f;
            int ic = 0xFFBFEFFF;
            switch (i) {
                case 0:
                    iconFlag(c, tcx, iy, 24f, ic);
                    break;
                case 1:
                    iconOrb(c, tcx, iy, 26f, ic);
                    break;
                case 2:
                    iconBars(c, tcx, iy, 22f, ic);
                    break;
                default:
                    iconDoc(c, tcx, iy, 24f, ic);
                    break;
            }
            textC(c, labels[i], tcx, mNavCy + 42f, 25f, 0xFFCFE3F0, tfCond, Paint.Align.CENTER);
            addHit(ids[i], tl, mNavCy - th / 2f, tr, mNavCy + th / 2f);

            if (i == 0) {
                // Бейдж заданий: золото — есть что делать, мята — всё сделано.
                String badge = done + "/3";
                float bw = measure(badge, 21f, tfBlack) + 26f;
                float bx = tr - 14f - bw;
                float by = mNavCy - th / 2f + 16f;
                boolean all = done >= 3;
                box(c, bx, by, bx + bw, by + 36f, 18f, all ? C_MINT : C_GOLD, 0);
                textC(c, badge, bx + bw / 2f, by + 18f, 21f, UI_ON_ACCENT, tfBlack,
                        Paint.Align.CENTER);
            }
        }
    }

    // ==================================================================
    //  Общий каркас вложенных экранов
    // ==================================================================

    private void drawScreenBg(Canvas c) {
        pFill.setColor(0xF0060914);
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);
    }

    /** Кнопка «назад», заголовок; возвращает верх области содержимого. */
    private float drawHeader(Canvas c, String title) {
        float cy = insetTop + 84f;
        float bx = UI_PAD + 42f;
        boolean down = pressed(B_BACK);
        pFill.setColor(down ? UI_SURFACE_HI : UI_SURFACE);
        c.drawCircle(bx, cy, 42f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(UI_LINE);
        c.drawCircle(bx, cy, 42f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        iconChevron(c, bx - 3f, cy, 14f, UI_INK, true);
        addHit(B_BACK, bx - 60f, cy - 60f, bx + 60f, cy + 60f);
        text(c, title, bx + 70f, cy + 18f, 50f, UI_INK, tfBlack, Paint.Align.LEFT);
        return cy + 70f;
    }

    private void drawNeonPill(Canvas c, float right, float cy) {
        String neon = fmt(progress.pointsTotal);
        float nw = measure(neon, 27f, tfBlack) + 88f;
        box(c, right - nw, cy - 32f, right, cy + 32f, 32f, 0xB30E1630, 0x55FFD75E);
        iconDiamond(c, right - nw + 36f, cy, 15f, C_GOLD);
        textC(c, neon, right - nw + 62f, cy, 27f, 0xFFFFE9A6, tfBlack, Paint.Align.LEFT);
    }

    private float beginScroll(Canvas c, float top) {
        scrollTop = top;
        scrollBottom = worldH - insetBottom - 6f;
        c.save();
        c.clipRect(0f, scrollTop, W, scrollBottom);
        clipping = true;
        return top + 20f - scrollY;
    }

    private void endScroll(Canvas c, float y) {
        c.restore();
        clipping = false;
        float contentBottom = y + scrollY;
        scrollMax = Math.max(0f, contentBottom + 40f - scrollBottom);
        if (scrollY > scrollMax) {
            scrollY = scrollMax;
            scrollVel = 0f;
        }
        if (scrollY > 1f) {
            pFill.setColor(UI_LINE);
            c.drawRect(0f, scrollTop, W, scrollTop + 2f, pFill);
        }
        if (scrollMax > 0f) {
            float trackTop = scrollTop + 12f;
            float trackBottom = scrollBottom - 12f;
            float span = trackBottom - trackTop;
            float thumbH = Math.max(70f, span * span / (span + scrollMax));
            float thumbTop = trackTop + (span - thumbH) * (scrollY / scrollMax);
            box(c, W - 14f, thumbTop, W - 8f, thumbTop + thumbH, 3f,
                    scrollDragging || scrollVel != 0f ? 0xAA5FF3FF : 0x445FF3FF, 0);
        }
    }

    /** Строка-ссылка внутри экрана: карточка с подписью, счётчиком и шевроном. */
    private float linkRow(Canvas c, int id, String label, String value, float y) {
        float l = UI_PAD;
        float r = W - UI_PAD;
        float h = 104f;
        boolean down = pressed(id);
        card(c, l, y, r, y + h, 0, false);
        if (down) {
            box(c, l, y, r, y + h, 28f, 0x18FFFFFF, 0);
        }
        textC(c, label, l + 36f, y + h / 2f, 30f, UI_INK, tfBold, Paint.Align.LEFT);
        if (value.length() > 0) {
            textC(c, value, r - 76f, y + h / 2f, 28f, UI_FOG, tfBold, Paint.Align.RIGHT);
        }
        iconChevron(c, r - 40f, y + h / 2f, 13f, UI_FOG, false);
        addHit(id, l, y, r, y + h);
        return y + h;
    }

    // ==================================================================
    //  Статистика и история
    // ==================================================================

    private void drawStats(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sStats);
        float y = beginScroll(c, top);
        float l = UI_PAD;
        float r = W - UI_PAD;

        y = sectionLabel(c, sRecords, y);
        float rowH = 88f;
        card(c, l, y, r, y + rowH * 4f, 0, false);
        for (int i = 0; i < 4; i++) {
            float ry = y + rowH * i + rowH / 2f;
            int type = i < 3 ? i : Progress.RUN_DAILY;
            if (i > 0) {
                pFill.setColor(UI_LINE);
                c.drawRect(l + 72f, y + rowH * i - 1f, r - 32f, y + rowH * i + 1f, pFill);
            }
            pFill.setColor(historyColor(type));
            c.drawCircle(l + 44f, ry, 9f, pFill);
            textC(c, i < 3 ? sModeNames[i] : sDailyChallenge, l + 72f, ry, 29f,
                    UI_INK, tfMed, Paint.Align.LEFT);
            int v = i < 3 ? progress.bestForMode(i) : progress.challengeBest;
            textC(c, fmt(v), r - 36f, ry, 34f, v > 0 ? UI_INK : UI_DIM, tfBlack,
                    Paint.Align.RIGHT);
        }
        y += rowH * 4f + 20f;

        int hc = progress.runHistory.size();
        y = linkRow(c, B_HISTORY, sHistory, hc > 0 ? String.valueOf(hc) : "", y);
        y += 32f;

        y = sectionLabel(c, sTotals, y);
        String[] values = {
                fmt(progress.dodged),
                "x" + fmt(progress.bestCombo),
                formatTime((int) progress.totalTime),
                fmt(progress.stars),
                fmt(progress.runs),
                fmt(progress.pointsTotal)
        };
        float gap = 18f;
        float tw = (r - l - gap * 2f) / 3f;
        float th = 140f;
        for (int i = 0; i < 6; i++) {
            float tl = l + (i % 3) * (tw + gap);
            float tt = y + (i / 3) * (th + gap);
            card(c, tl, tt, tl + tw, tt + th, 0, false);
            boolean neon = i == 5;
            text(c, values[i], tl + tw / 2f, tt + 72f, 40f,
                    neon ? 0xFFFFE9A6 : UI_INK, tfBlack, Paint.Align.CENTER);
            text(c, sStatName[i], tl + tw / 2f, tt + 112f, 22f, UI_FOG, tfMed,
                    Paint.Align.CENTER);
        }
        y += th * 2f + gap + 32f;

        int unlocked = 0;
        for (int i = 0; i < Progress.ACH_COUNT; i++) {
            if (progress.achUnlocked(i)) {
                unlocked++;
            }
        }
        y = sectionLabel(c, sAchievements + "  " + unlocked + "/" + Progress.ACH_COUNT, y);
        for (int i = 0; i < Progress.ACH_COUNT; i++) {
            boolean un = progress.achUnlocked(i);
            float h = 116f;
            card(c, l, y, r, y + h, 0, false);
            float ix = l + 58f;
            float iy = y + h / 2f;
            if (un) {
                pFill.setColor(C_MINT);
                c.drawCircle(ix, iy, 24f, pFill);
                iconCheck(c, ix, iy, 13f, UI_ON_ACCENT, 4.5f);
            } else {
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setStrokeWidth(2.5f);
                pStroke.setColor(0x33FFFFFF);
                c.drawCircle(ix, iy, 22f, pStroke);
                pStroke.setStyle(Paint.Style.FILL);
            }
            text(c, sAchName[i], l + 108f, y + 50f, 29f, un ? UI_INK : 0xFFC4D4E2,
                    tfBold, Paint.Align.LEFT);
            int val = progress.achValue(i);
            int tgt = progress.achTarget(i);
            boolean timeLike = i == Progress.ACH_TIME_10 || i == Progress.ACH_SURVIVE_120;
            String pr = timeLike
                    ? formatTime(Math.min(val, tgt)) + " / " + formatTime(tgt)
                    : fmt(Math.min(val, tgt)) + " / " + fmt(tgt);
            text(c, pr, r - 32f, y + 50f, 23f, un ? 0xCC7CF9D0 : UI_FOG, tfMed,
                    Paint.Align.RIGHT);
            progressBar(c, l + 108f, r - 32f, y + 82f, 8f, (float) val / tgt,
                    un ? C_MINT : 0xCC5FF3FF);
            y += h + 14f;
        }
        endScroll(c, y);
    }

    private void drawHistory(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sHistory);
        float l = UI_PAD;
        float r = W - UI_PAD;
        int count = progress.runHistory.size();
        if (count == 0) {
            float cy = (top + worldH - insetBottom) / 2f - 80f;
            iconBars(c, W / 2f, cy - 90f, 44f, 0x555FF3FF);
            drawWrapped(c, sHistoryEmpty, W / 2f, cy + 20f, 30f, W - 220f,
                    UI_FOG, tfMed, Paint.Align.CENTER, 42f);
            primaryButton(c, B_EMPTY_PLAY, W / 2f, cy + 170f, 560f, 128f,
                    sHistoryCta, C_CYAN, 0f);
            return;
        }
        float y = beginScroll(c, top);
        for (int i = 0; i < count; i++) {
            Progress.RunRecord rec = progress.runHistory.get(i);
            float h = 124f;
            card(c, l, y, r, y + h, historyColor(rec.type), false);
            String modeLabel = rec.type == Progress.RUN_DAILY
                    ? sDailyChallenge : sModeNames[rec.type];
            text(c, modeLabel, l + 44f, y + 54f, 29f, UI_INK, tfBold, Paint.Align.LEFT);
            text(c, rec.date, l + 44f, y + 92f, 23f, UI_FOG, tfMed, Paint.Align.LEFT);
            text(c, fmt(rec.score), r - 36f, y + 60f, 42f, UI_INK, tfBlack,
                    Paint.Align.RIGHT);
            text(c, formatTime(rec.seconds), r - 36f, y + 96f, 23f, UI_FOG, tfMed,
                    Paint.Align.RIGHT);
            y += h + 14f;
        }
        endScroll(c, y);
    }

    private int historyColor(int type) {
        switch (type) {
            case Progress.MODE_HARDCORE:
                return C_RED;
            case Progress.MODE_ZEN:
                return C_LIME;
            case Progress.RUN_DAILY:
                return C_MAGENTA;
            default:
                return C_CYAN;
        }
    }

    // ==================================================================
    //  Задания
    // ==================================================================

    private void drawQuests(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sQuests);
        float y = beginScroll(c, top);
        float l = UI_PAD;
        float r = W - UI_PAD;

        int done = 0;
        for (int i = 0; i < 3; i++) {
            if (progress.questDone[i]) {
                done++;
            }
        }
        boolean all = done >= 3;
        text(c, String.format(sQuestsSummary, done), l + 8f, y + 40f, 34f,
                all ? C_MINT : UI_INK, tfBlack, Paint.Align.LEFT);
        text(c, all ? sAllQuestsDone : sQuestsRefresh, l + 8f, y + 82f, 25f,
                UI_FOG, tfMed, Paint.Align.LEFT);
        y += 116f;

        for (int i = 0; i < 3; i++) {
            int id = progress.questIds[i];
            int value = progress.questProgress[i];
            int target = Progress.QUEST_TARGETS[id];
            boolean qd = progress.questDone[i];
            float textW = r - l - 88f;
            int lines = countWrapped(sQuestName[id], 31f, textW, tfMed);
            float h = 150f + lines * 42f;
            card(c, l, y, r, y + h, qd ? C_MINT : C_GOLD, false);
            drawWrapped(c, sQuestName[id], l + 44f, y + 58f, 31f, textW,
                    qd ? 0xFFC4D4E2 : UI_INK, tfMed, Paint.Align.LEFT, 42f);
            float barY = y + h - 78f;
            progressBar(c, l + 44f, r - 40f, barY, 12f, (float) value / target,
                    qd ? C_MINT : C_CYAN);
            boolean timeQuest = id == Progress.QUEST_TIME_DAY;
            String pr = timeQuest
                    ? formatTime(Math.min(value, target)) + " / " + formatTime(target)
                    : fmt(Math.min(value, target)) + " / " + fmt(target);
            float by = y + h - 32f;
            text(c, pr, l + 44f, by, 25f, UI_FOG, tfBold, Paint.Align.LEFT);
            if (qd) {
                float tw = measure(sQuestDone, 25f, tfBold);
                text(c, sQuestDone, r - 40f, by, 25f, C_MINT, tfBold, Paint.Align.RIGHT);
                iconCheck(c, r - 40f - tw - 26f, by - 9f, 13f, C_MINT, 4f);
            } else {
                float tw = measure(sQuestReward, 25f, tfBold);
                text(c, sQuestReward, r - 40f, by, 25f, C_GOLD, tfBold, Paint.Align.RIGHT);
                iconDiamond(c, r - 40f - tw - 20f, by - 9f, 11f, C_GOLD);
            }
            y += h + 18f;
        }
        endScroll(c, y);
    }

    // ==================================================================
    //  Скины
    // ==================================================================

    private void drawSkins(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sSkins);
        drawNeonPill(c, W - UI_PAD, insetTop + 84f);
        float l = UI_PAD;
        float r = W - UI_PAD;

        // Вкладки закреплены над прокруткой.
        float segH = 84f;
        float sy = top + 14f + segH / 2f;
        box(c, l, sy - segH / 2f, r, sy + segH / 2f, segH / 2f, 0xCC0A1226, UI_LINE);
        float segW = (r - l - 12f) / 2f;
        for (int i = 0; i < 2; i++) {
            float sl = l + 6f + i * segW;
            float sr = sl + segW;
            boolean sel = skinTab == i;
            if (sel) {
                box(c, sl, sy - segH / 2f + 6f, sr, sy + segH / 2f - 6f,
                        (segH - 12f) / 2f, 0x305FF3FF, 0xCC5FF3FF);
            }
            textC(c, i == 0 ? sSphere : sTrail, (sl + sr) / 2f, sy, 29f,
                    sel ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(B_TAB + i, sl, sy - segH / 2f, sr, sy + segH / 2f);
        }

        float y = beginScroll(c, sy + segH / 2f + 8f);

        // Превью: выбранная сфера летит со своим шлейфом.
        float ph = 250f;
        float pcy = y + ph / 2f;
        float ox = W / 2f + (float) Math.sin(time * 1.3f) * 150f;
        float oy = pcy - 14f + (float) Math.cos(time * 2.6f) * 10f;
        int ti = clamp(progress.trailSkin, 0, Progress.TRAIL_COUNT - 1);
        for (int k = 12; k >= 1; k--) {
            float tt = time - k * 0.045f;
            float tx = W / 2f + (float) Math.sin(tt * 1.3f) * 150f;
            float tyy = pcy - 14f + (float) Math.cos(tt * 2.6f) * 10f + k * 9f;
            pFill.setColor(TRAIL_COLORS[ti]);
            pFill.setAlpha((int) (Color.alpha(TRAIL_COLORS[ti]) * (1f - k / 13f)));
            c.drawCircle(tx, tyy, (11f - k * 0.6f) * TRAIL_SIZE[ti], pFill);
        }
        drawPlayer(c, ox, oy);
        y += ph + 10f;

        int count = skinTab == 0 ? Progress.SPHERE_COUNT : Progress.TRAIL_COUNT;
        float gap = 18f;
        float cw = (r - l - gap) / 2f;
        float ch = 268f;
        for (int i = 0; i < count; i++) {
            float cl = l + (i % 2) * (cw + gap);
            float ct = y + (i / 2) * (ch + gap);
            boolean unlocked = skinTab == 0
                    ? progress.sphereUnlocked(i) : progress.trailUnlocked(i);
            boolean selected = skinTab == 0
                    ? progress.sphereSkin == i : progress.trailSkin == i;
            drawSkinCard(c, cl, ct, cw, ch, i, unlocked, selected);
        }
        y += ((count + 1) / 2) * (ch + gap);
        endScroll(c, y);
    }

    private void drawSkinCard(Canvas c, float l, float t, float w, float h, int index,
                              boolean unlocked, boolean selected) {
        int id = B_SKIN + index;
        boolean down = pressed(id);
        card(c, l, t, l + w, t + h, 0, selected);
        if (down) {
            box(c, l, t, l + w, t + h, 28f, 0x18FFFFFF, 0);
        }
        float cx = l + w / 2f;
        float sy = t + 82f;
        float a = unlocked ? 1f : 0.35f;
        if (skinTab == 0) {
            int outer = SPHERE_OUTER[index];
            int inner = SPHERE_INNER[index];
            drawGlow(c, cx, sy, 150f, outer, (int) (140f * a));
            pFill.setColor(outer);
            pFill.setAlpha((int) (240f * a));
            c.drawCircle(cx, sy, 36f, pFill);
            pFill.setColor(inner);
            pFill.setAlpha((int) (255f * a));
            c.drawCircle(cx, sy, 23f, pFill);
            pFill.setColor(0xFFFFFFFF);
            pFill.setAlpha((int) (255f * a));
            c.drawCircle(cx, sy, 9f, pFill);
        } else {
            int col = TRAIL_COLORS[index];
            for (int k = 0; k < 7; k++) {
                pFill.setColor(col);
                pFill.setAlpha((int) (Color.alpha(col) * a * (1f - k / 7f)));
                c.drawCircle(cx + 54f - k * 17f, sy + (k % 2 == 0 ? -3f : 4f),
                        (11f - k * 0.8f) * TRAIL_SIZE[index], pFill);
            }
            int si = clamp(progress.sphereSkin, 0, Progress.SPHERE_COUNT - 1);
            pFill.setColor(SPHERE_OUTER[si]);
            pFill.setAlpha((int) (235f * a));
            c.drawCircle(cx + 58f, sy, 20f, pFill);
            pFill.setColor(0xFFFFFFFF);
            pFill.setAlpha((int) (255f * a));
            c.drawCircle(cx + 58f, sy, 8f, pFill);
        }
        if (!unlocked) {
            pFill.setColor(0xE00E1630);
            c.drawCircle(l + w - 44f, t + 44f, 24f, pFill);
            iconLock(c, l + w - 44f, t + 46f, 13f, UI_FOG);
        }

        text(c, skinTab == 0 ? sSphereName[index] : sTrailName[index],
                cx, t + 168f, 30f, unlocked ? UI_INK : 0xFFAFC0CF,
                tfBlack, Paint.Align.CENTER);

        float sy2 = t + 212f;
        if (selected) {
            float tw = measure(sSelected, 24f, tfBold);
            text(c, sSelected, cx + 16f, sy2, 24f, C_MINT, tfBold, Paint.Align.CENTER);
            iconCheck(c, cx - tw / 2f - 6f, sy2 - 8f, 12f, C_MINT, 4f);
        } else if (unlocked) {
            text(c, sSelect, cx, sy2, 24f, 0xFF9FE9FF, tfBold, Paint.Align.CENTER);
        } else {
            String cond = skinTab == 0 ? sSphereCond[index] : sTrailCond[index];
            drawWrapped(c, cond, cx, sy2 - 6f, 21f, w - 40f, UI_FOG, tfMed,
                    Paint.Align.CENTER, 27f);
            float ratio = skinTab == 0
                    ? progress.sphereRatio(index) : progress.trailRatio(index);
            progressBar(c, l + 40f, l + w - 40f, t + h - 22f, 6f, ratio, 0xCCFFD75E);
        }
        addHit(id, l, t, l + w, t + h);
    }

    // ==================================================================
    //  Правила
    // ==================================================================

    private void drawRules(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sRules);
        float y = beginScroll(c, top) + 10f;

        y = rulesHeader(c, sRulesHowTo, y);
        for (int i = 0; i < sRuleLine.length; i++) {
            y = rulesBullet(c, sRuleLine[i], y);
        }

        y = rulesHeader(c, sRulesMultiplier, y + 18f);
        y = rulesBody(c, sRulesMultiplierDesc, y);

        y = rulesHeader(c, sRulesProgress, y + 18f);
        for (int i = 0; i < sRuleProgress.length; i++) {
            y = rulesBullet(c, sRuleProgress[i], y);
        }

        y = rulesHeader(c, sLegendAsteroids, y + 18f);
        for (int i = 0; i < sAstName.length; i++) {
            y = rulesAsteroid(c, i, y);
        }

        y = rulesHeader(c, sLegendBonuses, y + 18f);
        for (int i = 0; i < 8; i++) {
            y = rulesBonus(c, i, y);
        }
        endScroll(c, y);
    }

    private float rulesHeader(Canvas c, String title, float y) {
        text(c, title, UI_PAD + 8f, y + 40f, 38f, UI_INK, tfBlack, Paint.Align.LEFT);
        return y + 82f;
    }

    private float rulesBullet(Canvas c, String s, float y) {
        pFill.setColor(C_CYAN);
        c.drawCircle(UI_PAD + 18f, y + 2f, 5f, pFill);
        float h = drawWrapped(c, s, UI_PAD + 44f, y + 12f, 28f, W - UI_PAD * 2f - 60f,
                0xFFD6E6F0, tfMed, Paint.Align.LEFT, 40f);
        return y + h + 16f;
    }

    private float rulesBody(Canvas c, String s, float y) {
        float h = drawWrapped(c, s, UI_PAD + 8f, y + 12f, 28f, W - UI_PAD * 2f - 20f,
                0xFFD6E6F0, tfMed, Paint.Align.LEFT, 40f);
        return y + h + 14f;
    }

    private float rulesAsteroid(Canvas c, int index, float y) {
        drawHex(c, UI_PAD + 34f, y + 22f, 26f, time * 16f + index * 24f, OBS_COLORS[index]);
        text(c, sAstName[index], UI_PAD + 92f, y + 22f, 29f, UI_INK,
                tfBold, Paint.Align.LEFT);
        float h = drawWrapped(c, sAstDesc[index], UI_PAD + 92f, y + 58f, 25f,
                W - UI_PAD * 2f - 110f, UI_FOG, tfMed, Paint.Align.LEFT, 34f);
        return y + 36f + h + 22f;
    }

    private float rulesBonus(Canvas c, int index, float y) {
        float ix = UI_PAD + 34f;
        float iy = y + 22f;
        if (index == 0) {
            drawGlow(c, ix, iy, 100f, C_GOLD, 130);
            starPath.reset();
            for (int k = 0; k < 8; k++) {
                float rr = (k % 2 == 0) ? 22f : 8f;
                double ang = Math.toRadians(time * 60f + k * 45f);
                float sx = ix + (float) Math.cos(ang) * rr;
                float sy = iy + (float) Math.sin(ang) * rr;
                if (k == 0) {
                    starPath.moveTo(sx, sy);
                } else {
                    starPath.lineTo(sx, sy);
                }
            }
            starPath.close();
            pFill.setColor(C_GOLD);
            pFill.setAlpha(240);
            c.drawPath(starPath, pFill);
        } else if (index == 1) {
            drawGlow(c, ix, iy, 100f, C_RED, 120);
            drawHeart(c, ix, iy, 21f, C_RED, 255);
        } else {
            drawGlow(c, ix, iy, 100f, bonusColor(index), 130);
            drawBonusIcon(c, index, ix, iy, 22f, 255);
        }
        text(c, sBonusName[index], UI_PAD + 92f, y + 22f, 29f, UI_INK,
                tfBold, Paint.Align.LEFT);
        float h = drawWrapped(c, sBonusDesc[index], UI_PAD + 92f, y + 58f, 25f,
                W - UI_PAD * 2f - 110f, UI_FOG, tfMed, Paint.Align.LEFT, 34f);
        return y + 36f + h + 22f;
    }

    private int countWrapped(String s, float size, float maxW, Typeface tf) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setLetterSpacing(0f);
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && pText.measureText(test) > maxW) {
                lines++;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }
        if (line.length() > 0) {
            lines++;
        }
        return Math.max(1, lines);
    }

    private float drawWrapped(Canvas c, String s, float x, float y, float size,
                              float maxW, int color, Typeface tf,
                              Paint.Align align, float lineH) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setTextAlign(align);
        pText.setLetterSpacing(0f);
        pText.clearShadowLayer();
        pText.setColor(color);

        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && pText.measureText(test) > maxW) {
                c.drawText(line.toString(), x, y, pText);
                y += lineH;
                lines++;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }
        if (line.length() > 0) {
            c.drawText(line.toString(), x, y, pText);
            lines++;
        }
        return Math.max(1, lines) * lineH;
    }

    private void drawToast(Canvas c) {
        if (toastTimer <= 0f || toastText.length() == 0) {
            return;
        }
        float a = clamp(toastTimer / 0.45f, 0f, 1f);
        float y = insetTop + (state == ST_PLAY ? 330f : 200f);
        float tw = Math.min(W - 2f * UI_PAD, measure(toastText, 26f, tfBold) + 110f);
        box(c, W / 2f - tw / 2f, y - 38f, W / 2f + tw / 2f, y + 38f, 38f,
                Color.argb((int) (240f * a), 24, 36, 80),
                Color.argb((int) (160f * a), 255, 215, 94));
        iconDiamond(c, W / 2f - tw / 2f + 44f, y, 13f,
                Color.argb((int) (255f * a), 255, 215, 94));
        textC(c, toastText, W / 2f + 18f, y, 26f,
                Color.argb((int) (250f * a), 255, 236, 180), tfBold, Paint.Align.CENTER);
    }

    private String formatTime(int totalSec) {
        if (totalSec < 0) {
            totalSec = 0;
        }
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        if (h > 0) {
            return h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        }
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    // ==================================================================
    //  Пауза
    // ==================================================================

    private void drawPause(Canvas c) {
        pFill.setColor(0xCC050812);
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);

        float l = UI_PAD;
        float r = W - UI_PAD;
        float pw = r - l;
        float inner = pw - 96f;
        float panelH = 830f;
        float pt = Math.max(insetTop + 40f, (worldH - panelH) / 2f);
        box(c, l, pt, r, pt + panelH, 44f, 0xF00E1630, UI_LINE);
        drawGlow(c, W / 2f, pt, pw * 0.9f, C_CYAN, 30);

        float cx = W / 2f;
        float y = pt + 112f;
        text(c, sPause, cx, y, 72f, UI_INK, tfBlack, Paint.Align.CENTER);
        y += 52f;
        updateShownScore();
        text(c, sScore + " " + shownScoreText + "   ·   " + formatTime((int) elapsed),
                cx, y, 27f, UI_FOG, tfMed, Paint.Align.CENTER);
        y += 104f;

        primaryButton(c, B_CONTINUE, cx, y, inner, 136f, sTapContinue, C_CYAN, 0.5f);
        y += 68f + 34f;

        // Плитки звука: заливка циан — включено, перечёркнутая иконка — выключено.
        float gap = 18f;
        float tw = (inner - gap * 2f) / 3f;
        float th = 150f;
        float tl0 = cx - inner / 2f;
        int[] ids = {B_MUSIC, B_SOUND, B_VIBRATION};
        String[] labels = {sMusic, sSound, sVibration};
        boolean[] on = {musicOn, soundOn, vibOn};
        for (int i = 0; i < 3; i++) {
            float tl = tl0 + i * (tw + gap);
            float tcx = tl + tw / 2f;
            boolean down = pressed(ids[i]);
            float in = down ? 4f : 0f;
            box(c, tl + in, y + in, tl + tw - in, y + th - in, 30f,
                    on[i] ? 0x265FF3FF : 0x10FFFFFF, on[i] ? 0x885FF3FF : UI_LINE);
            int ic = on[i] ? 0xFFBFF6FF : UI_DIM;
            float iy = y + 58f;
            if (i == 0) {
                iconNote(c, tcx, iy, 22f, ic);
            } else if (i == 1) {
                iconSpeaker(c, tcx, iy, 22f, ic);
            } else {
                iconVibrate(c, tcx, iy, 22f, ic);
            }
            if (!on[i]) {
                iconSlash(c, tcx, iy, 24f, ic);
            }
            textC(c, labels[i], tcx, y + 112f, 24f, on[i] ? UI_INK : UI_FOG,
                    tfCond, Paint.Align.CENTER);
            addHit(ids[i], tl, y, tl + tw, y + th);
        }
        y += th + 30f + 56f;

        secondaryButton(c, B_PAUSE_RULES, cx, y, inner, 112f, sRulesButton,
                C_CYAN, UI_INK);
        y += 56f + 22f + 50f;

        boolean armed = exitArmed > 0f;
        boolean down = pressed(B_EXIT);
        box(c, cx - inner / 2f, y - 50f, cx + inner / 2f, y + 50f, 50f,
                down ? 0x22FF4F6A : (armed ? 0x18FF4F6A : 0),
                armed ? 0xAAFF4F6A : 0);
        textC(c, armed ? sExitConfirm : sToMenu, cx, y, armed ? 27f : 30f,
                armed ? 0xFFFF8A9C : UI_FOG, tfCond, Paint.Align.CENTER);
        addHit(B_EXIT, cx - inner / 2f, y - 50f, cx + inner / 2f, y + 50f);
    }

    // ==================================================================
    //  Настройки
    // ==================================================================

    private void drawSettings(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sSettings);
        float y = beginScroll(c, top);
        float l = UI_PAD;
        float r = W - UI_PAD;

        y = sectionLabel(c, sControlMode, y);
        boolean offsetEnabled = controlMode == CONTROL_ABOVE;
        String hint = offsetEnabled ? sOffsetHint : sControlAtHint;
        int hintLines = countWrapped(hint, 24f, r - l - 72f, tfMed);
        float ch = 330f + hintLines * 34f;
        card(c, l, y, r, y + ch, 0, false);

        float segH = 88f;
        float sy = y + 30f + segH / 2f;
        float sl0 = l + 30f;
        float sr0 = r - 30f;
        box(c, sl0, sy - segH / 2f, sr0, sy + segH / 2f, segH / 2f, 0x99060914, UI_LINE);
        float segW = (sr0 - sl0 - 12f) / 2f;
        for (int i = 0; i < 2; i++) {
            float sl = sl0 + 6f + i * segW;
            float sr = sl + segW;
            boolean sel = controlMode == i;
            if (sel) {
                box(c, sl, sy - segH / 2f + 6f, sr, sy + segH / 2f - 6f,
                        (segH - 12f) / 2f, 0x305FF3FF, 0xCC5FF3FF);
            }
            textC(c, i == CONTROL_ABOVE ? sControlAbove : sControlAt, (sl + sr) / 2f, sy,
                    27f, sel ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(B_CONTROL + i, sl, sy - segH / 2f, sr, sy + segH / 2f);
        }

        float ry = sy + segH / 2f + 64f;
        text(c, sOffset, l + 36f, ry, 28f, offsetEnabled ? UI_INK : UI_DIM, tfMed,
                Paint.Align.LEFT);
        text(c, offsetEnabled ? String.valueOf((int) fingerOffset) : "—", r - 36f, ry + 2f,
                36f, offsetEnabled ? C_CYAN : UI_DIM, tfBlack, Paint.Align.RIGHT);

        sliderX1 = l + 56f;
        sliderX2 = r - 56f;
        float sly = ry + 66f;
        box(c, sliderX1, sly - 5f, sliderX2, sly + 5f, 5f,
                offsetEnabled ? 0x33FFFFFF : 0x14FFFFFF, 0);
        float kx = sliderX1 + (sliderX2 - sliderX1)
                * (fingerOffset - OFFSET_MIN) / (OFFSET_MAX - OFFSET_MIN);
        if (kx > sliderX1 + 1f) {
            box(c, sliderX1, sly - 5f, kx, sly + 5f, 5f,
                    offsetEnabled ? C_CYAN : 0x335FF3FF, 0);
        }
        if (offsetEnabled) {
            drawGlow(c, kx, sly, draggingOffset ? 130f : 90f, C_CYAN, 120);
            pFill.setColor(0xFFFFFFFF);
            c.drawCircle(kx, sly, draggingOffset ? 24f : 20f, pFill);
            pFill.setColor(C_CYAN);
            c.drawCircle(kx, sly, 11f, pFill);
            addHit(B_SLIDER, sliderX1 - 40f, sly - 56f, sliderX2 + 40f, sly + 56f);
        } else {
            pFill.setColor(0x55FFFFFF);
            c.drawCircle(kx, sly, 15f, pFill);
        }
        drawWrapped(c, hint, l + 36f, sly + 70f, 24f, r - l - 72f,
                offsetEnabled ? UI_FOG : 0xCC7CF9D0, tfMed, Paint.Align.LEFT, 34f);
        y += ch + 32f;

        y = sectionLabel(c, sSoundSection, y);
        float rowH = 104f;
        card(c, l, y, r, y + rowH * 3f, 0, false);
        int[] ids = {B_MUSIC, B_SOUND, B_VIBRATION};
        String[] labels = {sMusic, sSound, sVibration};
        boolean[] on = {musicOn, soundOn, vibOn};
        for (int i = 0; i < 3; i++) {
            float rt = y + i * rowH;
            float rcy = rt + rowH / 2f;
            if (i > 0) {
                pFill.setColor(UI_LINE);
                c.drawRect(l + 36f, rt - 1f, r - 36f, rt + 1f, pFill);
            }
            if (pressed(ids[i])) {
                box(c, l + 4f, rt + 4f, r - 4f, rt + rowH - 4f, 24f, 0x12FFFFFF, 0);
            }
            textC(c, labels[i], l + 36f, rcy, 29f, UI_INK, tfMed, Paint.Align.LEFT);
            switchToggle(c, r - 84f, rcy, on[i]);
            addHit(ids[i], l, rt, r, rt + rowH);
        }
        y += rowH * 3f + 32f;

        float h = 104f;
        boolean down = pressed(B_TUTORIAL);
        card(c, l, y, r, y + h, 0, false);
        if (down) {
            box(c, l, y, r, y + h, 28f, 0x18FFFFFF, 0);
        }
        textC(c, sReplayTutorial, l + 36f, y + h / 2f, 29f, UI_INK, tfMed, Paint.Align.LEFT);
        iconReplay(c, r - 56f, y + h / 2f, 20f, UI_FOG);
        addHit(B_TUTORIAL, l, y, r, y + h);
        y += h + 40f;

        if (versionName != null && versionName.length() > 0) {
            text(c, "Neon Dodge · " + sVersion + " " + versionName, W / 2f, y + 20f, 23f,
                    UI_DIM, tfMed, Paint.Align.CENTER);
            y += 40f;
        }
        endScroll(c, y);
    }

    // ==================================================================
    //  Итог забега
    // ==================================================================

    private void drawOver(Canvas c) {
        float appear = clamp(overTime / 0.45f, 0f, 1f);
        float ease = 1f - (1f - appear) * (1f - appear) * (1f - appear);
        pFill.setColor(Color.argb((int) (0xCC * ease), 5, 8, 18));
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);

        float l = UI_PAD;
        float r = W - UI_PAD;
        float pw = r - l;
        float inner = pw - 96f;
        float panelH = 1000f;
        float pt = Math.max(insetTop + 30f, (worldH - panelH) / 2f) + (1f - ease) * 80f;
        int saved = ease < 1f
                ? c.saveLayerAlpha(-100f, -100f, W + 100f, worldH + 100f, (int) (255 * ease))
                : c.save();
        box(c, l, pt, r, pt + panelH, 44f, 0xF00E1630, newRecord ? 0x88FFD75E : UI_LINE);
        drawGlow(c, W / 2f, pt, pw * 0.9f, newRecord ? C_GOLD : C_MAGENTA, 34);

        float cx = W / 2f;
        float y = pt + 100f;
        if (newRecord) {
            float pulse = 0.75f + 0.25f * (float) Math.sin(time * 5f);
            textGlow(c, sNewRecord, cx, y, 60f, 0xFFFFF2C4,
                    Color.argb((int) (230f * pulse), 255, 215, 94), tfBlack);
        } else {
            text(c, sGameOver, cx, y, 54f, UI_INK, tfBlack, Paint.Align.CENTER);
        }
        y += 58f;

        String modeLabel = dailyChallenge ? sDailyChallenge : sModeNames[mode];
        int mc = dailyChallenge ? C_MAGENTA : modeColor(mode);
        float mw = measure(modeLabel, 24f, tfCond) + 74f;
        box(c, cx - mw / 2f, y - 24f, cx + mw / 2f, y + 24f, 24f, withAlpha(mc, 0x22), 0);
        pFill.setColor(mc);
        c.drawCircle(cx - mw / 2f + 28f, y, 7f, pFill);
        textC(c, modeLabel, cx + 12f, y, 24f, UI_INK, tfCond, Paint.Align.CENTER);
        y += 170f;

        // Счёт «досчитывается» за первую секунду — единственная анимация экрана.
        float count = clamp((overTime - 0.15f) / 0.85f, 0f, 1f);
        count = 1f - (1f - count) * (1f - count);
        textGlow(c, fmt((int) (score * count)), cx, y, 156f, 0xFFFFFFFF,
                newRecord ? 0xCCFFD75E : 0xAA5FF3FF, tfBlack);
        y += 58f;
        int best = dailyChallenge ? progress.challengeBest : progress.bestForMode(mode);
        text(c, sBest + "  " + fmt(best), cx, y, 28f, UI_FOG, tfMed, Paint.Align.CENTER);
        y += 46f;

        String[] values = {
                fmt(runStars),
                fmt(runDodged),
                formatTime((int) elapsed),
                "x" + runMaxCombo,
                "x" + fmtMult(runMaxMult) + " · x" + fmtMult(runMaxCloseMult),
                zenMode ? "—" : "+" + fmt((int) score)
        };
        String[] labels = {sStatName[3], sStatName[0], sStatName[2],
                sCombo, sMultiplier, sNeon};
        float gap = 14f;
        float tw = (inner - gap * 2f) / 3f;
        float th = 120f;
        float gl = cx - inner / 2f;
        for (int i = 0; i < 6; i++) {
            float tl = gl + (i % 3) * (tw + gap);
            float tt = y + (i / 3) * (th + gap);
            box(c, tl, tt, tl + tw, tt + th, 24f, 0x0FFFFFFF, 0);
            boolean neon = i == 5;
            text(c, values[i], tl + tw / 2f, tt + 60f, 36f,
                    neon && !zenMode ? 0xFFFFE9A6 : UI_INK, tfBlack, Paint.Align.CENTER);
            text(c, labels[i], tl + tw / 2f, tt + 96f, 21f, UI_FOG, tfMed,
                    Paint.Align.CENTER);
        }
        y += th * 2f + gap + 34f + 66f;

        primaryButton(c, B_AGAIN, cx, y, inner, 132f, sPlayAgain, C_CYAN, 0.5f);
        y += 66f + 20f + 56f;
        secondaryButton(c, B_MENU, cx, y, inner, 112f, sToMenu, C_CYAN, UI_INK);
        c.restoreToCount(saved);
    }

    private void text(Canvas c, String s, float x, float y, float size,
                      int color, Typeface tf, Paint.Align align) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setTextAlign(align);
        pText.setLetterSpacing(0f);
        pText.clearShadowLayer();
        pText.setColor(color);
        c.drawText(s, x, y, pText);
    }

    private void textGlow(Canvas c, String s, float x, float y, float size,
                          int color, int glow, Typeface tf) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setLetterSpacing(0.02f);
        pText.setColor(color);
        pText.setShadowLayer(size * 0.30f, 0f, 0f, glow);
        c.drawText(s, x, y, pText);
        pText.clearShadowLayer();
        pText.setLetterSpacing(0f);
    }

    // ==================================================================
    //  input
    // ==================================================================

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (scale <= 0f) {
            return true;
        }
        float x = e.getX() / scale;
        float y = e.getY() / scale;
        int action = e.getActionMasked();

        if (state == ST_PLAY) {
            if (inputLock > 0f) {
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN && hitAt(x, y) == B_PAUSE) {
                pauseGame();
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                activePointerId = e.getPointerId(0);
            } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
                return true;
            } else if (action == MotionEvent.ACTION_POINTER_UP
                    && e.getPointerId(e.getActionIndex()) == activePointerId) {
                int next = e.getActionIndex() == 0 ? 1 : 0;
                activePointerId = next < e.getPointerCount() ? e.getPointerId(next)
                        : MotionEvent.INVALID_POINTER_ID;
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                int pointerIndex = e.findPointerIndex(activePointerId);
                if (pointerIndex < 0) {
                    return true;
                }
                x = e.getX(pointerIndex) / scale;
                y = e.getY(pointerIndex) / scale;
                float ny = touchTargetY(y);
                if (!tutorialDone
                        && (Math.abs(x - targetX) > 25f || Math.abs(ny - targetY) > 25f)) {
                    tutorialDone = true;
                    prefs.edit().putBoolean("tutorial_done", true).apply();
                }
                targetX = clamp(x, 0f, W);
                targetY = ny;
            }
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (inputLock > 0f) {
                    gestureIgnored = true;
                    pressedId = B_NONE;
                    return true;
                }
                gestureIgnored = false;
                touchDownX = x;
                touchDownY = y;
                touchMoved = false;
                pressedId = hitAt(x, y);
                if (pressedId == B_SLIDER) {
                    draggingOffset = true;
                    setOffsetFromX(x);
                }
                scrollDragging = isScrollScreen() && !draggingOffset
                        && y >= scrollTop && y <= scrollBottom;
                scrollVel = 0f;
                scrollLastY = y;
                scrollLastT = e.getEventTime();
                break;

            case MotionEvent.ACTION_MOVE:
                if (gestureIgnored) {
                    break;
                }
                if (draggingOffset) {
                    setOffsetFromX(x);
                    break;
                }
                if (!touchMoved && (Math.abs(y - touchDownY) > 16f
                        || Math.abs(x - touchDownX) > 16f)) {
                    touchMoved = true;
                }
                if (scrollDragging && touchMoved) {
                    long t = e.getEventTime();
                    float dy = y - scrollLastY;
                    float dtEv = Math.max(1L, t - scrollLastT) / 1000f;
                    scrollY = clamp(scrollY - dy, 0f, scrollMax);
                    scrollVel = 0.7f * (-dy / dtEv) + 0.3f * scrollVel;
                    scrollLastY = y;
                    scrollLastT = t;
                }
                break;

            case MotionEvent.ACTION_UP: {
                if (gestureIgnored) {
                    break;
                }
                if (draggingOffset) {
                    draggingOffset = false;
                    pressedId = B_NONE;
                    break;
                }
                int id = pressedId;
                pressedId = B_NONE;
                boolean scrolled = scrollDragging && touchMoved;
                scrollDragging = false;
                if (scrolled) {
                    if (e.getEventTime() - scrollLastT > 90L) {
                        scrollVel = 0f;
                    }
                    scrollVel = clamp(scrollVel, -7000f, 7000f);
                } else {
                    scrollVel = 0f;
                    if (id != B_NONE && hitAt(x, y) == id) {
                        onButton(id);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
                pressedId = B_NONE;
                scrollDragging = false;
                draggingOffset = false;
                break;

            default:
                break;
        }
        return true;
    }

    private void onButton(int id) {
        switch (id) {
            case B_PLAY:
            case B_EMPTY_PLAY:
                startGame();
                break;
            case B_DAILY:
                startDailyChallenge();
                break;
            case B_QUESTS:
                go(ST_QUESTS);
                break;
            case B_SKINS:
            case B_SKINS + 1000:
                go(ST_SKINS);
                break;
            case B_STATS:
                go(ST_STATS);
                break;
            case B_RULES:
                rulesReturn = ST_MENU;
                go(ST_RULES);
                break;
            case B_SETTINGS:
                go(ST_SETTINGS);
                break;
            case B_BACK:
                handleBack();
                break;
            case B_HISTORY:
                go(ST_HISTORY);
                break;
            case B_CONTINUE:
                resumePlay();
                break;
            case B_PAUSE_RULES:
                rulesReturn = ST_PAUSE;
                go(ST_RULES);
                break;
            case B_EXIT:
                armOrExit();
                break;
            case B_MUSIC:
                toggleMusic();
                buzz(10);
                break;
            case B_SOUND:
                toggleSound();
                buzz(10);
                break;
            case B_VIBRATION:
                toggleVibration();
                break;
            case B_AGAIN:
                if (overTime > 0.5f) {
                    startGame();
                }
                break;
            case B_MENU:
                if (overTime > 0.5f) {
                    resetMenu();
                }
                break;
            case B_TUTORIAL:
                tutorialDone = false;
                prefs.edit().putBoolean("tutorial_done", false).apply();
                toast(sTutorialReset);
                buzz(12);
                break;
            default:
                if (id >= B_MODE && id < B_MODE + Progress.MODE_COUNT) {
                    mode = id - B_MODE;
                    progress.mode = mode;
                    progress.saveMode();
                    buzz(12);
                } else if (id == B_CONTROL + CONTROL_ABOVE || id == B_CONTROL + CONTROL_AT) {
                    int cm = id - B_CONTROL;
                    if (controlMode != cm) {
                        controlMode = cm;
                        prefs.edit().putInt("control_mode", controlMode).apply();
                        buzz(12);
                    }
                } else if (id == B_TAB || id == B_TAB + 1) {
                    if (skinTab != id - B_TAB) {
                        skinTab = id - B_TAB;
                        scrollY = 0f;
                        scrollVel = 0f;
                    }
                } else if (id >= B_SKIN && id < B_SKIN + Progress.SPHERE_COUNT) {
                    selectSkin(id - B_SKIN);
                }
                break;
        }
    }

    private void selectSkin(int idx) {
        if (skinTab == 0) {
            if (idx >= Progress.SPHERE_COUNT) {
                return;
            }
            if (progress.sphereUnlocked(idx)) {
                progress.sphereSkin = idx;
                progress.saveSkins();
                buzz(14);
            } else {
                toast(sSphereCond[idx]);
                buzz(10);
            }
        } else {
            if (idx >= Progress.TRAIL_COUNT) {
                return;
            }
            if (progress.trailUnlocked(idx)) {
                progress.trailSkin = idx;
                progress.saveSkins();
                buzz(14);
            } else {
                toast(sTrailCond[idx]);
                buzz(10);
            }
        }
    }

    private void pauseGame() {
        state = ST_PAUSE;
        inputLock = 0.25f;
        exitArmed = 0f;
        pressedId = B_NONE;
    }

    /** Продолжение после паузы: короткий отсчёт, мир стоит, сферу можно двигать. */
    private void resumePlay() {
        state = ST_PLAY;
        resumeTimer = RESUME_TIME;
        exitArmed = 0f;
        inputLock = 0.12f;
    }

    /** Первый тап по «Выйти в меню» только предупреждает — забег не теряется случайно. */
    private void armOrExit() {
        if (exitArmed > 0f) {
            exitArmed = 0f;
            resetMenu();
        } else {
            exitArmed = EXIT_CONFIRM_TIME;
            buzz(14);
        }
    }

    public boolean handleBack() {
        switch (state) {
            case ST_PLAY:
                pauseGame();
                return true;
            case ST_RULES:
                go(rulesReturn);
                return true;
            case ST_HISTORY:
                go(ST_STATS);
                return true;
            case ST_SETTINGS:
            case ST_STATS:
            case ST_QUESTS:
            case ST_SKINS:
                go(ST_MENU);
                return true;
            case ST_PAUSE:
                armOrExit();
                return true;
            case ST_OVER:
                resetMenu();
                return true;
            default:
                return false;
        }
    }

    private float pauseBtnX() {
        return W - UI_PAD - 42f;
    }

    private float pauseBtnY() {
        return insetTop + 66f;
    }

    private void setOffsetFromX(float x) {
        float t = clamp((x - sliderX1) / (sliderX2 - sliderX1), 0f, 1f);
        fingerOffset = Math.round((OFFSET_MIN + t * (OFFSET_MAX - OFFSET_MIN)) / 5f) * 5f;
        fingerOffset = clamp(fingerOffset, OFFSET_MIN, OFFSET_MAX);
        prefs.edit().putFloat("finger_offset", fingerOffset).apply();
    }

    // ==================================================================
    //  helpers
    // ==================================================================

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float dist2(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private String fmt(int v) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
        nf.setGroupingUsed(true);
        return nf.format(v).replace('\u00A0', ' ');
    }

    private void toggleSound() {
        soundOn = !soundOn;
        prefs.edit().putBoolean("sound", soundOn).apply();
        if (soundOn) {
            playSound(sndCoin, 1.1f);
        }
    }

    private void toggleVibration() {
        vibOn = !vibOn;
        prefs.edit().putBoolean("vibrate", vibOn).apply();
        if (vibOn) {
            buzz(25);
        }
    }

    private void playSound(int id, float rate) {
        if (!soundOn || soundPool == null || id == 0) {
            return;
        }
        try {
            soundPool.play(id, 0.55f, 0.55f, 1, 0, clamp(rate, 0.5f, 2f));
        } catch (Exception ignored) {
        }
    }

    private void buzz(int ms) {
        if (!vibOn || vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(ms,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {
        }
    }

    // ==================================================================
    //  entities
    // ==================================================================

    private static class Obs {
        int type;
        float x;
        float y;
        float vx;
        float vy;
        float baseVy;
        float r;
        float rot;
        float vrot;
        float burstPhase;
        float bounceFlash;
        float fuse;
        float stopY;
        float splitY;
        int color;
        boolean passed;
        boolean mineArmed;
        boolean canSplit;
        boolean boss;
        int bossStage;
        int bossHits;
    }

    private static class Pickup {
        float x;
        float y;
        float vy;
        float r;
        float rot;
        int kind;
    }

    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float maxLife;
        float size;
        float grav;
        int color;
    }
}
