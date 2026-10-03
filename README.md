# MC Chat

A small Fabric mod for Minecraft 26.3 that gives you a private chat that works over the mod's
own network channel, so it isn't affected by Microsoft's chat restrictions. It's also nicer than vanilla
chat: compact, bottom-left, chat heads instead of names, messages fade to faint text instead of vanishing, and
history that survives restarts.

## Install

Both players need: Fabric Loader 0.19.5+, Fabric API for 26.3, and `mcchat-1.0.0.jar` in `mods/`.
The host's world (LAN or dedicated server) needs the mod too — if you host from your own game,
that's already covered.

## Use

- **T** — open chat (rebind via Controls → "Open Chat"). **/** still opens vanilla command chat.
- **Enter** send · **Esc** close · **Up/Down** previous messages · **Mouse wheel / PgUp / PgDn** scroll.
- A soft chime plays when the other person messages. With the HUD hidden (F1), a small blue dot
  in the bottom-left corner means there's an unread message.

## Settings — `config/mcchat.json`

Created on first launch. Restart the game after editing.

| Key | Default | Meaning |
|---|---|---|
| `scale` | 0.75 | Text size (0.25–2.0) |
| `width` | 200 | Panel width in GUI pixels (80–600) |
| `visibleLines` | 5 | Lines always shown in the corner (0 hides the panel) |
| `expandedLines` | 15 | Lines shown while typing |
| `brightSeconds` | 30 | How long new messages stay bright (with the background box) before fading |
| `dimOpacity` | 0.45 | Opacity of older lines (0.1–1.0) |
| `soundEnabled` | true | Chime when the other person messages |
| `soundVolume` | 0.4 | Chime volume (0–1) |

History lives in `<world>/mcchat/history.jsonl` on the host (last 1000 messages; the last 100 are
sent to each player when they join).

## Build

`./gradlew build` → `build/libs/mcchat-1.0.0.jar`. Gradle runs on JDK 25 (picked automatically
via `gradle/gradle-daemon-jvm.properties`).

- Unit tests: `./gradlew test`
- In-game automated test (opens a real client, drives the chat, saves screenshots to
  `build/run/clientGameTest/screenshots/`): `./gradlew runClientGameTest`
- Two players on one machine: `./gradlew runClient` (Host) and `./gradlew runClient2` (Friend),
  then Open to LAN on Host and Direct Connect to `localhost:<port>` on Friend.
