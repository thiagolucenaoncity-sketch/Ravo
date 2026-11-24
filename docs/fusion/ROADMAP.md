# RAVO Fusion

Experimental development only. Main stays unchanged.

## Replica reference
- https://github.com/Jakeschincariol/replica-skill
- https://github.com/Itskorrah/replica-skill-universal

Use Recon, Architect, Design, Build, QA and Diff as research workflows. Review upstream skills and dependencies before installing or executing anything. No external scripts or credentials in CI.

## Scope
Benchmark publicly observable Waze features using authorized usage and independently implement improvements. Do not copy proprietary code, assets, private endpoints or traffic data. Preserve RAVO branding, dashboard, custom colors, trip, media controls and existing map engines.

## Priorities
| Feature | Validation |
|---|---|
| Route guidance and rerouting | Synthetic GPS traces, off-route recovery, no repeated requests |
| Map camera and GPS smoothing | Compare frame times, latency, memory and GPS accuracy with baseline |
| Speed limits and camera alerts | Licensed data, direction-aware deduplication, unknown limit handling |
| Traffic and incident reporting | Authorized data sources, expiry and provenance |
| Navigation UI | Legible instructions, arrival time and distance, minimal interaction while moving |
| Offline recovery | Network loss, reconnect, no misleading live-data claims |

## Stages
1. Inventory actual RAVO code and dependencies; record Waze observable behavior and evidence, not assumptions.
2. Compare gaps and select first improvement based on measured performance.
3. Implement independently behind default-off experimental flags where appropriate.
4. Unit and instrumentation tests, simulated route replay, stress tests on low-end and target Android devices. Measure crash/ANR, frame times, memory, battery and routing latency against baseline.
5. Review licensing, privacy, regression results and APK build; no merge or production update before approval.

## Current status
Branch and roadmap only. Replica not installed, no app code changed, no APK built. Benchmark evidence and baseline measurements pending.
