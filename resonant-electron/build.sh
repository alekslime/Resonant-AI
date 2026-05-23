#!/bin/bash
# Resonant build script
# Compiles Python binaries with PyInstaller, then packages with electron-builder.
# Run this from the resonant-electron/ directory.
# Requirements: Python venv at ../.venv, Node.js, npm

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VENV="$SCRIPT_DIR/../.venv"
DIST="$SCRIPT_DIR/dist-python"

echo ""
echo "=== Resonant Build Script ==="
echo ""

# ── 1. Activate venv ──────────────────────────────────────────────────────────
if [ ! -f "$VENV/bin/activate" ]; then
  echo "ERROR: Virtual environment not found at $VENV"
  echo "       Run: python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt"
  exit 1
fi

source "$VENV/bin/activate"

# ── 2. Install PyInstaller if needed ─────────────────────────────────────────
if ! python -c "import PyInstaller" 2>/dev/null; then
  echo "Installing PyInstaller..."
  pip install pyinstaller --quiet
fi

# ── 3. Compile server.py ──────────────────────────────────────────────────────
echo "Compiling server.py..."
pyinstaller \
  --onefile \
  --distpath "$DIST" \
  --workpath "$SCRIPT_DIR/build-pyinstaller/server" \
  --specpath "$SCRIPT_DIR/build-pyinstaller" \
  --name server \
  --noconfirm \
  "$SCRIPT_DIR/../server.py"

echo "server binary → $DIST/server"

# ── 4. Compile agent.py ───────────────────────────────────────────────────────
echo "Compiling agent.py..."
pyinstaller \
  --onefile \
  --distpath "$DIST" \
  --workpath "$SCRIPT_DIR/build-pyinstaller/agent" \
  --specpath "$SCRIPT_DIR/build-pyinstaller" \
  --name agent \
  --noconfirm \
  "$SCRIPT_DIR/../agent.py"

echo "agent binary → $DIST/agent"

# ── 5. Install Node deps ──────────────────────────────────────────────────────
echo ""
echo "Installing Node dependencies..."
cd "$SCRIPT_DIR"
npm install --silent

# ── 6. Build Electron app ─────────────────────────────────────────────────────
echo ""
echo "Building Electron app..."

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
  npm run build:win
else
  # On Linux we build both Linux and Windows (Windows needs Wine installed)
  npm run build:linux
  echo ""
  echo "To also build the Windows installer, install Wine and run: npm run build:win"
fi

echo ""
echo "=== Build complete ==="
echo "Output: $SCRIPT_DIR/dist/"
echo ""
