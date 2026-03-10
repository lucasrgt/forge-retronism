#!/bin/bash
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$BASE"

# Convert MSYS /c/ paths to Windows C:/ for javac/java compatibility
win_path() {
    echo "$1" | sed 's|^/\([a-zA-Z]\)/|\1:/|'
}

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Recompiling ==="
cd "$BASE/mcp"
echo "recompile" | java -jar RetroMCP-Java-CLI.jar
cd "$BASE"

# Windows paths for javac/java classpath
WBIN="$(win_path "$BASE/mcp/minecraft/bin")"
WJUNIT="$(win_path "$BASE/tests/libs/junit-4.13.2.jar")"
WHAMCREST="$(win_path "$BASE/tests/libs/hamcrest-core-1.3.jar")"
WTEST_OUT="$(win_path "$BASE/tests/out")"

# Ensure mod is compiled in bin/
if [ ! -f "$BASE/mcp/minecraft/bin/net/minecraft/src/Retronism_TileFluidPipe.class" ]; then
    echo "ERROR: Mod classes not found in bin/. Run recompile first."
    exit 1
fi

# Clean and compile tests
rm -rf "$BASE/tests/out"
mkdir -p "$BASE/tests/out"

echo "=== Compiling tests ==="
# Collect test files using MSYS paths (for glob), convert to Windows paths for javac
TEST_FILES=()
for f in "$BASE"/tests/src/net/minecraft/src/*Test.java; do
    TEST_FILES+=("$(win_path "$f")")
done
javac -source 1.8 -target 1.8 \
    -cp "$WBIN;$WJUNIT;$WHAMCREST" \
    -d "$WTEST_OUT" \
    "${TEST_FILES[@]}"

echo "=== Running tests ==="
java -cp "$WTEST_OUT;$WBIN;$WJUNIT;$WHAMCREST" \
    org.junit.runner.JUnitCore \
    net.minecraft.src.FluidTypeTest \
    net.minecraft.src.FluidTankTest \
    net.minecraft.src.GasTypeTest \
    net.minecraft.src.GasTankTest \
    net.minecraft.src.FluidPipeTest \
    net.minecraft.src.GasPipeTest \
    net.minecraft.src.ElectrolysisTest \
    net.minecraft.src.PumpTest \
    net.minecraft.src.PumpSlotTest \
    net.minecraft.src.SideConfigTest \
    net.minecraft.src.ItemPipeTest \
    net.minecraft.src.MegaPipeItemTest \
    net.minecraft.src.PortRegistryTest \
    net.minecraft.src.AnimClipTest \
    net.minecraft.src.AnimationDefStateTest
