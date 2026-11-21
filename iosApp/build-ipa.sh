#!/usr/bin/env bash
set -euo pipefail

# Builds NAPH-1.0.0-ios-arm64.ipa with:
# 1. Shared Kotlin Multiplatform ComposeApp.framework (iosArm64)
# 2. Real on-device Go Sequential BitTorrent Engine (anacrolix/torrent -> libnaph_p2p_ios.a)
# 3. Real NuvioMedia/MPVKit (libmpv + MoltenVK + VideoToolbox + libass + libdovi + FFmpeg)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
APP_DIR="$BUILD_DIR/Payload/NAPH.app"
P2P_BUILD_DIR="$BUILD_DIR/p2p"
FRAMEWORK_DIR="$ROOT_DIR/composeApp/build/bin/iosArm64/releaseFramework"
SDK_PATH="$(xcrun --sdk iphoneos --show-sdk-path)"
CLANG_BIN="$(xcrun --sdk iphoneos --find clang)"

echo "==> [1/5] Compiling Kotlin Multiplatform static ComposeApp.framework (iosArm64)..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :composeApp:linkReleaseFrameworkIosArm64 --no-daemon

echo "==> [2/5] Compiling on-device iOS Go Sequential BitTorrent Engine (libnaph_p2p_ios.a)..."
rm -rf "$BUILD_DIR"
mkdir -p "$APP_DIR" "$P2P_BUILD_DIR"

(
  cd "$SCRIPT_DIR/p2p-go"
  go mod tidy
  CGO_ENABLED=1 \
  GOOS=ios \
  GOARCH=arm64 \
  CC="$CLANG_BIN" \
  CGO_CFLAGS="-arch arm64 -isysroot $SDK_PATH -miphoneos-version-min=15.0" \
  CGO_LDFLAGS="-arch arm64 -isysroot $SDK_PATH -miphoneos-version-min=15.0" \
  go build -buildmode=c-archive -trimpath -ldflags="-s -w" -o "$P2P_BUILD_DIR/libnaph_p2p_ios.a" .
)
ls -lh "$P2P_BUILD_DIR/libnaph_p2p_ios.a" "$P2P_BUILD_DIR/libnaph_p2p_ios.h"

echo "==> [3/5] Resolving NuvioMedia/MPVKit binary XCFrameworks via SwiftPM..."
(
  cd "$SCRIPT_DIR/mpv-deps"
  swift package resolve
)

MPV_SEARCH_FLAGS=()
MPV_LINK_FLAGS=()
MPV_INCLUDE_FLAGS=("-I" "$P2P_BUILD_DIR")
DYNAMIC_FRAMEWORKS=()

while IFS= read -r fw_path; do
  # Skip duplicate -GPL slices (and GPL-only Libsmbclient) + macOS-only Libluajit
  if [[ "$fw_path" == *"-GPL/"* ]] || [[ "$fw_path" == *"/Libsmbclient/"* ]] || [[ "$fw_path" == *"/Libluajit/"* ]]; then
    continue
  fi

  fw_parent="$(dirname "$fw_path")"
  fw_base="$(basename "$fw_path")"
  fw_name="${fw_base%.framework}"
  fw_bin="$fw_path/$fw_name"

  MPV_SEARCH_FLAGS+=("-F" "$fw_parent")
  MPV_LINK_FLAGS+=("-framework" "$fw_name")

  if [[ "$fw_name" == "Libmpv" && -d "$fw_path/Headers" ]]; then
    MPV_INCLUDE_FLAGS+=("-I" "$fw_path/Headers")
  fi

  if [[ -f "$fw_bin" ]]; then
    if file "$fw_bin" | grep -q "dynamically linked shared library"; then
      DYNAMIC_FRAMEWORKS+=("$fw_path")
    fi
  fi
done < <(find "$SCRIPT_DIR/mpv-deps/.build/artifacts" -type d -path "*/ios-arm64/*.framework" | sort)

echo "Resolved ${#MPV_LINK_FLAGS[@]} MPVKit framework flags (${#DYNAMIC_FRAMEWORKS[@]} dynamic frameworks)"

echo "==> [4/5] Preparing Payload/NAPH.app & linking ComposeApp + MPVKit + libnaph_p2p_ios.a..."
cp "$SCRIPT_DIR/iosApp/Info.plist" "$APP_DIR/Info.plist"
plutil -lint "$APP_DIR/Info.plist"

ICON_SRC="$SCRIPT_DIR/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png"
sips -z 120 120 "$ICON_SRC" --out "$APP_DIR/AppIcon60x60@2x.png" >/dev/null
sips -z 180 180 "$ICON_SRC" --out "$APP_DIR/AppIcon60x60@3x.png" >/dev/null
sips -z 152 152 "$ICON_SRC" --out "$APP_DIR/AppIcon76x76@2x~ipad.png" >/dev/null
printf "APPL????" > "$APP_DIR/PkgInfo"

if [[ ${#DYNAMIC_FRAMEWORKS[@]} -gt 0 ]]; then
  mkdir -p "$APP_DIR/Frameworks"
  for dyn_fw in "${DYNAMIC_FRAMEWORKS[@]}"; do
    cp -R "$dyn_fw" "$APP_DIR/Frameworks/"
  done
fi

xcrun --sdk iphoneos swiftc \
  -target arm64-apple-ios15.0 \
  -sdk "$SDK_PATH" \
  -O \
  -parse-as-library \
  -import-objc-header "$SCRIPT_DIR/iosApp/NaphBridgingHeader.h" \
  "${MPV_INCLUDE_FLAGS[@]}" \
  -F "$FRAMEWORK_DIR" \
  "${MPV_SEARCH_FLAGS[@]}" \
  -framework ComposeApp \
  "${MPV_LINK_FLAGS[@]}" \
  "$P2P_BUILD_DIR/libnaph_p2p_ios.a" \
  -framework UIKit \
  -framework SwiftUI \
  -framework Foundation \
  -framework AVFoundation \
  -framework AVKit \
  -framework CoreAudio \
  -framework CoreMedia \
  -framework CoreGraphics \
  -framework CoreVideo \
  -framework QuartzCore \
  -framework Metal \
  -framework VideoToolbox \
  -framework CoreText \
  -framework ImageIO \
  -framework AudioToolbox \
  -framework Security \
  -framework CFNetwork \
  -framework SystemConfiguration \
  -framework Network \
  -framework UniformTypeIdentifiers \
  -framework MobileCoreServices \
  -framework GameController \
  -framework IOSurface \
  -lsqlite3 \
  -lbz2 \
  -liconv \
  -lexpat \
  -lresolv \
  -lxml2 \
  -lc++ \
  -lz \
  -Xlinker -rpath -Xlinker @executable_path/Frameworks \
  "$SCRIPT_DIR/iosApp/iOSApp.swift" \
  "$SCRIPT_DIR/iosApp/NaphMpvMetalPlayer.swift" \
  "$SCRIPT_DIR/iosApp/NaphGoTorrentBridge.swift" \
  -o "$APP_DIR/NAPH"

chmod +x "$APP_DIR/NAPH"
file "$APP_DIR/NAPH"

echo "==> [5/5] Ad-hoc codesign & packaging NAPH-1.0.0-ios-arm64.ipa..."
if [[ -d "$APP_DIR/Frameworks" ]]; then
  for fw in "$APP_DIR/Frameworks/"*.framework; do
    [[ -d "$fw" ]] && codesign --force --sign - "$fw"
  done
fi
codesign --force --deep --sign - "$APP_DIR"

(cd "$BUILD_DIR" && zip -qry "$BUILD_DIR/NAPH-1.0.0-ios-arm64.ipa" Payload)
ls -lh "$BUILD_DIR/NAPH-1.0.0-ios-arm64.ipa"
echo "==> Done! Ready for AltStore: $BUILD_DIR/NAPH-1.0.0-ios-arm64.ipa"
