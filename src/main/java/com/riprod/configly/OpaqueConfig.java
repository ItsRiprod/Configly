package com.riprod.configly;

import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class OpaqueConfig extends Config {
    public static final BuilderCodec<OpaqueConfig> CODEC =
        BuilderCodec.builder(OpaqueConfig.class, OpaqueConfig::new).build();

    private OpaqueConfig() {
    }
}
