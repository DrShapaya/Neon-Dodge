package com.isaev.neondodge;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
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
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
                || state == ST_HISTORY || state == ST_WORKSHOP) {
            py = groundY();
            targetY = py;
        }
    }

    void rebuildScrim() {
        float h = Math.max(240f, insetTop + 220f);
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
        // The emulator's software GPU cannot sustain a 60 Hz animated menu
        // with large Canvas surfaces. Throttle non-game screens to 10 fps so
        // input events keep a responsive main thread while the game itself
        // remains frame driven.
        long minFrameNs = isInteractiveMenuState()
                ? 100_000_000L : 16_000_000L;
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

    boolean isInteractiveMenuState() {
        return state == ST_MENU || state == ST_SETTINGS || state == ST_STATS
                || state == ST_QUESTS || state == ST_SKINS || state == ST_HISTORY
                || state == ST_WORKSHOP || state == ST_RULES || state == ST_PAUSE
                || state == ST_OVER;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAudioInit();
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
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
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
        if (bombTimer > 0f) {
            bombTimer -= dt;
            if (bombTimer <= 0f) {
                resolveBomb();
            }
        }

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
            progress.saveDaily();
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
        float pullLen = (float) Math.sqrt(pullX * pullX + pullY * pullY);
        if (pullLen > 132f) {
            pullX *= 132f / pullLen;
            pullY *= 132f / pullLen;
        }
        px += (targetX + pullX - px) * k;
        py += (targetY + pullY - py) * k;
    }

    float slowFactor() {
        return slowTimer > 0f ? SLOW_SCALE : 1f;
    }

    float scoreFactor() {
        return scoreMult * closeMult * bossMult
                * (hardcoreMode ? HARDCORE_MULT : 1f)
                * (doubleTimer > 0f ? 2f : 1f);
    }

    void updateObstacles(float dt) {
        float sdt = dt * slowFactor();
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obs o = obstacles.get(i);

            if (o.boss) {
                float stopY = Math.max(190f, insetTop + 190f);
                o.vy = o.y < stopY ? o.baseVy : 0f;
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
            o.warnTimer = Math.max(0f, o.warnTimer - dt);
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
                    if (!hintComboShown) {
                        hintComboShown = true;
                        prefs.edit().putBoolean("hint_combo", true).apply();
                        toast(sHintCombo);
                    }
                    closeMult = Math.min(999f, closeMult + MULT_STEP);
                    runMaxCloseMult = Math.max(runMaxCloseMult, closeMult);
                    float comboPoints = 5f * combo * scoreFactor();
                    score += comboPoints;
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

            if (invuln <= 0f && state == ST_PLAY) {
                float rr = o.r * 0.80f + PLAYER_R * 0.80f;
                if (dist2(o.x, o.y, px, py) < rr * rr) {
                    // Броня выдерживает удар щита: щит спасает сферу,
                    // но сам астероид продолжает падать.
                    boolean survivesShield = (o.type == T_ARMOR || o.boss)
                            && shieldCharges > 0 && !zenMode;
                    if (o.type == T_ARMOR && o.armorHits > 1 && !o.boss) {
                        o.armorHits = GameRules.nextArmorHits(o.armorHits);
                        o.bounceFlash = 1f;
                        explode(o.x, o.y, 16, o.color);
                        invuln = 0.55f;
                        shake = 14f;
                        flash = 0.12f;
                        toast("БРОНЯ · ТРЕЩИНА");
                        continue;
                    }
                    if (o.boss) {
                        // The boss is defeated by zones, not by ramming it.
                        hit(o);
                        continue;
                    }
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

    void updatePickups(float dt) {
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

    /** Multi-stage boss encounter: four capture zones and a readable bullet pattern. */
    void updateBossMechanics(float dt) {
        Obs boss = null;
        for (int i = 0; i < obstacles.size(); i++) {
            if (obstacles.get(i).boss) {
                boss = obstacles.get(i);
                break;
            }
        }
        if (boss == null) {
            bossZone = null;
            bossBullets.clear();
            return;
        }

        float stopY = Math.max(190f, insetTop + 190f);
        if (boss.y < stopY) {
            return;
        }

        boss.bossShotTimer -= dt;
        if (boss.bossShotTimer <= 0f) {
            BossBullet bullet = new BossBullet();
            bullet.x = boss.x;
            bullet.y = boss.y + boss.r * 0.65f;
            bullet.vx = (px - bullet.x) * 0.12f;
            bullet.vy = 340f + boss.bossStage * 42f;
            bullet.r = 18f + boss.bossStage * 2f;
            bullet.color = boss.color;
            bossBullets.add(bullet);
            boss.bossShotTimer = Math.max(0.72f, 1.35f - boss.bossStage * 0.12f);
        }

        for (int i = bossBullets.size() - 1; i >= 0; i--) {
            BossBullet bullet = bossBullets.get(i);
            bullet.x += bullet.vx * dt;
            bullet.y += bullet.vy * dt;
            if (bullet.x < bullet.r || bullet.x > W - bullet.r) {
                bullet.vx = -bullet.vx;
                bullet.x = clamp(bullet.x, bullet.r, W - bullet.r);
            }
            if (bullet.y - bullet.r > worldH + 40f) {
                bossBullets.remove(i);
                continue;
            }
            if (invuln <= 0f && dist2(bullet.x, bullet.y, px, py)
                    < (bullet.r + PLAYER_R) * (bullet.r + PLAYER_R)) {
                bossBullets.remove(i);
                hitBossBullet(bullet);
            }
        }

        if (bossZone == null && bossZonesCompleted < 4) {
            bossZoneTimer -= dt;
            if (bossZoneTimer <= 0f) {
                bossZone = new BossZone();
                bossZone.x = 180f + gameRnd.nextFloat() * (W - 360f);
                bossZone.y = Math.max(430f, worldH * (0.30f + gameRnd.nextFloat() * 0.30f));
                bossZone.fill = 0f;
                bossZoneTimer = 0.65f;
                toast(sBossZone + " " + (bossZonesCompleted + 1) + "/4");
            }
        }
        if (bossZone != null) {
            boolean inside = dist2(px, py, bossZone.x, bossZone.y)
                    <= (BOSS_ZONE_RADIUS + PLAYER_R) * (BOSS_ZONE_RADIUS + PLAYER_R);
            bossZone.fill += (inside ? dt / BOSS_ZONE_FILL_TIME
                    : -dt / (BOSS_ZONE_FILL_TIME * 0.70f));
            bossZone.fill = clamp(bossZone.fill, 0f, 1f);
            if (bossZone.fill >= 1f) {
                bossZonesCompleted++;
                explode(bossZone.x, bossZone.y, 24, C_CYAN);
                addScorePop(bossZone.x, bossZone.y - 45f, "ZONE +250", C_CYAN);
                score += 250f * scoreFactor();
                bossZone = null;
                bossZoneTimer = 0.75f;
                toast(sBossZone + " " + bossZonesCompleted + "/4");
                if (bossZonesCompleted >= 4) {
                    bossMult = BOSS_MULT;
                    for (int i = obstacles.size() - 1; i >= 0; i--) {
                        if (obstacles.get(i).boss) {
                            explode(obstacles.get(i).x, obstacles.get(i).y, 55, obstacles.get(i).color);
                            obstacles.remove(i);
                        }
                    }
                    bossBullets.clear();
                    score += 1000f * scoreFactor();
                    addScorePop(px, py - 100f, "BOSS x2", C_GOLD);
                    toast(sBossReward);
                }
            }
        }
    }

    void hitBossBullet(BossBullet bullet) {
        if (shieldCharges > 0) {
            shieldCharges = 0;
            progress.shieldBlocks++;
            invuln = 1.2f;
            explode(px, py, 22, C_MINT);
            playSound(sndPower, 0.85f);
            toast(sShieldBlocked);
            checkProgress();
            return;
        }
        if (phaseTimer > 0f) {
            return;
        }
        combo = 0;
        comboTimer = 0f;
        resetMultipliers();
        lives--;
        invuln = 1.7f;
        shake = 30f;
        flash = 1f;
        explode(px, py, 18, C_CYAN);
        explode(bullet.x, bullet.y, 12, bullet.color);
        playSound(sndHit, 0.9f);
        buzz(45);
        if (lives <= 0) {
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
