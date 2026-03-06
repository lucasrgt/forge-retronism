#!/bin/bash
set -e
BASE="c:/Users/lucas/RetroNism"
cd "$BASE"

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Recompiling ==="
cd "$BASE/mcp"
echo "recompile" | java -jar RetroMCP-Java-CLI.jar
cd "$BASE"

JUNIT="$BASE/tests/libs/junit-4.13.2.jar"
HAMCREST="$BASE/tests/libs/hamcrest-core-1.3.jar"
BIN="$BASE/mcp/minecraft/bin"
TEST_SRC="$BASE/tests/src"
TEST_OUT="$BASE/tests/out"

# Ensure mod is compiled in bin/
if [ ! -f "$BIN/net/minecraft/src/RetroNism_TileFluidPipe.class" ]; then
    echo "ERROR: Mod classes not found in bin/. Run recompile first."
    exit 1
fi

# Clean and compile tests
rm -rf "$TEST_OUT"
mkdir -p "$TEST_OUT"

echo "=== Compiling tests ==="
javac -source 1.8 -target 1.8 \
    -cp "$BIN;$JUNIT;$HAMCREST" \
    -d "$TEST_OUT" \
    "$TEST_SRC"/net/minecraft/src/*Test.java

echo "=== Running tests ==="
java -cp "$TEST_OUT;$BIN;$JUNIT;$HAMCREST" \
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
    net.minecraft.src.SideConfigTest
