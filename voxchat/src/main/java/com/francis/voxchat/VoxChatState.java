package com.francis.voxchat;

/**
 * Shared, thread-safe state holder used by both the mixin and client logic.
 * Stores text recognized by Vosk until the ChatScreen can consume it.
 */
public final class VoxChatState {

    private VoxChatState() {}

    /** Text waiting to be injected into the ChatScreen input field. */
    private static volatile String pendingText = null;

    /** Whether voice recording is currently active. */
    private static volatile boolean recording = false;

    public static String getPendingText() {
        return pendingText;
    }

    public static void setPendingText(String text) {
        pendingText = text;
    }

    public static void clearPendingText() {
        pendingText = null;
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void setRecording(boolean value) {
        recording = value;
    }
}
