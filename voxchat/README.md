# VoxChat 🎤

**Voice-to-text chat mod for Minecraft Fabric 1.21.1**

Press **V**, speak, see your words appear in the chat box — then press **Enter** to send.
No auto-sending. No cloud APIs. 100% offline with [Vosk](https://alphacephei.com/vosk/).

---

## ✅ Features

| Feature | Details |
|---|---|
| Voice input | Hold **V** (push-to-talk) or press to toggle |
| Speech engine | Vosk — fully offline, no internet required at runtime |
| Languages | English 🇬🇧 and German 🇩🇪 (switchable with **L** key) |
| Chat injection | Text appears in chat input; you press Enter to send |
| Config | `config/voxchat.properties` — survives updates |
| Keybinds | Fully remappable in *Options → Controls → VoxChat* |
| Client-side | No server mod required |

---

## 📦 Installation

### 1 — Prerequisites

- Minecraft **Java Edition 1.21.1**
- [Fabric Loader](https://fabricmc.net/use/) ≥ 0.15
- [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1

### 2 — Install the mod

Drop `voxchat-<version>.jar` into your `.minecraft/mods/` folder.

### 3 — Install a Vosk model ⚠️ **Required**

VoxChat ships without model files (they are 40–150 MB each).
You must download one manually:

#### English (default)

```
URL   : https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
Unzip → rename folder to:  en
Place at: .minecraft/voxchat-models/en/
```

#### German

```
URL   : https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip
Unzip → rename folder to:  de
Place at: .minecraft/voxchat-models/de/
```

**Final structure:**

```
.minecraft/
└── voxchat-models/
    ├── en/          ← English model files here
    │   ├── am/
    │   ├── conf/
    │   └── ...
    └── de/          ← German model files here (optional)
        ├── am/
        ├── conf/
        └── ...
```

---

## 🎮 Usage

| Action | Key |
|---|---|
| Start/stop voice input | **V** (hold = push-to-talk, press = toggle) |
| Switch language | **L** |
| Send message | **Enter** (after text appears in chat) |

All keybinds are remappable in **Options → Controls → VoxChat**.

---

## ⚙️ Configuration

Config file: `.minecraft/config/voxchat.properties`

```properties
# Language code: en or de
language=en

# true  = hold key to record (push-to-talk)
# false = press once to start, press again to stop (toggle)
push_to_talk=true
```

---

## 🏗️ Building from source

Requirements: **Java 21**, internet connection (Gradle downloads dependencies).

```bash
git clone https://github.com/francis/voxchat.git
cd voxchat
chmod +x gradlew
./gradlew build
```

The output JAR is at `build/libs/voxchat-<version>.jar`.

---

## 🧩 How it works

```
[Player presses V]
       │
       ▼
KeybindHandler.tick()
       │
       ▼
SpeechManager.startListening()      ← background thread starts
       │  (TargetDataLine captures 16kHz PCM from microphone)
       │  (Vosk Recognizer processes chunks in real time)
       │
[Player releases V]
       │
       ▼
SpeechManager.stopListening(callback)
       │  (Vosk flushes final result)
       │
       ▼
client.execute(() → ChatInjector.injectText())   ← back on MC thread
       │
       ▼
[Chat screen opens / text appended to existing chat]
       │
[Player presses Enter to send]
```

---

## 🔧 Architecture

| Class | Purpose |
|---|---|
| `VoxChatMod` | Fabric entry point — wires all modules |
| `VoxChatConfig` | Persistent config (language, mode) |
| `KeybindHandler` | Registers keybinds, handles tick events |
| `SpeechManager` | Vosk model loading + microphone + recognition loop |
| `LanguageManager` | Model path resolution + install help logging |
| `ChatInjector` | Thread-safe chat screen text injection |

---

## ❓ FAQ

**Q: Nothing happens when I press V.**
A: Check the log (`latest.log`) for `[VoxChat] Missing Vosk model` — you need to install a model first.

**Q: My microphone isn't detected.**
A: Ensure Java has microphone access (macOS: System Settings → Privacy → Microphone → check your Java runtime).

**Q: Can I use larger / more accurate models?**
A: Yes — download any Vosk model from https://alphacephei.com/vosk/models, rename its directory to `en` or `de`, and place it in `voxchat-models/`.

**Q: Will this work on servers?**
A: Yes — it is 100% client-side. No server mod needed.

---

## 📄 License

MIT License — see [LICENSE](LICENSE).
