#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=$(sed -n 's/.*gradle-\([0-9.]*\)-bin.zip.*/\1/p' "$APP_HOME/gradle/wrapper/gradle-wrapper.properties")
CACHE_DIR="${HOME}/.gradle/dynamic-island-wrapper/${GRADLE_VERSION}"
if [ ! -x "$CACHE_DIR/bin/gradle" ]; then
  mkdir -p "$CACHE_DIR"
  TMP="${CACHE_DIR}/gradle.zip"
  curl -fsSL --retry 3 "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$TMP"
  unzip -q "$TMP" -d "$CACHE_DIR/unpacked"
  mv "$CACHE_DIR/unpacked/gradle-${GRADLE_VERSION}"/* "$CACHE_DIR/"
  rm -rf "$CACHE_DIR/unpacked" "$TMP"
fi
exec "$CACHE_DIR/bin/gradle" "$@"
