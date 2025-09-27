# MurderMystery3 – Punktevergabe & Ablaufdokumentation

## 📊 Ablauf der Punktevergabe nach Rolle

### 🔪 Murderer
- **Kill mit Schwert** →  
  `SwordListener.onSwordHit()` → `RoundStats.addKill(attacker)`
- **Eliminieren anderer Spieler** →  
  Opfer wird Spectator (`PlayerManager.eliminate()`)
- **Sieg durch Eliminierung aller** →  
  `RoundResultManager.distributeMurdererWin()`
    - `Kills × points.kill-as-murderer`
    - `+ points.win`
- **Niederlage** →
    - `+ points.lose` (Trostpunkte)
    - `+ Kills × points.kill-as-murderer`

---

### 🏹 Detective
- **Schuss auf Murderer** →  
  `BowListener.onArrowHit()`
    - `checkWinConditions()` löst Detective-Sieg aus
    - Runde endet sofort
- **Schuss auf Innocent** →  
  `RoundStats.addDetectiveInnocentKill(shooter)`
    - Strafe kommt erst am Ende
- **Endabrechnung** → `RoundResultManager.distributeDetectiveWin()`
    - **Win:** `points.win`
    - **Mörder erwischt:** `points.kill-murderer`
    - **Fehlabschüsse:** `fails × points.kill-innocent`
    - Überleben bei TIME_UP → `points.survive`

---

### 🧍 Innocent (Bystander)
- **Kann nicht selbst Punkte durch Kills holen.**
- **Aufnahme von Detective-Bogen** (wenn Detective stirbt) → Rolle wechselt zu Detective.
- **Überleben** → `RoundStats.markSurvived(player)`
- **Endabrechnung**:
    - **Detective-Sieg:**
        - Überlebt → `points.survive + points.co-win`
        - Tot → `points.co-win`
    - **Murderer-Sieg:**
        - Tot → `points.lose` (Trostpunkte)
    - **Zeitablauf:**
        - Überlebt → `points.survive`
        - Tot → `points.lose`

---

## ⏰ Sonderfall: Zeitablauf (TIME_UP)
- **Murderer:** `kills × points.kill-as-murderer + (survive ? points.survive : 0)`
- **Detective:** `(survive ? points.survive : 0) + fails × points.kill-innocent`
- **Innocent:** `survive ? points.survive : points.lose`

---

## 🔒 Weitere Regeln
- **Quitter:** `RoundStats.markQuitter()` → Punkte = 0, zusätzlich `points.quit` Strafe sofort bei Leave.
- **Negativwerte:** werden am Ende mit `Math.max(0, …)` begrenzt → Rundenpunkte nie negativ.
- **Rundenstatistik (Chat):** Ausgabe angepasst je nach Rolle → zeigt Sieg/Niederlage/Unentschieden, Kills, Überleben, Fehlabschüsse.

---

## 🧮 Beispielrunde (5 Spieler)

- **Spieler A** → Murderer
- **Spieler B** → Detective
- **Spieler C, D, E** → Innocents

### Ablauf
1. Murderer A tötet Innocents C & D.
    - `RoundStats.addKill(A)` = 2
    - C & D → eliminiert.
2. Detective B schiesst versehentlich Innocent E.
    - `RoundStats.addDetectiveInnocentKill(B)` = 1
    - E → eliminiert.
3. Murderer A und Detective B stehen im Finale.
    - B schiesst A → Murderer eliminiert.
    - `RoundStats.addDetectiveInnocentKill(B)` bleibt = 1
    - Runde endet mit **Detective-Sieg**.

### Punkteberechnung
- **Murderer A**
    - 2 Kills × 2 Punkte = 4
    - Verloren → `+ lose (2)`
    - **Gesamt: 6**

- **Detective B**
    - Sieg → `+ win (5)`
    - Murderer erwischt → `+ kill-murderer (5)`
    - Fehlabschuss 1 × -5 = -5
    - **Gesamt: 5**

- **Innocents C, D, E**
    - Alle tot → keine Überlebenspunkte.
    - Co-Win bei Detective-Sieg → `+ co-win (2)`
    - **Gesamt: 2 je Spieler**

### Endergebnis
- A (Murderer) → 6 Punkte
- B (Detective) → 5 Punkte
- C, D, E (Innocents) → je 2 Punkte

---
