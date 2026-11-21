#!/usr/bin/env bash
set -euo pipefail

# Builds NAPH-1.0.0-ios-arm64.ipa (Unsigned Sideloadable IPA for AltStore / SideStore / Sideloadly / TrollStore)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "==> [1/3] Compiling Kotlin Multiplatform ComposeApp.framework for iOS arm64..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :composeApp:linkReleaseFrameworkIosArm64 --no-daemon

echo "==> [2/3] Building NAPH.app bundle via xcodebuild..."
xcodebuild \
  -project "$SCRIPT_DIR/iosApp.xcodeproj" \
  -scheme iosApp \
  -configuration Release \
  -sdk iphoneos \
  -derivedDataPath "$SCRIPT_DIR/build/DerivedData" \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGNING_ALLOWED=NO \
  build

echo "==> [3/3] Packaging Payload/NAPH.app into NAPH-1.0.0-ios-arm64.ipa..."
rm -rf "$SCRIPT_DIR/build/Payload" "$SCRIPT_DIR/build/NAPH-1.0.0-ios-arm64.ipa"
mkdir -p "$SCRIPT_DIR/build/Payload"
cp -R "$SCRIPT_DIR/build/DerivedData/Build/Products/Release-iphoneos/NAPH.app" "$SCRIPT_DIR/build/Payload/NAPH.app"
(cd "$SCRIPT_DIR/build" && zip -qry NAPH-1.0.0-ios-arm64.ipa Payload)

echo "==> Built: $SCRIPT_DIR/build/NAPH-1.0.0-ios-arm64.ipa"
