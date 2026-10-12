import random, math
import sys, os, math
from PIL import Image, ImageDraw, ImageFilter, ImageFont
OUT = sys.argv[1]   # carpeta textures/gui/gasha
S = 2; W, H = 1536 * S, 1024 * S
rnd = random.Random(5)
def F(sz, bold=True): return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', sz * S)
def s(v): return int(v * S)
ORO=(255,205,45); ORO_D=(176,112,20); ORO_L=(255,238,150); ROSA=(255,120,190); ROSA_D=(150,40,110); MOR=(120,70,200); MOR_D=(46,24,84); NOCHE=(22,10,48); BL=(250,246,255)

# ---------- fondo: noche espacial violeta
bg = Image.new('RGB', (W, H)); d = ImageDraw.Draw(bg)
for y in range(H):
    t = y / H
    d.line([(0, y), (W, y)], fill=(int(24 + 40 * t), int(10 + 8 * t), int(56 + 40 * t)))
glow = Image.new('RGB', (W, H), (0, 0, 0)); g = ImageDraw.Draw(glow)
for cx, cy, r, col in ((768, 430, 380, (150, 70, 230)), (150, 200, 220, (230, 80, 170)), (1250, 120, 200, (90, 80, 240)), (300, 800, 260, (180, 60, 200))):
    g.ellipse([s(cx - r), s(cy - r), s(cx + r), s(cy + r)], fill=col)
glow = glow.filter(ImageFilter.GaussianBlur(s(150)))
bg = Image.blend(bg, Image.eval(glow, lambda v: v), 0.55)
d = ImageDraw.Draw(bg)
for _ in range(420):
    x, y = rnd.randrange(W), rnd.randrange(H); r = rnd.choice([1, 1, 1, 2, 2, 3]) * S // 1
    c = rnd.choice([(255, 255, 255), (255, 230, 250), (200, 220, 255), (255, 240, 170)])
    d.ellipse([x - r // 2, y - r // 2, x + r // 2, y + r // 2], fill=c)
def estrella(cx, cy, r, col, dd):
    pts = []
    for i in range(8):
        a = i * math.pi / 4; rr = r if i % 2 == 0 else r * 0.28
        pts.append((cx + rr * math.cos(a - math.pi / 2), cy + rr * math.sin(a - math.pi / 2)))
    dd.polygon(pts, fill=col)
for _ in range(26): estrella(rnd.randrange(W), rnd.randrange(H), s(rnd.choice([6, 8, 11])), rnd.choice([(255, 240, 200), (255, 200, 240), (210, 200, 255)]), d)
def pata(cx, cy, r, col, dd):
    dd.ellipse([cx - r, cy - r * 0.2, cx + r, cy + r * 0.9], fill=col)
    for dx in (-1.1, -0.4, 0.4, 1.1):
        dd.ellipse([cx + dx * r - r * 0.32, cy - r * (1.0 if abs(dx) < 1 else 0.55) - r * 0.3, cx + dx * r + r * 0.32, cy - r * (1.0 if abs(dx) < 1 else 0.55) + r * 0.4], fill=col)
pl = Image.new('RGBA', (W, H), (0, 0, 0, 0)); pd = ImageDraw.Draw(pl)
for _ in range(14): pata(rnd.randrange(W), rnd.randrange(H), s(rnd.choice([12, 16, 20])), (255, 170, 230, 34), pd)
bg = Image.alpha_composite(bg.convert('RGBA'), pl)
# planeta con anillo y platillo (detalle alien) arriba izquierda
pl = Image.new('RGBA', (W, H), (0, 0, 0, 0)); pd = ImageDraw.Draw(pl)
pd.ellipse([s(70), s(60), s(190), s(180)], fill=(150, 90, 220, 255)); pd.ellipse([s(88), s(78), s(150), s(140)], fill=(185, 130, 245, 255))
pd.ellipse([s(40), s(105), s(220), s(135)], outline=(255, 200, 240, 255), width=s(5))
pd.pieslice([s(70), s(60), s(190), s(180)], 0, 0, fill=None)
bg = Image.alpha_composite(bg, pl); d = ImageDraw.Draw(bg)

# ---------- utilidades de marcos dorados
def marco(box, r=18, fill=(40, 16, 70, 235), borde=ORO, grosor=5, esquinas=True):
    x0, y0, x1, y1 = [s(v) for v in box]
    d.rounded_rectangle([x0 - s(4), y0 - s(4), x1 + s(4), y1 + s(4)], radius=s(r + 4), fill=ORO_D)
    d.rounded_rectangle([x0, y0, x1, y1], radius=s(r), fill=borde)
    d.rounded_rectangle([x0 + s(grosor), y0 + s(grosor), x1 - s(grosor), y1 - s(grosor)], radius=s(r - 3), fill=fill)
    d.rounded_rectangle([x0 + s(grosor + 4), y0 + s(grosor + 4), x1 - s(grosor + 4), y1 - s(grosor + 4)], radius=s(r - 6), outline=(255, 150, 215), width=max(1, S))
    if esquinas:
        for (cx, cy) in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
            d.polygon([(cx, cy - s(9)), (cx + s(9), cy), (cx, cy + s(9)), (cx - s(9), cy)], fill=ROSA, outline=ORO_L)
def texto(*a, **k): pass
def michi(cx, cy, sc, dd=None):
    dd = dd or d; u = s(sc)
    for sx in (-1, 1):                                    # antenas
        dd.line([(cx + sx * u * 2.2, cy - u * 3.4), (cx + sx * u * 2.8, cy - u * 6.0)], fill=(140, 140, 170), width=max(2, int(u * 0.5)))
        dd.ellipse([cx + sx * u * 2.8 - u * 1.1, cy - u * 6.0 - u * 1.1, cx + sx * u * 2.8 + u * 1.1, cy - u * 6.0 + u * 1.1], fill=MOR, outline=(80, 40, 150))
    dd.rounded_rectangle([cx - u * 4.2, cy - u * 3.6, cx + u * 4.2, cy + u * 3.4], radius=int(u * 1.2), fill=BL, outline=(90, 60, 130), width=max(2, int(u * 0.35)))
    for sx in (-1, 1):
        dd.polygon([(cx + sx * u * 3.9, cy - u * 3.2), (cx + sx * u * 3.2, cy - u * 4.9), (cx + sx * u * 1.9, cy - u * 3.5)], fill=ROSA)
        dd.polygon([(cx + sx * u * 1.2, cy - u * 0.9), (cx + sx * u * 2.8, cy - u * 1.4), (cx + sx * u * 2.8, cy + u * 0.5), (cx + sx * u * 1.2, cy + u * 0.4)], fill=(60, 40, 110))
        dd.ellipse([cx + sx * u * 1.9 - u * 0.35, cy - u * 0.6 - u * 0.35, cx + sx * u * 1.9 + u * 0.35, cy - u * 0.6 + u * 0.35], fill=BL)
        dd.ellipse([cx + sx * u * 3.0 - u * 0.6, cy + u * 1.6 - u * 0.5, cx + sx * u * 3.0 + u * 0.6, cy + u * 1.6 + u * 0.4], fill=(255, 170, 215))
    dd.polygon([(cx - u * 0.5, cy + u * 0.7), (cx + u * 0.5, cy + u * 0.7), (cx, cy + u * 1.2)], fill=(255, 140, 190))
    dd.arc([cx - u * 1.2, cy + u * 0.9, cx + u * 1.2, cy + u * 2.4], 20, 160, fill=(60, 40, 110), width=max(2, int(u * 0.3)))

# ---------- domo central
cx, cy = s(768), s(430)
# base / plataforma
d.ellipse([cx - s(330), s(612), cx + s(330), s(690)], fill=ORO_D)
d.ellipse([cx - s(330), s(600), cx + s(330), s(670)], fill=ORO)
d.ellipse([cx - s(300), s(606), cx + s(300), s(656)], fill=(220, 70, 150))
d.ellipse([cx - s(260), s(614), cx + s(260), s(646)], fill=(255, 130, 195))
for k in range(-3, 4):
    pata(cx + s(k * 82), s(640), s(11), (255, 235, 150), d)
# cristal
dome = Image.new('RGBA', (W, H), (0, 0, 0, 0)); dd = ImageDraw.Draw(dome)
dd.rounded_rectangle([cx - s(300), s(150), cx + s(300), s(640)], radius=s(260), fill=(170, 130, 255, 70))
dd.ellipse([cx - s(300), s(130), cx + s(300), s(250)], fill=(190, 150, 255, 50))
dome = dome.filter(ImageFilter.GaussianBlur(S))
bg = Image.alpha_composite(bg, dome); d = ImageDraw.Draw(bg)
# interior: halo
halo = Image.new('RGBA', (W, H), (0, 0, 0, 0)); hd = ImageDraw.Draw(halo)
hd.ellipse([cx - s(210), s(250), cx + s(210), s(600)], fill=(255, 190, 255, 120)); halo = halo.filter(ImageFilter.GaussianBlur(s(40)))
bg = Image.alpha_composite(bg, halo); d = ImageDraw.Draw(bg)
# capsulas en el fondo del domo
CAP = [(240, 84, 84), (255, 150, 40), (255, 224, 56), (90, 200, 96), (110, 230, 235), (66, 138, 245), (150, 90, 230), (250, 110, 190), (255, 255, 255)]
def capsula(x, y, r, col):
    d.ellipse([x - r, y - r, x + r, y + r], fill=tuple(int(c * 0.72) for c in col))
    d.pieslice([x - r, y - r, x + r, y + r], 180, 360, fill=col)
    d.line([(x - r, y), (x + r, y)], fill=(40, 20, 60), width=max(2, S * 2))
    d.ellipse([x - r * 0.55, y - r * 0.7, x - r * 0.15, y - r * 0.35], fill=(255, 255, 255))
for k in range(26):
    x = cx + s(rnd.randint(-240, 240)); y = s(rnd.randint(520, 600))
    capsula(x, y, s(rnd.choice([26, 30, 34])), rnd.choice(CAP))
# pokeball grande en el centro (sin michialien)
def pokeball(x, y, r, dd, brillo=True):
    dd.ellipse([x - r - s(5), y - r - s(5), x + r + s(5), y + r + s(5)], fill=(40, 14, 70))
    dd.ellipse([x - r, y - r, x + r, y + r], fill=(250, 244, 255))
    for k in range(int(r)):
        yy = y - r + k
        half = math.sqrt(max(0, r * r - (yy - y) ** 2))
        if yy < y - r * 0.08:
            t = k / r; dd.line([(x - half, yy), (x + half, yy)], fill=(int(255 - 40 * t), int(110 - 30 * t), int(185 - 30 * t)), width=1)
    dd.rectangle([x - r, y - int(r * 0.09), x + r, y + int(r * 0.09)], fill=(40, 14, 70))
    dd.ellipse([x - r, y - r, x + r, y + r], outline=(40, 14, 70), width=max(2, int(r * 0.07)))
    dd.ellipse([x - r * 0.3, y - r * 0.3, x + r * 0.3, y + r * 0.3], fill=(40, 14, 70))
    dd.ellipse([x - r * 0.22, y - r * 0.22, x + r * 0.22, y + r * 0.22], fill=(250, 244, 255))
    pata(x, y + int(r * 0.03), int(r * 0.09), (255, 120, 190), dd)
    if brillo: dd.ellipse([x - r * 0.72, y - r * 0.78, x - r * 0.38, y - r * 0.5], fill=(255, 255, 255))
for _ in range(14):
    ex, ey = cx + s(rnd.randint(-250, 250)), s(rnd.randint(190, 520)); er = s(rnd.choice([8, 12]))
    if math.hypot(ex - cx, ey - s(400)) > s(215): estrella(ex, ey, er, (255, 245, 190), d)
# reflejos del cristal + armazon
refl = Image.new('RGBA', (W, H), (0, 0, 0, 0)); rd = ImageDraw.Draw(refl)
rd.rounded_rectangle([cx - s(270), s(190), cx - s(245), s(520)], radius=s(12), fill=(255, 255, 255, 90))
rd.rounded_rectangle([cx + s(238), s(230), cx + s(252), s(480)], radius=s(7), fill=(255, 255, 255, 60))
bg = Image.alpha_composite(bg, refl); d = ImageDraw.Draw(bg)
d.rounded_rectangle([cx - s(304), s(150), cx + s(304), s(640)], radius=s(262), outline=ORO, width=s(7))
d.rounded_rectangle([cx - s(312), s(144), cx + s(312), s(646)], radius=s(266), outline=ORO_D, width=s(3))
# corona superior: banda con orejas de gato y antenas alien
d.rounded_rectangle([cx - s(330), s(110), cx + s(330), s(178)], radius=s(30), fill=(60, 24, 100), outline=ORO, width=s(6))
for sx in (-1, 1):
    d.polygon([(cx + sx * s(318), s(116)), (cx + sx * s(268), s(40)), (cx + sx * s(200), s(114))], fill=ORO, outline=ORO_D)
    d.polygon([(cx + sx * s(296), s(112)), (cx + sx * s(268), s(66)), (cx + sx * s(232), s(112))], fill=ROSA)
    d.line([(cx + sx * s(120), s(112)), (cx + sx * s(150), s(48))], fill=(200, 200, 225), width=s(5))
    d.ellipse([cx + sx * s(150) - s(14), s(48) - s(14), cx + sx * s(150) + s(14), s(48) + s(14)], fill=MOR, outline=ORO_L, width=s(3))
# lazo y gema central
d.polygon([(cx - s(8), s(70)), (cx - s(86), s(36)), (cx - s(80), s(102))], fill=ROSA, outline=ROSA_D)
d.polygon([(cx + s(8), s(70)), (cx + s(86), s(36)), (cx + s(80), s(102))], fill=ROSA, outline=ROSA_D)
d.polygon([(cx, s(40)), (cx + s(34), s(76)), (cx, s(118)), (cx - s(34), s(76))], fill=(190, 120, 255), outline=ORO_L)
pata(cx, s(84), s(13), ORO_L, d)
texto((768, 144), 'GASHA ESPECIAL DE EVENTOS', 27, ORO_L)

# ---------- panel izquierdo: ticket
marco((70, 330, 330, 650), fill=(46, 20, 82, 235))
texto((200, 362), 'TU TICKET', 24, ORO_L)
# ticket pixel-art
# ---------- paneles derechos
marco((1090, 40, 1480, 120), fill=(46, 20, 82, 240))
texto((1285, 80), 'Michialien · Evento', 28, ORO_L)
marco((1090, 150, 1480, 420))
texto((1285, 182), 'PREMIOS', 22, ORO_L)
filas = [('Pokémon Emi', ROSA, '10%'), ('Peluches', MOR, '7%'), ('GatitoAlien', (255, 224, 56), '7%'), ('¡Pareja!', (110, 230, 235), '2%'), ('Objetos útiles', (150, 220, 150), '74%')]
for k, (n, col, pc) in enumerate(filas):
    y = 222 + k * 38
    d.rounded_rectangle([s(1118), s(y - 13), s(1138), s(y + 13)], radius=s(5), fill=col, outline=BL)
    texto((1154, y), n, 22, BL, 'lm'); texto((1452, y), pc, 22, ORO_L, 'rm')
marco((1090, 450, 1480, 640))
texto((1285, 482), 'GARANTÍA', 22, ORO_L)
for k in range(10):
    x = 1124 + k * 35; lleno = k < 7
    pass
texto((1285, 600), '7 / 10  ·  a la 10ª, pareja segura', 18, (230, 215, 255), bold=False)
marco((1090, 670, 1480, 860))
texto((1285, 702), 'ÚLTIMO PREMIO', 22, ORO_L)
texto((1368, 755), 'GatitoAlien', 24, BL); texto((1368, 786), 'con corona dorada', 18, (255, 220, 120), bold=False); texto((1368, 812), '¡Nivel 50!', 18, ROSA, bold=False)

# ---------- botones inferiores
def boton(box, t, sub, col):
    marco(box, fill=col)
    texto(((box[0] + box[2]) / 2, (box[1] + box[3]) / 2 - 8), t, 36, BL); texto(((box[0] + box[2]) / 2, (box[1] + box[3]) / 2 + 26), sub, 18, (255, 235, 160), bold=False)
boton((330, 860, 740, 975), 'TIRAR  x1', '1 ticket', (110, 40, 130, 245))
boton((790, 860, 1200, 975), 'TIRAR  x10', '10 tickets · 1 clic', (150, 40, 110, 245))
# cerrar
d.ellipse([s(1450), s(900), s(1500), s(950)], fill=ROSA_D, outline=ORO, width=s(4)); texto((1475, 925), 'X', 30, BL)
# hint
texto((768, 1000), 'Tickets Gasha Especial de Eventos · cada 10 tiradas sin pareja te aseguro un Pokémon con su peluche', 17, (210, 190, 245), bold=False)

os.makedirs(OUT, exist_ok=True)
bg.convert('RGB').resize((1536, 1024), Image.LANCZOS).save(f'{OUT}/fondo.png')

# ================================================================== sprites sueltos para las animaciones (se tiñen/rotan desde Java)
def sprite_ball(oro):
    N = 512; im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); dd = ImageDraw.Draw(im); X = Y = N // 2; R = int(N * 0.45)
    dd.ellipse([X - R - 8, Y - R - 8, X + R + 8, Y + R + 8], fill=(40, 14, 70, 255))
    dd.ellipse([X - R, Y - R, X + R, Y + R], fill=(250, 244, 255, 255))
    top = (255, 215, 70) if oro else (255, 110, 185); bot = (230, 150, 20) if oro else (225, 80, 155)
    for k in range(R):
        yy = Y - R + k; half = math.sqrt(max(0, R * R - (yy - Y) ** 2))
        if yy < Y - R * 0.08:
            t = k / R; dd.line([(X - half, yy), (X + half, yy)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,))
    dd.rectangle([X - R, Y - int(R * 0.09), X + R, Y + int(R * 0.09)], fill=(40, 14, 70, 255))
    dd.ellipse([X - R, Y - R, X + R, Y + R], outline=(40, 14, 70, 255), width=int(R * 0.07))
    dd.ellipse([X - R * 0.3, Y - R * 0.3, X + R * 0.3, Y + R * 0.3], fill=(40, 14, 70, 255)); dd.ellipse([X - R * 0.22, Y - R * 0.22, X + R * 0.22, Y + R * 0.22], fill=(250, 244, 255, 255))
    pata(X, Y + int(R * 0.03), int(R * 0.09), (255, 205, 45, 255) if oro else (255, 120, 190, 255), dd)
    dd.ellipse([X - R * 0.72, Y - R * 0.78, X - R * 0.38, Y - R * 0.5], fill=(255, 255, 255, 255))
    return im.resize((256, 256), Image.LANCZOS)
sprite_ball(False).save(f'{OUT}/ball.png'); sprite_ball(True).save(f'{OUT}/ball_oro.png')
# rayos (blancos, se tiñen desde Java)
im = Image.new('RGBA', (512, 512), (0, 0, 0, 0)); dd = ImageDraw.Draw(im)
for i in range(24):
    a0 = math.radians(i * 15 - 2.2); a1 = math.radians(i * 15 + 2.2)
    dd.polygon([(256 + math.cos(a0) * 70, 256 + math.sin(a0) * 70), (256 + math.cos(a1) * 70, 256 + math.sin(a1) * 70), (256 + math.cos(a1) * 250, 256 + math.sin(a1) * 250), (256 + math.cos(a0) * 250, 256 + math.sin(a0) * 250)], fill=(255, 255, 255, 235))
im.filter(ImageFilter.GaussianBlur(1.2)).save(f'{OUT}/rayos.png')
# halo (degradado radial blanco)
im = Image.new('RGBA', (256, 256), (0, 0, 0, 0)); px = im.load()
for y in range(256):
    for x in range(256):
        r = math.hypot(x - 127.5, y - 127.5) / 128
        if r < 1: px[x, y] = (255, 255, 255, int(255 * (1 - r) ** 1.6))
im.save(f'{OUT}/halo.png')
# anillo (se escala desde Java)
im = Image.new('RGBA', (256, 256), (0, 0, 0, 0)); ImageDraw.Draw(im).ellipse([6, 6, 250, 250], outline=(255, 255, 255, 255), width=8)
im.filter(ImageFilter.GaussianBlur(0.8)).save(f'{OUT}/anillo.png')
# estrella y huella (32x32, blancas)
im = Image.new('RGBA', (128, 128), (0, 0, 0, 0)); dd = ImageDraw.Draw(im); estrella(64, 64, 60, (255, 255, 255, 255), dd); im.resize((32, 32), Image.LANCZOS).save(f'{OUT}/estrella.png')
im = Image.new('RGBA', (128, 128), (0, 0, 0, 0)); dd = ImageDraw.Draw(im); pata(64, 74, 36, (255, 255, 255, 255), dd); im.resize((32, 32), Image.LANCZOS).save(f'{OUT}/pata.png')
print('GUI del Gasha:', sorted(os.listdir(OUT)))
