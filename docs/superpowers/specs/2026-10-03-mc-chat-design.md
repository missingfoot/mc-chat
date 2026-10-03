# MC Chat — Design Spec

Date: 2026-10-03
Status: Approved in conversation, pending written-spec review

## Purpose

A small Fabric mod that gives two players (host + one friend) a private, nicer-looking
replacement for vanilla chat. Vanilla player chat is blocked for age-unverified Microsoft
accounts; mod-to-mod networking on a custom channel is not affected. The mod also fixes
what the user dislikes about vanilla chat: too big/ugly, messages vanish too fast,
opening it is clunky, and it's hard to see who said what and when.

## Constraints

- Minecraft **26.3** (Fabric), Java 25 target (what MC 26.x requires — confirm during planning; dev machine has JDK 27).
- Mark compatible with 26.2+ only if it actually builds/runs there; 26.3 is the baseline.
- Fabric Loader + Fabric API. Official Mojang names (26.x is unobfuscated).
- Usage: user hosts an integrated LAN world; friend joins over Tailscale. Same jar on
  both clients; the host's integrated server runs the server side. Must also work on a
  dedicated server, but that is not a tested target.
- Both players must have the mod installed.

## Success criteria

1. Friend presses T, types, presses Enter; host sees it in the corner panel within a
   moment, and vice versa.
2. Messages remain visible (dimmed) rather than disappearing.
3. After both players quit and rejoin, scrollback shows earlier messages.
4. Up-arrow recalls previously sent messages, including across game restarts.
5. Vanilla `/` still opens vanilla chat for commands.

## Architecture

One mod jar, three source areas:

### Common (`main` source set)

- `ChatMessage` record: `UUID senderId`, `String senderName`, `String text`,
  `long timestampMillis`.
- Payloads (Fabric `CustomPacketPayload` + `StreamCodec`), channel namespace `mcchat`:
  - `SendPayload(String text)` — client → server.
  - `DeliverPayload(ChatMessage message)` — server → client.
  - `HistoryPayload(List<ChatMessage> messages)` — server → client on join.
- `TextSanitizer`: strips control characters and `§` formatting codes, trims
  whitespace, truncates to 256 chars; returns empty for blank input.
- Payload types registered with `PayloadTypeRegistry` in the common initializer.

### Server (common initializer, runs on integrated or dedicated server)

- `ServerPlayNetworking` receiver for `SendPayload`:
  sanitize → drop if empty → build `ChatMessage` with sender's UUID, name and server
  time → append to `ChatHistoryStore` → send `DeliverPayload` to every player that can
  receive the channel.
- On player join (`ServerPlayConnectionEvents.JOIN`): send `HistoryPayload` with the last
  100 messages, if the player can receive the channel.
- `ChatHistoryStore`:
  - File: `<world save>/mcchat/history.jsonl`, one JSON object per line.
  - Loaded on server start, kept in memory (capped at 1000), appended per message.
  - When the file exceeds 1000 lines, it is rewritten with the newest 1000 on server stop.
  - Corrupt lines are skipped with a log warning; IO errors are logged, never crash.

### Client (`client` source set)

- **`ClientChatState`**: in-memory list of received messages (the history payload
  replaces it; deliveries append), each tagged with its local receive time for
  fade/brightness, plus an `unread` flag. Cleared on disconnect.
- **`ChatHudPanel`** (drawn from a client mixin at the end of `Hud.extractRenderState`, so it
  can still draw the unread dot while F1 hides the HUD), bottom-left above the hotbar:
  - Text scale 0.75, width ~200px (scaled px), no heavy box: a translucent card
    background (~25% black) with a 1px coloured accent strip on the left edge.
  - Line format: `HH:mm` (dim grey) + name (player colour) + message; long messages
    wrap within the panel width.
  - Grouping: consecutive messages from the same sender within 2 minutes omit the
    name and timestamp after the first.
  - Shows the last 5 messages at all times. Messages younger than 30s render at full
    opacity; older ones render dimmed (~45% opacity). They never disappear.
  - Hidden when the HUD is hidden (F1) or while the chat screen is open (the screen
    draws its own expanded version). When hidden by F1 and an unread message from
    the other player exists, draw a small dot in the corner instead.
- **`CompactChatScreen`** (opened by T):
  - Non-pausing, no background blur or dim.
  - Draws the same panel style, expanded upward to ~15 lines, with mouse-wheel scroll
    through the full history the client holds.
  - Single-line input box inside the panel, panel width, 256 char limit.
  - Enter sends `SendPayload` (if non-blank) and closes; Esc closes; Up/Down walk the
    sent-message history.
  - If the server can't receive the channel, show an inline red line
    "MC Chat isn't installed on the server" instead of sending.
  - Opening the screen clears the unread flag.
- **Keybind**: the mod takes over vanilla's "Open Chat" key (T by default, rebindable in
  Controls). A client mixin on `Gui.openChatScreen(ChatMethod)` opens `CompactChatScreen`
  instead of vanilla's chat screen when the method is `MESSAGE`. `COMMAND` (the `/` key)
  is untouched, so vanilla command chat still works.
- **Notification**: when a delivery arrives from someone other than the local player,
  play a soft chime (`SoundEvents.NOTE_BLOCK_BELL`, high pitch, low volume) locally and
  set `unread`.
- **`SentHistory`**: last 50 sent messages stored in `config/mcchat-sent.txt`, loaded on
  client start, saved after each send.
- **`McChatConfig`**: `config/mcchat.json`, created with defaults if missing; invalid
  values fall back to defaults with a log warning:
  - `scale` (0.75), `width` (200), `visibleLines` (5), `expandedLines` (15),
    `brightSeconds` (30), `dimOpacity` (0.45), `soundEnabled` (true),
    `soundVolume` (0.4), `nameColors` (map of player name → hex colour; players not
    listed get a stable colour from a fixed palette hashed by UUID).
  - Corner position is not configurable in v1 (bottom-left only).

## Error handling summary

| Situation | Behaviour |
|---|---|
| Blank / whitespace message | Ignored client-side and server-side |
| Message > 256 chars | Truncated by input limit and by server sanitizer |
| Server lacks mod | Inline error in chat screen; nothing sent |
| Other client lacks mod | Server skips sending to them |
| History file corrupt line | Line skipped, warning logged |
| History/config IO failure | Logged; mod continues with in-memory state/defaults |

## Out of scope (v1)

Channels/DMs, emoji, clickable links, formatting codes, configurable corner, in-game
config screen, chat filtering/moderation, relaying to/from vanilla chat.

## Testing

- JUnit tests (no Minecraft runtime) for `TextSanitizer`, `ChatHistoryStore`
  (append, cap, reload, corrupt lines), `SentHistory`, and message grouping logic.
- Manual test: `runClient` hosting an Open-to-LAN world, plus a second offline dev
  client joining it; verify the success criteria above.
