# Texture Sources

The current atlas is generated from project-local procedural 16x16 pixel art in `scripts/generate_block_textures.ps1`.

No third-party image files were downloaded or committed for this pass. This keeps the atlas deterministic and avoids mixing incompatible palettes. The importer supports vetted replacements by placing same-named PNGs in `texture_sources/import`; the generator normalizes them to 16x16 with nearest-neighbor scaling before copying them into `texture_sources/blocks`.

Compatible CC0 sources reviewed for future imports:

- OpenGameArt, "16*16 Block Textures" by ARoachIFoundOnMyPillow, CC0: https://opengameart.org/content/1616-block-textures
- OpenGameArt, "16x16 Block Texture Set" by ARoachIFoundOnMyPillow, CC0: https://opengameart.org/content/16x16-block-texture-set
- OpenGameArt, "Food Items 16x16" by Red_Voxel, CC0: https://opengameart.org/content/food-items-16x16
- OpenGameArt, "16x16 RPG items", CC0: https://opengameart.org/content/16x16-rpg-items
- OpenGameArt, "Minery Icons!", CC0: https://opengameart.org/content/minery-icons
- itch.io, "Blocks, Lines, and Shapes - 16x16 Tiles (CC0, Free)" by VEXED, CC0: https://v3x3d.itch.io/blocks-lines-shapes
- itch.io, "Block Land - 16x16 Mario/Minecraft-like Platformer Assets Tileset (CC0, Free)" by VEXED, CC0: https://v3x3d.itch.io/block-land
- itch.io, "Four Leaf Asset Pack 16x16" by Crumpaloo, CC0: https://crumpaloo.itch.io/four-leaf-asset-pack16x16

Import rule: only place files in `texture_sources/import` when the license permits reuse, modification, and redistribution in this project.
