# Projet Minecraft Warfare

Serveur Minecraft moddé façon Call of Duty (Multijoueur, Zombie, DMZ).
Discord : https://discord.gg/yJR9m8fvP

**Les instructions complètes sont dans [AGENTS.md](AGENTS.md).** Les lire au début de
chaque session, ainsi que `docs/HANDOFF.md` et `docs/ETAT_ACTUEL.md`.

## Rappels essentiels

- Minecraft 1.20.1, Forge 47.4.10, Java 17, TaCZ 1.1.8-hotfix2, mod maison `warfare`.
- Priorité : **le multijoueur jouable à deux**. Zombie et DMZ viennent après.
- Aucun fichier appartenant à Call of Duty / Activision (texture, son, police, modèle).
  Sons vanilla de Minecraft autorisés.
- Ne jamais inventer un nom d'API TaCZ : le vérifier dans le jar avec `javap`.
- Tous les réglages de gameplay vont dans `server/config/warfare-common.toml`.
- Après chaque modification : `cd mod && ./gradlew build`, puis copier le jar dans
  `server/mods/`, dans l'instance Prism **et** dans `pack/` (`tools/packwiz refresh`),
  relancer le serveur et lire les logs.
- Un seul serveur à la fois (le monde est verrouillé) : vérifier avec
  `ps -eo pid,command | grep unix_args`.
- Toute modification des paquets réseau impose d'incrémenter `PROTOCOL` dans
  `net/WarfareNetwork.java` et de redéployer chez tous les joueurs.
- Demander avant de pousser le pack sur GitHub (ça distribue aux joueurs), d'ajouter un
  mod non prévu ou de toucher au réseau.
- Code et commentaires en français sans accents. Modifications ciblées.
- Fin de session : mettre à jour `docs/CHANGELOG.md`, `docs/ETAT_ACTUEL.md`,
  `docs/HANDOFF.md`, `docs/PROCHAINES_ETAPES.md` + checklist de tests.
- Répondre de manière concise.
