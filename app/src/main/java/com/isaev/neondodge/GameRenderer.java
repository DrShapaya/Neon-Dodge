package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/** Отрисовка мира: фон, звёзды, игрок, астероиды, бонусы, частицы. */
abstract class GameRenderer extends GameLoop {

    GameRenderer(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (scale <= 0f || worldH <= 0f) {
            return;
        }
        c.save();
        c.translate(viewOffsetX, 0f);
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
                || state == ST_STATS || state == ST_SKINS
                || state == ST_HISTORY || state == ST_WORKSHOP || state == ST_PROFILE
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
            drawBossHpBar(c);
        }
        if (state == ST_MENU) {
            drawMenu(c);
        } else if (state == ST_SETTINGS) {
            drawSettings(c);
        } else if (state == ST_PROFILE) {
            drawProfile(c);
        } else if (state == ST_STATS) {
            drawStats(c);
        } else if (state == ST_HISTORY) {
            drawHistory(c);
        } else if (state == ST_SKINS) {
            drawSkins(c);
        } else if (state == ST_WORKSHOP) {
            drawWorkshop(c);
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

    void drawStars(Canvas c) {
        for (int i = 0; i < starX.length; i++) {
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * 1.6f + starPh[i]));
            pFill.setColor(0xFFDDF2FF);
            pFill.setAlpha((int) (190f * tw));
            c.drawCircle(starX[i], starY[i], starSize[i], pFill);
        }
    }

    void drawWorld(Canvas c) {
        drawBossTelegraphs(c);
        drawBossConduits(c);
        drawBossBullets(c);
        drawBossLaser(c);
        for (int i = 0; i < pickups.size(); i++) {
            drawPickup(c, pickups.get(i));
        }
        for (int i = 0; i < obstacles.size(); i++) {
            drawObs(c, obstacles.get(i));
        }
        drawParticles(c);
        drawBombWave(c);
        drawEssenceHarvest(c);
        drawScorePops(c);
        if (state != ST_OVER) {
            drawPlayer(c, px, py);
        }
    }

    void drawBombWave(Canvas c) {
        if (bombWaveTimer <= 0f) {
            return;
        }
        float progress = 1f - clamp(bombWaveTimer / BOMB_WAVE_DURATION, 0f, 1f);
        float maxR = Math.max(W, worldH) * 1.35f;
        float curR = progress * maxR;
        float a = 1f - progress;

        strokeStyle(C_ORANGE, 18f * (1f - progress * 0.5f));
        pStroke.setAlpha((int) (170f * a));
        c.drawCircle(bombWaveOriginX, bombWaveOriginY, curR, pStroke);

        strokeStyle(0xFFFFFFFF, 6f * (1f - progress * 0.4f));
        pStroke.setAlpha((int) (240f * a));
        c.drawCircle(bombWaveOriginX, bombWaveOriginY, curR, pStroke);

        if (curR > 40f) {
            strokeStyle(C_GOLD, 3f);
            pStroke.setAlpha((int) (130f * a));
            c.drawCircle(bombWaveOriginX, bombWaveOriginY, curR * 0.92f, pStroke);
        }
        strokeReset();
    }

    void drawBossHpBar(Canvas c) {
        if (!bossActive && bossDeathPhase == 0) {
            return;
        }
        float barY = insetTop + 330f;
        float barW = 460f;
        float barH = 14f;
        float left = W / 2f - barW / 2f;

        String name = bossAnnounceName.isEmpty() ? getBossName(bossKind) : bossAnnounceName;
        textC(c, name, W / 2f, barY - 14f, 22f, C_GOLD, tfDisplay, Paint.Align.CENTER);

        box(c, left - 4f, barY - 2f, left + barW + 4f, barY + barH + 2f, 6f, 0x44000000, 0x66FFFFFF);

        float segGap = 6f;
        float segW = (barW - segGap * (bossMaxHp - 1)) / bossMaxHp;

        for (int i = 0; i < bossMaxHp; i++) {
            float sx = left + i * (segW + segGap);
            boolean filled = (i < bossHp);
            int segCol = filled ? BOSS_COLORS[bossKind % BOSS_COLORS.length] : 0x33FF4F6A;
            int segBorder = filled ? 0xFFFFFFFF : 0x55FF4F6A;

            box(c, sx, barY, sx + segW, barY + barH, 4f,
                    filled ? withAlpha(segCol, 190) : segCol, segBorder);

            if (filled) {
                pFill.setColor(0x88FFFFFF);
                c.drawRect(sx + 3f, barY + 2f, sx + segW - 3f, barY + 5f, pFill);
            }
        }
    }

    void drawBossTelegraphs(Canvas c) {
        // 1. Прицельная линия лазера (Prism)
        if (bossLaserAimTimer > 0f) {
            float a = 0.5f + 0.5f * (float) Math.sin(time * 20f);
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setStrokeWidth(4f);
            pStroke.setColor(C_CYAN);
            pStroke.setAlpha((int) (190f * a));
            c.drawLine(bossLaserX, bossLaserY, bossLaserX, worldH, pStroke);

            pFill.setColor(0x155FF3FF);
            c.drawRect(bossLaserX - bossLaserWidth, bossLaserY,
                    bossLaserX + bossLaserWidth, worldH, pFill);
            pStroke.setStyle(Paint.Style.FILL);
        }

        // 2. Предупреждающая полоса тарана (Striker)
        if (bossDashWarnTimer > 0f) {
            float a = 0.5f + 0.5f * (float) Math.sin(time * 24f);
            float laneW = 140f;
            float topY = bossStopY();
            pFill.setColor(withAlpha(C_ORANGE, (int) (40f * a)));
            c.drawRect(bossDashX - laneW / 2f, topY, bossDashX + laneW / 2f, worldH, pFill);

            strokeStyle(C_ORANGE, 4f);
            pStroke.setAlpha((int) (200f * a));
            c.drawLine(bossDashX - laneW / 2f, topY, bossDashX - laneW / 2f, worldH, pStroke);
            c.drawLine(bossDashX + laneW / 2f, topY, bossDashX + laneW / 2f, worldH, pStroke);
            strokeReset();

            float arrowY = ((time * 600f) % (worldH - topY - 100f)) + topY + 40f;
            textC(c, "▼ ▼ ▼", bossDashX, arrowY, 28f, C_ORANGE, tfBlack, Paint.Align.CENTER);
        }
    }

    void drawBossLaser(Canvas c) {
        if (bossLaserFireTimer > 0f) {
            float laserW = bossLaserWidth * 2f;
            pFill.setColor(0x445FF3FF);
            c.drawRect(bossLaserX - laserW * 1.5f, bossLaserY,
                    bossLaserX + laserW * 1.5f, worldH, pFill);

            pFill.setColor(C_CYAN);
            c.drawRect(bossLaserX - laserW, bossLaserY,
                    bossLaserX + laserW, worldH, pFill);

            pFill.setColor(0xFFFFFFFF);
            c.drawRect(bossLaserX - laserW * 0.4f, bossLaserY,
                    bossLaserX + laserW * 0.4f, worldH, pFill);
        }
    }

    void drawBossConduits(Canvas c) {
        if (bossConduits.isEmpty()) {
            return;
        }

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(3.5f);
        pStroke.setColor(0x885FF3FF);
        for (int i = 0; i < bossConduits.size() - 1; i++) {
            BossConduit c1 = bossConduits.get(i);
            BossConduit c2 = bossConduits.get(i + 1);
            c.drawLine(c1.x, c1.y, c2.x, c2.y, pStroke);
        }
        strokeReset();

        for (int i = 0; i < bossConduits.size(); i++) {
            BossConduit con = bossConduits.get(i);
            float s = con.appearAnim * (1f + 0.08f * (float) Math.sin(con.pulse));
            float r = con.r * s;

            if (con.active) {
                drawGlow(c, con.x, con.y, r * 3.2f, C_MINT, 170);
                pFill.setColor(0x337CF9D0);
                c.drawCircle(con.x, con.y, r, pFill);
                strokeStyle(C_MINT, 4.5f);
                c.drawCircle(con.x, con.y, r, pStroke);
                strokeReset();
                pFill.setColor(0xFFFFFFFF);
                c.drawCircle(con.x, con.y, r * 0.35f, pFill);
            } else {
                drawGlow(c, con.x, con.y, r * 2.8f, C_CYAN, 130);
                pFill.setColor(0x225FF3FF);
                c.drawCircle(con.x, con.y, r, pFill);
                strokeStyle(C_CYAN, 3.5f);
                c.drawCircle(con.x, con.y, r, pStroke);
                strokeReset();

                hexPath.reset();
                hexPath.moveTo(con.x, con.y - r * 0.55f);
                hexPath.lineTo(con.x + r * 0.55f, con.y);
                hexPath.lineTo(con.x, con.y + r * 0.55f);
                hexPath.lineTo(con.x - r * 0.55f, con.y);
                hexPath.close();
                pFill.setColor(0xFFFFFFFF);
                c.drawPath(hexPath, pFill);

                textC(c, String.valueOf(i + 1), con.x, con.y, 22f, UI_INK, tfBlack, Paint.Align.CENTER);
            }
        }

        if (conduitArcTimer > 0f) {
            float progress = conduitArcTimer / 0.65f;
            float targetX = W / 2f;
            float targetY = insetTop + 210f;
            for (int i = 0; i < obstacles.size(); i++) {
                if (obstacles.get(i).boss) {
                    targetX = obstacles.get(i).x;
                    targetY = obstacles.get(i).y;
                    break;
                }
            }
            strokeStyle(0xFFFFFFFF, 8f * progress);
            c.drawLine(conduitArcX, conduitArcY, targetX, targetY, pStroke);
            strokeStyle(C_GOLD, 16f * progress);
            pStroke.setAlpha((int) (220f * progress));
            c.drawLine(conduitArcX, conduitArcY, targetX, targetY, pStroke);
            strokeReset();
        }
    }

    void drawBossBullets(Canvas c) {
        for (int i = 0; i < bossBullets.size(); i++) {
            BossBullet b = bossBullets.get(i);
            if (b.reflected) {
                drawGlow(c, b.x, b.y, b.r * 4.5f, C_MINT, 190);
                pFill.setColor(C_MINT);
                c.drawCircle(b.x, b.y, b.r, pFill);
                pFill.setColor(0xFFFFFFFF);
                c.drawCircle(b.x, b.y, b.r * 0.5f, pFill);
            } else if (b.kind == BULLET_REFLECTABLE) {
                float pulse = 0.88f + 0.12f * (float) Math.sin(time * 8f);
                drawGlow(c, b.x, b.y, b.r * 3.8f * pulse, C_GOLD, 180);
                pFill.setColor(C_GOLD);
                c.drawCircle(b.x, b.y, b.r, pFill);
                pFill.setColor(0xFFFFFFFF);
                c.drawCircle(b.x, b.y, b.r * 0.45f, pFill);
            } else if (b.kind == BULLET_SPIRAL) {
                drawGlow(c, b.x, b.y, b.r * 3.2f, b.color, 150);
                pFill.setColor(b.color);
                c.drawCircle(b.x, b.y, b.r, pFill);
                pFill.setColor(0xFFFFFFFF);
                c.drawCircle(b.x, b.y, b.r * 0.35f, pFill);
            } else {
                drawGlow(c, b.x, b.y, b.r * 3.8f, b.color, 145);
                pFill.setColor(b.color);
                c.drawCircle(b.x, b.y, b.r, pFill);
                pFill.setColor(0xFFFFFFFF);
                c.drawCircle(b.x - b.r * 0.25f, b.y - b.r * 0.25f, b.r * 0.28f, pFill);
            }
        }
    }

    void drawEssenceHarvest(Canvas c) {
        if (bossDeathPhase < 2) {
            return;
        }

        for (int i = 0; i < essenceOrbs.size(); i++) {
            EssenceOrb orb = essenceOrbs.get(i);
            if (orb.absorbed) continue;
            drawGlow(c, orb.x, orb.y, orb.r * 4.0f, orb.color, 190);
            pFill.setColor(orb.color);
            c.drawCircle(orb.x, orb.y, orb.r, pFill);
            pFill.setColor(0xFFFFFFFF);
            c.drawCircle(orb.x, orb.y, orb.r * 0.45f, pFill);
        }

        if (playerAbsorptionPulse > 0.01f) {
            float waveR = PLAYER_R + (1f - playerAbsorptionPulse) * 140f;
            strokeStyle(C_GOLD, 6f * playerAbsorptionPulse);
            pStroke.setAlpha((int) (220f * playerAbsorptionPulse));
            c.drawCircle(px, py, waveR, pStroke);

            strokeStyle(C_CYAN, 3.5f * playerAbsorptionPulse);
            pStroke.setAlpha((int) (180f * playerAbsorptionPulse));
            c.drawCircle(px, py, waveR * 0.7f, pStroke);
            strokeReset();
        }

        if (bossDeathPhase == 3) {
            float midY = worldH * 0.38f;
            textGlow(c, sBossEssenceHarvested, W / 2f, midY, 46f,
                    0xFFFFFFFF, 0xFFFFD75E, tfDisplay);

            String scoreText = "+" + fmt((int) scoreRollupCurrent);
            textGlow(c, scoreText, W / 2f, midY + 68f, 62f,
                    C_GOLD, 0xAAFFD75E, tfDisplayBold);
        }
    }

    void drawParticles(Canvas c) {
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            float lr = clamp(p.life / p.maxLife, 0f, 1f);
            int baseA = Color.alpha(p.color);
            int alpha = (int) (baseA * lr);
            if (p.style == 1) {
                // Расширяющееся кольцо-импульс
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setColor(p.color);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(3.5f * lr);
                float curR = p.size * (0.8f + (1f - lr) * 2.2f);
                c.drawCircle(p.x, p.y, curR, pStroke);
                pStroke.setStyle(Paint.Style.FILL);
            } else if (p.style == 2) {
                // Искрящийся штрих-след
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setColor(p.color);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(2.8f * lr);
                c.drawLine(p.x, p.y, p.x - p.vx * 0.05f, p.y - p.vy * 0.05f, pStroke);
                pStroke.setStyle(Paint.Style.FILL);
            } else if (p.style == 3) {
                // Мягкое полупрозрачное эхо
                pFill.setColor(p.color);
                pFill.setAlpha((int) (alpha * 0.45f));
                c.drawCircle(p.x, p.y, p.size * (0.85f + 0.15f * lr), pFill);
            } else {
                pFill.setColor(p.color);
                pFill.setAlpha(alpha);
                c.drawCircle(p.x, p.y, p.size * (0.4f + 0.6f * lr), pFill);
            }
        }
    }

    void drawPlayer(Canvas c, float x, float y) {
        float a = 1f;
        if (invuln > 0f && state == ST_PLAY) {
            a = ((int) (invuln * 14f) % 2 == 0) ? 0.3f : 1f;
        }

        int si = clamp(progress.sphereSkin, 0, Progress.SPHERE_COUNT - 1);
        int outer = SPHERE_OUTER[si];
        int inner = SPHERE_INNER[si];


        drawGlow(c, x, y, 260f, outer, (int) (165f * a));

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(outer);
        pStroke.setAlpha((int) (110f * a));
        pStroke.setStrokeWidth(3f);
        c.drawCircle(x, y, PLAYER_R + 8f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        pFill.setColor(outer);
        pFill.setAlpha((int) (255f * a));
        c.drawCircle(x, y, PLAYER_R, pFill);

        pFill.setColor(inner);
        c.drawCircle(x, y, PLAYER_R * 0.65f, pFill);

        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(x, y, PLAYER_R * 0.28f, pFill);

        // Уникальный визуальный стиль для каждого скина сферы
        pStroke.setStyle(Paint.Style.STROKE);
        switch (si) {
            case 0: { // Циан: двойные орбитальные квантовые кольца со спутниками
                float rAngle = time * 85f;
                strokeStyle(outer, 2.5f);
                pStroke.setAlpha((int) (140f * a));
                c.save();
                c.rotate(rAngle, x, y);
                tmpRect.set(x - PLAYER_R - 12f, y - PLAYER_R * 0.5f, x + PLAYER_R + 12f, y + PLAYER_R * 0.5f);
                c.drawOval(tmpRect, pStroke);
                c.rotate(90f, x, y);
                c.drawOval(tmpRect, pStroke);
                c.restore();
                strokeReset();
                for (int k = 0; k < 2; k++) {
                    double ang = Math.toRadians(rAngle + k * 180f);
                    float sx = x + (float) Math.cos(ang) * (PLAYER_R + 12f);
                    float sy = y + (float) Math.sin(ang) * (PLAYER_R * 0.5f);
                    pFill.setColor(0xFFFFFFFF);
                    c.drawCircle(sx, sy, 3.5f, pFill);
                }
                break;
            }
            case 1: { // Магента: вращающийся кибер-шестиугольник
                float rot = time * 45f;
                hexPath.reset();
                for (int k = 0; k < 6; k++) {
                    double ang = Math.toRadians(rot + k * 60f);
                    float hx = x + (float) Math.cos(ang) * (PLAYER_R + 14f);
                    float hy = y + (float) Math.sin(ang) * (PLAYER_R + 14f);
                    if (k == 0) hexPath.moveTo(hx, hy);
                    else hexPath.lineTo(hx, hy);
                }
                hexPath.close();
                strokeStyle(outer, 3.5f);
                pStroke.setAlpha((int) (180f * a));
                c.drawPath(hexPath, pStroke);
                strokeReset();
                break;
            }
            case 2: { // Золото: 8 лучей солнечной короны
                float rot = time * 30f;
                strokeStyle(outer, 2.5f);
                pStroke.setAlpha((int) (170f * a));
                for (int k = 0; k < 8; k++) {
                    double ang = Math.toRadians(rot + k * 45f);
                    float ca = (float) Math.cos(ang);
                    float sa = (float) Math.sin(ang);
                    c.drawLine(x + ca * (PLAYER_R + 4f), y + sa * (PLAYER_R + 4f),
                            x + ca * (PLAYER_R + 16f + (k % 2 == 0 ? 8f : 2f)),
                            y + sa * (PLAYER_R + 16f + (k % 2 == 0 ? 8f : 2f)), pStroke);
                }
                strokeReset();
                break;
            }
            case 3: { // Кислота: био-плазменные вращающиеся шипы
                float rot = -time * 70f;
                strokeStyle(outer, 3.5f);
                pStroke.setAlpha((int) (190f * a));
                for (int k = 0; k < 6; k++) {
                    double ang = Math.toRadians(rot + k * 60f);
                    float ca = (float) Math.cos(ang);
                    float sa = (float) Math.sin(ang);
                    c.drawLine(x + ca * (PLAYER_R + 3f), y + sa * (PLAYER_R + 3f),
                            x + ca * (PLAYER_R + 16f), y + sa * (PLAYER_R + 16f), pStroke);
                }
                strokeReset();
                break;
            }
            case 4: { // Пульсар: двойные электромагнитные спиральные дуги
                float rot = time * 110f;
                strokeStyle(outer, 3f);
                pStroke.setAlpha((int) (180f * a));
                tmpRect.set(x - PLAYER_R - 15f, y - PLAYER_R - 15f, x + PLAYER_R + 15f, y + PLAYER_R + 15f);
                c.drawArc(tmpRect, rot, 70f, false, pStroke);
                c.drawArc(tmpRect, rot + 180f, 70f, false, pStroke);
                strokeReset();
                break;
            }
            case 5: { // Призрак: двойные пульсирующие полупрозрачные волны
                float wave1 = 0.5f + 0.5f * (float) Math.sin(time * 3f);
                float wave2 = 0.5f + 0.5f * (float) Math.sin(time * 3f + 1.5f);
                strokeStyle(0x66FFFFFF, 2.5f);
                c.drawCircle(x, y, PLAYER_R + 10f + 8f * wave1, pStroke);
                strokeStyle(0x33A4D8FF, 2f);
                c.drawCircle(x, y, PLAYER_R + 16f + 12f * wave2, pStroke);
                strokeReset();
                break;
            }
            case 6: { // Закат: двойная яркая солнечная корона
                float pulse = 0.8f + 0.2f * (float) Math.sin(time * 4f);
                strokeStyle(0xFFFF9E4F, 4f);
                pStroke.setAlpha((int) (190f * a));
                c.drawCircle(x, y, PLAYER_R + 12f + 3f * pulse, pStroke);
                strokeStyle(0xFFFF4F4F, 2.5f);
                pStroke.setAlpha((int) (130f * a));
                c.drawCircle(x, y, PLAYER_R + 20f + 5f * pulse, pStroke);
                strokeReset();
                break;
            }
            case 7: { // Аква: вращающиеся кристаллы и ледяное кольцо
                float rot = time * 50f;
                strokeStyle(outer, 2.5f);
                pStroke.setAlpha((int) (170f * a));
                for (int k = 0; k < 4; k++) {
                    double ang = Math.toRadians(rot + k * 90f);
                    float cx2 = x + (float) Math.cos(ang) * (PLAYER_R + 14f);
                    float cy2 = y + (float) Math.sin(ang) * (PLAYER_R + 14f);
                    pFill.setColor(0xFFFFFFFF);
                    c.drawCircle(cx2, cy2, 3f, pFill);
                }
                c.drawCircle(x, y, PLAYER_R + 14f, pStroke);
                strokeReset();
                break;
            }
        }
        pStroke.setStyle(Paint.Style.FILL);

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
            if (shieldCharges > 1) {
                pStroke.setColor(C_CYAN);
                pStroke.setAlpha((int) (150f * pulse));
                pStroke.setStrokeWidth(2.2f);
                c.drawCircle(x, y, PLAYER_R + 39f, pStroke);
            }
            pStroke.setStyle(Paint.Style.FILL);
            int motes = shieldCharges > 1 ? 8 : 4;
            for (int i = 0; i < motes; i++) {
                double ang = Math.toRadians(time * (shieldCharges > 1 ? -120f : 90f) + i * (360f / motes));
                pFill.setColor(i % 2 == 0 ? C_MINT : C_CYAN);
                pFill.setAlpha(210);
                float dist = shieldCharges > 1 ? (PLAYER_R + 33f) : (PLAYER_R + 27f);
                c.drawCircle(x + (float) Math.cos(ang) * dist,
                        y + (float) Math.sin(ang) * dist, 3.4f, pFill);
            }
        }
    }

    void drawObs(Canvas c, Obs o) {
        if (o.warnTimer > 0f && o.y < 80f) {
            float a = clamp(o.warnTimer / 0.85f, 0f, 1f);
            float wy = insetTop + 92f;
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(o.color);
            pStroke.setAlpha((int) (220f * a));
            pStroke.setStrokeWidth(4f);
            c.drawLine(o.x, wy - 25f, o.x, wy + 18f, pStroke);
            c.drawLine(o.x, wy + 18f, o.x - 10f, wy + 4f, pStroke);
            c.drawLine(o.x, wy + 18f, o.x + 10f, wy + 4f, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
        }
        if (o.boss) {
            float bx = o.x + bossWobble;
            float by = o.y;
            float pulse = 0.88f + 0.12f * (float) Math.sin(time * 5f);
            int bossCol = (bossStunTimer > 0f) ? 0xFFFFFFFF : o.color;

            drawGlow(c, bx, by, o.r * 3.8f, bossCol, 200);

            switch (bossKind) {
                case BOSS_KIND_PRISM:
                    c.save();
                    c.translate(bx, by);
                    c.rotate(o.rot);
                    hexPath.reset();
                    hexPath.moveTo(0f, -o.r * pulse);
                    hexPath.lineTo(o.r * 0.85f * pulse, 0f);
                    hexPath.lineTo(0f, o.r * pulse);
                    hexPath.lineTo(-o.r * 0.85f * pulse, 0f);
                    hexPath.close();
                    pFill.setColor(withAlpha(bossCol, 190));
                    c.drawPath(hexPath, pFill);
                    strokeStyle(0xFFFFFFFF, 5f);
                    c.drawPath(hexPath, pStroke);

                    c.rotate(-o.rot * 2.2f);
                    hexPath.reset();
                    hexPath.moveTo(0f, -o.r * 0.45f);
                    hexPath.lineTo(o.r * 0.45f, 0f);
                    hexPath.lineTo(0f, o.r * 0.45f);
                    hexPath.lineTo(-o.r * 0.45f, 0f);
                    hexPath.close();
                    pFill.setColor(0xFFFFFFFF);
                    c.drawPath(hexPath, pFill);
                    strokeReset();
                    c.restore();
                    break;

                case BOSS_KIND_VORTEX:
                    pFill.setColor(0xFF140A28);
                    c.drawCircle(bx, by, o.r * 0.65f, pFill);
                    strokeStyle(bossCol, 6f);
                    c.drawCircle(bx, by, o.r * 0.65f, pStroke);
                    c.save();
                    c.translate(bx, by);
                    c.rotate(time * 90f);
                    strokeStyle(0xFFA46BFF, 4f);
                    for (int a = 0; a < 4; a++) {
                        tmpRect.set(-o.r * 0.95f, -o.r * 0.95f, o.r * 0.95f, o.r * 0.95f);
                        c.drawArc(tmpRect, a * 90f + 10f, 70f, false, pStroke);
                    }
                    strokeReset();
                    c.restore();
                    break;

                case BOSS_KIND_STRIKER:
                    c.save();
                    c.translate(bx, by);
                    hexPath.reset();
                    hexPath.moveTo(0f, o.r * 0.9f);
                    hexPath.lineTo(o.r * 0.95f, -o.r * 0.7f);
                    hexPath.lineTo(0f, -o.r * 0.35f);
                    hexPath.lineTo(-o.r * 0.95f, -o.r * 0.7f);
                    hexPath.close();
                    pFill.setColor(withAlpha(bossCol, 210));
                    c.drawPath(hexPath, pFill);
                    strokeStyle(0xFFFFFFFF, 5f);
                    c.drawPath(hexPath, pStroke);

                    pFill.setColor(C_GOLD);
                    c.drawCircle(-o.r * 0.35f, -o.r * 0.45f, 12f * pulse, pFill);
                    c.drawCircle(o.r * 0.35f, -o.r * 0.45f, 12f * pulse, pFill);
                    strokeReset();
                    c.restore();
                    break;

                case BOSS_KIND_NEXUS:
                default:
                    drawHex(c, bx, by, o.r * pulse, o.rot, bossCol);
                    pStroke.setStyle(Paint.Style.STROKE);
                    pStroke.setColor(0xFFFFFFFF);
                    pStroke.setStrokeWidth(5f);
                    c.drawCircle(bx, by, o.r * 0.52f, pStroke);
                    c.drawCircle(bx, by, o.r * 0.25f, pStroke);
                    strokeReset();
                    break;
            }
            return;
        }
        switch (o.type) {
            case T_GREEN:
                drawObsGreen(c, o);
                break;
            case T_BLUE:
                drawObsBlue(c, o);
                break;
            case T_VIOLET:
                drawObsViolet(c, o);
                break;
            case T_ORANGE:
                drawObsOrange(c, o);
                break;
            case T_RED:
                drawObsRed(c, o);
                break;
            case T_SPLIT:
                drawObsSplit(c, o);
                break;
            case T_MINE:
                drawObsMine(c, o);
                break;
            case T_GRAVITY:
                drawObsGravity(c, o);
                break;
            case T_ARMOR:
            default:
                drawObsArmor(c, o);
                break;
        }
    }

    /** 1. T_GREEN: Рикошет — Вращающийся 4-лопастной бритвенный сюрикен */
    void drawObsGreen(Canvas c, Obs o) {
        float bf = o.bounceFlash;
        drawGlow(c, o.x, o.y, o.r * 2.9f, o.color, 110 + (int) (130f * bf));

        c.save();
        c.translate(o.x, o.y);
        if (bf > 0f) {
            c.scale(1f + 0.30f * bf, 1f - 0.20f * bf);
        }
        c.rotate(o.rot);

        // 4-лопастной сюрикен
        obsPath.reset();
        float outerR = o.r * 1.05f;
        float innerR = o.r * 0.32f;
        for (int i = 0; i < 4; i++) {
            double tipAng = Math.toRadians(i * 90f);
            double baseAng = Math.toRadians(i * 90f + 45f);
            float tx = (float) Math.cos(tipAng) * outerR;
            float ty = (float) Math.sin(tipAng) * outerR;
            float bx = (float) Math.cos(baseAng) * innerR;
            float by = (float) Math.sin(baseAng) * innerR;
            if (i == 0) {
                obsPath.moveTo(tx, ty);
            } else {
                obsPath.lineTo(tx, ty);
            }
            obsPath.lineTo(bx, by);
        }
        obsPath.close();

        pFill.setColor(o.color);
        pFill.setAlpha(55);
        c.drawPath(obsPath, pFill);

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(5f);
        pStroke.setAlpha(240);
        c.drawPath(obsPath, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(1.8f);
        pStroke.setAlpha(bf > 0f ? 240 : 130);
        c.drawPath(obsPath, pStroke);

        // Внутренний ротор
        c.rotate(-o.rot * 2f);
        obsInnerPath.reset();
        float r2 = o.r * 0.45f;
        obsInnerPath.moveTo(0f, -r2);
        obsInnerPath.lineTo(r2, 0f);
        obsInnerPath.lineTo(0f, r2);
        obsInnerPath.lineTo(-r2, 0f);
        obsInnerPath.close();

        pFill.setColor(withAlpha(o.color, 90));
        c.drawPath(obsInnerPath, pFill);
        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2f);
        pStroke.setAlpha(180);
        c.drawPath(obsInnerPath, pStroke);

        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(0f, 0f, o.r * 0.16f, pFill);
        strokeReset();
        c.restore();
    }

    /** 2. T_BLUE: Быстрый — Сверхзвуковая кинетическая стрела / Дельта-перехватчик */
    void drawObsBlue(Canvas c, Obs o) {
        drawGlow(c, o.x, o.y, o.r * 2.7f, o.color, 120);

        c.save();
        c.translate(o.x, o.y);
        float tilt = clamp(o.vx * 0.08f, -18f, 18f);
        c.rotate(tilt);
        c.scale(0.84f, 1.24f);

        // 1. Сверхзвуковые дуги ударной волны перед носом
        float wavePhase = (time * 8f) % 1f;
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(2.5f);
        pStroke.setAlpha((int) (140f * (1f - wavePhase)));
        tmpRect.set(-o.r * 0.9f, o.r * 0.6f + wavePhase * 18f,
                o.r * 0.9f, o.r * 1.5f + wavePhase * 18f);
        c.drawArc(tmpRect, 30f, 120f, false, pStroke);

        // 2. Двойные плазменные струи двигателей
        float fl1 = o.r * 0.65f + (float) Math.sin(time * 40f) * 7f;
        float fl2 = o.r * 0.65f + (float) Math.cos(time * 35f) * 7f;
        float engX = o.r * 0.38f;
        float engY = -o.r * 0.50f;

        pStroke.setColor(0xFF1FC2FF);
        pStroke.setStrokeWidth(6f);
        pStroke.setAlpha(200);
        c.drawLine(-engX, engY, -engX, engY - fl1, pStroke);
        c.drawLine(engX, engY, engX, engY - fl2, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2.5f);
        pStroke.setAlpha(240);
        c.drawLine(-engX, engY, -engX, engY - fl1 * 0.65f, pStroke);
        c.drawLine(engX, engY, engX, engY - fl2 * 0.65f, pStroke);

        // 3. Стреловидный корпус перехватчика
        obsPath.reset();
        obsPath.moveTo(0f, o.r * 1.15f);
        obsPath.lineTo(o.r * 0.85f, -o.r * 0.65f);
        obsPath.lineTo(o.r * 0.35f, -o.r * 0.40f);
        obsPath.lineTo(0f, -o.r * 0.22f);
        obsPath.lineTo(-o.r * 0.35f, -o.r * 0.40f);
        obsPath.lineTo(-o.r * 0.85f, -o.r * 0.65f);
        obsPath.close();

        pFill.setColor(withAlpha(o.color, 60));
        c.drawPath(obsPath, pFill);

        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(5f);
        pStroke.setAlpha(240);
        c.drawPath(obsPath, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(1.8f);
        pStroke.setAlpha(160);
        c.drawPath(obsPath, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2.8f);
        pStroke.setAlpha(220);
        c.drawLine(0f, -o.r * 0.15f, 0f, o.r * 0.95f, pStroke);

        strokeReset();
        c.restore();
    }

    /** 3. T_VIOLET: Самонаводящийся — Дрон-охотник с кибернетическим глазом, следящим за игроком */
    void drawObsViolet(Canvas c, Obs o) {
        drawGlow(c, o.x, o.y, o.r * 2.8f, o.color, 115);

        c.save();
        c.translate(o.x, o.y);

        pStroke.setStyle(Paint.Style.STROKE);
        if (o.vx > 8f) {
            float jlen = Math.min(22f, o.vx * 0.28f);
            pStroke.setColor(0xFFE28FFF);
            pStroke.setStrokeWidth(4.5f);
            c.drawLine(-o.r * 0.85f, 0f, -o.r * 0.85f - jlen, 0f, pStroke);
        } else if (o.vx < -8f) {
            float jlen = Math.min(22f, -o.vx * 0.28f);
            pStroke.setColor(0xFFE28FFF);
            pStroke.setStrokeWidth(4.5f);
            c.drawLine(o.r * 0.85f, 0f, o.r * 0.85f + jlen, 0f, pStroke);
        }

        // 1. Восьмиугольный корпус
        obsPath.reset();
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45f + 22.5f);
            float rad = (i % 2 == 0) ? o.r * 0.98f : o.r * 0.85f;
            float hx = (float) Math.cos(ang) * rad;
            float hy = (float) Math.sin(ang) * rad;
            if (i == 0) obsPath.moveTo(hx, hy);
            else obsPath.lineTo(hx, hy);
        }
        obsPath.close();

        pFill.setColor(withAlpha(o.color, 50));
        c.drawPath(obsPath, pFill);

        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(5f);
        pStroke.setAlpha(235);
        c.drawPath(obsPath, pStroke);

        // 2. Внешняя оправа сенсора и засечки прицела
        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2f);
        pStroke.setAlpha(110);
        c.drawCircle(0f, 0f, o.r * 0.60f, pStroke);

        pStroke.setStrokeWidth(2.4f);
        pStroke.setAlpha(170);
        for (int k = 0; k < 4; k++) {
            double ang = Math.toRadians(k * 90f + time * 45f);
            float ca = (float) Math.cos(ang);
            float sa = (float) Math.sin(ang);
            c.drawLine(ca * o.r * 0.48f, sa * o.r * 0.48f,
                    ca * o.r * 0.64f, sa * o.r * 0.64f, pStroke);
        }

        // 3. Кибернетический глаз, следящий за игроком
        float dx = px - o.x;
        float dy = py - o.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float eyeOffset = o.r * 0.24f;
        float pupilX = dist > 1f ? (dx / dist) * eyeOffset : 0f;
        float pupilY = dist > 1f ? (dy / dist) * eyeOffset : 0f;

        pFill.setColor(0xFF38085C);
        c.drawCircle(pupilX, pupilY, o.r * 0.28f, pFill);
        pStroke.setColor(C_MAGENTA);
        pStroke.setStrokeWidth(2.5f);
        pStroke.setAlpha(220);
        c.drawCircle(pupilX, pupilY, o.r * 0.28f, pStroke);

        pFill.setColor(0xFFFF2A4B);
        c.drawCircle(pupilX, pupilY, o.r * 0.14f, pFill);
        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(pupilX - 2f, pupilY - 2f, o.r * 0.06f, pFill);

        strokeReset();
        c.restore();
    }

    /** 4. T_ORANGE: Импульсный — Тяжёлый ракетный перехватчик с мощными форсажными факелами */
    void drawObsOrange(Canvas c, Obs o) {
        boolean boosting = o.burstPhase < 0.30f;
        drawGlow(c, o.x, o.y, o.r * (boosting ? 3.2f : 2.6f), o.color, boosting ? 190 : 85);

        c.save();
        c.translate(o.x, o.y);

        if (boosting) {
            float boostT = o.burstPhase / 0.30f;
            float sy = 1.32f - 0.12f * boostT;
            float sx = 0.82f + 0.08f * boostT;
            c.scale(sx, sy);
        } else {
            c.scale(1.08f, 0.94f);
        }

        // 1. Форсажные факелы двух двигателей
        float engX = o.r * 0.44f;
        float engY = -o.r * 0.40f;
        float flameLen = boosting
                ? (o.r * 1.35f + (float) Math.sin(time * 50f) * 12f)
                : (o.r * 0.35f + (float) Math.sin(time * 20f) * 4f);

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(0xFFFF9500);
        pStroke.setStrokeWidth(boosting ? 8.5f : 4f);
        pStroke.setAlpha(boosting ? 230 : 120);
        c.drawLine(-engX, engY, -engX, engY - flameLen, pStroke);
        c.drawLine(engX, engY, engX, engY - flameLen, pStroke);

        pStroke.setColor(0xFFFFE875);
        pStroke.setStrokeWidth(boosting ? 4f : 1.8f);
        pStroke.setAlpha(boosting ? 255 : 160);
        c.drawLine(-engX, engY, -engX, engY - flameLen * 0.65f, pStroke);
        c.drawLine(engX, engY, engX, engY - flameLen * 0.65f, pStroke);

        // 2. Стреловидный тяжёлый носовой клин
        obsPath.reset();
        obsPath.moveTo(0f, o.r * 1.05f);
        obsPath.lineTo(o.r * 0.88f, o.r * 0.10f);
        obsPath.lineTo(o.r * 0.65f, -o.r * 0.55f);
        obsPath.lineTo(o.r * 0.22f, -o.r * 0.35f);
        obsPath.lineTo(0f, -o.r * 0.20f);
        obsPath.lineTo(-o.r * 0.22f, -o.r * 0.35f);
        obsPath.lineTo(-o.r * 0.65f, -o.r * 0.55f);
        obsPath.lineTo(-o.r * 0.88f, o.r * 0.10f);
        obsPath.close();

        pFill.setColor(withAlpha(o.color, boosting ? 80 : 45));
        c.drawPath(obsPath, pFill);

        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(5.5f);
        pStroke.setAlpha(240);
        c.drawPath(obsPath, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2f);
        pStroke.setAlpha(boosting ? 220 : 120);
        c.drawPath(obsPath, pStroke);

        pFill.setColor(boosting ? 0xFFFFFFFF : withAlpha(o.color, 170));
        c.drawCircle(0f, o.r * 0.25f, o.r * 0.22f, pFill);

        strokeReset();
        c.restore();
    }

    /** 5. T_RED: Титан / Колосс — Огромный шипастый астероид-джаггернаут с пылающим лавовым ядром */
    void drawObsRed(Canvas c, Obs o) {
        float pulse = 0.93f + 0.07f * (float) Math.sin(time * 5f);
        drawGlow(c, o.x, o.y, o.r * 3.1f, o.color, 135);

        c.save();
        c.translate(o.x, o.y);
        c.rotate(o.rot * 0.4f);

        // 1. Концентрическая гравитационная ударная волна
        float shockProg = (time * 1.6f) % 1f;
        float shockR = o.r * (1.1f + shockProg * 1.3f);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(C_RED);
        pStroke.setStrokeWidth(3f * (1f - shockProg));
        pStroke.setAlpha((int) (150f * (1f - shockProg)));
        c.drawCircle(0f, 0f, shockR, pStroke);

        // 2. 8-шипастый монолитный бастион
        obsPath.reset();
        for (int i = 0; i < 8; i++) {
            double tipAng = Math.toRadians(i * 45f);
            double baseAng = Math.toRadians(i * 45f + 22.5f);
            float tx = (float) Math.cos(tipAng) * o.r * 1.05f * pulse;
            float ty = (float) Math.sin(tipAng) * o.r * 1.05f * pulse;
            float bx = (float) Math.cos(baseAng) * o.r * 0.74f;
            float by = (float) Math.sin(baseAng) * o.r * 0.74f;
            if (i == 0) obsPath.moveTo(tx, ty);
            else obsPath.lineTo(tx, ty);
            obsPath.lineTo(bx, by);
        }
        obsPath.close();

        pFill.setColor(0x35FF2A4B);
        c.drawPath(obsPath, pFill);

        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(7.5f);
        pStroke.setAlpha(245);
        c.drawPath(obsPath, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2f);
        pStroke.setAlpha(90);
        c.drawPath(obsPath, pStroke);

        // 3. Радиальные раскалённые трещины/жилы
        pStroke.setColor(0xFFFF851B);
        pStroke.setStrokeWidth(3.5f);
        pStroke.setAlpha(200);
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45f);
            float ca = (float) Math.cos(ang);
            float sa = (float) Math.sin(ang);
            c.drawLine(ca * o.r * 0.35f, sa * o.r * 0.35f,
                    ca * o.r * 0.95f, sa * o.r * 0.95f, pStroke);
        }

        // 4. Пылающее лавовое ядро
        pFill.setColor(0xFFFF3355);
        c.drawCircle(0f, 0f, o.r * 0.40f * pulse, pFill);
        pFill.setColor(0xFFFFE875);
        c.drawCircle(0f, 0f, o.r * 0.22f * pulse, pFill);

        strokeReset();
        c.restore();
    }

    /** 6. T_SPLIT: Делящийся — Нестабильный бинарный кристалл с электрическими дугами */
    void drawObsSplit(Canvas c, Obs o) {
        drawGlow(c, o.x, o.y, o.r * 2.7f, o.color, 125);

        c.save();
        c.translate(o.x, o.y);
        c.rotate(o.rot);

        if (!o.canSplit) {
            obsPath.reset();
            obsPath.moveTo(0f, -o.r * 1.15f);
            obsPath.lineTo(o.r * 0.65f, 0f);
            obsPath.lineTo(0f, o.r * 1.15f);
            obsPath.lineTo(-o.r * 0.65f, 0f);
            obsPath.close();

            pFill.setColor(withAlpha(o.color, 70));
            c.drawPath(obsPath, pFill);
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(o.color);
            pStroke.setStrokeWidth(4.5f);
            c.drawPath(obsPath, pStroke);
            pStroke.setColor(0xFFFFFFFF);
            pStroke.setStrokeWidth(1.8f);
            c.drawPath(obsPath, pStroke);
            strokeReset();
            c.restore();
            return;
        }

        float jitter = (float) Math.sin(time * 35f) * 3.5f;

        for (int side = -1; side <= 1; side += 2) {
            float ox = side * (o.r * 0.32f) + (side > 0 ? jitter : -jitter);
            obsPath.reset();
            obsPath.moveTo(ox, -o.r * 0.95f);
            obsPath.lineTo(ox + side * o.r * 0.55f, 0f);
            obsPath.lineTo(ox, o.r * 0.95f);
            obsPath.lineTo(ox - side * o.r * 0.08f, 0f);
            obsPath.close();

            pFill.setColor(withAlpha(o.color, 65));
            c.drawPath(obsPath, pFill);

            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(o.color);
            pStroke.setStrokeWidth(4.5f);
            c.drawPath(obsPath, pStroke);

            pStroke.setColor(0xFFFFFFFF);
            pStroke.setStrokeWidth(1.8f);
            c.drawPath(obsPath, pStroke);
        }

        // Электрические молнии между половинами кристалла
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2.5f);
        pStroke.setAlpha(230);
        float zapY1 = (float) Math.sin(time * 28f) * o.r * 0.4f;
        float zapY2 = -zapY1 * 0.8f;
        c.drawLine(-o.r * 0.20f, zapY1 - 8f, 0f, zapY1 + 6f, pStroke);
        c.drawLine(0f, zapY1 + 6f, o.r * 0.20f, zapY1 - 5f, pStroke);
        c.drawLine(-o.r * 0.20f, zapY2 + 5f, 0f, zapY2 - 6f, pStroke);
        c.drawLine(0f, zapY2 - 6f, o.r * 0.20f, zapY2 + 8f, pStroke);

        strokeReset();
        c.restore();
    }

    /** 7. T_MINE: Пространственная мина — Выдвижные детонаторы, радарная волна и круговой таймер */
    void drawObsMine(Canvas c, Obs o) {
        boolean armed = o.mineArmed;
        float fuseFrac = armed ? clamp(o.fuse / MINE_FUSE_TIME, 0f, 1f) : 0f;
        float blinkFreq = armed ? (16f + fuseFrac * 35f) : 8f;
        float blink = 0.5f + 0.5f * (float) Math.sin(time * blinkFreq);
        int mineCol = armed ? mix(o.color, C_RED, fuseFrac) : o.color;

        drawGlow(c, o.x, o.y, o.r * (armed ? 3.2f : 2.6f), mineCol, armed ? 180 : 100);

        if (armed) {
            float pulseRadar = (time * 2.5f) % 1f;
            pFill.setColor(withAlpha(C_RED, (int) (16f + 25f * blink * fuseFrac)));
            c.drawCircle(o.x, o.y, MINE_BLAST_R, pFill);

            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(withAlpha(C_RED, (int) (140f * (1f - pulseRadar))));
            pStroke.setStrokeWidth(3f);
            c.drawCircle(o.x, o.y, MINE_BLAST_R * pulseRadar, pStroke);

            pStroke.setColor(withAlpha(C_RED, (int) (100f + 140f * blink)));
            pStroke.setStrokeWidth(2.5f + 1.5f * blink);
            c.drawCircle(o.x, o.y, MINE_BLAST_R, pStroke);
        }

        c.save();
        c.translate(o.x, o.y);
        c.rotate(o.rot);

        // Выдвигающиеся сенсорные иглы-детонаторы
        float spikeReach = armed ? (1.10f + 0.15f * blink) : 0.95f;
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(mineCol);
        pStroke.setStrokeWidth(3.8f);
        pStroke.setAlpha(230);
        for (int k = 0; k < 8; k++) {
            double ang = Math.toRadians(k * 45f);
            float ca = (float) Math.cos(ang);
            float sa = (float) Math.sin(ang);
            c.drawLine(ca * o.r * 0.45f, sa * o.r * 0.45f,
                    ca * o.r * spikeReach, sa * o.r * spikeReach, pStroke);
            pFill.setColor(armed ? (blink > 0.5f ? 0xFFFFFFFF : C_RED) : mineCol);
            c.drawCircle(ca * o.r * spikeReach, sa * o.r * spikeReach, 3.5f, pFill);
        }

        pFill.setColor(0xFF1E0E08);
        c.drawCircle(0f, 0f, o.r * 0.52f, pFill);

        pStroke.setColor(mineCol);
        pStroke.setStrokeWidth(4.5f);
        c.drawCircle(0f, 0f, o.r * 0.52f, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(1.8f);
        pStroke.setAlpha(120);
        c.drawCircle(0f, 0f, o.r * 0.40f, pStroke);

        pFill.setColor(armed ? (blink > 0.4f ? 0xFFFFFFFF : C_RED) : mineCol);
        c.drawCircle(0f, 0f, o.r * 0.22f, pFill);

        if (armed) {
            pStroke.setColor(0xFFFFE875);
            pStroke.setStrokeWidth(3.5f);
            tmpRect.set(-o.r * 0.32f, -o.r * 0.32f, o.r * 0.32f, o.r * 0.32f);
            c.drawArc(tmpRect, -90f, (1f - fuseFrac) * 360f, false, pStroke);
        }

        strokeReset();
        c.restore();
    }

    /** 8. T_GRAVITY: Сингулярность — Космическая чёрная дыра с аккреционным диском и искажением пространства */
    void drawObsGravity(Canvas c, Obs o) {
        drawGlow(c, o.x, o.y, o.r * 3.3f, o.color, 150);

        float ripple1 = (time * 1.5f) % 1f;
        float ripple2 = (time * 1.5f + 0.5f) % 1f;
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(2f);
        pStroke.setAlpha((int) (90f * (1f - ripple1)));
        c.drawCircle(o.x, o.y, o.r * (1.2f + ripple1 * 1.2f), pStroke);
        pStroke.setAlpha((int) (90f * (1f - ripple2)));
        c.drawCircle(o.x, o.y, o.r * (1.2f + ripple2 * 1.2f), pStroke);

        c.save();
        c.translate(o.x, o.y);

        // Вращающийся спиральный аккреционный диск
        c.rotate(time * 140f);
        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(3.5f);
        pStroke.setAlpha(200);
        for (int a = 0; a < 4; a++) {
            float armAngle = a * 90f;
            tmpRect.set(-o.r * 0.92f, -o.r * 0.92f, o.r * 0.92f, o.r * 0.92f);
            c.drawArc(tmpRect, armAngle, 65f, false, pStroke);
        }

        c.rotate(time * 80f);
        pStroke.setColor(0xFFF396FF);
        pStroke.setStrokeWidth(2.2f);
        for (int a = 0; a < 4; a++) {
            float armAngle = a * 90f + 45f;
            tmpRect.set(-o.r * 0.65f, -o.r * 0.65f, o.r * 0.65f, o.r * 0.65f);
            c.drawArc(tmpRect, armAngle, 55f, false, pStroke);
        }

        pFill.setColor(0xFFFFFFFF);
        for (int k = 0; k < 4; k++) {
            double ang = Math.toRadians(time * 220f + k * 90f);
            float distM = o.r * (0.45f + 0.25f * (float) Math.sin(time * 4f + k));
            c.drawCircle((float) Math.cos(ang) * distM, (float) Math.sin(ang) * distM, 3f, pFill);
        }

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(3.5f);
        pStroke.setAlpha(255);
        c.drawCircle(0f, 0f, o.r * 0.44f, pStroke);

        pFill.setColor(0xFF040108);
        c.drawCircle(0f, 0f, o.r * 0.40f, pFill);

        strokeReset();
        c.restore();
    }

    /** 9. T_ARMOR: Бронированный — Тяжёлый бастион с композитными плитами и силовым щитом Эгида */
    void drawObsArmor(Canvas c, Obs o) {
        drawGlow(c, o.x, o.y, o.r * 2.9f, o.color, 135);

        c.save();
        c.translate(o.x, o.y);
        c.rotate(o.rot * 0.5f);

        // Силовое поле «Эгида»
        float shieldPulse = 0.5f + 0.5f * (float) Math.sin(time * 6f);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(C_CYAN);
        pStroke.setStrokeWidth(3f + shieldPulse * 1.5f);
        pStroke.setAlpha(140 + (int) (70f * shieldPulse));
        tmpRect.set(-o.r * 1.14f, -o.r * 1.14f, o.r * 1.14f, o.r * 1.14f);
        for (int s = 0; s < 4; s++) {
            c.drawArc(tmpRect, s * 90f + time * 35f, 70f, false, pStroke);
        }

        pFill.setColor(0xFFFFFFFF);
        for (int s = 0; s < 4; s++) {
            double ang = Math.toRadians(s * 90f + time * 35f);
            c.drawCircle((float) Math.cos(ang) * o.r * 1.14f,
                    (float) Math.sin(ang) * o.r * 1.14f, 4f, pFill);
        }

        // Сегментированный бастион
        obsPath.reset();
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(i * 60f);
            float hx = (float) Math.cos(ang) * o.r * 0.92f;
            float hy = (float) Math.sin(ang) * o.r * 0.92f;
            if (i == 0) obsPath.moveTo(hx, hy);
            else obsPath.lineTo(hx, hy);
        }
        obsPath.close();

        pFill.setColor(0x45E5A824);
        c.drawPath(obsPath, pFill);

        pStroke.setColor(o.color);
        pStroke.setStrokeWidth(7.5f);
        pStroke.setAlpha(245);
        c.drawPath(obsPath, pStroke);

        pStroke.setColor(0xFFFFFFFF);
        pStroke.setStrokeWidth(2f);
        pStroke.setAlpha(120);
        c.drawPath(obsPath, pStroke);

        obsInnerPath.reset();
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(i * 60f + 30f);
            float hx = (float) Math.cos(ang) * o.r * 0.54f;
            float hy = (float) Math.sin(ang) * o.r * 0.54f;
            if (i == 0) obsInnerPath.moveTo(hx, hy);
            else obsInnerPath.lineTo(hx, hy);
        }
        obsInnerPath.close();
        pStroke.setStrokeWidth(2.8f);
        pStroke.setAlpha(170);
        c.drawPath(obsInnerPath, pStroke);

        pFill.setColor(0xFFFFE875);
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(i * 60f);
            c.drawCircle((float) Math.cos(ang) * o.r * 0.72f,
                    (float) Math.sin(ang) * o.r * 0.72f, 3.5f, pFill);
        }

        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(0f, 0f, o.r * 0.22f, pFill);

        strokeReset();
        c.restore();
    }

    void drawEnemyPreview(Canvas c, float x, float y, float r, int type) {
        previewObs.type = type;
        previewObs.x = x;
        previewObs.y = y;
        previewObs.r = r;
        previewObs.rot = time * 45f + type * 30f;
        previewObs.color = OBS_COLORS[type % OBS_COLORS.length];
        previewObs.burstPhase = (time * 0.9f + type * 0.3f) % 1.05f;
        previewObs.bounceFlash = 0f;
        previewObs.mineArmed = false;
        previewObs.fuse = 0f;
        previewObs.canSplit = true;
        previewObs.armorHits = 2;
        previewObs.boss = false;
        previewObs.vx = 0f;
        previewObs.vy = 200f;
        drawObs(c, previewObs);
    }

    void drawPickup(Canvas c, Pickup p) {
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

    int bonusColor(int kind) {
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

    void drawBonusIcon(Canvas c, int kind, float x, float y, float size, int alpha) {
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
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setColor(col);
                pStroke.setAlpha(alpha);
                pStroke.setStrokeWidth(size * 0.16f);
                c.drawCircle(x, y, size * 0.82f, pStroke);
                pStroke.setColor(0xFFFFFFFF);
                pStroke.setAlpha(alpha / 2);
                pStroke.setStrokeWidth(size * 0.06f);
                c.drawCircle(x, y, size * 0.94f, pStroke);
                pStroke.setStyle(Paint.Style.FILL);
                textC(c, "×" + fmtMult(progress.temporaryMultiplier()), x, y, size * 0.72f, col, tfBlack, Paint.Align.CENTER);
                break;
            }
            default:
                break;
        }
    }

    void drawHex(Canvas c, float x, float y, float r, float rot, int color) {
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

    void drawHeart(Canvas c, float x, float y, float size, int color, int alpha) {
        c.save();
        c.translate(x, y);
        c.scale(size, size);
        pFill.setColor(color);
        pFill.setAlpha(alpha);
        c.drawPath(heartPath, pFill);
        c.restore();
    }

    void drawGlow(Canvas c, float x, float y, float size, int color, int alpha) {
        if (alpha <= 0) {
            return;
        }
        tmpRect.set(x - size * 0.5f, y - size * 0.5f, x + size * 0.5f, y + size * 0.5f);
        pGlow.setColorFilter(filter(color));
        pGlow.setAlpha(Math.min(255, alpha));
        c.drawBitmap(glowBmp, srcGlow, tmpRect, pGlow);
    }

    PorterDuffColorFilter filter(int color) {
        Integer key = color;
        PorterDuffColorFilter f = filters.get(key);
        if (f == null) {
            f = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
            filters.put(key, f);
        }
        return f;
    }
}
