# 🔪 MurderMystery – Minecraft Minigame (Paper 1.21+)

Ein **komplettes Murder Mystery Minigame** für Minecraft (Paper 1.21+).  
Spieler übernehmen die Rollen **Murderer**, **Detective** oder **Bystander** und kämpfen ums Überleben.  
Das Plugin ist speziell für Servernetzwerke mit **RankPointsAPI-Integration** entwickelt.

---

## 🚀 Features

- Automatisches **Rollen-System**
    - 1x Murderer (Schwert, tötet heimlich alle)
    - 1x Detective (Bogen mit Cooldown, muss den Murderer enttarnen)
    - Rest: Bystander (unschuldig, gewinnen durch Überleben)
- **Countdown-System** beim Start
- **Arena-Management**
    - Mehrere Arenen über `config.yml` definierbar
    - Feste Spawnpunkte (`/mm setspawn`) oder dynamische Safe-Spawns per Region
    - Automatische Teleports zwischen Lobby, Arenen und Main-Welt
- **Lobby-System**
    - Join-Schilder mit Größenwahl (small/mid/large)
    - Schilder blockieren automatisch andere Größen, sobald ein Spieler eine Runde startet
    - Lobby ist wie Arenen vor Interaktionen/Abbau geschützt
- **Punkte-System** (RankPointsAPI)
    - Dynamische Punktevergabe: Kills, Überleben, Sieg, Niederlage, Quit-Strafen
    - Konfigurierbare Werte in `config.yml`
    - Transparente Anzeige der Punkte am Rundenende im Chat
- **Große Titel-Anzeigen**
    - Kill-Meldungen sofort als Titel für alle
    - Runde-Ende: Gewinner & Verlierer mit fetter Anzeige
- **Anti-Cheat Schutz**
    - Murderer-Schwert & Detective-Bogen können nicht gedroppt, bewegt oder gelagert werden
    - Cooldown für Detective-Bogen (3 Sekunden)
    - Quit während des Spiels → Strafe + korrektes Handling
- **Debug-Modus** für Entwickler/Serveradmins

---

## 🕹️ Spielablauf

1. Spieler joinen über `/mm join` oder Lobby-Schilder `[MurderMystery] small/mid/large`.
2. Sobald die **Mindestanzahl Spieler** erreicht ist, startet ein Countdown.
3. Nach Countdown:
    - Rollen werden zufällig verteilt
    - Spieler werden auf **verschiedene Spawnpunkte** verteilt (keine Überschneidungen)
    - Murderer bekommt Schwert, Detective Bogen+Pfeil, Bystander nichts
4. Siegbedingungen:
    - Murderer tötet alle → Murderer gewinnt
    - Murderer wird getötet (z. B. durch Detective-Bogen) → Innocents/Detective gewinnen
    - Detective schießt auf Innocent → Punkteabzug & Broadcast
5. Runde endet → Punkte werden verteilt, Statistiken im Chat ausgegeben, Arena & Lobby werden zurückgesetzt.

---

## 📜 Befehle

| Befehl                  | Beschreibung |
|-------------------------|--------------|
| `/mm join`              | Spieler tritt einer Lobby/Runde bei |
| `/mm leave`             | Spieler verlässt die Runde |
| `/mm forcestart`        | Startet eine Runde sofort (Admin) |
| `/mm setspawn <arena>`  | Fügt einen neuen Spawnpunkt für eine Arena hinzu (Admin) |
| `/mm setspawn lobby`    | Fügt einen Spawnpunkt für die Lobby hinzu |
| `/mm help`              | Zeigt alle verfügbaren Subcommands |

---

## 🔑 Permissions

| Permission              | Beschreibung |
|-------------------------|--------------|
| `murdermystery.use`     | Basis-Permission für `/mm` |
| `murdermystery.admin`   | Erlaubt Admin-Befehle wie `/mm forcestart` und `/mm setspawn` |
| `murdermystery.join`    | Erlaubt einem Spieler, einer Runde beizutreten |
| `murdermystery.leave`   | Erlaubt einem Spieler, eine Runde zu verlassen |

---

## ⚙️ Konfiguration (`config.yml`)

```yaml
worlds:
  main: world   # Hauptwelt, in die Spieler nach Spielende zurückkehren
  lobby: lobby  # Lobby-Welt, in der Spieler zwischen den Runden warten

lobby-spawns:
  - "0, 65, 0"
  - "5, 65, 5"
  - "-5, 65, 5"
  - "10, 65, -3"

arenas:
  map1:
    world: map1
    maxPlayers: 16
    size: small
    spawns:
      - 0,64,0
      - 10,64,0

  map2:
    world: map2
    maxPlayers: 12
    size: mid
    region:
      minX: -30
      maxX: 30
      minZ: -30
      maxZ: 30

  map3:
    world: map3
    maxPlayers: 20
    size: large
    spawns:
      - -5,64,-5

points:
  kill-murderer: 5
  kill-innocent: -5
  kill-as-murderer: 2
  survive: 3
  win: 5
  co-win: 2
  lose: 2
  consolation: 2
  quit: -3
  time-up: 3

min-players: 3
countdown-seconds: 15
max-game-seconds: 600

gamemode: bow-fallback

protection:
  allow-admin-move: true

rankpoints:
  debug: false
  exclude-staff: false

Rank-Points-API-url: "jdbc:mysql://server:3306/rankpoints"
Rank-Points-API-user: "user"
Rank-Points-API-password: "password"

debug: true

message-cooldown:
  global: 3000
  player: 2000

player-gamemode: adventure
```

---

## 🧪 Debug-Modus

- Aktivieren in `config.yml`:
  ```yaml
  debug: true
  ```
- Ausgabe im Server-Log (`[DEBUG] …`) für:
    - Spielstart, Countdown, Rollenverteilung
    - Kills, Quit/Rejoin, Punktevergabe
    - Arena-Teleports, Spawnverhalten

---

## 🔒 Anti-Cheat Mechanismen

- Murderer-Schwert & Detective-Bogen **können nicht bewegt, gedroppt oder gelagert werden**
- Detective-Bogen mit **3 Sekunden Cooldown**
- Quit während des Spiels → -Punkte + Kick in Hauptwelt
- FailSafe-System stellt Waffen automatisch wieder her, falls gelöscht
- Lobby & Arenen: BlockBreak, BlockPlace, Interaktionen, Feuerzeug, Eimer deaktiviert

---

## 📦 Installation

1. `MurderMystery.jar` in den `plugins/`-Ordner legen.
2. Server starten → `config.yml` wird generiert.
3. Arenen & Spawns per `/mm setspawn <arena>` hinzufügen.
4. Punkte- und DB-Einstellungen in der Config anpassen.
5. Server neu starten.
6. `/mm join` testen 🚀

---

## 🏆 Credits

Basierend auf eigenen Entwicklungen & inspiriert von Community-Projekten.  
RankPointsAPI: https://github.com/Catmaster420  
