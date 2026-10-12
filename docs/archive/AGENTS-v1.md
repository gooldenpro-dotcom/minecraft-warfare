# Minecraft Warfare — instructions pour l'agent

Ce dépôt est un serveur Minecraft moddé façon Call of Duty (lobby, Free For All, création de classe, atouts, killstreaks), développé étape par étape. Les prompts de chaque étape sont dans `PROMPTS.md`.

## Contexte fixe (ne pas changer sans me demander)

- Minecraft **1.20.1**, **Forge** (dernière version recommandée pour 1.20.1), **Java 17**.
- Mod d'armes : **Timeless and Classics Zero (TaCZ)**, version officielle pour Forge 1.20.1.
- Mod perso : modid `warfare`, dans `mod/`, basé sur le MDK Forge officiel.
- Tout tourne **en local** sur mon Mac (Apple Silicon) : serveur et jeu sur la même machine. Je joue avec **Prism Launcher**.
- Structure : `server/` (serveur), `mod/` (mod warfare), `pack/` (modpack packwiz, étape 5), `docs/` (documentation).

## Règles de travail

1. Une seule étape par session. Ne commence pas l'étape suivante sans que je le demande.
2. Avant de coder, lis `docs/` et l'état actuel du dépôt pour reprendre le contexte.
3. Pour toute intégration avec TaCZ (kills, dégâts, rechargement, liste des armes), cherche les vrais événements ou l'API dans son jar ou son code source (GPL-3.0). Ne devine pas les noms ; si c'est ambigu, dis-le-moi.
4. Après chaque modification : compile avec Gradle, copie le jar dans `server/mods/`, démarre le serveur, lis les logs et corrige jusqu'à ce qu'il démarre sans erreur. Arrête-le ensuite proprement.
5. Tous les réglages de gameplay vont dans un fichier de config TOML, pas en dur dans le code.
6. Aucun élément appartenant à Call of Duty / Activision : pas de logo, texture, modèle, son ou interface copiés. Design original uniquement ; sons vanilla de Minecraft autorisés.
7. Ne télécharge que depuis des sources officielles (Forge, Modrinth, CurseForge, Mojang).
8. Demande-moi avant : d'accepter l'EULA de Minecraft, d'ajouter un mod non prévu, ou de toucher à quoi que ce soit hors de ce dossier (réseau, box, système).
9. Mets à jour `docs/` à chaque étape et termine toujours par une courte **checklist de tests en jeu**.
10. Git : `.gitignore` qui exclut mondes, logs, crash-reports et jars téléchargés. Je te dirai quand faire le commit d'une étape.
