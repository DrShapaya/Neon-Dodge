package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Экраны статистики, истории, заданий, скинов и правил. */
abstract class InfoScreens extends MenuScreen {

    InfoScreens(Context context) {
        super(context);
    }

    void drawStats(Canvas c) {
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

    void drawHistory(Canvas c) {
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

    int historyColor(int type) {
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

    void drawQuests(Canvas c) {
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

    void drawSkins(Canvas c) {
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

    void drawSkinCard(Canvas c, float l, float t, float w, float h, int index,
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

    void drawRules(Canvas c) {
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

    float rulesHeader(Canvas c, String title, float y) {
        text(c, title, UI_PAD + 8f, y + 40f, 38f, UI_INK, tfBlack, Paint.Align.LEFT);
        return y + 82f;
    }

    float rulesBullet(Canvas c, String s, float y) {
        pFill.setColor(C_CYAN);
        c.drawCircle(UI_PAD + 18f, y + 2f, 5f, pFill);
        float h = drawWrapped(c, s, UI_PAD + 44f, y + 12f, 28f, W - UI_PAD * 2f - 60f,
                0xFFD6E6F0, tfMed, Paint.Align.LEFT, 40f);
        return y + h + 16f;
    }

    float rulesBody(Canvas c, String s, float y) {
        float h = drawWrapped(c, s, UI_PAD + 8f, y + 12f, 28f, W - UI_PAD * 2f - 20f,
                0xFFD6E6F0, tfMed, Paint.Align.LEFT, 40f);
        return y + h + 14f;
    }

    float rulesAsteroid(Canvas c, int index, float y) {
        drawHex(c, UI_PAD + 34f, y + 22f, 26f, time * 16f + index * 24f, OBS_COLORS[index]);
        text(c, sAstName[index], UI_PAD + 92f, y + 22f, 29f, UI_INK,
                tfBold, Paint.Align.LEFT);
        float h = drawWrapped(c, sAstDesc[index], UI_PAD + 92f, y + 58f, 25f,
                W - UI_PAD * 2f - 110f, UI_FOG, tfMed, Paint.Align.LEFT, 34f);
        return y + 36f + h + 22f;
    }

    float rulesBonus(Canvas c, int index, float y) {
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

    void drawToast(Canvas c) {
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
}
