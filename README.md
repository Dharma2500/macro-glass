# Macro Glass

Client-side Fabric mod for Minecraft 1.21.11.

## Features

- Centered dark-glass window, roughly 50% of the screen width and height.
- Seven independent macro tabs: 1 through 7.
- Multiline editor with selection, mouse drag, scrolling, wrapping and clipboard shortcuts.
- Ctrl+A / Ctrl+C / Ctrl+X / Ctrl+V.
- Arrow navigation, Shift+arrows, Home/End, Ctrl+Left/Right, Backspace/Delete, Enter and Tab.
- Repeat count and delay in milliseconds between cycles.
- Run / Stop controls.
- Config is saved automatically to `.minecraft/config/macro_glass.json`.
- Default opening key: Right Control.

## Macro format

Each non-empty line is treated as one action.

- `/spawn` sends the command `spawn` to the server.
- `hello` sends `hello` as a chat message.
- Lines are executed in order, one action per client tick.
- The configured delay is applied between complete cycles.

Use the mod only where the server rules permit automation.
