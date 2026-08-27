# v1.1.0

Updated to Update 6

# v1.0.2

Updated configly to correctly be MIT

Updated configly to support Update 6
Note: it will continue to error until update 6 fully releases due to semver issues


# v1.0.1
Updated to use CI/CD publishing

# v1.0.0

Initial release.

- Shared `Server/Configs/` asset store, registered once per JVM regardless of how many mods bundle Configly.
- Per-mod config types declared with `Configly.register(type, class, codec)`.
- `ConfigRef` typed accessor with reload-aware caching.
- `.patch` support on configs via Patchly.
- Configs visible in the asset editor.
