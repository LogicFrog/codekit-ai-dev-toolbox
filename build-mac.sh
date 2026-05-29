#!/bin/bash
set -e

echo "=== CodeKit 构建 ==="
ROOT="$(cd "$(dirname "$0")" && pwd)"
DMGBUILD_HOME="$HOME/.codekit/dmgbuild"

echo "[1/4] 构建后端 JAR..."
cd "$ROOT"
./mvnw clean package -DskipTests -q
JAR=$(ls target/codekit-*-SNAPSHOT.jar | head -1)
cp "$JAR" web/codekit-client/build/codekit.jar

echo "[2/4] 下载 DMG 构建工具..."
if [ ! -f "$DMGBUILD_HOME/dmgbuild" ]; then
  mkdir -p "$DMGBUILD_HOME"
  curl -L -o /tmp/dmgbuild.tar.gz \
    "https://github.com/electron-userland/electron-builder-binaries/releases/download/dmg-builder%401.2.0/dmgbuild-bundle-arm64-75c8a6c.tar.gz"
  tar xzf /tmp/dmgbuild.tar.gz -C "$DMGBUILD_HOME"
  rm /tmp/dmgbuild.tar.gz
  chmod +x "$DMGBUILD_HOME/dmgbuild"
fi

echo "[3/4] 构建前端..."
cd "$ROOT/web/codekit-client"
npx vite build

echo "[4/4] 打包 macOS DMG..."
export CUSTOM_DMGBUILD_PATH="$DMGBUILD_HOME/dmgbuild"
export ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
npx electron-builder --mac --publish=never

echo ""
echo "=== 完成 ==="
ls -lh "$ROOT/web/codekit-client/dist-electron/CodeKit-"*.dmg
echo ""
echo "双击 .dmg 安装即可使用。"
