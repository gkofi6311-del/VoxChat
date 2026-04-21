import com.francis.voxchat.keybind.KeybindHandler;

public class VoxChatClient {

    public void onInitializeClient() {

        KeybindHandler.register();

        // later in tick:
        if (KeybindHandler.VOICE_RECORD == null) return;

        if (KeybindHandler.VOICE_RECORD.wasPressed()) {
            // start voice recording
        }
    }
}
