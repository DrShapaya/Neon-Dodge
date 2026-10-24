package com.isaev.neondodge;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

/** Главное меню и общий каркас вложенных экранов. */
abstract class MenuScreen extends HudLayer {

    MenuScreen(Context context) {
        super(context);
    }

    /**
     * Меню раскладывается снизу вверх: у большого пальца — игра, над ней —
     * сцена, где разделы висят на орбите вокруг сферы, наверху — вывеска.
     */
    void layoutMenu() {
        float bottom = worldH - insetBottom - 28f;
        mDailyCy = bottom - 44f;
        float dailyTop = mDailyCy - 44f;
        mPlayCy = dailyTop - 20f - 66f;
        float playTop = mPlayCy - 66f;
        mInfoY = playTop - 32f;
        mSegCy = mInfoY - 44f - 40f;
        float segTop = mSegCy - 40f;
        mTopBarY = insetTop + 76f;
        float free = segTop - (mTopBarY + 48f);
        // Орбите нужно место под кнопки с подписями; логотип уступает ей.
        mLogoSize = free > 1180f ? 168f : (free > 1000f ? 140f : 110f);
        float logoBlock = mLogoSize * 1.9f + 60f;
        float heroSpace = Math.max(0f, free - logoBlock);
        mOrbitRy = clamp((heroSpace - 200f) / 2f, 120f, 220f);
        mOrbitRx = 360f;
        mLogoY = mTopBarY + 48f + Math.max(16f, (heroSpace - 2f * mOrbitRy - 200f) * 0.25f)
                + mLogoSize * 0.80f;
        float logoBottom = mLogoY + mLogoSize * 0.95f + 62f;
        // Центр орбиты: верхние кнопки не залезают на подпись логотипа,
        // нижняя с подписью — на вкладки режимов.
        float lo = logoBottom + mOrbitRy * 0.5f + 80f;
        float hi = segTop - mOrbitRy - 120f;
        mHeroY = Math.min(hi, Math.max(lo, (logoBottom + segTop) / 2f - 20f));
    }

    /** Точка на орбите под углом deg (0° — справа, 90° — снизу). */
    float orbitX(float deg) {
        return W / 2f + mOrbitRx * (float) Math.cos(Math.toRadians(deg));
    }

    float orbitY(float deg) {
        return mHeroY + mOrbitRy * (float) Math.sin(Math.toRadians(deg));
    }

    void drawMenuDecor(Canvas c) {
        float h = mHeroY;
        // Орбита: эллипс, на котором висят кнопки разделов, и бегущая искра.
        strokeStyle(0x225FF3FF, 2f);
        tmpRect.set(W / 2f - mOrbitRx, h - mOrbitRy, W / 2f + mOrbitRx, h + mOrbitRy);
        c.drawOval(tmpRect, pStroke);
        strokeStyle(0x105FF3FF, 2f);
        tmpRect.set(W / 2f - mOrbitRx * 0.55f, h - mOrbitRy * 0.55f,
                W / 2f + mOrbitRx * 0.55f, h + mOrbitRy * 0.55f);
        c.drawOval(tmpRect, pStroke);
        strokeReset();
        float spark = time * 22f;
        float sx = orbitX(spark);
        float sy = orbitY(spark);
        drawGlow(c, sx, sy, 60f, C_CYAN, 140);
        pFill.setColor(0xFFDFFBFF);
        c.drawCircle(sx, sy, 4f, pFill);

        drawPlayer(c, W / 2f, h + (float) Math.sin(time * 2f) * 8f);
        drawParticles(c);
    }

    /**
     * Яркость трубок логотипа. При запуске вывеска «зажигается» с мерцанием —
     * единственная самостоятельная анимация меню; без системных анимаций
     * логотип сразу горит ровно.
     */
    float logoPower(float delay) {
        if (!ValueAnimator.areAnimatorsEnabled()) {
            return 1f;
        }
        float t = time - delay;
        if (t <= 0.15f) {
            return 0f;
        }
        if (t >= 1.25f) {
            return 1f;
        }
        // Псевдослучайные вспышки, к концу трубка горит всё чаще.
        float n = (float) Math.sin(Math.floor(t * 18f) * 12.9898f) * 43758.547f;
        n -= (float) Math.floor(n);
        return n < 0.25f + t * 0.6f ? 1f : 0.08f;
    }

    void drawMenu(Canvas c) {
        float cx = W / 2f;
        float l = UI_PAD;
        float r = W - UI_PAD;

        // Верхняя строка: баланс неона (ведёт в мастерскую) и настройки.
        String neon = fmt(progress.pointsTotal);
        float nw = measure(neon, 30f, tfBlack) + 100f;
        boolean nDown = pressed(B_WORKSHOP);
        box(c, l, mTopBarY - 36f, l + nw, mTopBarY + 36f, 36f,
                nDown ? UI_SURFACE_HI : UI_SURFACE, 0x44FFD75E);
        iconDiamond(c, l + 40f, mTopBarY, 16f, C_GOLD);
        textC(c, neon, l + 68f, mTopBarY, 30f, 0xFFFFE9A6, tfBlack, Paint.Align.LEFT);
        addHit(B_WORKSHOP, l, mTopBarY - 44f, l + nw, mTopBarY + 44f);

        float gx = r - 38f;
        roundButton(c, B_SETTINGS, gx, mTopBarY, 38f);
        iconGear(c, gx, mTopBarY, 19f, 0xFFDFF5FF);

        // Логотип — неоновая вывеска: два слова двумя цветами трубок.
        neonText(c, "NEON", cx, mLogoY, mLogoSize, C_CYAN, logoPower(0f), tfBold);
        neonText(c, "DODGE", cx, mLogoY + mLogoSize * 0.95f, mLogoSize, C_MAGENTA,
                logoPower(0.35f), tfBold);
        text(c, sTagline, cx, mLogoY + mLogoSize * 0.95f + 62f, 27f,
                0x99CFE8F5, tfMed, Paint.Align.CENTER);

        drawOrbitNav(c);

        // Выбор режима — вкладки; под выбранной горит трубка в цвете режима.
        float segW = (r - l) / Progress.MODE_COUNT;
        float lineY = mSegCy + 34f;
        pFill.setColor(UI_LINE);
        c.drawRect(l, lineY - 1f, r, lineY + 1f, pFill);
        for (int i = 0; i < Progress.MODE_COUNT; i++) {
            float sl = l + i * segW;
            float sr = sl + segW;
            float scx = (sl + sr) / 2f;
            boolean sel = i == mode;
            if (!sel && pressed(B_MODE + i)) {
                box(c, sl + 8f, mSegCy - 36f, sr - 8f, mSegCy + 26f, 20f, 0x12FFFFFF, 0);
            }
            textC(c, sModeNames[i], scx, mSegCy - 4f, 31f,
                    sel ? UI_INK : UI_FOG, tfBold, Paint.Align.CENTER);
            if (sel) {
                float tw = measure(sModeNames[i], 31f, tfBold) / 2f + 18f;
                tube(c, scx - tw, lineY, scx + tw, lineY, modeColor(i), 5f);
            }
            addHit(B_MODE + i, sl, mSegCy - 40f, sr, mSegCy + 40f);
        }

        text(c, sModeHints[mode], l + 4f, mInfoY, 25f, UI_FOG, tfMed, Paint.Align.LEFT);
        String bestValue = fmt(progress.bestForMode(mode));
        float bvw = measure(bestValue, 28f, tfBlack);
        text(c, bestValue, r - 4f, mInfoY, 28f, UI_INK, tfBlack, Paint.Align.RIGHT);
        text(c, sBest, r - 16f - bvw, mInfoY, 25f, UI_FOG, tfMed, Paint.Align.RIGHT);

        float breathe = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
        primaryButton(c, B_PLAY, cx, mPlayCy, r - l, 132f, sTapStart, C_CYAN, breathe);

        drawDailyCard(c, l, r);
    }

    /**
     * Разделы на орбите: у каждого свой цвет трубки. Кнопки стоят на месте —
     * по орбите бежит только искра, чтобы цели нажатия не уезжали из-под пальца.
     */
    void drawOrbitNav(Canvas c) {
        int done = 0;
        for (int i = 0; i < 3; i++) {
            if (progress.questDone[i]) {
                done++;
            }
        }
        int level = progress.multiplierUpgradeLevel;
        boolean ready = progress.canUpgradeMultiplier();
        String wsStatus = ready ? sWorkshopReady : String.format(sWorkshopLevelOf, level, 4);
        orbitButton(c, B_WORKSHOP, orbitX(-150f), orbitY(-150f), C_GOLD, 0,
                sWorkshop, wsStatus, ready ? C_GOLD : 0);
        orbitButton(c, B_QUESTS, orbitX(-30f), orbitY(-30f), C_MINT, 1,
                sQuests, done + " / 3", done < 3 ? C_GOLD : 0);
        orbitButton(c, B_SKINS, orbitX(150f), orbitY(150f), C_VIOLET, 2,
                sSkins, "", 0);
        orbitButton(c, B_STATS, orbitX(30f), orbitY(30f), C_CYAN, 3,
                sStats, "", 0);
        orbitButton(c, B_RULES, orbitX(90f), orbitY(90f), C_STEEL, 4,
                sRulesButton, "", 0);
    }

    /** Круглая неоновая кнопка раздела: кольцо-трубка, иконка, подпись и статус. */
    void orbitButton(Canvas c, int id, float x, float y, int color, int icon,
                     String label, String status, int signal) {
        float rr = 52f;
        boolean down = pressed(id);
        float s = down ? 0.94f : 1f;
        float ring = rr * s;
        pFill.setColor(down ? withAlpha(color, 0x38) : 0xE00B1128);
        c.drawCircle(x, y, ring, pFill);
        strokeStyle(withAlpha(color, down ? 0x55 : 0x2E), 12f);
        c.drawCircle(x, y, ring, pStroke);
        strokeStyle(color, 3f);
        c.drawCircle(x, y, ring, pStroke);
        strokeStyle(withAlpha(mix(color, 0xFFFFFFFF, 0.75f), 0xAA), 1f);
        c.drawCircle(x, y, ring, pStroke);
        strokeReset();

        int ic = mix(color, 0xFFFFFFFF, 0.55f);
        switch (icon) {
            case 0:
                iconUpgrade(c, x, y, 18f, ic);
                break;
            case 1:
                iconFlag(c, x + 2f, y, 19f, ic);
                break;
            case 2:
                iconOrb(c, x + 2f, y, 18f, ic);
                break;
            case 3:
                iconBars(c, x, y, 18f, ic);
                break;
            default:
                iconDoc(c, x, y, 18f, ic);
                break;
        }
        if (signal != 0) {
            // Сигнал: здесь есть что сделать.
            float dx = x + rr * 0.72f;
            float dy = y - rr * 0.72f;
            pFill.setColor(0xFF0B1128);
            c.drawCircle(dx, dy, 12f, pFill);
            pFill.setColor(signal);
            c.drawCircle(dx, dy, 8f, pFill);
        }

        text(c, label, x, y + rr + 38f, 25f, UI_INK, tfBold, Paint.Align.CENTER);
        float bottom = y + rr + 48f;
        if (status.length() > 0) {
            text(c, status, x, y + rr + 68f, 21f, signal == C_GOLD && id == B_WORKSHOP
                    ? C_GOLD : UI_FOG, tfMed, Paint.Align.CENTER);
            bottom = y + rr + 78f;
        }
        addHit(id, x - 120f, y - rr - 14f, x + 120f, bottom);
    }

    void drawDailyCard(Canvas c, float l, float r) {
        boolean done = progress.challengeDone;
        float t = mDailyCy - 44f;
        float b = mDailyCy + 44f;
        boolean down = pressed(B_DAILY);
        box(c, l, t, r, b, 44f, down ? UI_SURFACE_HI : UI_SURFACE,
                done ? UI_LINE : 0x55FF4FD8);
        float ix = l + 50f;
        // Значок: кольцо-трубка со вспышкой; после попытки кольцо гаснет.
        strokeStyle(done ? 0x22FF4FD8 : 0x40FF4FD8, 10f);
        c.drawCircle(ix, mDailyCy, 24f, pStroke);
        strokeStyle(done ? 0x88FF4FD8 : C_MAGENTA, 3f);
        c.drawCircle(ix, mDailyCy, 24f, pStroke);
        strokeReset();
        iconSpark(c, ix, mDailyCy, 12f, done ? 0x99FF9FE9 : 0xFFFFD6F6);

        text(c, sDailyChallenge, l + 94f, mDailyCy - 4f, 27f,
                done ? UI_FOG : UI_INK, tfBold, Paint.Align.LEFT);
        String sub = done
                ? sDailyDone + " · " + fmt(progress.challengeBest)
                : sDailyHint;
        text(c, sub, l + 94f, mDailyCy + 27f, 21f,
                done ? UI_DIM : 0xCCFF9FE9, tfMed, Paint.Align.LEFT);
        if (done) {
            iconCheck(c, r - 46f, mDailyCy, 16f, C_MINT, 5f);
        } else {
            iconChevron(c, r - 42f, mDailyCy, 12f, 0xCCFF9FE9, false);
        }
        addHit(B_DAILY, l, t, r, b);
    }

    /**
     * Мастерская: что даёт текущий уровень, сколько стоит следующий и хватает
     * ли неона. Улучшение — главное действие экрана, поэтому кнопка внизу,
     * под большим пальцем.
     */
    void drawWorkshop(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sWorkshop);
        drawNeonPill(c, W - UI_PAD, insetTop + 84f);
        float l = UI_PAD;
        float r = W - UI_PAD;
        float cx = W / 2f;
        int level = progress.multiplierUpgradeLevel;
        boolean maxed = level >= 4;
        float step = progress.starMultiplierStep();

        // Текущий эффект: крупное число в золотом кольце-трубке.
        float y = top + 36f;
        float ch = 420f;
        box(c, l, y, r, y + ch, UI_RADIUS, UI_SURFACE, 0x44FFD75E);
        float ry = y + 96f;
        strokeStyle(0x33FFD75E, 14f);
        c.drawCircle(cx, ry, 50f, pStroke);
        strokeStyle(C_GOLD, 3.5f);
        c.drawCircle(cx, ry, 50f, pStroke);
        strokeReset();
        iconSpark(c, cx, ry, 24f, 0xFFFFE9A6);
        text(c, sWorkshopStat, cx, ry + 100f, 28f, UI_FOG, tfBold, Paint.Align.CENTER);
        textGlow(c, "+" + fmtMult(step), cx, ry + 196f, 88f, 0xFFFFF2C4, 0x88FFD75E,
                tfBlack, 0.18f);
        drawWrapped(c, sWorkshopHint, cx, ry + 248f, 24f, r - l - 120f, UI_FOG, tfMed,
                Paint.Align.CENTER, 32f);
        y += ch + 28f;

        // Уровни — четыре трубки; горят пройденные.
        String lv = String.format(sWorkshopLevelOf, level, 4);
        text(c, lv, l + 6f, y + 30f, 26f, UI_INK, tfBold, Paint.Align.LEFT);
        y += 62f;
        float pg = 16f;
        float pw = (r - l - pg * 3f) / 4f;
        for (int i = 0; i < 4; i++) {
            float pl = l + i * (pw + pg);
            if (i < level) {
                tube(c, pl + 6f, y, pl + pw - 6f, y, maxed ? C_MINT : C_GOLD, 6f);
            } else {
                strokeStyle(0x22FFFFFF, 6f);
                c.drawLine(pl + 6f, y, pl + pw - 6f, y, pStroke);
                strokeReset();
            }
        }
        y += 52f;

        if (maxed) {
            box(c, l, y, r, y + 132f, UI_RADIUS, UI_SURFACE, 0x667CF9D0);
            iconCheck(c, l + 62f, y + 66f, 20f, C_MINT, 5f);
            textC(c, sWorkshopMax, l + 104f, y + 66f, 28f, C_MINT, tfBold, Paint.Align.LEFT);
            return;
        }

        // Следующий уровень: прирост и цена.
        int cost = progress.multiplierUpgradeCost();
        boolean ready = progress.canUpgradeMultiplier();
        float nh = 196f;
        box(c, l, y, r, y + nh, UI_RADIUS, UI_SURFACE, UI_LINE);
        text(c, sWorkshopNext, l + 36f, y + 56f, 28f, UI_INK, tfBold, Paint.Align.LEFT);
        String gain = "+" + fmtMult(step) + "  →  +" + fmtMult(step + 0.1f);
        text(c, gain, r - 36f, y + 56f, 30f, C_GOLD, tfBlack, Paint.Align.RIGHT);
        pFill.setColor(UI_LINE);
        c.drawRect(l + 36f, y + 92f, r - 36f, y + 94f, pFill);
        text(c, sWorkshopCost, l + 36f, y + 148f, 25f, UI_FOG, tfMed, Paint.Align.LEFT);
        String costText = fmt(cost);
        float cw = measure(costText, 30f, tfBlack);
        text(c, costText, r - 36f, y + 150f, 30f, ready ? 0xFFFFE9A6 : UI_INK, tfBlack,
                Paint.Align.RIGHT);
        iconDiamond(c, r - 36f - cw - 24f, y + 140f, 13f, C_GOLD);
        y += nh + 34f;

        float bh = 124f;
        float by = Math.max(y + bh / 2f, worldH - insetBottom - 40f - bh / 2f);
        if (ready) {
            primaryButton(c, B_WORKSHOP_UPGRADE, cx, by, r - l, bh, sWorkshopBuy, C_GOLD, 0.4f);
        } else {
            // Неона не хватает: кнопка неактивна и говорит, сколько осталось.
            String need = String.format(sWorkshopNeed, fmt(cost - progress.pointsTotal));
            box(c, l, by - bh / 2f, r, by + bh / 2f, bh / 2f, 0x0DFFFFFF, UI_LINE);
            textC(c, need, cx, by, 28f, UI_FOG, tfBold, Paint.Align.CENTER);
            addHit(B_WORKSHOP_UPGRADE, l, by - bh / 2f, r, by + bh / 2f);
        }
    }

    void drawScreenBg(Canvas c) {
        pFill.setColor(0xF0060914);
        c.drawRect(-100f, -100f, W + 100f, worldH + 100f, pFill);
    }

    /** Кнопка «назад», заголовок; возвращает верх области содержимого. */
    float drawHeader(Canvas c, String title) {
        float cy = insetTop + 84f;
        float bx = UI_PAD + 40f;
        roundButton(c, B_BACK, bx, cy, 40f);
        iconChevron(c, bx - 3f, cy, 13f, UI_INK, true);
        textC(c, title, bx + 70f, cy, 46f, UI_INK, tfBlack, Paint.Align.LEFT);
        return cy + 70f;
    }

    void drawNeonPill(Canvas c, float right, float cy) {
        String neon = fmt(progress.pointsTotal);
        float nw = measure(neon, 27f, tfBlack) + 88f;
        box(c, right - nw, cy - 32f, right, cy + 32f, 32f, UI_SURFACE, 0x44FFD75E);
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
