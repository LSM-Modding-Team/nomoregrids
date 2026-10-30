#!/usr/bin/env python3
"""Read-only explicit import lookup in inspected archives; never resolves dependencies or invokes Java."""
import argparse
from pathlib import Path
import re
import sys
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--minecraft', type=Path, required=True, help='Official Minecraft client jar for the pinned version')
parser.add_argument('--neoforge-sources', type=Path, required=True, help='NeoForge sources jar for the pinned version')
args = parser.parse_args()
with zipfile.ZipFile(args.minecraft) as archive:
    minecraft = set(archive.namelist())
with zipfile.ZipFile(args.neoforge_sources) as archive:
    neoforge = set(archive.namelist())
errors = []
checked = set()
root = Path(__file__).resolve().parents[1]
for path in (root / 'src').rglob('*.java'):
    for name in re.findall(r'^import\s+(net\.(?:minecraft|neoforged\.neoforge)\.[\w.]+);', path.read_text(), re.M):
        checked.add(name)
        target = name.replace('.', '/')
        available = target + '.class' in minecraft if name.startswith('net.minecraft.') else target + '.java' in neoforge
        if not available:
            errors.append(f'{path.relative_to(root)}: {name} absent in inspected archive')
for error in errors:
    print(error, file=sys.stderr)
print(f'Checked {len(checked)} explicit Minecraft/NeoForge import names; missing={len(errors)}.')
print('Wildcards, members, types, patched Minecraft signatures and compilation: NOT CHECKED.')
sys.exit(bool(errors))
