# Minecraft Warfare — guide d'installation

Salut ! Ce guide t'installe tout ce qu'il faut pour jouer sur **Minecraft Warfare**,
un serveur Minecraft moddé en mode « chacun pour soi » avec de vraies armes à feu.

Compte **15 minutes** la première fois. Après, tu cliques juste sur « Launch ».

Il te faut un **compte Minecraft Java Edition payant** (le serveur vérifie les comptes).
Minecraft Bedrock — la version Microsoft Store, console ou téléphone — ne marche pas.

---

## 1. Installer Java 17

Minecraft 1.20.1 moddé a besoin de Java 17.

- **Windows et macOS** : télécharge-le sur [adoptium.net](https://adoptium.net/temurin/releases/?version=17)
  - Version : **17 (LTS)**, Package type : **JDK**, puis l'installeur `.msi` (Windows) ou `.pkg` (macOS)
  - Sur macOS, prends **aarch64** si ton Mac est en Apple Silicon (M1/M2/M3/M4), **x64** sinon
- **Linux** : `sudo apt install openjdk-17-jdk` (ou l'équivalent de ta distribution)

Installe-le en laissant toutes les options par défaut.

## 2. Installer Prism Launcher

[prismlauncher.org/download](https://prismlauncher.org/download/) — c'est gratuit et open source.

Au premier lancement, Prism te demande de te connecter à ton compte Minecraft :
**Add Account** → **Microsoft** → suis les instructions dans ton navigateur.

Si Prism te demande où est Java, pointe vers le Java 17 de l'étape 1 (il le trouve
généralement tout seul).

## 3. Importer l'instance

Clément t'a envoyé un fichier **`MinecraftWarfare-Prism.zip`**.

1. Dans Prism : **Add Instance** (en haut à gauche)
2. Onglet **Import** → **Browse** → choisis le `.zip`
3. **OK**

Une instance « Minecraft Warfare » apparaît dans la liste. **Ne touche à rien d'autre** :
les mods ne sont pas encore là, c'est normal.

## 4. Premier lancement

Double-clique sur l'instance, ou sélectionne-la et clique **Launch**.

Au démarrage, une fenêtre apparaît brièvement : c'est **packwiz-installer** qui télécharge
les mods. La première fois ça prend une minute ou deux (le mod d'armes pèse 55 Mo).

**C'est automatique et ça se refait à chaque lancement.** Quand Clément met le serveur à
jour, tu n'as rien à faire : relance l'instance et tes mods se mettent à jour tout seuls.

Minecraft se lance ensuite normalement. Si tu vois le menu principal de Minecraft, c'est gagné.

## 5. Se connecter au serveur

1. **Multijoueur**
2. **Ajouter un serveur**
3. Nom du serveur : `Minecraft Warfare`
4. Adresse du serveur : **l'adresse que Clément t'a donnée**
   (quelque chose comme `warfare-xxxx.gl.joinmc.link` ou une adresse en chiffres)
5. **Terminé**, puis double-clique sur le serveur

Si le serveur apparaît en rouge avec « Connexion impossible », c'est que Clément ne l'a pas
démarré. Demande-lui.

---

## Comment on joue

Tu arrives dans une **zone d'attente** : tu ne peux ni casser de blocs ni te faire tuer.
Dès qu'il y a assez de joueurs, un **compte à rebours de 30 secondes** s'affiche, puis la
partie démarre. Chacun pour soi, **premier à 30 éliminations** ou le meilleur score au bout
de **10 minutes**.

Quand tu meurs, pas d'écran de mort : 3 secondes de vue aérienne et tu réapparais équipé,
loin des autres.

### Les touches

| Touche | Effet |
|---|---|
| **C** | Ouvre le menu « Créer une classe » (seulement dans la zone d'attente ou juste après ta mort) |
| **R** | Recharger |
| **Clic droit** | Viser |
| **Clic gauche** | Tirer |
| **9** | Sélectionne l'emplacement de récompense (killstreaks) |

Les touches des armes sont réglables dans *Options → Commandes → catégorie
« Timeless & Classics Guns: Zero »*. La touche du menu de classe est dans la catégorie
« Minecraft Warfare ».

### Créer une classe

Appuie sur **C** dans la zone d'attente. Tu as **5 emplacements** de classe, et pour chacun :

- un **nom**
- une **arme principale** et une **arme secondaire** (onglet ARMES, filtres par type, molette pour défiler)
- **3 atouts**, un par catégorie (onglet ATOUTS) :
  - Sprinteur (+10 % de vitesse) **ou** Visée éclair (-30 % de temps de visée)
  - Puissance d'arrêt (+20 % de dégâts) **ou** Blindé (-30 % de dégâts d'explosion)
  - Pas de loup (tes pas ne font aucun bruit) **ou** Cadence soutenue (+15 % de cadence)
- **3 killstreaks** parmi 5 (onglet KILLSTREAKS)

**ENREGISTRER** sauvegarde la classe, **ÉQUIPER** la rend active. Elle te sera donnée
**à ta prochaine réapparition**, pas tout de suite.

### Les killstreaks

Des récompenses pour tes éliminations **d'affilée sans mourir**. Mourir remet le compteur
à zéro. Quand tu en débloques une, elle arrive comme objet dans le **dernier emplacement de
ta barre** (touche **9**) : **clic droit** pour l'utiliser.

| Éliminations | Récompense |
|---|---|
| 3 | **Drone de repérage** — les ennemis sont en surbrillance pour toi pendant 15 s |
| 4 | **Ravitaillement** — chargeurs pleins + un bonus au hasard |
| 5 | **Frappe aérienne** — vise un point au loin, clic droit, ça explose 5 s plus tard |
| 7 | **Tourelle sentinelle** — vise un bloc près de toi, elle tire 30 s sur les ennemis |
| 11 | **Bombardement** — tout le terrain est pilonné pendant 10 s |

Aucune explosion ne casse de blocs, le terrain reste intact.

---

## Si ça ne marche pas

**« Incompatible mod set » / « Mod rejections » à la connexion**
Ton pack n'est pas à jour. Ferme Minecraft, relance l'instance depuis Prism et laisse
packwiz-installer finir. Si ça persiste, demande à Clément s'il a publié la dernière version.

**Le jeu plante au démarrage**
Vérifie que tu es bien en Java 17 : instance → **Edit** → **Settings** → **Java**.
Et donne-lui assez de RAM : même écran, **Memory**, maximum **4096 Mo**.

**Ça rame**
Dans *Options → Graphismes*, baisse la distance d'affichage à 8 chunks et passe les
graphismes en « Rapides ». Le pack contient déjà Embeddium, qui améliore nettement les FPS.

**Les armes sont invisibles ou roses**
Le mod d'armes ne s'est pas téléchargé entièrement. Relance l'instance ;
packwiz-installer revérifie tous les fichiers à chaque lancement et retélécharge ce qui manque.

**N'installe pas de shaders.** Oculus et Iris provoquent des crashs connus avec les armes
de ce mod. Si tu en avais déjà, retire-les.

**Rien de tout ça ne marche**
Dans Prism, sélectionne l'instance, clique sur **Logs** (ou **Minecraft Log**), copie les
dernières lignes et envoie-les à Clément.
