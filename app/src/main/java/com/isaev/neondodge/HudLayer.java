package com.isaev.neondodge;

import android.graphics.Shader;
import android.graphics.LinearGradient;
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
        float hy = top + 66f;

        // Счёт — Unbounded; под ним полоса «до рекорда», золотая после рекорда.
        updateShownScore();
        textGlow(c, shownScoreText, W / 2f, top + 112f, 96f,
                0xFFF2FEFF, 0x805FF3FF, tfDisplay, 0.15f);
        int best = dailyChallenge ? progress.challengeBest : progress.bestForMode(mode);
        float barW = 330f;
        float barY = top + 146f;
        box(c, W / 2f - barW / 2f, barY - 5f, W / 2f + barW / 2f, barY + 5f, 5f, 0x1AFFFFFF, 0);
        float ratio = best > 0 ? clamp(score / best, 0f, 1f) : 1f;
        boolean beaten = best > 0 && score > best;
        if (ratio > 0.01f) {
            float br = W / 2f - barW / 2f + barW * ratio;
            pShade.setShader(new LinearGradient(W / 2f - barW / 2f, 0f, W / 2f + barW / 2f, 0f,
                    beaten ? new int[]{C_GOLD, 0xFFFFE9A6} : new int[]{C_CYAN, C_GOLD},
                    null, Shader.TileMode.CLAMP));
            tmpRect.set(W / 2f - barW / 2f, barY - 5f, Math.max(br, W / 2f - barW / 2f + 10f),
                    barY + 5f);
            c.drawRoundRect(tmpRect, 5f, 5f, pShade);
            pShade.setShader(null);
        }
        if (best != hudBestValue || dailyChallenge != hudBestDaily) {
            hudBestValue = best;
            hudBestDaily = dailyChallenge;
            hudBestText = (dailyChallenge ? sDailyChallenge + " · " : "")
                    + sBest + " " + fmt(best);
        }
        text(c, hudBestText, W / 2f, top + 190f, 27f,
                dailyChallenge ? 0xDDFF9FE9 : 0xFF93A9C2, tfBold, Paint.Align.CENTER);

        // Множители: звёзды, близкие пролёты и постоянный множитель уровня.
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
        String levelText = "x" + fmtMult(levelMult);
        float my = top + 262f;
        float mw = chipWidth(hudMultText);
        float cw = chipWidth(hudCloseText);
        float lw = chipWidth(levelText);
        float gap = 18f;
        float ml = W / 2f - (mw + gap + cw + gap + lw) / 2f;
        hudChip(c, ml, my, mw, hudMultText, tenths > 10 ? C_GOLD : 0, 0);
        hudChip(c, ml + mw + gap, my, cw, hudCloseText, closeTenths > 10 ? C_MAGENTA : 0, 1);
        hudChip(c, ml + mw + gap + cw + gap, my, lw, levelText,
                levelMult > 1.01f ? C_CYAN : 0, 2);

        float under = my + 74f;
        if (bossMult > 1.01f) {
            String bt = sBossMultiplier + " x" + fmtMult(bossMult);
            float bw = measure(bt, 27f, tfBlack) + 48f;
            box(c, W / 2f - bw / 2f, under - 26f, W / 2f + bw / 2f, under + 26f,
                    26f, 0x33FFD75E, 0xAAFFD75E);
            textC(c, bt, W / 2f, under, 27f, C_GOLD, tfBlack, Paint.Align.CENTER);
            under += 70f;
        }

        if (combo >= 2) {
            float pop = 1f + 0.30f * comboPop;
            float a = Math.min(1f, comboTimer / 0.6f);
            c.save();
            c.scale(pop, pop, W / 2f, under + 20f);
            textGlow(c, sCombo + " x" + combo, W / 2f, under + 36f, 48f,
                    Color.argb((int) (245f * a), 255, 233, 166),
                    Color.argb((int) (200f * a), 255, 215, 94), tfDisplay, 0.25f);
            c.restore();
        }

        if (zenMode) {
            float zw = measure(sZen, 30f, tfDisplayBold) + 52f;
            box(c, UI_PAD, hy - 30f, UI_PAD + zw, hy + 30f, 30f, 0x22B6FF5C, 0x88B6FF5C);
            textC(c, sZen, UI_PAD + zw / 2f, hy, 30f, C_LIME, tfDisplayBold, Paint.Align.CENTER);
        } else {
            // Жизни: живое сердце светится, потерянное — пустой контур.
            for (int i = 0; i < maxLives; i++) {
                float hx = UI_PAD + 30f + i * 72f;
                if (i < lives) {
                    drawGlow(c, hx, hy, 90f, C_RED, 110);
                    drawHeart(c, hx, hy, 27f, C_RED, 255);
                    drawHeart(c, hx - 7f, hy - 7f, 8f, 0xFFFFD9E0, 130);
                } else {
                    c.save();
                    c.translate(hx, hy);
                    c.scale(27f, 27f);
                    strokeStyle(UI_DIM, 2.4f / 27f);
                    c.drawPath(heartPath, pStroke);
                    strokeReset();
                    c.restore();
                }
            }
        }
        if (hardcoreMode) {
            float hw = measure(sHardcoreMultiplier, 24f, tfBlack) + 40f;
            box(c, W - UI_PAD - hw, top + 142f, W - UI_PAD, top + 190f, 24f,
                    0x22FF4F6A, 0x88FF4F6A);
            textC(c, sHardcoreMultiplier, W - UI_PAD - hw / 2f, top + 166f, 24f,
                    C_RED, tfBlack, Paint.Align.CENTER);
        }

        // Активные бонусы: чипы с кольцом-таймером и названием.
        float ex = UI_PAD;
        float ey = under + (combo >= 2 ? 120f : 40f);
        if (shieldCharges > 0) {
            ex = hudEffect(c, P_SHIELD, ex, ey, 1f, 0f);
        }
        if (magnetTimer > 0f) {
            ex = hudEffect(c, P_MAGNET, ex, ey, magnetTimer / MAGNET_TIME, magnetTimer);
        }
        if (slowTimer > 0f) {
            ex = hudEffect(c, P_SLOW, ex, ey, slowTimer / SLOW_TIME, slowTimer);
        }
        if (phaseTimer > 0f) {
            ex = hudEffect(c, P_PHASE, ex, ey, phaseTimer / PHASE_TIME, phaseTimer);
        }
        if (doubleTimer > 0f) {
            hudEffect(c, P_DOUBLE, ex, ey, doubleTimer / DOUBLE_TIME, doubleTimer);
        }

        float bx = pauseBtnX();
        float by = pauseBtnY();
        boolean down = state == ST_PLAY && pressed(B_PAUSE);
        pFill.setColor(down ? UI_SURFACE_HI : 0xBF101634);
        c.drawCircle(bx, by, 50f, pFill);
        strokeStyle(0x1FFFFFFF, 2f);
        c.drawCircle(bx, by, 50f, pStroke);
        strokeReset();
        box(c, bx - 13f, by - 17f, bx - 4f, by + 17f, 4f, 0xF0DFF5FF, 0);
        box(c, bx + 4f, by - 17f, bx + 13f, by + 17f, 4f, 0xF0DFF5FF, 0);
        if (state == ST_PLAY) {
            addHit(B_PAUSE, bx - 70f, by - 70f, bx + 70f, by + 70f);
        }
    }

    float chipWidth(String value) {
        return 76f + 14f + measure(value, 32f, tfBlack) + 26f;
    }

    /**
     * Чип множителя: иконка в кольце и значение. Выше x1 кольцо и число
     * загораются цветом множителя. icon: 0 — звезда, 1 — пролёт, 2 — уровень.
     */
    void hudChip(Canvas c, float l, float cy, float w, String value, int lit, int icon) {
        boolean on = lit != 0;
        box(c, l, cy - 38f, l + w, cy + 38f, 38f,
                on ? withAlpha(lit, 0x24) : 0x99101634, on ? withAlpha(lit, 0x8C) : 0x1AFFFFFF);
        float ix = l + 38f;
        strokeStyle(on ? lit : 0x26FFFFFF, 5f);
        c.drawCircle(ix, cy, 27f, pStroke);
        strokeReset();
        pFill.setColor(0xFF1A1730);
        c.drawCircle(ix, cy, 22f, pFill);
        int ic = on ? lit : 0xAA9FB8CC;
        if (icon == 0) {
            iconSpark(c, ix, cy, 13f, ic);
        } else if (icon == 1) {
            iconNearMiss(c, ix, cy, 13f, ic);
        } else {
            iconUpgrade(c, ix, cy, 11f, ic);
        }
        textC(c, value, ix + 27f + 14f, cy, 32f, on ? mix(lit, 0xFFFFFFFF, 0.3f) : 0xFFA9C4D6,
                tfBlack, Paint.Align.LEFT);
    }

    /** Чип бонуса: кольцо — оставшееся время, рядом название и секунды; возвращает правый край. */
    float hudEffect(Canvas c, int kind, float l, float cy, float frac, float secs) {
        int col = bonusColor(kind);
        String label = secs > 0f
                ? sBonusName[kind] + " " + String.format(sSecondsShort, (int) Math.ceil(secs))
                : sBonusName[kind];
        float w = 92f + 12f + measure(label, 28f, tfBold) + 28f;
        box(c, l, cy - 46f, l + w, cy + 46f, 46f, 0xCC101634, withAlpha(col, 0x73));
        float ix = l + 46f;
        strokeStyle(0x22FFFFFF, 6f);
        c.drawCircle(ix, cy, 34f, pStroke);
        strokeStyle(col, 6f);
        tmpRect.set(ix - 34f, cy - 34f, ix + 34f, cy + 34f);
        c.drawArc(tmpRect, -90f, 360f * clamp(frac, 0f, 1f), false, pStroke);
        strokeReset();
        drawBonusIcon(c, kind, ix, cy, 15f, 240);
        textC(c, label, ix + 46f + 12f, cy, 28f, mix(col, 0xFFFFFFFF, 0.6f), tfBold,
                Paint.Align.LEFT);
        return l + w + 16f;
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
                0xCC5FF3FF, tfDisplay);
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
                Color.argb((int) (170f * a), 255, 79, 216), tfDisplay);
    }
}
