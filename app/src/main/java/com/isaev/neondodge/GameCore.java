package com.isaev.neondodge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Vibrator;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/** Состояние игры: константы, поля, ресурсы, звук, сущности и общие помощники. */
abstract class GameCore extends View implements Choreographer.FrameCallback {

    static final float W = 1080f;
    static final float PLAYER_R = 36f;

    static final float OFFSET_MIN = 0f;
    static final float OFFSET_MAX = 300f;
    static final float OFFSET_DEFAULT = 110f;

    static final int ST_MENU = 0;
    static final int ST_PLAY = 1;
    static final int ST_PAUSE = 2;
    static final int ST_OVER = 3;
    static final int ST_SETTINGS = 4;
    static final int ST_RULES = 5;
    static final int ST_STATS = 6;
    static final int ST_SKINS = 8;
    static final int ST_HISTORY = 9;
    static final int ST_WORKSHOP = 10;
    static final int ST_PROFILE = 11;

    static final int C_CYAN = 0xFF5FF3FF;
    static final int C_MAGENTA = 0xFFFF4FD8;
    static final int C_VIOLET = 0xFFA46BFF;
    static final int C_ORANGE = 0xFFFF9E4F;
    static final int C_LIME = 0xFFB6FF5C;
    static final int C_GOLD = 0xFFFFD75E;
    static final int C_RED = 0xFFFF4F6A;
    static final int C_MINT = 0xFF7CF9D0;
    static final int C_ICE = 0xFFC9A6FF;
    static final int C_STEEL = 0xFFC4D2E0;

    // Pickup kinds.
    static final int P_STAR = 0;
    static final int P_HEART = 1;
    static final int P_SHIELD = 2;
    static final int P_MAGNET = 3;
    static final int P_SLOW = 4;
    static final int P_BOMB = 5;
    static final int P_PHASE = 6;
    static final int P_DOUBLE = 7;

    static final float MAGNET_TIME = 8f;
    static final float SLOW_TIME = 6f;
    static final float SLOW_SCALE = 0.45f;
    static final float PHASE_TIME = 6f;
    static final float DOUBLE_TIME = 8f;
    static final int MAX_LIVES = 3;

    float magnetDuration() {
        return MAGNET_TIME * progress.bonusDurationScale();
    }

    float slowDuration() {
        return SLOW_TIME * progress.bonusDurationScale();
    }

    float phaseDuration() {
        return PHASE_TIME * progress.bonusDurationScale();
    }

    float doubleDuration() {
        return DOUBLE_TIME * progress.bonusDurationScale();
    }

    /** Zen is a training run: only its mode record is persistent progress. */
    boolean countsPersistentProgress() {
        return !zenMode;
    }

    static final int CONTROL_ABOVE = 0;
    static final int CONTROL_AT = 1;

    static final float MULT_STEP = 0.1f;
    static final float HARDCORE_MULT = 2f;
    static final float BOSS_ZONE_RADIUS = 150f;
    static final float BOSS_ZONE_FILL_TIME = 2.8f;
    /** Потолок накопленного множителя за близкие пролёты. */
    static final float CLOSE_MULT_CAP = 15f;
    /** Первый тематический late-event сразу после основного разгона (1:30). */
    static final float LATE_EVENT_START = 90f;

    // Sphere / trail skin palettes.
    static final int[] SPHERE_OUTER = {
            0xFF5FF3FF, 0xFFFF4FD8, 0xFFFFD75E, 0xFFB6FF5C, 0xFFA46BFF, 0xFFEFFBFF,
            0xFFFF7A45, 0xFF65E6C5
    };
    static final int[] SPHERE_INNER = {
            0xFFBFFAFF, 0xFFFFB8F0, 0xFFFFF0B8, 0xFFEBFFC0, 0xFFDCC7FF, 0xFFFFFFFF,
            0xFFFFD0B8, 0xFFB9FFF0
    };
    static final int[] TRAIL_COLORS = {
            0xAA5FF3FF, 0xCCFFD75E, 0xCCFF9E4F, 0x88A46BFF, 0xCCFF4FD8,
            0xCCFF6A45, 0xCC65E6C5
    };
    static final float[] TRAIL_SIZE = {1f, 0.85f, 1.15f, 1.30f, 1f, 1.2f, 0.75f};
    static final float[] TRAIL_RATE = {0.022f, 0.017f, 0.025f, 0.030f, 0.020f, 0.015f, 0.035f};

    // Asteroid types: colour tells the player which ability it has.
    static final int T_GREEN = 0;   // bounces off walls
    static final int T_BLUE = 1;    // increased fall speed
    static final int T_VIOLET = 2;  // slight horizontal homing
    static final int T_ORANGE = 3;  // moves in bursts
    static final int T_RED = 4;     // increased size
    static final int T_SPLIT = 5;   // breaks into two fragments
    static final int T_MINE = 6;    // stops, blinks, then dashes down
    static final int T_GRAVITY = 7; // pulls the player when close
    static final int T_ARMOR = 8;   // survives a shield hit

    static final int[] OBS_COLORS = {
            C_LIME, C_CYAN, C_VIOLET, C_ORANGE, C_RED,
            C_GOLD, C_MAGENTA, C_MINT, C_STEEL
    };

    /** Палитра боссов по стадиям; после пятой стадии цвет повторяется по кругу. */
    static final int[] BOSS_COLORS = {C_MAGENTA, C_ORANGE, C_GOLD, C_VIOLET, C_RED};

    // Поздние события забега: короткие тематические фазы после разгона сложности.
    static final int RE_NONE = 0;
    static final int RE_MINE_STORM = 1;   // рой мин
    static final int RE_SPLIT_FIELD = 2;  // поле осколков
    static final int RE_BLUE_RUSH = 3;    // рывок синих

    float scale = 1f;
    float viewOffsetX;
    float worldH = 1920f;
    int insetTopPx;
    int insetBottomPx;
    float insetTop;
    float insetBottom;

    final Random rnd = new Random();

    Progress progress;
    int state = ST_MENU;
    int rulesReturn = ST_MENU;
    int skinTab = 0;
    int workshopTab = 0;
    int rulesTab = 0;
    float time;
    float elapsed;
    float difficulty;
    float latePressure;
    float score;
    /** Накопленная неоновая база забега: суммы очков до метамножителей,
     *  а множитель очков применён с потолком ×3 (GameRules.neonMult). */
    float neonBase;
    boolean newRecord;
    int lives = MAX_LIVES;

    int mode = Progress.MODE_CLASSIC;
    int maxLives = MAX_LIVES;
    boolean hardcoreMode;
    boolean zenMode;
    boolean hintStarShown;
    boolean hintShieldShown;
    boolean hintComboShown;
    Random gameRnd = new Random();
    Random pickupRnd = new Random();
    int runStars;
    static final int BOSS_KIND_PRISM = 0;
    static final int BOSS_KIND_VORTEX = 1;
    static final int BOSS_KIND_STRIKER = 2;
    static final int BOSS_KIND_NEXUS = 3;

    static final int BULLET_NORMAL = 0;
    static final int BULLET_SPIRAL = 1;
    static final int BULLET_REFLECTABLE = 2;
    static final int BULLET_MINE = 3;

    int runDodged;
    int runMaxCombo;
    /** Минута последнего минибосса; боссы приходят каждые 2 минуты и без конца. */
    int lastBossMinute;
    boolean bossActive;
    int bossKind;
    int bossHp = 3;
    int bossMaxHp = 3;
    float bossEntranceTimer;
    float bossStunTimer;
    float bossWobble;

    // Лазер
    float bossLaserAimTimer;
    float bossLaserFireTimer;
    float bossLaserX;
    float bossLaserY;
    float bossLaserTargetX;
    float bossLaserWidth = 26f;

    float bossStopY() {
        return Math.max(480f, insetTop + 440f);
    }

    // Таран
    float bossDashWarnTimer;
    float bossDashTimer;
    float bossDashX;
    float bossDashStartY;
    float bossDashTargetY;

    // Спирали / мины
    float bossSpiralTimer;
    float bossSpiralAngle;

    // Резонаторы перегрузки (Conduits)
    final ArrayList<BossConduit> bossConduits = new ArrayList<>();
    float conduitArcTimer;
    float conduitArcX;
    float conduitArcY;

    // Эссенция и финал боя
    final ArrayList<EssenceOrb> essenceOrbs = new ArrayList<>();
    float bossDeathTimer;
    int bossDeathPhase;
    float bossEssenceX;
    float bossEssenceY;
    float playerAbsorptionPulse;
    float bossSlowMoTimer;

    // Бегущий счет
    float scoreRollupBonus;
    float scoreRollupCurrent;
    float scoreRollupTimer;
    String bossAnnounceName = "";

    int bossZonesCompleted;
    float bossZoneTimer;
    BossZone bossZone;
    final ArrayList<BossBullet> bossBullets = new ArrayList<>();
    boolean scoreBanked;

    float scoreMult = 1f;
    float closeMult = 1f;
    float runMaxOverallMult = 1f;
    float bossMult = 1f;
    float levelMult = 1f;

    boolean soundOn = true;
    boolean vibOn = true;
    boolean musicOn = true;
    float soundVolume = 1f;
    float musicVolume = 1f;
    int targetHz = 60;
    String languageTag = "ru";
    boolean tutorialDone;
    float fingerOffset = OFFSET_DEFAULT;
    boolean draggingOffset;
    int draggingAudioSlider = B_NONE;
    int controlMode = CONTROL_ABOVE;
    int combo;
    float comboTimer;
    float comboPop;
    int lastLevel;
    float announceTimer;

    int shieldCharges;
    float magnetTimer;
    float slowTimer;
    float phaseTimer;
    float doubleTimer;
    float bombWaveTimer;
    /** Стоп-кадр при уроне: мир замирает на пару кадров. */
    float hitStop;
    /** Короткий slow-mo при победе над боссом. */
    float bossSlowTimer;
    static final float BOMB_WAVE_DURATION = 0.65f;
    float bombWaveOriginX;
    float bombWaveOriginY;
    static final float MINE_TRIGGER_R = 250f;
    static final float MINE_BLAST_R = 220f;
    static final float MINE_FUSE_TIME = 0.65f;
    int profileReturn;
    float spawnPauseTimer;
    float specialTimer;
    float checkTimer;
    float saveTimer;
    // Поздние события: kind активного события, left — остаток, timer — до следующего.
    int runEventKind;
    float runEventLeft;
    float runEventTimer;
    String toastText = "";
    float toastTimer;
    float px = W / 2f;
    float py = 1400f;
    float targetX = px;
    float targetY = py;
    int activePointerId = MotionEvent.INVALID_POINTER_ID;
    float invuln;
    float shake;
    float flash;
    float inputLock;
    float overTime;

    float spawnTimer;
    float coinTimer;
    float heartTimer;
    float trailTimer;
    float ambientTimer;
    int shownScore = -1;
    String shownScoreText = "0";

    final ArrayList<Obs> obstacles = new ArrayList<>();
    final ArrayList<Pickup> pickups = new ArrayList<>();
    final ArrayList<Particle> particles = new ArrayList<>();
    final ArrayList<ScorePop> scorePops = new ArrayList<>();

    final float[] starX = new float[120];
    final float[] starY = new float[120];
    final float[] starSpd = new float[120];
    final float[] starSize = new float[120];
    final float[] starPh = new float[120];
    Shader bgShader;
    Shader nebulaOne;
    Shader nebulaTwo;

    final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint pGlow = new Paint(Paint.FILTER_BITMAP_FLAG);
    final Paint pBg = new Paint();
    final Paint pNebula = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint pFlash = new Paint();
    final Paint pScrim = new Paint();
    Shader hudScrim;
    float hudScrimH = 240f;

    Bitmap glowBmp;
    final Rect srcGlow = new Rect(0, 0, 128, 128);
    final RectF tmpRect = new RectF();
    final HashMap<Integer, PorterDuffColorFilter> filters = new HashMap<>();
    final HashMap<String, Path> neonPaths = new HashMap<>();
    final Path hexPath = new Path();
    final Path obsPath = new Path();
    final Path obsInnerPath = new Path();
    final Obs previewObs = new Obs();
    final Paint pShade = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path podPath = new Path();
    final android.graphics.Matrix shaderMatrix = new android.graphics.Matrix();
    Shader planetShader;
    float planetShaderTop = -1f;
    Shader playShader;
    float playShaderKey = -1f;

    // ---------- итоги забега ----------

    /** Событие забега для ленты наград на экране итога. */
    static final class RunEvent {
        static final int ACHIEVEMENT = 1;
        static final int SKIN = 2;
        static final int RANK = 3;

        final int kind;
        final String text;
        final String value;

        RunEvent(int kind, String text, String value) {
            this.kind = kind;
            this.text = text;
            this.value = value;
        }
    }

    final ArrayList<RunEvent> runEvents = new ArrayList<>();
    long runNeonBefore;
    long runNeonGained;
    int runRankBefore = 1;
    int runPrevBest;
    int runSkinMask;
    final Paint pNeon = new Paint(Paint.ANTI_ALIAS_FLAG);

    final Path starPath = new Path();
    final Path heartPath = new Path();
    final Path shieldPath = new Path();
    final Path iconPath = new Path();

    Typeface tfBlack;
    Typeface tfBold;
    Typeface tfMed;
    Typeface tfCond;
    /** Unbounded: вывеска, заголовки, крупные числа и главные кнопки. */
    Typeface tfDisplay;
    Typeface tfDisplayBold;

    String sScore;
    String sBest;
    String sGameOver;
    String sNewRecord;
    String sPlayAgain;
    String sPause;
    String sTapContinue;
    String sTapStart;
    String sTagline;
    String sHintMove;
    String sToMenu;
    String sSpeedUp;
    String sCombo;
    String sSound;
    String sVibration;
    String sMusic;
    String sLanguage;
    String sRussian;
    String sEnglish;
    String sRefreshRate;
    String sHz60;
    String sHz120;
    String sMusicVolume;
    String sSoundVolume;
    String sOn;
    String sOff;
    String sTutorial;
    String sSettings;
    String sOffset;
    String sOffsetHint;
    String sDone;
    String sVersion;
    String versionName;
    String sRules;
    String sRulesButton;
    String sLegendAsteroids;
    String sLegendBonuses;
    String sRulesHowTo;
    String sRulesMultiplier;
    String sRulesMultiplierDesc;
    String sRulesProgress;
    String sRulesScrollHint;
    String sMultiplier;
    String sControlMode;
    String sControlAbove;
    String sControlAt;
    String sControlAtHint;
    String sStats;
    String sAchievements;
    String sHistory;
    String sHistoryEmpty;
    String sSkins;
    String sBack;
    String sNeon;
    String sSelected;
    String sSelect;
    String sAchievementUnlocked;
    String sShieldActive;
    String sShieldBlocked;
    String sArmorCracked;
    String sHintStar;
    String sHintShield;
    String sHintCombo;
    String sMagnetActive;
    String sSlowActive;
    String sBombActive;
    String sPhaseActive;
    String sDoubleActive;
    String sBossMultiplier;
    String sBossZone;
    String sBossReward;
    String sMiniBossToast;
    String sLateMineStorm;
    String sLateSplitField;
    String sBossLabel;
    String sBossZoneScore;
    String sBossRewardScore;
    String sBossNamePrism;
    String sBossNameVortex;
    String sBossNameStriker;
    String sBossNameNexus;
    String sBossWarningTitle;
    String sBossConduitLinked;
    String sBossEssenceHarvested;
    String sBossReflect;

    String getBossName(int kind) {
        switch (kind) {
            case BOSS_KIND_PRISM: return sBossNamePrism;
            case BOSS_KIND_VORTEX: return sBossNameVortex;
            case BOSS_KIND_STRIKER: return sBossNameStriker;
            case BOSS_KIND_NEXUS: return sBossNameNexus;
            default: return sBossLabel;
        }
    }
    String sWorkshop;
    String sWorkshopLevel;
    String sWorkshopNext;
    String sWorkshopMax;
    String sWorkshopBuy;
    String sWorkshopHint;
    String sWorkshopStat;
    String sWorkshopStep;
    String sWorkshopLevelOf;
    String sWorkshopReady;
    String sWorkshopNeed;
    String sWorkshopLocked;
    String sWorkshopCost;
    String sWorkshopStars;
    String sWorkshopStarStep;
    String sWorkshopClose;
    String sWorkshopBoss;
    String sWorkshopLevelMult;
    String sWorkshopCloseStep;
    String sWorkshopBossValue;
    String sWorkshopLevelStep;
    String sWorkshopProgress;
    String sWorkshopTabMultipliers;
    String sWorkshopTabVessel;
    String sWorkshopTabBonuses;
    String sWorkshopBonusDuration;
    String sWorkshopBonusDurationStep;
    String sWorkshopTempMultiplier;
    String sWorkshopTempMultiplierStep;
    String sWorkshopStartMult;
    String sWorkshopStartMultStep;
    String sWorkshopComboTime;
    String sWorkshopComboTimeStep;
    String sWorkshopShieldCap;
    String sWorkshopShieldCapStep;
    String sWorkshopMagnetRadius;
    String sWorkshopMagnetRadiusStep;
    String sWorkshopEmergencyPhase;
    String sWorkshopEmergencyPhaseStep;
    String sWorkshopSecondWind;
    String sWorkshopSecondWindStep;
    String sWorkshopConduitBoost;
    String sWorkshopConduitBoostStep;
    String sWorkshopEssenceBoost;
    String sWorkshopEssenceBoostStep;
    boolean emergencyPhaseUsed;
    String sSphere;
    String sTrail;
    String sZen;
    String sRecords;
    String sTotals;
    String sHistoryCta;
    String sExitConfirm;
    String sReplayTutorial;
    String sTutorialReset;
    String sSoundSection;
    final String[] sModeNames = new String[Progress.MODE_COUNT];
    final String[] sModeHints = new String[Progress.MODE_COUNT];
    final String[] sRuleLine = new String[7];
    final String[] sRuleProgress = new String[3];
    final String[] sAstName = new String[9];
    final String[] sAstDesc = new String[9];
    final String[] sBonusName = new String[8];
    final String[] sRankTitle = new String[GameRules.TITLES_COUNT];
    String sProfileTitle;
    String sProfileMaxRank;
    String sProfileNextRank;
    String sProfileTotalNeon;
    String sProfileLadder;
    String sProfileCurrent;
    String sProfileLvlRange;
    String sProfileLvlSingle;
    String sRulesTabBasics;
    String sRulesTabAsteroids;
    String sRulesTabBonuses;
    String sRankLabel;
    String sRankToNext;
    String sRankUp;
    String sRankReached;
    String sPlayRecord;
    String sPlayNoRecord;
    String sModeNoRecord;
    String sModeNoLose;
    String sRecordWas;
    String sRecordFirst;
    String sNewRecordMode;
    String sSkinUnlocked;
    String sSecondsShort;
    String sSecUnit;
    String sStatMaxMult;
    final String[] sBonusDesc = new String[8];
    final String[] sStatName = new String[6];
    final String[] sAchName = new String[Progress.ACH_COUNT];
    final String[] sSphereName = new String[Progress.SPHERE_COUNT];
    final String[] sTrailName = new String[Progress.TRAIL_COUNT];
    final String[] sSphereCond = new String[Progress.SPHERE_COUNT];
    final String[] sTrailCond = new String[Progress.TRAIL_COUNT];

    SharedPreferences prefs;
    volatile SoundPool soundPool;
    volatile boolean audioInitStarted;
    MediaPlayer musicPlayer;
    AudioManager audioManager;
    AudioFocusRequest audioFocusRequest;
    boolean audioFocusHeld;
    int musicTrack;
    int sndCoin;
    int sndHit;
    int sndPower;
    Vibrator vibrator;

    boolean running;
    long lastNs;

    GameCore(Context context) {
        super(context);
        setFocusable(true);
        setKeepScreenOn(false);

        prefs = context.getSharedPreferences("neon_dodge", Context.MODE_PRIVATE);
        progress = new Progress(context);
        mode = progress.mode;
        soundOn = prefs.getBoolean("sound", true);
        vibOn = prefs.getBoolean("vibrate", true);
        musicOn = prefs.getBoolean("music", true);
        soundVolume = clamp(prefs.getFloat("sound_volume", 1f), 0f, 1f);
        musicVolume = clamp(prefs.getFloat("music_volume", 1f), 0f, 1f);
        targetHz = prefs.getInt("refresh_hz", 60) == 120 ? 120 : 60;
        languageTag = prefs.getString("language", "ru");
        tutorialDone = prefs.getBoolean("tutorial_done", false);
        hintStarShown = prefs.getBoolean("hint_star", false);
        hintShieldShown = prefs.getBoolean("hint_shield", false);
        hintComboShown = prefs.getBoolean("hint_combo", false);
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
        resetMenu();
    }

    // ---------- UI-токены ----------
    //
    // Образ — неоновая вывеска в ночном небе. Смелость потрачена в одном
    // месте: логотип нарисован трубками. Остальное — тихое тёмное стекло.
    //
    // Цвет несёт смысл: циан — действие, золото — неон и награды, магента —
    // опасность и близкие пролёты, мята — выполнено, цвет режима — сам режим. Форма тоже:
    // главная кнопка — сплошная капсула, вторичные — стекло с контуром,
    // карточки — скругление UI_RADIUS, иконки — контур толщиной ICON_STROKE.

    static final int UI_INK = 0xFFEAF6FF;
    static final int UI_FOG = 0xFF93A9C2;
    static final int UI_DIM = 0xFF5A6D86;
    static final int UI_SURFACE = 0xD90F1532;
    static final int UI_SURFACE_HI = 0xF01A2350;
    static final int UI_LINE = 0x1FFFFFFF;
    static final int UI_ON_ACCENT = 0xFF061018;
    static final float UI_PAD = 48f;
    static final float ORBIT_TILT = -8f;
    static final float PLANET_R = 1100f;
    static final float UI_RADIUS = 28f;
    static final float ICON_STROKE = 0.15f;

    static final int B_NONE = -1;
    static final int B_PLAY = 1;
    static final int B_SKINS = 4;
    static final int B_STATS = 5;
    static final int B_RULES = 6;
    static final int B_SETTINGS = 7;
    static final int B_BACK = 8;
    static final int B_HISTORY = 9;
    static final int B_PAUSE = 10;
    static final int B_CONTINUE = 11;
    static final int B_PAUSE_RULES = 12;
    static final int B_EXIT = 13;
    static final int B_MUSIC = 14;
    static final int B_SOUND = 15;
    static final int B_VIBRATION = 16;
    static final int B_AGAIN = 17;
    static final int B_MENU = 18;
    static final int B_SLIDER = 19;
    static final int B_TUTORIAL = 20;
    static final int B_EMPTY_PLAY = 21;
    static final int B_WORKSHOP = 22;
    static final int B_WORKSHOP_UPGRADE = 23;
    static final int B_WORKSHOP_UPGRADE_CLOSE = 24;
    static final int B_WORKSHOP_UPGRADE_BOSS = 25;
    static final int B_WORKSHOP_UPGRADE_LEVEL = 26;
    static final int B_WORKSHOP_UPGRADE_DURATION = 27;
    static final int B_WORKSHOP_UPGRADE_TEMP = 28;
    static final int B_WORKSHOP_UPGRADE_COMBO = 120;
    static final int B_WORKSHOP_UPGRADE_SHIELD = 121;
    static final int B_WORKSHOP_UPGRADE_MAGNET = 122;
    static final int B_WORKSHOP_UPGRADE_PHASE = 123;
    static final int B_WORKSHOP_UPGRADE_HEART = 124;
    static final int B_WORKSHOP_UPGRADE_CONDUIT = 125;
    static final int B_PROFILE = 29;
    static final int B_WORKSHOP_TAB_MULT = 80;
    static final int B_WORKSHOP_TAB_VESSEL = 81;
    static final int B_WORKSHOP_TAB_BONUS = 85;
    static final int B_RULES_TAB_BASICS = 82;
    static final int B_RULES_TAB_ASTEROIDS = 83;
    static final int B_RULES_TAB_BONUSES = 84;
    static final int B_LANGUAGE_RU = 70;
    static final int B_LANGUAGE_EN = 71;
    static final int B_HZ_60 = 72;
    static final int B_HZ_120 = 73;
    static final int B_MUSIC_VOLUME = 74;
    static final int B_SOUND_VOLUME = 75;
    static final int B_MODE = 30;     // + индекс режима
    static final int B_CONTROL = 40;  // + тип управления
    static final int B_TAB = 50;      // + вкладка скинов
    static final int B_SKIN = 60;     // + индекс скина

    static final float RESUME_TIME = 1.5f;
    static final float EXIT_CONFIRM_TIME = 3f;

    // Зоны нажатия собираются во время отрисовки кадра, поэтому разметка
    // и обработка касаний не расходятся.
    static final int MAX_HITS = 48;
    final RectF[] hitRect = new RectF[MAX_HITS];
    final int[] hitId = new int[MAX_HITS];
    int hitCount;
    int hitState = -1;
    int pressedId = B_NONE;
    boolean gestureIgnored;
    boolean touchMoved;
    float touchDownX;
    float touchDownY;

    float scrollY;
    float scrollMax;
    float scrollVel;
    boolean scrollDragging;
    float scrollLastY;
    long scrollLastT;
    float scrollTop;
    float scrollBottom = Float.MAX_VALUE;
    boolean clipping;

    float sliderX1 = 170f;
    float sliderX2 = 910f;
    float offsetSliderX1 = 170f;
    float offsetSliderX2 = 910f;
    float volumeSliderX1 = 170f;
    float volumeSliderX2 = 910f;
    float exitArmed;
    float resumeTimer;

    float mTopBarY;
    float mLogoY;
    float mLogoSize;
    float mHeroY = 1300f;
    float mSegCy;
    float mInfoY;
    float mPlayCy;
    float mNavCy;
    float mFootY;
    float mPodScale = 1f;
    float mPlanetTop = 1400f;
    float mOrbitRx = 360f;
    float mOrbitRy = 200f;

    int hudBestValue = -1;
    String hudBestText = "";
    int hudMultTenths = -1;
    String hudMultText = "x1.0";
    int hudCloseTenths = -1;
    String hudCloseText = "x1.0";

    {
        for (int i = 0; i < MAX_HITS; i++) {
            hitRect[i] = new RectF();
        }
    }

    static int mix(int a, int b, float t) {
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

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (clamp(alpha, 0, 255) << 24);
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static float dist2(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    String fmt(int v) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
        nf.setGroupingUsed(true);
        return nf.format(v).replace('\u00A0', ' ');
    }

    String fmt(long v) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
        nf.setGroupingUsed(true);
        return nf.format(v).replace('\u00A0', ' ');
    }

    void strokeStyle(int color, float width) {
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeCap(Paint.Cap.ROUND);
        pStroke.setStrokeJoin(Paint.Join.ROUND);
        pStroke.setColor(color);
        pStroke.setStrokeWidth(width);
    }

    void strokeReset() {
        pStroke.setStrokeCap(Paint.Cap.BUTT);
        pStroke.setStrokeJoin(Paint.Join.MITER);
        pStroke.setStyle(Paint.Style.FILL);
    }

    static class Obs {
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
        float splitY;
        int color;
        boolean passed;
        boolean mineArmed;
        boolean canSplit;
        boolean boss;
        int bossStage;
        float bossShotTimer;
        int armorHits;
        float warnTimer;
    }

    static class BossZone {
        float x;
        float y;
        float fill;
    }

    static class BossConduit {
        float x;
        float y;
        float r = 42f;
        boolean active;
        float pulse;
        float appearAnim;
        int index;
    }

    static class BossBullet {
        float x;
        float y;
        float vx;
        float vy;
        float r;
        int color;
        int kind;
        float timer;
        boolean reflected;
    }

    static class EssenceOrb {
        float x;
        float y;
        float vx;
        float vy;
        float r;
        int color;
        float life;
        float maxLife;
        float delay;
        boolean absorbed;
    }

    static class Pickup {
        float x;
        float y;
        float vy;
        float r;
        float rot;
        int kind;
    }

    static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float maxLife;
        float size;
        float grav;
        int color;
        int style;
    }

    static class ScorePop {
        float x;
        float y;
        float life;
        float maxLife;
        String text;
        int color;
    }

    // ---------- реализовано в верхних слоях ----------

    /** См. {@link GameResources}. */
    abstract void loadStrings(Context context);

    /** См. {@link GameResources}. */
    abstract void initPaints();

    /** См. {@link GameResources}. */
    abstract void initStars();

    /** См. {@link GameSpawner}. */
    abstract void resetMenu();

    /** См. {@link GameLoop}. */
    abstract float groundY();

    /** См. {@link GameLoop}. */
    abstract float scoreFactor();

    /** См. {@link UiKit}. */
    abstract void updateUi(float dt);

    /** См. {@link UiKit}. */
    abstract void textC(Canvas c, String s, float x, float cy, float size, int color, Typeface tf, Paint.Align align);


    /** См. {@link UiKit}. */
    abstract String formatTime(int totalSec);

    /** См. {@link UiKit}. */
    abstract void text(Canvas c, String s, float x, float y, float size, int color, Typeface tf, Paint.Align align);

    /** См. {@link UiKit}. */
    abstract void box(Canvas c, float l, float t, float r, float b, float rad, int fill, int stroke);

    /** См. {@link UiKit}. */
    abstract void textGlow(Canvas c, String s, float x, float y, float size, int color, int glow, Typeface tf);

    /** См. {@link HudLayer}. */
    abstract void drawHud(Canvas c);

    /** См. {@link HudLayer}. */
    abstract void drawResumeCountdown(Canvas c);

    /** См. {@link HudLayer}. */
    abstract void drawTutorial(Canvas c);

    /** См. {@link HudLayer}. */
    abstract void drawAnnounce(Canvas c);

    /** См. {@link MenuScreen}. */
    abstract void layoutMenu();

    /** См. {@link MenuScreen}. */
    abstract void drawMenuDecor(Canvas c);

    /** См. {@link MenuScreen}. */
    abstract void drawMenu(Canvas c);

    /** См. {@link MenuScreen}. */
    abstract void drawWorkshop(Canvas c);

    /** См. {@link InfoScreens}. */
    abstract void drawProfile(Canvas c);

    /** См. {@link InfoScreens}. */
    abstract void drawStats(Canvas c);

    /** См. {@link InfoScreens}. */
    abstract void drawHistory(Canvas c);

    /** См. {@link InfoScreens}. */
    abstract int historyColor(int type);

    /** См. {@link InfoScreens}. */
    abstract void drawSkins(Canvas c);

    /** См. {@link InfoScreens}. */
    abstract void drawRules(Canvas c);

    /** См. {@link InfoScreens}. */
    abstract void drawToast(Canvas c);

    /** См. {@link OverlayScreens}. */
    abstract void drawPause(Canvas c);

    /** См. {@link OverlayScreens}. */
    abstract void drawSettings(Canvas c);

    /** См. {@link OverlayScreens}. */
    abstract void drawOver(Canvas c);

    /** См. {@link GameView}. */
    abstract void pauseGame();

    /** См. {@link GameView}. */
    abstract float pauseBtnX();

    /** См. {@link GameView}. */
    abstract float pauseBtnY();
}
