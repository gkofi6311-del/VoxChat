# 🎙️ VoxChat — Minecraft 1.21.1 Fabric Mod

Press **V** to record your voice. VoxChat transcribes it with **Vosk** (100 % offline, no cloud)
and places the text in your chat input so you can review and send it yourself.

| Feature | Details |
|---|---|
| Minecraft | 1.21.1 (Fabric) |
| Java | 21 |
| Languages | 🇬🇧 English · 🇩🇪 German |
| STT Engine | [Vosk](https://alphacephei.com/vosk/) (offline) |
| Default keybind | **V** (rebindable in Controls) |

---

## 📦 Building

### Option A — GitHub Actions (recommended, no local setup needed)

1. Push the repo to GitHub.
2. The workflow at `.github/workflows/build.yml` runs automatically.
3. Download the JAR from **Actions → your run → Artifacts → voxchat-mod-jar**.

### Option B — Local build

Requires **Java 21** and **Gradle 8.8** installed.

```bash
# First time only – generates the Gradle wrapper
gradle wrapper --gradle-version=8.8

# Every build
./gradlew build
```

The compiled JAR is at `build/libs/voxchat-1.0.0.jar`.

---

## 🗣️ Setting up Vosk models (required for speech recognition)

The mod JAR does **not** include language models — they are too large (~50–500 MB each).
You must download them separately.

### Step 1 — Download models

| Language | Recommended model | Size |
|---|---|---|
| English | [vosk-model-small-en-us-0.15](https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip) | ~40 MB |
| German | [vosk-model-small-de-0.15](https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip) | ~45 MB |

For better accuracy use the full models (300–500 MB) — same URL pattern without `small-`.

### Step 2 — Place models in your `.minecraft` folder

```
.minecraft/
  voxchat-models/
    en/          ← contents of vosk-model-small-en-us-0.15.zip
    de/          ← contents of vosk-model-small-de-0.15.zip
```

> **Important:** unzip the archive and copy its *contents* (the files, not the folder) into `en/` or `de/`.
> The folder must contain `am/`, `conf/`, `graph/`, `ivector/`, `README` etc.

### Step 3 — Start Minecraft

The mod will log model loading status in the console:
```
[VoxChat] ✅ Vosk ready. EN=true DE=true
```

---

## 🎮 Usage

| Action | Result |
|---|---|
| Press **V** | Start recording (🔴 REC badge appears) |
| Press **V** again | Stop recording, recognize speech |
| Recognized text | Opens chat with text pre-filled (you press Enter to send) |

### Changing the language

Open `VoxChatClient.java` and change:
```java
private static final VoskSpeechService.Language DEFAULT_LANGUAGE =
        VoskSpeechService.Language.ENGLISH;   // ← change to GERMAN
```

(A future update will add an in-game config screen.)

---

## 🔧 Troubleshooting

| Problem | Fix |
|---|---|
| "Vosk models not loaded" | Check your `voxchat-models/` folder structure (see above) |
| "Could not open microphone" | Grant Java/Minecraft microphone permission in OS settings |
| "Could not understand speech" | Speak more clearly; try the larger (non-small) model |
| GitHub Actions: "no Gradle build" | Should be fixed — the workflow generates the wrapper automatically |

---

## 📁 Project Structure

```
voxchat/                          ← repository root (NO nesting!)
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/gradle-wrapper.properties
├── .github/workflows/build.yml
└── src/
    ├── main/
    │   ├── java/com/francis/voxchat/
    │   │   ├── VoxChatMod.java
    │   │   └── VoxChatState.java
    │   └── resources/
    │       ├── fabric.mod.json
    │       └── assets/voxchat/lang/
    └── client/
        ├── java/com/francis/voxchat/
        │   ├── VoxChatClient.java     ← main client logic + keybind polling
        │   ├── KeyBindings.java       ← V keybind
        │   ├── audio/VoiceRecorder.java
        │   ├── stt/VoskSpeechService.java
        │   ├── hud/RecordingOverlay.java
        │   └── mixin/ChatScreenMixin.java
        └── resources/
            └── voxchat.client.mixins.json
```

---

## 📜 License

MIT — do whatever you want with it.
