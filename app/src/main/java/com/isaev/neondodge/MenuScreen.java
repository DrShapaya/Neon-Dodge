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
     * Меню раскладывается снизу вверх: у большого пальца — «Играть»
     * и порталы режимов; выше — сцена с планетой, где разделы
     * висят на орбите вокруг сферы; наверху — ранг, неон и вывеска.
     */
    void layoutMenu() {
        mFootY = worldH - insetBottom - 30f;
        mSegCy = mFootY - 44f - 144f;
        float segTop = mSegCy - 144f;
        mTopBarY = insetTop + 80f;
        float room = segTop - (mTopBarY + 60f);
        mLogoSize = room > 1250f ? 138f : (room > 1050f ? 118f : 96f);
        mLogoY = mTopBarY + 60f + 44f + mLogoSize * 0.80f;
        float logoBottom = mLogoY + mLogoSize * 0.98f + 64f;
        float hero = Math.max(0f, segTop - logoBottom - 160f);
        mPodScale = clamp(hero / 560f, 0.75f, 1f);
        mOrbitRy = 150f * mPodScale;
        mOrbitRx = 400f;
        mHeroY = logoBottom + (hero - 560f * mPodScale) / 2f + 230f * mPodScale;
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

        // Зона нажатия для старта игры между разделами и режимами
        float row2 = mHeroY + 95f * mPodScale;
        float zoneTop = row2 + 155f * mPodScale;
        float zoneBottom = mSegCy - 144f - 24f;
        float zoneCy = (zoneTop + zoneBottom) / 2f;
        boolean playDown = pressed(B_PLAY);
        float breathe = 0.5f + 0.5f * (float) Math.sin(time * 3.5f);

        // Мягкий фон без резкой рамки
        box(c, l, zoneTop, r, zoneBottom, 40f,
                playDown ? 0x225FF3FF : 0x085FF3FF, 0);
        drawGlow(c, cx, zoneCy, (r - l) * 0.45f, C_CYAN, playDown ? 45 : (int) (18 + 18 * breathe));

        float iconY = zoneCy - 34f;
        drawGlow(c, cx, iconY, 110f, C_CYAN, (int) (80 + 70 * breathe));
        strokeStyle(withAlpha(C_CYAN, (int) (60 + 50 * breathe)), 3f);
        c.drawCircle(cx, iconY, 44f, pStroke);
        strokeReset();
        pFill.setColor(withAlpha(C_CYAN, (int) (20 + 20 * breathe)));
        c.drawCircle(cx, iconY, 44f, pFill);
        iconPlay(c, cx + 3f, iconY, 28f, withAlpha(0xFFFFFFFF, (int) (220 + 35 * breathe)));

        textGlow(c, sTapStart, cx, zoneCy + 44f, 50f,
                0xFFFFFFFF, withAlpha(C_CYAN, (int) (160 + 95 * breathe)), tfDisplayBold, 0.28f);
        addHit(B_PLAY, l, zoneTop, r, zoneBottom);

        drawModePortals(c, l, r);

        int rank = progress.rank();
        if (rank >= GameRules.MAX_RANK) {
            text(c, sProfileMaxRank, cx, mFootY, 28f, C_GOLD, tfBold, Paint.Align.CENTER);
        } else {
            long need = GameRules.rankThreshold(rank + 1) - progress.neonEarned;
            text(c, String.format(sRankToNext, rank + 1, fmt(need)),
                    cx, mFootY, 28f, 0xFF6F8199, tfBold, Paint.Align.CENTER);
        }
    }

    /** Верхняя строка: ранг с кольцом прогресса, баланс неона и настройки. */
    void drawTopBar(Canvas c, float l, float r) {
        float cy = mTopBarY;
        int rank = progress.rank();
        float prog = GameRules.rankProgress(progress.neonEarned);
        String rankText = String.format(sRankLabel, rank);
        String rankSub = sRankTitle[GameRules.titleIndexForRank(rank)]
                + (rank >= GameRules.MAX_RANK ? " · MAX" : " · " + Math.round(prog * 100f) + "%");
        float tw = Math.max(measure(rankText, 34f, tfDisplayBold), measure(rankSub, 28f, tfBold));
        float chipR = l + 12f + 100f + 22f + tw + 32f;
        boolean pDown = pressed(B_PROFILE);
        box(c, l, cy - 61f, chipR, cy + 61f, 61f, pDown ? UI_SURFACE_HI : UI_SURFACE, 0x1CFFFFFF);
        float ax = l + 12f + 50f;
        // Аватар — текущий скин сферы в кольце прогресса ранга.
        strokeStyle(0x1FFFFFFF, 8f);
        c.drawCircle(ax, cy, 45f, pStroke);
        strokeStyle(rank >= GameRules.MAX_RANK ? C_GOLD : C_CYAN, 8f);
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
        addHit(B_PROFILE, l, cy - 61f, chipR, cy + 61f);

        float gx = r - 61f;
        roundButton(c, B_SETTINGS, gx, cy, 61f);
        iconGear(c, gx, cy, 27f, 0xFFDFF5FF);

        String neon = fmt(progress.neon);
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
        int affordable = (progress.canUpgradeMultiplier() ? 1 : 0)
                + (progress.canUpgradeCloseMultiplier() ? 1 : 0)
                + (progress.canUpgradeBossMultiplier() ? 1 : 0)
                + (progress.canUpgradeLevelMultiplier() ? 1 : 0)
                + (progress.canUpgradeBonusDuration() ? 1 : 0)
                + (progress.canUpgradeTempMultiplier() ? 1 : 0);
        float k = mPodScale;
        float row1 = mHeroY - 150f * k;
        float row2 = mHeroY + 95f * k;
        drawPod(c, B_WORKSHOP, W * 0.22f, row1, 155f * k, C_GOLD,
                0xFFFFE9A6, 0xFF8A5A12, 0, sWorkshop, "", 0,
                affordable > 0 ? "+" + affordable : "", -1f);
        drawPod(c, B_STATS, W * 0.78f, row1, 155f * k, C_CYAN,
                0xFFC8FCFF, 0xFF106A80, 3, sStats, "", 0, "", -1f);
        drawPod(c, B_SKINS, W * 0.28f, row2, 140f * k, C_VIOLET,
                0xFFD8C2FF, 0xFF4A2A8A, 2, sSkins, "", 0, "", -1f);
        drawPod(c, B_RULES, W * 0.72f, row2, 140f * k, C_STEEL,
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
                iconSkull(c, ccx, iy, 24f, C_RED);
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



    /** Все ветки мастерской имеют собственный лимит в 20 уровней. */
    void drawWorkshop(Canvas c) {
        drawScreenBg(c);
        float top = drawHeader(c, sWorkshop);
        drawNeonPill(c, W - UI_PAD, insetTop + 84f);
        float l = UI_PAD;
        float r = W - UI_PAD;

        // Вкладки: Множители, Корабль и щит, Модули и титаны (3 вкладки)
        float segH = 84f;
        float sy = top + 14f + segH / 2f;
        box(c, l, sy - segH / 2f, r, sy + segH / 2f, segH / 2f, 0xCC0A1226, UI_LINE);
        float segW = (r - l - 12f) / 3f;
        for (int i = 0; i < 3; i++) {
            float sl = l + 6f + i * segW;
            float sr = sl + segW;
            boolean sel = workshopTab == i;
            if (sel) {
                box(c, sl, sy - segH / 2f + 6f, sr, sy + segH / 2f - 6f,
                        (segH - 12f) / 2f, 0x305FF3FF, 0xCC5FF3FF);
            }
            String tabTitle = i == 0 ? sWorkshopTabMultipliers
                    : (i == 1 ? sWorkshopTabVessel : sWorkshopTabBonuses);
            int tabBtn = i == 0 ? B_WORKSHOP_TAB_MULT
                    : (i == 1 ? B_WORKSHOP_TAB_VESSEL : B_WORKSHOP_TAB_BONUS);
            textC(c, tabTitle, (sl + sr) / 2f, sy, 22f,
                    sel ? UI_INK : UI_FOG, tfCond, Paint.Align.CENTER);
            addHit(tabBtn, sl, sy - segH / 2f, sr, sy + segH / 2f);
        }

        float y = beginScroll(c, sy + segH / 2f + 8f);
        if (workshopTab == 0) {
            // Вкладка 0: Множители
            // 1) Неоновый старт
            y = drawWorkshopCard(c, y, sWorkshopStartMult, progress.levelMultiplierUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    String.format(sWorkshopStartMultStep, fmtMult(progress.startMultiplier())),
                    String.format(sWorkshopStartMultStep, fmtMult(progress.startMultiplier() + 0.20f)),
                    true, progress.levelMultiplierUpgradeCost(),
                    progress.canUpgradeLevelMultiplier(),
                    B_WORKSHOP_UPGRADE_LEVEL, C_CYAN);

            // 2) Звёздный импульс
            y = drawWorkshopCard(c, y, sWorkshopStars, progress.multiplierUpgradeLevel,
                    Progress.WORKSHOP_MAX_10,
                    String.format(sWorkshopStarStep, fmtMult(progress.starMultiplierStep())),
                    String.format(sWorkshopStarStep, fmtMult(progress.starMultiplierStep() + 0.02f)),
                    true, progress.multiplierUpgradeCost(),
                    progress.canUpgradeMultiplier(),
                    B_WORKSHOP_UPGRADE, C_GOLD);

            // 3) Неоновый манёвр
            y = drawWorkshopCard(c, y, sWorkshopClose, progress.closeMultiplierUpgradeLevel,
                    Progress.WORKSHOP_MAX_10,
                    String.format(sWorkshopCloseStep, fmtMult(progress.closeMultiplierStep())),
                    String.format(sWorkshopCloseStep, fmtMult(progress.closeMultiplierStep() + 0.02f)),
                    true, progress.closeMultiplierUpgradeCost(),
                    progress.canUpgradeCloseMultiplier(),
                    B_WORKSHOP_UPGRADE_CLOSE, C_MAGENTA);

            // 4) Резонанс комбо
            y = drawWorkshopCard(c, y, sWorkshopComboTime, progress.comboTimeUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    String.format(sWorkshopComboTimeStep, fmtMult(progress.comboExtraTime())),
                    String.format(sWorkshopComboTimeStep, fmtMult(progress.comboExtraTime() + 0.15f)),
                    true, progress.comboTimeUpgradeCost(),
                    progress.canUpgradeComboTime(),
                    B_WORKSHOP_UPGRADE_COMBO, C_ICE);
        } else if (workshopTab == 1) {
            // Вкладка 1: Корабль и щит
            // 1) Нано-щит
            y = drawWorkshopCard(c, y, sWorkshopShieldCap, progress.shieldCapUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    String.format(sWorkshopShieldCapStep, fmtMult(progress.shieldInvulnBonus())),
                    String.format(sWorkshopShieldCapStep, fmtMult(progress.shieldInvulnBonus() + 0.30f)),
                    true, progress.shieldCapUpgradeCost(),
                    progress.canUpgradeShieldCap(),
                    B_WORKSHOP_UPGRADE_SHIELD, C_CYAN);

            // 2) Магнитная аура
            int magPct = Math.round((progress.magnetRadiusScale() - 1f) * 100f);
            int nextMagPct = magPct + 15;
            y = drawWorkshopCard(c, y, sWorkshopMagnetRadius, progress.magnetRadiusUpgradeLevel,
                    Progress.WORKSHOP_MAX_8,
                    String.format(sWorkshopMagnetRadiusStep, magPct),
                    String.format(sWorkshopMagnetRadiusStep, nextMagPct),
                    true, progress.magnetRadiusUpgradeCost(),
                    progress.canUpgradeMagnetRadius(),
                    B_WORKSHOP_UPGRADE_MAGNET, C_GOLD);

            // 3) Аварийный сдвиг
            String curPhase = progress.emergencyPhaseUpgradeLevel == 0 ? "—"
                    : String.format(sWorkshopEmergencyPhaseStep, fmtMult(progress.emergencyPhaseDuration()));
            String nextPhase = String.format(sWorkshopEmergencyPhaseStep,
                    fmtMult(progress.emergencyPhaseUpgradeLevel == 0 ? 1.4f : progress.emergencyPhaseDuration() + 0.40f));
            y = drawWorkshopCard(c, y, sWorkshopEmergencyPhase, progress.emergencyPhaseUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    curPhase, nextPhase,
                    true, progress.emergencyPhaseUpgradeCost(),
                    progress.canUpgradeEmergencyPhase(),
                    B_WORKSHOP_UPGRADE_PHASE, C_MAGENTA);

            // 4) Второе дыхание
            int healPct = Math.round((progress.secondWindRateScale() - 1f) * 100f);
            int nextHealPct = healPct + 15;
            y = drawWorkshopCard(c, y, sWorkshopSecondWind, progress.secondWindUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    String.format(sWorkshopSecondWindStep, healPct),
                    String.format(sWorkshopSecondWindStep, nextHealPct),
                    true, progress.secondWindUpgradeCost(),
                    progress.canUpgradeSecondWind(),
                    B_WORKSHOP_UPGRADE_HEART, C_MINT);
        } else {
            // Вкладка 2: Модули и титаны
            // 1) Жатва эссенции
            int essPct = Math.round((progress.essenceNeonBonus() - 1f) * 100f);
            int nextEssPct = essPct + 20;
            y = drawWorkshopCard(c, y, sWorkshopEssenceBoost, progress.bossMultiplierUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    String.format(sWorkshopEssenceBoostStep, essPct),
                    String.format(sWorkshopEssenceBoostStep, nextEssPct),
                    true, progress.bossMultiplierUpgradeCost(),
                    progress.canUpgradeBossMultiplier(),
                    B_WORKSHOP_UPGRADE_BOSS, C_GOLD);

            // 2) Перегрузка узлов
            int condPct = Math.round((progress.conduitDamageScale() - 1f) * 100f);
            int nextCondPct = condPct + 20;
            y = drawWorkshopCard(c, y, sWorkshopConduitBoost, progress.conduitBoostUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    String.format(sWorkshopConduitBoostStep, condPct),
                    String.format(sWorkshopConduitBoostStep, nextCondPct),
                    true, progress.conduitBoostUpgradeCost(),
                    progress.canUpgradeConduitBoost(),
                    B_WORKSHOP_UPGRADE_CONDUIT, C_CYAN);

            // 3) Перегрузка модулей
            int durPct = Math.round((progress.bonusDurationScale() - 1f) * 100f);
            int nextDurPct = durPct + 10;
            y = drawWorkshopCard(c, y, sWorkshopBonusDuration, progress.bonusDurationUpgradeLevel,
                    Progress.WORKSHOP_MAX_10,
                    String.format(sWorkshopBonusDurationStep, durPct),
                    String.format(sWorkshopBonusDurationStep, nextDurPct),
                    true, progress.bonusDurationUpgradeCost(),
                    progress.canUpgradeBonusDuration(),
                    B_WORKSHOP_UPGRADE_DURATION, C_ICE);

            // 4) Гипер-множитель
            y = drawWorkshopCard(c, y, sWorkshopTempMultiplier, progress.tempMultiplierUpgradeLevel,
                    Progress.WORKSHOP_MAX_5,
                    String.format(sWorkshopTempMultiplierStep, fmtMult(progress.temporaryMultiplier())),
                    String.format(sWorkshopTempMultiplierStep, fmtMult(progress.temporaryMultiplier() + 0.60f)),
                    true, progress.tempMultiplierUpgradeCost(),
                    progress.canUpgradeTempMultiplier(),
                    B_WORKSHOP_UPGRADE_TEMP, C_MAGENTA);
        }

        // Общий счётчик прогресса улучшений
        y += 12f;
        textC(c, String.format(sWorkshopProgress, progress.workshopTotalLevel(), Progress.WORKSHOP_TOTAL_MAX_LEVEL),
                (l + r) / 2f, y, 22f, UI_FOG, tfMed, Paint.Align.CENTER);
        y += 40f;

        endScroll(c, y);
    }

    float drawWorkshopCard(Canvas c, float y, String title, int level, int maxLevel,
                           String current, String next, boolean unlocked, int cost, boolean ready,
                           int buttonId, int accent) {
        float l = UI_PAD;
        float r = W - UI_PAD;
        float h = 286f;
        box(c, l, y, r, y + h, UI_RADIUS, UI_SURFACE, withAlpha(accent, 0x66));
        text(c, title, l + 32f, y + 44f, 36f, accent, tfDisplayBold, Paint.Align.LEFT);
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

        if (!unlocked) {
            box(c, l + 24f, y + 144f, r - 24f, y + h - 18f, 28f,
                    0x101B2A3A, 0x445B7087);
            textC(c, sWorkshopLocked, (l + r) / 2f, y + 203f, 22f, UI_FOG,
                    tfBold, Paint.Align.CENTER);
            return y + h + 22f;
        }

        String costText = fmt(cost);
        text(c, sWorkshopCost, l + 32f, y + 153f, 23f, UI_FOG, tfMed, Paint.Align.LEFT);
        text(c, costText, l + 170f, y + 153f, 26f,
                ready ? 0xFFFFE9A6 : UI_INK, tfBlack, Paint.Align.LEFT);
        iconDiamond(c, l + 145f, y + 145f, 11f, C_GOLD);
        float bx = r - 32f - 280f;
        float by = y + 178f;
        float bw = 280f;
        float bh = 72f;
        if (ready) {
            primaryButton(c, buttonId, bx + bw / 2f, by + bh / 2f, bw, bh,
                    sWorkshopBuy, accent, 0.25f);
        } else {
            String need = String.format(sWorkshopNeed, fmt(cost - progress.neon));
            box(c, bx, by, bx + bw, by + bh, 26f, 0x0DFFFFFF, UI_LINE);
            textC(c, need, bx + bw / 2f, by + 43f, 21f, UI_FOG, tfBold, Paint.Align.CENTER);
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
        String neon = fmt(progress.neon);
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
            box(c, W - 16f, trackTop, W - 6f, trackBottom, 5f, 0x1A5FF3FF, 0);
            box(c, W - 16f, thumbTop, W - 6f, thumbTop + thumbH, 5f,
                    scrollDragging || scrollVel != 0f ? 0xCC5FF3FF : 0x775FF3FF, 0);
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
