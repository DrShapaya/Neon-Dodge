package com.isaev.neondodge;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.view.Choreographer;

/** Раскладка экрана, игровой цикл и обновление мира. */
abstract class GameLoop extends GameSpawner {

    GameLoop(Context context) {
        super(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        // Keep the portrait game field readable on tablets and landscape
        // windows: letterbox the 1080xvariable-height world instead of
        // stretching the menu into a narrow strip.
        scale = Math.min(w / W, h / 1920f);
        viewOffsetX = (w - W * scale) * 0.5f;
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
                || state == ST_STATS || state == ST_SKINS
                || state == ST_WORKSHOP
                || state == ST_HISTORY) {
            px = W / 2f;
            targetX = px;
            py = groundY();
            targetY = py;
        }
    }

    float touchTargetY(float touchY) {
        return clamp(touchY - effectiveFingerOffset(), playerMinY(), playerMaxY());
    }

    float effectiveFingerOffset() {
        return controlMode == CONTROL_AT ? 0f : fingerOffset;
    }

    void recalcInsets() {
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
                || state == ST_STATS || state == ST_SKINS
                || state == ST_HISTORY || state == ST_WORKSHOP) {
            py = groundY();
            targetY = py;
        }
    }

    void rebuildScrim() {
        float h = Math.max(340f, insetTop + 320f);
        hudScrimH = h;
        hudScrim = new LinearGradient(0f, 0f, 0f, h,
                new int[]{0xCC05070F, 0x8005070F, 0x0005070F},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP);
    }

    float groundY() {
        return clamp(worldH - 250f - insetBottom, playerMinY(), playerMaxY());
    }

    float playerMinY() {
        return Math.max(PLAYER_R + 24f, insetTop + 210f);
    }

    float playerMaxY() {
        return Math.max(playerMinY() + 1f,
                worldH - insetBottom - PLAYER_R - 24f);
    }

    void startLoop() {
        if (running) {
            return;
        }
        running = true;
        lastNs = 0L;
        Choreographer.getInstance().postFrameCallback(this);
    }

    void stopLoop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        // Keep menus and gameplay on the same 60 FPS frame cadence.
        long minFrameNs = targetHz == 120 ? 8_333_333L : 16_000_000L;
        if (lastNs != 0L && frameTimeNanos - lastNs < minFrameNs) {
            Choreographer.getInstance().postFrameCallback(this);
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
        applyTargetRefreshRate();
        startAudioInit();
        startLoop();
    }

    void applyTargetRefreshRate() {
        if (Build.VERSION.SDK_INT >= 35) {
            setRequestedFrameRate(targetHz);
        }
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
        pauseMusic();
        stopLoop();
    }

    public void onHostResume() {
        // Не блокируем первый кадр меню декодированием OGG; музыка стартует
        // при начале забега или возвращении из паузы.
        if (state == ST_PLAY || state == ST_PAUSE) {
            startMusicIfEnabled();
        }
        startLoop();
    }

    void update(float dt) {
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
        updateScorePops(dt);

        if (state == ST_PLAY) {
            if (resumeTimer > 0f) {
                resumeTimer = Math.max(0f, resumeTimer - dt);
                updateResume(dt);
            } else {
                updatePlay(dt);
            }
        } else if (state == ST_MENU || state == ST_SETTINGS
                || state == ST_STATS || state == ST_SKINS
                || state == ST_HISTORY) {
            updateMenu(dt);
        } else if (state == ST_OVER) {
            overTime += dt;
        }
    }

    /** Во время отсчёта мир стоит, а сфера уже слушается пальца. */
    void updateResume(float dt) {
        targetX = clamp(targetX, PLAYER_R + 6f, W - PLAYER_R - 6f);
        targetY = clamp(targetY, playerMinY(), playerMaxY());
        float k = 1f - (float) Math.exp(-16f * dt);
        followTarget(k);
        px = clamp(px, PLAYER_R + 6f, W - PLAYER_R - 6f);
        py = clamp(py, playerMinY(), playerMaxY());
        invuln = Math.max(invuln, 0.5f);
    }

    void updateStars(float dt) {
        float extra = state == ST_PLAY && resumeTimer <= 0f ? 40f + difficulty * 90f : 0f;
        for (int i = 0; i < starX.length; i++) {
            starY[i] += (starSpd[i] + extra) * dt;
            if (starY[i] > worldH + 12f) {
                starY[i] = -12f;
                starX[i] = rnd.nextFloat() * W;
            }
        }
    }

    void updateMenu(float dt) {
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

    void updatePlay(float dt) {
        elapsed += dt;
        float diffCap = zenMode ? 0.60f : 1f;
        float diffTime = zenMode ? 120f : 90f;
        difficulty = Math.min(diffCap, elapsed / diffTime);
        // После достижения потолка сложность не останавливается: давление
        // растёт ещё несколько минут, но медленно и с жёстким лимитом.
        float late = zenMode ? 0f : Math.max(0f, elapsed - diffTime);
        latePressure = Math.min(0.30f, late / 300f);
        float f = scoreFactor();
        float scoreTick = dt * (70f + 120f * difficulty)
                * (1f + 0.25f * latePressure);
        score += scoreTick * f;
        neonBase += scoreTick * GameRules.neonMult(f);
        if (countsPersistentProgress()) {
            progress.totalTime += dt;
        }
        if (countsPersistentProgress() && (int) elapsed > progress.bestTime) {
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
        spawnPauseTimer = Math.max(0f, spawnPauseTimer - dt);
        if (bombWaveTimer > 0f) {
            bombWaveTimer -= dt;
        }

        updateLateEvent(dt);

        // Mini-bosses arrive every two minutes. The stage keeps increasing, so
        // long record runs continue to get a readable tempo spike.
        int minute = (int) (elapsed / 60f);
        if (minute >= 2 && (minute & 1) == 0 && minute > lastBossMinute) {
            lastBossMinute = minute;
            spawnBoss(minute);
            announceTimer = 2.4f;
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
            float pyOffset = py + PLAYER_R * 0.65f;
            switch (ti) {
                case 0: // Неон: двойной неоновый поток слева и справа
                    addParticle(px - 14f, pyOffset, (rnd.nextFloat() - 0.5f) * 20f,
                            60f + rnd.nextFloat() * 70f, 0.40f, 6.5f, 0xCC5FF3FF, 0f, 0);
                    addParticle(px + 14f, pyOffset, (rnd.nextFloat() - 0.5f) * 20f,
                            60f + rnd.nextFloat() * 70f, 0.40f, 6.5f, 0xCC5FF3FF, 0f, 0);
                    break;
                case 1: // Искры: электрические штрихи во все стороны
                    for (int s = 0; s < 2; s++) {
                        float sAngle = rnd.nextFloat() * 6.28f;
                        float sSpd = 90f + rnd.nextFloat() * 160f;
                        addParticle(px + (rnd.nextFloat() - 0.5f) * 16f, pyOffset,
                                (float) Math.cos(sAngle) * sSpd,
                                50f + (float) Math.sin(sAngle) * sSpd * 0.6f,
                                0.28f + rnd.nextFloat() * 0.20f, 8f,
                                rnd.nextBoolean() ? 0xFFFFFFFF : 0xFFFFD75E, 0f, 2);
                    }
                    break;
                case 2: // Пламя: градиент от белого к огненно-рыжему с подъёмом
                    int flameCol = rnd.nextFloat() < 0.35f ? 0xFFFFF6D0
                            : (rnd.nextBoolean() ? 0xFFFF9E4F : 0xFFFF4F4F);
                    addParticle(px + (rnd.nextFloat() - 0.5f) * 18f, pyOffset,
                            (rnd.nextFloat() - 0.5f) * 45f,
                            50f + rnd.nextFloat() * 100f,
                            0.45f + rnd.nextFloat() * 0.25f,
                            7f + rnd.nextFloat() * 8f, flameCol, -60f, 0);
                    break;
                case 3: // Тень / Призрак: мягкие призрачные эхо-силуэты сферы
                    addParticle(px, py, 0f, 20f, 0.55f, PLAYER_R * 0.95f, 0x55A46BFF, 0f, 3);
                    break;
                case 4: // Магнит: вихревые частицы электромагнитного поля
                    float magPhase = time * 18f;
                    float mx = px + (float) Math.sin(magPhase) * 26f;
                    addParticle(mx, pyOffset, (float) Math.cos(magPhase) * 60f,
                            55f + rnd.nextFloat() * 70f, 0.38f, 7f, 0xEEFF4FD8, 0f, 0);
                    addParticle(px - (mx - px), pyOffset, -(float) Math.cos(magPhase) * 60f,
                            55f + rnd.nextFloat() * 70f, 0.38f, 7f, 0xEE8B4FFF, 0f, 0);
                    break;
                case 5: // Комета: сверкающее ядро и россыпь космической пыли
                    addParticle(px + (rnd.nextFloat() - 0.5f) * 8f, pyOffset,
                            (rnd.nextFloat() - 0.5f) * 30f, 90f + rnd.nextFloat() * 110f,
                            0.42f, 9f, 0xFFFFE5D0, 0f, 0);
                    for (int ci = 0; ci < 2; ci++) {
                        addParticle(px + (rnd.nextFloat() - 0.5f) * 24f, pyOffset + rnd.nextFloat() * 12f,
                                (rnd.nextFloat() - 0.5f) * 110f, 60f + rnd.nextFloat() * 80f,
                                0.32f, 3.5f, 0xDDFF7A45, 0f, 0);
                    }
                    break;
                case 6: // Импульс: расходящиеся ударные звуковые кольца
                    addParticle(px, pyOffset, 0f, 35f, 0.48f, 18f, 0xEE65E6C5, 0f, 1);
                    break;
            }
        }

        float sdt = dt * slowFactor();

        spawnTimer -= sdt;
        if (!bossActive && spawnTimer <= 0f && spawnPauseTimer <= 0f && elapsed > 0.7f) {
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
            heartTimer -= dt * progress.secondWindRateScale();
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
        updateBossMechanics(sdt);
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
        }
    }

    /** Runs a short late-game pattern every 45 seconds after the pressure ramp. */
    void updateLateEvent(float dt) {
        if (bossActive) {
            return;
        }
        if (runEventKind != RE_NONE) {
            runEventLeft -= dt;
            if (runEventLeft <= 0f) {
                runEventKind = RE_NONE;
                runEventLeft = 0f;
                runEventTimer = 45f;
            }
            return;
        }
        if (elapsed < LATE_EVENT_START) {
            return;
        }
        runEventTimer -= dt;
        if (runEventTimer <= 0f) {
            int cycle = Math.max(0, (int) ((elapsed - LATE_EVENT_START) / 45f));
            runEventKind = (cycle & 1) == 0 ? RE_MINE_STORM : RE_SPLIT_FIELD;
            runEventLeft = 15f;
            runEventTimer = 45f;
            if (runEventKind == RE_MINE_STORM) {
                toast(sLateMineStorm);
                spawnLateEventPattern(T_MINE, 4);
            } else {
                toast(sLateSplitField);
                spawnLateEventPattern(T_SPLIT, 4);
            }
        }
    }

    void spawnLateEventPattern(int type, int count) {
        for (int i = 0; i < count; i++) {
            spawnObstacle(type);
        }
    }

    void followTarget(float k) {
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
        if (bossActive && bossKind == BOSS_KIND_VORTEX && bossStunTimer <= 0f) {
            float dx = (W / 2f) - px;
            float dy = bossStopY() - py;
            float d2 = dx * dx + dy * dy;
            if (d2 > 1f) {
                float d = (float) Math.sqrt(d2);
                float strength = 75f;
                pullX += (dx / d) * strength;
                pullY += (dy / d) * (strength * 0.4f);
            }
        }
        float pullLen = (float) Math.sqrt(pullX * pullX + pullY * pullY);
        if (pullLen > 132f) {
            pullX *= 132f / pullLen;
            pullY *= 132f / pullLen;
        }
        px += (targetX + pullX - px) * k;
        py += (targetY + pullY - py) * k;
    }

    float slowFactor() {
        if (bossSlowMoTimer > 0f) {
            return 0.20f;
        }
        return slowTimer > 0f ? SLOW_SCALE : 1f;
    }

    float scoreFactor() {
        float f = scoreMult * closeMult * bossMult * levelMult
                * (hardcoreMode ? HARDCORE_MULT : 1f)
                * (doubleTimer > 0f ? progress.temporaryMultiplier() : 1f);
        f = Math.min(f, 25000f);
        if (state == ST_PLAY) {
            runMaxOverallMult = Math.max(runMaxOverallMult, f);
        }
        return f;
    }

    void updateObstacles(float dt) {
        float sdt = dt * slowFactor();
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obs o = obstacles.get(i);

            if (o.boss) {
                float stopY = bossStopY();
                if (bossDashTimer > 0f) {
                    o.vy = 0f;
                    o.vx = 0f;
                } else {
                    o.vy = o.y < stopY ? o.baseVy : 0f;
                }
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
                    o.vy = o.baseVy * 0.72f;
                    if (dist2(o.x, o.y, px, py) <= MINE_TRIGGER_R * MINE_TRIGGER_R) {
                        o.mineArmed = true;
                        o.fuse = 0f;
                        buzz(16);
                        playSound(sndPower, 1.8f);
                    }
                } else {
                    o.fuse += sdt;
                    o.vy *= 0.12f;
                    o.vx *= 0.12f;
                    if (o.fuse >= MINE_FUSE_TIME) {
                        explode(o.x, o.y, 32, o.color);
                        playSound(sndHit, 0.85f);
                        shake = Math.max(shake, 20f);
                        flash = 0.25f;
                        float blastR = MINE_BLAST_R + PLAYER_R;
                        if (dist2(o.x, o.y, px, py) <= blastR * blastR) {
                            hit(o);
                        } else {
                            float pts = award(70f);
                            addScorePop(o.x, o.y, "+" + fmt((int) pts), C_LIME);
                            combo++;
                            comboTimer = 1.8f + progress.comboExtraTime();
                        }
                        obstacles.remove(i);
                        continue;
                    }
                }
            } else {
                o.vy = o.baseVy;
            }

            o.y += o.vy * sdt;
            o.x += o.vx * sdt;

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
            o.warnTimer = Math.max(0f, o.warnTimer - dt);
            o.rot += o.vrot * sdt;

            if (!o.passed && o.y > py + PLAYER_R * 0.5f) {
                o.passed = true;
                runDodged++;
                if (countsPersistentProgress()) {
                    progress.dodged++;
                }
                float dx = Math.abs(o.x - px);
                // В фазе или при обычном уклонении без урона засчитываем комбо
                if ((phaseTimer > 0f || invuln <= 0f) && dx < o.r + PLAYER_R + 92f) {
                    combo++;
                    if (combo > runMaxCombo) {
                        runMaxCombo = combo;
                    }
                    if (countsPersistentProgress() && combo > progress.bestCombo) {
                        progress.bestCombo = combo;
                    }
                    comboTimer = 2.2f + progress.comboExtraTime();
                    comboPop = 1f;
                    if (!hintComboShown) {
                        hintComboShown = true;
                        prefs.edit().putBoolean("hint_combo", true).apply();
                        toast(sHintCombo);
                    }
                    closeMult = Math.min(CLOSE_MULT_CAP, closeMult + progress.closeMultiplierStep());
                    float comboPoints = award(5f * combo);
                    addScorePop(o.x, py - 70f, "+" + fmt((int) comboPoints), C_GOLD);
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

            if (phaseTimer <= 0f && invuln <= 0f && state == ST_PLAY) {
                float rr = o.r * 0.80f + PLAYER_R * 0.80f;
                if (dist2(o.x, o.y, px, py) < rr * rr) {
                    boolean survivesShield = (o.type == T_ARMOR || o.boss)
                            && shieldCharges > 0 && !zenMode;
                    if (o.type == T_ARMOR && survivesShield) {
                        o.armorHits = GameRules.nextArmorHits(o.armorHits);
                        o.bounceFlash = 1f;
                        toast(sArmorCracked);
                        hit(o);
                        continue;
                    }
                    if (o.boss) {
                        hit(o);
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

    void updatePickups(float dt) {
        float sdt = dt * slowFactor();
        float magScale = progress.magnetRadiusScale();
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            boolean pulled = false;
            float dx = px - p.x;
            float dy = py - p.y;
            float d2 = dx * dx + dy * dy;
            float d = (float) Math.sqrt(Math.max(1f, d2));
            float passiveRadius = 110f * magScale;
            if (magnetTimer > 0f) {
                // Медленно и плавно притягивает по всему экрану (звезды, хп, усиления)
                float sp = 160f * magScale;
                p.x += dx / d * sp * dt;
                p.y += dy / d * sp * dt;
                pulled = true;
                if (rnd.nextFloat() < dt * 6f) {
                    addParticle(p.x, p.y, (rnd.nextFloat() - 0.5f) * 40f,
                            (rnd.nextFloat() - 0.5f) * 40f,
                            0.25f + rnd.nextFloat() * 0.25f,
                            2f + rnd.nextFloat() * 2f, C_MAGENTA, 0f);
                }
            } else if (d < passiveRadius) {
                float sp = 130f * (1f - d / passiveRadius) * magScale;
                p.x += dx / d * sp * dt;
                p.y += dy / d * sp * dt;
                pulled = true;
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

    /** Multi-stage epic boss encounter with unique attack patterns, conduits, and essence harvesting. */
    void updateBossMechanics(float dt) {
        if (bossDeathPhase > 0) {
            updateBossDeath(dt);
            return;
        }

        Obs boss = null;
        for (int i = 0; i < obstacles.size(); i++) {
            if (obstacles.get(i).boss) {
                boss = obstacles.get(i);
                break;
            }
        }
        if (boss == null) {
            if (bossActive) {
                cleanBossState();
            }
            return;
        }

        float stopY = bossStopY();
        if (boss.y < stopY) {
            return;
        }

        // 1. Оглушение и покачивание босса
        if (bossStunTimer > 0f) {
            bossStunTimer -= dt;
            bossWobble = (float) Math.sin(time * 35f) * 14f;
            boss.bounceFlash = Math.max(boss.bounceFlash, 0.6f);
        } else {
            bossWobble = 0f;
            if (bossDashTimer <= 0f) {
                boss.x = clamp(boss.x + (float) Math.sin(time * 0.85f + boss.bossStage) * (110f + boss.bossStage * 15f) * dt,
                        boss.r + 30f, W - boss.r - 30f);
            }
        }

        // 2. Резонаторы перегрузки (Conduits)
        conduitArcTimer = Math.max(0f, conduitArcTimer - dt);
        boolean allConduitsDone = true;
        for (int i = 0; i < bossConduits.size(); i++) {
            BossConduit c = bossConduits.get(i);
            c.pulse += dt * 3.5f;
            c.appearAnim = Math.min(1f, c.appearAnim + dt * 3f);
            if (!c.active) {
                allConduitsDone = false;
                if (dist2(px, py, c.x, c.y) <= (c.r + PLAYER_R) * (c.r + PLAYER_R)) {
                    c.active = true;
                    explode(c.x, c.y, 20, C_CYAN);
                    playSound(sndCoin, 1.25f);
                    buzz(18);
                    float pts = award(250f * progress.conduitDamageScale());
                    addScorePop(c.x, c.y - 35f, "+" + fmt((int) pts), C_CYAN);
                }
            }
        }

        // Если все узлы замкнуты — наносим урон боссу!
        if (allConduitsDone && !bossConduits.isEmpty() && bossStunTimer <= 0f) {
            conduitArcTimer = 0.65f;
            conduitArcX = bossConduits.get(bossConduits.size() - 1).x;
            conduitArcY = bossConduits.get(bossConduits.size() - 1).y;
            playSound(sndHit, 1.35f);
            playSound(sndPower, 1.4f);
            shake = 28f;
            flash = 0.45f;
            bossHp--;
            bossStunTimer = 1.15f + progress.conduitStunBonus();
            explode(boss.x, boss.y, 50, boss.color);
            addScorePop(boss.x, boss.y + 40f, sBossConduitLinked, C_GOLD);

            if (bossHp <= 0) {
                // Победа над боссом: переход к фазе разрушения и поглощения эссенции
                bossDeathPhase = 1;
                bossDeathTimer = 0f;
                bossSlowMoTimer = 2.8f;
                bossConduits.clear();
                bossBullets.clear();
                return;
            } else {
                spawnBossConduits(bossHp);
            }
        }

        // 3. Атаки босса (если не оглушен)
        if (bossStunTimer <= 0f) {
            updateBossAttacks(boss, dt);
        }

        // 4. Обновление снарядов босса
        updateBossProjectiles(boss, dt);
    }

    void updateBossAttacks(Obs boss, float dt) {
        switch (bossKind) {
            case BOSS_KIND_PRISM: // Лазер + веер кристаллов
                bossLaserY = boss.y + boss.r * 0.45f;
                if (bossLaserFireTimer > 0f) {
                    bossLaserFireTimer -= dt;
                    if (py > bossLaserY && Math.abs(px - bossLaserX) < (bossLaserWidth + PLAYER_R * 0.72f)) {
                        hitLaser();
                    }
                } else if (bossLaserAimTimer > 0f) {
                    bossLaserAimTimer -= dt;
                    bossLaserTargetX = px;
                    bossLaserX += (bossLaserTargetX - bossLaserX) * Math.min(1f, dt * 5.5f);
                    if (bossLaserAimTimer <= 0f) {
                        bossLaserFireTimer = 0.48f;
                        playSound(sndPower, 1.6f);
                        shake = 20f;
                    }
                }

                boss.bossShotTimer -= dt;
                if (boss.bossShotTimer <= 0f) {
                    int numShards = (bossHp <= 1) ? 6 : 5;
                    float spread = 70f;
                    float startAngle = -spread / 2f;
                    float step = spread / (numShards - 1);
                    float spd = 320f + boss.bossStage * 25f;

                    for (int s = 0; s < numShards; s++) {
                        float angle = (float) Math.toRadians(90f + startAngle + s * step);
                        BossBullet b = new BossBullet();
                        b.x = boss.x;
                        b.y = boss.y + boss.r * 0.7f;
                        b.vx = (float) Math.cos(angle) * spd;
                        b.vy = (float) Math.sin(angle) * spd;
                        b.r = 16f;
                        b.color = boss.color;
                        b.kind = (s == numShards / 2) ? BULLET_REFLECTABLE : BULLET_NORMAL;
                        if (b.kind == BULLET_REFLECTABLE) {
                            b.r = 24f;
                            b.color = C_GOLD;
                        }
                        bossBullets.add(b);
                    }
                    boss.bossShotTimer = (bossHp <= 1) ? 1.7f : 2.3f;
                    if (bossLaserAimTimer <= 0f && bossLaserFireTimer <= 0f) {
                        bossLaserAimTimer = 1.1f;
                        bossLaserX = px;
                    }
                }
                break;

            case BOSS_KIND_VORTEX: // Спирали + притяжение
                bossSpiralTimer -= dt;
                if (bossSpiralTimer <= 0f) {
                    bossSpiralTimer = (bossHp <= 1) ? 0.16f : 0.22f;
                    bossSpiralAngle += 24f;
                    for (int arm = 0; arm < 2; arm++) {
                        float rad = (float) Math.toRadians(bossSpiralAngle + arm * 180f);
                        BossBullet b = new BossBullet();
                        b.x = boss.x;
                        b.y = boss.y + boss.r * 0.5f;
                        float spd = 260f + boss.bossStage * 18f;
                        b.vx = (float) Math.cos(rad) * spd;
                        b.vy = Math.max(120f, (float) Math.sin(rad) * spd + 160f);
                        b.r = 14f;
                        b.color = (arm == 0) ? C_MAGENTA : C_VIOLET;
                        b.kind = BULLET_SPIRAL;
                        bossBullets.add(b);
                    }
                }
                break;

            case BOSS_KIND_STRIKER: // Тараны + дуговые лезвия
                if (bossDashTimer > 0f) {
                    bossDashTimer -= dt;
                    boss.x = bossDashX;
                    float dashProgress = 1f - bossDashTimer / 0.65f;
                    if (dashProgress < 0.5f) {
                        boss.y = bossDashStartY + (bossDashTargetY - bossDashStartY) * (dashProgress * 2f);
                    } else {
                        boss.y = bossDashTargetY - (bossDashTargetY - bossDashStartY) * ((dashProgress - 0.5f) * 2f);
                    }
                    if (dist2(boss.x, boss.y, px, py) < (boss.r + PLAYER_R) * (boss.r + PLAYER_R)) {
                        hit(boss);
                    }
                } else if (bossDashWarnTimer > 0f) {
                    bossDashWarnTimer -= dt;
                    if (bossDashWarnTimer <= 0f) {
                        bossDashTimer = 0.65f;
                        boss.x = bossDashX;
                        bossDashStartY = boss.y;
                        bossDashTargetY = groundY() - 40f;
                        playSound(sndHit, 1.4f);
                        shake = 26f;
                    }
                } else {
                    boss.bossShotTimer -= dt;
                    if (boss.bossShotTimer <= 0f) {
                        bossDashWarnTimer = 0.9f;
                        bossDashX = px;
                        boss.bossShotTimer = 3.2f;

                        for (int side = -1; side <= 1; side += 2) {
                            BossBullet b = new BossBullet();
                            b.x = boss.x;
                            b.y = boss.y + boss.r * 0.6f;
                            b.vx = side * 280f;
                            b.vy = 260f;
                            b.r = 20f;
                            b.color = C_ORANGE;
                            b.kind = BULLET_REFLECTABLE;
                            bossBullets.add(b);
                        }
                    }
                }
                break;

            case BOSS_KIND_NEXUS: // Комбинированный бой
            default:
                boss.bossShotTimer -= dt;
                if (boss.bossShotTimer <= 0f) {
                    for (int s = -2; s <= 2; s++) {
                        BossBullet b = new BossBullet();
                        b.x = boss.x + s * 30f;
                        b.y = boss.y + boss.r * 0.7f;
                        b.vx = s * 65f;
                        b.vy = 320f + boss.bossStage * 30f;
                        b.r = 17f;
                        b.color = boss.color;
                        b.kind = (s == 0) ? BULLET_REFLECTABLE : BULLET_NORMAL;
                        if (b.kind == BULLET_REFLECTABLE) {
                            b.r = 24f;
                            b.color = C_GOLD;
                        }
                        bossBullets.add(b);
                    }
                    boss.bossShotTimer = 1.8f;
                }
                break;
        }
    }

    void updateBossProjectiles(Obs boss, float dt) {
        for (int i = bossBullets.size() - 1; i >= 0; i--) {
            BossBullet b = bossBullets.get(i);
            if (b.reflected) {
                if (boss == null) {
                    b.y -= 800f * dt;
                    if (b.y < -120f) {
                        bossBullets.remove(i);
                    }
                    continue;
                }
                float dx = boss.x - b.x;
                float dy = boss.y - b.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 1f) {
                    float spd = 950f;
                    b.vx = (dx / dist) * spd;
                    b.vy = (dy / dist) * spd;
                }
                b.x += b.vx * dt;
                b.y += b.vy * dt;

                if (dist2(b.x, b.y, boss.x, boss.y) <= (boss.r + b.r) * (boss.r + b.r)) {
                    bossBullets.remove(i);
                    bossStunTimer = Math.max(bossStunTimer, 0.8f);
                    explode(boss.x, boss.y, 28, C_MINT);
                    playSound(sndHit, 1.2f);
                    addScorePop(boss.x, boss.y + 45f, sBossReflect + " +500", C_MINT);
                    award(500f);
                    shake = 22f;
                    continue;
                }
            } else {
                b.x += b.vx * dt;
                b.y += b.vy * dt;

                if (b.x < b.r || b.x > W - b.r) {
                    b.vx = -b.vx;
                    b.x = clamp(b.x, b.r, W - b.r);
                }

                // Close Dodge для отражения нестабильных ядер
                if (b.kind == BULLET_REFLECTABLE && !b.reflected && invuln <= 0f) {
                    float d2 = dist2(b.x, b.y, px, py);
                    float hitDist = b.r + PLAYER_R;
                    float reflectDist = hitDist + 65f;
                    if (d2 > hitDist * hitDist && d2 <= reflectDist * reflectDist) {
                        b.reflected = true;
                        b.color = C_MINT;
                        b.vy = -600f;
                        playSound(sndPower, 1.4f);
                        addScorePop(px, py - 35f, sBossReflect, C_MINT);
                        bumpMultiplier();
                        continue;
                    }
                }

                if (invuln <= 0f && dist2(b.x, b.y, px, py) < (b.r + PLAYER_R) * (b.r + PLAYER_R)) {
                    bossBullets.remove(i);
                    hitBossBullet(b);
                    continue;
                }
            }

            if (b.y - b.r > worldH + 60f || b.y + b.r < -120f) {
                bossBullets.remove(i);
            }
        }
    }

    void updateBossDeath(float dt) {
        bossSlowMoTimer = Math.max(0f, bossSlowMoTimer - dt);
        bossDeathTimer += dt;

        // Фаза 1: Разрушение босса
        if (bossDeathPhase == 1) {
            bossWobble = (float) Math.sin(time * 45f) * 18f;
            if (bossDeathTimer >= 0.75f) {
                bossDeathPhase = 2;
                bossDeathTimer = 0f;
                for (int i = obstacles.size() - 1; i >= 0; i--) {
                    Obs o = obstacles.get(i);
                    if (o.boss) {
                        bossEssenceX = o.x;
                        bossEssenceY = o.y;
                        explode(o.x, o.y, 70, o.color);
                        obstacles.remove(i);
                        break;
                    }
                }
                essenceOrbs.clear();
                for (int e = 0; e < 18; e++) {
                    EssenceOrb orb = new EssenceOrb();
                    orb.x = bossEssenceX;
                    orb.y = bossEssenceY;
                    float ang = rnd.nextFloat() * 6.28f;
                    float spd = 200f + rnd.nextFloat() * 280f;
                    orb.vx = (float) Math.cos(ang) * spd;
                    orb.vy = (float) Math.sin(ang) * spd;
                    orb.r = 12f + rnd.nextFloat() * 8f;
                    orb.color = (e % 2 == 0) ? C_CYAN : C_GOLD;
                    orb.life = 0f;
                    orb.maxLife = 2.4f;
                    orb.delay = e * 0.045f;
                    orb.absorbed = false;
                    essenceOrbs.add(orb);
                }
                playSound(sndPower, 1.5f);
                shake = 32f;
                flash = 0.5f;
            }
        }
        // Фаза 2: Полет и поглощение эссенции игроком
        else if (bossDeathPhase == 2) {
            boolean allAbsorbed = true;
            for (int e = 0; e < essenceOrbs.size(); e++) {
                EssenceOrb orb = essenceOrbs.get(e);
                if (orb.absorbed) continue;
                allAbsorbed = false;
                orb.life += dt;
                if (orb.life < orb.delay) {
                    orb.vx *= 0.94f;
                    orb.vy *= 0.94f;
                } else {
                    float dx = px - orb.x;
                    float dy = py - orb.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist > 1f) {
                        float speed = 1100f + (orb.life - orb.delay) * 800f;
                        orb.vx = (dx / dist) * speed;
                        orb.vy = (dy / dist) * speed;
                    }
                }
                orb.x += orb.vx * dt;
                orb.y += orb.vy * dt;

                if (dist2(orb.x, orb.y, px, py) <= (PLAYER_R + orb.r + 20f) * (PLAYER_R + orb.r + 20f)) {
                    orb.absorbed = true;
                    playerAbsorptionPulse = 1.0f;
                    explode(px, py, 6, orb.color);
                    playSound(sndCoin, 1.15f + (rnd.nextFloat() * 0.35f));
                    buzz(10);
                }
            }

            if (allAbsorbed || bossDeathTimer >= 1.6f) {
                bossDeathPhase = 3;
                bossDeathTimer = 0f;
                scoreRollupTimer = 2.0f;

                float baseAward = 1500f + lastBossMinute * 250f;
                float f = scoreFactor();
                scoreRollupBonus = baseAward * f;
                scoreRollupCurrent = 0f;

                shieldCharges = Math.min(shieldCharges + 1, progress.maxShieldCharges());
                bossMult = progress.bossMultiplier();

                if (countsPersistentProgress()) {
                    progress.bossesDefeated++;
                }
                progress.saveStats();
                checkProgress();

                playSound(sndPower, 1.45f);
                toast(sBossEssenceHarvested + " · " + String.format(sBossRewardScore, fmtMult(bossMult)));
                addScorePop(px, py - 110f, "+" + fmt((int) scoreRollupBonus), C_GOLD);
            }
        }
        // Фаза 3: Плавное увеличение счета и искры
        else if (bossDeathPhase == 3) {
            scoreRollupTimer -= dt;
            playerAbsorptionPulse = Math.max(0f, playerAbsorptionPulse - dt * 2.2f);

            float delta = Math.min(scoreRollupBonus - scoreRollupCurrent, (scoreRollupBonus / 1.3f) * dt);
            if (delta > 0f) {
                scoreRollupCurrent += delta;
                score += delta;
                neonBase += delta * GameRules.neonMult(scoreFactor()) * progress.essenceNeonBonus();
            }

            addParticle(px + (rnd.nextFloat() - 0.5f) * 60f, py + (rnd.nextFloat() - 0.5f) * 60f,
                    (rnd.nextFloat() - 0.5f) * 40f, -90f, 0.35f, 6.5f, C_GOLD, 0f, 0);

            if (scoreRollupTimer <= 0f) {
                cleanBossState();
                invuln = 2.2f;
                spawnPauseTimer = 1.2f;
                bossSlowMoTimer = 0f;
            }
        }
    }

    void hitLaser() {
        if (invuln > 0f || phaseTimer > 0f || state != ST_PLAY) {
            return;
        }
        if (shieldCharges > 0) {
            shieldCharges--;
            if (countsPersistentProgress()) {
                progress.shieldBlocks++;
            }
            invuln = 1.3f + progress.shieldInvulnBonus();
            explode(px, py, 24, C_MINT);
            playSound(sndPower, 0.85f);
            toast(sShieldBlocked);
            checkProgress();
            return;
        }
        if (zenMode) {
            combo = 0;
            comboTimer = 0f;
            comboPop = 0f;
            softenMultipliersAfterHit();
            shake = 20f;
            flash = 0.5f;
            invuln = 1.4f;
            explode(px, py, 14, C_CYAN);
            playSound(sndHit, 1.1f);
            buzz(25);
            return;
        }
        combo = 0;
        comboTimer = 0f;
        comboPop = 0f;
        softenMultipliersAfterHit();
        lives--;
        hitStop = 0.08f;
        invuln = 1.6f;
        shake = 30f;
        flash = 0.6f;
        explode(px, py, 28, C_RED);
        playSound(sndHit, 1.0f);
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

    void hitBossBullet(BossBullet bullet) {
        if (invuln > 0f || phaseTimer > 0f || state != ST_PLAY) {
            return;
        }
        if (shieldCharges > 0) {
            shieldCharges--;
            if (countsPersistentProgress()) {
                progress.shieldBlocks++;
            }
            invuln = 1.2f + progress.shieldInvulnBonus();
            explode(px, py, 22, C_MINT);
            playSound(sndPower, 0.85f);
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
            explode(bullet.x, bullet.y, 10, bullet.color);
            playSound(sndHit, 1.2f);
            buzz(22);
            return;
        }
        combo = 0;
        comboTimer = 0f;
        comboPop = 0f;
        softenMultipliersAfterHit();
        lives--;
        invuln = 1.7f;
        shake = 30f;
        flash = 1f;
        explode(px, py, 18, C_CYAN);
        explode(bullet.x, bullet.y, 12, bullet.color);
        playSound(sndHit, 0.9f);
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

    void updateParticles(float dt) {
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

    void updateShownScore() {
        int s = (int) score;
        if (s != shownScore) {
            shownScore = s;
            shownScoreText = fmt(s);
        }
    }
}
