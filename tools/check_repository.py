#!/usr/bin/env python3
"""Read-only structural checks. Never invokes Gradle, Java, a compiler or Minecraft."""
from pathlib import Path
import hashlib
import json
import re
import sys
import tomllib

ROOT = Path(__file__).resolve().parents[1]


def check():
    errors = []
    for path in ROOT.glob("src/**/*.json"):
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except (ValueError, UnicodeError) as exc:
            errors.append(f"{path.relative_to(ROOT)}: {exc}")

    properties = dict(line.split("=", 1) for line in
                      (ROOT / "gradle.properties").read_text().splitlines()
                      if line.strip() and not line.lstrip().startswith("#"))
    template = (ROOT / "src/main/templates/META-INF/neoforge.mods.toml").read_text()
    for name, value in properties.items():
        template = template.replace("${" + name + "}", value)
    try:
        if "${" in template:
            errors.append("Unresolved mod metadata placeholder")
        tomllib.loads(template)
    except ValueError as exc:
        errors.append(f"Mod metadata: {exc}")
    if properties["mod_license"] != "MIT" or "MIT License" not in (ROOT / "LICENSE").read_text():
        errors.append("License metadata differs from the existing MIT license")

    wrapper = ROOT / "gradle/wrapper/gradle-wrapper.jar"
    expected = "423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9"
    if not wrapper.exists() or hashlib.sha256(wrapper.read_bytes()).hexdigest() != expected:
        errors.append("Missing or altered Gradle 9.2.1 wrapper")

    vault = ROOT / "docs/obsidian/NoMoreGrids"
    notes = list(vault.rglob("*.md"))
    titles = [path.stem for path in notes]
    if len(set(titles)) != len(titles):
        errors.append("Ambiguous Obsidian note names")
    for path in notes:
        for target in re.findall(r"\[\[([^\]]+)\]\]", path.read_text()):
            target = target.split("|", 1)[0].split("#", 1)[0]
            if target and target not in titles:
                errors.append(f"{path.name}: missing wiki target {target}")

    sources = ROOT / "src/main/java/net/nicomar/nomoregrids"
    for folder in ("core", "geometry"):
        for path in (sources / folder).rglob("*.java"):
            text = path.read_text()
            if re.search(r"import\s+(?:net\.minecraft|net\.neoforged|.*\.client\.)", text):
                errors.append(f"{path.name}: pure core depends on platform/client")

    for error in errors:
        print(error, file=sys.stderr)
    if errors:
        return 1
    print("Static checks passed: JSON, mod TOML, license, wrapper, wiki links and pure-core imports.")
    print("Java compilation, Java tests and Minecraft execution: NOT RUN.")
    return 0


if __name__ == "__main__":
    raise SystemExit(check())
