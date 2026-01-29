# FarmWorld Plugin - Testplan

## Vorbereitung
1. Server starten und prüfen, ob `[FarmWorld]`-Logs für Konfiguration und Command-Registrierung erscheinen.
2. Sicherstellen, dass `config/farmworld.json` geladen ist (optional anpassen und neu laden).

## Farm
1. `/farm status`
   - Spawn-Koordinaten, Prefab-Name und Reset-Timer prüfen.
2. `/farm setspawn self`
   - Mit `/kill` respawnen und prüfen, ob der Spawnpunkt verwendet wird.
3. `/farm reset now`
   - Server-Log prüfen: Reset gestartet/abgeschlossen.

## Protect
1. `/protect add self 32 Spawn`
2. `/protect list`
3. Innerhalb des Radius Blöcke platzieren/abbauen:
   - Erwartung: blockiert (ohne Bypass-Permission).
4. Außerhalb des Radius Blöcke platzieren/abbauen:
   - Erwartung: erlaubt.
5. `/protect status`
   - Erwartung: IN/OUT mit Zone, Distanz, Radius.
6. `/protect remove Spawn`
   - Zone entfernen und erneut testen.

## Combat
1. Spieler A greift Spieler B an.
2. `/combat status` von Spieler A und B:
   - Erwartung: im Kampf, Restzeit in Sekunden.
3. `/combat canwarp`:
   - Erwartung: `gesperrt` im Combat.
4. Spieler im Combat loggt aus:
   - Erwartung: Server-Log mit Combat-Logout und Spielername.
