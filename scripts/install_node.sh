#!/usr/bin/env bash
set -euo pipefail

DEST_DIR="${1:-frontend/node}"
mkdir -p "$DEST_DIR"

# Ordered list of Node versions to try (major.minor.patch with leading 'v')
VERSIONS=(
  "v20.24.2"
  "v20.24.1"
  "v20.24.0"
  "v20.23.0"
  "v20.22.1"
  "v18.20.0"
)

ARCH="$(uname -m)"
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
if [ "$ARCH" = "x86_64" ]; then ARCH="x64"; fi

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

for ver in "${VERSIONS[@]}"; do
  filename="node-${ver}-${OS}-${ARCH}.tar.xz"
  url="https://nodejs.org/dist/${ver}/${filename}"
  echo "Trying $url"
  if curl -sSfL "$url" -o "$TMPDIR/$filename"; then
    echo "Downloaded $filename"
    tar -xf "$TMPDIR/$filename" -C "$TMPDIR"
    extracted_dir=$(tar -tf "$TMPDIR/$filename" | head -1 | cut -f1 -d"/")
    if [ -d "$TMPDIR/$extracted_dir" ]; then
      # Copy extracted contents into DEST_DIR
      mkdir -p "$DEST_DIR"
      # Remove any previous content
      rm -rf "$DEST_DIR"/* || true
      mv "$TMPDIR/$extracted_dir"/* "$DEST_DIR/"
      echo "Installed $ver into $DEST_DIR"
      exit 0
    fi
  else
    echo "Failed to download $url" >&2
  fi
done

echo "Failed to download any Node version from list: ${VERSIONS[*]}" >&2
exit 2
