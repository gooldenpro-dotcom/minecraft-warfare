# Minecraft Warfare — Setup (étape 1 : serveur qui démarre)

Serveur Minecraft moddé local, façon Call of Duty. Cette étape ne contient **aucun gameplay** :
uniquement un serveur Forge fonctionnel avec le mod d'armes TaCZ.

## 1. Versions figées

| Élément | Version | Source |
|---|---|---|
| Minecraft | 1.20.1 | — |
| Forge | **1.20.1-47.4.10** (recommandée) | files.minecraftforge.net |
| Java | 17 (17.0.12 installé) | — |
| TaCZ | **1.1.8-hotfix2** (`tacz-1.20.1-1.1.8-hotfix2.jar`) | Modrinth, projet `timeless-and-classics-zero` |

Le **client et le serveur doivent utiliser exactement les mêmes versions** de Forge et de TaCZ.

## 2. Arborescence

```
minecraft-warfare/
├── start.sh            # lancement du serveur (4 Go de RAM)
├── server/             # serveur Forge installé
│   ├── mods/           # tacz-1.20.1-1.1.8-hotfix2.jar
│   ├── server.properties
│   ├── user_jvm_args.txt   # -Xmx4G / -Xms4G
│   ├── eula.txt        # eula=true (accepté le 2026-10-12)
│   ├── tacz/           # packs d'armes TaCZ (généré au 1er lancement)
│   └── world/          # monde (ignoré par git)
├── mod/                # vide — futur mod de gameplay
├── downloads/          # installeurs et jars téléchargés (ignoré par git)
└── docs/SETUP.md
```

## 3. Lancer le serveur

```bash
cd ~/Documents/minecraft-warfare
./start.sh
```

Le serveur est prêt quand la console affiche `Done (XX.XXXs)! For help, type "help"`.
Il écoute sur le port **25565**.

**Arrêter proprement** : taper `stop` dans la console du serveur, puis attendre
`ThreadedAnvilChunkStorage: All dimensions are saved`. (`Ctrl+C` fonctionne aussi : Minecraft
sauvegarde via son shutdown hook.)

Premier démarrage mesuré : ~27 s (dont 14 s de génération du spawn), 0 erreur.

## 4. Configuration du serveur (`server/server.properties`)

| Clé | Valeur | Effet |
|---|---|---|
| `motd` | `Minecraft Warfare` | nom affiché dans la liste des serveurs |
| `online-mode` | `true` | comptes Minecraft authentifiés obligatoires |
| `max-players` | `18` | 18 joueurs |
| `pvp` | `true` | combat entre joueurs activé |
| `difficulty` | `normal` | — |
| `gamemode` | `adventure` | pas de destruction de blocs |
| `force-gamemode` | `true` | tout le monde repasse en adventure à la connexion |
| `spawn-protection` | `0` | aucune zone protégée au spawn |
| `enable-command-block` | `true` | utile pour le gameplay plus tard |

## 5. Client : instance Prism Launcher

1. **Créer l'instance**
   - Prism Launcher → **Add Instance**
   - Onglet *Custom* → Version : **1.20.1**
   - Section *Mod Loader* → **Forge** → choisir **47.4.10** (la même que le serveur)
   - Nom : `Minecraft Warfare` → **OK**

2. **Vérifier Java**
   - Instance → **Edit** → *Settings* → *Java* → Java 17 (`/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`)
   - *Memory* : TaCZ est lourd (modèles 3D + sons) → **4096 Mo** maximum conseillé

3. **Installer TaCZ côté client**
   - Instance → **Edit** → **Mods** (panneau de gauche) → **Add file…**
   - Choisir `~/Documents/minecraft-warfare/downloads/tacz-1.20.1-1.1.8-hotfix2.jar`
   - Vérifier que le mod apparaît et qu'il est coché

4. **Ajouter le serveur local**
   - Lancer l'instance → **Multijoueur** → **Ajouter un serveur**
   - Nom : `Warfare local`
   - Adresse : `localhost` (équivalent : `127.0.0.1:25565`)

> `online-mode=true` : il faut un compte Minecraft payant connecté dans Prism Launcher,
> même pour jouer sur `localhost`.

## 6. Devenir opérateur

Les commandes (`/give`, `/gamemode`…) demandent le rang op. Dans la **console du serveur** :

```
op TonPseudo
```

## 7. Identifiants TaCZ utiles

Vérifiés dans le jar et dans le pack par défaut `server/tacz/tacz_default_gun` :

- Item arme : `tacz:modern_kinetic_gun`, l'arme est choisie par le NBT `GunId`
- Item munition : `tacz:ammo`, le calibre est choisi par le NBT `AmmoId`
- Armes du pack par défaut (`server/tacz/tacz_default_gun/data/tacz/index/guns/`) :
  `tacz:ak47`, `tacz:m4a1`, `tacz:glock_17`, `tacz:hk416d`, `tacz:deagle`, `tacz:ai_awp`,
  `tacz:kar98`, `tacz:m249`, `tacz:aa12`… (~40 armes)
- Munition de l'AK-47 : `tacz:762x39`
- Commande TaCZ interne : `/tacz reload` (recharge les packs d'armes), `/tacz overwrite true|false`

Donner une AK-47 + 64 cartouches :

```
/give TonPseudo tacz:modern_kinetic_gun{GunId:"tacz:ak47"}
/give TonPseudo tacz:ammo{AmmoId:"tacz:762x39"} 64
```

## 8. Dépannage

| Symptôme | Cause probable |
|---|---|
| `Incompatible mod set` / kick à la connexion | versions Forge ou TaCZ différentes client/serveur |
| Serveur refuse de démarrer, message EULA | `server/eula.txt` n'a pas `eula=true` |
| `LanServerPinger: No route to host` | normal en local sur macOS, sans effet |
| `Configuration file ... is not correct. Correcting` | normal au premier lancement (création des configs) |
| Arme invisible / texture manquante | TaCZ absent côté client |

## 9. Le mod `warfare` (etape 2)

Mode de jeu **Free For All** : chacun pour soi, premier a 30 eliminations ou meilleur score
au bout de 10 minutes. Entierement cote serveur — **rien a installer sur le client a cette
etape** (`displayTest="IGNORE_SERVER_VERSION"` dans mods.toml).

### 9.1 Compiler et deployer

Le mod est un MDK Forge 1.20.1-47.4.10 standard. TaCZ est resolu depuis `mod/libs/`,
donc la compilation utilise exactement le jar deploye sur le serveur :

```bash
cd ~/Documents/minecraft-warfare/mod
cp ../server/mods/tacz-1.20.1-1.1.8-hotfix2.jar libs/   # si libs/ est vide
./gradlew build
cp build/libs/warfare-0.2.0.jar ../server/mods/
```

La premiere compilation telecharge Gradle 8.8 et decompile Minecraft (~5 min). Les suivantes
prennent une vingtaine de secondes.

### 9.2 Preparer l'arene

Les positions sont stockees dans `server/world/data/warfare.dat` (elles survivent aux
redemarrages, mais pas a une suppression du monde).

1. Dans la console du serveur : `op TonPseudo`
2. En jeu, pour construire : `/gamemode creative`
3. Se placer dans la zone d'attente, regarder dans la bonne direction, puis `/warfare setlobby`
4. Aller sur chaque point d'apparition et faire `/warfare addspawn` — **au moins 2**, idealement
   6 a 8 bien repartis (le mod choisit celui le plus eloigne des ennemis vivants)
5. Verifier avec `/warfare listspawns`
6. `/gamemode adventure` pour revenir en mode de jeu

### 9.3 Commandes

| Commande | Effet |
|---|---|
| `/warfare start` | Lance le compte a rebours sans attendre le minimum de joueurs |
| `/warfare stop` | Coupe la partie en cours et renvoie tout le monde au lobby |
| `/warfare setlobby` | Definit le lobby a ta position et ton orientation |
| `/warfare addspawn` | Ajoute un point d'apparition a ta position |
| `/warfare listspawns` | Liste le lobby et les points d'apparition numerotes |
| `/warfare removespawn <n>` | Supprime le point d'apparition numero n |
| `/warfare reload` | Relit `config/warfare-common.toml` |

Toutes ces commandes demandent le niveau operateur (permission 2).

### 9.4 Deroulement d'une partie

1. **Lobby** — a la connexion : teleportation au lobby, mode aventure, invincible,
   inventaire vide. Message `En attente de joueurs... (1/2)` dans la barre d'action.
2. **Compte a rebours** — des que `minPlayers` est atteint : 30 s affiches en titre.
   Il s'annule si des joueurs partent.
3. **Partie** — chacun est teleporte sur un point d'apparition avec l'equipement par defaut.
   Scoreboard lateral : top 5, ta position si tu es hors du top 5, temps restant dans le titre.
   Chaque elimination est annoncee dans le chat avec l'arme utilisee et les tirs a la tete.
4. **Mort** — pas d'ecran de mort : mode spectateur pendant 3 s sur le lieu de la mort,
   puis reapparition avec 2 s d'invincibilite.
5. **Fin** — a 30 eliminations ou au bout de 10 minutes : titre du vainqueur, classement
   complet dans le chat, retour au lobby apres 10 s, et une nouvelle partie demarre
   automatiquement si assez de joueurs sont connectes.

### 9.5 Configuration — `server/config/warfare-common.toml`

| Cle | Defaut | Role |
|---|---|---|
| `match.minPlayers` | 2 | Joueurs necessaires pour lancer le compte a rebours |
| `match.countdownSeconds` | 30 | Duree du compte a rebours |
| `match.killLimit` | 30 | Eliminations qui mettent fin a la partie |
| `match.matchSeconds` | 600 | Duree maximale (10 min) |
| `match.respawnDelaySeconds` | 3 | Delai de reapparition |
| `match.spawnProtectionSeconds` | 2 | Invincibilite a la reapparition |
| `match.endScreenSeconds` | 10 | Duree de l'ecran de fin |
| `loadout.primaryGun` | `tacz:ak47` | Arme principale |
| `loadout.secondaryGun` | `tacz:glock_17` | Arme secondaire |
| `loadout.primaryMagazines` | 5 | Chargeurs de rechange (en plus de l'arme chargee) |
| `loadout.secondaryMagazines` | 4 | Idem pour le pistolet |
| `loadout.extraItems` | `[]` | Items en plus, ex. `["minecraft:golden_apple 1"]` |
| `display.showGunInKillfeed` | true | Nom de l'arme dans les annonces |
| `display.announceHeadshots` | true | Mention des tirs a la tete |
| `display.manageGameRules` | true | Laisse le mod imposer les gamerules ci-dessous |

Le calibre et la taille du chargeur ne sont **pas** a configurer : ils sont lus dans l'index
TaCZ de l'arme choisie. Changer `primaryGun` suffit, les munitions suivent.

Apres modification : `/warfare reload` (Forge recharge aussi le fichier tout seul a la volee).

### 9.6 Gamerules imposees par le mod

Au debut de chaque partie, si `display.manageGameRules = true` :

| Gamerule | Valeur | Pourquoi |
|---|---|---|
| `doImmediateRespawn` | true | Supprime l'ecran de mort cote client |
| `keepInventory` | true | Evite que les armes TaCZ jonchent le sol (le mod vide l'inventaire lui-meme) |
| `showDeathMessages` | false | Les annonces d'elimination sont celles du mod |

### 9.7 Integration TaCZ

Ce que le mod utilise reellement, verifie dans le jar TaCZ :

- **`com.tacz.guns.api.event.common.EntityKillByGunEvent`** — donne la victime, l'auteur,
  l'identifiant de l'arme et le tir a la tete.
  Point important : TaCZ ne poste cet evenement que si la victime est deja `isDeadOrDying()`
  apres le tir. **Annuler `LivingDeathEvent`** pour supprimer l'ecran de mort empecherait donc
  l'evenement de partir ; c'est pour cela que le mod laisse la mort vanilla se produire et
  utilise le gamerule `doImmediateRespawn`.
- **`com.tacz.guns.api.TimelessAPI.getCommonGunIndex(id)`** — calibre (`getAmmoId()`),
  taille du chargeur (`getAmmoAmount()`) et nom localise de l'arme.
- **`IGun` / `IAmmo`** — construction des ItemStacks (`tacz:modern_kinetic_gun` + NBT `GunId`,
  `tacz:ammo` + NBT `AmmoId`), arme livree chargee avec une balle en chambre.

### 9.8 Equipement par defaut

`loadout.primaryGun` / `secondaryGun` ne servent plus que de **valeur de repli** : depuis
l'etape 3, chaque joueur reapparait avec la classe qu'il a equipee (voir section 10).
Les valeurs de la config restent utilisees pour les classes jamais modifiees et quand
une arme enregistree n'est plus autorisee.

## 10. Creation de classe et atouts (etape 3)

### 10.1 Installer le mod dans Prism Launcher — obligatoire desormais

Le mod ajoute un ecran client : **il doit etre installe sur le client comme sur le serveur**,
dans la meme version. `displayTest` a ete retire de `mods.toml`, donc un client sans le mod
est refuse avec `Incompatible mod set`.

Methode recommandee (sure) :

1. Prism Launcher -> instance `Minecraft Warfare` -> **Edit**
2. Panneau **Mods** -> **Add file...**
3. Choisir `~/Documents/minecraft-warfare/mod/build/libs/warfare-0.3.0.jar`
4. Verifier que `warfare` et `tacz` apparaissent tous les deux, coches

Methode par le dossier, si tu preferes la ligne de commande :

```bash
cp ~/Documents/minecraft-warfare/mod/build/libs/warfare-0.3.0.jar \
   "$HOME/Library/Application Support/PrismLauncher/instances/Minecraft Warfare/minecraft/mods/"
```

Remplace `Minecraft Warfare` par le nom exact de ton instance. Selon son age, Prism utilise
`minecraft/` ou `.minecraft/` comme dossier de jeu — verifie lequel existe :

```bash
ls -d "$HOME/Library/Application Support/PrismLauncher/instances/"*/{minecraft,.minecraft} 2>/dev/null
```

**A chaque recompilation du mod, il faut remplacer le jar des deux cotes** (serveur et Prism),
sinon la connexion est refusee.

### 10.2 Ouvrir le menu

Touche **C** par defaut, remappable dans *Options -> Commandes -> categorie « Minecraft Warfare »*.

Le menu ne s'ouvre **qu'au lobby ou entre deux vies** (pendant les 3 s de reapparition).
En pleine partie, un message apparait dans la barre d'action a la place. Le serveur envoie ce
droit au client ; il ne se devine pas cote client.

### 10.3 Le menu

```
+---------+--------------------------------------------------+
| CREER UNE CLASSE                                       [X] |
+---------+--------------------------------------------------+
| > CL 1  | NOM  [Assaut__________]                          |
|   CL 2  |                                                  |
|   CL 3  | [ PRINCIPALE ] [ SECONDAIRE ]                     |
|   CL 4  | [Tout][Fusils][Mitraillettes][Pistolets]...       |
|   CL 5  | +-----------------------+  ATOUTS                 |
|         | | AK-47          Fusils |  [ Sprinteur        ]   |
|         | | M4A1           Fusils |  [ Visee eclair     ]   |
|         | | HK416D         Fusils |                         |
|         | | ...                   |  [ Puissance d'arret]   |
|         | +-----------------------+  [ Blinde           ]   |
|         |                            [ Pas de loup      ]   |
|         | [ENREGISTRER] [EQUIPER]    [ Cadence soutenue ]   |
+---------+--------------------------------------------------+
```

- **5 emplacements** par joueur, colonne de gauche. Le `>` marque la classe equipee.
- **NOM** : 16 caracteres maximum.
- **PRINCIPALE / SECONDAIRE** : choisit l'arme que la liste modifie. Survoler le bouton affiche
  l'arme actuellement choisie.
- Les boutons de **type** filtrent la liste. Les types viennent de TaCZ (`rifle`, `smg`,
  `sniper`, `pistol`, `shotgun`...), pas d'une liste ecrite en dur.
- **ATOUTS** : un seul atout par categorie, les deux options de chaque categorie sont
  exclusives. Survoler un atout affiche son effet exact.
- **ENREGISTRER** enregistre la classe. **EQUIPER** l'enregistre et la rend active ;
  elle est donnee **a la prochaine reapparition**, jamais en cours de vie.

Le menu est dessine entierement a l'aplat (aucune texture, aucun son, aucun visuel repris
d'un jeu existant) : fond sombre, liseres olive, accent ambre.

### 10.4 Les atouts

| Cat. | Atout | Effet | Branchement |
|---|---|---|---|
| 1 | **Sprinteur** | +10 % de vitesse de deplacement | Attribut vanilla `MOVEMENT_SPEED` (`MULTIPLY_TOTAL`) |
| 1 | **Visee eclair** | -30 % de temps de mise en joue | `GunProperties.ADS_TIME` via `AttachmentPropertyEvent` |
| 2 | **Puissance d'arret** | +20 % de degats par balle | `EntityHurtByGunEvent.Pre.setBaseAmount()` |
| 2 | **Blinde** | -30 % de degats d'explosion | `LivingHurtEvent` + tag `IS_EXPLOSION` |
| 3 | **Pas de loup** | Tes pas ne font aucun bruit pour les autres | `PlayLevelSoundEvent.AtPosition` annule |
| 3 | **Cadence soutenue** | +15 % de cadence de tir | `GunProperties.ROUNDS_PER_MINUTE` via `AttachmentPropertyEvent` |

Les atouts qui agissent sur l'arme passent par les proprietes TaCZ. Comme
`AttachmentPropertyEvent` ne fournit que l'ItemStack et pas son porteur, les armes distribuees
par le mod portent un tag NBT `WarfarePerks` qui liste les atouts de la classe. Les autres
atouts sont appliques au joueur au moment de la reapparition et retires au retour au lobby.

### 10.5 Liste blanche des armes

`config/warfare-common.toml` :

```toml
[loadout]
	# Liste vide = toutes les armes TaCZ chargees
	gunWhitelist = ["tacz:ak47", "tacz:m4a1", "tacz:hk416d", "tacz:glock_17", "tacz:deagle"]
```

Puis `/warfare reload`. La liste est envoyee au client, qui n'affiche que ces armes, **et**
revalidee a l'enregistrement : une arme hors liste est remplacee par celle de la config.

### 10.6 Stockage et validation

- Un fichier JSON par joueur : `server/world/warfare/classes/<uuid>.json`
- Charge a la connexion, reecrit a chaque enregistrement et a la deconnexion

```json
{
  "selected": 0,
  "classes": [
    { "name": "Assaut", "primary": "tacz:ak47", "secondary": "tacz:glock_17",
      "perks": ["sprinteur", "puissance_arret", "cadence"] }
  ]
}
```

Rien de ce qui vient du client n'est repris tel quel. `ClassStore.sanitize` borne le nom a
16 caracteres et retire les codes de couleur et les caracteres de controle, verifie chaque arme
contre le catalogue TaCZ et la liste blanche, et force un atout valide par categorie. Un client
modifie qui envoie `tacz:m107` alors qu'il est hors liste blanche se retrouve avec l'arme de la
config, pas avec le M107.

### 10.7 Deux atouts demandes n'ont pas pu etre branches sur TaCZ

Verifie dans `tacz-1.20.1-1.1.8-hotfix2.jar`, pas devine :

- **« Mains rapides » (rechargement +30 %)** — `com.tacz.guns.api.GunProperties` contient 17
  proprietes modifiables (degats, cadence, recul, visee, portee, silence, poids...) mais
  **aucune propriete de rechargement**. Le seul autre point d'entree serait
  `ShooterDataHolder.reloadTimestamp`, or `GunReloadEvent` est poste **avant** que
  `LivingEntityReload` n'ecrive ce champ : toute valeur qu'on y mettrait serait ecrasee.
  Il faudrait un Mixin dans `LivingEntityReload.tickReloadState()`, applique a l'identique
  sur le client et le serveur pour que l'animation reste synchrone.
  **Remplace par « Visee eclair »** (-30 % de temps de mise en joue), meme categorie,
  meme logique de maniement, via l'API supportee.

- **« Commando » (portee au corps a corps augmentee)** — `GunMeleeData.getDistance()` est lu
  directement dans les donnees de l'arme a chaque coup, sans modificateur ni evenement.
  `GunMeleeEvent` est annulable mais ne porte aucune distance : il faudrait annuler le corps
  a corps de TaCZ et reimplementer son raycast, ses degats, son recul, son temps de recharge
  et son animation.
  **Remplace par « Cadence soutenue »** (+15 % de cadence de tir), qui garde l'esprit
  « agressif au contact » face a l'option furtive de la meme categorie.

Si tu veux les deux atouts d'origine, c'est faisable avec des Mixins dans TaCZ — dis-le-moi
et je le fais dans une etape dediee, en sachant que ca casse potentiellement a chaque mise
a jour de TaCZ.

## 11. Checklist de tests en jeu

### 11.1 Serveur et client

- [ ] `./start.sh` -> la console affiche `Done (XX.XXXs)!`
- [ ] La console affiche `Warfare pret : lobby=... points d'apparition=...`
- [ ] Prism Launcher -> instance `Minecraft Warfare` se lance sans erreur
- [ ] Multijoueur -> le serveur `localhost` repond, MOTD **Minecraft Warfare**
- [ ] Connexion OK, aucun kick `Incompatible mod set`
- [ ] Console serveur : `op TonPseudo`

### 11.2 Armes TaCZ

- [ ] `/gamemode creative`
- [ ] `/give TonPseudo tacz:modern_kinetic_gun{GunId:"tacz:ak47"}` -> l'AK-47 apparait avec son modele 3D
- [ ] `/give TonPseudo tacz:ammo{AmmoId:"tacz:762x39"} 64` -> munitions en inventaire
- [ ] Touches TaCZ : *Options -> Commandes -> categorie « Timeless & Classics Guns: Zero »*
- [ ] Recharger, viser, tirer : son + animation + impacts visibles

### 11.3 Arene

- [ ] `/warfare setlobby` -> message de confirmation avec les coordonnees
- [ ] `/warfare addspawn` sur au moins 2 points eloignes -> `#1`, `#2`...
- [ ] `/warfare listspawns` -> le lobby et tous les points sont listes
- [ ] `/warfare removespawn 2` puis `/warfare listspawns` -> le point a disparu
- [ ] Redemarrer le serveur -> `/warfare listspawns` affiche toujours tout

### 11.4 Partie (il faut 2 joueurs, ou baisser `minPlayers` a 1 puis `/warfare reload`)

- [ ] A la connexion : teleportation au lobby, mode aventure, inventaire vide
- [ ] Se taper dessus au lobby -> aucun degat
- [ ] Barre d'action : `En attente de joueurs... (1/2)`
- [ ] Au 2e joueur : compte a rebours 30 s en gros titre
- [ ] `/warfare start` force le demarrage sans attendre
- [ ] Au debut : titre `A L'ATTAQUE`, teleportation sur un point d'apparition
- [ ] Inventaire : AK-47 chargee en slot 1, Glock en slot 2, munitions des deux calibres
- [ ] Scoreboard lateral visible : titre `WARFARE 9:59` qui descend, classement en dessous
- [ ] Tuer l'autre joueur -> chat `X a elimine Y [AK-47]`, le score monte dans le scoreboard
- [ ] Tir a la tete -> mention `TETE` dans l'annonce
- [ ] A la mort : **aucun ecran de mort**, vue spectateur ~3 s, puis reapparition equipee
- [ ] Reapparition loin de l'adversaire (tester a 2 avec des spawns opposes)
- [ ] Invincible ~2 s apres la reapparition
- [ ] Baisser `killLimit` a 2 -> la partie se termine : titre vainqueur + classement dans le chat
- [ ] Retour automatique au lobby apres 10 s, puis nouveau compte a rebours
- [ ] `/warfare stop` en pleine partie -> tout le monde revient au lobby
- [ ] Console serveur : aucune ligne `ERROR` pendant toute la session

### 11.5 Classes et atouts

- [ ] Le jar `warfare-0.3.0.jar` est dans `server/mods/` **et** dans l'instance Prism
- [ ] Connexion sans `Incompatible mod set`
- [ ] *Options -> Commandes* : la categorie « Minecraft Warfare » existe, touche **C**
- [ ] Au lobby, **C** ouvre le menu « Creer une classe »
- [ ] En pleine partie vivant, **C** affiche le refus dans la barre d'action
- [ ] Pendant les 3 s de reapparition, **C** ouvre bien le menu
- [ ] La liste d'armes contient les armes TaCZ, les filtres par type fonctionnent
- [ ] La molette fait defiler la liste, la barre de defilement suit
- [ ] Choisir une arme principale et une secondaire, 3 atouts, nommer la classe
- [ ] **ENREGISTRER** -> message `Classe "..." enregistree` dans le chat
- [ ] **EQUIPER** -> message `... equipee a la prochaine reapparition`
- [ ] Mourir -> reapparaitre avec les bonnes armes, message `Classe : <nom>`
- [ ] Changer d'emplacement (CL 1 -> CL 2), modifier, revenir : les deux sont conservees
- [ ] Se deconnecter / reconnecter -> les 5 classes sont toujours la
- [ ] `server/world/warfare/classes/<ton-uuid>.json` existe et contient tes classes
- [ ] **Sprinteur** : se deplacer, comparer avec une classe sans l'atout
- [ ] **Visee eclair** : mise en joue nettement plus rapide
- [ ] **Cadence soutenue** : cadence plus elevee en automatique
- [ ] **Puissance d'arret** : moins de balles pour tuer (tester a 2)
- [ ] **Blinde** : survivre a une explosion qui tuerait sans l'atout
- [ ] **Pas de loup** : un autre joueur ne doit plus entendre tes pas
- [ ] Mettre `gunWhitelist = ["tacz:ak47"]` + `/warfare reload` -> le menu ne propose plus qu'elle
- [ ] Console serveur : aucune ligne `ERROR` pendant toute la session
