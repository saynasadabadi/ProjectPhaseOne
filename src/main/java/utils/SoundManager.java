package utils;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

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
    private static int masterVolumePercent = 80; // Default volume 80%
    // Define a reasonable dB range for volume control
    private static final float MIN_DB = -40.0f; // Or lower for complete silence, e.g., -80.0f for FloatControl. minimo
    private static final float MAX_DB = 6.0206f;  // Maximum gain (can be clip-dependent, +6dB is common max)

    static {
        // Pre-load all sound effects
        for (SoundEffect effect : SoundEffect.values()) {
            loadSound(effect);
        }
        // Apply initial volume after loading
        setVolume(masterVolumePercent); 
    }

    private static void loadSound(SoundEffect effect) {
        if (!soundEnabled && effect != SoundEffect.BACKGROUND_MUSIC) { 
             // Always try to load background music clip to get its controls later, even if sound is off initially
             // but don't load others if sound is off.
        }
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
            // Apply current volume to newly loaded clip
            applyVolumeToClip(clip, masterVolumePercent);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading sound " + effect.getFilePath() + ": " + e.getMessage());
            effect.setClip(null);
        }
    }
    
    private static void applyVolumeToClip(Clip clip, int volumePercent) {
        if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float range = MAX_DB - MIN_DB;
            float gain = MIN_DB + (range * (volumePercent / 100.0f));
            gain = Math.min(Math.max(gain, gainControl.getMinimum()), gainControl.getMaximum()); // Clamp to clip's actual min/max
            gainControl.setValue(gain);
        }
    }

    public static void setVolume(int volumePercent) {
        masterVolumePercent = Math.max(0, Math.min(100, volumePercent));
        System.out.println("Setting master volume to: " + masterVolumePercent + "%");
        for (SoundEffect effect : SoundEffect.values()) {
            if (effect.isLoaded() && effect.getClip() != null) {
                applyVolumeToClip(effect.getClip(), masterVolumePercent);
            }
        }
    }

    public static int getVolume() {
        return masterVolumePercent;
    }

    public static void playSound(SoundEffect effect) {
        if (!soundEnabled || !effect.isLoaded() || effect.getClip() == null) return;
        Clip clip = effect.getClip();
        applyVolumeToClip(clip, masterVolumePercent); // Ensure volume is set before playing
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public static void loopSound(SoundEffect effect) {
        if (!soundEnabled || !effect.isLoaded() || effect.getClip() == null) return;
        Clip clip = effect.getClip();
        applyVolumeToClip(clip, masterVolumePercent); // Ensure volume is set before looping
        if (!clip.isRunning()) {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void stopSound(SoundEffect effect) {
        if (!effect.isLoaded() || effect.getClip() == null) return; // Allow stopping even if soundEnabled is false
        Clip clip = effect.getClip();
        if (clip.isRunning()) clip.stop();
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
        boolean oldState = soundEnabled;
        soundEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        } else if (!oldState && enabled) { // If re-enabling sound
            for (SoundEffect effect : SoundEffect.values()) {
                if (!effect.isLoaded()) {
                    loadSound(effect); // Try to load sounds that weren't loaded
                } else {
                    // For already loaded sounds, ensure volume is applied
                    applyVolumeToClip(effect.getClip(), masterVolumePercent);
                }
            }
        }
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
} 