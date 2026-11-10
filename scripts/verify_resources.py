#!/usr/bin/env python3
"""Offline structural checks; this does not compile or exercise Android."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'android/app/src/main'
LANGS = ['values', 'values-pt-rPT', 'values-en-rUS', 'values-ja-rJP', 'values-es-rES']
keys = {}
for lang in LANGS:
    values = []
    for file in (MAIN / 'res' / lang).glob('*.xml'):
        for item in ET.parse(file).getroot():
            if item.tag in ('string', 'plurals', 'string-array'):
                values.append(item.attrib['name'])
    assert len(values) == len(set(values)), f'Duplicate key in {lang}'
    keys[lang] = set(values)
    assert keys[lang], f'Empty locale: {lang}'
for lang in LANGS:
    assert keys[lang] == keys['values'], f'Missing/extra keys: {lang}'
for file in (MAIN / 'res').rglob('*.xml'):
    ET.parse(file)
references = set()
for file in (MAIN / 'java').rglob('*.kt'):
    references.update(re.findall(r'R\.string\.(\w+)', file.read_text()))
assert references <= keys['values'], f'Unresolved resources: {references - keys["values"]}'
manifest = ET.parse(MAIN / 'AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
permissions = {p.attrib[ns + 'name'] for p in manifest.findall('uses-permission')}
assert not permissions & {'android.permission.SEND_SMS', 'android.permission.CALL_PHONE', 'android.permission.READ_SMS', 'android.permission.READ_CALL_LOG'}
print(f'PASS: {len(keys["values"])} keys in each of 5 locales; {len(references)} Kotlin resource references; XML and prohibited permissions checks.')
