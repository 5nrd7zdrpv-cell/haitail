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

## Abnahme/Test-Checkliste
1. `./gradlew clean jar`
2. Server starten, Plugin lädt ohne Exceptions.
3. `/protect add spawn self 30` → innerhalb: Block break/place wird verhindert; außerhalb: erlaubt.
4. `/protect status` → zeigt inside/outside + Entfernung + aktive Schutzpunkte.
5. `/farm setspawn self` → `/kill` → Respawn am gesetzten Punkt.
6. PVP: Spieler A schlägt Spieler B → `/combat status` zeigt "im Kampf" + Restzeit; `/combat canwarp` ist false; nach Ablauf true.
7. Combat-Log: im Kampf disconnect → Serverlog enthält Eintrag mit Username.
8. `/farm reset now` → Reset passiert, Prefab-Spawn wird geladen, Schedule läuft weiter.
