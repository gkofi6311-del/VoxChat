package com.francis.voxchat.mixin;

import com.francis.voxchat.VoxChatState;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects pending VoxChat text into the chat input field whenever ChatScreen is
 * initialised (opened).  Does NOT send the message — the player confirms manually.
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method = "init", at = @At("TAIL"))
    private void voxchat$onChatOpen(CallbackInfo ci) {
        String pending = VoxChatState.getPendingText();
        if (pending != null && !pending.isEmpty()) {
            // Append to whatever the player had already typed (usually empty)
            String existing = chatField.getText();
            String newText  = existing.isEmpty() ? pending : existing + " " + pending;
            chatField.setText(newText);
            chatField.setCursorToEnd(false);
            VoxChatState.clearPendingText();
        }
    }
}
