# v1.0.0

Initial release.

- Shared `Server/Configs/` asset store, registered once per JVM regardless of how many mods bundle Configly.
- Per-mod config types declared with `Configly.register(type, class, codec)`.
- `ConfigRef` typed accessor with reload-aware caching.
- `.patch` support on configs via Patchly.
- Configs visible in the asset editor.
