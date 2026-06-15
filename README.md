# DrinkBeer Forge 1.20.1

This is a Forge 1.20.1 port of DrinkBeer. It preserves the active DrinkBeer content from the older Forge codebase and updates the mod to current Forge/Minecraft APIs.

## Status

- Minecraft: `1.20.1`
- Forge: `47.4.10` build target, metadata allows Forge `47.x`
- Java: `17`
- ForgeGradle: `6`
- Gradle wrapper: `8.1.1`
- Resource pack format: `15`

The mod has been built successfully and verified to pass dedicated-server datapack loading. It was also tested on a CABIN server, where the server started and a client connected with the mod installed.

## Included Content

- Beer barrel block and GUI
- Placeable beer mugs
- Beer brewing recipes
- Recipe boards and recipe board package
- Iron and golden call bells
- Drink sounds and other active sound events
- Drunk Frost Walker effect

The unfinished frothy pink eggnog content remains inactive and is not registered.

## Build

Use a Java 17 JDK. A JRE is not enough because ForgeGradle needs `javac`.

```powershell
.\gradlew.bat clean build
```

The built jar is created under:

```text
build/libs/
```

## Run Checks

Dedicated server check:

```powershell
.\gradlew.bat runServer
```

For local server testing, Minecraft requires accepting the EULA in the generated `run/eula.txt`.

Successful datapack verification should include log lines like:

```text
Loaded 8 recipes
Done (...s)! For help, type "help"
```

## Important Port Fix

The custom brewing recipe type is registered through Forge's deferred registry system. This avoids the datapack-load crash:

```text
Can not register to a locked registry
```

That crash happened when `RecipeType.register("drinkbeer:brewing")` was initialized during recipe reload after registries were locked.

## Notes

Detailed migration notes are in [PORTING_NOTES.md](PORTING_NOTES.md).

Useful upstream links:

- [Original Forge 1.16.5 repository](https://github.com/Lekavar/DrinkBeer-Forge1.16.5-)
- [Forge 1.18.1 repository](https://github.com/Naetheline/DrinkBeer-Forge1.18.1)
- [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/drink-beer-forge)
