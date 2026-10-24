package com.isaev.neondodge;

import android.content.Context;
import android.view.MotionEvent;

/**
 * Вся игра нарисована на одном Canvas: фон, астероиды, частицы, HUD и меню.
 * Мир масштабируется по ширине экрана (1080 условных единиц).
 *
 * Код разбит на слои-наследники, снизу вверх: {@link GameCore} → {@link GameResources}
 * → {@link GameAudio} → {@link GameSpawner}
 * → {@link GameLoop} → {@link GameRenderer} → {@link UiKit} → {@link HudLayer}
 * → {@link MenuScreen} → {@link InfoScreens} → {@link OverlayScreens} → GameView.
 * Здесь — обработка касаний.
 */
public class GameView extends OverlayScreens {

    public GameView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (scale <= 0f) {
            return true;
        }
        float x = (e.getX() - viewOffsetX) / scale;
        float y = e.getY() / scale;
        int action = e.getActionMasked();

        if (state == ST_PLAY) {
            if (inputLock > 0f) {
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN && hitAt(x, y) == B_PAUSE) {
                pauseGame();
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                activePointerId = e.getPointerId(0);
            } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
                return true;
            } else if (action == MotionEvent.ACTION_POINTER_UP
                    && e.getPointerId(e.getActionIndex()) == activePointerId) {
                int next = e.getActionIndex() == 0 ? 1 : 0;
                activePointerId = next < e.getPointerCount() ? e.getPointerId(next)
                        : MotionEvent.INVALID_POINTER_ID;
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                int pointerIndex = e.findPointerIndex(activePointerId);
                if (pointerIndex < 0) {
                    return true;
                }
                x = (e.getX(pointerIndex) - viewOffsetX) / scale;
                y = e.getY(pointerIndex) / scale;
                float ny = touchTargetY(y);
                if (!tutorialDone
                        && (Math.abs(x - targetX) > 25f || Math.abs(ny - targetY) > 25f)) {
                    tutorialDone = true;
                    prefs.edit().putBoolean("tutorial_done", true).apply();
                }
                targetX = clamp(x, 0f, W);
                targetY = ny;
            }
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (inputLock > 0f) {
                    gestureIgnored = true;
                    pressedId = B_NONE;
                    return true;
                }
                gestureIgnored = false;
                touchDownX = x;
                touchDownY = y;
                touchMoved = false;
                pressedId = hitAt(x, y);
                if (pressedId == B_SLIDER) {
                    draggingOffset = true;
                    setOffsetFromX(x);
                }
                scrollDragging = isScrollScreen() && !draggingOffset
                        && y >= scrollTop && y <= scrollBottom;
                scrollVel = 0f;
                scrollLastY = y;
                scrollLastT = e.getEventTime();
                break;

            case MotionEvent.ACTION_MOVE:
                if (gestureIgnored) {
                    break;
                }
                if (draggingOffset) {
                    setOffsetFromX(x);
                    break;
                }
                if (!touchMoved && (Math.abs(y - touchDownY) > 16f
                        || Math.abs(x - touchDownX) > 16f)) {
                    touchMoved = true;
                }
                if (scrollDragging && touchMoved) {
                    long t = e.getEventTime();
                    float dy = y - scrollLastY;
                    float dtEv = Math.max(1L, t - scrollLastT) / 1000f;
                    scrollY = clamp(scrollY - dy, 0f, scrollMax);
                    scrollVel = 0.7f * (-dy / dtEv) + 0.3f * scrollVel;
                    scrollLastY = y;
                    scrollLastT = t;
                }
                break;

            case MotionEvent.ACTION_UP: {
                if (gestureIgnored) {
                    break;
                }
                if (draggingOffset) {
                    draggingOffset = false;
                    pressedId = B_NONE;
                    break;
                }
                int id = pressedId;
                pressedId = B_NONE;
                boolean scrolled = scrollDragging && touchMoved;
                scrollDragging = false;
                if (scrolled) {
                    if (e.getEventTime() - scrollLastT > 90L) {
                        scrollVel = 0f;
                    }
                    scrollVel = clamp(scrollVel, -7000f, 7000f);
                } else {
                    scrollVel = 0f;
                    if (id != B_NONE && hitAt(x, y) == id) {
                        onButton(id);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
                pressedId = B_NONE;
                scrollDragging = false;
                draggingOffset = false;
                break;

            default:
                break;
        }
        return true;
    }

    void onButton(int id) {
        switch (id) {
            case B_PLAY:
            case B_EMPTY_PLAY:
                startGame();
                break;
            case B_DAILY:
                startDailyChallenge();
                break;
            case B_QUESTS:
                go(ST_QUESTS);
                break;
            case B_SKINS:
                go(ST_SKINS);
                break;
            case B_WORKSHOP:
                go(ST_WORKSHOP);
                break;
            case B_WORKSHOP_UPGRADE:
                if (progress.upgradeMultiplier()) {
                    toast(sWorkshop + " · +" + fmtMult(progress.starMultiplierStep()));
                    buzz(18);
                } else {
                    toast(sWorkshopNext + " · " + fmt(progress.multiplierUpgradeCost()));
                    buzz(10);
                }
                break;
            case B_WORKSHOP_UPGRADE_CLOSE:
                if (progress.upgradeCloseMultiplier()) {
                    toast(sWorkshopClose + " · +" + fmtMult(progress.closeMultiplierStep()));
                    buzz(18);
                } else {
                    toast(sWorkshopNext + " · " + fmt(progress.closeMultiplierUpgradeCost()));
                    buzz(10);
                }
                break;
            case B_WORKSHOP_UPGRADE_BOSS:
                if (progress.upgradeBossMultiplier()) {
                    toast(sWorkshopBoss + " · x" + fmtMult(progress.bossMultiplier()));
                    buzz(18);
                } else {
                    toast(sWorkshopNext + " · " + fmt(progress.bossMultiplierUpgradeCost()));
                    buzz(10);
                }
                break;
            case B_WORKSHOP_UPGRADE_LEVEL:
                if (progress.upgradeLevelMultiplier()) {
                    toast(sWorkshopLevelMult + " · +" + fmtMult(progress.levelMultiplierStep()));
                    buzz(18);
                } else {
                    toast(sWorkshopNext + " · " + fmt(progress.levelMultiplierUpgradeCost()));
                    buzz(10);
                }
                break;
            case B_STATS:
                go(ST_STATS);
                break;
            case B_RULES:
                rulesReturn = ST_MENU;
                go(ST_RULES);
                break;
            case B_SETTINGS:
                go(ST_SETTINGS);
                break;
            case B_BACK:
                handleBack();
                break;
            case B_HISTORY:
                go(ST_HISTORY);
                break;
            case B_CONTINUE:
                resumePlay();
                break;
            case B_PAUSE_RULES:
                rulesReturn = ST_PAUSE;
                go(ST_RULES);
                break;
            case B_EXIT:
                armOrExit();
                break;
            case B_MUSIC:
                toggleMusic();
                buzz(10);
                break;
            case B_SOUND:
                toggleSound();
                buzz(10);
                break;
            case B_VIBRATION:
                toggleVibration();
                break;
            case B_AGAIN:
                if (overTime > 0.5f) {
                    startGame();
                }
                break;
            case B_MENU:
                if (overTime > 0.5f) {
                    resetMenu();
                }
                break;
            case B_TUTORIAL:
                tutorialDone = false;
                prefs.edit().putBoolean("tutorial_done", false).apply();
                toast(sTutorialReset);
                buzz(12);
                break;
            default:
                if (id >= B_MODE && id < B_MODE + Progress.MODE_COUNT) {
                    mode = id - B_MODE;
                    progress.mode = mode;
                    progress.saveMode();
                    buzz(12);
                } else if (id == B_CONTROL + CONTROL_ABOVE || id == B_CONTROL + CONTROL_AT) {
                    int cm = id - B_CONTROL;
                    if (controlMode != cm) {
                        controlMode = cm;
                        prefs.edit().putInt("control_mode", controlMode).apply();
                        buzz(12);
                    }
                } else if (id == B_TAB || id == B_TAB + 1) {
                    if (skinTab != id - B_TAB) {
                        skinTab = id - B_TAB;
                        scrollY = 0f;
                        scrollVel = 0f;
                    }
                } else if (id >= B_SKIN && id < B_SKIN + Progress.SPHERE_COUNT) {
                    selectSkin(id - B_SKIN);
                }
                break;
        }
    }

    void selectSkin(int idx) {
        if (skinTab == 0) {
            if (idx >= Progress.SPHERE_COUNT) {
                return;
            }
            if (progress.sphereUnlocked(idx)) {
                progress.sphereSkin = idx;
                progress.saveSkins();
                buzz(14);
            } else {
                toast(sSphereCond[idx]);
                buzz(10);
            }
        } else {
            if (idx >= Progress.TRAIL_COUNT) {
                return;
            }
            if (progress.trailUnlocked(idx)) {
                progress.trailSkin = idx;
                progress.saveSkins();
                buzz(14);
            } else {
                toast(sTrailCond[idx]);
                buzz(10);
            }
        }
    }

    void pauseGame() {
        state = ST_PAUSE;
        inputLock = 0.25f;
        exitArmed = 0f;
        pressedId = B_NONE;
    }

    /** Продолжение после паузы: короткий отсчёт, мир стоит, сферу можно двигать. */
    void resumePlay() {
        state = ST_PLAY;
        resumeTimer = RESUME_TIME;
        exitArmed = 0f;
        inputLock = 0.12f;
    }

    /** Первый тап по «Выйти в меню» только предупреждает — забег не теряется случайно. */
    void armOrExit() {
        if (exitArmed > 0f) {
            exitArmed = 0f;
            resetMenu();
        } else {
            exitArmed = EXIT_CONFIRM_TIME;
            buzz(14);
        }
    }

    public boolean handleBack() {
        switch (state) {
            case ST_PLAY:
                pauseGame();
                return true;
            case ST_RULES:
                go(rulesReturn);
                return true;
            case ST_HISTORY:
                go(ST_STATS);
                return true;
            case ST_SETTINGS:
            case ST_STATS:
            case ST_QUESTS:
            case ST_SKINS:
            case ST_WORKSHOP:
                go(ST_MENU);
                return true;
            case ST_PAUSE:
                armOrExit();
                return true;
            case ST_OVER:
                resetMenu();
                return true;
            default:
                return false;
        }
    }

    float pauseBtnX() {
        return W - UI_PAD - 42f;
    }

    float pauseBtnY() {
        return insetTop + 66f;
    }

    void setOffsetFromX(float x) {
        float t = clamp((x - sliderX1) / (sliderX2 - sliderX1), 0f, 1f);
        fingerOffset = Math.round((OFFSET_MIN + t * (OFFSET_MAX - OFFSET_MIN)) / 5f) * 5f;
        fingerOffset = clamp(fingerOffset, OFFSET_MIN, OFFSET_MAX);
        prefs.edit().putFloat("finger_offset", fingerOffset).apply();
    }
}
