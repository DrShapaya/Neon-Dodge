package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Экраны статистики, истории, скинов и правил. */
abstract class InfoScreens extends MenuScreen {

    InfoScreens(Context context) {
        super(context);
    }

    void drawProfile(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sProfileTitle);
        float y = beginScroll(c, top);
        float l = UI_PAD;
        float r = W - UI_PAD;

        int rank = progress.rank();
        int titleIdx = GameRules.titleIndexForRank(rank);
        String title = sRankTitle[titleIdx];
        float prog = GameRules.rankProgress(progress.neonEarned);
        boolean maxed = rank >= GameRules.MAX_RANK;

        // Главная карточка ранга с неоновым гербом
        float cardH = 340f;
        int emblemColor = maxed ? C_GOLD : (rank >= 70 ? C_MAGENTA : (rank >= 40 ? C_VIOLET : C_CYAN));
        box(c, l, y, r, y + cardH, UI_RADIUS, UI_SURFACE, withAlpha(emblemColor, 0x66));

        // Эмблема ранга
        float cx = W / 2f;
        float ey = y + 104f;
        drawGlow(c, cx, ey, 180f, emblemColor, 150);

        float pulse = 1f + 0.05f * (float) Math.sin(time * 3f);
        strokeStyle(withAlpha(emblemColor, 210), 4f);
        c.drawCircle(cx, ey, 56f * pulse, pStroke);
        strokeStyle(0x55FFFFFF, 2f);
        c.drawCircle(cx, ey, 68f * pulse, pStroke);
        strokeReset();

        pFill.setColor(withAlpha(emblemColor, 45));
        c.drawCircle(cx, ey, 50f, pFill);
        textC(c, String.valueOf(rank), cx, ey, 54f, maxed ? 0xFFFFE9A6 : UI_INK, tfBlack, Paint.Align.CENTER);

        // Звание и прогресс
        textC(c, title, cx, y + 204f, 42f, emblemColor, tfDisplayBold, Paint.Align.CENTER);
        textC(c, maxed ? sProfileMaxRank : String.format(sRankLabel, rank) + " · " + Math.round(prog * 100f) + "%",
                cx, y + 244f, 26f, UI_FOG, tfBold, Paint.Align.CENTER);

        // Полоса прогресса
        progressBar(c, l + 40f, r - 40f, y + 288f, 14f, prog, emblemColor);
        if (maxed) {
            textC(c, sProfileMaxRank, cx, y + 318f, 22f, C_MINT, tfBold, Paint.Align.CENTER);
        } else {
            long need = GameRules.rankThreshold(rank + 1) - progress.neonEarned;
            String needText = String.format(sProfileNextRank, rank + 1, fmt(need));
            textC(c, needText, cx, y + 318f, 22f, UI_FOG, tfMed, Paint.Align.CENTER);
        }
        y += cardH + 26f;

        // Карточка заработанного неона
        float statH = 110f;
        card(c, l, y, r, y + statH, 0, false);
        iconDiamond(c, l + 54f, y + statH / 2f, 18f, C_GOLD);
        text(c, sProfileTotalNeon, l + 96f, y + 46f, 25f, UI_FOG, tfMed, Paint.Align.LEFT);
        text(c, fmt(progress.neonEarned), l + 96f, y + 86f, 34f, 0xFFFFE9A6, tfBlack, Paint.Align.LEFT);
        y += statH + 28f;

        // Лестница званий
        y = sectionLabel(c, sProfileLadder, y);
        for (int i = 0; i < GameRules.TITLES_COUNT; i++) {
            int minR = GameRules.titleMinRank(i);
            int maxR = GameRules.titleMaxRank(i);
            boolean isCur = i == titleIdx;
            boolean isPassed = rank > maxR;
            float rowH = 104f;
            int accent = isCur ? emblemColor : (isPassed ? C_MINT : 0x44FFFFFF);
            box(c, l, y, r, y + rowH, 24f, isCur ? UI_SURFACE_HI : UI_SURFACE,
                    isCur ? withAlpha(accent, 200) : (isPassed ? withAlpha(C_MINT, 90) : 0x1AFFFFFF));

            float ix = l + 48f;
            float iy = y + rowH / 2f;
            if (isPassed) {
                pFill.setColor(C_MINT);
                c.drawCircle(ix, iy, 20f, pFill);
                iconCheck(c, ix, iy, 12f, UI_ON_ACCENT, 4f);
            } else if (isCur) {
                drawGlow(c, ix, iy, 60f, accent, 140);
                pFill.setColor(accent);
                c.drawCircle(ix, iy, 20f, pFill);
                pFill.setColor(0xFFFFFFFF);
                c.drawCircle(ix, iy, 9f, pFill);
            } else {
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setColor(0x44FFFFFF);
                pStroke.setStrokeWidth(2.5f);
                c.drawCircle(ix, iy, 18f, pStroke);
                pStroke.setStyle(Paint.Style.FILL);
            }

            text(c, sRankTitle[i], l + 92f, y + 46f, 30f, isCur ? accent : (isPassed ? UI_INK : UI_FOG), tfBold, Paint.Align.LEFT);
            String rangeStr = minR == maxR ? String.format(sProfileLvlSingle, minR) : String.format(sProfileLvlRange, minR, maxR);
            text(c, rangeStr, l + 92f, y + 80f, 22f, UI_DIM, tfMed, Paint.Align.LEFT);

            if (isCur) {
                box(c, r - 160f, y + 26f, r - 24f, y + rowH - 26f, 20f, withAlpha(accent, 40), accent);
                textC(c, sProfileCurrent, r - 92f, y + rowH / 2f, 20f, accent, tfBold, Paint.Align.CENTER);
            }
            y += rowH + 14f;
        }

        endScroll(c, y);
    }

    void drawStats(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sStats);
        float y = beginScroll(c, top);
        float l = UI_PAD;
        float r = W - UI_PAD;

        // Переход в профиль ранга
        int rank = progress.rank();
        y = linkRow(c, B_PROFILE, sProfileTitle, sRankTitle[GameRules.titleIndexForRank(rank)] + " · " + rank, y);
        y += 18f;

        y = sectionLabel(c, sRecords, y);
        float rowH = 88f;
        card(c, l, y, r, y + rowH * 3f, 0, false);
        for (int i = 0; i < 3; i++) {
            float ry = y + rowH * i + rowH / 2f;
            int type = i;
            if (i > 0) {
                pFill.setColor(UI_LINE);
                c.drawRect(l + 72f, y + rowH * i - 1f, r - 32f, y + rowH * i + 1f, pFill);
            }
            pFill.setColor(historyColor(type));
            c.drawCircle(l + 44f, ry, 9f, pFill);
            textC(c, sModeNames[i], l + 72f, ry, 29f,
                    UI_INK, tfMed, Paint.Align.LEFT);
            int v = progress.bestForMode(i);
            textC(c, fmt(v), r - 36f, ry, 34f, v > 0 ? UI_INK : UI_DIM, tfBlack,
                    Paint.Align.RIGHT);
        }
        y += rowH * 3f + 20f;

        int hc = progress.runHistory.size();
        y = linkRow(c, B_HISTORY, sHistory, hc > 0 ? String.valueOf(hc) : "", y);
        y += 28f;

        y = sectionLabel(c, sTotals, y);
        String[] values = {
                fmt(progress.dodged),
                "x" + fmt(progress.bestCombo),
                formatTime((int) progress.totalTime),
                fmt(progress.stars),
                fmt(progress.runs),
                fmt(progress.neonEarned)
        };
        float gap = 18f;
        float tw = (r - l - gap * 2f) / 3f;
        float th = 140f;
        for (int i = 0; i < 6; i++) {
            float tl = l + (i % 3) * (tw + gap);
            float tt = y + (i / 3) * (th + gap);
            card(c, tl, tt, tl + tw, tt + th, 0, false);
            boolean neon = i == 5;
            float sz = 38f;
            if (measure(values[i], sz, tfBlack) > tw - 24f) {
                sz = 28f;
            }
            text(c, values[i], tl + tw / 2f, tt + 72f, sz,
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
            String modeLabel = (rec.type >= 0 && rec.type < sModeNames.length)
                    ? sModeNames[rec.type] : sModeNames[0];
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
            default:
                return C_CYAN;
        }
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
        float l = UI_PAD;
        float r = W - UI_PAD;

        // Вкладки закреплены над прокруткой
        float segH = 84f;
        float sy = top + 14f + segH / 2f;
        box(c, l, sy - segH / 2f, r, sy + segH / 2f, segH / 2f, 0xCC0A1226, UI_LINE);
        float segW = (r - l - 12f) / 3f;
        for (int i = 0; i < 3; i++) {
            float sl = l + 6f + i * segW;
            float sr = sl + segW;
            boolean sel = rulesTab == i;
            if (sel) {
                box(c, sl, sy - segH / 2f + 6f, sr, sy + segH / 2f - 6f,
                        (segH - 12f) / 2f, 0x305FF3FF, 0xCC5FF3FF);
            }
            String tabName = i == 0 ? sRulesTabBasics : (i == 1 ? sRulesTabAsteroids : sRulesTabBonuses);
            textC(c, tabName, (sl + sr) / 2f, sy, 25f,
                    sel ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(B_RULES_TAB_BASICS + i, sl, sy - segH / 2f, sr, sy + segH / 2f);
        }

        float y = beginScroll(c, sy + segH / 2f + 16f);

        if (rulesTab == 0) {
            y = drawRulesBasics(c, l, r, y);
        } else if (rulesTab == 1) {
            for (int i = 0; i < sAstName.length; i++) {
                y = rulesAsteroid(c, l, r, i, y);
            }
        } else {
            for (int i = 0; i < 8; i++) {
                y = rulesBonus(c, l, r, i, y);
            }
        }
        endScroll(c, y);
    }

    float drawRulesBasics(Canvas c, float l, float r, float y) {
        y = sectionLabel(c, sRulesHowTo, y);
        for (int i = 0; i < sRuleLine.length; i++) {
            int lines = countWrapped(sRuleLine[i], 25f, r - l - 76f, tfMed);
            float cardH = Math.max(76f, 30f + lines * 34f);
            box(c, l, y, r, y + cardH, 20f, UI_SURFACE, 0x1AFFFFFF);
            pFill.setColor(C_CYAN);
            c.drawCircle(l + 34f, y + cardH / 2f, 5f, pFill);
            drawWrapped(c, sRuleLine[i], l + 54f, y + 26f, 25f, r - l - 76f,
                    0xFFE0ECF6, tfMed, Paint.Align.LEFT, 34f);
            y += cardH + 10f;
        }
        y += 14f;

        y = sectionLabel(c, sRulesMultiplier, y);
        int multLines = countWrapped(sRulesMultiplierDesc, 25f, r - l - 50f, tfMed);
        float multCardH = 40f + multLines * 36f;
        box(c, l, y, r, y + multCardH, 22f, UI_SURFACE, 0x2E5FF3FF);
        drawWrapped(c, sRulesMultiplierDesc, l + 25f, y + 30f, 25f, r - l - 50f,
                0xFFE0ECF6, tfMed, Paint.Align.LEFT, 36f);
        y += multCardH + 22f;

        y = sectionLabel(c, sRulesProgress, y);
        for (int i = 0; i < sRuleProgress.length; i++) {
            int lines = countWrapped(sRuleProgress[i], 25f, r - l - 76f, tfMed);
            float cardH = Math.max(76f, 30f + lines * 34f);
            box(c, l, y, r, y + cardH, 20f, UI_SURFACE, 0x1AFFFFFF);
            pFill.setColor(C_GOLD);
            c.drawCircle(l + 34f, y + cardH / 2f, 5f, pFill);
            drawWrapped(c, sRuleProgress[i], l + 54f, y + 26f, 25f, r - l - 76f,
                    0xFFE0ECF6, tfMed, Paint.Align.LEFT, 34f);
            y += cardH + 10f;
        }
        return y;
    }

    float rulesAsteroid(Canvas c, float l, float r, int index, float y) {
        int col = OBS_COLORS[index];
        int lines = countWrapped(sAstDesc[index], 24f, r - l - 124f, tfMed);
        float textH = lines * 33f;
        float cardH = Math.max(116f, 84f + textH);

        box(c, l, y, r, y + cardH, 22f, UI_SURFACE, withAlpha(col, 0x55));

        float ix = l + 54f;
        float iy = y + cardH / 2f;
        drawGlow(c, ix, iy, 90f, col, 120);
        drawHex(c, ix, iy, 26f, time * 18f + index * 30f, col);

        text(c, sAstName[index], l + 104f, y + 42f, 29f, UI_INK, tfBold, Paint.Align.LEFT);
        drawWrapped(c, sAstDesc[index], l + 104f, y + 76f, 24f,
                r - l - 124f, UI_FOG, tfMed, Paint.Align.LEFT, 33f);

        return y + cardH + 14f;
    }

    float rulesBonus(Canvas c, float l, float r, int index, float y) {
        int bcol = bonusColor(index);
        int lines = countWrapped(sBonusDesc[index], 24f, r - l - 124f, tfMed);
        float textH = lines * 33f;
        float cardH = Math.max(116f, 84f + textH);

        box(c, l, y, r, y + cardH, 22f, UI_SURFACE, withAlpha(bcol, 0x55));

        float ix = l + 54f;
        float iy = y + cardH / 2f;
        drawGlow(c, ix, iy, 90f, bcol, 120);
        if (index == 0) {
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
            pFill.setAlpha(245);
            c.drawPath(starPath, pFill);
        } else if (index == 1) {
            drawHeart(c, ix, iy, 21f, C_RED, 255);
        } else {
            drawBonusIcon(c, index, ix, iy, 22f, 255);
        }

        text(c, sBonusName[index], l + 104f, y + 42f, 29f, UI_INK, tfBold, Paint.Align.LEFT);
        drawWrapped(c, sBonusDesc[index], l + 104f, y + 76f, 24f,
                r - l - 124f, UI_FOG, tfMed, Paint.Align.LEFT, 33f);

        return y + cardH + 14f;
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
