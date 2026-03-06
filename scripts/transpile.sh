#!/bin/bash
# Transpiles organized src/retronism/ → flat mcp/minecraft/src/net/minecraft/src/
# Rewrites packages and removes internal imports so RetroMCP can compile
set -e
BASE="c:/Users/lucas/RetroNism"
SRC="$BASE/src/retronism"
DEST="$BASE/mcp/minecraft/src/net/minecraft/src"

# Remove old transpiled mod files (only RetroNism_ prefixed + mod_RetroNism)
find "$DEST" -maxdepth 1 -name "RetroNism_*.java" -delete 2>/dev/null || true
find "$DEST" -maxdepth 1 -name "mod_RetroNism.java" -delete 2>/dev/null || true

count=0
find "$SRC" -name "*.java" | while read -r file; do
    filename=$(basename "$file")
    sed \
        -e 's/^package retronism\(\.[a-z]*\)\?;/package net.minecraft.src;/' \
        -e '/^import retronism\./d' \
        -e '/^import net\.minecraft\.src\.\*;/d' \
        "$file" > "$DEST/$filename"
    count=$((count + 1))
done

echo "Transpiled $(find "$SRC" -name '*.java' | wc -l) files to $DEST"
