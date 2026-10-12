# Checklist de tests en jeu — 0.7.0

A faire a deux (ou seul avec `minPlayers = 1` dans `[match]`).

## Connexion et menu
1. Se connecter : le menu d'accueil s'ouvre tout seul.
2. **Echap** : le menu se ferme, on revient au jeu. ← correction principale de cette version
3. **M** : le menu se rouvre. **C** : la creation de classe s'ouvre.
4. **QUITTER LE SERVEUR** : retour a l'ecran-titre, sans blocage.
5. Panneau JOUEURS : les deux joueurs sont listes avec leur niveau.

## Cartes et modes
6. CARTE : `terminal_mw2` apparait et est selectionnable.
7. MODE DE JEU : choisir « Melee generale » → le changement est annonce dans le chat
   chez les deux joueurs.
8. `/warfare listspawns` : 4 points (2 alpha, 2 bravo). Les replacer :
   `/warfare removespawn 1` x4, puis `/warfare addspawn alpha` / `bravo` aux bons endroits.

## Partie
9. LANCER LA PARTIE : compte a rebours de 30 s, ecran de preparation.
10. Apparition avec l'arme de sa classe, inventaire propre.
11. Tuer l'autre joueur : killfeed dans le chat, **« +100 ELIMINATION »** a l'ecran,
    score du tableau lateral qui monte.
12. Tir a la tete : mention « TETE » et **+150** au total.
13. Mourir : « ELIMINE », reapparition apres 3 s au point le plus eloigne.
14. Atteindre le score ou la fin du temps : titre de victoire/defaite, classement dans
    le chat, retour au menu apres 10 s.

## Modes particuliers
15. **Match a mort par equipe** : pseudos colores (rouge/bleu), tirer sur son allie ne
    fait aucun degat, les deux scores d'equipe sont en haut de l'ecran.
16. **Jeu d'armes** : l'arme change a chaque elimination, « Arme 2/17 » en bas a gauche.
17. **Une balle chargee** : une seule balle, elimination en un coup, une elimination
    rend une balle.
18. **1VS1 Sniper** : M700 impose pour les deux.

## Progression
19. `/warfare stats` : eliminations, morts, niveau, XP coherents.
20. Monter d'un niveau : titre « NIVEAU x » + son.
21. Menu CREER UNE CLASSE : les armes trop hautes affichent « NIV. x » et ne se
    selectionnent pas ; cliquer dessus affiche « Debloquee au niveau x ».
22. CLASSEMENT dans le menu et `/warfare top` : les deux joueurs apparaissent.
23. Se deconnecter et revenir : le niveau et les statistiques sont conserves.

## Monde
24. Aucun monstre ne doit apparaitre, meme la nuit (il doit d'ailleurs faire jour en
    permanence), aucune pluie.
