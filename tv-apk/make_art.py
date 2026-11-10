#!/usr/bin/env python3
"""Genera el icono (mipmap-*) y el banner de la tele (320x180) de la app."""
import json, pathlib, random
from PIL import Image, ImageDraw, ImageFont, ImageFilter
root = pathlib.Path(__file__).parent
L = pathlib.Path('/home/claude/launcher')
res = root / 'app/src/main/res'
# ---- iconos
ic = Image.open(L / 'assets/icon-512.png').convert('RGBA')
for d, s in {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}.items():
    ic.resize((s, s), Image.LANCZOS).save(res / f'mipmap-{d}/ic_launcher.png')
# ---- banner: noche azul, nieve, la corona con la foto de Alict, el nombre y los personajes
S = 4; W, H = 320 * S, 180 * S
random.seed(11)
im = Image.new('RGB', (W, H)); d = ImageDraw.Draw(im)
top, bot = (7, 21, 56), (62, 115, 184)
for y in range(H):
    t = y / H; d.line([(0, y), (W, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
for _ in range(70):
    x, y, r = random.randrange(W), random.randrange(int(H * .75)), random.choice([2, 3, 4])
    d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255))
d.ellipse([-W * .1, H * .86, W * 1.1, H * 1.5], fill='#eaf4ff'); d.ellipse([-W * .05, H * .92, W * 1.05, H * 1.6], fill='#ffffff')
# personajes sobre la nieve
atl = json.load(open(L / 'src/tv/atlas.json')); cat = json.load(open(L / 'src/tv/catalogo.json'))
ims = [Image.open(L / f'src/tv/atlas{i}.webp').convert('RGBA') for i in (0, 1)]
def sprite(fid, alto):
    f = next(x for x in cat['luchadores'] if x['id'] == fid); a = atl[f['e']['idle'][0]]
    sp = ims[a[0]].crop((a[1], a[2], a[1] + a[3], a[2] + a[4])); k = alto / a[4]
    return sp.resize((max(1, int(a[3] * k)), int(a[4] * k)), Image.NEAREST if f['px'] else Image.LANCZOS)
fila = [('mario', .36), ('sonic', .36), ('spiderpx', .37), ('yoshi', .34), ('bluey', .36), ('kirby', .30), ('pikachu', .30)]
x = int(W * .02)
for fid, hh in fila:
    try: sp = sprite(fid, int(H * hh))
    except StopIteration: continue
    im.paste(sp, (x, int(H * .965) - sp.height), sp); x += sp.width + int(W * .018)
# corona con la foto (icono) a la izquierda
c = ic.resize((int(H * .62), int(H * .62)), Image.LANCZOS); im.paste(c, (int(W * .035), int(H * .06)), c)
# texto
f1 = ImageFont.truetype('/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf', int(H * .185))
f2 = ImageFont.truetype('/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf', int(H * .105))
tx = int(W * .375)
def txt(xy, s, f, fill, sh=(0, 0, 0)):
    d.text((xy[0] + 5, xy[1] + 6), s, font=f, fill=sh); d.text(xy, s, font=f, fill=fill)
txt((tx, int(H * .10)), 'Juegos de', f1, '#ffffff'); txt((tx, int(H * .285)), 'Alict', f1, '#ffd24d')
txt((tx, int(H * .50)), '¡A jugar!', f2, '#cfe6ff')
im = im.resize((320, 180), Image.LANCZOS); im.save(res / 'drawable-xhdpi/banner.png', optimize=True)
print('arte listo')
