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

## 9. Checklist de tests en jeu

- [ ] `./start.sh` → la console affiche `Done (XX.XXXs)!`
- [ ] Prism Launcher → instance `Minecraft Warfare` se lance sans erreur
- [ ] Multijoueur → le serveur `localhost` répond, MOTD **Minecraft Warfare**
- [ ] Connexion OK, aucun kick `Incompatible mod set`
- [ ] Console serveur : `op TonPseudo`, puis en jeu `/gamemode creative` (test libre)
- [ ] `/give TonPseudo tacz:modern_kinetic_gun{GunId:"tacz:ak47"}` → l'AK-47 apparaît avec son modèle 3D
- [ ] `/give TonPseudo tacz:ammo{AmmoId:"tacz:762x39"} 64` → munitions en inventaire
- [ ] Touches TaCZ : *Options → Commandes → catégorie « Timeless & Classics Guns: Zero »*
      (vérifier Shoot / Aim / Reload / Fire Select)
- [ ] Recharger, viser, tirer : son + animation + impacts visibles
- [ ] Console serveur : aucune ligne `ERROR` pendant le tir
- [ ] Repasser en `/gamemode adventure` → les blocs ne se cassent plus, le tir fonctionne toujours
- [ ] `stop` dans la console → `All dimensions are saved`
