# Minecraft Warfare — prompts

Cinq prompts à donner à Codex dans l'ordre, **un par session**, en testant en jeu entre chaque. Codex lit automatiquement `AGENTS.md` (le contexte fixe du projet).

**Méthode à chaque étape :**

1. Ouvre Codex dans ce dossier (`Minecraft Warfare` sur le bureau).
2. Colle le prompt de l'étape et laisse-le travailler.
3. Teste en jeu avec la checklist qu'il te donne.
4. Quand tout marche : « fais un commit git de cette étape ».
5. Nouvelle session pour l'étape suivante.

**Sur Mac :** Java 17 se vérifie avec `java -version` dans le Terminal. Comme le serveur et le jeu tournent sur la même machine, garde 4 Go pour le serveur et 4–6 Go pour le jeu.

---

## Prompt 1 — un serveur local qui démarre

Objectif : te connecter à ton propre serveur et tirer avec une arme TaCZ.

```text
Je crée un serveur Minecraft moddé façon Call of Duty, entièrement en local sur mon Mac. Cette session ne fait QUE l'étape 1 : un serveur qui démarre. Ne commence pas le code de gameplay.

Choix techniques (voir AGENTS.md, ne pas changer sans me demander) :
- Minecraft 1.20.1, Forge (dernière version recommandée pour 1.20.1), Java 17.
- Mod d'armes : Timeless and Classics Zero (TaCZ), version officielle pour Forge 1.20.1, téléchargée depuis Modrinth ou CurseForge.

À faire :
1. Vérifie que Java 17 est installé. S'il manque, explique-moi comment l'installer et attends.
2. Crée la structure : server/ (le serveur), mod/ (vide pour l'instant), docs/. Initialise un dépôt git avec un .gitignore adapté (ignore les mondes, logs et jars téléchargés).
3. Télécharge l'installateur Forge et installe le serveur dans server/.
4. Avant d'accepter l'EULA de Minecraft, montre-moi le lien et demande-moi mon accord.
5. server.properties : online-mode=true, max-players=18, pvp=true, difficulty=normal, gamemode=adventure, spawn-protection=0, motd "Minecraft Warfare".
6. Place TaCZ dans server/mods/.
7. Crée un script de lancement start.sh avec 4 Go de RAM.
8. Lance le serveur, lis les logs et corrige jusqu'à voir "Done". Arrête-le ensuite proprement.
9. Écris docs/SETUP.md : comment lancer le serveur, et comment créer dans Prism Launcher une instance Forge 1.20.1 (même version de Forge) avec TaCZ pour me connecter à localhost.

Termine par une courte checklist de tests à faire en jeu (me connecter, me donner une arme TaCZ avec une commande, tirer).
```

---

## Prompt 2 — lobby et Free For All

Objectif : une vraie partie qui se lance, se joue et se termine toute seule. Pour tester seul, mets le minimum de joueurs à 1 dans la config.

```text
Étape 2 du projet Minecraft Warfare. Lis d'abord docs/ et l'état du dossier server/ pour reprendre le contexte.

Crée dans mod/ un mod Forge 1.20.1 (modid "warfare") à partir du MDK officiel Forge, avec TaCZ en dépendance. Cette étape est côté serveur uniquement : pas encore d'écran client.

Gameplay Free For All :
- Lobby : à la connexion, le joueur est téléporté dans une zone lobby, en mode aventure et invincible.
- Démarrage : quand le nombre minimum de joueurs est atteint (config, défaut 2), afficher "En attente de joueurs..." puis un compte à rebours de 30 s en titre à l'écran.
- Partie : chacun pour soi, fin à 30 kills ou 10 minutes (config). Points d'apparition enregistrés par commande ; au respawn, choisir celui le plus éloigné des ennemis.
- Mort : pas d'écran de mort, réapparition après 3 s avec un équipement par défaut (un fusil d'assaut et un pistolet TaCZ + munitions, configurables).
- Interface : scoreboard latéral (top 5, mon score, temps restant) et annonce des éliminations ("X a éliminé Y").
- Fin : titre avec le vainqueur, classement complet dans le chat, retour au lobby, nouvelle partie automatique.
- Commandes admin : /warfare start, stop, setlobby, addspawn, listspawns, reload.
- Tous les réglages dans un fichier de config TOML.

Pour détecter qu'un kill vient d'une arme TaCZ, cherche les vrais événements ou l'API de TaCZ (dans son jar ou son code source GPL) plutôt que de deviner. Si c'est ambigu, dis-le-moi.

Ensuite : compile avec Gradle, copie le jar dans server/mods/, démarre le serveur et corrige les erreurs. Mets à jour docs/SETUP.md (commandes, comment placer le lobby et les spawns). Termine par une checklist de tests en jeu.
```

---

## Prompt 3 — création de classe et atouts

Objectif : le menu « Créer une classe ». À partir de cette étape, le mod `warfare` doit aussi être installé dans ton instance Prism, car il ajoute un écran côté client.

```text
Étape 3 du projet Minecraft Warfare. Lis d'abord docs/ et le code du mod warfare.

Ajoute la création de classe et les atouts.

Menu "Créer une classe" :
- Écran client ouvert par une touche configurable (défaut C), utilisable seulement dans le lobby ou entre deux vies.
- Style militaire sombre et lisible, avec un design original : n'utilise aucun logo, texture ou son de Call of Duty.
- 5 emplacements de classe par joueur. Chaque classe : un nom, une arme principale, une arme secondaire, et 3 atouts (un par catégorie).
- Liste des armes lue depuis les armes TaCZ chargées, classées par type (fusil d'assaut, mitraillette, sniper, pistolet...), avec une liste blanche configurable.

Atouts :
- Catégorie 1 : Mains rapides (rechargement +30 %) ou Sprinteur (vitesse +10 %).
- Catégorie 2 : Puissance d'arrêt (+20 % de dégâts par balle) ou Blindé (-30 % de dégâts d'explosion).
- Catégorie 3 : Commando (portée au corps à corps augmentée) ou Pas de loup (pas de bruits de pas).
Si un atout est trop difficile à brancher sur TaCZ, propose un remplaçant et dis-le-moi.

Technique :
- Classes sauvegardées côté serveur (un fichier JSON par UUID de joueur).
- Synchronisation par paquets réseau ; le serveur valide chaque choix (pas de triche possible depuis le client).
- La classe choisie est donnée au prochain respawn.
- Pour le rechargement et les dégâts, trouve les vrais événements de TaCZ dans son code plutôt que de deviner les noms.

Compile, déploie le jar dans server/mods/, explique-moi où copier le jar dans mon instance Prism, mets à jour docs/, et termine par une checklist de tests.
```

---

## Prompt 4 — killstreaks

Objectif : des récompenses pour les séries de kills sans mourir, choisies dans le menu de classe.

```text
Étape 4 du projet Minecraft Warfare. Lis d'abord docs/ et le code du mod warfare.

Ajoute les killstreaks :
- Compteur de kills consécutifs sans mourir, remis à zéro à la mort. Les kills faits avec une killstreak ne comptent pas dans la série (réglable).
- Dans le menu de classe, le joueur choisit 3 killstreaks parmi :
  - 3 kills, Drone de repérage : les ennemis sont en surbrillance pendant 15 s. Idéalement visible seulement par le joueur ; si ce n'est pas faisable proprement, fais-le pour tous et dis-le-moi.
  - 4 kills, Ravitaillement : munitions pleines + un bonus aléatoire.
  - 5 kills, Frappe aérienne : le joueur vise un point avec un objet désignateur ; 5 s plus tard, une salve d'explosions frappe la zone. Les kills sont crédités au joueur.
  - 7 kills, Tourelle sentinelle : une tourelle posée qui tire sur les ennemis proches pendant 30 s.
  - 11 kills, Bombardement : plusieurs frappes sur toute la map pendant 10 s.
- La récompense arrive comme objet dans un emplacement dédié de la barre, activé par clic droit.
- Annonces : titre "Frappe aérienne disponible", message à tous quand elle est utilisée, sons vanilla de Minecraft.
- Les explosions ne cassent jamais de blocs.
- Seuils, durées, dégâts et rayons réglables dans la config.

Compile, déploie (serveur + instruction pour mon instance Prism), mets à jour docs/, et termine par une checklist de tests.
```

---

## Prompt 5 — modpack pour tes amis

Objectif : tes amis installent tout en un clic, reçoivent les mises à jour automatiquement et rejoignent ton Mac.

```text
Étape 5 du projet Minecraft Warfare. Lis d'abord docs/.

Prépare la distribution à mes amis avec packwiz :
1. Installe packwiz et crée pack/ : Minecraft 1.20.1, exactement la même version de Forge que le serveur, TaCZ (depuis Modrinth ou CurseForge) et mon mod warfare.
2. Si tu proposes des mods d'optimisation côté client, vérifie d'abord qu'ils sont compatibles avec TaCZ et demande-moi avant de les ajouter.
3. Le pack sera hébergé sur un dépôt GitHub public. Explique-moi comment créer le dépôt ; je ferai la mise en ligne moi-même.
4. Configure une instance Prism Launcher qui lance packwiz-installer avant chaque démarrage (mises à jour automatiques), et exporte-la en .zip à importer dans Prism.
5. Exporte aussi une version .mrpack.
6. Écris docs/AMIS.md : guide pas à pas pour mes amis, de l'installation de Prism jusqu'à la connexion au serveur.
7. Explique-moi deux façons de rendre mon serveur local accessible : la redirection du port 25565 sur ma box, et l'alternative playit.gg. Ne modifie rien sur mon réseau toi-même.
```

---

## Quand ça casse

Donne toujours à Codex ces trois éléments :

1. **Ce que tu as fait** : « je me suis connecté, j'ai tué un joueur avec le sniper ».
2. **Ce que tu attendais et ce qui s'est passé** : « le score devait monter, il est resté à 0 ».
3. **Le log** : demande-lui de lire `server/logs/latest.log`, ou le fichier dans `server/crash-reports/` si ça a planté. Côté jeu, le log est dans le dossier de ton instance Prism.

Si une étape part dans tous les sens, demande-lui de revenir au dernier commit git qui marchait et de recommencer plus simplement.
