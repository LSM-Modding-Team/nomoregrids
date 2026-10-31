#!/usr/bin/env python3
"""Optional parser-only Java check. No type resolution, compilation or execution."""
from pathlib import Path
import sys

try:
    from tree_sitter import Language, Parser
    import tree_sitter_java
except ImportError:
    raise SystemExit("Requires tree-sitter==0.25.2 and tree-sitter-java==0.23.5; see tools/README.md")

root = Path(__file__).resolve().parents[1]
parser = Parser(Language(tree_sitter_java.language()))
failed = False
paths = sorted([*(root / "src").rglob("*.java"), *(root / "tools/validation").rglob("*.java")])
for path in paths:
    tree = parser.parse(path.read_bytes())
    if not tree.root_node.has_error:
        continue
    failed = True
    pending = [tree.root_node]
    while pending:
        node = pending.pop()
        if node.is_error or node.is_missing:
            print(f"{path.relative_to(root)}:{node.start_point.row + 1}: syntax {node.type}", file=sys.stderr)
        pending.extend(reversed(node.children))
if failed:
    raise SystemExit(1)
print(f"Parsed {len(paths)} Java sources without syntax errors; types/compilation/tests NOT CHECKED.")
