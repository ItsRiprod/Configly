# Configly

Mod-wide configuration files for Hytale mods. Zero dependencies.

Every mod that wants a config file today has to register its own `AssetStore`, which means a whole asset folder for a single file, a hand-rolled codec surface, and no way for anyone else to override it. Configly registers one store for everyone and hands each mod a typed view of its own file.

- One shared `Server/Configs/` folder, one file per config, keyed by filename.
- Each config declares a `Type`; the owning mod supplies the codec, so a config can be any shape.
- Configs show up in the asset editor as real typed fields, not a JSON blob.
- Other packs can `.patch` a config to override or extend values.

Configly can be dropped into `mods/` as a standalone jar, or shaded into your own mod jar. Multiple copies across multiple mods coordinate so exactly one registers the store and every other copy binds to it, so your mod ships and runs on its own with no required dependency.

## Initializing

Extend `Config` with your own class and codec:

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

Register it in your plugin's `setup()`:

```java
@Override
protected void setup() {
    Configly.register(HexcodeConfig.TYPE, HexcodeConfig.class, HexcodeConfig.CODEC);
}
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

Full setup, shading, and patching docs: [docs/index.md](docs/index.md).

## Building

```
./gradlew build
```
