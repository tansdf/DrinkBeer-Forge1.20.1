# DrinkBeer 1.20.1 Forge Port Notes

Date: 2026-06-15

## Goal

Port DrinkBeer from its 1.17.1 Forge-era codebase to Minecraft 1.20.1 Forge while preserving active content:

- Beer barrel
- Beer mugs
- Call bells
- Recipe boards/package
- Brewing recipes
- Sounds
- Beer barrel GUI
- Drunk Frost Walker effect

Do not revive unfinished frothy pink eggnog content.

## Target

- Minecraft: 1.20.1
- Forge: 47.4.10
- ForgeGradle: 6
- Gradle wrapper: 8.1.1
- Java: 17
- Resource pack format: 15

## Main Port Changes

- Replaced ForgeGradle 5 / Forge 37 / Minecraft 1.17.1 build setup with ForgeGradle 6 and Forge 1.20.1-47.4.10.
- Added `settings.gradle` plugin management for Forge Maven.
- Updated `mods.toml`:
  - loader range `[47,)`
  - Minecraft range `[1.20.1,1.21)`
  - dependency blocks changed from `examplemod` to `drinkbeer`
- Updated `pack.mcmeta` to `pack_format: 15`.
- Local verification used the CurseForge Java 17 runtime because system `JAVA_HOME` points to a JRE. Do not commit a machine-specific `org.gradle.java.home`; use a real JDK 17 or local Gradle/user environment configuration.
- Migrated old Forge registry imports from `net.minecraftforge.fmllegacy.RegistryObject` to current `net.minecraftforge.registries.RegistryObject`.
- Updated registry constants:
  - `ForgeRegistries.BLOCK_ENTITY_TYPES`
  - `ForgeRegistries.MENU_TYPES`
  - `ForgeRegistries.RECIPE_SERIALIZERS`
  - `ForgeRegistries.RECIPE_TYPES`
  - `ForgeRegistries.MOB_EFFECTS`
  - `ForgeRegistries.SOUND_EVENTS`
- Renamed `BLOKC_ENTITIES` to `BLOCK_ENTITIES`.
- Replaced old creative tab usage with registered 1.20.1 creative tabs.
- Removed `.tab(...)` from `Item.Properties`.
- Replaced removed `Material.*` block properties with vanilla block property copies.
- Replaced `TranslatableComponent` with `Component.translatable`.
- Updated sound registration to `SoundEvent.createVariableRangeEvent(...)`.
- Updated barrel GUI screen to the 1.20 `GuiGraphics` rendering API.
- Moved client-only screen registration into `client/ClientEventHandler.java`.
- Updated Forge network GUI opening to `NetworkHooks.openScreen(...)`.
- Updated block entity save/sync APIs:
  - `saveAdditional(...)`
  - `ClientboundBlockEntityDataPacket.create(this)`
- Fixed barrel `ContainerData#getCount()` from `1` to `2` so both timer and status sync.
- Updated recipe API signatures:
  - `assemble(..., RegistryAccess)`
  - `getResultItem(RegistryAccess)`

## Server Crash Investigation

Initial remote CABIN server symptom:

- Server appeared to hang after Forge version-check logs.
- Disabling Forge version checking revealed the real datapack reload error.

Actual crash:

```text
Failed to load datapacks
Caused by: java.lang.ExceptionInInitializerError
Caused by: java.lang.IllegalStateException: Can not register to a locked registry. Modder should use Forge Register methods.
at lekavar.lma.drinkbeer.registries.RecipeRegistry$Type.<clinit>(RecipeRegistry.java:12)
```

Root cause:

`RecipeType.register("drinkbeer:brewing")` was inside a static nested class and was initialized lazily during recipe/datapack reload. At that point Forge had already locked the registry.

Fix:

- Removed the static `RecipeRegistry.Type.BREWING = RecipeType.register(...)` pattern.
- Added deferred recipe type registration:

```java
public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "drinkbeer");

public static final RegistryObject<RecipeType<BrewingRecipe>> BREWING_TYPE =
        RECIPE_TYPES.register("brewing", () -> new RecipeType<>() {
            @Override
            public String toString() {
                return "drinkbeer:brewing";
            }
        });
```

- Registered `RecipeRegistry.RECIPE_TYPES` on the mod event bus in `DrinkBeer`.
- Updated recipe lookup and `BrewingRecipe#getType()` to use `RecipeRegistry.BREWING_TYPE.get()`.

## Verification

Build:

```powershell
.\gradlew.bat clean build
```

Result:

```text
BUILD SUCCESSFUL
```

Server datapack/runtime verification:

- Temporarily set local dev `run/eula.txt` to `eula=true`.
- Ran:

```powershell
.\gradlew.bat runServer
```

Important successful log lines:

```text
Loaded 8 recipes
Done (20.342s)! For help, type "help"
```

No local occurrences of:

- `Failed to load datapacks`
- `ExceptionInInitializerError`
- `locked registry`

## Final Jar

Use this rebuilt jar:

```text
C:\Users\tansdf-Legion\Projects\DrinkBeer\build\libs\drinkbeer-1.20.1-2.3.5.jar
```

## Remaining Notes

- `FMLJavaModLoadingContext.get()` still emits one deprecation warning during build, but it does not block compile, reobf, datapack loading, or server startup.
- Remote server uses Forge 47.4.20 in the CABIN log, while the mod builds against 47.4.10. The mod metadata allows `[47,)`, and local verification used Forge 47.4.10.
- The FancyMenu local world-creation issue is likely unrelated to DrinkBeer. DrinkBeer only registers its beer barrel screen client-side.
