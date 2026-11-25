#!/usr/bin/env python3
"""Actualiza apps.json con el tamaño, la huella SHA-256 y la fecha de cada APK.

Uso (desde la carpeta tienda/):   python3 actualizar_tienda.py
Solo usa la biblioteca estándar de Python. Si un APK no cambió, no toca su fecha.
"""
import hashlib, json, os, datetime
AQUI = os.path.dirname(os.path.abspath(__file__))
RUTA = os.path.join(AQUI, 'apps.json')
datos = json.load(open(RUTA, encoding='utf-8'))
hoy = datetime.date.today().isoformat()
for a in datos['apps']:
    apk = os.path.join(AQUI, a['apk'])
    if not os.path.exists(apk):
        print('FALTA el archivo', a['apk']); continue
    b = open(apk, 'rb').read()
    sha = hashlib.sha256(b).hexdigest()
    if a.get('sha256') != sha:
        a['actualizado'] = hoy
    a['sha256'] = sha
    a['bytes'] = len(b)
    print(f"{a['nombre']}: {len(b)/1048576:.2f} MB  {sha[:12]}…  actualizada {a['actualizado']}")
json.dump(datos, open(RUTA, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
open(RUTA, 'a').write('\n')
