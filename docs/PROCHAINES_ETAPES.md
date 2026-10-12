# Prochaines etapes

Priorite : **jouer a deux en multijoueur**, puis zombies, puis DMZ.
Une etape = une session de travail. Cocher en avancant.

## P0 — finir le premier rendu jouable

- [ ] Tester la 0.7.0 en jeu ([TESTS.md](TESTS.md)) et corriger ce qui casse.
- [ ] Publier le pack packwiz (`cd pack && git add -A && git commit && git push`),
      sinon le collegue reste en 0.6.1 et est refuse a la connexion.
- [ ] Replacer les points d'apparition de Terminal aux vrais emplacements de la carte
      (ceux generes automatiquement sont autour du spawn du monde).
- [ ] Installer **Shipment** (`maps/Shipment.zip`) comme deuxieme arene, pour les
      modes 1VS1. Deux options : coller la carte dans le meme monde a quelques
      centaines de blocs, puis `/warfare arena create shipment` — ou gerer plusieurs
      mondes (plus lourd, voir P2).
- [ ] Rendre le serveur joignable par le collegue : redirection du port 25565 sur la
      box, ou playit.gg. Rien de tout ca ne se fait depuis le mod.

## P1 — multijoueur « comme COD »

- [ ] **Minimap + drone** : minimap maison dessinee cote client a partir des donnees
      envoyees par le serveur (positions alliees en permanence, ennemis uniquement
      pendant un balayage UAV). A faire dans notre mod plutot qu'avec un mod tiers :
      on controle deja l'etat de la partie et les equipes.
- [ ] **Sons et voix d'annonce** : sons vanilla uniquement, ou sons enregistres par nous.
      ⚠️ Ne pas importer les fichiers de MW2/BO2 (fichiers Activision).
- [ ] **Police facon MW2** : ne pas copier la police du jeu. Soit une police libre
      proche (condensee, militaire) embarquee dans un pack de ressources servi par le
      serveur, soit une police bitmap dessinee pour le projet.
- [ ] Pack de ressources automatique (`resource-pack=` dans `server.properties`)
      pour que le joueur n'installe rien a la main.
- [ ] Modes manquants : Domination / Point strategique, Recherche et destruction.
- [ ] Stand de tir : une arene « stand », mode entrainement, toutes les armes
      disponibles sans deblocage, cibles.
- [ ] Equilibrage : degats par arme, vitesse de deplacement, temps de regeneration.

## P2 — infrastructure

- [ ] **Plusieurs mondes** (un monde par carte, chargement a la demande) ou garder
      « toutes les cartes dans un seul monde ». Decision a prendre avant d'ajouter
      beaucoup de cartes : c'est structurant pour le mode zombie et le DMZ.
- [ ] **Superb Warfare** (vehicules) : verifier la compatibilite avec TaCZ 1.1.8,
      desactiver la fabrication, ne pas dupliquer les armes deja fournies par TaCZ.
- [ ] Launcher « un clic » hors Prism (le pack packwiz existe deja, il manque un
      lanceur dedie ou un `.mrpack` a jour).
- [ ] Sauvegarde/export des statistiques pour le site externe (boutique, classements).

## P3 — mode Zombie (BO2 sur Town)

- [ ] Recuperer la carte **Town** (absente de `maps/`, il n'y a que Terminal et Shipment).
- [ ] Manches, zombies de plus en plus nombreux et resistants, chiens toutes les 5 manches.
- [ ] Points, portes a ouvrir, boite mystere, pack-a-punch.
- [ ] Atouts (munitions max, mort instantanee...), affichage des manches facon BO2.

## P4 — DMZ (extraction shooter)

- [ ] Carte dediee, zones d'exfiltration, PVE + PVP.
- [ ] Inventaire d'extraction : ce qui est ramene est conserve, ce qui est perdu l'est.
- [ ] Base personnelle hors partie (stockage du butin).
- [ ] S'inspirer d'Arc Raiders et du DMZ de MW2 (2022).

## P5 — boutique et site externe

- [ ] API ou export des points et des achats.
- [ ] Skins d'armes, skins de vehicules, skins de joueurs, boites mysteres.
- [ ] Achat en jeu **et** depuis le site.
