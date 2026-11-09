# GemstoneGuardian — notes for Claude

Read the "For developers and AI agents" section of `README.md` first; it explains how the game is built and run, how a frame is drawn, the tile/character/item/GUI systems, the sprite sheet and the working agreements. The short version:

- Build with `javac` (no Maven here) into `target/classes`, copy `src/main/resources` on top, run from the project root. Close a running game before rebuilding.
- Tile ids are list positions in `tileTypes.java`: only append, never reorder.
- The owner draws the textures in `media/tiles.png`; draft and label new cells, never redraw finished ones (the rock at (1000, 1500) is finished).
- Design general systems rather than single features, and ask design questions before starting a big one.
- Do not screenshot to verify visuals — start the game and ask the owner to look.
