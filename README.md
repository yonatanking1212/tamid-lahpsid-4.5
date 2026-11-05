# Tamid Lahapsid - updated source

Minecraft Java Edition 1.21.4 / Fabric / Java 21.

## Build
Install JDK 21 and Gradle 8.12 or newer, then run from this folder:

    gradle build

The compiled mod will be created in:

    build/libs/tamidlahapsid-1.0.0.jar

Install Fabric API and this JAR into your Fabric 1.21.4 instance.

## Updates in this source
- Bot delay slider in milliseconds (50-2000 ms).
- Bot waits for the vanilla attack cooldown to reduce premature/ghost clicks.
- Optional Crit Timing mode: only attempts an attack while the player is falling in a vanilla-compatible critical-hit window.
- Bot hotbar slot selector (1-9).
- Aim hotbar slot selector (1-9).
- Aim FOV selector (10-180 degrees) and target selection inside that cone.
- Smooth Aim toggle; horizontal and vertical speed sliders remain available.
- Auto Firework toggle and bind in Misc; default bind is C. While enabled, it uses a firework rocket from the hotbar while elytra-gliding.
- Off on Death and Off on Teleport guards retained and improved.
- Click GUI can be dragged by its top title bar; its screen position is saved in the config.
- Existing dark-blue visual style retained with more room for the added controls.

## Notes
- The bot and aim slot values are hotbar slots numbered 1 through 9.
- Auto Firework needs at least one firework rocket in the hotbar and an equipped elytra while gliding.
- Crit Timing is optional and off by default. Server-side anticheat and server mechanics may still affect hit registration and critical hits.
