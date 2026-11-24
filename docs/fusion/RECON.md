# Fusion Recon — baseline

Verified from main: app/build.gradle.kts, settings.gradle.kts, app/src/main/AndroidManifest.xml.
- Kotlin Android, Java/JVM 17, min SDK 26, target/compile SDK 34.
- MapLibre Android 11.11.0; JUnit 4.13.2.
- Single app module; applicationId com.moto.painel.
- MainActivity, SettingsActivity, SplashActivity, RavoMapsLabActivity (not exported), MusicListenerService.

Replica is an agent workflow, not an Android dependency. Universal upstream README documents project-local Codex installation; installation and host activation are not verified. Review installer and skills before execution; no CI execution or secret access.

## Comparison protocol
Use recorded synthetic GPS sequences on a stationary device, identical settings and repeated runs. Observe Waze only through authorized normal use, with no proprietary data extraction. Record evidence and distinguish missing functionality from unverified functionality.

| Scenario | Metrics |
|---|---|
| Straight road and turns | camera jitter, frame times, instruction latency |
| GPS loss and recovery | recovery time, stale position handling |
| Off-route event | reroute latency, duplicate requests |
| Network loss | clear status, recovery, stale traffic handling |
| Speed limit changes | direction correctness, unknown limits |
| Extended session | memory, crash/ANR, battery |

## Next step
Inspect actual navigation and location classes before implementing camera smoothing. Establish baseline traces and regression tests. Do not modify production or ship until tested.

Status: partial source inventory; no live Waze observations, installation, runtime benchmarks or APK yet.
