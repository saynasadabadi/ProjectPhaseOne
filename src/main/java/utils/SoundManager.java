package utils;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    public enum SoundEffect {
        BACKGROUND_MUSIC("sounds/background.wav"),
        WIRE_CONNECTED("sounds/connect.wav"),
        LEVEL_END("sounds/level_end.wav"),
        PACKET_DAMAGE("sounds/damage.wav"),
        GAME_OVER("sounds/game_over.wav"); // Added a specific game_over sound

        private final String filePath;
        private Clip clip;
        private boolean loaded = false;

        SoundEffect(String filePath) {
            this.filePath = filePath;
        }

        public String getFilePath() {
            return filePath;
        }

        public Clip getClip() {
            return clip;
        }

        public void setClip(Clip clip) {
            this.clip = clip;
            this.loaded = (clip != null);
        }

        public boolean isLoaded() {
            return loaded;
        }
    }

    private static boolean soundEnabled = true; // Global toggle for sound

    static {
        // Pre-load all sound effects
        for (SoundEffect effect : SoundEffect.values()) {
            loadSound(effect);
        }
    }

    private static void loadSound(SoundEffect effect) {
        if (!soundEnabled) return;
        try {
            URL soundURL = SoundManager.class.getClassLoader().getResource(effect.getFilePath());
            if (soundURL == null) {
                System.err.println("Sound file not found: " + effect.getFilePath());
                return;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            effect.setClip(clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading sound " + effect.getFilePath() + ": " + e.getMessage());
            effect.setClip(null);
        }
    }

    public static void playSound(SoundEffect effect) {
        if (!soundEnabled || !effect.isLoaded() || effect.getClip() == null) return;

        Clip clip = effect.getClip();
        if (clip.isRunning()) {
            clip.stop(); // Stop if already playing
        }
        clip.setFramePosition(0); // Rewind to the beginning
        clip.start();
    }

    public static void loopSound(SoundEffect effect) {
        if (!soundEnabled || !effect.isLoaded() || effect.getClip() == null) return;

        Clip clip = effect.getClip();
        if (!clip.isRunning()) {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void stopSound(SoundEffect effect) {
        if (!soundEnabled || !effect.isLoaded() || effect.getClip() == null) return;

        Clip clip = effect.getClip();
        if (clip.isRunning()) {
            clip.stop();
        }
    }

    public static void stopAllSounds() {
        if (!soundEnabled) return;
        for (SoundEffect effect : SoundEffect.values()) {
            if (effect.isLoaded() && effect.getClip() != null && effect.getClip().isRunning()) {
                effect.getClip().stop();
            }
        }
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        } else {
            // Reload sounds if they weren't loaded due to being disabled initially
            for (SoundEffect effect : SoundEffect.values()) {
                if (!effect.isLoaded()) {
                    loadSound(effect);
                }
            }
        }
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
} 