# Etat actuel — Minecraft Warfare

> Mis a jour le 12 octobre 2026 — mod `warfare 0.7.0`
> Ce fichier decrit ce qui **tourne vraiment aujourd'hui**. Pour la suite, voir
> [PROCHAINES_ETAPES.md](PROCHAINES_ETAPES.md) et [ROADMAP.md](ROADMAP.md).

## Socle technique

| Element | Version | Ou |
|---|---|---|
| Minecraft | 1.20.1 | — |
| Forge | 47.4.10 | `server/`, `pack/pack.toml` |
| Java | 17 (Oracle, arm64) | `/Library/Java/JavaVirtualMachines/jdk-17.jdk` |
| TaCZ (armes) | 1.20.1-1.1.8-hotfix2 | `server/mods/`, `mod/libs/` |
| Mod maison | `warfare 0.7.0` | `mod/`, compile vers `mod/build/libs/` |
| Monde actif | `Terminal_MW2` | `server/Terminal_MW2/` |
| Optimisation client | Embeddium, FerriteCore, ModernFix | `pack/mods/` |

Serveur : port **25565**, `online-mode=false`, `max-players=18`, `gamemode=adventure`,
MOTD « Minecraft Warfare ». Lancement : `./start.sh` (4 Go).

## Ce qui fonctionne

### Menu d'accueil (touche **M**, s'ouvre aussi a la connexion)
- **Echap ferme le menu** et ne le rouvre pas tout seul (le blocage « impossible de
  quitter la partie » est corrige), **QUITTER LE SERVEUR** deconnecte proprement.
- Choix du **mode de jeu** et de la **carte** depuis le menu, pousses a tous les joueurs.
- Acces direct a **CREER UNE CLASSE** et **KILLSTREAKS**.
- Panneaux **JOUEURS**, **CLASSEMENT** (serveur entier, joueurs hors ligne compris),
  **MON PROFIL** (grade, niveau, barre d'XP, bouton prestige au niveau 70),
  **REGLAGES DE PARTIE**.
- Par defaut **n'importe quel joueur** peut lancer la partie et changer de mode
  (`[modes] playersCanStart`), pas seulement les operateurs.

### Modes multijoueur (6)
| Mode | Identifiant | Regles |
|---|---|---|
| Match a mort par equipe | `tdm` | 2 camps (ALPHA/BRAVO), 75 points, tir allie coupe |
| Melee generale | `ffa` | chacun pour soi, 30 eliminations |
| Jeu d'armes | `jeu_d_armes` | echelle de 17 armes, une elimination = l'arme suivante |
| Une balle chargee | `une_balle` | Deagle, 1 balle, degats x10, une elimination rend une balle |
| 1VS1 Sniper | `1v1_sniper` | M700 impose, degats x2.5, 2 joueurs |
| 1VS1 | `1v1` | classes libres, 2 joueurs |

Chaque mode a son score, sa duree et ses reglages dans `[modes]` du fichier de config.

### Progression
- **70 niveaux**, **10 prestiges**, grades francophones (Recrue → General).
- XP : elimination 100, tir a la tete +50, killstreak +75, victoire 500, defaite 150,
  participation 100 — le tout multiplie par `xpMultiplier` (**reglee a 3.0** sur ce
  serveur pour progresser vite a deux).
- **Deblocages par niveau** : armes (deduites du catalogue TaCZ par type : pistolets et
  fusils d'assaut des le niveau 1, pompes 5, snipers 8, mitrailleuses 12, RPG 30),
  atouts (1 a 22) et killstreaks (1 a 26). Les entrees verrouillees s'affichent
  « NIV. x » et sont refusees cote serveur, pas seulement grisees.
- Statistiques par joueur dans `world/warfare/stats/<uuid>.json` : eliminations, morts,
  tirs a la tete, victoires, parties, meilleure serie, meilleur score, temps de jeu.
- Classement : `/warfare top` et panneau CLASSEMENT du menu.

### ATH (en partie)
Chrono et scores en haut, mode / serie en cours / vies / progression dans l'echelle
d'armes en bas a gauche, grade et barre d'XP en bas a droite, **points qui montent
(« +100 ELIMINATION »)** avec un son au centre. Desactivable (`[display] hudEnabled`,
`scorePopups`).

### Cartes (arenes)
Plusieurs cartes cohabitent dans le meme monde. Chaque arene a un lobby et des points
d'apparition, etiquetes `alpha` / `bravo` / libre. **Au premier demarrage le serveur
cree tout seul** une arene autour du point d'apparition du monde (4 points, 2 par camp)
pour etre jouable immediatement — a corriger ensuite en jeu.

### Monde
Monstres desactives (apparition coupee + nettoyage toutes les 5 s), heure fixe a midi,
meteo coupee, pas de degats de feu, pas de mobGriefing, pas d'ecran de mort,
killfeed maison.

## Ce qui n'est PAS fait

Zombies (survie BO2 sur Town), DMZ, boutique, minimap + drone UAV, Superb Warfare
(vehicules), launcher « un clic » hors Prism, police et sons facon MW2/BO2,
stand de tir, skins. Voir [ROADMAP.md](ROADMAP.md).

## Distribution aux joueurs

- Pack packwiz dans `pack/`, publie sur `https://github.com/gooldenpro-dotcom/minecraft-warfare-pack`.
- `pack/` contient deja `warfare-0.7.0.jar` et l'index rafraichi, **mais rien n'est
  pousse sur GitHub** : tant que le `git push` n'est pas fait, les joueurs restent en 0.6.1
  et **ne pourront pas se connecter** (le protocole reseau du mod est passe de 4 a 5).
- Instance Prism locale (`~/Library/Application Support/PrismLauncher/instances/Minecraft Warfare`)
  deja a jour en 0.7.0.
