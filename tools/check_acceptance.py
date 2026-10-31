#!/usr/bin/env python3
"""Static traceability audit. Never runs Java, tests, the game or benchmark scenes."""
import json
import re
from pathlib import Path

root=Path(__file__).resolve().parents[1]
catalog=json.loads((root/'docs/acceptance/catalog.json').read_text())
spec=(root/catalog['specification']).read_text()
expected={m[0]:m[1].strip() for m in re.findall(r'^\| ([A-Z]\d\d) \| [^|]+ \| ([^|]+) \|',spec,re.M)}
seen=set()
for row in catalog['cases']:
    ident=row['id']
    assert ident not in seen, f'Duplicate {ident}'
    seen.add(ident)
    assert row['expected']==expected[ident], f'Specification drift: {ident}'
    assert row['procedure'] and row['required_evidence'], f'Incomplete procedure: {ident}'
    assert row['status']=='PENDING_EXECUTION_OR_REVIEW', f'Catalog must not claim a result: {ident}'
    for ref in row['unit_sources']:
        path,method=ref.split('#')
        source=(root/path).read_text()
        assert re.search(r'\bvoid\s+'+re.escape(method)+r'\s*\(',source), f'Missing method: {ref}'
    for case in row['world_sources']:
        assert (root/case['file']).is_file(), f'Missing world source: {case}'
        assert case['scope'], f'Missing scope restriction: {ident}'
        source=(root/case['file']).read_text()
        command=case['command'].split()
        assert 'literal("'+command[0]+'")' in source, f'Missing command root: {ident}'
        if command[0]=='nmg_accept':assert '"'+command[2]+'"' in source, f'Missing world case: {ident}'
assert seen==set(expected), f'Missing/extra IDs: {seen ^ set(expected)}'
print(f'{len(seen)} matrix IDs have procedures and valid source references. Execution and acceptance NOT CHECKED.')
