# Minecraft Warfare — instructions pour les agents

Serveur Minecraft moddé façon Call of Duty. Trois piliers : **Multijoueur** (priorité
actuelle), **Zombie** (survie façon BO2) et **DMZ** (extraction shooter).
Discord du projet : https://discord.gg/yJR9m8fvP

## A lire avant de coder, dans cet ordre

1. `docs/HANDOFF.md` — où on en est, la boucle build/deploy, les pièges.
2. `docs/ETAT_ACTUEL.md` — ce qui fonctionne vraiment aujourd'hui.
3. `docs/PROCHAINES_ETAPES.md` — la prochaine tâche à prendre.
4. `docs/ROADMAP.md` — la vision complète, pour ne pas partir à contre-sens.

## Contexte technique (ne pas changer sans demander)

- Minecraft **1.20.1**, **Forge 47.4.10**, **Java 17**. Tout tourne en local sur un Mac
  Apple Silicon ; serveur et client sur la même machine.
- Armes : **TaCZ** (Timeless and Classics Zero) `1.20.1-1.1.8-hotfix2`.
- Véhicules : **Superb Warfare** (prévu, pas encore installé) — sans dupliquer les armes
  de TaCZ, et sans fabrication par les joueurs.
- Mod maison : modid `warfare`, dans `mod/` (MDK Forge officiel).
- Distribution : pack **packwiz** dans `pack/`, publié sur GitHub, instance Prism Launcher.

## Structure

```
mod/        le mod warfare (source + Gradle)
server/     le serveur Forge (mods/, config/, monde Terminal_MW2/)
pack/       pack packwiz publié aux joueurs (dépôt git séparé)
maps/       cartes à installer (Terminal_MW2.zip, Shipment.zip)
dist/       exports (.mrpack, zips de launcher)
downloads/  installateurs et jars téléchargés
tools/      packwiz et scripts
docs/       documentation (voir ci-dessus)
start.sh    lance le serveur (4 Go)
```

## Règles de travail

1. **Une tâche à la fois**, prise dans `docs/PROCHAINES_ETAPES.md`. Ne pas enchaîner sur
   la suivante sans qu'on le demande.
2. Avant de coder, lire `docs/` et l'état du dépôt pour reprendre le contexte.
3. Pour toute intégration TaCZ (kills, dégâts, rechargement, liste d'armes) : chercher
   les vrais événements ou l'API dans le jar (`javap`) ou le code source (GPL-3.0).
   **Ne jamais deviner un nom d'API** ; si c'est ambigu, le dire.
4. Après chaque modification : compiler avec Gradle, copier le jar dans `server/mods/`,
   **dans l'instance Prism et dans `pack/`** (puis `packwiz refresh`), démarrer le
   serveur, lire les logs et corriger jusqu'à un démarrage propre, puis l'arrêter.
5. Tous les réglages de gameplay vont dans `server/config/warfare-common.toml`
   (section + commentaire), jamais en dur dans le code.
6. **Aucun élément appartenant à Call of Duty / Activision** : pas de logo, texture,
   modèle, son, police ni interface copiés. Design original uniquement ; sons vanilla
   de Minecraft autorisés. On copie le ressenti, pas les fichiers.
7. Ne télécharger que depuis des sources officielles (Forge, Modrinth, CurseForge, Mojang).
8. **Demander avant** : accepter l'EULA, ajouter un mod non prévu, pousser le pack sur
   GitHub (ça distribue aux joueurs), toucher au réseau/à la box/au système, ou toucher
   à quoi que ce soit hors de ce dossier.
9. Tout ce qui peut être fait côté serveur doit l'être. Le client ne doit avoir à
   installer que le mod, TaCZ et l'optimisation — le reste arrive automatiquement.
10. Code et commentaires **en français sans accents** (cohérent avec l'existant).
    Modifications ciblées ; pas de réécriture complète de fichier sans raison.
11. **Fin de session obligatoire** : mettre à jour `docs/CHANGELOG.md`,
    `docs/ETAT_ACTUEL.md`, `docs/HANDOFF.md` et `docs/PROCHAINES_ETAPES.md`, puis
    terminer par une courte **checklist de tests en jeu** (`docs/TESTS.md`).
12. Git : on travaille à plusieurs. Annoncer sur le Discord le sous-système qu'on prend.
    Le commit d'une étape se fait quand on le demande.

## Rappels de protocole réseau

Toute modification des paquets (`mod/src/main/java/com/warfare/net/WarfareNetwork.java`)
impose d'incrémenter `PROTOCOL` **et** de redéployer le jar à tous les joueurs : sinon
ils sont déconnectés sans message clair. Les nouveaux paquets s'ajoutent **à la fin** de
`register()`.
