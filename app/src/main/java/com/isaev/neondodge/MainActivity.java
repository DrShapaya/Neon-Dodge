package com.isaev.neondodge;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.graphics.Color;
import android.widget.LinearLayout;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.YandexAds;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import java.util.Locale;

public class MainActivity extends Activity {

    private GameView game;
    private BannerAdView banner;
    private RewardedAdLoader rewardedLoader;
    private RewardedAd rewardedAd;
    private boolean rewardedShowing;

    private static final String BANNER_AD_UNIT_ID = "R-M-20265890-3";
    private static final String REWARDED_AD_UNIT_ID = "R-M-20265890-2";

    @Override
    protected void attachBaseContext(Context newBase) {
        String tag = newBase.getSharedPreferences("neon_dodge", MODE_PRIVATE)
                .getString("language", "ru");
        super.attachBaseContext(withLocale(newBase, tag));
    }

    private static Context withLocale(Context context, String tag) {
        Locale locale = Locale.forLanguageTag(tag == null || tag.isEmpty() ? "ru" : tag);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.setLocale(locale);
        }
        return context.createConfigurationContext(config);
    }

    void setLanguage(String tag) {
        getSharedPreferences("neon_dodge", MODE_PRIVATE).edit()
                .putString("language", tag).apply();
        recreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = new GameView(this);

        // Sticky banner gets its own row, so it never covers the game controls.
        // The game keeps all of its existing coordinate system in the remaining space.
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        banner = new BannerAdView(this);
        root.addView(banner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(game, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
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

        YandexAds.initialize(this, () -> {
            loadBanner();
            loadRewarded();
        });
    }

    private void loadBanner() {
        if (banner == null || isFinishing()) {
            return;
        }
        int widthPx = banner.getWidth();
        if (widthPx <= 0) {
            widthPx = getResources().getDisplayMetrics().widthPixels;
        }
        int widthDp = Math.max(1, Math.round(widthPx
                / getResources().getDisplayMetrics().density));
        banner.setAdSize(BannerAdSize.sticky(this, widthDp));
        banner.loadAd(new AdRequest.Builder(BANNER_AD_UNIT_ID).build());
    }

    private void loadRewarded() {
        rewardedLoader = new RewardedAdLoader(this);
        rewardedLoader.loadAd(new AdRequest.Builder(REWARDED_AD_UNIT_ID).build(),
                new RewardedAdLoadListener() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(AdRequestError error) {
                        rewardedAd = null;
                    }
                });
    }

    /** Called by the game only when the player taps the revive CTA. */
    void showRewardedRevive() {
        if (rewardedShowing) {
            return;
        }
        if (rewardedAd == null) {
            game.showAdLoadingMessage();
            return;
        }
        final RewardedAd ad = rewardedAd;
        rewardedShowing = true;
        ad.setAdEventListener(new RewardedAdEventListener() {
            @Override
            public void onAdShown() {
            }

            @Override
            public void onAdFailedToShow(AdError error) {
                rewardedShowing = false;
                rewardedAd = null;
                loadRewarded();
                game.showAdLoadingMessage();
            }

            @Override
            public void onAdDismissed() {
                rewardedShowing = false;
                if (rewardedAd == ad) {
                    rewardedAd = null;
                }
                ad.setAdEventListener(null);
                loadRewarded();
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression(ImpressionData impressionData) {
            }

            @Override
            public void onRewarded(Reward reward) {
                game.reviveAfterReward();
            }
        });
        ad.show(this);
    }

    private void applyImmersiveMode() {
        Window window = getWindow();
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

    @Override
    protected void onDestroy() {
        if (banner != null) {
            banner.destroy();
            banner = null;
        }
        if (rewardedAd != null) {
            rewardedAd.setAdEventListener(null);
            rewardedAd = null;
        }
        rewardedLoader = null;
        super.onDestroy();
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
