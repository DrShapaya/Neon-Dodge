package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Загрузка строк, кистей и звёздного фона. */
abstract class GameResources extends GameCore {

    GameResources(Context context) {
        super(context);
    }

    void loadStrings(Context context) {
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
        sLanguage = context.getString(R.string.language);
        sRussian = context.getString(R.string.russian);
        sEnglish = context.getString(R.string.english);
        sRefreshRate = context.getString(R.string.refresh_rate);
        sHz60 = context.getString(R.string.hz_60);
        sHz120 = context.getString(R.string.hz_120);
        sMusicVolume = context.getString(R.string.music_volume);
        sSoundVolume = context.getString(R.string.sound_volume);
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
        sSkins = context.getString(R.string.skins_title);
        sBack = context.getString(R.string.back);
        sNeon = context.getString(R.string.neon_points);
        sSelected = context.getString(R.string.skin_selected);
        sSelect = context.getString(R.string.skin_select);
        sAchievementUnlocked = context.getString(R.string.achievement_unlocked);
        sShieldActive = context.getString(R.string.bonus_shield_active);
        sShieldBlocked = context.getString(R.string.bonus_shield_blocked);
        sArmorCracked = context.getString(R.string.armor_cracked);
        sHintStar = context.getString(R.string.hint_star);
        sHintShield = context.getString(R.string.hint_shield);
        sHintCombo = context.getString(R.string.hint_combo);
        sMagnetActive = context.getString(R.string.bonus_magnet_active);
        sSlowActive = context.getString(R.string.bonus_slow_active);
        sBombActive = context.getString(R.string.bonus_bomb_active);
        sPhaseActive = context.getString(R.string.bonus_phase_active);
        sDoubleActive = context.getString(R.string.bonus_double_active);
        sBossMultiplier = context.getString(R.string.boss_multiplier);
        sBossZone = context.getString(R.string.boss_zone);
        sBossReward = context.getString(R.string.boss_reward);
        sMiniBossToast = context.getString(R.string.mini_boss_toast);
        sLateMineStorm = context.getString(R.string.late_mine_storm);
        sLateSplitField = context.getString(R.string.late_split_field);
        sBossLabel = context.getString(R.string.boss_label);
        sBossZoneScore = context.getString(R.string.boss_zone_score);
        sBossRewardScore = context.getString(R.string.boss_reward_score);
        sWorkshop = context.getString(R.string.workshop_title);
        sWorkshopLevel = context.getString(R.string.workshop_level);
        sWorkshopNext = context.getString(R.string.workshop_next);
        sWorkshopMax = context.getString(R.string.workshop_max);
        sWorkshopBuy = context.getString(R.string.workshop_buy);
        sWorkshopHint = context.getString(R.string.workshop_hint);
        sRankLabel = context.getString(R.string.rank_label);
        sRankTitle[0] = context.getString(R.string.rank_title_1);
        sRankTitle[1] = context.getString(R.string.rank_title_2);
        sRankTitle[2] = context.getString(R.string.rank_title_3);
        sRankTitle[3] = context.getString(R.string.rank_title_4);
        sRankTitle[4] = context.getString(R.string.rank_title_5);
        sRankTitle[5] = context.getString(R.string.rank_title_6);
        sRankTitle[6] = context.getString(R.string.rank_title_7);
        sRankTitle[7] = context.getString(R.string.rank_title_8);
        sRankTitle[8] = context.getString(R.string.rank_title_9);
        sRankTitle[9] = context.getString(R.string.rank_title_10);
        sRankTitle[10] = context.getString(R.string.rank_title_11);
        sProfileTitle = context.getString(R.string.profile_title);
        sProfileMaxRank = context.getString(R.string.profile_max_rank);
        sProfileNextRank = context.getString(R.string.profile_next_rank);
        sProfileTotalNeon = context.getString(R.string.profile_total_neon);
        sProfileLadder = context.getString(R.string.profile_ladder);
        sProfileCurrent = context.getString(R.string.profile_current);
        sProfileLvlRange = context.getString(R.string.profile_lvl_range);
        sProfileLvlSingle = context.getString(R.string.profile_lvl_single);
        sRulesTabBasics = context.getString(R.string.rules_tab_basics);
        sRulesTabAsteroids = context.getString(R.string.rules_tab_asteroids);
        sRulesTabBonuses = context.getString(R.string.rules_tab_bonuses);
        sRankToNext = context.getString(R.string.rank_to_next);
        sRankUp = context.getString(R.string.rank_up);
        sRankReached = context.getString(R.string.rank_reached);
        sPlayRecord = context.getString(R.string.play_record);
        sPlayNoRecord = context.getString(R.string.play_no_record);
        sModeNoRecord = context.getString(R.string.mode_no_record);
        sModeNoLose = context.getString(R.string.mode_no_lose);
        sRecordWas = context.getString(R.string.record_was);
        sRecordFirst = context.getString(R.string.record_first);
        sNewRecordMode = context.getString(R.string.new_record_mode);
        sSkinUnlocked = context.getString(R.string.skin_unlocked);
        sSecondsShort = context.getString(R.string.seconds_short);
        sStatMaxMult = context.getString(R.string.stat_max_mult);
        sWorkshopStat = context.getString(R.string.workshop_stat);
        sWorkshopStep = context.getString(R.string.workshop_star_step);
        sWorkshopLevelOf = context.getString(R.string.workshop_level_of);
        sWorkshopReady = context.getString(R.string.workshop_ready);
        sWorkshopNeed = context.getString(R.string.workshop_need);
        sWorkshopLocked = context.getString(R.string.workshop_locked);
        sWorkshopCost = context.getString(R.string.workshop_cost);
        sWorkshopStars = context.getString(R.string.workshop_stars);
        sWorkshopClose = context.getString(R.string.workshop_close);
        sWorkshopBoss = context.getString(R.string.workshop_boss);
        sWorkshopBossValue = context.getString(R.string.workshop_boss_value);
        sWorkshopLevelMult = context.getString(R.string.workshop_level_mult);
        sWorkshopCloseStep = context.getString(R.string.workshop_close_step);
        sWorkshopLevelStep = context.getString(R.string.workshop_level_step);
        sWorkshopProgress = context.getString(R.string.workshop_progress);
        sWorkshopTabMultipliers = context.getString(R.string.workshop_tab_multipliers);
        sWorkshopTabBonuses = context.getString(R.string.workshop_tab_bonuses);
        sWorkshopBonusDuration = context.getString(R.string.workshop_bonus_duration);
        sWorkshopBonusDurationStep = context.getString(R.string.workshop_bonus_duration_step);
        sWorkshopTempMultiplier = context.getString(R.string.workshop_temp_multiplier);
        sWorkshopTempMultiplierStep = context.getString(R.string.workshop_temp_multiplier_step);
        sSphere = context.getString(R.string.skins_sphere);
        sTrail = context.getString(R.string.skins_trail);
        sZen = context.getString(R.string.zen_hud);
        sRecords = context.getString(R.string.records);
        sTotals = context.getString(R.string.totals);
        sHistoryCta = context.getString(R.string.history_cta);
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
        sAchName[Progress.ACH_SCORE_10000] = context.getString(R.string.ach_score_10000);
        sAchName[Progress.ACH_COMBO_50] = context.getString(R.string.ach_combo_50);
        sAchName[Progress.ACH_BOSS_DEFEATED] = context.getString(R.string.ach_boss_defeated);

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
        sTrailCond[4] = context.getString(R.string.unlock_stars, fmt(150));
        sTrailCond[5] = context.getString(R.string.unlock_stars, fmt(350));
        sTrailCond[6] = context.getString(R.string.unlock_time, formatTime(1800));
    }

    void initPaints() {
        pFill.setStyle(Paint.Style.FILL);
        pStroke.setStyle(Paint.Style.FILL);
        pFlash.setStyle(Paint.Style.FILL);

        tfBlack = Typeface.create("sans-serif-black", Typeface.BOLD);
        tfBold = Typeface.create("sans-serif", Typeface.BOLD);
        tfMed = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        tfCond = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        try {
            tfDisplay = getResources().getFont(R.font.unbounded_black);
            tfDisplayBold = getResources().getFont(R.font.unbounded_bold);
        } catch (RuntimeException e) {
            // Шрифт не загрузился — интерфейс остаётся рабочим на системном.
            tfDisplay = tfBlack;
            tfDisplayBold = tfBold;
        }
        pText.setFontFeatureSettings("tnum");

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

    void initStars() {
        for (int i = 0; i < starX.length; i++) {
            starX[i] = rnd.nextFloat() * W;
            starY[i] = rnd.nextFloat() * worldH;
            starSpd[i] = 8f + rnd.nextFloat() * 46f;
            starSize[i] = 1.2f + rnd.nextFloat() * 2.6f;
            starPh[i] = rnd.nextFloat() * 6.283f;
        }
    }
}
