# FarmWorldDaemon (Step 4) – CombatService (Anti-Combat-Log API) + Warp-Query

Dieses ZIP baut auf Step 3 auf und ergänzt:

## CombatService
- Combat-Tagging mit TTL (Sekunden) aus Config:
  - `combat.enabled`
  - `combat.tagSeconds`
  - `combat.onQuit` = `NONE | KILL | PENALTY`
  - optional: `combat.penaltySeconds` (Cooldown/Timeout fürs Warp-System)
- API-Funktionen:
  - `tag(playerId, reason)`
  - `isInCombat(playerId)`
  - `getRemainingSeconds(playerId)`
  - `canWarp(playerId)` (false wenn in combat oder penalty aktiv)
  - `onPlayerQuit(playerId)` (wendet onQuit-Policy an, setzt ggf. penalty)

## Test ohne Hytale-API (Simulation)
Commands:
- `combat tag <playerId> [seconds] [reason...]`
- `combat status <playerId>`
- `combat canwarp <playerId>`
- `combat quit <playerId>`
- `combat cleanup` (purge expired)

Hinweis:
- In echter Integration callt ihr `combat.tag(uuid, "damage")` bei Damage/Hits
- Und im Warp-System prüft ihr `combat.canWarp(uuid)` + ggf. `getRemainingSeconds(uuid)`
