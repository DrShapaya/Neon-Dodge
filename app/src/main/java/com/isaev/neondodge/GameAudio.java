package com.isaev.neondodge;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build;

/** Звуки, музыка, аудиофокус и вибрация. */
abstract class GameAudio extends GameResources {

    GameAudio(Context context) {
        super(context);
    }

    void initAudio() {
        if (soundPool != null) {
            return;
        }
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            SoundPool pool = new SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(attrs)
                    .build();
            int coin = pool.load(getContext(), R.raw.coin, 1);
            int hit = pool.load(getContext(), R.raw.hit, 1);
            int power = pool.load(getContext(), R.raw.power, 1);
            soundPool = pool;
            sndCoin = coin;
            sndHit = hit;
            sndPower = power;
        } catch (Exception e) {
            soundPool = null;
        }
        try {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            vibrator = null;
        }
    }

    /** Загрузка коротких звуков не должна блокировать первый кадр меню. */
    void startAudioInit() {
        if (audioInitStarted || soundPool != null) {
            return;
        }
        audioInitStarted = true;
        Thread thread = new Thread(() -> {
            try {
                initAudio();
            } finally {
                audioInitStarted = false;
            }
        }, "neon-audio-init");
        thread.start();
    }

    void initMusic() {
        if (musicPlayer != null) {
            return;
        }
        try {
            musicPlayer = MediaPlayer.create(getContext(),
                    musicTrack == 0 ? R.raw.music : R.raw.music_alt);
            if (musicPlayer != null) {
                musicPlayer.setLooping(true);
                musicPlayer.setVolume(0.45f * musicVolume, 0.45f * musicVolume);
                if (musicOn && requestAudioFocus()) {
                    musicPlayer.start();
                }
            }
        } catch (Exception e) {
            musicPlayer = null;
        }
    }

    boolean requestAudioFocus() {
        if (audioManager == null) {
            audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        if (audioManager == null) {
            return true;
        }
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setOnAudioFocusChangeListener(change -> {
                            if (change == AudioManager.AUDIOFOCUS_LOSS
                                    || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                                pauseMusic();
                            } else if (change == AudioManager.AUDIOFOCUS_GAIN && musicOn) {
                                startMusicIfEnabled();
                            }
                        })
                        .build();
            }
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(change -> {
                if (change <= 0) {
                    pauseMusic();
                } else if (musicOn) {
                    startMusicIfEnabled();
                }
            }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        audioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return audioFocusHeld;
    }

    void abandonAudioFocus() {
        if (!audioFocusHeld || audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        }
        audioFocusHeld = false;
    }

    void startMusicIfEnabled() {
        if (!musicOn) {
            return;
        }
        if (!requestAudioFocus()) {
            return;
        }
        if (musicPlayer == null) {
            initMusic();
            return;
        }
        try {
            if (!musicPlayer.isPlaying()) {
                musicPlayer.start();
            }
        } catch (Exception ignored) {
        }
    }

    void pauseMusic() {
        if (musicPlayer == null) {
            return;
        }
        try {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
            }
        } catch (Exception ignored) {
        }
    }

    void toggleMusic() {
        musicOn = !musicOn;
        prefs.edit().putBoolean("music", musicOn).apply();
        if (musicOn) {
            startMusicIfEnabled();
        } else {
            pauseMusic();
        }
    }

    void setMusicVolume(float value) {
        musicVolume = clamp(value, 0f, 1f);
        prefs.edit().putFloat("music_volume", musicVolume).apply();
        if (musicPlayer != null) {
            musicPlayer.setVolume(0.45f * musicVolume, 0.45f * musicVolume);
        }
    }

    void releaseMusic() {
        abandonAudioFocus();
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                musicPlayer.release();
            } catch (Exception ignored) {
            }
            musicPlayer = null;
        }
    }

    void releaseAudio() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    void toggleSound() {
        soundOn = !soundOn;
        prefs.edit().putBoolean("sound", soundOn).apply();
        if (soundOn) {
            playSound(sndCoin, 1.1f);
        }
    }

    void setSoundVolume(float value) {
        soundVolume = clamp(value, 0f, 1f);
        prefs.edit().putFloat("sound_volume", soundVolume).apply();
        if (soundOn) {
            playSound(sndCoin, 1.1f);
        }
    }

    void toggleVibration() {
        vibOn = !vibOn;
        prefs.edit().putBoolean("vibrate", vibOn).apply();
        if (vibOn) {
            buzz(25);
        }
    }

    void playSound(int id, float rate) {
        if (!soundOn || soundPool == null || id == 0) {
            return;
        }
        try {
            float volume = 0.55f * soundVolume;
            soundPool.play(id, volume, volume, 1, 0, clamp(rate, 0.5f, 2f));
        } catch (Exception ignored) {
        }
    }

    void buzz(int ms) {
        if (!vibOn || vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(ms,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {
        }
    }
}
