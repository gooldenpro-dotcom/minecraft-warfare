# Journal des modifications

Format : version du mod `warfare`, de la plus recente a la plus ancienne.

## 0.7.0 — 12 octobre 2026 — « modes multijoueur, progression, menu repare »

### Rangement
- Tout le projet est rassemble dans `~/Desktop/Minecraft Warfare` (il etait eclate
  entre le Bureau et `~/Documents/minecraft-warfare`). Historique git conserve.
- `Maps/` → `maps/`, anciens `AGENTS.md` et `PROMPTS.md` archives dans `docs/archive/`.
- Nouvelle documentation : `ETAT_ACTUEL.md`, `HANDOFF.md`, `CHANGELOG.md`,
  `PROCHAINES_ETAPES.md`, `ROADMAP.md`, `TESTS.md`, plus `AGENTS.md` et `CLAUDE.md`
  a la racine.

### Menu (corrections)
- **Echap ferme le menu** (`shouldCloseOnEsc` renvoyait toujours faux hors partie : le
  joueur ne pouvait ni revenir au jeu ni quitter le serveur).
- Le menu ne se rouvre plus automatiquement une fois ferme (`menuDismissed`), il se
  rouvre avec **M** ou au retour au lobby apres une partie.
- Boutons **RETOUR AU JEU** et **QUITTER LE SERVEUR** (deconnexion propre).
- Nouveaux panneaux : MODE DE JEU, CARTE, CLASSEMENT, MON PROFIL.
- Touche **M** = menu d'accueil, **C** = creation de classe (etait `open_menu` pour C).

### Modes de jeu
- Nouveau `game/GameMode.java` : `tdm`, `ffa`, `jeu_d_armes`, `une_balle`,
  `1v1_sniper`, `1v1`, avec score, duree, equipement, vies et multiplicateur de degats
  par mode, tous reglables dans `[modes]`.
- Equipes ALPHA / BRAVO (`game/Team.java`, `game/Teams.java`) : couleur du pseudo,
  tir allie coupe (scoreboard vanilla + annulation explicite des degats).
- Jeu d'armes : echelle configurable de 17 armes, l'arme change a chaque elimination.
- Une balle chargee : Deagle, une seule balle, degats x10, une elimination rend une balle.
- Duels 1v1 limites a 2 joueurs.
- Choix du mode et de la carte depuis le menu (`RequestMode`, `RequestArena`) ou en
  commande (`/warfare mode`, `/warfare arena select`), console comprise.

### Cartes
- `game/Arena.java` : plusieurs cartes dans le meme monde, chacune avec son lobby et
  ses points d'apparition etiquetes par camp. `WarfareData` relit l'ancien format
  (lobby + liste de points) et le convertit en arene « defaut ».
- Creation automatique d'une arene jouable au premier demarrage sur un monde.
- Commandes : `/warfare arena create|select|delete|list`, `/warfare addspawn [alpha|bravo]`.

### Progression
- `progression/` : `Rank` (70 niveaux, 10 prestiges, grades), `PlayerStats`,
  `StatsStore` (JSON par joueur + classement serveur), `Unlocks` (niveau requis par
  arme, atout et killstreak, deduit du catalogue TaCZ et surchargeable en config).
- XP a l'elimination, au tir a la tete, a la killstreak, a la victoire, a la defaite et
  a la participation ; montee de niveau annoncee a l'ecran.
- Les choix verrouilles sont refuses **cote serveur** (`ClassStore.sanitize` prend le
  niveau du joueur) et affiches « NIV. x » dans le menu de classe.
- `/warfare stats [joueur]`, `/warfare top`, `/warfare xp`, `/warfare setlevel`.

### ATH
- `client/HudOverlay.java` : chrono, scores d'equipe, serie en cours, vies, echelle
  d'armes, grade et barre d'XP.
- `client/ScorePopups.java` : « +100 ELIMINATION » qui monte a l'ecran, avec son vanilla.

### Monde
- Monstres desactives (gamerules + nettoyage periodique des entites hostiles),
  heure fixe, meteo coupee, feu et mobGriefing coupes — section `[world]`.

### Technique
- Protocole reseau **4 → 5** (paquets `MatchState` et `SyncClasses` enrichis, 6 nouveaux
  paquets). **Les clients 0.6.1 ne peuvent plus se connecter.**
- `WarfareData.Spot` devient `game/Spot.java`.
- `xpMultiplier` mis a 3.0 dans la config du serveur local (progression a deux).

## 0.6.1 — menu d'accueil, demarrage depuis le menu
## 0.6.0 — hub de lobby et ecran de preparation
## 0.5.x — pack packwiz, instance Prism auto-mise-a-jour, guide amis
## 0.4.0 — killstreaks
## 0.3.0 — creation de classes et atouts
## 0.2.0 — mode Free For All cote serveur
## 0.1.0 — serveur Forge 1.20.1 + TaCZ operationnel
