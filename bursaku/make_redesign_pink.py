#!/usr/bin/env python3
"""Tahap 1: Mockup redesign BursaKu palet pink-biru + 3 opsi ikon launcher.
Hanya mockup — tidak mengubah kode/sumber daya aplikasi."""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.expanduser("~/workspace/bursaku/screenshots/redesign_pink")
os.makedirs(OUT, exist_ok=True)

# --- Palet ---
BLUE   = (106, 130, 251)   # #6A82FB primer (arsip)
PINK   = (255, 111, 145)   # #FF6F91 aksen (arsip)
MAGENTA = (232, 67, 147)   # #E84393 gradien #2 atas (baru)
PURPLE_D = (91, 42, 134)   # #5B2A86 gradien #2 bawah (baru)
BLUE_S = (162, 194, 224)   # #A2C2E0 soft
CREAM  = (249, 251, 203)   # #F9FBCB
YELLOW = (249, 199, 79)    # #F9C74F
DARK   = (11, 14, 26)      # latar fintech gelap
CARD   = (20, 24, 40)      # kartu gelap
WHITE  = (255, 255, 255)
GRAY_T = (148, 163, 184)   # teks abu
GREEN  = (34, 197, 94)     # naik

FD = "/usr/share/fonts/truetype/noto/"
def font(name, size):
    return ImageFont.truetype(os.path.join(FD, name), size)

F_XB = lambda s: font("NotoSans-ExtraBold.ttf", s)
F_B  = lambda s: font("NotoSans-Bold.ttf", s)
F_M  = lambda s: font("NotoSans-Medium.ttf", s)
F_R  = lambda s: font("NotoSans-Regular.ttf", s)

W, H = 1080, 1920

def diag_gradient(size, c1, c2):
    """Gradien diagonal c1 (kiri atas) -> c2 (kanan bawah)."""
    w, h = size
    base = Image.new("RGB", (w, h), c1)
    top = Image.new("RGB", (w, h), c2)
    mask = Image.new("L", (w, h))
    mp = mask.load()
    for y in range(h):
        for x in range(w):
            mp[x, y] = int(255 * (x / max(w - 1, 1) + y / max(h - 1, 1)) / 2)
    return Image.composite(top, base, mask)

def rounded(img_draw_target):
    return img_draw_target

def label_mockup(d):
    txt = "MOCKUP \u2014 bukan screenshot perangkat"
    f = F_B(26)
    bb = d.textbbox((0, 0), txt, font=f)
    tw = bb[2] - bb[0] + 60
    x0 = (W - tw) // 2
    d.rounded_rectangle([x0, 96, x0 + tw, 146], radius=25, fill=WHITE)
    d.text((W // 2, 121), txt, font=f, fill=MAGENTA, anchor="mm")

# ================= HOME =================
img = Image.new("RGB", (W, H), DARK)
d = ImageDraw.Draw(img)

# status bar
d.text((60, 30), "15:31", font=F_B(34), fill=WHITE)
d.text((880, 30), "\u25cf\u25cf\u25cf  77%", font=F_B(30), fill=WHITE)

# header gradien pink/magenta -> ungu tua (gradien #2)
hdr_h = 350
hdr = diag_gradient((W, hdr_h), MAGENTA, PURPLE_D)
img.paste(hdr, (0, 0))
d = ImageDraw.Draw(img)
d.text((60, 140), "BursaKu", font=F_XB(96), fill=WHITE)
d.text((62, 242), "Info pasar saham Indonesia", font=F_R(34), fill=(255,255,255))
# status pasar pill
d.ellipse([60, 292, 92, 324], fill=GREEN)
d.text((108, 308), "BUKA", font=F_XB(34), fill=WHITE, anchor="lm")
d.text((240, 308), "Sesi 2", font=F_R(32), fill=(255,255,255), anchor="lm")

label_mockup(d)

y = 400
d.text((60, y), "Indeks", font=F_XB(52), fill=WHITE); y += 80
# kartu indeks dengan aksen gradien kiri (pink -> ungu tua)
d.rounded_rectangle([48, y, 1032, y + 280], radius=36, fill=CARD)
acc = diag_gradient((14, 280), MAGENTA, PURPLE_D)
mask = Image.new("L", (14, 280), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, 14, 280], radius=7, fill=255)
img.paste(acc, (48, y), mask); d = ImageDraw.Draw(img)
d.text((100, y + 30), "IHSG", font=F_M(28), fill=GRAY_T)
d.text((100, y + 62), "6.148,41", font=F_XB(76), fill=WHITE)
d.text((100, y + 152), "+138,91 (+2,31%)", font=F_B(32), fill=GREEN)
d.text((100, y + 200), "LQ45", font=F_M(28), fill=GRAY_T)
d.text((100, y + 228), "606,28", font=F_XB(56), fill=WHITE)
y += 330

d.text((60, y), "Valas & Komoditas", font=F_XB(52), fill=WHITE); y += 78
cards = [
    ("USD/IDR", "Rp 16.420"),
    ("Emas", "US$ 4.216"),
    ("Perak", "US$ 61,05"),
    ("Brent", "US$ 104,72"),
    ("WTI", "US$ 91,85"),
    ("Tembaga", "US$ 6,69"),
    ("Batubara", "US$ 104,75"),
    ("Nikel", "US$ 13,49"),
    ("Timah", "US$ 121,87"),
]
cw, ch, gap = 296, 148, 16
for i, (t, v) in enumerate(cards):
    r, c = divmod(i, 3)
    x0 = 48 + c * (cw + gap)
    y0 = y + r * (ch + gap)
    d.rounded_rectangle([x0, y0, x0 + cw, y0 + ch], radius=28, fill=CARD)
    d.text((x0 + 28, y0 + 18), t, font=F_M(26), fill=GRAY_T)
    d.text((x0 + 28, y0 + 52), v, font=F_XB(34), fill=WHITE)
    d.text((x0 + 28, y0 + 100), "+0,25%", font=F_B(26), fill=GREEN)
y += 3 * ch + 2 * gap + 36

d.rounded_rectangle([48, y, 1032, y + 170], radius=32, fill=CARD)
d.text((90, y + 36), "Berita Pasar", font=F_XB(44), fill=WHITE)
d.text((90, y + 96), "Headline saham terkini  \u2022  otomatis", font=F_R(30), fill=GRAY_T)
d.text((960, y + 85), ">", font=F_XB(60), fill=GRAY_T)
y += 215

d.text((540, y + 20), "Terakhir diperbarui 14:05 WIB \u2022 otomatis", font=F_R(28), fill=GRAY_T, anchor="mm")
d.text((540, y + 66), "\u00a9 2026 aldire__", font=F_R(28), fill=GRAY_T, anchor="mm")

# bottom nav
nav_y = H - 190
d.rectangle([0, nav_y, W, H], fill=(13, 17, 32))
tabs = ["Pasar", "Ranking", "Watchlist", "Alert", "Portofolio", "Analisa"]
tw = W // 6
for i, t in enumerate(tabs):
    cx = tw * i + tw // 2
    if i == 0:
        pill = diag_gradient((120, 120), MAGENTA, PURPLE_D)
        m = Image.new("L", (120, 120), 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, 120, 120], radius=36, fill=255)
        img.paste(pill, (cx - 60, nav_y + 14), m)
        d = ImageDraw.Draw(img)
        d.ellipse([cx - 20, nav_y + 34, cx + 20, nav_y + 74], fill=WHITE)
        d.text((cx, nav_y + 140), t, font=F_B(26), fill=MAGENTA, anchor="mm")
    else:
        d.ellipse([cx - 20, nav_y + 34, cx + 20, nav_y + 74], fill=(70, 80, 110))
        d.text((cx, nav_y + 140), t, font=F_R(26), fill=GRAY_T, anchor="mm")

img.save(os.path.join(OUT, "home.png"))
print("home.png OK")

# ================= IKON LAUNCHER =================
S = 512
def icon_base():
    im = Image.new("RGB", (S, S), (13, 17, 32))
    # glow gradien halus di tengah
    glow = diag_gradient((S, S), (30, 38, 80), (60, 30, 70))
    m = Image.new("L", (S, S), 0)
    ImageDraw.Draw(m).ellipse([56, 56, S - 56, S - 56], fill=70)
    return Image.composite(glow, im, m)

def grad_box(w, h):
    return diag_gradient((w, h), BLUE, PINK)

# --- Opsi A: huruf B tebal + candlestick ---
im = icon_base(); d = ImageDraw.Draw(im)
d.text((256, 236), "B", font=F_XB(300), fill=WHITE, anchor="mm")
# candlestick kecil di kanan bawah B
cx0 = 330
for i, (o, c, hh) in enumerate([(0, GREEN, 60), (1, PINK, 90), (2, GREEN, 45)]):
    x = cx0 + i * 34
    d.line([x + 10, 300 - hh, x + 10, 300 + 20], fill=c, width=6)
    d.rectangle([x, 300 - hh + 12, x + 20, 300 + 8], fill=c)
# panah naik gradien
arr = grad_box(120, 120); am = Image.new("L", (120, 120), 0)
ad = ImageDraw.Draw(am); ad.polygon([(60, 8), (112, 70), (78, 70), (78, 112), (42, 112), (42, 70), (8, 70)], fill=255)
im.paste(arr, (330, 120), am)
im.save(os.path.join(OUT, "icon_a.png")); print("icon_a.png OK")

# --- Opsi B: panah naik + candlestick garis minimalis ---
im = icon_base(); d = ImageDraw.Draw(im)
# garis tren naik gradien (tebal, rounded)
pts = [(110, 350), (200, 280), (280, 310), (400, 170)]
d.line(pts, fill=BLUE_S, width=26, joint="curve")
# panah di ujung
d.polygon([(400, 120), (452, 200), (400, 200), (400, 170)], fill=PINK)
# candlestick minimalis di bawah garis
for i, (x, hh, c) in enumerate([(150, 70, GREEN), (230, 100, PINK), (310, 60, GREEN)]):
    d.line([x, 400 - hh, x, 430], fill=c, width=10)
    d.rectangle([x - 14, 400 - hh + 16, x + 14, 424], outline=c, width=10)
# titik aksen kuning
d.ellipse([386, 156, 414, 184], fill=YELLOW)
im.save(os.path.join(OUT, "icon_b.png")); print("icon_b.png OK")

# --- Opsi C: koin Rp modern + aksen chart ---
im = icon_base(); d = ImageDraw.Draw(im)
# koin gradien
coin = grad_box(300, 300); cm = Image.new("L", (300, 300), 0)
ImageDraw.Draw(cm).ellipse([0, 0, 300, 300], fill=255)
im.paste(coin, (106, 106), cm); d = ImageDraw.Draw(im)
d.ellipse([106, 106, 406, 406], outline=(255, 255, 255), width=10)
d.text((256, 256), "Rp", font=F_XB(120), fill=WHITE, anchor="mm")
# mini chart di bawah koin (di dalam lingkaran aman, tidak menabrak ring)
for i, h in enumerate([36, 58, 44, 74, 60, 92]):
    x = 158 + i * 34
    d.rectangle([x, 448 - h, x + 20, 448], fill=PINK if i % 2 else BLUE_S)
im.save(os.path.join(OUT, "icon_c.png")); print("icon_c.png OK")

print("SELESAI:", OUT)
