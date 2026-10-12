package com.riprod.configly;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.Priority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;

public final class Configly {
    static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String STORE_PATH = "Configs";
    private static final String STORE_MARKER = Config.class.getSimpleName();

    private static volatile AssetStore<String, ?, ?> store;

    private Configly() {
    }

    public static <T extends Config> void register(@Nonnull String type, @Nonnull Class<T> clazz, @Nonnull BuilderCodec<T> codec) {
        ((AssetCodecMapCodec) mapCodec()).register(type, clazz, codec);
        LOGGER.at(Level.FINE).log("Registered config type '%s' for %s", type, clazz.getName());
    }

    @Nullable
    public static <T extends Config> T get(@Nonnull String id, @Nonnull Class<T> clazz) {
        var asset = assetMap().getAsset(id);
        return clazz.isInstance(asset) ? clazz.cast(asset) : null;
    }

    @Nonnull
    public static <T extends Config> T getOrElse(@Nonnull String id, @Nonnull Class<T> clazz, @Nonnull T fallback) {
        var value = get(id, clazz);
        return value != null ? value : fallback;
    }

    public static void install() {
        store();
    }

    @Nonnull
    private static AssetMap assetMap() {
        return store().getAssetMap();
    }

    @Nonnull
    private static AssetCodecMapCodec<?, ?> mapCodec() {
        var codec = store().getCodec();
        if (codec instanceof AssetCodecMapCodec<?, ?> map) return map;
        throw new IllegalStateException("The registered config store is not backed by an AssetCodecMapCodec: " + codec.getClass().getName());
    }

    static synchronized AssetStore<String, ?, ?> store() {
        if (store != null) return store;

        var existing = find();
        if (existing != null) {
            LOGGER.at(Level.FINE).log("Configly %s bound to a config store registered by another copy", BuildInfo.VERSION);
            return store = existing;
        }

        var map = new AssetCodecMapCodec<String, Config>(Codec.STRING,
            setter(ConfigAccess::setId), getter(ConfigAccess::getId),
            setter(ConfigAccess::setData), getter(ConfigAccess::getData),
            true);

        // the static registry, not PluginBase.getAssetRegistry(): the scoped one unregisters the
        // store when its registering plugin disables, which would break every other copy still reading
        var registered = AssetRegistry.register(HytaleAssetStore
            .builder(Config.class, new DefaultAssetMap<String, Config>())
            .setPath(STORE_PATH)
            .setCodec(map)
            .setKeyFunction(getter(ConfigAccess::getId))
            .build());

        map.register(Priority.DEFAULT, "Configly:Opaque", OpaqueConfig.class, OpaqueConfig.CODEC);

        LOGGER.at(Level.INFO).log("Configly %s registered the config store at Server/%s", BuildInfo.VERSION, STORE_PATH);
        return store = registered;
    }

    // these run against every loaded config, including leaves from other shaded copies. declaring
    // the lambda over Object keeps the checkcast out of its bridge method; the cast below applies
    // to the function object, not the config, so it is a runtime no-op
    private static <A, B> BiConsumer<A, B> setter(BiConsumer<Object, Object> raw) {
        return (BiConsumer) raw;
    }

    private static <A, B> Function<A, B> getter(Function<Object, Object> raw) {
        return (Function) raw;
    }

    // copies find each other through the engine's store map. relocation rewrites packages but
    // never simple names, so the marker matches across shaded copies
    @Nullable
    private static AssetStore<String, ?, ?> find() {
        for (var candidate : AssetRegistry.getStoreMap().values()) {
            if (STORE_PATH.equals(candidate.getPath()) && STORE_MARKER.equals(candidate.getAssetClass().getSimpleName())) {
                return (AssetStore<String, ?, ?>) candidate;
            }
        }
        return null;
    }
}
