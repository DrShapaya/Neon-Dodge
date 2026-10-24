package com.isaev.neondodge;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** UI-кит: палитра, зоны нажатия, прокрутка, примитивы, текст и иконки. */
abstract class UiKit extends GameRenderer {

    UiKit(Context context) {
        super(context);
    }

    void addHit(int id, float l, float t, float r, float b) {
        if (clipping) {
            t = Math.max(t, scrollTop);
            b = Math.min(b, scrollBottom);
        }
        if (b <= t || hitCount >= MAX_HITS) {
            return;
        }
        hitRect[hitCount].set(l, t, r, b);
        hitId[hitCount] = id;
        hitCount++;
    }

    int hitAt(float x, float y) {
        if (hitState != state) {
            return B_NONE;
        }
        for (int i = hitCount - 1; i >= 0; i--) {
            RectF r = hitRect[i];
            if (x >= r.left - 6f && x <= r.right + 6f
                    && y >= r.top - 6f && y <= r.bottom + 6f) {
                return hitId[i];
            }
        }
        return B_NONE;
    }

    boolean pressed(int id) {
        return pressedId == id && !(scrollDragging && touchMoved);
    }

    boolean isScrollScreen() {
        return state == ST_STATS || state == ST_HISTORY || state == ST_QUESTS
                || state == ST_SKINS || state == ST_WORKSHOP || state == ST_RULES || state == ST_SETTINGS;
    }

    void go(int newState) {
        state = newState;
        scrollY = 0f;
        scrollVel = 0f;
        scrollMax = 0f;
        scrollDragging = false;
        pressedId = B_NONE;
        draggingOffset = false;
        inputLock = 0.12f;
    }

    void updateUi(float dt) {
        if (exitArmed > 0f) {
            exitArmed = Math.max(0f, exitArmed - dt);
        }
        if (!scrollDragging && scrollVel != 0f) {
            scrollY += scrollVel * dt;
            scrollVel *= (float) Math.exp(-4.5f * dt);
            if (scrollY <= 0f || scrollY >= scrollMax) {
                scrollY = clamp(scrollY, 0f, scrollMax);
                scrollVel = 0f;
            }
            if (Math.abs(scrollVel) < 25f) {
                scrollVel = 0f;
            }
        }
    }

    void box(Canvas c, float l, float t, float r, float b, float rad,
                     int fill, int stroke) {
        tmpRect.set(l, t, r, b);
        if (fill != 0) {
            pFill.setColor(fill);
            c.drawRoundRect(tmpRect, rad, rad, pFill);
        }
        if (stroke != 0) {
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setColor(stroke);
            pStroke.setStrokeWidth(2f);
            c.drawRoundRect(tmpRect, rad, rad, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
        }
    }

    /** Карточка; светящаяся «трубка» слева кодирует статус или режим. */
    void card(Canvas c, float l, float t, float r, float b, int accent,
                      boolean highlight) {
        box(c, l, t, r, b, UI_RADIUS, highlight ? UI_SURFACE_HI : UI_SURFACE,
                highlight ? 0x995FF3FF : UI_LINE);
        if (accent != 0) {
            float inset = Math.min(30f, (b - t) * 0.28f);
            tube(c, l + 2f, t + inset, l + 2f, b - inset, accent, 4f);
        }
    }

    /**
     * Неоновая трубка: широкий полупрозрачный ореол, цветное тело и светлая
     * сердцевина. Свечение нарисовано штрихами, а не тенью, чтобы одинаково
     * выглядеть на всех версиях Android.
     */
    void tube(Canvas c, float x1, float y1, float x2, float y2, int color, float width) {
        strokeStyle(withAlpha(color, 0x30), width * 4f);
        c.drawLine(x1, y1, x2, y2, pStroke);
        strokeStyle(color, width);
        c.drawLine(x1, y1, x2, y2, pStroke);
        strokeStyle(mix(color, 0xFFFFFFFF, 0.7f), Math.max(1f, width * 0.35f));
        c.drawLine(x1, y1, x2, y2, pStroke);
        strokeReset();
    }

    /** Надпись неоновыми трубками: контур буквы вместо заливки. */
    void neonText(Canvas c, String s, float x, float y, float size, int color,
                  float power, Typeface tf) {
        // Контуры глифов пересекаются; без объединения внутри букв видны
        // лишние линии. Объединённый путь строится один раз на надпись.
        String key = s + '@' + (int) size;
        Path path = neonPaths.get(key);
        if (path == null) {
            pText.setTypeface(tf);
            pText.setTextSize(size);
            pText.setTextAlign(Paint.Align.CENTER);
            pText.setLetterSpacing(0.04f);
            Path raw = new Path();
            pText.getTextPath(s, 0, s.length(), 0f, 0f, raw);
            pText.setLetterSpacing(0f);
            path = new Path();
            if (!path.op(raw, new Path(), Path.Op.UNION)) {
                path = raw;
            }
            neonPaths.put(key, path);
        }
        int a = (int) (255f * clamp(power, 0f, 1f));
        c.save();
        c.translate(x, y);
        pNeon.setStyle(Paint.Style.STROKE);
        pNeon.setStrokeJoin(Paint.Join.ROUND);
        pNeon.setStrokeCap(Paint.Cap.ROUND);
        if (a > 8) {
            pNeon.setStrokeWidth(size * 0.11f);
            pNeon.setColor(withAlpha(color, a * 0x30 / 255));
            pNeon.setShadowLayer(size * 0.20f, 0f, 0f, withAlpha(color, a));
            c.drawPath(path, pNeon);
            pNeon.clearShadowLayer();
        }
        // Погасшая трубка остаётся видна тусклым стеклом.
        pNeon.setStrokeWidth(size * 0.045f);
        pNeon.setColor(withAlpha(color, Math.max(60, a)));
        c.drawPath(path, pNeon);
        if (a > 8) {
            pNeon.setStrokeWidth(size * 0.016f);
            pNeon.setColor(withAlpha(mix(color, 0xFFFFFFFF, 0.8f), a));
            c.drawPath(path, pNeon);
        }
        c.restore();
    }

    void textC(Canvas c, String s, float x, float cy, float size,
                       int color, Typeface tf, Paint.Align align) {
        text(c, s, x, cy + size * 0.36f, size, color, tf, align);
    }

    float measure(String s, float size, Typeface tf) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setLetterSpacing(0f);
        return pText.measureText(s);
    }

    void primaryButton(Canvas c, int id, float cx, float cy, float w, float h,
                               String label, int color, float breathe) {
        primaryButton(c, id, cx, cy, w, h, label, null, color, breathe);
    }

    /**
     * Главная кнопка: градиентная капсула (циан → фиолетовый, золото для
     * улучшений) с внутренним бликом. У «Играть» и «Ещё раз» по ней раз в
     * несколько секунд пробегает световая полоса; sub — вторая строка.
     */
    void primaryButton(Canvas c, int id, float cx, float cy, float w, float h,
                       String label, String sub, int color, float breathe) {
        boolean down = pressed(id);
        float in = down ? 5f : 0f;
        float l = cx - w / 2f + in;
        float t = cy - h / 2f + in;
        float r = cx + w / 2f - in;
        float b = cy + h / 2f - in;
        float rad = Math.min(60f, (b - t) / 2f);
        int[] grad = color == C_GOLD
                ? new int[]{0xFFFFE9A6, 0xFFFFD75E, 0xFFFFB13D}
                : (color == C_CYAN ? new int[]{0xFF36E6FF, 0xFF5FF3FF, 0xFF9C8BFF}
                : new int[]{mix(color, 0xFFFFFFFF, 0.35f), color, mix(color, 0xFF000000, 0.25f)});

        // Мягкий ореол под кнопкой.
        c.save();
        c.scale(w / h * 0.55f, 1f, cx, cy + h * 0.15f);
        drawGlow(c, cx, cy + h * 0.15f, h * 2.6f, grad[1], (int) (60f + 30f * breathe));
        c.restore();

        tmpRect.set(l, t, r, b);
        pShade.setShader(new LinearGradient(l, 0f, r, 0f, grad, new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRoundRect(tmpRect, rad, rad, pShade);
        pShade.setShader(null);
        if (down) {
            pFill.setColor(0x33000000);
            c.drawRoundRect(tmpRect, rad, rad, pFill);
        }
        boolean sweep = (id == B_PLAY || id == B_AGAIN) && ValueAnimator.areAnimatorsEnabled();
        if (sweep) {
            float phase = (time % 3.4f) / 3.4f;
            if (phase < 0.6f) {
                float sx = l - 200f + (r - l + 400f) * (phase / 0.6f);
                iconPath.reset();
                iconPath.addRoundRect(tmpRect, rad, rad, Path.Direction.CW);
                c.save();
                c.clipPath(iconPath);
                c.skew(-0.36f, 0f);
                pShade.setShader(new LinearGradient(sx + cy * 0.36f - 100f, 0f,
                        sx + cy * 0.36f + 100f, 0f,
                        new int[]{0x00FFFFFF, 0x8CFFFFFF, 0x00FFFFFF}, null,
                        Shader.TileMode.CLAMP));
                c.drawRect(sx + cy * 0.36f - 100f, t, sx + cy * 0.36f + 100f, b, pShade);
                pShade.setShader(null);
                c.restore();
            }
        }
        strokeStyle(0x59FFFFFF, 2f);
        tmpRect.set(l + 1f, t + 1f, r - 1f, b - 1f);
        c.drawRoundRect(tmpRect, rad, rad, pStroke);
        strokeReset();

        int ink = UI_ON_ACCENT;
        boolean twoLines = sub != null && sub.length() > 0;
        float size = Math.min(60f, h * (twoLines ? 0.33f : 0.32f));
        float tw = measure(label, size, tfDisplay);
        float subSize = size * 0.55f;
        if (twoLines) {
            tw = Math.max(tw, measure(sub, subSize, tfBlack));
        }
        boolean play = id == B_PLAY || id == B_AGAIN || id == B_EMPTY_PLAY || id == B_CONTINUE;
        float tri = size * 0.42f;
        float gap = play ? size * 0.42f : 0f;
        float total = (play ? tri * 1.3f : 0f) + gap + tw;
        float x0 = cx - total / 2f;
        if (play) {
            if (id == B_AGAIN) {
                iconReplay(c, x0 + tri * 0.65f, cy, tri * 1.1f, ink);
            } else {
                iconPlay(c, x0 + tri * 0.55f, cy, tri, ink);
            }
        }
        float tx = x0 + (play ? tri * 1.3f : 0f) + gap;
        if (twoLines) {
            text(c, label, tx, cy + size * 0.10f, size, ink, tfDisplay, Paint.Align.LEFT);
            text(c, sub, tx, cy + size * 0.10f + subSize * 1.35f, subSize, 0xFF0B3346, tfBlack,
                    Paint.Align.LEFT);
        } else {
            textC(c, label, tx, cy, size, ink, tfDisplay, Paint.Align.LEFT);
        }
        addHit(id, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    void secondaryButton(Canvas c, int id, float cx, float cy, float w, float h,
                                 String label, int accent, int textColor) {
        boolean down = pressed(id);
        float in = down ? 4f : 0f;
        box(c, cx - w / 2f + in, cy - h / 2f + in, cx + w / 2f - in, cy + h / 2f - in,
                (h - 2f * in) / 2f, down ? UI_SURFACE_HI : UI_SURFACE,
                withAlpha(accent, down ? 170 : 70));
        textC(c, label, cx, cy, Math.min(31f, h * 0.29f), textColor,
                tfBold, Paint.Align.CENTER);
        addHit(id, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    /** Круглая кнопка-иконка со стеклянным фоном. */
    void roundButton(Canvas c, int id, float cx, float cy, float r) {
        boolean down = pressed(id);
        pFill.setColor(down ? UI_SURFACE_HI : UI_SURFACE);
        c.drawCircle(cx, cy, r, pFill);
        strokeStyle(down ? 0x55FFFFFF : UI_LINE, 2f);
        c.drawCircle(cx, cy, r, pStroke);
        strokeReset();
        addHit(id, cx - r - 16f, cy - r - 16f, cx + r + 16f, cy + r + 16f);
    }

    void progressBar(Canvas c, float l, float r, float cy, float h,
                             float ratio, int color) {
        box(c, l, cy - h / 2f, r, cy + h / 2f, h / 2f, 0x1FFFFFFF, 0);
        ratio = clamp(ratio, 0f, 1f);
        if (ratio > 0f) {
            float rr = Math.max(l + h, l + (r - l) * ratio);
            box(c, l, cy - h / 2f, rr, cy + h / 2f, h / 2f, color, 0);
        }
    }

    void switchToggle(Canvas c, float cx, float cy, boolean on) {
        float w = 96f;
        float h = 56f;
        box(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h / 2f,
                on ? C_CYAN : 0x1AFFFFFF, on ? 0 : 0x26FFFFFF);
        float kx = on ? cx + w / 2f - h / 2f : cx - w / 2f + h / 2f;
        pFill.setColor(on ? UI_ON_ACCENT : 0xFF8093A8);
        c.drawCircle(kx, cy, h / 2f - 7f, pFill);
        if (on) {
            pFill.setColor(C_CYAN);
            c.drawCircle(kx, cy, 6f, pFill);
        }
    }

    /** Подпись раздела; тонкая линия тянется до правого края. */
    float sectionLabel(Canvas c, String s, float y) {
        float tw = measure(s, 26f, tfBold);
        text(c, s, UI_PAD + 6f, y + 34f, 26f, UI_FOG, tfBold, Paint.Align.LEFT);
        pFill.setColor(UI_LINE);
        c.drawRect(UI_PAD + 6f + tw + 20f, y + 25f, W - UI_PAD, y + 27f, pFill);
        return y + 58f;
    }

    void strokeStyle(int color, float width) {
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeCap(Paint.Cap.ROUND);
        pStroke.setStrokeJoin(Paint.Join.ROUND);
        pStroke.setColor(color);
        pStroke.setStrokeWidth(width);
    }

    void strokeReset() {
        pStroke.setStrokeCap(Paint.Cap.BUTT);
        pStroke.setStrokeJoin(Paint.Join.MITER);
        pStroke.setStyle(Paint.Style.FILL);
    }

    void iconPlay(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        iconPath.moveTo(x - s * 0.55f, y - s * 0.75f);
        iconPath.lineTo(x + s * 0.75f, y);
        iconPath.lineTo(x - s * 0.55f, y + s * 0.75f);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
    }

    void iconChevron(Canvas c, float x, float y, float s, int color, boolean left) {
        strokeStyle(color, s * 0.30f);
        float d = left ? 1f : -1f;
        c.drawLine(x + d * s * 0.45f, y - s, x - d * s * 0.45f, y, pStroke);
        c.drawLine(x - d * s * 0.45f, y, x + d * s * 0.45f, y + s, pStroke);
        strokeReset();
    }

    void iconCheck(Canvas c, float x, float y, float s, int color, float width) {
        strokeStyle(color, width);
        c.drawLine(x - s * 0.55f, y + s * 0.02f, x - s * 0.15f, y + s * 0.42f, pStroke);
        c.drawLine(x - s * 0.15f, y + s * 0.42f, x + s * 0.60f, y - s * 0.40f, pStroke);
        strokeReset();
    }

    void iconDiamond(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        iconPath.moveTo(x, y - s);
        iconPath.lineTo(x + s * 0.72f, y);
        iconPath.lineTo(x, y + s);
        iconPath.lineTo(x - s * 0.72f, y);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
        pFill.setColor(0x88FFFFFF);
        c.drawCircle(x - s * 0.12f, y - s * 0.30f, s * 0.16f, pFill);
    }

    // Все иконки интерфейса — контурные: линия ICON_STROKE от размера,
    // скруглённые концы; заливкой выделяется не больше одной точки-акцента.

    void iconGear(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        int teeth = 8;
        for (int i = 0; i < teeth * 4; i++) {
            // Зуб: два угла на внешнем радиусе, два — на внутреннем.
            double a = Math.toRadians(i * 360.0 / (teeth * 4) - 90.0 + 360.0 / (teeth * 8));
            float rr = (i % 4 == 0 || i % 4 == 1) ? s : s * 0.74f;
            float px = x + (float) Math.cos(a) * rr;
            float py = y + (float) Math.sin(a) * rr;
            if (i == 0) {
                iconPath.moveTo(px, py);
            } else {
                iconPath.lineTo(px, py);
            }
        }
        iconPath.close();
        strokeStyle(color, s * ICON_STROKE);
        c.drawPath(iconPath, pStroke);
        c.drawCircle(x, y, s * 0.30f, pStroke);
        strokeReset();
    }

    void iconFlag(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * ICON_STROKE);
        c.drawLine(x - s * 0.62f, y - s, x - s * 0.62f, y + s, pStroke);
        iconPath.reset();
        iconPath.moveTo(x - s * 0.62f, y - s * 0.88f);
        iconPath.quadTo(x - s * 0.05f, y - s * 1.10f, x + s * 0.30f, y - s * 0.80f);
        iconPath.quadTo(x + s * 0.55f, y - s * 0.60f, x + s * 0.78f, y - s * 0.72f);
        iconPath.lineTo(x + s * 0.78f, y + s * 0.02f);
        iconPath.quadTo(x + s * 0.55f, y + s * 0.14f, x + s * 0.30f, y - s * 0.06f);
        iconPath.quadTo(x - s * 0.05f, y - s * 0.36f, x - s * 0.62f, y - s * 0.14f);
        c.drawPath(iconPath, pStroke);
        strokeReset();
    }

    void iconOrb(Canvas c, float x, float y, float s, int color) {
        // Сфера со шлейфом: точки уходят влево и вниз, как в полёте.
        float ox = x + s * 0.22f;
        float oy = y - s * 0.12f;
        strokeStyle(color, s * ICON_STROKE);
        c.drawCircle(ox, oy, s * 0.70f, pStroke);
        strokeReset();
        pFill.setColor(color);
        c.drawCircle(ox, oy, s * 0.26f, pFill);
        pFill.setColor(withAlpha(color, 150));
        c.drawCircle(x - s * 0.80f, y + s * 0.56f, s * 0.13f, pFill);
        pFill.setColor(withAlpha(color, 90));
        c.drawCircle(x - s * 1.08f, y + s * 0.86f, s * 0.09f, pFill);
    }

    void iconBars(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.24f);
        float base = y + s * 0.80f;
        float[] hs = {0.70f, 1.20f, 1.65f};
        for (int i = 0; i < 3; i++) {
            float bx = x + (i - 1) * s * 0.62f;
            c.drawLine(bx, base, bx, base - s * hs[i], pStroke);
        }
        strokeReset();
    }

    /** Раскрытая книга — правила. */
    void iconDoc(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * ICON_STROKE);
        iconPath.reset();
        iconPath.moveTo(x, y - s * 0.62f);
        iconPath.quadTo(x - s * 0.45f, y - s * 0.92f, x - s * 0.98f, y - s * 0.76f);
        iconPath.lineTo(x - s * 0.98f, y + s * 0.66f);
        iconPath.quadTo(x - s * 0.45f, y + s * 0.50f, x, y + s * 0.82f);
        iconPath.quadTo(x + s * 0.45f, y + s * 0.50f, x + s * 0.98f, y + s * 0.66f);
        iconPath.lineTo(x + s * 0.98f, y - s * 0.76f);
        iconPath.quadTo(x + s * 0.45f, y - s * 0.92f, x, y - s * 0.62f);
        iconPath.lineTo(x, y + s * 0.82f);
        c.drawPath(iconPath, pStroke);
        strokeReset();
    }

    /** Четырёхлучевая вспышка: звёзды и «событие дня». */
    void iconSpark(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        iconPath.moveTo(x, y - s);
        iconPath.quadTo(x + s * 0.12f, y - s * 0.12f, x + s, y);
        iconPath.quadTo(x + s * 0.12f, y + s * 0.12f, x, y + s);
        iconPath.quadTo(x - s * 0.12f, y + s * 0.12f, x - s, y);
        iconPath.quadTo(x - s * 0.12f, y - s * 0.12f, x, y - s);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
    }

    /** Близкий пролёт: точка-сфера и дуга астероида рядом с ней. */
    void iconNearMiss(Canvas c, float x, float y, float s, int color) {
        pFill.setColor(color);
        c.drawCircle(x - s * 0.30f, y + s * 0.20f, s * 0.34f, pFill);
        strokeStyle(color, s * ICON_STROKE * 1.1f);
        tmpRect.set(x - s * 0.95f, y - s * 0.95f, x + s * 0.95f, y + s * 0.95f);
        c.drawArc(tmpRect, -100f, 130f, false, pStroke);
        strokeReset();
    }

    /** Дверь со стрелкой — выход в меню. */
    void iconExit(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * ICON_STROKE);
        iconPath.reset();
        iconPath.moveTo(x + s * 0.10f, y - s * 0.55f);
        iconPath.lineTo(x + s * 0.10f, y - s * 0.90f);
        iconPath.lineTo(x - s * 0.85f, y - s * 0.90f);
        iconPath.lineTo(x - s * 0.85f, y + s * 0.90f);
        iconPath.lineTo(x + s * 0.10f, y + s * 0.90f);
        iconPath.lineTo(x + s * 0.10f, y + s * 0.55f);
        c.drawPath(iconPath, pStroke);
        c.drawLine(x - s * 0.30f, y, x + s * 0.95f, y, pStroke);
        c.drawLine(x + s * 0.60f, y - s * 0.35f, x + s * 0.95f, y, pStroke);
        c.drawLine(x + s * 0.60f, y + s * 0.35f, x + s * 0.95f, y, pStroke);
        strokeReset();
    }

    /** Две стрелки вверх — улучшение в мастерской. */
    void iconUpgrade(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * ICON_STROKE * 1.2f);
        for (int k = 0; k < 2; k++) {
            float cy = y - s * 0.30f + k * s * 0.70f;
            c.drawLine(x - s * 0.75f, cy + s * 0.40f, x, cy - s * 0.35f, pStroke);
            c.drawLine(x, cy - s * 0.35f, x + s * 0.75f, cy + s * 0.40f, pStroke);
        }
        strokeReset();
    }

    /** Кубок — рекорд и достижения. */
    void iconTrophy(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * ICON_STROKE * 1.2f);
        iconPath.reset();
        iconPath.moveTo(x - s * 0.62f, y - s * 0.85f);
        iconPath.lineTo(x + s * 0.62f, y - s * 0.85f);
        iconPath.lineTo(x + s * 0.62f, y - s * 0.20f);
        iconPath.quadTo(x + s * 0.62f, y + s * 0.42f, x, y + s * 0.42f);
        iconPath.quadTo(x - s * 0.62f, y + s * 0.42f, x - s * 0.62f, y - s * 0.20f);
        iconPath.close();
        c.drawPath(iconPath, pStroke);
        tmpRect.set(x + s * 0.40f, y - s * 0.70f, x + s * 1.05f, y - s * 0.05f);
        c.drawArc(tmpRect, -90f, 180f, false, pStroke);
        tmpRect.set(x - s * 1.05f, y - s * 0.70f, x - s * 0.40f, y - s * 0.05f);
        c.drawArc(tmpRect, 90f, 180f, false, pStroke);
        c.drawLine(x, y + s * 0.42f, x, y + s * 0.82f, pStroke);
        c.drawLine(x - s * 0.45f, y + s * 0.95f, x + s * 0.45f, y + s * 0.95f, pStroke);
        strokeReset();
    }

    /** Часы — время забега. */
    void iconClock(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * ICON_STROKE * 1.1f);
        c.drawCircle(x, y, s * 0.9f, pStroke);
        c.drawLine(x, y, x, y - s * 0.5f, pStroke);
        c.drawLine(x, y, x + s * 0.38f, y + s * 0.25f, pStroke);
        strokeReset();
    }

    void iconLock(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.20f);
        tmpRect.set(x - s * 0.45f, y - s * 1.0f, x + s * 0.45f, y - s * 0.1f);
        c.drawArc(tmpRect, 180f, 180f, false, pStroke);
        c.drawLine(x - s * 0.45f, y - s * 0.55f, x - s * 0.45f, y - s * 0.1f, pStroke);
        c.drawLine(x + s * 0.45f, y - s * 0.55f, x + s * 0.45f, y - s * 0.1f, pStroke);
        strokeReset();
        box(c, x - s * 0.72f, y - s * 0.15f, x + s * 0.72f, y + s * 0.9f, s * 0.2f,
                color, 0);
    }

    void iconNote(Canvas c, float x, float y, float s, int color) {
        pFill.setColor(color);
        c.drawCircle(x - s * 0.35f, y + s * 0.6f, s * 0.36f, pFill);
        strokeStyle(color, s * 0.16f);
        c.drawLine(x - s * 0.02f, y + s * 0.6f, x - s * 0.02f, y - s * 0.9f, pStroke);
        c.drawLine(x - s * 0.02f, y - s * 0.9f, x + s * 0.6f, y - s * 0.55f, pStroke);
        strokeReset();
    }

    void iconSpeaker(Canvas c, float x, float y, float s, int color) {
        iconPath.reset();
        iconPath.moveTo(x - s * 0.9f, y - s * 0.32f);
        iconPath.lineTo(x - s * 0.5f, y - s * 0.32f);
        iconPath.lineTo(x - s * 0.02f, y - s * 0.8f);
        iconPath.lineTo(x - s * 0.02f, y + s * 0.8f);
        iconPath.lineTo(x - s * 0.5f, y + s * 0.32f);
        iconPath.lineTo(x - s * 0.9f, y + s * 0.32f);
        iconPath.close();
        strokeStyle(color, s * ICON_STROKE);
        c.drawPath(iconPath, pStroke);
        tmpRect.set(x - s * 0.35f, y - s * 0.5f, x + s * 0.55f, y + s * 0.5f);
        c.drawArc(tmpRect, -50f, 100f, false, pStroke);
        tmpRect.set(x - s * 0.55f, y - s * 0.9f, x + s * 0.95f, y + s * 0.9f);
        c.drawArc(tmpRect, -50f, 100f, false, pStroke);
        strokeReset();
    }

    void iconVibrate(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.15f);
        tmpRect.set(x - s * 0.40f, y - s * 0.85f, x + s * 0.40f, y + s * 0.85f);
        c.drawRoundRect(tmpRect, s * 0.18f, s * 0.18f, pStroke);
        c.drawLine(x - s * 0.72f, y - s * 0.35f, x - s * 0.72f, y + s * 0.35f, pStroke);
        c.drawLine(x + s * 0.72f, y - s * 0.35f, x + s * 0.72f, y + s * 0.35f, pStroke);
        c.drawLine(x - s * 1.0f, y - s * 0.18f, x - s * 1.0f, y + s * 0.18f, pStroke);
        c.drawLine(x + s * 1.0f, y - s * 0.18f, x + s * 1.0f, y + s * 0.18f, pStroke);
        strokeReset();
    }

    void iconReplay(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.18f);
        tmpRect.set(x - s * 0.75f, y - s * 0.75f, x + s * 0.75f, y + s * 0.75f);
        c.drawArc(tmpRect, -60f, 300f, false, pStroke);
        strokeReset();
        iconPath.reset();
        float ax = x + s * 0.375f;
        float ay = y - s * 0.65f;
        iconPath.moveTo(ax + s * 0.42f, ay - s * 0.05f);
        iconPath.lineTo(ax - s * 0.10f, ay - s * 0.40f);
        iconPath.lineTo(ax - s * 0.05f, ay + s * 0.30f);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
    }

    /** Косая черта поверх иконки выключенной настройки. */
    void iconSlash(Canvas c, float x, float y, float s, int color) {
        strokeStyle(0xFF0E1630, s * 0.36f);
        c.drawLine(x - s, y - s, x + s, y + s, pStroke);
        strokeStyle(color, s * 0.15f);
        c.drawLine(x - s, y - s, x + s, y + s, pStroke);
        strokeReset();
    }

    int modeColor(int m) {
        return historyColor(m);
    }

    int countWrapped(String s, float size, float maxW, Typeface tf) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setLetterSpacing(0f);
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && pText.measureText(test) > maxW) {
                lines++;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }
        if (line.length() > 0) {
            lines++;
        }
        return Math.max(1, lines);
    }

    float drawWrapped(Canvas c, String s, float x, float y, float size,
                              float maxW, int color, Typeface tf,
                              Paint.Align align, float lineH) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setTextAlign(align);
        pText.setLetterSpacing(0f);
        pText.clearShadowLayer();
        pText.setColor(color);

        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && pText.measureText(test) > maxW) {
                c.drawText(line.toString(), x, y, pText);
                y += lineH;
                lines++;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }
        if (line.length() > 0) {
            c.drawText(line.toString(), x, y, pText);
            lines++;
        }
        return Math.max(1, lines) * lineH;
    }

    String formatTime(int totalSec) {
        if (totalSec < 0) {
            totalSec = 0;
        }
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        if (h > 0) {
            return h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        }
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    void text(Canvas c, String s, float x, float y, float size,
                      int color, Typeface tf, Paint.Align align) {
        pText.setTypeface(tf);
        pText.setTextSize(size);
        pText.setTextAlign(align);
        pText.setLetterSpacing(0f);
        pText.clearShadowLayer();
        pText.setColor(color);
        c.drawText(s, x, y, pText);
    }

    void textGlow(Canvas c, String s, float x, float y, float size,
                          int color, int glow, Typeface tf) {
        textGlow(c, s, x, y, size, color, glow, tf, 0.30f);
    }

    /** Текст со свечением; spread — радиус ореола в долях кегля. */
    void textGlow(Canvas c, String s, float x, float y, float size,
                  int color, int glow, Typeface tf, float spread) {
        pText.setTypeface(tf);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setLetterSpacing(0.02f);
        pText.setTextSize(size);
        pText.setColor(color);
        pText.setShadowLayer(size * spread, 0f, 0f, glow);
        c.drawText(s, x, y, pText);
        pText.clearShadowLayer();
        pText.setLetterSpacing(0f);
    }
}
