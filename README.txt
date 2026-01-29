FarmWorldDaemon – Step 4.1

Patch: nur Stil/Lesbarkeit (keine Logik geändert)
- Logs kürzer + alltagstauglich ([FarmWorld]/[Protect]/[Combat])
- Kommentare/Javadoc entschlackt
- ein paar pragmatische TODOs

Start:
  gradlew.bat run

Befehle:
  farm reset now
  farm reset schedule
  farm setspawn <x> <y> <z>
  farm status

  protect status
  protect test <ACTION> <x> <y> <z> [perm=true|false]

  combat tag <playerId> [seconds] [reason]
  combat status <playerId>
  combat canwarp <playerId>
  combat quit <playerId>
  combat cleanup
