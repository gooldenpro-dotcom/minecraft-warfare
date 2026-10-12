#!/usr/bin/env bash
# Prepare un clone neuf du depot : installe Forge, recupere TaCZ, compile le mod.
# A lancer depuis la racine du projet :  ./tools/bootstrap.sh
#
# Ce que le depot ne contient pas (et que ce script reconstruit) :
#   server/libraries/   l'installation Forge (des centaines de fichiers)
#   server/mods/*.jar   TaCZ et le mod compile
#   mod/libs/           TaCZ, necessaire pour compiler le mod
set -euo pipefail

FORGE="1.20.1-47.4.10"
TACZ_FILE="tacz-1.20.1-1.1.8-hotfix2.jar"
TACZ_URL="https://cdn.modrinth.com/data/SzzJttH8/versions/AzCBJlex/$TACZ_FILE"
TACZ_SHA1="5c2f72dfe8125ffa863e3b5feafa27d86d7e56ea"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== 1/5  Java 17 =="
if ! command -v java >/dev/null; then
  echo "Java absent. Installe Java 17 : https://adoptium.net/temurin/releases/?version=17" >&2
  exit 1
fi
java -version 2>&1 | head -1
case "$(java -version 2>&1 | head -1)" in
  *\"17.*) ;;
  *) echo "ATTENTION : Java 17 est attendu. Le reste peut echouer." >&2 ;;
esac

echo "== 2/5  TaCZ =="
mkdir -p downloads server/mods mod/libs
if [ ! -f "downloads/$TACZ_FILE" ]; then
  echo "   telechargement depuis Modrinth..."
  curl -sL --fail -o "downloads/$TACZ_FILE" "$TACZ_URL"
fi
GOT=$(shasum -a 1 "downloads/$TACZ_FILE" | cut -d' ' -f1)
if [ "$GOT" != "$TACZ_SHA1" ]; then
  echo "Empreinte TaCZ inattendue ($GOT). Fichier supprime, relance le script." >&2
  rm -f "downloads/$TACZ_FILE"; exit 1
fi
cp -f "downloads/$TACZ_FILE" server/mods/
cp -f "downloads/$TACZ_FILE" mod/libs/
echo "   ok (empreinte verifiee)"

echo "== 3/5  Serveur Forge $FORGE =="
if [ -f "server/libraries/net/minecraftforge/forge/$FORGE/unix_args.txt" ]; then
  echo "   deja installe"
else
  INSTALLER="downloads/forge-$FORGE-installer.jar"
  [ -f "$INSTALLER" ] || curl -sL --fail -o "$INSTALLER" \
    "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar"
  echo "   installation (telecharge Minecraft et ses bibliotheques, quelques minutes)..."
  (cd server && java -jar "../$INSTALLER" --installServer . >/dev/null)
  echo "   ok"
fi

echo "== 4/5  Compilation du mod =="
(cd mod && ./gradlew --no-daemon build -q)
JAR=$(ls -t mod/build/libs/warfare-*.jar | head -1)
rm -f server/mods/warfare-*.jar
cp "$JAR" server/mods/
echo "   $(basename "$JAR") deploye dans server/mods/"

echo "== 5/5  EULA Minecraft =="
if [ -f server/eula.txt ] && grep -q '^eula=true' server/eula.txt; then
  echo "   deja accepte"
else
  cat <<'EULA'
   Le serveur ne demarrera pas tant que tu n'auras pas accepte l'EULA Minecraft.
   Lis-la : https://aka.ms/MinecraftEULA
   Puis, si tu es d'accord :   echo 'eula=true' > server/eula.txt
EULA
fi

echo
echo "Termine. Lance le serveur avec :  ./start.sh"
echo "Cote client, suis docs/AMIS.md (instance Prism ou launcher officiel)."
