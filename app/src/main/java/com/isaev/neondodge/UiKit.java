package com.isaev.neondodge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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

    /** Карточка; цветная полоса слева кодирует статус или режим. */
    void card(Canvas c, float l, float t, float r, float b, int accent,
                      boolean highlight) {
        box(c, l, t, r, b, 28f, highlight ? UI_SURFACE_HI : UI_SURFACE,
                highlight ? 0x995FF3FF : UI_LINE);
        if (accent != 0) {
            box(c, l + 12f, t + 24f, l + 18f, b - 24f, 3f, accent, 0);
        }
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
        boolean down = pressed(id);
        float in = down ? 5f : 0f;
        c.save();
        c.scale(w / h * 0.62f, 1f, cx, cy);
        drawGlow(c, cx, cy, h * 2.4f, color, (int) ((down ? 50f : 70f) + 40f * breathe));
        c.restore();
        box(c, cx - w / 2f + in, cy - h / 2f + in, cx + w / 2f - in, cy + h / 2f - in,
                (h - 2f * in) / 2f, down ? mix(color, 0xFF000000, 0.22f) : color, 0);
        float size = Math.min(48f, h * 0.34f);
        float tw = measure(label, size, tfBlack);
        float tri = size * 0.42f;
        float tx = cx + tri;
        if (id == B_PLAY || id == B_AGAIN || id == B_EMPTY_PLAY || id == B_CONTINUE) {
            float ix = cx - tw / 2f - tri * 0.4f;
            iconPlay(c, ix, cy, tri, UI_ON_ACCENT);
        } else {
            tx = cx;
        }
        textC(c, label, tx, cy, size, UI_ON_ACCENT, tfBlack, Paint.Align.CENTER);
        addHit(id, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    void secondaryButton(Canvas c, int id, float cx, float cy, float w, float h,
                                 String label, int accent, int textColor) {
        boolean down = pressed(id);
        float in = down ? 4f : 0f;
        box(c, cx - w / 2f + in, cy - h / 2f + in, cx + w / 2f - in, cy + h / 2f - in,
                (h - 2f * in) / 2f, down ? UI_SURFACE_HI : 0x990E1630,
                withAlpha(accent, down ? 200 : 120));
        textC(c, label, cx, cy, Math.min(34f, h * 0.30f), textColor,
                tfCond, Paint.Align.CENTER);
        addHit(id, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
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
        float w = 92f;
        float h = 52f;
        box(c, cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h / 2f,
                on ? 0x665FF3FF : 0x22FFFFFF, on ? 0 : 0x22FFFFFF);
        float kx = on ? cx + w / 2f - h / 2f : cx - w / 2f + h / 2f;
        if (on) {
            drawGlow(c, kx, cy, 90f, C_CYAN, 90);
        }
        pFill.setColor(on ? 0xFFFFFFFF : 0xFF7D90A6);
        c.drawCircle(kx, cy, h / 2f - 6f, pFill);
    }

    float sectionLabel(Canvas c, String s, float y) {
        text(c, s, UI_PAD + 8f, y + 34f, 27f, UI_FOG, tfMed, Paint.Align.LEFT);
        return y + 56f;
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

    void iconGear(Canvas c, float x, float y, float s, int color) {
        pFill.setColor(color);
        for (int i = 0; i < 8; i++) {
            c.save();
            c.rotate(i * 45f, x, y);
            tmpRect.set(x - s * 0.17f, y - s, x + s * 0.17f, y - s * 0.55f);
            c.drawRoundRect(tmpRect, s * 0.06f, s * 0.06f, pFill);
            c.restore();
        }
        strokeStyle(color, s * 0.26f);
        c.drawCircle(x, y, s * 0.58f, pStroke);
        strokeReset();
    }

    void iconFlag(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.16f);
        c.drawLine(x - s * 0.62f, y - s, x - s * 0.62f, y + s, pStroke);
        strokeReset();
        iconPath.reset();
        iconPath.moveTo(x - s * 0.55f, y - s * 0.92f);
        iconPath.lineTo(x + s * 0.80f, y - s * 0.55f);
        iconPath.lineTo(x - s * 0.55f, y - s * 0.05f);
        iconPath.close();
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
    }

    void iconOrb(Canvas c, float x, float y, float s, int color) {
        // Сфера со шлейфом: точки уходят влево и вниз, как в полёте.
        pFill.setColor(withAlpha(color, 80));
        c.drawCircle(x - s * 1.05f, y + s * 0.58f, s * 0.14f, pFill);
        pFill.setColor(withAlpha(color, 130));
        c.drawCircle(x - s * 0.80f, y + s * 0.30f, s * 0.20f, pFill);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(s * 0.12f);
        pStroke.setColor(withAlpha(color, 150));
        c.drawCircle(x + s * 0.20f, y - s * 0.10f, s * 0.86f, pStroke);
        pStroke.setStyle(Paint.Style.FILL);
        pFill.setColor(color);
        c.drawCircle(x + s * 0.20f, y - s * 0.10f, s * 0.56f, pFill);
        pFill.setColor(0xFFFFFFFF);
        c.drawCircle(x + s * 0.20f, y - s * 0.10f, s * 0.22f, pFill);
    }

    void iconBars(Canvas c, float x, float y, float s, int color) {
        pFill.setColor(color);
        float w = s * 0.40f;
        float base = y + s * 0.85f;
        float[] hs = {0.85f, 1.35f, 1.80f};
        for (int i = 0; i < 3; i++) {
            float bx = x + (i - 1) * s * 0.66f;
            tmpRect.set(bx - w / 2f, base - s * hs[i], bx + w / 2f, base);
            c.drawRoundRect(tmpRect, w * 0.3f, w * 0.3f, pFill);
        }
    }

    void iconDoc(Canvas c, float x, float y, float s, int color) {
        strokeStyle(color, s * 0.15f);
        tmpRect.set(x - s * 0.72f, y - s, x + s * 0.72f, y + s);
        c.drawRoundRect(tmpRect, s * 0.22f, s * 0.22f, pStroke);
        c.drawLine(x - s * 0.36f, y - s * 0.45f, x + s * 0.36f, y - s * 0.45f, pStroke);
        c.drawLine(x - s * 0.36f, y, x + s * 0.36f, y, pStroke);
        c.drawLine(x - s * 0.36f, y + s * 0.45f, x + s * 0.12f, y + s * 0.45f, pStroke);
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
        pFill.setColor(color);
        c.drawPath(iconPath, pFill);
        strokeStyle(color, s * 0.14f);
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
        pText.setTypeface(tf);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setLetterSpacing(0.02f);
        pText.setTextSize(size);
        pText.setColor(color);
        pText.setShadowLayer(size * 0.30f, 0f, 0f, glow);
        c.drawText(s, x, y, pText);
        pText.clearShadowLayer();
        pText.setLetterSpacing(0f);
    }
}
