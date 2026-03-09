#!/bin/bash
# Transpiles organized src/retronism/ -> flat mcp/minecraft/src/net/minecraft/src/
# Rewrites packages and removes internal imports so RetroMCP can compile
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$BASE/src/retronism"
DEST="$BASE/mcp/minecraft/src/net/minecraft/src"

# Remove old transpiled mod files (only Retronism_ prefixed + mod_Retronism)
find "$DEST" -maxdepth 1 -name "Retronism_*.java" -delete 2>/dev/null || true
find "$DEST" -maxdepth 1 -name "mod_Retro*.java" -delete 2>/dev/null || true

find "$SRC" -name "*.java" | while read -r file; do
    filename=$(basename "$file")
    sed \
        -e 's/^package retronism\(\.[a-z]*\)\?;/package net.minecraft.src;/' \
        -e '/^import retronism\./d' \
        -e '/^import static retronism\./d' \
        -e '/^import net\.minecraft\.src\.\*;/d' \
        "$file" > "$DEST/$filename"
done

echo "Transpiled $(find "$SRC" -name '*.java' | wc -l) files to $DEST"

# Copy assets (textures, models) to temp/merged for jar injection
ASSETS="$SRC/assets"
if [ -d "$ASSETS" ]; then
    mkdir -p "$BASE/temp/merged"
    cp -r "$ASSETS"/* "$BASE/temp/merged/"
    echo "Copied assets to temp/merged/"
fi