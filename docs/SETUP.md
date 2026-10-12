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
3. Choisir `~/Documents/minecraft-warfare/mod/build/libs/warfare-0.4.0.jar`
4. Verifier que `warfare` et `tacz` apparaissent tous les deux, coches

Methode par le dossier, si tu preferes la ligne de commande :

```bash
cp ~/Documents/minecraft-warfare/mod/build/libs/warfare-0.4.0.jar \
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

## 11. Killstreaks (etape 4)

### 11.1 Principe

Un compteur d'**eliminations consecutives sans mourir** par joueur. Il repart a zero a chaque
mort. Par defaut, **les eliminations faites avec une killstreak ne font pas monter la serie** :
une frappe aerienne qui tue 4 personnes ne debloque pas la killstreak suivante.
Reglable avec `killstreaks.streakKillsCountTowardStreak`.

La recompense est accordee quand le compteur atteint **exactement** le seuil d'une des
3 killstreaks equipees sur la classe en cours.

### 11.2 Les cinq killstreaks

| Seuil | Nom | Objet | Effet | Comment c'est fait |
|---|---|---|---|---|
| 3 | **Drone de reperage** | Longue-vue | Ennemis en surbrillance 15 s, **visible par toi seul** | Paquet `ClientboundSetEntityDataPacket` envoye au seul proprietaire |
| 4 | **Ravitaillement** | Coffre | Chargeurs pleins sur toutes tes armes + 1 bonus aleatoire | API TaCZ `IGun.setCurrentAmmoCount` + effet vanilla |
| 5 | **Frappe aerienne** | Fusee | Tu designes un point, salve de 9 explosions 5 s plus tard | Raycast puis `Level.explode(..., NONE)` |
| 7 | **Tourelle sentinelle** | Distributeur | Tourelle posee, tire 30 s sur 20 blocs | Porte-armure + degats directs credites a toi |
| 11 | **Bombardement** | TNT | 30 frappes sur toute l'arene en 10 s | Boite englobante des points d'apparition |

Les 5 bonus possibles du ravitaillement : Absorption, Regeneration, Vitesse, Force, Resistance.
Le nom du bonus tire est annonce dans ton chat.

La zone du bombardement est la **boite englobante de tes points d'apparition**, elargie de
`carpetMargin` blocs : c'est donc litteralement l'arene que tu as definie, pas un rayon
arbitraire. S'il n'y a aucun point d'apparition enregistre, la zone retombe sur un carre
autour de l'auteur.

### 11.3 L'objet-recompense

La recompense arrive dans le **dernier emplacement de la barre d'action** (touche 9), reserve :
l'equipement de depart deplace tout ce qui y aurait atterri vers l'inventaire. **Clic droit**
pour declencher. L'objet est consomme a l'usage.

Si tu debloques une deuxieme killstreak avant d'avoir utilise la premiere, elle attend en file
et descend dans l'emplacement des qu'il se libere. Mourir vide la file **et** l'emplacement :
une recompense non utilisee est perdue, comme le compteur.

Les objets utilises sont des items vanilla (coffre, distributeur, TNT) : le clic droit est
intercepte et annule, **rien n'est jamais pose**.

### 11.4 Annonces et sons

| Moment | Effet |
|---|---|
| Killstreak debloquee | Titre `<nom>` / `disponible` + message chat + `entity.player.levelup` |
| Frappe designee | Message prive avec les coordonnees et le compte a rebours, `item.elytra.flying` sur la zone |
| Killstreak utilisee | Message a tous : `X a declenche <nom>` |
| Bombardement | Titre `BOMBARDEMENT` / `Mettez-vous a couvert` a tous + `entity.lightning_bolt.thunder` |
| Tir de tourelle | `entity.arrow.shoot` + particules de trace |
| Drone | `item.elytra.flying` aigu |

Tous les sons sont des sons vanilla de Minecraft.

### 11.5 Les explosions ne cassent jamais de blocs

Toutes les explosions du mod passent par
`Level.explode(auteur, x, y, z, puissance, Level.ExplosionInteraction.NONE)`.
`NONE` supprime entierement la phase de destruction de blocs ; seuls les degats aux entites
sont appliques. L'auteur est passe comme source de l'explosion, donc **les eliminations lui
sont bien creditees** (et l'atout *Blinde* reduit bien ces degats de 30 %).

### 11.6 Configuration — section `[killstreaks]`

```toml
[killstreaks]
	enabled = true
	# false = seules les eliminations a l'arme font monter la serie
	streakKillsCountTowardStreak = false
	droneThreshold = 3
	droneSeconds = 15
	resupplyThreshold = 4
	airstrikeThreshold = 5
	airstrikeDelaySeconds = 5
	airstrikeBombs = 9
	airstrikeSpread = 8          # rayon de dispersion de la salve, en blocs
	airstrikePower = 3.0         # 4.0 = une TNT
	turretThreshold = 7
	turretSeconds = 30
	turretRange = 20
	turretIntervalTicks = 15     # 20 ticks = 1 s
	turretDamage = 4.0
	carpetThreshold = 11
	carpetSeconds = 10
	carpetBombs = 30
	carpetMargin = 20            # marge autour de la zone des spawns
	carpetPower = 3.0
```

Les seuils affiches dans le menu suivent la config : changer `airstrikeThreshold` a 6 et faire
`/warfare reload` met a jour le libelle dans l'onglet KILLSTREAKS.

### 11.7 Le drone est bien visible par toi seul

Tu demandais de te prevenir si ce n'etait pas faisable proprement. **Ca l'est** : au lieu de
poser l'effet vanilla *Luminescence* sur les ennemis (qui serait visible par tout le monde), le
mod renvoie au seul proprietaire du drone les drapeaux synchronises de chaque ennemi avec le bit
de surbrillance (`0x40`) force, via `ClientboundSetEntityDataPacket`. Le serveur ne modifie
aucune entite : les autres joueurs ne voient strictement rien.

Seule subtilite : si le serveur envoie ensuite une mise a jour legitime de ces drapeaux (sprint,
accroupissement...), le client ecrase la surbrillance. Le mod la reaffirme donc **toutes les
10 ticks** tant que le drone est actif. En pratique ca peut donner un tres bref clignotement
quand un ennemi se met a sprinter ; dis-moi si tu le vois et je descends l'intervalle.

### 11.8 Onglet KILLSTREAKS du menu de classe

Le menu a maintenant trois onglets : **ARMES**, **ATOUTS**, **KILLSTREAKS**. L'onglet
killstreaks liste les 5 recompenses triees par seuil, avec leur description a droite. Clic pour
cocher ou decocher. Au-dela de 3, la plus anciennement cochee sort automatiquement. Le compteur
`selection : 3/3` passe en ambre quand la selection est complete.

## 12. Checklist de tests en jeu

### 12.1 Serveur et client

- [ ] `./start.sh` -> la console affiche `Done (XX.XXXs)!`
- [ ] La console affiche `Warfare pret : lobby=... points d'apparition=...`
- [ ] Prism Launcher -> instance `Minecraft Warfare` se lance sans erreur
- [ ] Multijoueur -> le serveur `localhost` repond, MOTD **Minecraft Warfare**
- [ ] Connexion OK, aucun kick `Incompatible mod set`
- [ ] Console serveur : `op TonPseudo`

### 12.2 Armes TaCZ

- [ ] `/gamemode creative`
- [ ] `/give TonPseudo tacz:modern_kinetic_gun{GunId:"tacz:ak47"}` -> l'AK-47 apparait avec son modele 3D
- [ ] `/give TonPseudo tacz:ammo{AmmoId:"tacz:762x39"} 64` -> munitions en inventaire
- [ ] Touches TaCZ : *Options -> Commandes -> categorie « Timeless & Classics Guns: Zero »*
- [ ] Recharger, viser, tirer : son + animation + impacts visibles

### 12.3 Arene

- [ ] `/warfare setlobby` -> message de confirmation avec les coordonnees
- [ ] `/warfare addspawn` sur au moins 2 points eloignes -> `#1`, `#2`...
- [ ] `/warfare listspawns` -> le lobby et tous les points sont listes
- [ ] `/warfare removespawn 2` puis `/warfare listspawns` -> le point a disparu
- [ ] Redemarrer le serveur -> `/warfare listspawns` affiche toujours tout

### 12.4 Partie (il faut 2 joueurs, ou baisser `minPlayers` a 1 puis `/warfare reload`)

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

### 12.5 Classes et atouts

- [ ] Le jar `warfare-0.4.0.jar` est dans `server/mods/` **et** dans l'instance Prism
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

### 12.6 Killstreaks

- [ ] Onglet **KILLSTREAKS** : les 5 recompenses sont listees avec leurs seuils
- [ ] Cocher 3 killstreaks, le compteur affiche `3/3` en ambre
- [ ] Cocher une 4e -> la plus anciennement cochee se decoche
- [ ] **ENREGISTRER** puis se reconnecter -> la selection est conservee
- [ ] Faire 3 eliminations d'affilee -> titre `Drone de reperage` / `disponible` + son
- [ ] L'objet apparait bien dans le **dernier emplacement** de la barre (touche 9)
- [ ] Clic droit -> message a tous `X a declenche Drone de reperage`
- [ ] **Drone** : les ennemis sont en surbrillance pour toi ; demander a l'autre joueur de
      confirmer qu'il **ne voit rien** (c'est le point a verifier en priorite)
- [ ] La surbrillance s'arrete au bout de 15 s avec le message `Drone hors service`
- [ ] **Ravitaillement** (4) : chargeurs pleins + message indiquant le bonus tire
- [ ] **Frappe aerienne** (5) : viser un point au loin, clic droit -> message avec les
      coordonnees, puis salve d'explosions 5 s plus tard a l'endroit vise
- [ ] Une elimination par la frappe est bien creditee (score + killfeed)
- [ ] Une elimination par la frappe **ne fait pas monter** la serie
- [ ] **Tourelle** (7) : viser un bloc proche, clic droit -> la tourelle apparait, nommee,
      et tire sur l'ennemi qui s'approche a moins de 20 blocs
- [ ] La tourelle disparait apres 30 s
- [ ] **Bombardement** (11) : titre `BOMBARDEMENT` pour tout le monde + frappes sur toute l'arene
- [ ] **Aucun bloc casse** apres toutes ces explosions (verifier le terrain de l'arene)
- [ ] Clic droit avec un objet de killstreak sur un bloc -> **rien n'est pose**
- [ ] Mourir -> le compteur repart a zero, l'objet non utilise disparait
- [ ] `/warfare stop` en pleine partie -> les tourelles posees disparaissent
- [ ] Changer `droneThreshold` a 2 + `/warfare reload` -> le menu affiche `2` pour le drone
- [ ] Console serveur : aucune ligne `ERROR` pendant toute la session

## 13. Distribution du pack avec packwiz (etape 5)

### 13.1 packwiz est installe dans `tools/`

packwiz n'a **aucune release taguee** et n'est pas dans Homebrew. Les deux voies officielles
sont les binaires de CI et la compilation depuis les sources.

Le binaire installe ici vient de la source officielle indiquee par le README de packwiz
(`nightly.link/packwiz/packwiz/workflows/go/main`). **Il n'existe qu'en x86_64** : sur ton Mac
Apple Silicon il tourne via Rosetta, ce qui fonctionne et n'a demande aucune installation.

```bash
./tools/packwiz list        # verifier que packwiz repond
```

Si un jour Rosetta pose probleme, la version native se compile en deux commandes :

```bash
brew install go
go install github.com/packwiz/packwiz@latest   # binaire dans ~/go/bin/packwiz
```

`tools/packwiz` est ignore par git (12 Mo de binaire) : sur une autre machine, retelecharge-le.

### 13.2 Ce que contient `pack/`

```
pack/
├── pack.toml                              # Minecraft 1.20.1 + Forge 47.4.10
├── index.toml                             # liste et empreintes de tous les fichiers
├── README.md
└── mods/
    ├── timeless-and-classics-zero.pw.toml  # metafichier -> telecharge depuis Modrinth
    ├── embeddium.pw.toml                   # metafichier
    ├── ferrite-core.pw.toml                # metafichier
    ├── modernfix.pw.toml                   # metafichier
    └── warfare-0.4.0.jar                   # fichier reel, servi par ton depot GitHub
```

Deux sortes d'entrees :

- un **metafichier** `.pw.toml` ne contient qu'un identifiant Modrinth, une URL et une
  empreinte. Le jar n'est jamais dans ton depot : c'est le client qui le prend chez Modrinth.
- le mod `warfare` n'est publie nulle part, donc son **jar est dans le depot** et les clients
  le telechargent depuis GitHub. C'est la facon normale de distribuer un mod prive avec packwiz.

| Mod | Version | Role |
|---|---|---|
| TaCZ | 1.1.8-hotfix2 (epinglee) | armes |
| warfare | 0.4.0 | le mode de jeu |
| Embeddium | 0.3.31+mc1.20.1 | FPS (le Sodium de Forge) |
| FerriteCore | 6.0.1-forge | moins de RAM |
| ModernFix | 5.27.88+mc1.20.1 | demarrage plus rapide |

**Pas de shaders.** Oculus provoque des crashs documentes en tenant une arme TaCZ
(MCModderAnchor/TACZ #250, Asek3/Oculus #678). Embeddium seul est la configuration avec
laquelle l'addon de performance officiel de TaCZ publie ses propres benchmarks.

### 13.3 Creer le depot GitHub public

`pack/` est **son propre depot git**, separe du projet principal (qui l'ignore). Comme ca le
depot public ne contient que le pack, et l'URL du pack reste courte.

Le depot local est deja initialise et le premier commit est fait. Il reste a le mettre en ligne.

1. Va sur [github.com/new](https://github.com/new)
2. **Repository name** : `minecraft-warfare-pack`
3. **Public** (obligatoire : les clients telechargent sans authentification)
4. **Ne coche rien** — pas de README, pas de .gitignore, pas de licence : le dossier en a deja un
5. **Create repository**
6. Puis, dans un terminal :

```bash
cd ~/Documents/minecraft-warfare/pack
# le remote est deja configure
git push -u origin main
```

GitHub te demandera un identifiant : utilise un **personal access token** comme mot de passe
(Settings → Developer settings → Tokens), pas ton mot de passe de compte.

L'URL du pack devient alors :

```
https://raw.githubusercontent.com/gooldenpro-dotcom/minecraft-warfare-pack/main/pack.toml
```

Verifie-la dans un navigateur : tu dois voir le contenu de `pack.toml` en texte brut.
Si tu vois une page GitHub avec des boutons, c'est que tu as pris l'URL `github.com`
au lieu de `raw.githubusercontent.com`.

### 13.4 L'instance Prism a envoyer a tes amis

```bash
./tools/build-prism-instance.sh gooldenpro-dotcom minecraft-warfare-pack
```

Produit `dist/MinecraftWarfare-Prism.zip`, deja genere avec ton URL. A relancer seulement si
tu changes de pseudo ou de nom de depot.

Le zip contient :

```
instance.cfg                              # dont la commande de pre-lancement
mmc-pack.json                             # Minecraft 1.20.1 + Forge 47.4.10
.minecraft/packwiz-installer-bootstrap.jar
```

La ligne qui fait tout le travail, dans `instance.cfg` :

```
OverrideCommands=true
PreLaunchCommand="$INST_JAVA" -jar packwiz-installer-bootstrap.jar "<URL du pack>"
```

Prism execute cette commande **avant chaque demarrage**, avec `.minecraft` comme dossier
courant. Le bootstrap recupere packwiz-installer, qui compare `index.toml` aux fichiers
locaux et ne telecharge que ce qui a change. Tes amis n'ont donc jamais a reinstaller quoi
que ce soit : ils relancent, c'est a jour.

Tu peux aussi corriger l'URL sans regenerer le zip : dans Prism, instance → **Edit** →
**Settings** → **Custom commands**.

### 13.5 Le .mrpack

```bash
cd pack && ../tools/packwiz modrinth export -o ../dist/MinecraftWarfare-1.0.0.mrpack
```

`dist/MinecraftWarfare-1.0.0.mrpack` est deja genere. Il s'importe dans Prism, l'app Modrinth
et ATLauncher. Les 4 mods Modrinth sont dans le manifeste, le mod `warfare` est embarque
dans `overrides/`.

**Difference importante avec l'instance Prism** : le .mrpack est un **instantane**. Il ne se
met pas a jour tout seul ; il faut en renvoyer un nouveau a chaque changement. Donne-le en
secours, a quelqu'un qui n'utilise pas Prism par exemple, et privilegie l'instance pour
tout le monde.

### 13.6 Publier une mise a jour

Apres avoir recompile le mod :

```bash
cd ~/Documents/minecraft-warfare
(cd mod && ./gradlew build)
cp mod/build/libs/warfare-X.Y.Z.jar server/mods/        # cote serveur
rm -f server/mods/warfare-<ancienne>.jar

rm -f pack/mods/warfare-*.jar                           # cote pack
cp mod/build/libs/warfare-X.Y.Z.jar pack/mods/
(cd pack && ../tools/packwiz refresh)
(cd pack && git add -A && git commit -m "pack: warfare X.Y.Z" && git push)
```

`packwiz refresh` est **obligatoire** : c'est lui qui recalcule `index.toml`, et c'est l'index
que lisent les clients. Sans refresh, tes amis gardent l'ancien jar et se font refuser avec
`Incompatible mod set`.

Mettre a jour les mods Modrinth :

```bash
cd pack
../tools/packwiz update --all     # ou: ../tools/packwiz update embeddium
git add -A && git commit -m "pack: mise a jour des mods" && git push
```

Attention : ne mets **jamais** a jour TaCZ dans le pack sans mettre a jour le serveur en
meme temps, et sans recompiler `warfare` contre la nouvelle version (`mod/libs/`).

## 14. Rendre le serveur accessible a tes amis

**Je n'ai rien touche sur ton reseau.** Les deux options ci-dessous sont a faire par toi.

Rappel : `server.properties` a `server-ip=` vide, donc le serveur ecoute deja sur toutes les
interfaces de ta machine, et `server-port=25565`. Au premier lancement, macOS peut demander
d'autoriser Java a accepter les connexions entrantes : il faut accepter.

### 14.1 Option A — redirection de port sur ta box

La plus rapide et la meilleure en latence, mais elle expose un port de ta machine a Internet.

1. **Fixer l'adresse locale du Mac.** Trouve-la :

   ```bash
   ipconfig getifaddr en0 || ipconfig getifaddr en1
   ```

   Puis, dans l'interface de ta box, reserve cette adresse pour ton Mac (bail DHCP statique) —
   sinon elle changera et la redirection pointera dans le vide.

2. **Creer la redirection.** Interface d'administration de la box (souvent `http://192.168.1.1`,
   section « NAT/PAT », « Redirection de ports » ou « Port forwarding ») :

   | Champ | Valeur |
   |---|---|
   | Port externe | `25565` |
   | Port interne | `25565` |
   | Protocole | **TCP** |
   | IP de destination | l'adresse locale de ton Mac |

3. **Trouver ton adresse publique** : `curl -s https://api.ipify.org`
   C'est cette adresse que tu donnes a tes amis.

4. **Verifier depuis l'exterieur** : [portchecktool.com](https://www.portchecktool.com/) sur le
   port 25565, serveur demarre. Un test depuis ton propre reseau ne prouve rien.

Les limites a connaitre :

- **CGNAT** : beaucoup d'abonnements fibre et toutes les box 4G/5G partagent une adresse
  publique entre plusieurs clients. Dans ce cas la redirection ne peut pas fonctionner, quoi
  que tu configures. Si l'adresse publique de l'etape 3 ne correspond pas a celle affichee
  dans l'interface de ta box, tu es derriere un CGNAT → passe a l'option B.
- **Adresse dynamique** : la plupart des FAI changent l'adresse publique regulierement.
  Tes amis devront la remettre a jour, sauf si tu utilises un DNS dynamique (DuckDNS, No-IP).
- **Exposition** : n'importe qui scannant Internet trouvera le port. `online-mode=true` est
  deja actif, donc seuls des comptes Minecraft authentifies peuvent se connecter — c'est la
  protection qui compte. Tu peux aussi passer `white-list=true` et ajouter tes amis avec
  `whitelist add <pseudo>` dans la console.
- Ton adresse IP publique devient visible de tes amis.

### 14.2 Option B — playit.gg

Un tunnel : un agent sur ta machine ouvre une connexion **sortante** vers playit.gg, qui te
donne une adresse publique. **Aucune configuration de box, fonctionne derriere un CGNAT.**
L'offre gratuite suffit pour un serveur Minecraft entre amis.

1. Cree un compte sur [playit.gg](https://playit.gg/)
2. Installe l'agent : `brew install --cask playit` (ou le `.pkg` depuis leur site)
3. Lance-le, il affiche un lien a ouvrir pour associer l'agent a ton compte
4. Dans leur interface web : **Add Tunnel** → **Minecraft Java** → local address
   `127.0.0.1` port `25565`
5. playit te donne une adresse du type `warfare-xxxx.gl.joinmc.link` : c'est elle que tes amis
   mettent dans Minecraft

Les limites a connaitre :

- **Latence** : le trafic fait un detour par un relais playit. Compte quelques dizaines de ms
  en plus. Choisis la region la plus proche dans leur interface.
- **Dependance** : si playit.gg est en panne, personne ne se connecte.
- L'agent doit tourner en meme temps que ton serveur.

### 14.3 Lequel choisir

Commence par **playit.gg**. Rien a configurer, rien d'expose, et ca marche meme derriere un
CGNAT — tu sauras en 5 minutes si ca suffit. Si la latence te gene en jeu, bascule sur la
redirection de port, qui sera toujours meilleure.

## 15. Checklist de distribution

- [ ] `./tools/packwiz list` repond (4 mods Modrinth listes)
- [ ] Depot GitHub public `minecraft-warfare-pack` cree, `git push` passe
- [ ] L'URL `https://raw.githubusercontent.com/<pseudo>/minecraft-warfare-pack/main/pack.toml`
      affiche du texte brut dans un navigateur
- [ ] `mods/warfare-0.4.0.jar` est bien visible sur GitHub (c'est le seul jar du depot)
- [ ] L'instance « Minecraft Warfare » apparait dans Prism (deja placee dans son dossier)
      et se lance une fois le depot publie
- [ ] Au lancement, la fenetre de packwiz-installer apparait et les 5 mods arrivent dans
      `.minecraft/mods/`
- [ ] Le jeu se lance, le menu « Creer une classe » repond a **C**
- [ ] Connexion a `localhost` depuis cette instance importee : aucun `Incompatible mod set`
- [ ] Modifier une virgule dans le pack, `packwiz refresh`, `git push`, relancer l'instance :
      le fichier se met a jour tout seul
- [ ] Rendre le serveur accessible (section 14), puis demander a **un** ami de tester avant
      d'envoyer le zip a tout le monde
- [ ] `dist/MinecraftWarfare-1.0.0.mrpack` s'importe aussi dans Prism (secours)
