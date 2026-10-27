#!/usr/bin/env bash
# Builds a single-file Linux AppImage from the Compose Desktop distribution.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/home/ricardo/Android/toolchain/jdk-17}"
VERSION="${VERSION:-1.0.1}"
TOOL_DIR="$ROOT/build/appimage-tool"
TOOL="$TOOL_DIR/appimagetool"
WORK="$ROOT/build/appimage"
RELEASE="$ROOT/release"
APP_NAME="MDViewer"

if [ ! -f "$ROOT/packaging/icon.png" ]; then
  echo "==> drawing icon"
  "$JAVA_HOME/bin/java" "$ROOT/packaging/make-icon.java" "$ROOT/packaging/icon.png"
fi

echo "==> building runtime image with jpackage"
./gradlew createDistributable --console=plain
APP_IMAGE="$ROOT/build/compose/binaries/main/app/$APP_NAME"
if [ ! -d "$APP_IMAGE" ]; then
  echo "error: jpackage output not found at $APP_IMAGE" >&2
  exit 1
fi

echo "==> assembling AppDir"
rm -rf "$WORK"
mkdir -p "$WORK/$APP_NAME.AppDir/usr"
cp -a "$APP_IMAGE/." "$WORK/$APP_NAME.AppDir/usr/"
cp "$ROOT/packaging/MDViewer.desktop" "$WORK/$APP_NAME.AppDir/MDViewer.desktop"
cp "$ROOT/packaging/icon.png" "$WORK/$APP_NAME.AppDir/mdviewer.png"

cat > "$WORK/$APP_NAME.AppDir/AppRun" <<'EOF'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/usr/bin/MDViewer" "$@"
EOF
chmod +x "$WORK/$APP_NAME.AppDir/AppRun"

if [ ! -x "$TOOL" ]; then
  echo "==> fetching appimagetool"
  mkdir -p "$TOOL_DIR"
  curl -L --fail --silent --show-error \
    "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage" \
    -o "$TOOL"
  chmod +x "$TOOL"
fi

echo "==> packaging AppImage"
mkdir -p "$RELEASE"
ARCH=x86_64 APPIMAGE_EXTRACT_AND_RUN=1 "$TOOL" \
  "$WORK/$APP_NAME.AppDir" \
  "$RELEASE/${APP_NAME}-${VERSION}-x86_64.AppImage"

chmod +x "$RELEASE/${APP_NAME}-${VERSION}-x86_64.AppImage"
echo "==> done: $RELEASE/${APP_NAME}-${VERSION}-x86_64.AppImage"
