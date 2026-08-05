# Configly
[![CurseForge](https://img.shields.io/curseforge/dt/1448311?logo=curseforge&logoColor=white&label=CurseForge&color=F16436)](https://mod.hexcodec.com)[![Docs](https://img.shields.io/badge/Docs-Read-2088FF?logo=readthedocs&logoColor=white)](https://docs.hexcodec.com)[![GitHub](https://img.shields.io/badge/GitHub-Source-181717?logo=github&logoColor=white)](https://github.com/itsriprod/configly)

**Mod-wide configuration for Hytale mods. A library for mod developers.**

Configly is infrastructure for your mod, not something your users install. You shade it into your jar, define your config, and ship. Your users never learn the name Configly - they just get a clean, editable config file where they expect one.

---

## The problem

Giving your mod a config file today means registering your own `AssetStore`. That gets you:

- A whole asset folder holding a single file, `Server/YourMod/Config/YourMod.json`
- An asset class, a codec, a key function, and store registration written by hand
- No way for anyone else to override a value without replacing the entire file

And every mod that does this puts its settings somewhere different, so a server owner running twelve mods hunts through twelve layouts.

## What Configly does

Registers the store once, for every mod. You write the codec for your own fields and nothing else. Every mod's config lands in one shared `Server/Configs/` folder, one file each.

```
Server/Configs/
├── Hexcode.json
├── Icarus.json
└── YourMod.json
```

---

## Integration

Define your config. Extend `Config`, give it a `BuilderCodec`, and write it exactly like any other Hytale asset codec:

```java
public final class HexcodeConfig extends Config {
    public static final BuilderCodec<HexcodeConfig> CODEC = BuilderCodec.builder(HexcodeConfig.class, HexcodeConfig::new)
        .append(new KeyedCodec<>("RespectPvp", Codec.BOOLEAN),
            (config, b) -> config.respectPvp = b,
            config -> config.respectPvp)
        .documentation("Whether Hexcode spells honour the server PVP setting")
        .add()
        .build();

    private boolean respectPvp = true;

    public boolean respectsPvp() {
        return respectPvp;
    }
}
```

Register it in `setup()`:

```java
Configly.register("Hexcode", HexcodeConfig.class, HexcodeConfig.CODEC);
```

Ship the default at `Server/Configs/Hexcode.json`:

```json
{
  "Type": "Hexcode",
  "RespectPVP": true
}
```

Read it anywhere:

```java
HexcodeConfig.get().respectsPvp();
```

That is the whole integration.

---

## What you get

**Any shape you want.** Your codec, your fields, your validation. Configly never inspects your config, so it can be as simple as one flag or as deep as a nested balance table.

**Typed asset editor support, free.** Every field becomes a real editor control and every `.documentation(...)` string becomes its description. Your users stop guessing at raw JSON, and you stop fielding "what does this value do" reports.

**Hot reload.** Edits from disk or the asset editor take effect without a restart.

**Patchable by other packs.** A compatibility pack can override a single value of your config without touching your mod, gated on your mod being installed:

`Server/Configs/Hexcode.patch`
```json
{
  "$Requires": "Riprod:Hexcode",
  "RespectPVP": false
}
```

Everything the patch does not name keeps your value. Requires [Patchly](https://modtale.net/mod/patchly); Configly works fine without it.

**Uninstall-safe.** If your mod is removed, its config file is left byte-for-byte intact rather than erroring. Reinstall and every tuned value and third-party patch is still there.

---

## Shipping it

Two options: You can either put a Configly.jar into `lib/` and locally shade it or reference Configly from the maven repo

```groovy
plugins {
    id 'com.gradleup.shadow' version '9.4.2'
}

configurations {
    shadowBundle
    implementation.extendsFrom(shadowBundle)
}

dependencies {
    shadowBundle(files('lib/Configly-X.Y.Z.jar'))
    // or shadowBundle('curse.maven:configly-1550889:8498417')
}

shadowJar {
    archiveClassifier.set('')
    mergeServiceFiles()
    configurations = [project.configurations.shadowBundle]
}

tasks.build {
    dependsOn("shadowJar")
}
```

**Your mod ships alone.** No required dependency, nothing for your users to install.

**Bundling is safe.** Any number of mods can each carry their own shaded copy. At startup one registers the store and the rest bind to it, so copies never collide and never fight over the folder.

---

**Docs and source:** https://github.com/itsriprod/configly
**Issues:** https://github.com/itsriprod/configly/issues
**License:** GPLv3
