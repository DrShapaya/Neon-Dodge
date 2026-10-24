package com.isaev.neondodge;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;

/** Главное меню и общий каркас вложенных экранов. */
abstract class MenuScreen extends HudLayer {

    MenuScreen(Context context) {
        super(context);
    }

    /**
     * Меню раскладывается снизу вверх: у большого пальца — испытание дня,
     * «Играть» и порталы режимов; выше — сцена с планетой, где разделы
     * висят на орбите вокруг сферы; наверху — ранг, неон и вывеска.
     */
    void layoutMenu() {
        mFootY = worldH - insetBottom - 30f;
        mDailyCy = mFootY - 44f - 88f;
        float dailyTop = mDailyCy - 88f;
        mPlayCy = dailyTop - 36f - 92f;
        float playTop = mPlayCy - 92f;
        mSegCy = playTop - 40f - 144f;
        float segTop = mSegCy - 144f;
        mTopBarY = insetTop + 80f;
        float room = segTop - (mTopBarY + 60f);
        mLogoSize = room > 1250f ? 138f : (room > 1050f ? 118f : 96f);
        mLogoY = mTopBarY + 60f + 44f + mLogoSize * 0.80f;
        float logoBottom = mLogoY + mLogoSize * 0.98f + 64f;
        float hero = Math.max(0f, segTop - logoBottom);
        // Капсулы занимают ~560 по высоте: ряд сверху, ряд ниже, правила внизу.
        mPodScale = clamp(hero / 600f, 0.78f, 1f);
        mOrbitRy = 150f * mPodScale;
        mOrbitRx = 400f;
        mHeroY = logoBottom + (hero - 560f * mPodScale) / 2f + 250f * mPodScale;
        mPlanetTop = Math.min(segTop - 120f, mHeroY + 210f * mPodScale);
    }

    /** Орбита наклонена на ORBIT_TILT; deg — угол на эллипсе (0° справа, 90° снизу). */
    float orbitX(float deg) {
        double a = Math.toRadians(deg);
        double t = Math.toRadians(ORBIT_TILT);
        double ex = mOrbitRx * Math.cos(a);
        double ey = mOrbitRy * Math.sin(a);
        return W / 2f + (float) (ex * Math.cos(t) - ey * Math.sin(t));
    }

    float orbitY(float deg) {
        double a = Math.toRadians(deg);
        double t = Math.toRadians(ORBIT_TILT);
        double ex = mOrbitRx * Math.cos(a);
        double ey = mOrbitRy * Math.sin(a);
        return mHeroY + (float) (ex * Math.sin(t) + ey * Math.cos(t));
    }

    void drawMenuDecor(Canvas c) {
        drawPlanet(c);

        // Орбита: наклонный эллипс, по которому бежит искра.
        c.save();
        c.rotate(ORBIT_TILT, W / 2f, mHeroY);
        strokeStyle(0x385FF3FF, 2.5f);
        tmpRect.set(W / 2f - mOrbitRx, mHeroY - mOrbitRy, W / 2f + mOrbitRx, mHeroY + mOrbitRy);
        c.drawOval(tmpRect, pStroke);
        strokeReset();
        c.restore();
        float spark = time * 26f;
        float sx = orbitX(spark);
        float sy = orbitY(spark);
        drawGlow(c, sx, sy, 70f, C_CYAN, 170);
        pFill.setColor(0xFFE9FDFF);
        c.drawCircle(sx, sy, 7f, pFill);

        drawPlayer(c, W / 2f, mHeroY + (float) Math.sin(time * 2f) * 12f);
        drawParticles(c);
    }

    /** Край планеты под сценой: тёмный шар с голубой каймой и розовым отсветом. */
    void drawPlanet(Canvas c) {
        float r = PLANET_R;
        float cy = mPlanetTop + r;
        if (planetShader == null || planetShaderTop != mPlanetTop) {
            planetShaderTop = mPlanetTop;
            planetShader = new RadialGradient(W / 2f, mPlanetTop, r * 1.4f,
                    new int[]{0xFF1B1F4A, 0xFF0A0C22, 0xFF05060F},
                    new float[]{0f, 0.40f, 0.70f}, Shader.TileMode.CLAMP);
        }
        // Отсвет атмосферы — широкие полупрозрачные кольца снаружи.
        strokeStyle(0x16FF4FD8, 150f);
        c.drawCircle(W / 2f, cy, r + 60f, pStroke);
        strokeStyle(0x2A5FF3FF, 34f);
        c.drawCircle(W / 2f, cy, r + 14f, pStroke);
        strokeReset();
        pShade.setShader(planetShader);
        c.drawCircle(W / 2f, cy, r, pShade);
        pShade.setShader(null);
        // Широты — только у горизонта, дальше растворяются.
        for (int k = 1; k <= 4; k++) {
            strokeStyle(withAlpha(C_CYAN, 0x14 - k * 3), 2f);
            tmpRect.set(W / 2f - r + k * 60f, cy - r + k * 60f, W / 2f + r - k * 60f,
                    cy + r - k * 60f);
            c.drawArc(tmpRect, 235f, 70f, false, pStroke);
        }
        strokeStyle(0xE68FF8FF, 5f);
        c.drawCircle(W / 2f, cy, r, pStroke);
        strokeReset();
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

        drawTopBar(c, l, r);

        // Логотип — неоновая вывеска шрифтом Unbounded.
        neonText(c, "NEON", cx, mLogoY, mLogoSize, C_CYAN, logoPower(0f), tfDisplay);
        neonText(c, "DODGE", cx, mLogoY + mLogoSize * 0.98f, mLogoSize, C_MAGENTA,
                logoPower(0.35f), tfDisplay);
        text(c, sTagline, cx, mLogoY + mLogoSize * 0.98f + 64f, 34f,
                UI_FOG, tfBold, Paint.Align.CENTER);

        drawOrbitNav(c);
        drawModePortals(c, l, r);

        // «Играть»: режим и рекорд подписаны прямо на кнопке.
        int best = progress.bestForMode(mode);
        String sub = best > 0
                ? String.format(sPlayRecord, sModeNames[mode], fmt(best))
                : String.format(sPlayNoRecord, sModeNames[mode]);
        primaryButton(c, B_PLAY, cx, mPlayCy, r - l, 184f, sTapStart, sub, C_CYAN, 1f);

        drawDailyCard(c, l, r);

        int rank = progress.rank();
        long need = GameRules.rankThreshold(rank + 1) - progress.neonEarned;
        text(c, String.format(sRankToNext, rank + 1, fmt((int) Math.min(need, Integer.MAX_VALUE))),
                cx, mFootY, 28f, 0xFF6F8199, tfBold, Paint.Align.CENTER);
    }

    /** Верхняя строка: ранг с кольцом прогресса, баланс неона и настройки. */
    void drawTopBar(Canvas c, float l, float r) {
        float cy = mTopBarY;
        int rank = progress.rank();
        float prog = GameRules.rankProgress(progress.neonEarned);
        String rankText = String.format(sRankLabel, rank);
        String rankSub = sRankTitle[rankTitleIndex(rank)] + " · " + Math.round(prog * 100f) + "%";
        float tw = Math.max(measure(rankText, 34f, tfDisplayBold), measure(rankSub, 28f, tfBold));
        float chipR = l + 12f + 100f + 22f + tw + 32f;
        box(c, l, cy - 61f, chipR, cy + 61f, 61f, UI_SURFACE, 0x1CFFFFFF);
        float ax = l + 12f + 50f;
        // Аватар — текущий скин сферы в кольце прогресса ранга.
        strokeStyle(0x1FFFFFFF, 8f);
        c.drawCircle(ax, cy, 45f, pStroke);
        strokeStyle(C_CYAN, 8f);
        tmpRect.set(ax - 45f, cy - 45f, ax + 45f, cy + 45f);
        c.drawArc(tmpRect, -90f, 360f * prog, false, pStroke);
        strokeReset();
        int si = clamp(progress.sphereSkin, 0, Progress.SPHERE_COUNT - 1);
        drawGlow(c, ax, cy, 110f, SPHERE_OUTER[si], 120);
        pFill.setColor(SPHERE_OUTER[si]);
        c.drawCircle(ax, cy, 34f, pFill);
        pFill.setColor(SPHERE_INNER[si]);
        c.drawCircle(ax, cy, 22f, pFill);
        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(ax - 4f, cy - 4f, 9f, pFill);
        float tx = ax + 50f + 22f;
        text(c, rankText, tx, cy - 4f, 34f, UI_INK, tfDisplayBold, Paint.Align.LEFT);
        text(c, rankSub, tx, cy + 34f, 28f, UI_FOG, tfBold, Paint.Align.LEFT);
        addHit(B_STATS, l, cy - 61f, chipR, cy + 61f);

        float gx = r - 61f;
        roundButton(c, B_SETTINGS, gx, cy, 61f);
        iconGear(c, gx, cy, 27f, 0xFFDFF5FF);

        String neon = fmt(progress.pointsTotal);
        float nw = measure(neon, 40f, tfBlack) + 110f;
        float nr = gx - 61f - 22f;
        boolean nDown = pressed(B_WORKSHOP);
        box(c, nr - nw, cy - 61f, nr, cy + 61f, 61f, nDown ? UI_SURFACE_HI : UI_SURFACE,
                0x59FFD75E);
        iconDiamond(c, nr - nw + 44f, cy, 20f, C_GOLD);
        textC(c, neon, nr - nw + 76f, cy, 40f, 0xFFFFE9A6, tfBlack, Paint.Align.LEFT);
        addHit(B_WORKSHOP, nr - nw, cy - 61f, nr, cy + 61f);
    }

    /**
     * Разделы — шестигранные капсулы (форма астероидов) на орбите. Кнопки
     * стоят на месте: по орбите бежит только искра, цели не уезжают из-под пальца.
     */
    void drawOrbitNav(Canvas c) {
        int done = 0;
        for (int i = 0; i < 3; i++) {
            if (progress.questDone[i]) {
                done++;
            }
        }
        int affordable = (progress.canUpgradeMultiplier() ? 1 : 0)
                + (progress.canUpgradeCloseMultiplier() ? 1 : 0)
                + (progress.canUpgradeBossMultiplier() ? 1 : 0)
                + (progress.canUpgradeLevelMultiplier() ? 1 : 0);
        float k = mPodScale;
        float row1 = mHeroY - 170f * k;
        float row2 = mHeroY + 84f * k;
        drawPod(c, B_WORKSHOP, W * 0.20f, row1, 160f * k, C_GOLD,
                0xFFFFE9A6, 0xFF8A5A12, 0, sWorkshop, "", 0,
                affordable > 0 ? "+" + affordable : "", -1f);
        drawPod(c, B_QUESTS, W * 0.80f, row1 - 24f * k, 160f * k, C_MINT,
                0xFFC9FFF0, 0xFF0F5A44, 1, sQuests, done + "/3", C_MINT, "", done / 3f);
        drawPod(c, B_SKINS, W * 0.135f, row2, 142f * k, C_VIOLET,
                0xFFD8C2FF, 0xFF4A2A8A, 2, sSkins, "", 0, "", -1f);
        drawPod(c, B_STATS, W * 0.865f, row2 - 16f * k, 142f * k, C_CYAN,
                0xFFC8FCFF, 0xFF106A80, 3, sStats, "", 0, "", -1f);
        drawPod(c, B_RULES, W / 2f, mHeroY + 205f * k, 120f * k, C_STEEL,
                0xFFF2F7FB, 0xFF46566A, 4, sRulesButton, "", 0, "", -1f);
    }

    /** Шестигранник «остриём вверх» с центром (x, y). */
    void podHex(Path p, float x, float y, float w, float h) {
        p.reset();
        p.moveTo(x, y - h / 2f);
        p.lineTo(x + w / 2f, y - h / 4f);
        p.lineTo(x + w / 2f, y + h / 4f);
        p.lineTo(x, y + h / 2f);
        p.lineTo(x - w / 2f, y + h / 4f);
        p.lineTo(x - w / 2f, y - h / 4f);
        p.close();
    }

    /**
     * Капсула раздела: металлическая рамка в цвете раздела, тёмное стекло
     * внутри, иконка, подпись. ring ≥ 0 — рамка работает полосой прогресса.
     */
    void drawPod(Canvas c, int id, float x, float y, float w, int color, int light, int dark,
                 int icon, String label, String extra, int extraColor, String badge, float ring) {
        boolean down = pressed(id);
        float s = down ? 0.94f : 1f;
        float pw = w * s;
        float ph = pw * 1.13f;
        drawGlow(c, x, y, pw * 2.1f, color, down ? 140 : 95);

        podHex(podPath, x, y, pw, ph);
        if (ring >= 0f) {
            // Рамка-прогресс: яркая часть — выполненная доля.
            float p = clamp(ring, 0f, 1f);
            SweepGradient sweep = new SweepGradient(x, y,
                    new int[]{color, color, withAlpha(color, 0x30), withAlpha(color, 0x30)},
                    new float[]{0f, p, p, 1f});
            // Прогресс начинается сверху, а не справа.
            shaderMatrix.setRotate(-90f, x, y);
            sweep.setLocalMatrix(shaderMatrix);
            pShade.setShader(sweep);
            c.drawPath(podPath, pShade);
        } else {
            pShade.setShader(new LinearGradient(x - pw / 2f, y - ph / 2f, x + pw / 2f, y + ph / 2f,
                    new int[]{light, color, dark}, new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP));
            c.drawPath(podPath, pShade);
        }
        pShade.setShader(null);
        podHex(podPath, x, y, pw - 14f, ph - 15f);
        pFill.setColor(down ? mix(0xFF0C1028, color, 0.25f) : 0xFF0C1028);
        c.drawPath(podPath, pFill);
        pFill.setColor(withAlpha(color, 0x1C));
        c.drawCircle(x, y - ph * 0.12f, pw * 0.30f, pFill);

        int ic = mix(color, 0xFFFFFFFF, 0.6f);
        float is = pw * 0.15f;
        switch (icon) {
            case 0:
                iconUpgrade(c, x, y, is, ic);
                break;
            case 1:
                iconFlag(c, x + 3f, y, is, ic);
                break;
            case 2:
                iconOrb(c, x + 3f, y, is, ic);
                break;
            case 3:
                iconBars(c, x, y, is, ic);
                break;
            default:
                iconDoc(c, x, y, is, ic);
                break;
        }

        if (badge.length() > 0) {
            float bw = measure(badge, 28f, tfBlack) + 28f;
            float bx = x + pw * 0.30f;
            float by = y - ph * 0.46f;
            box(c, bx, by, bx + bw, by + 50f, 25f, C_GOLD, 0);
            textC(c, badge, bx + bw / 2f, by + 25f, 28f, 0xFF2B1E02, tfBlack, Paint.Align.CENTER);
        }

        float ls = 33f * Math.max(0.85f, mPodScale);
        float ly = y + w * 1.13f / 2f + ls * 1.35f;
        if (extra.length() > 0) {
            float lw = measure(label, ls, tfBold);
            float ew = measure(extra, ls, tfBold);
            float lx = x - (lw + 12f + ew) / 2f;
            text(c, label, lx, ly, ls, UI_INK, tfBold, Paint.Align.LEFT);
            text(c, extra, lx + lw + 12f, ly, ls, extraColor, tfBold, Paint.Align.LEFT);
        } else {
            text(c, label, x, ly, ls, UI_INK, tfBold, Paint.Align.CENTER);
        }
        addHit(id, x - 125f, y - ph / 2f - 10f, x + 125f, ly + 16f);
    }

    /** Режимы — три карточки-портала с иллюстрацией, названием и рекордом. */
    void drawModePortals(Canvas c, float l, float r) {
        float gap = 22f;
        float w = (r - l - gap * 2f) / 3f;
        float h = 288f;
        float t = mSegCy - h / 2f;
        for (int i = 0; i < Progress.MODE_COUNT; i++) {
            float cl = l + i * (w + gap);
            float cr = cl + w;
            float ccx = (cl + cr) / 2f;
            boolean sel = i == mode;
            int mc = modeColor(i);
            boolean down = pressed(B_MODE + i);
            tmpRect.set(cl, t, cr, t + h);
            if (sel) {
                box(c, cl - 11f, t - 11f, cr + 11f, t + h + 11f, 61f, withAlpha(mc, 0x1A), 0);
                pShade.setShader(new LinearGradient(0f, t, 0f, t + h,
                        new int[]{mix(0xFF0A1028, mc, 0.25f), mix(0xFF0A1028, mc, 0.04f),
                                0xFF0A1028},
                        new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
                c.drawRoundRect(tmpRect, 50f, 50f, pShade);
                pShade.setShader(null);
                strokeStyle(mc, 4f);
                c.drawRoundRect(tmpRect, 50f, 50f, pStroke);
                strokeReset();
            } else {
                box(c, cl, t, cr, t + h, 50f, down ? UI_SURFACE_HI : 0xFA0A1028,
                        withAlpha(mc, 0x4D));
            }
            float iy = t + 74f;
            if (i == Progress.MODE_CLASSIC) {
                for (int k = -1; k <= 1; k++) {
                    drawHeart(c, ccx + k * 52f, iy, 21f, C_RED, 255);
                }
            } else if (i == Progress.MODE_HARDCORE) {
                drawHeart(c, ccx - 34f, iy, 21f, C_RED, 255);
                text(c, "x5", ccx + 12f, iy + 14f, 38f, 0xFFFF8A9C, tfDisplay, Paint.Align.LEFT);
            } else {
                strokeStyle(C_LIME, 5f);
                for (int k = 0; k < 2; k++) {
                    iconPath.reset();
                    float wy = iy - 6f + k * 18f;
                    iconPath.moveTo(ccx - 44f, wy);
                    iconPath.quadTo(ccx - 22f, wy - 16f, ccx, wy);
                    iconPath.quadTo(ccx + 22f, wy + 16f, ccx + 44f, wy);
                    pStroke.setAlpha(k == 0 ? 255 : 130);
                    c.drawPath(iconPath, pStroke);
                }
                strokeReset();
            }
            text(c, sModeNames[i], ccx, t + 170f, 36f, sel ? UI_INK : 0xFFC9D6E4,
                    tfDisplayBold, Paint.Align.CENTER);
            int best = progress.bestForMode(i);
            String rec = best > 0 ? fmt(best)
                    : (i == Progress.MODE_ZEN ? sModeNoLose : sModeNoRecord);
            text(c, rec, ccx, t + 222f, 30f, sel ? mix(mc, 0xFFFFFFFF, 0.5f) : 0xFF7E8FA6,
                    tfBold, Paint.Align.CENTER);
            addHit(B_MODE + i, cl, t, cr, t + h);
        }
    }

    /** Испытание дня — «билет»: пунктирная рамка, таймер до смены дня. */
    void drawDailyCard(Canvas c, float l, float r) {
        boolean done = progress.challengeDone;
        float t = mDailyCy - 88f;
        float b = mDailyCy + 88f;
        boolean down = pressed(B_DAILY);
        box(c, l, t, r, b, 50f, down ? UI_SURFACE_HI : 0xDB100E2C, 0);
        drawGlow(c, l + 90f, mDailyCy, 520f, C_MAGENTA, done ? 40 : 90);
        strokeStyle(done ? 0x55FF4FD8 : 0x8CFF4FD8, 3f);
        pStroke.setPathEffect(dailyDash);
        tmpRect.set(l, t, r, b);
        c.drawRoundRect(tmpRect, 50f, 50f, pStroke);
        pStroke.setPathEffect(null);
        strokeReset();

        float ix = l + 39f + 53f;
        strokeStyle(done ? 0x33FF4FD8 : 0x55FF4FD8, 16f);
        c.drawCircle(ix, mDailyCy, 50f, pStroke);
        strokeStyle(done ? 0x99FF4FD8 : C_MAGENTA, 5f);
        c.drawCircle(ix, mDailyCy, 50f, pStroke);
        strokeReset();
        iconSpark(c, ix, mDailyCy, 24f, done ? 0x99FF9FE9 : 0xFFFFD6F6);

        float tx = ix + 53f + 33f;
        text(c, sDailyChallenge, tx, mDailyCy - 8f, 37f, done ? UI_FOG : UI_INK,
                tfDisplayBold, Paint.Align.LEFT);
        String sub = (done ? sDailyDone + " · " + fmt(progress.challengeBest)
                : sDailyHint) + " · " + String.format(sDailyResets, timeToMidnight());
        if (measure(sub, 30f, tfBold) > r - 110f - tx) {
            // Не помещается — оставляем главное: счёт сегодняшней попытки или таймер.
            sub = done ? sDailyDone + " · " + fmt(progress.challengeBest)
                    : String.format(sDailyResets, timeToMidnight());
        }
        text(c, sub, tx, mDailyCy + 38f, 30f, done ? UI_DIM : 0xFFFF9FE9, tfBold,
                Paint.Align.LEFT);
        if (done) {
            iconCheck(c, r - 60f, mDailyCy, 22f, C_MINT, 6f);
        } else {
            iconChevron(c, r - 56f, mDailyCy, 16f, 0xCCFF9FE9, false);
        }
        addHit(B_DAILY, l, t, r, b);
    }

    /** Сколько осталось до полуночи по местному времени, «ЧЧ:ММ:СС». */
    String timeToMidnight() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int left = 24 * 3600 - (now.get(java.util.Calendar.HOUR_OF_DAY) * 3600
                + now.get(java.util.Calendar.MINUTE) * 60 + now.get(java.util.Calendar.SECOND));
        int h = left / 3600;
        int m = (left % 3600) / 60;
        int s = left % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }

    /** Все ветки мастерской имеют собственный лимит в 20 уровней. */
    void drawWorkshop(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sWorkshop);
        drawNeonPill(c, W - UI_PAD, insetTop + 84f);

        float y = beginScroll(c, top + 12f);
        y = drawWorkshopCard(c, y, sWorkshopStars, progress.multiplierUpgradeLevel,
                Progress.WORKSHOP_MAX_LEVEL, "+" + fmtMult(progress.starMultiplierStep()),
                "+" + fmtMult(progress.starMultiplierStep() + 0.1f),
                progress.multiplierUpgradeCost(), progress.canUpgradeMultiplier(),
                B_WORKSHOP_UPGRADE, C_GOLD);
        y = drawWorkshopCard(c, y, sWorkshopClose, progress.closeMultiplierUpgradeLevel,
                Progress.WORKSHOP_MAX_LEVEL,
                String.format(sWorkshopCloseStep, fmtMult(progress.closeMultiplierStep())),
                String.format(sWorkshopCloseStep, fmtMult(progress.closeMultiplierStep() + 0.025f)),
                progress.closeMultiplierUpgradeCost(), progress.canUpgradeCloseMultiplier(),
                B_WORKSHOP_UPGRADE_CLOSE, C_MAGENTA);
        y = drawWorkshopCard(c, y, sWorkshopBoss, progress.bossMultiplierUpgradeLevel,
                Progress.WORKSHOP_MAX_LEVEL,
                String.format(sWorkshopBossValue, fmtMult(progress.bossMultiplier())),
                String.format(sWorkshopBossValue, fmtMult(progress.bossMultiplier() + 0.2f)),
                progress.bossMultiplierUpgradeCost(), progress.canUpgradeBossMultiplier(),
                B_WORKSHOP_UPGRADE_BOSS, C_GOLD);
        y = drawWorkshopCard(c, y, sWorkshopLevelMult, progress.levelMultiplierUpgradeLevel,
                Progress.WORKSHOP_MAX_LEVEL,
                String.format(sWorkshopLevelStep, fmtMult(progress.levelMultiplierStep())),
                String.format(sWorkshopLevelStep, fmtMult(progress.levelMultiplierStep() + 0.01f)),
                progress.levelMultiplierUpgradeCost(), progress.canUpgradeLevelMultiplier(),
                B_WORKSHOP_UPGRADE_LEVEL, C_CYAN);
        endScroll(c, y);
    }

    float drawWorkshopCard(Canvas c, float y, String title, int level, int maxLevel,
                           String current, String next, int cost, boolean ready,
                           int buttonId, int accent) {
        float l = UI_PAD;
        float r = W - UI_PAD;
        float h = 244f;
        box(c, l, y, r, y + h, UI_RADIUS, UI_SURFACE, withAlpha(accent, 0x66));
        text(c, title, l + 32f, y + 42f, 30f, UI_INK, tfBold, Paint.Align.LEFT);
        text(c, String.format(sWorkshopLevelOf, level, maxLevel), r - 32f, y + 42f,
                24f, UI_FOG, tfMed, Paint.Align.RIGHT);
        text(c, current, l + 32f, y + 86f, 29f, accent, tfBlack, Paint.Align.LEFT);
        if (level < maxLevel) {
            text(c, sWorkshopNext + "  " + next, r - 32f, y + 86f,
                    24f, UI_FOG, tfMed, Paint.Align.RIGHT);
        } else {
            text(c, sWorkshopMax, r - 32f, y + 86f, 24f, C_MINT, tfBold, Paint.Align.RIGHT);
        }

        float gap = 5f;
        float pw = (r - l - 64f - gap * (maxLevel - 1)) / maxLevel;
        for (int i = 0; i < maxLevel; i++) {
            float pl = l + 32f + i * (pw + gap);
            pFill.setColor(i < level ? accent : 0x223B5572);
            c.drawRoundRect(pl, y + 112f, pl + pw, y + 120f, 4f, 4f, pFill);
        }
        if (level >= maxLevel) {
            box(c, l + 24f, y + 144f, r - 24f, y + h - 18f, 28f,
                    0x1228A47B, 0x667CF9D0);
            textC(c, sWorkshopMax, (l + r) / 2f, y + 190f, 27f, C_MINT,
                    tfBold, Paint.Align.CENTER);
            return y + h + 22f;
        }

        String costText = fmt(cost);
        text(c, sWorkshopCost, l + 32f, y + 153f, 23f, UI_FOG, tfMed, Paint.Align.LEFT);
        text(c, costText, l + 170f, y + 153f, 26f,
                ready ? 0xFFFFE9A6 : UI_INK, tfBlack, Paint.Align.LEFT);
        iconDiamond(c, l + 145f, y + 145f, 11f, C_GOLD);
        float bx = r - 32f - 220f;
        float by = y + 180f;
        float bw = 220f;
        float bh = 52f;
        if (ready) {
            primaryButton(c, buttonId, bx + bw / 2f, by + bh / 2f, bw, bh,
                    sWorkshopBuy, accent, 0.25f);
        } else {
            String need = String.format(sWorkshopNeed, fmt(cost - progress.pointsTotal));
            box(c, bx, by, bx + bw, by + bh, 26f, 0x0DFFFFFF, UI_LINE);
            textC(c, need, bx + bw / 2f, by + 33f, 19f, UI_FOG, tfBold, Paint.Align.CENTER);
            addHit(buttonId, bx, by, bx + bw, by + bh);
        }
        return y + h + 22f;
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
        textC(c, title, bx + 70f, cy, 42f, UI_INK, tfDisplayBold, Paint.Align.LEFT);
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
