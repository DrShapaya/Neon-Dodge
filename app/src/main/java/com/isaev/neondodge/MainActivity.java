package com.isaev.neondodge;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;

public class MainActivity extends Activity {

    private GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        game = new GameView(this);
        setContentView(game);
        applyImmersiveMode();
        registerBackHandler();

        game.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = i.top;
                bottom = i.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            game.setSafeInsets(top, bottom);
            return insets;
        });
        game.requestApplyInsets();
    }

    private void applyImmersiveMode() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (game != null) {
            game.onHostResume();
        }
    }

    @Override
    protected void onPause() {
        if (game != null) {
            game.onHostPause();
        }
        super.onPause();
    }

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BackHandler33.register(this, this::handleBack);
        }
    }

    private void handleBack() {
        if (game == null || !game.handleBack()) {
            finish();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    @SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        // На Android 13+ событие приходит через OnBackInvokedCallback.
        // На старых версиях этот метод остаётся единственным обработчиком.
        handleBack();
    }

    /** Изолирует API 33+, чтобы класс не загружался на Android 8–12. */
    private static final class BackHandler33 {
        private BackHandler33() {
        }

        @TargetApi(Build.VERSION_CODES.TIRAMISU)
        static void register(Activity activity, Runnable handler) {
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> handler.run());
        }
    }
}
