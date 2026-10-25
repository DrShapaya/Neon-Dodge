package com.isaev.neondodge;

import android.graphics.Shader;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Пауза, настройки и итог забега. */
abstract class OverlayScreens extends InfoScreens {

    OverlayScreens(Context context) {
        super(context);
    }

    void drawPause(Canvas c) {
        pFill.setColor(0xD0050812);
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);

        float l = UI_PAD;
        float r = W - UI_PAD;
        float pw = r - l;
        float inner = pw - 96f;
        float panelH = 744f;
        float pt = Math.max(insetTop + 40f, (worldH - panelH) / 2f);
        box(c, l, pt, r, pt + panelH, 44f, 0xF00E1630, UI_LINE);
        tube(c, W / 2f - 90f, pt, W / 2f + 90f, pt, C_CYAN, 4f);

        float cx = W / 2f;
        float y = pt + 116f;
        text(c, sPause, cx, y, 58f, UI_INK, tfDisplay, Paint.Align.CENTER);
        y += 50f;
        updateShownScore();
        text(c, sScore + " " + shownScoreText + "   ·   " + formatTime((int) elapsed),
                cx, y, 26f, UI_FOG, tfMed, Paint.Align.CENTER);
        y += 98f;

        primaryButton(c, B_CONTINUE, cx, y, inner, 132f, sTapContinue, C_CYAN, 0.5f);
        y += 66f + 92f;

        // Звук — три круглых переключателя: горящее кольцо — включено,
        // перечёркнутая иконка — выключено.
        int[] ids = {B_MUSIC, B_SOUND, B_VIBRATION};
        String[] labels = {sMusic, sSound, sVibration};
        boolean[] on = {musicOn, soundOn, vibOn};
        float slot = inner / 3f;
        float rr = 52f;
        for (int i = 0; i < 3; i++) {
            float tcx = cx - inner / 2f + slot * (i + 0.5f);
            boolean down = pressed(ids[i]);
            pFill.setColor(down ? UI_SURFACE_HI : (on[i] ? 0x1F5FF3FF : 0x0FFFFFFF));
            c.drawCircle(tcx, y, rr, pFill);
            if (on[i]) {
                strokeStyle(0x305FF3FF, 10f);
                c.drawCircle(tcx, y, rr, pStroke);
            }
            strokeStyle(on[i] ? C_CYAN : 0x26FFFFFF, on[i] ? 3f : 2f);
            c.drawCircle(tcx, y, rr, pStroke);
            strokeReset();
            int ic = on[i] ? 0xFFDFFBFF : UI_DIM;
            if (i == 0) {
                iconNote(c, tcx, y, 20f, ic);
            } else if (i == 1) {
                iconSpeaker(c, tcx, y, 20f, ic);
            } else {
                iconVibrate(c, tcx, y, 20f, ic);
            }
            if (!on[i]) {
                iconSlash(c, tcx, y, 22f, ic);
            }
            textC(c, labels[i], tcx, y + rr + 34f, 23f, on[i] ? UI_INK : UI_FOG,
                    tfMed, Paint.Align.CENTER);
            addHit(ids[i], tcx - slot / 2f, y - rr - 10f, tcx + slot / 2f, y + rr + 56f);
        }
        y += rr + 96f;

        pFill.setColor(UI_LINE);
        c.drawRect(cx - inner / 2f, y - 1f, cx + inner / 2f, y + 1f, pFill);
        y += 74f;

        // Правила и выход — одной строкой. Первое нажатие «выйти» раскрывает
        // кнопку на всю ширину с предупреждением, второе завершает забег.
        boolean armed = exitArmed > 0f;
        float bh = 100f;
        float half = (inner - 18f) / 2f;
        if (!armed) {
            float rcx = cx - inner / 2f + half / 2f;
            ghostButton(c, B_PAUSE_RULES, rcx, y, half, bh, sRulesButton, 0, false);
        }
        float ew = armed ? inner : half;
        float ecx = armed ? cx : cx + inner / 2f - half / 2f;
        ghostButton(c, B_EXIT, ecx, y, ew, bh, armed ? sExitConfirm : sToMenu, 1, armed);
    }

    /** Кнопка-строка паузы: иконка и подпись; danger — красное предупреждение. */
    void ghostButton(Canvas c, int id, float cx, float cy, float w, float h, String label,
                     int icon, boolean danger) {
        boolean down = pressed(id);
        int accent = danger ? C_RED : 0xFFFFFFFF;
        box(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h / 2f,
                down ? withAlpha(accent, 0x22) : (danger ? 0x18FF4F6A : 0x0DFFFFFF),
                danger ? 0xAAFF4F6A : UI_LINE);
        int col = danger ? 0xFFFF8A9C : UI_INK;
        float size = danger ? 26f : 28f;
        float tw = measure(label, size, tfBold);
        float total = tw + 54f;
        float ix = cx - total / 2f + 16f;
        if (icon == 0) {
            iconDoc(c, ix, cy, 16f, col);
        } else {
            iconExit(c, ix, cy, 16f, col);
        }
        textC(c, label, ix + 38f + tw / 2f, cy, size, col, tfBold, Paint.Align.CENTER);
        addHit(id, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    void drawSettings(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sSettings);
        float y = beginScroll(c, top);
        float l = UI_PAD;
        float r = W - UI_PAD;

        y = sectionLabel(c, sControlMode, y);
        boolean offsetEnabled = controlMode == CONTROL_ABOVE;
        String hint = offsetEnabled ? sOffsetHint : sControlAtHint;
        int hintLines = countWrapped(hint, 24f, r - l - 72f, tfMed);
        float ch = 330f + hintLines * 34f;
        card(c, l, y, r, y + ch, 0, false);

        float segH = 88f;
        float sy = y + 30f + segH / 2f;
        float sl0 = l + 30f;
        float sr0 = r - 30f;
        box(c, sl0, sy - segH / 2f, sr0, sy + segH / 2f, segH / 2f, 0x99060914, UI_LINE);
        float segW = (sr0 - sl0 - 12f) / 2f;
        for (int i = 0; i < 2; i++) {
            float sl = sl0 + 6f + i * segW;
            float sr = sl + segW;
            boolean sel = controlMode == i;
            if (sel) {
                box(c, sl, sy - segH / 2f + 6f, sr, sy + segH / 2f - 6f,
                        (segH - 12f) / 2f, 0x305FF3FF, 0xCC5FF3FF);
            }
            textC(c, i == CONTROL_ABOVE ? sControlAbove : sControlAt, (sl + sr) / 2f, sy,
                    27f, sel ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(B_CONTROL + i, sl, sy - segH / 2f, sr, sy + segH / 2f);
        }

        float ry = sy + segH / 2f + 64f;
        text(c, sOffset, l + 36f, ry, 28f, offsetEnabled ? UI_INK : UI_DIM, tfMed,
                Paint.Align.LEFT);
        text(c, offsetEnabled ? String.valueOf((int) fingerOffset) : "—", r - 36f, ry + 2f,
                36f, offsetEnabled ? C_CYAN : UI_DIM, tfBlack, Paint.Align.RIGHT);

        offsetSliderX1 = l + 56f;
        offsetSliderX2 = r - 56f;
        float sly = ry + 66f;
        box(c, offsetSliderX1, sly - 5f, offsetSliderX2, sly + 5f, 5f,
                offsetEnabled ? 0x33FFFFFF : 0x14FFFFFF, 0);
        float kx = offsetSliderX1 + (offsetSliderX2 - offsetSliderX1)
                * (fingerOffset - OFFSET_MIN) / (OFFSET_MAX - OFFSET_MIN);
        if (kx > offsetSliderX1 + 1f) {
            box(c, offsetSliderX1, sly - 5f, kx, sly + 5f, 5f,
                    offsetEnabled ? C_CYAN : 0x335FF3FF, 0);
        }
        if (offsetEnabled) {
            drawGlow(c, kx, sly, draggingOffset ? 130f : 90f, C_CYAN, 120);
            pFill.setColor(0xFFFFFFFF);
            c.drawCircle(kx, sly, draggingOffset ? 24f : 20f, pFill);
            pFill.setColor(C_CYAN);
            c.drawCircle(kx, sly, 11f, pFill);
            addHit(B_SLIDER, offsetSliderX1 - 40f, sly - 56f, offsetSliderX2 + 40f, sly + 56f);
        } else {
            pFill.setColor(0x55FFFFFF);
            c.drawCircle(kx, sly, 15f, pFill);
        }
        drawWrapped(c, hint, l + 36f, sly + 70f, 24f, r - l - 72f,
                offsetEnabled ? UI_FOG : 0xCC7CF9D0, tfMed, Paint.Align.LEFT, 34f);
        y += ch + 32f;

        y = sectionLabel(c, sLanguage, y);
        float optionH = 104f;
        card(c, l, y, r, y + optionH, 0, false);
        drawSettingsChoice(c, y, B_LANGUAGE_RU, B_LANGUAGE_EN, sRussian, sEnglish,
                languageTag.equals("ru"));
        y += optionH + 32f;

        y = sectionLabel(c, sRefreshRate, y);
        card(c, l, y, r, y + optionH, 0, false);
        drawSettingsChoice(c, y, B_HZ_60, B_HZ_120, sHz60, sHz120, targetHz == 60);
        y += optionH + 32f;

        y = sectionLabel(c, sSoundSection, y);
        float rowH = 104f;
        card(c, l, y, r, y + rowH * 5f, 0, false);
        int[] ids = {B_MUSIC, B_SOUND, B_VIBRATION};
        String[] labels = {sMusic, sSound, sVibration};
        boolean[] on = {musicOn, soundOn, vibOn};
        for (int i = 0; i < 5; i++) {
            float rt = y + i * rowH;
            float rcy = rt + rowH / 2f;
            if (i > 0) {
                pFill.setColor(UI_LINE);
                c.drawRect(l + 36f, rt - 1f, r - 36f, rt + 1f, pFill);
            }
            if (i < 3) {
                if (pressed(ids[i])) {
                    box(c, l + 4f, rt + 4f, r - 4f, rt + rowH - 4f, 24f, 0x12FFFFFF, 0);
                }
                textC(c, labels[i], l + 36f, rcy, 29f, UI_INK, tfMed, Paint.Align.LEFT);
                switchToggle(c, r - 84f, rcy, on[i]);
                addHit(ids[i], l, rt, r, rt + rowH);
            } else {
                int sliderId = i == 3 ? B_MUSIC_VOLUME : B_SOUND_VOLUME;
                String label = i == 3 ? sMusicVolume : sSoundVolume;
                float value = i == 3 ? musicVolume : soundVolume;
                drawVolumeSlider(c, sliderId, label, value, rt, l, r);
            }
        }
        y += rowH * 5f + 32f;

        float h = 104f;
        boolean down = pressed(B_TUTORIAL);
        card(c, l, y, r, y + h, 0, false);
        if (down) {
            box(c, l, y, r, y + h, 28f, 0x18FFFFFF, 0);
        }
        textC(c, sReplayTutorial, l + 36f, y + h / 2f, 29f, UI_INK, tfMed, Paint.Align.LEFT);
        iconReplay(c, r - 56f, y + h / 2f, 20f, UI_FOG);
        addHit(B_TUTORIAL, l, y, r, y + h);
        y += h + 40f;

        if (versionName != null && versionName.length() > 0) {
            text(c, "Neon Dodge · " + sVersion + " " + versionName, W / 2f, y + 20f, 23f,
                    UI_DIM, tfMed, Paint.Align.CENTER);
            y += 40f;
        }
        endScroll(c, y);
    }

    void drawSettingsChoice(Canvas c, float y, int firstId, int secondId,
                            String first, String second, boolean firstSelected) {
        float l = UI_PAD + 30f;
        float r = W - UI_PAD - 30f;
        float h = 88f;
        float gap = 12f;
        float w = (r - l - gap) / 2f;
        for (int i = 0; i < 2; i++) {
            float sl = l + i * (w + gap);
            float sr = sl + w;
            boolean selected = i == 0 ? firstSelected : !firstSelected;
            int id = i == 0 ? firstId : secondId;
            box(c, sl, y + 8f, sr, y + 8f + h, h / 2f,
                    selected ? 0x305FF3FF : 0x99060914,
                    selected ? 0xCC5FF3FF : UI_LINE);
            textC(c, i == 0 ? first : second, (sl + sr) / 2f, y + 8f + h / 2f,
                    27f, selected ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(id, sl, y + 8f, sr, y + 8f + h);
        }
    }

    void drawVolumeSlider(Canvas c, int id, String label, float value,
                          float rowTop, float l, float r) {
        textC(c, label, l + 36f, rowTop + 30f, 27f, UI_INK, tfMed, Paint.Align.LEFT);
        textC(c, ((int) (value * 100f)) + "%", r - 36f, rowTop + 30f,
                25f, C_CYAN, tfBlack, Paint.Align.RIGHT);
        volumeSliderX1 = l + 56f;
        volumeSliderX2 = r - 56f;
        float sy = rowTop + 70f;
        box(c, volumeSliderX1, sy - 5f, volumeSliderX2, sy + 5f, 5f, 0x33FFFFFF, 0);
        float kx = volumeSliderX1 + (volumeSliderX2 - volumeSliderX1) * value;
        box(c, volumeSliderX1, sy - 5f, kx, sy + 5f, 5f, C_CYAN, 0);
        drawGlow(c, kx, sy, 90f, C_CYAN, 120);
        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(kx, sy, 20f, pFill);
        pFill.setColor(C_CYAN);
        c.drawCircle(kx, sy, 11f, pFill);
        addHit(id, volumeSliderX1 - 40f, sy - 42f, volumeSliderX2 + 40f, sy + 42f);
    }

    /**
     * Итог забега на весь экран: бейдж режима (или рекорда), крупный счёт,
     * прогресс ранга, три плитки статистики, лента наград и кнопки внизу,
     * под большим пальцем. При рекорде за счётом вращаются золотые лучи.
     */
    void drawOver(Canvas c) {
        float appear = clamp(overTime / 0.45f, 0f, 1f);
        float ease = 1f - (1f - appear) * (1f - appear) * (1f - appear);
        pFill.setColor(Color.argb((int) (0xEB * ease), 6, 8, 22));
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);
        int saved = ease < 1f
                ? c.saveLayerAlpha(-100f, -100f, W + 100f, worldH + 100f, (int) (255 * ease))
                : c.save();
        c.translate(0f, (1f - ease) * 60f);

        float top = insetTop;
        float cx = W / 2f;
        float l = UI_PAD;
        float r = W - UI_PAD;
        int mc = modeColor(mode);
        String modeLabel = sModeNames[mode];

        // Фон за счётом: золотые лучи при рекорде, мягкое свечение режима иначе.
        float rcy = top + 330f;
        if (newRecord) {
            float rot = ValueAnimator.areAnimatorsEnabled() ? time * 9f : 0f;
            pShade.setShader(new RadialGradient(cx, rcy, 440f,
                    new int[]{0x38FFD75E, 0x00FFD75E}, null, Shader.TileMode.CLAMP));
            tmpRect.set(cx - 440f, rcy - 440f, cx + 440f, rcy + 440f);
            for (int k = 0; k < 20; k++) {
                c.drawArc(tmpRect, rot + k * 18f, 6f, true, pShade);
            }
            pShade.setShader(null);
        } else {
            drawGlow(c, cx, rcy, 760f, mc, 55);
        }

        // Бейдж: рекорд — золото с кубком, иначе точка и название режима.
        float py = top + 178f;
        String badge = newRecord ? String.format(sNewRecordMode, modeLabel) : modeLabel;
        float bw = measure(badge, 32f, tfBlack) + 110f;
        int bc = newRecord ? C_GOLD : mc;
        box(c, cx - bw / 2f, py - 42f, cx + bw / 2f, py + 42f, 42f,
                withAlpha(bc, 0x29), withAlpha(bc, 0x99));
        if (newRecord) {
            iconTrophy(c, cx - bw / 2f + 50f, py, 17f, C_GOLD);
        } else {
            pFill.setColor(mc);
            c.drawCircle(cx - bw / 2f + 50f, py, 10f, pFill);
        }
        textC(c, badge, cx - bw / 2f + 80f, py, 32f, newRecord ? 0xFFFFE9A6 : UI_INK, tfBlack,
                Paint.Align.LEFT);

        // Счёт «досчитывается» за первую секунду.
        float count = clamp((overTime - 0.15f) / 0.85f, 0f, 1f);
        count = 1f - (1f - count) * (1f - count);
        textGlow(c, fmt((int) (score * count)), cx, top + 400f, 168f,
                newRecord ? 0xFFFFF6D8 : 0xFFFFFFFF,
                newRecord ? 0xE6FFD75E : 0xAA5FF3FF, tfDisplay, 0.2f);
        int best = progress.bestForMode(mode);
        String under = newRecord
                ? (runPrevBest > 0 ? String.format(sRecordWas, fmt(runPrevBest),
                fmt((int) score - runPrevBest)) : sRecordFirst)
                : sBest + "  " + fmt(best);
        text(c, under, cx, top + 462f, 34f, newRecord ? 0xFFC9B98A : UI_FOG, tfBold,
                Paint.Align.CENTER);

        float y = top + 514f;
        y = drawRankCard(c, l, r, y) + 26f;

        // Три плитки статистики.
        float gap = 22f;
        float tw = (r - l - gap * 2f) / 3f;
        float th = 250f;
        String[] values = {formatTime((int) elapsed), fmt(runDodged), "x" + fmtMult(runMaxOverallMult)};
        String[] labels = {sStatName[2], sStatName[0], sStatMaxMult};
        for (int i = 0; i < 3; i++) {
            float tl = l + i * (tw + gap);
            box(c, tl, y, tl + tw, y + th, 44f, 0xBF101634, 0x12FFFFFF);
            float tcx = tl + tw / 2f;
            if (i == 0) {
                iconClock(c, tcx, y + 62f, 20f, UI_FOG);
            } else if (i == 1) {
                iconNearMiss(c, tcx, y + 62f, 20f, 0xFFFF8FE6);
            } else {
                iconSpark(c, tcx, y + 62f, 20f, C_GOLD);
            }
            text(c, values[i], tcx, y + 152f, 54f, UI_INK, tfDisplay, Paint.Align.CENTER);
            text(c, labels[i], tcx, y + 204f, 28f, UI_FOG, tfBold, Paint.Align.CENTER);
        }
        y += th + 34f;

        // Кнопки — снизу вверх, под большим пальцем.
        float menuH = 150f;
        float menuCy = worldH - insetBottom - 40f - menuH / 2f - (1f - ease) * 60f;
        float againH = 184f;
        float againCy = menuCy - menuH / 2f - 22f - againH / 2f;

        // Лента наград: сколько поместится между плитками и кнопками.
        float rowH = 122f;
        float limit = againCy - againH / 2f - 30f;
        for (int i = 0; i < runEvents.size() && y + rowH <= limit; i++) {
            drawRunEvent(c, runEvents.get(i), l, r, y, rowH);
            y += rowH + 20f;
        }

        primaryButton(c, B_AGAIN, cx, againCy, r - l, againH, sPlayAgain, C_CYAN, 1f);
        secondaryButton(c, B_MENU, cx, menuCy, r - l, menuH, sToMenu, 0xFFFFFFFF, UI_INK);
        c.restoreToCount(saved);
    }

    /** Карточка ранга: что было, сколько неона принёс забег, сколько до следующего. */
    float drawRankCard(Canvas c, float l, float r, float y) {
        float h = 236f;
        box(c, l, y, r, y + h, 50f, 0xD1101634, 0x14FFFFFF);
        int rank = progress.rank();
        boolean up = rank > runRankBefore;
        String title = up ? String.format(sRankUp, runRankBefore, rank)
                : String.format(sRankLabel, rank);
        text(c, title, l + 44f, y + 66f, 36f, UI_INK, tfDisplayBold, Paint.Align.LEFT);
        if (runNeonGained > 0) {
            String gain = "+" + fmt((int) Math.min(runNeonGained, Integer.MAX_VALUE));
            float gw = measure(gain, 36f, tfBlack);
            text(c, gain, r - 44f, y + 66f, 36f, 0xFFFFE9A6, tfBlack, Paint.Align.RIGHT);
            iconDiamond(c, r - 44f - gw - 26f, y + 54f, 14f, C_GOLD);
        }

        long lo = GameRules.rankThreshold(rank);
        long hi = GameRules.rankThreshold(rank + 1);
        float span = Math.max(1f, hi - lo);
        float after = clamp((progress.neonEarned - lo) / span, 0f, 1f);
        float before = up ? 0f : clamp((runNeonBefore - lo) / span, 0f, 1f);
        float bl = l + 44f;
        float br = r - 44f;
        float by = y + 112f;
        box(c, bl, by - 11f, br, by + 11f, 11f, 0x14FFFFFF, 0);
        if (before > 0.005f) {
            box(c, bl, by - 11f, bl + (br - bl) * before, by + 11f, 11f, C_CYAN, 0);
        }
        if (after > before + 0.005f) {
            float gs = bl + (br - bl) * before;
            float ge = bl + (br - bl) * after;
            drawGlow(c, (gs + ge) / 2f, by, Math.max(80f, ge - gs), C_GOLD, 70);
            pShade.setShader(new LinearGradient(gs, 0f, ge, 0f, C_GOLD, 0xFFFFB13D,
                    Shader.TileMode.CLAMP));
            tmpRect.set(gs, by - 11f, Math.max(ge, gs + 22f), by + 11f);
            c.drawRoundRect(tmpRect, 11f, 11f, pShade);
            pShade.setShader(null);
        }
        String note = zenMode ? sModeHints[Progress.MODE_ZEN]
                : String.format(sRankToNext, rank + 1,
                fmt((int) Math.min(hi - progress.neonEarned, Integer.MAX_VALUE)));
        text(c, note, l + 44f, y + 182f, 30f, UI_FOG, tfBold, Paint.Align.LEFT);
        return y + h;
    }

    /** Строка ленты наград: значок по типу события, текст и значение справа. */
    void drawRunEvent(Canvas c, RunEvent e, float l, float r, float y, float h) {
        int col;
        switch (e.kind) {
            case RunEvent.ACHIEVEMENT:
                col = C_GOLD;
                break;
            case RunEvent.SKIN:
                col = C_VIOLET;
                break;
            default:
                col = C_CYAN;
                break;
        }
        box(c, l, y, r, y + h, 40f, withAlpha(col, 0x16), withAlpha(col, 0x59));
        float ix = l + 30f + 39f;
        float cy = y + h / 2f;
        strokeStyle(col, 5f);
        c.drawCircle(ix, cy, 36f, pStroke);
        strokeReset();
        if (e.kind == RunEvent.ACHIEVEMENT) {
            iconTrophy(c, ix, cy, 15f, col);
        } else if (e.kind == RunEvent.SKIN) {
            iconOrb(c, ix + 2f, cy, 14f, mix(col, 0xFFFFFFFF, 0.4f));
        } else {
            iconUpgrade(c, ix, cy, 14f, col);
        }
        float tx = ix + 39f + 30f;
        float vw = e.value.length() > 0 ? measure(e.value, 33f, tfBlack) + 30f : 0f;
        String label = e.text;
        float maxW = r - 30f - vw - tx;
        while (label.length() > 4 && measure(label, 34f, tfBold) > maxW) {
            label = label.substring(0, label.length() - 2) + "…";
        }
        textC(c, label, tx, cy, 34f, UI_INK, tfBold, Paint.Align.LEFT);
        if (vw > 0f) {
            textC(c, e.value, r - 30f, cy, 33f, mix(col, 0xFFFFFFFF, 0.4f), tfBlack, Paint.Align.RIGHT);
        }
    }
}
