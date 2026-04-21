package com.francis.voxchat.audio;

import com.francis.voxchat.VoxChatMod;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

/**
 * Captures mono 16 kHz 16-bit PCM audio from the default microphone.
 * Vosk requires exactly this format.
 */
public class VoiceRecorder {

    /** Vosk requires: 16 kHz, 16-bit, mono, signed, little-endian */
    public static final AudioFormat FORMAT =
            new AudioFormat(16_000f, 16, 1, true, false);

    private TargetDataLine line;
    private ByteArrayOutputStream buffer;
    private Thread captureThread;
    private volatile boolean active;

    /**
     * Opens the microphone and starts capturing audio in a background thread.
     *
     * @return true if recording started successfully
     */
    public boolean start() {
        if (active) {
            VoxChatMod.LOGGER.warn("[VoxChat] Already recording – ignoring start()");
            return false;
        }

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            VoxChatMod.LOGGER.error("[VoxChat] Microphone format not supported: {}", FORMAT);
            return false;
        }

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);
            line.start();
            buffer = new ByteArrayOutputStream();
            active = true;

            captureThread = new Thread(() -> {
                byte[] chunk = new byte[4096];
                while (active) {
                    int read = line.read(chunk, 0, chunk.length);
                    if (read > 0) {
                        buffer.write(chunk, 0, read);
                    }
                }
            }, "voxchat-recorder");
            captureThread.setDaemon(true);
            captureThread.start();

            VoxChatMod.LOGGER.info("[VoxChat] Recording started");
            return true;

        } catch (LineUnavailableException e) {
            VoxChatMod.LOGGER.error("[VoxChat] Microphone unavailable", e);
            return false;
        }
    }

    /**
     * Stops the capture thread and returns all recorded PCM bytes.
     *
     * @return raw PCM bytes (16 kHz, 16-bit, mono) or empty array on error
     */
    public byte[] stop() {
        if (!active) {
            VoxChatMod.LOGGER.warn("[VoxChat] Not recording – ignoring stop()");
            return new byte[0];
        }

        active = false;

        try {
            if (captureThread != null) captureThread.join(2000);
        } catch (InterruptedException ignored) {}

        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }

        byte[] data = buffer != null ? buffer.toByteArray() : new byte[0];
        VoxChatMod.LOGGER.info("[VoxChat] Recording stopped – captured {} bytes ({} ms)",
                data.length, data.length / 32);     // 16 kHz × 16-bit = 32 bytes/ms
        return data;
    }

    public boolean isActive() {
        return active;
    }
}
