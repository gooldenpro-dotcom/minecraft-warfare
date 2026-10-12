#!/usr/bin/env bash
# Construit l'instance Prism Launcher a importer (.zip).
# L'instance lance packwiz-installer avant chaque demarrage : les mods se mettent
# a jour tout seuls depuis le depot GitHub du pack.
#
# Usage : ./tools/build-prism-instance.sh <pseudo-github> [depot] [branche]
set -euo pipefail

GH_USER="${1:-TON-PSEUDO-GITHUB}"
GH_REPO="${2:-minecraft-warfare-pack}"
GH_BRANCH="${3:-main}"
PACK_URL="https://raw.githubusercontent.com/${GH_USER}/${GH_REPO}/${GH_BRANCH}/pack.toml"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BOOTSTRAP="$ROOT/downloads/packwiz-installer-bootstrap.jar"
BUILD="$ROOT/dist/prism-build"
OUT="$ROOT/dist/MinecraftWarfare-Prism.zip"

if [ ! -f "$BOOTSTRAP" ]; then
  echo "Telechargement de packwiz-installer-bootstrap..."
  mkdir -p "$ROOT/downloads"
  curl -sL --fail -o "$BOOTSTRAP" \
    "https://github.com/packwiz/packwiz-installer-bootstrap/releases/latest/download/packwiz-installer-bootstrap.jar"
fi

rm -rf "$BUILD" "$OUT"
mkdir -p "$BUILD/.minecraft"
cp "$BOOTSTRAP" "$BUILD/.minecraft/packwiz-installer-bootstrap.jar"

cat > "$BUILD/instance.cfg" <<EOF
InstanceType=OneSix
name=Minecraft Warfare
iconKey=default
notes=Serveur Minecraft Warfare. Les mods se mettent a jour automatiquement a chaque lancement.
OverrideCommands=true
PreLaunchCommand="\$INST_JAVA" -jar packwiz-installer-bootstrap.jar "${PACK_URL}"
OverrideMemory=true
MinMemAlloc=2048
MaxMemAlloc=4096
EOF

cat > "$BUILD/mmc-pack.json" <<'EOF'
{
    "components": [
        {
            "cachedName": "LWJGL 3",
            "cachedVersion": "3.3.1",
            "cachedVolatile": true,
            "dependencyOnly": true,
            "uid": "org.lwjgl3",
            "version": "3.3.1"
        },
        {
            "cachedName": "Minecraft",
            "cachedRequires": [
                {
                    "suggests": "3.3.1",
                    "uid": "org.lwjgl3"
                }
            ],
            "cachedVersion": "1.20.1",
            "important": true,
            "uid": "net.minecraft",
            "version": "1.20.1"
        },
        {
            "cachedName": "Forge",
            "cachedRequires": [
                {
                    "equals": "1.20.1",
                    "uid": "net.minecraft"
                }
            ],
            "cachedVersion": "47.4.10",
            "uid": "net.minecraftforge",
            "version": "47.4.10"
        }
    ],
    "formatVersion": 1
}
EOF

(cd "$BUILD" && zip -q -r "$OUT" . -x '.DS_Store')
rm -rf "$BUILD"

echo "Instance prete : $OUT"
echo "URL du pack    : $PACK_URL"
if [ "$GH_USER" = "TON-PSEUDO-GITHUB" ]; then
  echo
  echo "ATTENTION : URL de remplacement. Relance avec ton pseudo GitHub :"
  echo "  ./tools/build-prism-instance.sh mon-pseudo minecraft-warfare-pack"
fi
