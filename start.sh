#!/usr/bin/env bash
# Minecraft Warfare - lancement du serveur Forge 1.20.1 (47.4.10), 4 Go de RAM
set -euo pipefail

cd "$(dirname "$0")/server"

FORGE_ARGS="libraries/net/minecraftforge/forge/1.20.1-47.4.10/unix_args.txt"

if [ ! -f "$FORGE_ARGS" ]; then
  echo "Erreur : Forge n'est pas installe (fichier manquant : $FORGE_ARGS)" >&2
  exit 1
fi

if [ ! -f eula.txt ] || ! grep -q '^eula=true' eula.txt; then
  echo "Erreur : l'EULA Minecraft n'est pas accepte (server/eula.txt)." >&2
  echo "Voir https://aka.ms/MinecraftEULA puis mettre eula=true dans server/eula.txt" >&2
  exit 1
fi

echo "Demarrage de Minecraft Warfare (Forge 1.20.1-47.4.10, 4 Go)..."
exec java @user_jvm_args.txt "@$FORGE_ARGS" nogui "$@"
