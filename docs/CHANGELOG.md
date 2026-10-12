# Journal des modifications

Format : version du mod `warfare`, de la plus recente a la plus ancienne.

## 0.7.3 — 12 octobre 2026 — « ecran de choix de classe »

### Nouvel ecran : CHOISIR UNE CLASSE
- `client/ChooseClassScreen.java`. Le menu d'accueil ouvre desormais un **selecteur
  avec apercu** au lieu de l'editeur : liste des 5 emplacements a gauche, detail
  complet de la classe selectionnee a droite. L'editeur detaille (`ClassScreen`)
  reste accessible par l'entree **MODIFIER**, qui s'ouvre directement sur
  l'emplacement choisi (nouveau constructeur avec numero d'emplacement).
- Apercu : nom de la classe sur un bandeau, arme principale et secondaire avec leur
  **modele d'objet rendu en grand** (`GuiGraphics.renderItem` a l'echelle 4, la pile
  est construite avec `Loadout.makeGun`, exactement comme l'equipement reel), les
  3 atouts avec leur effet, les 3 killstreaks avec leur seuil.
- Les entrees verrouillees par le niveau s'affichent en rouge avec « NIV. x »
  (`Unlocks`), coherent avec le reste de la progression.
- **EQUIPER** envoie `SelectClass` ; l'entree devient « CLASSE EQUIPEE » et se grise
  quand la classe selectionnee est deja celle qui est equipee.
- Meme grille centree que le menu d'accueil, meme police, meme pied de page.
- L'ATH vanilla est masque sur cet ecran aussi.

## 0.7.2 — 12 octobre 2026 — « mise en page du menu refaite »

Defauts constates en jeu sur capture, tous corriges.

### Corrections
- **Le menu debordait de l'ecran** : avec 11 entrees a pas fixe, JOUEURS etait coupe en
  bas et REGLAGES / RETOUR / QUITTER etaient hors champ. L'ecart entre entrees est
  maintenant **deduit de la hauteur disponible** (borne entre 15 et 26 px), donc le
  menu tient quelle que soit la resolution ou l'echelle d'interface.
- **Le pied de page chevauchait les dernieres entrees.** Il a sa propre bande reservee.
- **La ligne du joueur sortait de l'ecran a droite** : les lignes sont desormais bornees
  a la largeur de la colonne.
- **L'ATH de Minecraft traversait le menu** (coeurs, barre d'objets). Il est masque tant
  qu'un ecran du mod est ouvert (`RenderGuiOverlayEvent`), le notre aussi.

### Mise en page
- Grille **centree** : zone de contenu bornee a 1080 px et centree, deux colonnes
  (menu 36 %, panneau le reste) separees de 48 px, gouttiere proportionnelle.
  Tout est calcule a partir de la taille de la fenetre, plus aucune coordonnee en dur.
- Trois bandes : titre (titre interlettre + filet ambre pleine largeur + etat a gauche,
  carte active a droite), corps, pied de page.
- Chaque panneau a un **intitule interlettre souligne** aligne sur la meme grille que
  le menu.
- CLASSEMENT : lignes alternees et colonnes alignees a droite depuis le bord du panneau.
- JOUEURS : lignes a hauteur fixe, liseré de couleur d'equipe, score aligne a droite.
- MON PROFIL : grade en gros interlettre, barre d'XP elargie.

## 0.7.1 — 12 octobre 2026 — « police et habillage du menu »

### Police de l'interface
- L'interface n'est plus dessinee avec la police de Minecraft. Le mod embarque
  **Rajdhani** (Indian Type Foundry), caractere carre pense pour les interfaces,
  sous licence **SIL Open Font 1.1** donc redistribuable.
- Deux faces : `warfare:ui` (SemiBold) pour le texte, `warfare:title` (Bold) pour les
  titres. Fichiers dans `mod/src/main/resources/assets/warfare/font/`, licence a cote.
- La police est appliquee par le **style du Component** (`Style.withFont`), voie
  supportee par Minecraft : ni mixin ni reflexion. Nouveau helper `client/Fonts.java`.
- Elle voyage dans le jar du mod, qui est deja un pack de ressources : **rien de plus
  a installer pour les joueurs**, et pas besoin de `resource-pack=` dans
  `server.properties`.
- Appliquee a `LobbyScreen`, `ClassScreen`, `MatchIntroScreen`, `HudOverlay`,
  `ScorePopups`, `MenuItem` et `WarButton` (80 appels de dessin convertis).

### Menu d'accueil
- Titre **MINECRAFT WARFARE** interlettre et agrandi (`Fonts.drawTracked` : Minecraft
  ne sait pas espacer les caracteres, on dessine glyphe par glyphe).
- Les 11 entrees sont regroupees par **filets de separation** : partie / equipement /
  informations / sortie. Les fonctions ajoutees en 0.7.0 sont toutes conservees.
- Bandeau de l'entree active en **noir franc** et prolonge jusqu'au bord gauche de
  l'ecran, au lieu d'un rectangle vert sombre.
- Texte et gris neutralises (la teinte verte donnait un aspect « interface Minecraft »).

### Police de Call of Duty : non retenue
La police fournie (`ModernWarfare-8MM6z.ttf`, ffonts.net) porte en interne
`Typeface (c) (your company). 2019. All Rights Reserved` et n'est accompagnee d'aucune
licence. Le pack etant publie sur un depot GitHub public, elle n'a pas ete embarquee.
Conforme a la regle 6 d'AGENTS.md. Elle reste utilisable **en local** par qui la
remplace dans `assets/warfare/font/warfare_ui.ttf` sans republier le pack.

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
