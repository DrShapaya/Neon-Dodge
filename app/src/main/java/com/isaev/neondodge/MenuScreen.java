package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

/** Главное меню и общий каркас вложенных экранов. */
abstract class MenuScreen extends HudLayer {

    MenuScreen(Context context) {
        super(context);
    }

    /** Меню раскладывается снизу вверх: кнопки у большого пальца, сцена сверху. */
    void layoutMenu() {
        float bottom = worldH - insetBottom - 36f;
        mNavCy = bottom - 78f;
        float navTop = mNavCy - 78f;
        mDailyCy = navTop - 28f - 58f;
        float dailyTop = mDailyCy - 58f;
        mPlayCy = dailyTop - 24f - 74f;
        float playTop = mPlayCy - 74f;
        mInfoY = playTop - 34f;
        mSegCy = mInfoY - 46f - 48f;
        float segTop = mSegCy - 48f;
        mTopBarY = insetTop + 76f;
        float free = segTop - (mTopBarY + 48f);
        mLogoSize = free > 820f ? 150f : (free > 620f ? 126f : 100f);
        float logoBlock = mLogoSize * 1.9f + 60f;
        float heroSpace = Math.max(0f, free - logoBlock);
        mLogoY = mTopBarY + 48f + Math.max(24f, heroSpace * 0.22f) + mLogoSize * 0.80f;
        float logoBottom = mLogoY + mLogoSize * 0.92f + 60f;
        mHeroY = (logoBottom + segTop) / 2f;
    }

    void drawMenuDecor(Canvas c) {
        float h = mHeroY;
        float spread = Math.max(120f, Math.min(260f, (mSegCy - mLogoY) * 0.30f));
        drawHex(c, W * 0.16f, h - spread * 0.55f + (float) Math.sin(time * 0.8f) * 16f,
                62f, time * 24f, C_MAGENTA);
        drawHex(c, W * 0.86f, h - spread * 1.05f + (float) Math.sin(time * 0.6f + 1.4f) * 20f,
                44f, -time * 18f, C_ORANGE);
        drawHex(c, W * 0.80f, h + spread * 0.55f + (float) Math.sin(time * 0.7f + 2.2f) * 14f,
                30f, time * 30f, C_LIME);
        float sy = h + (float) Math.sin(time * 2f) * 10f;
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(0x285FF3FF);
        tmpRect.set(W / 2f - 190f, sy - 46f, W / 2f + 190f, sy + 46f);
        c.drawOval(tmpRect, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        drawPlayer(c, W / 2f, sy);
        drawParticles(c);
    }

    void drawMenu(Canvas c) {
        float cx = W / 2f;
        float l = UI_PAD;
        float r = W - UI_PAD;

        // Верхняя строка: неон (ведёт в скины) и настройки.
        String neon = fmt(progress.pointsTotal);
        float nw = measure(neon, 30f, tfBlack) + 104f;
        boolean nDown = pressed(B_WORKSHOP);
        box(c, l, mTopBarY - 36f, l + nw, mTopBarY + 36f, 36f,
                nDown ? UI_SURFACE_HI : 0xB30E1630, 0x55FFD75E);
        drawGlow(c, l + 40f, mTopBarY, 80f, C_GOLD, 90);
        iconDiamond(c, l + 40f, mTopBarY, 17f, C_GOLD);
        textC(c, neon, l + 70f, mTopBarY, 30f, 0xFFFFE9A6, tfBlack, Paint.Align.LEFT);
        addHit(B_WORKSHOP, l, mTopBarY - 44f, l + nw, mTopBarY + 44f);

        float gx = r - 40f;
        boolean gDown = pressed(B_SETTINGS);
        pFill.setColor(gDown ? UI_SURFACE_HI : 0xB30E1630);
        c.drawCircle(gx, mTopBarY, 40f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(UI_LINE);
        c.drawCircle(gx, mTopBarY, 40f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        iconGear(c, gx, mTopBarY, 20f, 0xDDDFF5FF);
        addHit(B_SETTINGS, gx - 56f, mTopBarY - 56f, gx + 56f, mTopBarY + 56f);

        // Логотип: два слова, второе — магента, как у сферы и астероида.
        textGlow(c, "NEON", cx, mLogoY, mLogoSize, 0xFFFFFFFF, C_CYAN, tfBlack);
        textGlow(c, "DODGE", cx, mLogoY + mLogoSize * 0.92f, mLogoSize,
                0xFFFFECFB, C_MAGENTA, tfBlack);
        text(c, sTagline, cx, mLogoY + mLogoSize * 0.92f + 58f, 28f,
                0x99CFE8F5, tfMed, Paint.Align.CENTER);

        // Выбор режима — сегменты в цвете режима.
        float segH = 96f;
        box(c, l, mSegCy - segH / 2f, r, mSegCy + segH / 2f, segH / 2f, 0xCC0A1226, UI_LINE);
        float segW = (r - l - 12f) / Progress.MODE_COUNT;
        for (int i = 0; i < Progress.MODE_COUNT; i++) {
            float sl = l + 6f + i * segW;
            float sr = sl + segW;
            boolean sel = i == mode;
            int mc = modeColor(i);
            if (sel) {
                box(c, sl, mSegCy - segH / 2f + 6f, sr, mSegCy + segH / 2f - 6f,
                        (segH - 12f) / 2f, withAlpha(mc, 0x30), withAlpha(mc, 0xD0));
            } else if (pressed(B_MODE + i)) {
                box(c, sl, mSegCy - segH / 2f + 6f, sr, mSegCy + segH / 2f - 6f,
                        (segH - 12f) / 2f, 0x14FFFFFF, 0);
            }
            textC(c, sModeNames[i], (sl + sr) / 2f, mSegCy, 30f,
                    sel ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(B_MODE + i, sl, mSegCy - segH / 2f, sr, mSegCy + segH / 2f);
        }

        text(c, sModeHints[mode], l + 8f, mInfoY, 25f, UI_FOG, tfMed, Paint.Align.LEFT);
        String best = sBest + "  " + fmt(progress.bestForMode(mode));
        text(c, best, r - 8f, mInfoY, 27f, UI_INK, tfBold, Paint.Align.RIGHT);

        float breathe = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
        drawStartZone(c, B_PLAY, cx, mPlayCy, r - l, 148f, sTapStart, breathe);

        drawDailyCard(c, l, r);
        drawNav(c, l, r);
    }

    void drawStartZone(Canvas c, int id, float cx, float cy, float w, float h,
                               String label, float breathe) {
        boolean down = pressed(id);
        float in = down ? 5f : 0f;
        drawGlow(c, cx, cy, h * 2.8f, C_CYAN, (int) (72f + 38f * breathe));
        box(c, cx - w / 2f + in, cy - h / 2f + in,
                cx + w / 2f - in, cy + h / 2f - in, 42f,
                down ? 0xFF35B7D0 : 0xFF5FF3FF, 0xAAE8FFFF);
        iconPlay(c, cx - 220f, cy, 28f, UI_ON_ACCENT);
        textC(c, label, cx + 26f, cy, 42f, UI_ON_ACCENT, tfBlack, Paint.Align.CENTER);
        // The whole zone is interactive, including the label and empty margins.
        addHit(id, cx - w / 2f - 12f, cy - h / 2f - 12f,
                cx + w / 2f + 12f, cy + h / 2f + 12f);
    }

    void drawDailyCard(Canvas c, float l, float r) {
        boolean done = progress.challengeDone;
        float t = mDailyCy - 58f;
        float b = mDailyCy + 58f;
        boolean down = pressed(B_DAILY);
        card(c, l, t, r, b, done ? 0x66FF4FD8 : C_MAGENTA, false);
        if (down) {
            box(c, l, t, r, b, 28f, 0x18FFFFFF, 0);
        }
        float ix = l + 74f;
        if (!done) {
            drawGlow(c, ix, mDailyCy, 120f, C_MAGENTA, 100);
        }
        // Четырёхлучевая вспышка — «событие дня».
        iconPath.reset();
        float s = 22f;
        iconPath.moveTo(ix, mDailyCy - s);
        iconPath.quadTo(ix, mDailyCy, ix + s, mDailyCy);
        iconPath.quadTo(ix, mDailyCy, ix, mDailyCy + s);
        iconPath.quadTo(ix, mDailyCy, ix - s, mDailyCy);
        iconPath.quadTo(ix, mDailyCy, ix, mDailyCy - s);
        iconPath.close();
        pFill.setColor(done ? 0x88FF4FD8 : C_MAGENTA);
        c.drawPath(iconPath, pFill);

        text(c, sDailyChallenge, l + 124f, mDailyCy - 6f, 30f,
                done ? UI_FOG : UI_INK, tfBold, Paint.Align.LEFT);
        String sub = done
                ? sDailyDone + " · " + fmt(progress.challengeBest)
                : sDailyHint;
        text(c, sub, l + 124f, mDailyCy + 32f, 23f,
                done ? UI_DIM : 0xCCFF9FE9, tfMed, Paint.Align.LEFT);
        if (done) {
            iconCheck(c, r - 50f, mDailyCy, 18f, C_MINT, 5f);
        } else {
            iconChevron(c, r - 48f, mDailyCy, 14f, 0xCCFF9FE9, false);
        }
        addHit(B_DAILY, l, t, r, b);
    }

    void drawNav(Canvas c, float l, float r) {
        float gap = 18f;
        float tw = (r - l - gap * 3f) / 4f;
        float th = 156f;
        int[] ids = {B_QUESTS, B_SKINS, B_STATS, B_RULES};
        String[] labels = {sQuests, sSkins, sStats, sRulesButton};
        int done = 0;
        for (int i = 0; i < 3; i++) {
            if (progress.questDone[i]) {
                done++;
            }
        }
        for (int i = 0; i < 4; i++) {
            float tl = l + i * (tw + gap);
            float tr = tl + tw;
            float tcx = (tl + tr) / 2f;
            boolean down = pressed(ids[i]);
            float in = down ? 4f : 0f;
            box(c, tl + in, mNavCy - th / 2f + in, tr - in, mNavCy + th / 2f - in, 30f,
                    down ? UI_SURFACE_HI : UI_SURFACE, UI_LINE);
            float iy = mNavCy - 24f;
            int ic = 0xFFBFEFFF;
            switch (i) {
                case 0:
                    iconFlag(c, tcx, iy, 24f, ic);
                    break;
                case 1:
                    iconOrb(c, tcx, iy, 26f, ic);
                    break;
                case 2:
                    iconBars(c, tcx, iy, 22f, ic);
                    break;
                default:
                    iconDoc(c, tcx, iy, 24f, ic);
                    break;
            }
            textC(c, labels[i], tcx, mNavCy + 42f, 25f, 0xFFCFE3F0, tfCond, Paint.Align.CENTER);
            addHit(ids[i], tl, mNavCy - th / 2f, tr, mNavCy + th / 2f);

            if (i == 0) {
                // Бейдж заданий: золото — есть что делать, мята — всё сделано.
                String badge = done + "/3";
                float bw = measure(badge, 21f, tfBlack) + 26f;
                float bx = tr - 14f - bw;
                float by = mNavCy - th / 2f + 16f;
                boolean all = done >= 3;
                box(c, bx, by, bx + bw, by + 36f, 18f, all ? C_MINT : C_GOLD, 0);
                textC(c, badge, bx + bw / 2f, by + 18f, 21f, UI_ON_ACCENT, tfBlack,
                        Paint.Align.CENTER);
            }
        }
    }

    void drawWorkshop(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sWorkshop);
        float l = UI_PAD;
        float r = W - UI_PAD;
        float y = top + 48f;
        int level = progress.multiplierUpgradeLevel;
        float step = progress.starMultiplierStep();
        card(c, l, y, r, y + 240f, C_GOLD, true);
        text(c, sWorkshopLevel + " " + level + "/4", l + 42f, y + 58f,
                34f, UI_INK, tfBlack, Paint.Align.LEFT);
        text(c, "ЗВЁЗДЫ  +" + fmtMult(step), l + 42f, y + 112f,
                46f, C_GOLD, tfBlack, Paint.Align.LEFT);
        text(c, sWorkshopHint, l + 42f, y + 164f, 24f,
                UI_FOG, tfMed, Paint.Align.LEFT);
        progressBar(c, l + 42f, r - 42f, y + 208f, 10f, level / 4f, C_GOLD);
        y += 276f;

        if (level < 4) {
            int cost = progress.multiplierUpgradeCost();
            card(c, l, y, r, y + 170f, C_CYAN, false);
            text(c, sWorkshopNext + " +" + fmtMult(0.2f + level * 0.1f),
                    l + 42f, y + 54f, 30f, UI_INK, tfBold, Paint.Align.LEFT);
            text(c, fmt(progress.pointsTotal) + " / " + fmt(cost) + "  НЕОН",
                    l + 42f, y + 98f, 27f,
                    progress.canUpgradeMultiplier() ? C_MINT : UI_FOG,
                    tfMed, Paint.Align.LEFT);
            primaryButton(c, B_WORKSHOP_UPGRADE, r - 150f, y + 120f, 260f, 82f,
                    sWorkshopBuy, C_CYAN, 0.2f);
        } else {
            card(c, l, y, r, y + 170f, C_MINT, false);
            textC(c, sWorkshopMax, W / 2f, y + 82f, 32f, C_MINT,
                    tfBlack, Paint.Align.CENTER);
        }
    }

    void drawScreenBg(Canvas c) {
        pFill.setColor(0xF0060914);
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);
    }

    /** Кнопка «назад», заголовок; возвращает верх области содержимого. */
    float drawHeader(Canvas c, String title) {
        float cy = insetTop + 84f;
        float bx = UI_PAD + 42f;
        boolean down = pressed(B_BACK);
        pFill.setColor(down ? UI_SURFACE_HI : UI_SURFACE);
        c.drawCircle(bx, cy, 42f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(UI_LINE);
        c.drawCircle(bx, cy, 42f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        iconChevron(c, bx - 3f, cy, 14f, UI_INK, true);
        addHit(B_BACK, bx - 60f, cy - 60f, bx + 60f, cy + 60f);
        text(c, title, bx + 70f, cy + 18f, 50f, UI_INK, tfBlack, Paint.Align.LEFT);
        return cy + 70f;
    }

    void drawNeonPill(Canvas c, float right, float cy) {
        String neon = fmt(progress.pointsTotal);
        float nw = measure(neon, 27f, tfBlack) + 88f;
        box(c, right - nw, cy - 32f, right, cy + 32f, 32f, 0xB30E1630, 0x55FFD75E);
        iconDiamond(c, right - nw + 36f, cy, 15f, C_GOLD);
        textC(c, neon, right - nw + 62f, cy, 27f, 0xFFFFE9A6, tfBlack, Paint.Align.LEFT);
    }

    float beginScroll(Canvas c, float top) {
        scrollTop = top;
        scrollBottom = worldH - insetBottom - 6f;
        c.save();
        c.clipRect(0f, scrollTop, W, scrollBottom);
        clipping = true;
        return top + 20f - scrollY;
    }

    void endScroll(Canvas c, float y) {
        c.restore();
        clipping = false;
        float contentBottom = y + scrollY;
        scrollMax = Math.max(0f, contentBottom + 40f - scrollBottom);
        if (scrollY > scrollMax) {
            scrollY = scrollMax;
            scrollVel = 0f;
        }
        if (scrollY > 1f) {
            pFill.setColor(UI_LINE);
            c.drawRect(0f, scrollTop, W, scrollTop + 2f, pFill);
        }
        if (scrollMax > 0f) {
            float trackTop = scrollTop + 12f;
            float trackBottom = scrollBottom - 12f;
            float span = trackBottom - trackTop;
            float thumbH = Math.max(70f, span * span / (span + scrollMax));
            float thumbTop = trackTop + (span - thumbH) * (scrollY / scrollMax);
            box(c, W - 14f, thumbTop, W - 8f, thumbTop + thumbH, 3f,
                    scrollDragging || scrollVel != 0f ? 0xAA5FF3FF : 0x445FF3FF, 0);
        }
    }

    /** Строка-ссылка внутри экрана: карточка с подписью, счётчиком и шевроном. */
    float linkRow(Canvas c, int id, String label, String value, float y) {
        float l = UI_PAD;
        float r = W - UI_PAD;
        float h = 104f;
        boolean down = pressed(id);
        card(c, l, y, r, y + h, 0, false);
        if (down) {
            box(c, l, y, r, y + h, 28f, 0x18FFFFFF, 0);
        }
        textC(c, label, l + 36f, y + h / 2f, 30f, UI_INK, tfBold, Paint.Align.LEFT);
        if (value.length() > 0) {
            textC(c, value, r - 76f, y + h / 2f, 28f, UI_FOG, tfBold, Paint.Align.RIGHT);
        }
        iconChevron(c, r - 40f, y + h / 2f, 13f, UI_FOG, false);
        addHit(id, l, y, r, y + h);
        return y + h;
    }
}
