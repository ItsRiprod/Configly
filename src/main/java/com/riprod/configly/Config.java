package com.riprod.configly;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;

public abstract class Config implements JsonAssetWithMap<String, DefaultAssetMap<String, Config>> {
    protected AssetExtraInfo.Data data;
    protected String id;

    @Override
    public String getId() {
        return this.id;
    }
}
