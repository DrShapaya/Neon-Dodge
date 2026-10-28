package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.Locale;
import java.util.Random;

/** Появление астероидов, бонусов и боссов; игровые события. */
abstract class GameSpawner extends GameAudio {

    GameSpawner(Context context) {
        super(context);
    }

    void spawnObstacle() {
        spawnObstacle(-1);
    }

    void spawnObstacle(int forcedType) {
        Obs o = new Obs();
        o.type = forcedType >= T_GREEN && forcedType <= T_ARMOR
                ? forcedType : pickObstacleType();
        for (int retry = 0; retry < 4 && o.type == T_GRAVITY && countType(T_GRAVITY) >= 2; retry++) {
            o.type = pickObstacleType();
        }
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
            o.armorHits = 2;
        }

        float minX = o.r + 30f;
        float maxX = W - o.r - 30f;
        o.x = minX + gameRnd.nextFloat() * Math.max(1f, maxX - minX);
        // Never drop a new obstacle directly on the player's current lane.
        for (int attempt = 0; attempt < 8 && !GameRules.spawnLaneIsSafe(o.x, px, 150f); attempt++) {
            o.x = minX + gameRnd.nextFloat() * Math.max(1f, maxX - minX);
        }
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
            o.mineArmed = false;
            o.fuse = 0f;
            if (!GameRules.spawnLaneIsSafe(o.x, px, 180f)) {
                o.x = clamp(o.x + (o.x < W / 2f ? 220f : -220f), minX, maxX);
            }
        }

        if (o.type == T_GRAVITY || o.type == T_MINE || o.type == T_SPLIT
                || o.type == T_ARMOR) {
            o.warnTimer = 0.85f;
        }

        o.burstPhase = gameRnd.nextFloat() * 1.05f;
        o.rot = gameRnd.nextFloat() * 360f;
        o.vrot = (gameRnd.nextFloat() - 0.5f) * 190f;
        obstacles.add(o);
    }

    void spawnBossConduits(int phase) {
        bossConduits.clear();
        int count = (phase <= 1) ? 3 : 2;
        float minY = Math.max(630f, insetTop + 600f);
        float maxY = Math.min(worldH - 260f, groundY() - 70f);
        float laneHeight = (maxY - minY) / count;

        for (int i = 0; i < count; i++) {
            BossConduit c = new BossConduit();
            c.index = i;
            c.active = false;
            c.pulse = 0f;
            c.appearAnim = 0f;
            c.r = 44f;
            float laneY = minY + i * laneHeight + laneHeight * 0.5f;
            c.y = laneY + (gameRnd.nextFloat() - 0.5f) * 60f;
            if (count == 2) {
                if (i == 0) {
                    c.x = 220f + gameRnd.nextFloat() * 160f;
                } else {
                    c.x = W - 220f - gameRnd.nextFloat() * 160f;
                }
            } else {
                if (i == 0) {
                    c.x = 200f + gameRnd.nextFloat() * 120f;
                } else if (i == 1) {
                    c.x = W / 2f + (gameRnd.nextFloat() - 0.5f) * 120f;
                } else {
                    c.x = W - 200f - gameRnd.nextFloat() * 120f;
                }
            }
            bossConduits.add(c);
            explode(c.x, c.y, 14, C_CYAN);
        }
    }

    void cleanBossState() {
        bossActive = false;
        bossHp = 3;
        bossStunTimer = 0f;
        bossLaserAimTimer = 0f;
        bossLaserFireTimer = 0f;
        bossDashWarnTimer = 0f;
        bossDashTimer = 0f;
        bossSlowMoTimer = 0f;
        bossDeathPhase = 0;
        bossDeathTimer = 0f;
        conduitArcTimer = 0f;
        bossBullets.clear();
        bossConduits.clear();
        essenceOrbs.clear();
        bossZone = null;
        bossZonesCompleted = 0;
        bossZoneTimer = 0f;
    }

    void spawnBoss(int minute) {
        // Очистить все падающие обычные камни
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obs obs = obstacles.get(i);
            if (!obs.boss) {
                explode(obs.x, obs.y, 10, obs.color);
                obstacles.remove(i);
            }
        }
        bombWaveOriginX = W / 2f;
        bombWaveOriginY = worldH / 2f;
        bombWaveTimer = BOMB_WAVE_DURATION;
        flash = 0.45f;
        shake = 25f;
        playSound(sndPower, 0.8f);

        int stage = minute / 2;
        bossKind = (stage - 1) % 4;
        bossActive = true;
        bossHp = 3;
        bossMaxHp = 3;
        bossStunTimer = 0f;
        bossWobble = 0f;
        bossLaserAimTimer = 0f;
        bossLaserFireTimer = 0f;
        bossDashWarnTimer = 0f;
        bossDashTimer = 0f;
        bossSpiralTimer = 0f;
        bossSpiralAngle = 0f;
        bossDeathPhase = 0;
        bossDeathTimer = 0f;
        bossSlowMoTimer = 0f;
        conduitArcTimer = 0f;
        scoreRollupBonus = 0f;
        scoreRollupCurrent = 0f;
        scoreRollupTimer = 0f;
        bossBullets.clear();
        essenceOrbs.clear();

        Obs o = new Obs();
        o.boss = true;
        o.bossStage = stage;
        o.type = T_ARMOR;
        o.color = BOSS_COLORS[(stage - 1) % BOSS_COLORS.length];
        o.r = 90f + Math.min(6, stage) * 6f;
        o.x = W / 2f;
        o.y = -o.r - 40f;
        o.baseVy = 260f;
        o.vy = o.baseVy;
        o.vx = (gameRnd.nextBoolean() ? 1f : -1f) * (85f + stage * 15f);
        o.rot = 0f;
        o.vrot = 40f + stage * 8f;
        o.bossShotTimer = 1.3f;
        obstacles.add(o);

        spawnBossConduits(bossHp);

        bossAnnounceName = getBossName(bossKind);
        announceTimer = 2.8f;
        toast(sBossWarningTitle + ": " + bossAnnounceName);
    }

    void spawnSplitFragments(Obs parent) {
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
            obstacles.add(f);
        }
        explode(parent.x, parent.y, 12, parent.color);
    }

    int pickObstacleType() {
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
        // Late events keep the full roster readable while biasing the active
        // pattern toward its signature obstacle for the whole 15-second phase.
        if (runEventKind == RE_MINE_STORM && gameRnd.nextFloat() < 0.68f) {
            return T_MINE;
        }
        if (runEventKind == RE_SPLIT_FIELD && gameRnd.nextFloat() < 0.68f) {
            return T_SPLIT;
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

    int countType(int type) {
        int count = 0;
        for (int i = 0; i < obstacles.size(); i++) {
            if (obstacles.get(i).type == type) {
                count++;
            }
        }
        return count;
    }

    void bounceOff(Obs o) {
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

    void spawnPickup(int kind) {
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

    void hit(Obs o) {
        // В «Дзене» щит всё равно расходуется: он поглощает столкновение,
        // поэтому предмет не превращается в вечное декоративное кольцо.
        if (shieldCharges > 0) {
            shieldCharges--;
            if (countsPersistentProgress()) {
                progress.shieldBlocks++;
            }
            shake = 24f;
            flash = 0.55f;
            invuln = 1.2f + progress.shieldInvulnBonus();
            explode(px, py, 22, C_MINT);
            explode(o.x, o.y, 14, o.color);
            playSound(sndPower, 0.85f);
            buzz(35);
            toast(sShieldBlocked);
            checkProgress();
            return;
        }
        if (zenMode) {
            combo = 0;
            comboTimer = 0f;
            comboPop = 0f;
            softenMultipliersAfterHit();
            shake = 18f;
            flash = 0.45f;
            invuln = 1.2f;
            explode(px, py, 10, C_CYAN);
            explode(o.x, o.y, 10, o.color);
            playSound(sndHit, 1.2f);
            buzz(22);
            return;
        }

        combo = 0;
        comboTimer = 0f;
        comboPop = 0f;
        softenMultipliersAfterHit();

        lives--;
        shake = 30f;
        flash = 1f;
        invuln = 1.7f;
        explode(px, py, 18, C_CYAN);
        explode(o.x, o.y, 16, o.color);
        playSound(sndHit, 0.9f + rnd.nextFloat() * 0.25f);
        buzz(45);
        if (lives <= 0) {
            if (!emergencyPhaseUsed && progress.hasEmergencyPhase()) {
                emergencyPhaseUsed = true;
                lives = 1;
                invuln = progress.emergencyPhaseDuration();
                phaseTimer = invuln;
                toast(sWorkshopEmergencyPhase);
                playSound(sndPower, 1.4f);
                flash = 0.7f;
                return;
            }
            gameOver();
        }
    }

    /** A hit still costs the score multiplier, but keeps half of close-play progress. */
    void softenMultipliersAfterHit() {
        scoreMult = 1f;
        closeMult = 1f + (closeMult - 1f) * 0.5f;
    }

    /**
     * Начисляет базовые очки: на рекорд идёт сумма с полным множителем,
     * в неон — та же сумма с потолком множителя ×3. Возвращает начисленные очки.
     */
    float award(float base) {
        float f = scoreFactor();
        score += base * f;
        neonBase += base * GameRules.neonMult(f);
        return base * f;
    }

    void collect(Pickup p) {
        switch (p.kind) {
            case P_HEART:
                if (lives < maxLives) {
                    lives++;
                    playSound(sndPower, 1f);
                } else {
                    float points = award(60f);
                    addScorePop(p.x, p.y - 40f, "+" + fmt((int) points), C_GOLD);
                    playSound(sndCoin, 1.25f);
                }
                explode(p.x, p.y, 16, C_RED);
                buzz(18);
                break;

            case P_SHIELD:
                shieldCharges = Math.min(shieldCharges + 1, progress.maxShieldCharges());
                float shieldPoints = award(25f);
                addScorePop(p.x, p.y - 40f, "+" + fmt((int) shieldPoints), C_MINT);
                explode(p.x, p.y, 18, C_MINT);
                playSound(sndPower, 1.05f);
                buzz(20);
                toast(shieldCharges > 1 ? sShieldActive + " x" + shieldCharges : sShieldActive);
                if (!hintShieldShown) {
                    hintShieldShown = true;
                    prefs.edit().putBoolean("hint_shield", true).apply();
                    toast(sHintShield);
                }
                break;

            case P_MAGNET:
                magnetTimer = magnetDuration();
                float magnetPoints = award(30f);
                addScorePop(p.x, p.y - 40f, "+" + fmt((int) magnetPoints), C_MAGENTA);
                explode(p.x, p.y, 18, C_MAGENTA);
                playSound(sndPower, 1.15f);
                buzz(20);
                toast(sMagnetActive);
                break;

            case P_SLOW:
                slowTimer = slowDuration();
                float slowPoints = award(30f);
                addScorePop(p.x, p.y - 40f, "+" + fmt((int) slowPoints), C_ICE);
                explode(p.x, p.y, 18, C_ICE);
                playSound(sndPower, 0.85f);
                buzz(20);
                toast(sSlowActive);
                break;

            case P_BOMB:
                bombWaveOriginX = p.x;
                bombWaveOriginY = p.y;
                bombWaveTimer = BOMB_WAVE_DURATION;
                resolveBomb(p.x, p.y);
                toast(sBombActive);
                buzz(28);
                break;

            case P_PHASE:
                phaseTimer = phaseDuration();
                float phasePoints = award(35f);
                addScorePop(p.x, p.y - 40f, "+" + fmt((int) phasePoints), C_VIOLET);
                explode(p.x, p.y, 18, C_VIOLET);
                playSound(sndPower, 1.25f);
                toast(sPhaseActive);
                break;

            case P_DOUBLE:
                doubleTimer = doubleDuration();
                float doublePoints = award(35f);
                addScorePop(p.x, p.y - 40f, "+" + fmt((int) doublePoints), C_GOLD);
                explode(p.x, p.y, 18, C_GOLD);
                playSound(sndPower, 1.4f);
                toast(sDoubleActive);
                break;

            default:
                bumpMultiplier();
                float starPoints = award(50f);
                addScorePop(p.x, p.y - 40f, "+" + fmt((int) starPoints), C_GOLD);
                runStars++;
                if (countsPersistentProgress()) {
                    progress.stars++;
                }
                explode(p.x, p.y, 12, C_GOLD);
                playSound(sndCoin, 0.95f + rnd.nextFloat() * 0.25f);
                if (!hintStarShown) {
                    hintStarShown = true;
                    prefs.edit().putBoolean("hint_star", true).apply();
                    toast(sHintStar);
                }
                break;
        }
        checkProgress();
    }

    void gameOver() {
        // Keep the run alive on the result screen until the player decides
        // whether to spend the one rewarded revive available for this run.
        if (!reviveUsed && !revivePending && !zenMode) {
            holdGameOverForRevive();
            return;
        }
        finishGameOver();
    }

    void holdGameOverForRevive() {
        setKeepScreenOn(false);
        runPrevBest = progress.bestForMode(mode);
        newRecord = (int) score > runPrevBest;
        revivePending = true;
        state = ST_OVER;
        overTime = 0f;
        explode(px, py, 70, C_CYAN);
        explode(px, py, 24, 0xFFFFFFFF);
        shake = 42f;
        buzz(180);
        playSound(sndHit, 0.6f);
    }

    /** Permanently records the run after revive was declined or used. */
    void finishGameOver() {
        setKeepScreenOn(false);
        cleanBossState();
        if (!scoreBanked) {
            runPrevBest = progress.bestForMode(mode);
            newRecord = (int) score > runPrevBest;
            bankScore();
            progress.saveStats();
            checkProgress();
            collectRunEvents();
        }
        revivePending = false;
        state = ST_OVER;
        overTime = 0f;
        explode(px, py, 70, C_CYAN);
        explode(px, py, 24, 0xFFFFFFFF);
        shake = 42f;
        buzz(180);
        playSound(sndHit, 0.6f);
    }

    void finishPendingRun() {
        if (revivePending) {
            finishGameOver();
        }
    }

    /** Called by the rewarded-ad callback; the reward itself is the proof of viewing. */
    void reviveAfterReward() {
        if (!revivePending || reviveUsed) {
            return;
        }
        revivePending = false;
        reviveUsed = true;
        state = ST_PLAY;
        setKeepScreenOn(true);
        lives = 1;
        invuln = 2.4f;
        phaseTimer = 0f;
        hitStop = 0f;
        flash = 0f;
        shake = 0f;
        overTime = 0f;
        inputLock = 0.20f;
        resumeTimer = RESUME_TIME;
        pressedId = B_NONE;
        toast(sReviveWithAd);
        playSound(sndPower, 1.25f);
        buzz(25);
    }

    void showAdLoadingMessage() {
        toast(sAdLoading);
    }


    /** Битовая маска открытых скинов: сферы в младших битах, шлейфы выше. */
    int skinMask() {
        int mask = 0;
        for (int i = 0; i < Progress.SPHERE_COUNT; i++) {
            if (progress.sphereUnlocked(i)) {
                mask |= 1 << i;
            }
        }
        for (int i = 0; i < Progress.TRAIL_COUNT; i++) {
            if (progress.trailUnlocked(i)) {
                mask |= 1 << (16 + i);
            }
        }
        return mask;
    }

    /** Новые скины и ранг за забег — в ленту наград итога. */
    void collectRunEvents() {
        runNeonGained = Math.max(0L, progress.neonEarned - runNeonBefore);
        int fresh = skinMask() & ~runSkinMask;
        for (int i = 0; i < Progress.SPHERE_COUNT; i++) {
            if ((fresh & (1 << i)) != 0) {
                runEvents.add(new RunEvent(RunEvent.SKIN,
                        String.format(sSkinUnlocked, sSphereName[i]), ""));
            }
        }
        for (int i = 0; i < Progress.TRAIL_COUNT; i++) {
            if ((fresh & (1 << (16 + i))) != 0) {
                runEvents.add(new RunEvent(RunEvent.SKIN,
                        String.format(sSkinUnlocked, sTrailName[i]), ""));
            }
        }
        int rank = progress.rank();
        if (rank > runRankBefore) {
            runEvents.add(0, new RunEvent(RunEvent.RANK,
                    String.format(sRankReached, rank), sRankTitle[rankTitleIndex(rank)]));
        }
    }

    static int rankTitleIndex(int rank) {
        return GameRules.titleIndexForRank(rank);
    }

    void startGame() {
        setKeepScreenOn(true);
        // Чередуем два лупа между забегами, чтобы длинная сессия не звучала одинаково.
        musicTrack = (musicTrack + 1) & 1;
        if (musicPlayer != null) {
            releaseMusic();
            initMusic();
        }
        mode = progress.mode;
        hardcoreMode = mode == Progress.MODE_HARDCORE;
        zenMode = mode == Progress.MODE_ZEN;
        maxLives = hardcoreMode ? 1 : MAX_LIVES;
        gameRnd = new Random();
        pickupRnd = new Random();

        obstacles.clear();
        pickups.clear();
        particles.clear();
        scorePops.clear();
        runEvents.clear();
        runNeonBefore = progress.neonEarned;
        runNeonGained = 0L;
        runRankBefore = progress.rank();
        runSkinMask = skinMask();

        elapsed = 0f;
        difficulty = 0f;
        latePressure = 0f;
        score = 0f;
        neonBase = 0f;
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
        closeMult = 1f;
        runMaxOverallMult = 1f;
        bossMult = 1f;
        levelMult = progress.startMultiplier();
        emergencyPhaseUsed = false;
        scoreBanked = false;
        revivePending = false;
        reviveUsed = false;
        shieldCharges = 0;
        magnetTimer = 0f;
        slowTimer = 0f;
        phaseTimer = 0f;
        doubleTimer = 0f;
        spawnPauseTimer = 0f;
        lastBossMinute = 0;
        cleanBossState();
        toastTimer = 0f;
        toastText = "";
        resumeTimer = 0f;
        exitArmed = 0f;
        pressedId = B_NONE;

        if (countsPersistentProgress()) {
            progress.runs++;
            progress.saveStats();
        }

        px = W / 2f;
        targetX = px;
        py = groundY();
        targetY = py;

        spawnTimer = 1.0f;
        coinTimer = 2.0f;
        heartTimer = 18f;
        trailTimer = 0f;
        specialTimer = 12f + gameRnd.nextFloat() * 6f;
        runEventKind = RE_NONE;
        runEventLeft = 0f;
        runEventTimer = 0f;
        checkTimer = 0f;
        saveTimer = 5f;

        state = ST_PLAY;
        playSound(sndPower, 1.35f);
        if (countsPersistentProgress()) {
            int fresh = progress.refreshAchievements();
            toastAchievement(fresh);
        }
    }

    void resetMenu() {
        finishPendingRun();
        setKeepScreenOn(false);
        boolean finishedRun = !scoreBanked && score > 0f;
        if (finishedRun) {
            bankScore();
            progress.saveStats();
        }
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
        scorePops.clear();
        elapsed = 0f;
        difficulty = 0f;
        latePressure = 0f;
        score = 0f;
        neonBase = 0f;
        lives = maxLives;
        invuln = 0f;
        shake = 0f;
        flash = 0f;
        inputLock = 0.15f;
        overTime = 0f;
        revivePending = false;
        reviveUsed = false;
        newRecord = false;
        combo = 0;
        comboTimer = 0f;
        comboPop = 0f;
        scoreMult = 1f;
        closeMult = 1f;
        runMaxOverallMult = 1f;
        bossMult = 1f;
        levelMult = progress.startMultiplier();
        emergencyPhaseUsed = false;
        announceTimer = 0f;
        shieldCharges = 0;
        magnetTimer = 0f;
        slowTimer = 0f;
        phaseTimer = 0f;
        doubleTimer = 0f;
        spawnPauseTimer = 0f;
        lastBossMinute = 0;
        cleanBossState();
        runEventKind = RE_NONE;
        runEventLeft = 0f;
        runEventTimer = 0f;
        toastTimer = 0f;
        px = W / 2f;
        targetX = px;
        py = groundY();
        targetY = py;
        if (finishedRun) {
            checkProgress();
        }
    }

    void bankScore() {
        if (scoreBanked) {
            return;
        }
        scoreBanked = true;
        int finalScore = score >= Integer.MAX_VALUE
                ? Integer.MAX_VALUE : Math.max(0, (int) score);
        progress.recordScore(mode, finalScore);
        // «Дзен» — тренировка: рекорд обновляется, но неон за очки не даётся.
        // Неон считается от базы забега: множители очков влияют на него только
        // до потолка ×3, а «Хардкор» даёт фиксированный бонус +50%.
        if (!zenMode) {
            long gained = Math.round(neonBase * (hardcoreMode ? 1.5f : 1f));
            progress.addNeon((int) Math.min(Integer.MAX_VALUE, gained));
        }
        if (countsPersistentProgress()) {
            progress.addRun(mode, finalScore, Math.max(1, (int) elapsed));
        }
    }

    void bumpMultiplier() {
        scoreMult += progress.starMultiplierStep();
    }

    void resetMultiplier() {
        scoreMult = 1f;
    }

    void resetMultipliers() {
        scoreMult = 1f;
        closeMult = 1f;
    }

    String fmtMult(float m) {
        if (Math.abs(m - Math.round(m * 10f) / 10f) < 0.005f) {
            return String.format(java.util.Locale.getDefault(), "%.1f", m);
        }
        return String.format(java.util.Locale.getDefault(), "%.2f", m);
    }

    void checkProgress() {
        if (countsPersistentProgress() && combo > progress.bestCombo) {
            progress.bestCombo = combo;
        }
        if (countsPersistentProgress() && (int) elapsed > progress.bestTime) {
            progress.bestTime = (int) elapsed;
        }
        if (countsPersistentProgress()) {
            int fresh = progress.refreshAchievements();
            toastAchievement(fresh);
        }
    }

    void toastAchievement(int fresh) {
        if (fresh == 0) {
            return;
        }
        boolean toasted = false;
        for (int i = 0; i < Progress.ACH_COUNT; i++) {
            if ((fresh & (1 << i)) != 0) {
                if (!toasted) {
                    toast(sAchievementUnlocked + " · " + sAchName[i]);
                    toasted = true;
                }
                runEvents.add(new RunEvent(RunEvent.ACHIEVEMENT,
                        sAchievementUnlocked + " · " + sAchName[i], ""));
            }
        }
    }

    void toast(String text) {
        toastText = text;
        toastTimer = 2.2f;
    }

    void addParticle(float x, float y, float vx, float vy,
                     float life, float size, int color, float grav) {
        addParticle(x, y, vx, vy, life, size, color, grav, 0);
    }

    void addParticle(float x, float y, float vx, float vy,
                     float life, float size, int color, float grav, int style) {
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
        p.style = style;
        particles.add(p);
    }

    void explode(float x, float y, int count, int color) {
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

    void addScorePop(float x, float y, String text, int color) {
        if (scorePops.size() >= 28) {
            scorePops.remove(0);
        }
        ScorePop pop = new ScorePop();
        pop.x = x;
        pop.y = y;
        pop.text = text;
        pop.color = color;
        pop.life = 0.95f;
        pop.maxLife = pop.life;
        scorePops.add(pop);
    }

    void updateScorePops(float dt) {
        for (int i = scorePops.size() - 1; i >= 0; i--) {
            ScorePop pop = scorePops.get(i);
            pop.life -= dt;
            pop.y -= 58f * dt;
            if (pop.life <= 0f) {
                scorePops.remove(i);
            }
        }
    }

    void drawScorePops(Canvas c) {
        for (int i = 0; i < scorePops.size(); i++) {
            ScorePop pop = scorePops.get(i);
            float a = clamp(pop.life / pop.maxLife, 0f, 1f);
            text(c, pop.text, pop.x, pop.y, 28f,
                    Color.argb((int) (255f * a), Color.red(pop.color),
                            Color.green(pop.color), Color.blue(pop.color)),
                    tfBlack, Paint.Align.CENTER);
        }
    }
    void resolveBomb(float bx, float by) {
        int cleared = 0;
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obs o = obstacles.get(i);
            if (o.boss) {
                // Бомба оглушает босса и сбивает его выстрелы, но не уничтожает босса мгновенно
                o.bounceFlash = 1f;
                bossStunTimer = Math.max(bossStunTimer, 1.3f);
                o.bossShotTimer = Math.max(o.bossShotTimer, 2.0f);
                continue;
            }
            explode(o.x, o.y, 12, o.color);
            cleared++;
            obstacles.remove(i);
        }
        bossBullets.clear();
        spawnPauseTimer = 1.1f;
        flash = 0.40f;
        shake = 26f;
        explode(bx, by, 40, C_ORANGE);
        for (int k = 0; k < 12; k++) {
            float ang = k * 30f;
            float spd = 260f + gameRnd.nextFloat() * 320f;
            addParticle(bx, by, (float) Math.cos(Math.toRadians(ang)) * spd,
                    (float) Math.sin(Math.toRadians(ang)) * spd, 0.50f, 9f, C_ORANGE, 0f, 2);
        }
        float points = award(150f + cleared * 90f);
        addScorePop(bx, by - 40f, "+" + fmt((int) points), C_ORANGE);
        playSound(sndHit, 0.7f);
    }
}
