# Handoff — reprendre le projet

> **A relire au debut de chaque session, et a mettre a jour a la fin.**
> Derniere mise a jour : 12 octobre 2026, fin de la session « modes multi + menu ».

## 1. Ou en est-on

Mod `warfare 0.7.0` : le multijoueur est jouable (6 modes, progression, menu repare,
ATH). Zombies, DMZ, boutique et minimap ne sont pas commences.
Detail complet : [ETAT_ACTUEL.md](ETAT_ACTUEL.md).

## 2. A faire tout de suite (bloquant pour jouer a deux)

1. **Tester en jeu** avec la checklist [TESTS.md](TESTS.md).
2. ~~Publier le pack~~ **fait** : `warfare 0.7.0` est pousse sur
   `gooldenpro-dotcom/minecraft-warfare-pack` (commit `562b133`). Les joueurs se mettent
   a jour au prochain lancement de Prism. Rappel pour les prochaines versions :
   ```bash
   cd "/Users/clement/Desktop/Minecraft Warfare/pack" && git add -A && git commit -m "pack: warfare <version>" && git push
   ```
   Sans ca les joueurs restent sur l'ancienne version et **sont refuses a la connexion**
   des que le protocole reseau change.
3. **Replacer les points d'apparition** de Terminal : ceux crees automatiquement sont
   autour du spawn du monde, pas aux vrais points de la carte.
   ```
   /warfare arena select terminal_mw2
   /warfare setlobby            (place le lobby ou tu es)
   /warfare listspawns
   /warfare removespawn 1       (a repeter pour vider les 4 points automatiques)
   /warfare addspawn alpha      (a chaque emplacement voulu)
   /warfare addspawn bravo
   ```

## 3. Boucle de travail

```bash
cd "/Users/clement/Desktop/Minecraft Warfare/mod"
./gradlew build --no-daemon                 # compile -> build/libs/warfare-<version>.jar
```
Puis deployer (toujours les trois endroits, sinon desynchronisation client/serveur) :
```bash
D="/Users/clement/Desktop/Minecraft Warfare"
P="/Users/clement/Library/Application Support/PrismLauncher/instances/Minecraft Warfare/.minecraft"
cp "$D/mod/build/libs/warfare-0.7.0.jar" "$D/server/mods/"
cp "$D/mod/build/libs/warfare-0.7.0.jar" "$P/mods/"
cp "$D/mod/build/libs/warfare-0.7.0.jar" "$D/pack/mods/" && "$D/tools/packwiz" refresh
```
Lancer le serveur : `./start.sh` depuis la racine. Logs : `server/logs/latest.log`.

**Pour piloter le serveur depuis un script** (pratique pour tester les commandes) :
```bash
mkfifo /tmp/mcin
( tail -f /tmp/mcin | ./start.sh > /tmp/server.log 2>&1 & )
echo "warfare arena list" > /tmp/mcin
echo "stop" > /tmp/mcin
```

## 4. Pieges rencontres (ne pas les refaire)

- **Protocole reseau** : toute modification des paquets (`net/WarfareNetwork.java`)
  oblige a incrementer `PROTOCOL` (actuellement `"5"`) **et** a redeployer le jar
  chez tous les joueurs. Sinon : deconnexion sans message clair.
- **Nouveau paquet** : toujours l'ajouter **a la fin** de `register()`, les
  identifiants doivent rester stables.
- **Config Forge** : `ModConfigEvent` est poste sur le bus **du mod**, pas sur celui
  de Forge (voir `Warfare.java`). `WarfareConfig.reload()` est aussi appele en filet
  de securite au demarrage du serveur.
- **Un seul serveur a la fois** : le monde est verrouille (`session.lock`). Verifier
  avec `ps -eo pid,command | grep unix_args` avant de relancer.
- **Chemin avec un espace** (`Minecraft Warfare`) : Gradle et Forge s'en sortent, mais
  toujours mettre les chemins entre guillemets dans les scripts.
- **TaCZ** : ne jamais deviner les noms d'API. Les verifier dans le jar :
  `unzip -o mod/libs/tacz-*.jar 'com/tacz/guns/api/**' -d /tmp/tacz && javap -classpath /tmp/tacz com.tacz.guns.api.item.IGun`
- **Degats des armes** : passer par `EntityHurtByGunEvent.Pre` (`setBaseAmount`).
  `LivingHurtEvent` ne voit pas les balles TaCZ de la meme facon.
- **Scoreboard par joueur** : `game/Sidebar.java` envoie les paquets a la main a partir
  d'un `Scoreboard` factice, parce que le scoreboard du serveur est global.

## 5. Conventions

- Code et commentaires **en francais sans accents** (coherent avec l'existant).
- Tous les reglages de gameplay vont dans `warfare-common.toml`, jamais en dur.
- **Aucun fichier appartenant a Call of Duty / Activision** : pas de texture, modele,
  son, police ni interface copies. Sons vanilla de Minecraft autorises. Les noms de
  modes et de grades sont generiques ou originaux.
- Modifications ciblees, pas de reecriture complete de fichier sans raison.
- Mettre a jour `docs/CHANGELOG.md`, `docs/ETAT_ACTUEL.md` et ce fichier a chaque session.

## 6. Qui fait quoi / ou parler

Discord du projet : https://discord.gg/yJR9m8fvP
Plusieurs personnes travaillent sur le projet : annoncer sur le Discord le fichier ou
le sous-systeme sur lequel on part (mod serveur, client/menu, cartes, pack) avant de
commencer, pour eviter deux chantiers sur `GameManager.java` en meme temps.
