package com.isaev.neondodge;

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
        pFill.setColor(0xCC050812);
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);

        float l = UI_PAD;
        float r = W - UI_PAD;
        float pw = r - l;
        float inner = pw - 96f;
        float panelH = 830f;
        float pt = Math.max(insetTop + 40f, (worldH - panelH) / 2f);
        box(c, l, pt, r, pt + panelH, 44f, 0xF00E1630, UI_LINE);
        drawGlow(c, W / 2f, pt, pw * 0.9f, C_CYAN, 30);

        float cx = W / 2f;
        float y = pt + 112f;
        text(c, sPause, cx, y, 72f, UI_INK, tfBlack, Paint.Align.CENTER);
        y += 52f;
        updateShownScore();
        text(c, sScore + " " + shownScoreText + "   ·   " + formatTime((int) elapsed),
                cx, y, 27f, UI_FOG, tfMed, Paint.Align.CENTER);
        y += 104f;

        primaryButton(c, B_CONTINUE, cx, y, inner, 136f, sTapContinue, C_CYAN, 0.5f);
        y += 68f + 34f;

        // Плитки звука: заливка циан — включено, перечёркнутая иконка — выключено.
        float gap = 18f;
        float tw = (inner - gap * 2f) / 3f;
        float th = 150f;
        float tl0 = cx - inner / 2f;
        int[] ids = {B_MUSIC, B_SOUND, B_VIBRATION};
        String[] labels = {sMusic, sSound, sVibration};
        boolean[] on = {musicOn, soundOn, vibOn};
        for (int i = 0; i < 3; i++) {
            float tl = tl0 + i * (tw + gap);
            float tcx = tl + tw / 2f;
            boolean down = pressed(ids[i]);
            float in = down ? 4f : 0f;
            box(c, tl + in, y + in, tl + tw - in, y + th - in, 30f,
                    on[i] ? 0x265FF3FF : 0x10FFFFFF, on[i] ? 0x885FF3FF : UI_LINE);
            int ic = on[i] ? 0xFFBFF6FF : UI_DIM;
            float iy = y + 58f;
            if (i == 0) {
                iconNote(c, tcx, iy, 22f, ic);
            } else if (i == 1) {
                iconSpeaker(c, tcx, iy, 22f, ic);
            } else {
                iconVibrate(c, tcx, iy, 22f, ic);
            }
            if (!on[i]) {
                iconSlash(c, tcx, iy, 24f, ic);
            }
            textC(c, labels[i], tcx, y + 112f, 24f, on[i] ? UI_INK : UI_FOG,
                    tfCond, Paint.Align.CENTER);
            addHit(ids[i], tl, y, tl + tw, y + th);
        }
        y += th + 30f + 56f;

        secondaryButton(c, B_PAUSE_RULES, cx, y, inner, 112f, sRulesButton,
                C_CYAN, UI_INK);
        y += 56f + 22f + 50f;

        boolean armed = exitArmed > 0f;
        boolean down = pressed(B_EXIT);
        box(c, cx - inner / 2f, y - 50f, cx + inner / 2f, y + 50f, 50f,
                down ? 0x22FF4F6A : (armed ? 0x18FF4F6A : 0),
                armed ? 0xAAFF4F6A : 0);
        textC(c, armed ? sExitConfirm : sToMenu, cx, y, armed ? 27f : 30f,
                armed ? 0xFFFF8A9C : UI_FOG, tfCond, Paint.Align.CENTER);
        addHit(B_EXIT, cx - inner / 2f, y - 50f, cx + inner / 2f, y + 50f);
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

        sliderX1 = l + 56f;
        sliderX2 = r - 56f;
        float sly = ry + 66f;
        box(c, sliderX1, sly - 5f, sliderX2, sly + 5f, 5f,
                offsetEnabled ? 0x33FFFFFF : 0x14FFFFFF, 0);
        float kx = sliderX1 + (sliderX2 - sliderX1)
                * (fingerOffset - OFFSET_MIN) / (OFFSET_MAX - OFFSET_MIN);
        if (kx > sliderX1 + 1f) {
            box(c, sliderX1, sly - 5f, kx, sly + 5f, 5f,
                    offsetEnabled ? C_CYAN : 0x335FF3FF, 0);
        }
        if (offsetEnabled) {
            drawGlow(c, kx, sly, draggingOffset ? 130f : 90f, C_CYAN, 120);
            pFill.setColor(0xFFFFFFFF);
            c.drawCircle(kx, sly, draggingOffset ? 24f : 20f, pFill);
            pFill.setColor(C_CYAN);
            c.drawCircle(kx, sly, 11f, pFill);
            addHit(B_SLIDER, sliderX1 - 40f, sly - 56f, sliderX2 + 40f, sly + 56f);
        } else {
            pFill.setColor(0x55FFFFFF);
            c.drawCircle(kx, sly, 15f, pFill);
        }
        drawWrapped(c, hint, l + 36f, sly + 70f, 24f, r - l - 72f,
                offsetEnabled ? UI_FOG : 0xCC7CF9D0, tfMed, Paint.Align.LEFT, 34f);
        y += ch + 32f;

        y = sectionLabel(c, sSoundSection, y);
        float rowH = 104f;
        card(c, l, y, r, y + rowH * 3f, 0, false);
        int[] ids = {B_MUSIC, B_SOUND, B_VIBRATION};
        String[] labels = {sMusic, sSound, sVibration};
        boolean[] on = {musicOn, soundOn, vibOn};
        for (int i = 0; i < 3; i++) {
            float rt = y + i * rowH;
            float rcy = rt + rowH / 2f;
            if (i > 0) {
                pFill.setColor(UI_LINE);
                c.drawRect(l + 36f, rt - 1f, r - 36f, rt + 1f, pFill);
            }
            if (pressed(ids[i])) {
                box(c, l + 4f, rt + 4f, r - 4f, rt + rowH - 4f, 24f, 0x12FFFFFF, 0);
            }
            textC(c, labels[i], l + 36f, rcy, 29f, UI_INK, tfMed, Paint.Align.LEFT);
            switchToggle(c, r - 84f, rcy, on[i]);
            addHit(ids[i], l, rt, r, rt + rowH);
        }
        y += rowH * 3f + 32f;

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

    void drawOver(Canvas c) {
        float appear = clamp(overTime / 0.45f, 0f, 1f);
        float ease = 1f - (1f - appear) * (1f - appear) * (1f - appear);
        pFill.setColor(Color.argb((int) (0xCC * ease), 5, 8, 18));
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);

        float l = UI_PAD;
        float r = W - UI_PAD;
        float pw = r - l;
        float inner = pw - 96f;
        float panelH = 1000f;
        float pt = Math.max(insetTop + 30f, (worldH - panelH) / 2f) + (1f - ease) * 80f;
        int saved = ease < 1f
                ? c.saveLayerAlpha(-100f, -100f, W + 100f, worldH + 100f, (int) (255 * ease))
                : c.save();
        box(c, l, pt, r, pt + panelH, 44f, 0xF00E1630, newRecord ? 0x88FFD75E : UI_LINE);
        drawGlow(c, W / 2f, pt, pw * 0.9f, newRecord ? C_GOLD : C_MAGENTA, 34);

        float cx = W / 2f;
        float y = pt + 100f;
        if (newRecord) {
            float pulse = 0.75f + 0.25f * (float) Math.sin(time * 5f);
            textGlow(c, sNewRecord, cx, y, 60f, 0xFFFFF2C4,
                    Color.argb((int) (230f * pulse), 255, 215, 94), tfBlack);
        } else {
            text(c, sGameOver, cx, y, 54f, UI_INK, tfBlack, Paint.Align.CENTER);
        }
        y += 58f;

        String modeLabel = dailyChallenge ? sDailyChallenge : sModeNames[mode];
        int mc = dailyChallenge ? C_MAGENTA : modeColor(mode);
        float mw = measure(modeLabel, 24f, tfCond) + 74f;
        box(c, cx - mw / 2f, y - 24f, cx + mw / 2f, y + 24f, 24f, withAlpha(mc, 0x22), 0);
        pFill.setColor(mc);
        c.drawCircle(cx - mw / 2f + 28f, y, 7f, pFill);
        textC(c, modeLabel, cx + 12f, y, 24f, UI_INK, tfCond, Paint.Align.CENTER);
        y += 170f;

        // Счёт «досчитывается» за первую секунду — единственная анимация экрана.
        float count = clamp((overTime - 0.15f) / 0.85f, 0f, 1f);
        count = 1f - (1f - count) * (1f - count);
        textGlow(c, fmt((int) (score * count)), cx, y, 156f, 0xFFFFFFFF,
                newRecord ? 0xCCFFD75E : 0xAA5FF3FF, tfBlack);
        y += 58f;
        int best = dailyChallenge ? progress.challengeBest : progress.bestForMode(mode);
        text(c, sBest + "  " + fmt(best), cx, y, 28f, UI_FOG, tfMed, Paint.Align.CENTER);
        y += 46f;

        String[] values = {
                fmt(runStars),
                fmt(runDodged),
                formatTime((int) elapsed),
                "x" + runMaxCombo,
                "x" + fmtMult(runMaxMult) + " · x" + fmtMult(runMaxCloseMult),
                zenMode ? "—" : "+" + fmt((int) score)
        };
        String[] labels = {sStatName[3], sStatName[0], sStatName[2],
                sCombo, sMultiplier, sNeon};
        float gap = 14f;
        float tw = (inner - gap * 2f) / 3f;
        float th = 120f;
        float gl = cx - inner / 2f;
        for (int i = 0; i < 6; i++) {
            float tl = gl + (i % 3) * (tw + gap);
            float tt = y + (i / 3) * (th + gap);
            box(c, tl, tt, tl + tw, tt + th, 24f, 0x0FFFFFFF, 0);
            boolean neon = i == 5;
            text(c, values[i], tl + tw / 2f, tt + 60f, 36f,
                    neon && !zenMode ? 0xFFFFE9A6 : UI_INK, tfBlack, Paint.Align.CENTER);
            text(c, labels[i], tl + tw / 2f, tt + 96f, 21f, UI_FOG, tfMed,
                    Paint.Align.CENTER);
        }
        y += th * 2f + gap + 34f + 66f;

        primaryButton(c, B_AGAIN, cx, y, inner, 132f, sPlayAgain, C_CYAN, 0.5f);
        y += 66f + 20f + 56f;
        secondaryButton(c, B_MENU, cx, y, inner, 112f, sToMenu, C_CYAN, UI_INK);
        c.restoreToCount(saved);
    }
}
