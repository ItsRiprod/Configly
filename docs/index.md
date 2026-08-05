---
title: "Introduction"
order: 1
published: true
draft: false
---
# Configly - Mod-Wide Configuration

**One config folder, any shape you want, patchable by anyone**

Configly gives every mod on the server a shared `Server/Configs/` folder. Each mod declares a config type and supplies its own codec, so the file can be any shape it likes, while still being a first-class Hytale asset: typed in the asset editor, hot-reloadable, and patchable by other packs.

## TL;DR

Your mod defines a config class, registers it, and ships a default JSON file:

```java
Configly.register("Hexcode", HexcodeConfig.class, HexcodeConfig.CODEC);
```

`Server/Configs/Hexcode.json`
```json
{
  "Type": "Hexcode",
  "RespectPVP": true
}
```

```java
HexcodeConfig.get().respectsPvp();
```

Server owners edit the file, or the asset editor. Other packs override values with a `.patch`.

## Why not just register your own AssetStore

You can, and for a large asset family you should. For a config file it costs more than it gives:

- An `AssetStore` needs its own folder, so a single config file becomes `Server/YourMod/Config/YourMod.json` - a directory holding one file.
- Nothing else can reach it. Another pack cannot override a value without shipping a whole replacement file.
- You write the asset class, the codec plumbing, the key function, and the store registration, all for one file.

Configly does the store once for every mod. You write the codec for your own fields and nothing else.

## Setting up the project

Configly ships as a normal jar. Drop it in `mods/` and it works standalone, but a mod should **shade** it so users never have to install anything extra. Multiple copies coordinate at runtime: exactly one registers the store and every other copy binds to it, so it does not matter how many mods bundle their own.

Put `Configly-X.Y.Z.jar` in a `lib/` folder, then wire up the Shadow plugin:

```groovy
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '9.4.2'
}

configurations {
    shadowBundle
    implementation.extendsFrom(shadowBundle)
}

dependencies {
    shadowBundle(files('lib/Configly-1.0.0.jar'))
}

shadowJar {
    archiveClassifier.set('')
    mergeServiceFiles()
    configurations = [project.configurations.shadowBundle]
    relocate('com.riprod.configly', 'com.riprod.yourmod.shaded.configly')
}

tasks.build {
    dependsOn("shadowJar")
}
```

The `relocate` line is required. Without it your copy collides with every other mod's copy in the same package. The dedicated `shadowBundle` configuration keeps Shadow from folding in anything else.

## Defining a config

Extend `Config` and give it a `BuilderCodec`, the same way you would write any Hytale asset codec. The static `get()` is the conventional accessor, mirroring `GlyphAsset.getAssetMap()`:

```java
public final class HexcodeConfig extends Config {

    public static final String TYPE = "Hexcode";

    public static final HexcodeConfig DEFAULTS = new HexcodeConfig();

    public static final BuilderCodec<HexcodeConfig> CODEC = BuilderCodec.builder(HexcodeConfig.class, HexcodeConfig::new)
        .append(new KeyedCodec<>("RespectPVP", Codec.BOOLEAN),
            (config, b) -> config.respectPvp = b,
            config -> config.respectPvp)
        .documentation("Whether Hexcode spells honour the server PVP setting")
        .add()
        .build();

    private boolean respectPvp = true;

    private HexcodeConfig() {
    }

    public static HexcodeConfig get() {
        return Configly.getOrElse(TYPE, HexcodeConfig.class, DEFAULTS);
    }

    public boolean respectsPvp() {
        return respectPvp;
    }
}
```

Field defaults on the class are the fallback for anything the file omits, and `DEFAULTS` covers the file being missing entirely. Every `.documentation(...)` string shows up in the asset editor.

## Registering

Once, in your plugin's `setup()`:

```java
@Override
protected void setup() {
    Configly.register(HexcodeConfig.TYPE, HexcodeConfig.class, HexcodeConfig.CODEC);
}
```

This has to happen in `setup()`. Assets load after every plugin has been set up, so a registration made later misses the boot scan and your config will not load.

If your plugin has no config of its own but you still want the store present, call `Configly.install()` instead.

## Shipping the default

`src/main/resources/Server/Configs/Hexcode.json`

```json
{
  "Type": "Hexcode",
  "RespectPVP": true
}
```

Fields are flat, alongside `Type`. There is no nesting and no wrapper object.

## Reading

```java
HexcodeConfig config = Configly.get("Hexcode", HexcodeConfig.class);              // null if absent
HexcodeConfig config = Configly.getOrElse("Hexcode", HexcodeConfig.class, DEFAULTS);
```

`get` returns `null` when no file with that name exists, and also when the file's `Type` belongs to a different mod, so you can never be handed someone else's config by accident.

Lookups are a map lookup plus a type check, cheap enough to call per operation. Do not cache the result in a field: configs reload from disk and from the asset editor, and a cached reference goes stale.

### Two `Type` values, one file

Configly keys assets by **filename** and dispatches codecs by the **`Type`** field.

- The filename (`Hexcode.json`) is the id you pass to `Configly.get`.
- `Type` selects which mod's codec decodes the file.

They are usually the same string, and there is no reason for them to differ unless you ship several files of one type. Subfolders are allowed for organization but ignored for keying, so `Configs/Icarus/Combat.json` and `Configs/Combat.json` are the same asset and will clobber each other.

## Overriding a config from another pack

Any pack can change any config without touching the owning mod, using a [Patchly](https://modtale.net/mod/patchly) `.patch` beside it:

`Server/Configs/Hexcode.patch`
```json
{
  "RespectPVP": false
}
```

Only the named fields change; everything else keeps the owning mod's values. `$Requires` and `$Priority` work here as they do for any other patch, so a compatibility pack can gate its override on the mod actually being installed:

```json
{
  "$Requires": "Riprod:Hexcode",
  "RespectPVP": false
}
```

## If the owning mod is uninstalled

A config whose `Type` no longer has a registered codec does not crash the server. It decodes to an opaque placeholder, logs a warning, and keeps its file byte-for-byte, so reinstalling the mod restores every hand-tuned value and every third-party patch.

## Troubleshooting

**The config never loads.** `Configly.register` has to run in `setup()`. Check the log for `Configly X.Y.Z registered the config store at Server/Configs` - it should appear exactly once, from whichever copy won.

**`get` returns null.** The filename and the id you pass must match, and the file's `Type` must be the one you registered. Run `/log Configly FINE` to see the per-type registration lines and any type mismatch.

**Fields are missing after an edit.** Unknown keys are ignored and logged rather than failing the load, so a typo in a field name silently keeps the default. The asset editor's typed fields avoid this.
