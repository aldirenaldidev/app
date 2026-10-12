#!/usr/bin/env python3
"""Screenshot: nav.png (bottom nav baru), about.png (Tentang baru),
topdividen.png (status progres pindai penuh). Mock PIL gaya fintech."""
from PIL import Image, ImageDraw, ImageFont
import os, math

W = 824
BG = (11, 15, 26)
CARD = (21, 27, 46)
WHITE = (255, 255, 255)
GRAY = (139, 147, 167)
GREEN = (22, 199, 132)
RED = (234, 57, 67)
BLUE = (59, 130, 246)
PURPLE = (139, 92, 246)
LIGHT = (200, 208, 225)
OUT = os.path.expanduser("~/workspace/bursaku/screenshots/")

def font(sz, bold=False):
    p = "/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else "")
    return ImageFont.truetype(p, sz)

def rrect(d, box, r, fill):
    d.rounded_rectangle(box, radius=r, fill=fill)

def gradient_pill(img, box, r, c1, c2):
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    grad = Image.new("RGB", (w, h))
    gd = ImageDraw.Draw(grad)
    for x in range(w):
        t = x / max(w - 1, 1)
        gd.line([(x, 0), (x, h)], fill=(int(c1[0] + (c2[0] - c1[0]) * t),
                                        int(c1[1] + (c2[1] - c1[1]) * t),
                                        int(c1[2] + (c2[2] - c1[2]) * t)))
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w, h], radius=r, fill=255)
    img.paste(grad, (x0, y0), mask)

def ig_gradient(img, box, r):
    # diagonal ungu -> pink -> oranye
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    grad = Image.new("RGB", (w, h))
    gd = ImageDraw.Draw(grad)
    c1, c2, c3 = (131, 58, 180), (253, 29, 29), (252, 176, 69)
    for y in range(h):
        for x in range(0, w, 2):
            t = (x / w + y / h) / 2
            if t < 0.5:
                k = t * 2; a, b = c1, c2
            else:
                k = (t - 0.5) * 2; a, b = c2, c3
            gd.rectangle([x, y, x + 2, y + 1],
                         fill=(int(a[0] + (b[0] - a[0]) * k),
                               int(a[1] + (b[1] - a[1]) * k),
                               int(a[2] + (b[2] - a[2]) * k)))
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w, h], radius=r, fill=255)
    img.paste(grad, (x0, y0), mask)

# ---------- ikon garis modern (pusat cx,cy ; ukuran s ; warna col) ----------
def ico_pasar(d, cx, cy, s, col, w=4):
    x0, y0 = cx - s / 2, cy - s / 2
    d.line([(x0 + 2, y0 + 2), (x0 + 2, y0 + s - 2), (x0 + s - 2, y0 + s - 2)],
           fill=col, width=w, joint="curve")
    pts = [(x0 + 6, y0 + s - 9), (x0 + s * 0.45, y0 + s * 0.55),
           (x0 + s * 0.6, y0 + s * 0.68), (x0 + s - 5, y0 + 7)]
    d.line(pts, fill=col, width=w, joint="curve")
    d.line([(x0 + s - 12, y0 + 7), (x0 + s - 5, y0 + 7), (x0 + s - 5, y0 + 14)],
           fill=col, width=w, joint="curve")

def ico_ranking(d, cx, cy, s, col, w=4):
    x0, y0 = cx - s / 2, cy - s / 2
    d.line([(x0 + 2, y0 + s - 2), (x0 + s - 2, y0 + s - 2)], fill=col, width=w)
    for fx, fh in [(0.28, 0.45), (0.5, 0.75), (0.72, 0.6)]:
        x = x0 + s * fx
        d.line([(x, y0 + s - 2), (x, y0 + s - 2 - s * fh)], fill=col, width=w)

def ico_star(d, cx, cy, s, col, w=4):
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        r = s / 2 if i % 2 == 0 else s / 2 * 0.42
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    pts.append(pts[0])
    d.line(pts, fill=col, width=w, joint="curve")

def ico_bell(d, cx, cy, s, col, w=4):
    x0, y0 = cx - s / 2, cy - s / 2
    d.arc([x0 + 4, y0 + 2, x0 + s - 4, y0 + s - 6], start=200, end=340,
          fill=col, width=w)
    d.line([(x0 + 4, y0 + s * 0.62), (x0 + 2, y0 + s - 6),
            (x0 + s - 2, y0 + s - 6), (x0 + s - 4, y0 + s * 0.62)],
           fill=col, width=w, joint="curve")
    d.arc([cx - 5, y0 + s - 9, cx + 5, y0 + s + 1], start=20, end=160,
          fill=col, width=w)

def ico_brief(d, cx, cy, s, col, w=4):
    x0, y0 = cx - s / 2, cy - s / 2
    d.rounded_rectangle([x0 + 2, y0 + 8, x0 + s - 2, y0 + s - 2], radius=4,
                       outline=col, width=w)
    d.arc([cx - 8, y0 + 1, cx + 8, y0 + 13], start=180, end=360, fill=col, width=w)
    d.line([(x0 + 2, y0 + s * 0.55), (x0 + s - 2, y0 + s * 0.55)], fill=col, width=w)

def ico_analisa(d, cx, cy, s, col, w=4):
    d.ellipse([cx - s * 0.32, cy - s * 0.36, cx + s * 0.28, cy + s * 0.24],
              outline=col, width=w)
    d.line([(cx + s * 0.2, cy + s * 0.16), (cx + s * 0.42, cy + s * 0.42)],
           fill=col, width=w)
    d.line([(cx - s * 0.2, cy + s * 0.05), (cx - s * 0.08, cy - s * 0.05),
            (cx + s * 0.02, cy + s * 0.03), (cx + s * 0.14, cy - s * 0.1)],
           fill=col, width=3, joint="curve")

def ico_heat(d, cx, cy, s, col, w=4):
    x0, y0 = cx - s / 2, cy - s / 2
    g = 3
    cw = (s - g) / 2
    for ix in range(2):
        for iy in range(2):
            d.rounded_rectangle(
                [x0 + ix * (cw + g), y0 + iy * (cw + g),
                 x0 + ix * (cw + g) + cw, y0 + iy * (cw + g) + cw],
                radius=3, outline=col, width=w)

ICONS = [("Pasar", ico_pasar), ("Ranking", ico_ranking), ("Watchlist", ico_star),
         ("Alert", ico_bell), ("Portofolio", ico_brief), ("Analisa", ico_analisa),
         ("Heatmap", ico_heat)]

# ============ nav.png : close-up bottom nav baru ============
img = Image.new("RGB", (W, 420), BG)
d = ImageDraw.Draw(img)
y0 = 60
h = 300
rrect(d, [0, y0, W, y0 + h], 0, CARD)
d.rounded_rectangle([0, y0, W, y0 + 44], radius=20, fill=CARD)
d.rectangle([0, y0 + 22, W, y0 + h], fill=CARD)
n = len(ICONS)
cw = W // n
for i, (label, fn) in enumerate(ICONS):
    cx = cw * i + cw // 2
    on = (i == 1)  # Ranking aktif sebagai contoh
    if on:
        gradient_pill(img, [cw * i + 6, y0 + 18, cw * (i + 1) - 6, y0 + h - 18],
                      30, BLUE, PURPLE)
    fn(d, cx, y0 + 95, 56, WHITE if on else GRAY)
    d.text((cx, y0 + 165), label, font=font(24, bold=on),
           fill=WHITE if on else GRAY, anchor="ma")
img.save(OUT + "nav.png")
print("nav.png ok")

# ============ about.png : layar Tentang baru ============
img = Image.new("RGB", (W, 1750), BG)
d = ImageDraw.Draw(img)
y = 60
d.text((48, y), "Tentang", font=font(52, True), fill=WHITE)
y += 110
# kartu aplikasi
rrect(d, [36, y, W - 36, y + 300], 28, CARD)
d.text((W // 2, y + 90), "BursaKu", font=font(64, True), fill=WHITE, anchor="mm")
d.text((W // 2, y + 160), "Versi 1.1", font=font(30), fill=(224, 231, 255), anchor="mm")
d.text((W // 2, y + 210), "Info pasar saham Indonesia", font=font(30),
       fill=(224, 231, 255), anchor="mm")
y += 330
# hak cipta
rrect(d, [36, y, W - 36, y + 190], 28, CARD)
d.text((W // 2, y + 70), "\u00a9 2026 aldire__", font=font(40, True), fill=WHITE, anchor="mm")
d.text((W // 2, y + 125), "Hak cipta dilindungi undang-undang.", font=font(28),
       fill=GRAY, anchor="mm")
y += 220
# developer + IG premium
rrect(d, [36, y, W - 36, y + 330], 28, CARD)
d.text((72, y + 36), "Developer", font=font(32, True), fill=WHITE)
d.text((72, y + 92), "@aldire__", font=font(44, True), fill=BLUE)
ig_gradient(img, [72, y + 170, W - 72, y + 270], 32)
d2 = ImageDraw.Draw(img)
# ikon kamera IG (putih)
ix, iy = 170, y + 220
d2.rounded_rectangle([ix - 34, iy - 34, ix + 34, iy + 34], radius=22,
                     outline=WHITE, width=6)
d2.ellipse([ix - 15, iy - 15, ix + 15, iy + 15], outline=WHITE, width=6)
d2.ellipse([ix + 17, iy - 27, ix + 27, iy - 17], fill=WHITE)
d2.text((W // 2 + 40, y + 220), "@aldire__", font=font(38, True), fill=WHITE, anchor="mm")
y += 360
# kenapa bursaku dibuat
rrect(d, [36, y, W - 36, y + 470], 28, CARD)
d2 = ImageDraw.Draw(img)
d2.text((72, y + 36), "Kenapa BursaKu dibuat", font=font(32, True), fill=WHITE)
teks = ("BursaKu dibuat dengan satu tujuan sederhana: memberikan investor "
        "ritel Indonesia akses ke analisa saham yang lengkap \u2014 teknikal dan "
        "fundamental \u2014 tanpa biaya berlangganan yang mahal. Data pasar "
        "seharusnya bisa diakses siapa saja, bukan hanya mereka yang mampu "
        "membayar terminal data premium.")
# bungkus manual
words, lines, cur = teks.split(" "), [], ""
for wd in words:
    t = (cur + " " + wd).strip()
    if d2.textlength(t, font=font(30)) > W - 180:
        lines.append(cur); cur = wd
    else:
        cur = t
lines.append(cur)
yy = y + 100
for ln in lines:
    d2.text((72, yy), ln, font=font(30), fill=LIGHT)
    yy += 46
img.save(OUT + "about.png")
print("about.png ok")

# ============ topdividen.png : status progres pindai penuh ============
img = Image.new("RGB", (W, 1900), BG)
d = ImageDraw.Draw(img)
y = 60
d.text((48, y), "Ranking", font=font(52, True), fill=WHITE)
d.text((48, y + 66), "Semua emiten IDX", font=font(28), fill=GRAY)
y += 130
rrect(d, [36, y, W - 36, y + 96], 20, (11, 15, 26))
tabs = ["Top Gainer", "Top Loser", "Teraktif", "Top Dividen"]
tw = (W - 72) // 4
for i, t in enumerate(tabs):
    x0 = 36 + i * tw
    if i == 3:
        rrect(d, [x0 + 4, y + 8, x0 + tw - 4, y + 88], 14, BLUE)
        d.text((x0 + tw // 2, y + 48), t, font=font(26, True), fill=WHITE, anchor="mm")
    else:
        d.text((x0 + tw // 2, y + 48), t, font=font(26), fill=GRAY, anchor="mm")
y += 116
rrect(d, [36, y, W - 36, y + 92], 18, BLUE)
d.text((W // 2, y + 46), "\u2b07 Ekspor Top Dividen (CSV)", font=font(30, True),
       fill=WHITE, anchor="mm")
y += 112
# status progres jujur
rrect(d, [36, y, W - 36, y + 130], 20, CARD)
d.text((64, y + 28), "Pindai penuh 940 emiten \u2022 progres 123/940",
       font=font(28, True), fill=WHITE)
d.text((64, y + 70), "hasil sementara di bawah", font=font(26), fill=GRAY)
y += 146
# progress bar
rrect(d, [36, y, W - 36, y + 22], 11, (58, 67, 86))
rrect(d, [36, y, 36 + int((W - 72) * 123 / 940), y + 22], 11, BLUE)
y += 40
rrect(d, [36, y, W - 36, y + 92], 18, (58, 67, 86))
d.text((W // 2, y + 46), "\u25a0 Berhenti", font=font(30, True), fill=WHITE, anchor="mm")
y += 112
divs = [("BBCA", "Bank Central Asia Tbk", "Rp 10.100 \u2022 Rp 281/thn", "4,61%"),
        ("TLKM", "Telkom Indonesia Tbk", "Rp 3.420 \u2022 Rp 178/thn", "5,20%"),
        ("ASII", "Astra International Tbk", "Rp 5.150 \u2022 Rp 268/thn", "5,20%")]
for i, (code, name, sub, yld) in enumerate(divs):
    rrect(d, [36, y, W - 36, y + 150], 24, CARD)
    d.text((64, y + 18), str(i + 1), font=font(30, True), fill=GRAY)
    d.text((120, y + 14), code, font=font(34, True), fill=WHITE)
    d.text((120, y + 58), name, font=font(26), fill=GRAY)
    d.text((120, y + 96), sub, font=font(26), fill=LIGHT)
    rrect(d, [W - 200, y + 48, W - 64, y + 102], 18, (20, 60, 45))
    d.text((W - 132, y + 75), yld, font=font(28, True), fill=GREEN, anchor="mm")
    y += 162
d.text((W // 2, y + 8), "Terakhir dipindai penuh: 8 Okt 2026 \u2022 otomatis (30 hari)",
       font=font(26), fill=GRAY, anchor="ma")
img.save(OUT + "topdividen.png")
print("topdividen.png ok")
