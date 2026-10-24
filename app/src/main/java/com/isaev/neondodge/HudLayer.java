package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** HUD во время забега: счёт, жизни, множители, бонусы, подсказки. */
abstract class HudLayer extends UiKit {

    HudLayer(Context context) {
        super(context);
    }

    void drawHud(Canvas c) {
        float top = insetTop;

        updateShownScore();
        textGlow(c, shownScoreText, W / 2f, top + 112f, 96f,
                0xFFF2FEFF, 0x665FF3FF, tfBlack, 0.16f);

        int best = dailyChallenge ? progress.challengeBest : progress.bestForMode(mode);
        if (best != hudBestValue || dailyChallenge != hudBestDaily) {
            hudBestValue = best;
            hudBestDaily = dailyChallenge;
            hudBestText = (dailyChallenge ? sDailyChallenge + " · " : "")
                    + sBest + " " + fmt(best);
        }
        text(c, hudBestText, W / 2f, top + 154f, 24f,
                dailyChallenge ? 0xDDFF9FE9 : 0xAA9FB8CC, tfMed, Paint.Align.CENTER);

        // Множители — пара чипов с иконками: звёзды и близкие пролёты.
        int tenths = Math.round(scoreMult * 10f);
        if (tenths != hudMultTenths) {
            hudMultTenths = tenths;
            hudMultText = "x" + fmtMult(scoreMult);
        }
        int closeTenths = Math.round(closeMult * 10f);
        if (closeTenths != hudCloseTenths) {
            hudCloseTenths = closeTenths;
            hudCloseText = "x" + fmtMult(closeMult);
        }
        boolean hot = tenths > 10;
        boolean close = closeTenths > 10;
        float my = top + 200f;
        float mw = measure(hudMultText, 25f, tfBlack) + 74f;
        float cw = measure(hudCloseText, 25f, tfBlack) + 74f;
        float gap = 12f;
        float ml = W / 2f - (mw + gap + cw) / 2f;
        hudChip(c, ml, my, mw, hudMultText, hot ? C_GOLD : 0, true);
        hudChip(c, ml + mw + gap, my, cw, hudCloseText, close ? C_MAGENTA : 0, false);

        if (hardcoreMode) {
            float hw = measure(sHardcoreMultiplier, 22f, tfBlack) + 34f;
            box(c, W - UI_PAD - hw, my - 22f, W - UI_PAD, my + 22f, 22f,
                    0x22FF4F6A, 0x88FF4F6A);
            textC(c, sHardcoreMultiplier, W - UI_PAD - hw / 2f, my, 22f,
                    C_RED, tfBlack, Paint.Align.CENTER);
        }
        if (bossMult > 1.01f) {
            String bt = sBossMultiplier + " x" + fmtMult(bossMult);
            float bw = measure(bt, 23f, tfBlack) + 38f;
            box(c, W / 2f - bw / 2f, my + 34f, W / 2f + bw / 2f, my + 78f,
                    22f, 0x33FFD75E, 0xAAFFD75E);
            textC(c, bt, W / 2f, my + 56f, 23f, C_GOLD, tfBlack, Paint.Align.CENTER);
        }

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
            // Жизни: живое сердце с мягким ореолом, потерянное — тусклый силуэт.
            for (int i = 0; i < maxLives; i++) {
                float hx = UI_PAD + 22f + i * 58f;
                if (i < lives) {
                    drawGlow(c, hx, hy, 58f, C_RED, 80);
                    drawHeart(c, hx, hy, 21f, C_RED, 255);
                    drawHeart(c, hx - 5f, hy - 5f, 7f, 0xFFFFD9E0, 120);
                } else {
                    drawHeart(c, hx, hy, 21f, 0xFF5A6D86, 70);
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
        pFill.setColor(down ? UI_SURFACE_HI : UI_SURFACE);
        c.drawCircle(bx, by, 40f, pFill);
        strokeStyle(UI_LINE, 2f);
        c.drawCircle(bx, by, 40f, pStroke);
        strokeReset();
        box(c, bx - 11f, by - 14f, bx - 4f, by + 14f, 3.5f, 0xEEDFF5FF, 0);
        box(c, bx + 4f, by - 14f, bx + 11f, by + 14f, 3.5f, 0xEEDFF5FF, 0);
        if (state == ST_PLAY) {
            addHit(B_PAUSE, bx - 70f, by - 70f, bx + 70f, by + 70f);
        }
    }

    /** Чип множителя: иконка и значение; цвет загорается, когда множитель выше x1. */
    void hudChip(Canvas c, float l, float cy, float w, String value, int lit, boolean star) {
        boolean on = lit != 0;
        box(c, l, cy - 22f, l + w, cy + 22f, 22f,
                on ? withAlpha(lit, 0x26) : 0x14FFFFFF, on ? withAlpha(lit, 0x99) : 0);
        int ic = on ? lit : 0xAA9FB8CC;
        if (star) {
            iconSpark(c, l + 28f, cy, 12f, ic);
        } else {
            iconNearMiss(c, l + 28f, cy, 12f, ic);
        }
        textC(c, value, l + 48f, cy, 25f, on ? lit : 0xBBA9C4D6, tfBlack, Paint.Align.LEFT);
    }

    void hudEffect(Canvas c, int kind, float x, float y, float frac) {
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

    void drawResumeCountdown(Canvas c) {
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

    void drawTutorial(Canvas c) {
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

    void drawAnnounce(Canvas c) {
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
}
