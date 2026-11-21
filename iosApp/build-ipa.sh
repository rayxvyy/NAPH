#!/usr/bin/env bash
set -euo pipefail

# Builds a real Mach-O arm64 NAPH-1.0.0-ios-arm64.ipa for AltStore / SideStore / Sideloadly / TrollStore
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
APP_DIR="$BUILD_DIR/Payload/NAPH.app"
FRAMEWORK_DIR="$ROOT_DIR/composeApp/build/bin/iosArm64/releaseFramework"

echo "==> [1/4] Compiling Kotlin Multiplatform static ComposeApp.framework (iosArm64)..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :composeApp:linkReleaseFrameworkIosArm64 --no-daemon

echo "==> [2/4] Preparing Payload/NAPH.app bundle..."
rm -rf "$BUILD_DIR"
mkdir -p "$APP_DIR"

cp "$SCRIPT_DIR/iosApp/Info.plist" "$APP_DIR/Info.plist"
plutil -lint "$APP_DIR/Info.plist"

ICON_SRC="$SCRIPT_DIR/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png"
sips -z 120 120 "$ICON_SRC" --out "$APP_DIR/AppIcon60x60@2x.png" >/dev/null
sips -z 180 180 "$ICON_SRC" --out "$APP_DIR/AppIcon60x60@3x.png" >/dev/null
sips -z 152 152 "$ICON_SRC" --out "$APP_DIR/AppIcon76x76@2x~ipad.png" >/dev/null
printf "APPL????" > "$APP_DIR/PkgInfo"

echo "==> [3/4] Compiling Swift host & linking static ComposeApp.framework into Mach-O arm64 executable..."
SDK_PATH="$(xcrun --sdk iphoneos --show-sdk-path)"
xcrun --sdk iphoneos swiftc \
  -target arm64-apple-ios15.0 \
  -sdk "$SDK_PATH" \
  -O \
  -F "$FRAMEWORK_DIR" \
  -framework ComposeApp \
  -framework UIKit \
  -framework SwiftUI \
  -framework Foundation \
  -framework AVFoundation \
  -framework AVKit \
  -framework CoreMedia \
  -framework CoreGraphics \
  -framework QuartzCore \
  -framework Metal \
  -framework CoreText \
  -framework ImageIO \
  -framework AudioToolbox \
  -framework Security \
  -framework CFNetwork \
  -lsqlite3 \
  -lc++ \
  -lz \
  "$SCRIPT_DIR/iosApp/iOSApp.swift" \
  "$SCRIPT_DIR/iosApp/NaphTorrentBridge.swift" \
  -o "$APP_DIR/NAPH"

chmod +x "$APP_DIR/NAPH"
file "$APP_DIR/NAPH"

echo "==> [4/4] Applying ad-hoc codesign & packaging NAPH-1.0.0-ios-arm64.ipa..."
codesign --force --deep --sign - "$APP_DIR"
(cd "$BUILD_DIR" && zip -qry "$BUILD_DIR/NAPH-1.0.0-ios-arm64.ipa" Payload)

ls -lh "$BUILD_DIR/NAPH-1.0.0-ios-arm64.ipa"
echo "==> Done! Ready for AltStore: $BUILD_DIR/NAPH-1.0.0-ios-arm64.ipa"
