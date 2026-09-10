#!/bin/sh
set -eu

MVN_VERSION="3.9.16"
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DIST_DIR="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION"
ZIP="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION-bin.zip"
URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.zip"

if [ ! -x "$DIST_DIR/bin/mvn" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$URL" -O "$ZIP"
  else
    echo "curl or wget is required for the first Maven wrapper run" >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP" -d "$BASE_DIR/.mvn"
  else
    echo "unzip is required for the first Maven wrapper run" >&2
    exit 1
  fi
  rm -f "$ZIP"
fi

exec "$DIST_DIR/bin/mvn" "$@"
