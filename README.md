# Minecraft Warfare

Serveur Minecraft 1.20.1 moddé façon Call of Duty : modes multijoueur, progression par
niveaux et prestiges, killstreaks, ATH. Modes Zombie et DMZ prévus.

**Discord :** https://discord.gg/yJR9m8fvP

## Démarrer

```bash
./start.sh                 # lance le serveur (4 Go) ; logs dans server/logs/latest.log
```

Puis se connecter sur `localhost` depuis l'instance Prism « Minecraft Warfare ».

En jeu : **M** ouvre le menu d'accueil, **C** la création de classe, **Échap** ferme.

## Documentation

| Fichier | Contenu |
|---|---|
| [docs/HANDOFF.md](docs/HANDOFF.md) | reprendre le projet, boucle build/deploy, pièges |
| [docs/ETAT_ACTUEL.md](docs/ETAT_ACTUEL.md) | ce qui fonctionne aujourd'hui |
| [docs/PROCHAINES_ETAPES.md](docs/PROCHAINES_ETAPES.md) | backlog priorisé |
| [docs/ROADMAP.md](docs/ROADMAP.md) | la vision complète |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | journal des versions |
| [docs/TESTS.md](docs/TESTS.md) | checklist de tests en jeu |
| [docs/SETUP.md](docs/SETUP.md) | installation détaillée du serveur et du client |
| [docs/AMIS.md](docs/AMIS.md) | guide d'installation pour les joueurs |
| [AGENTS.md](AGENTS.md) | règles de travail pour les agents / contributeurs |

## Structure

```
mod/      le mod warfare (source Gradle)        server/  le serveur Forge
pack/     pack packwiz distribué aux joueurs    maps/    cartes à installer
tools/    packwiz et scripts                    dist/    exports
docs/     documentation                         downloads/ installateurs
```
