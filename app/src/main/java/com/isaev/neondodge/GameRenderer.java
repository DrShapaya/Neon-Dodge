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
                || state == ST_STATS || state == ST_QUESTS || state == ST_SKINS
                || state == ST_HISTORY || state == ST_WORKSHOP
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
        drawBossZone(c);
        drawBossBullets(c);
        for (int i = 0; i < pickups.size(); i++) {
            drawPickup(c, pickups.get(i));
        }
        for (int i = 0; i < obstacles.size(); i++) {
            drawObs(c, obstacles.get(i));
        }
        drawParticles(c);
        drawBombCharge(c);
        drawScorePops(c);
        if (state != ST_OVER) {
            drawPlayer(c, px, py);
        }
    }

    void drawBossZone(Canvas c) {
        if (bossZone == null) {
            return;
        }
        float pulse = 0.88f + 0.12f * (float) Math.sin(time * 4f);
        drawGlow(c, bossZone.x, bossZone.y, BOSS_ZONE_RADIUS * 2.6f, C_CYAN,
                110 + (int) (50f * pulse));
        pFill.setColor(0x1835E7FF);
        c.drawCircle(bossZone.x, bossZone.y, BOSS_ZONE_RADIUS, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(6f);
        pStroke.setColor(C_CYAN);
        pStroke.setAlpha(210);
        c.drawCircle(bossZone.x, bossZone.y, BOSS_ZONE_RADIUS, pStroke);
        pStroke.setColor(0xAAFFFFFF);
        pStroke.setStrokeWidth(3f);
        tmpRect.set(bossZone.x - BOSS_ZONE_RADIUS + 10f,
                bossZone.y - BOSS_ZONE_RADIUS + 10f,
                bossZone.x + BOSS_ZONE_RADIUS - 10f,
                bossZone.y + BOSS_ZONE_RADIUS - 10f);
        c.drawArc(tmpRect, -90f, bossZone.fill * 360f, false, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        textC(c, sBossZone + " " + (bossZonesCompleted + 1) + "/4",
                bossZone.x, bossZone.y - 12f, 24f, UI_INK, tfBlack, Paint.Align.CENTER);
        textC(c, fmt((int) (bossZone.fill * 100f)) + "%",
                bossZone.x, bossZone.y + 24f, 22f, C_CYAN, tfBold, Paint.Align.CENTER);
    }

    void drawBossBullets(Canvas c) {
        for (int i = 0; i < bossBullets.size(); i++) {
            BossBullet b = bossBullets.get(i);
            drawGlow(c, b.x, b.y, b.r * 4f, b.color, 145);
            pFill.setColor(b.color);
            c.drawCircle(b.x, b.y, b.r, pFill);
            pFill.setColor(0xFFFFFFFF);
            c.drawCircle(b.x - b.r * 0.25f, b.y - b.r * 0.25f, b.r * 0.28f, pFill);
        }
    }

    void drawParticles(Canvas c) {
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            float lr = clamp(p.life / p.maxLife, 0f, 1f);
            pFill.setColor(p.color);
            int baseA = Color.alpha(p.color);
            pFill.setAlpha((int) (baseA * lr));
            c.drawCircle(p.x, p.y, p.size * (0.4f + 0.6f * lr), pFill);
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

    void drawObsMark(Canvas c, Obs o) {
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
                if (o.armorHits <= 1) {
                    pStroke.setColor(0xFFFFF1B0);
                    pStroke.setAlpha(220);
                    pStroke.setStrokeWidth(3.4f);
                    c.drawLine(o.x - o.r * 0.45f, o.y - o.r * 0.50f,
                            o.x - o.r * 0.05f, o.y + o.r * 0.08f, pStroke);
                    c.drawLine(o.x - o.r * 0.05f, o.y + o.r * 0.08f,
                            o.x + o.r * 0.38f, o.y - o.r * 0.18f, pStroke);
                    c.drawLine(o.x - o.r * 0.05f, o.y + o.r * 0.08f,
                            o.x + o.r * 0.18f, o.y + o.r * 0.48f, pStroke);
                }
                break;
            }
            default:
                break;
        }
        pStroke.setStyle(Paint.Style.FILL);
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
                textC(c, "×2", x, y, size * 0.78f, col, tfBlack, Paint.Align.CENTER);
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
