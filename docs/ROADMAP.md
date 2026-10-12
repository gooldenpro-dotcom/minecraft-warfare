# Vision du projet

Un serveur Minecraft moddé qui donne les sensations de Call of Duty, avec trois piliers.

## 1. Multijoueur (priorite actuelle)

Modes a la COD, progression par niveaux et prestiges, deblocages, killstreaks, ATH,
classements. **C'est la partie la plus avancee** : voir [ETAT_ACTUEL.md](ETAT_ACTUEL.md).

Cartes voulues : Terminal (installee), Shipment (a installer), Nuketown, puis d'autres.

## 2. Zombie (survie facon BO2, carte Town)

Manches successives, difficulte croissante, chiens toutes les 5 manches, systeme de
points, portes a ouvrir, boite mystere, pack-a-punch, atouts. Affichage des manches et
ambiance sonore dans l'esprit BO2, **avec des elements originaux**.

## 3. DMZ (extraction shooter)

Melange PVE/PVP, exfiltrations, butin conserve entre les parties, base personnelle hors
partie pour stocker ce qu'on ramene. References : Arc Raiders, DMZ de MW2 (2022).

## Autour du jeu

- **Menu** : point d'entree unique — choisir son mode, sa carte, ses classes, aller au
  stand de tir, voir les classements, acceder a la boutique.
- **Boutique** : skins d'armes, de vehicules et de joueurs, boites mysteres. Achetable
  en jeu avec les points gagnes, et surtout depuis un **site externe**.
- **Launcher** : le joueur installe une seule chose et se connecte. Packs de textures,
  mods et mises a jour arrivent tout seuls (packwiz aujourd'hui).
- **Donnees** : toutes les performances des joueurs sont conservees cote serveur pour
  alimenter les classements et la boutique.

## Mods

| Mod | Role | Etat |
|---|---|---|
| TaCZ | armes, animations, visee | installe. Fabrication d'armes a desactiver pour les joueurs |
| Superb Warfare | vehicules, quelques armes | **a ajouter**, sans doublon d'armes avec TaCZ, sans fabrication |
| Mod maison `warfare` | modes, progression, menu, ATH, minimap | en cours |

Tout ce qui peut etre fait cote serveur doit l'etre : le client ne doit avoir que le
strict necessaire (le mod, TaCZ, l'optimisation).

## Regle non negociable

Aucun fichier appartenant a Call of Duty / Activision (texture, modele, son, police,
interface). On s'inspire du ressenti, on ne copie pas les fichiers.
