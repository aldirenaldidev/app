#!/usr/bin/env python3
"""Gambar ulang screenshot BursaKu gaya fintech modern (mock PIL, data contoh).
Menampilkan UI OTOMATIS: tanpa tombol refresh manual + penanda waktu auto."""
from PIL import Image, ImageDraw, ImageFont
import os, math, random

W = 824
BG = (11, 15, 26)
CARD = (21, 27, 46)
WHITE = (255, 255, 255)
GRAY = (139, 147, 167)
GREEN = (22, 199, 132)
RED = (234, 57, 67)
BLUE = (59, 130, 246)
PURPLE = (139, 92, 246)
GREEN_BG = (22, 199, 132, 40)

def font(sz, bold=False):
    for p in ["/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else ""),
              "/usr/share/fonts/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else "")]:
        if os.path.exists(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()

def rrect(d, box, r, fill):
    d.rounded_rectangle(box, radius=r, fill=fill)

def header(d, y0=0, h=300):
    for y in range(y0, y0 + h):
        t = y / h
        c = (int(59 + (139 - 59) * t), int(130 + (92 - 130) * t), int(246 + (246 - 246) * t))
        d.line([(0, y), (W, y)], fill=c)
    d.text((48, y0 + 40), "BursaKu", font=font(72, True), fill=WHITE)
    d.text((48, y0 + 125), "Info pasar saham Indonesia", font=font(30), fill=(230, 235, 255))
    d.ellipse([48, y0 + 185, 72, y0 + 209], fill=GREEN)
    d.text((84, y0 + 178), "BUKA", font=font(34, True), fill=WHITE)
    d.text((210, y0 + 182), "Sesi 2", font=font(30), fill=(230, 235, 255))

def spark(d, x0, y0, w, h, up=True, seed=1):
    random.seed(seed)
    pts = []
    n = 28
    v = 0.5
    for i in range(n):
        v += random.uniform(-0.09, 0.09) + (0.015 if up else -0.015)
        v = max(0.05, min(0.95, v))
        pts.append((x0 + i * w / (n - 1), y0 + h * (1 - v)))
    d.line(pts, fill=GREEN if up else RED, width=5, joint="curve")

def pill(d, x, y, text, up):
    f = font(26, True)
    bb = d.textbbox((0, 0), text, font=f)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    pad_x, pad_y = 22, 12
    rrect(d, [x, y, x + tw + pad_x * 2, y + th + pad_y * 2], 24,
          (22, 80, 60) if up else (90, 30, 38))
    d.text((x + pad_x, y + pad_y - 2), text, font=f, fill=GREEN if up else RED)

def section(d, y, title):
    d.text((48, y), title, font=font(38, True), fill=WHITE)
    return y + 62

# ============ 1. pasar.png ============
img = Image.new("RGB", (W, 2080), BG)
d = ImageDraw.Draw(img)
header(d)
y = 340
y = section(d, y, "Indeks")
rrect(d, [36, y, W - 36, y + 450], 28, CARD)
d.text((64, y + 24), "IHSG", font=font(28), fill=GRAY)
d.text((64, y + 54), "6.148,41", font=font(60, True), fill=WHITE)
pill(d, 64, y + 140, "+138,91 (+2,31%)", True)
spark(d, 470, y + 40, 260, 150, True, seed=3)
d.line([(64, y + 246), (W - 64, y + 246)], fill=(40, 48, 70), width=2)
d.text((64, y + 262), "LQ45", font=font(28), fill=GRAY)
d.text((64, y + 292), "606,28", font=font(60, True), fill=WHITE)
pill(d, 64, y + 378, "+13,90 (+2,35%)", True)
spark(d, 470, y + 268, 260, 140, True, seed=7)
y += 480
y = section(d, y, "Valas & Komoditas")
cw = (W - 72 - 32) // 3
for i, (lbl, val, chg, up) in enumerate([
        ("USD/IDR", "17.875", "+0,12%", True),
        ("Emas", "$4,160.30", "-0,05%", False),
        ("Minyak Brent", "$101.36", "-0,87%", False)]):
    x = 36 + i * (cw + 16)
    rrect(d, [x, y, x + cw, y + 190], 28, CARD)
    d.text((x + 24, y + 20), lbl, font=font(26), fill=GRAY)
    d.text((x + 24, y + 58), val, font=font(40, True), fill=WHITE)
    d.text((x + 24, y + 122), chg, font=font(30, True), fill=GREEN if up else RED)
y += 220
d.text((48, y), "Terakhir diperbarui 14:05 WIB \u2022 otomatis", font=font(24), fill=GRAY)
y += 48
y = section(d, y, "Menu")
items = ["Ranking", "Watchlist", "Alert Harga",
         "Berita", "Portofolio", "Tentang",
         "Pengaturan"]
bw = (W - 72 - 32) // 3
bh = 190
for i, t in enumerate(items):
    x = 36 + (i % 3) * (bw + 16)
    yy = y + (i // 3) * (bh + 16)
    rrect(d, [x, yy, x + bw, yy + bh], 28, CARD)
    d.text((x + bw // 2, yy + bh // 2), t, font=font(32, True), fill=WHITE, anchor="mm")
y += 3 * bh + 2 * 16 + 50
d.text((W // 2, y), "\u00A9 2026 aldire__", font=font(24), fill=GRAY, anchor="mm")
d.text((W // 2, y + 36), "Bukan rekomendasi investasi. Data delay \u00B115 menit.", font=font(22), fill=GRAY, anchor="mm")
img.save(os.path.expanduser("~/workspace/bursaku/screenshots/pasar.png"))
print("pasar.png ok")

# ============ 2. settings.png ============
img = Image.new("RGB", (W, 1500), BG)
d = ImageDraw.Draw(img)
d.text((48, 60), "Pengaturan", font=font(44, True), fill=WHITE)
rrect(d, [36, 170, W - 36, 420], 28, CARD)
d.text((64, 200), "Auto-refresh background", font=font(32, True), fill=WHITE)
d.text((64, 248), "Saat aktif, BursaKu otomatis memperbarui", font=font(24), fill=GRAY)
d.text((64, 282), "data indeks, watchlist, dan alert harga", font=font(24), fill=GRAY)
d.text((64, 316), "tiap 15 menit selama jam bursa", font=font(24), fill=GRAY)
d.text((64, 350), "(Senin\u2013Jumat). Di luar jam bursa", font=font(24), fill=GRAY)
d.text((64, 384), "tidak ada polling.", font=font(24), fill=GRAY)
# switch ON
sx, sy, sw, sh = W - 36 - 130, 250, 110, 62
rrect(d, [sx, sy, sx + sw, sy + sh], sh // 2, BLUE)
d.ellipse([sx + sw - sh + 6, sy + 6, sx + sw - 6, sy + sh - 6], fill=WHITE)
d.text((48, 470), "Satu-satunya kontrol manual \u2014 sisanya berjalan otomatis.", font=font(26), fill=GREEN)
d.text((W // 2, 1400), "\u00A9 2026 aldire__", font=font(24), fill=GRAY, anchor="mm")
img.save(os.path.expanduser("~/workspace/bursaku/screenshots/settings.png"))
print("settings.png ok")

# ============ 3. ranking.png (status auto) ============
img = Image.new("RGB", (W, 1600), BG)
d = ImageDraw.Draw(img)
d.text((48, 60), "Ranking LQ45", font=font(44, True), fill=WHITE)
d.text((48, 122), "Otomatis diperbarui \u2022 tanpa tombol manual", font=font(26), fill=GRAY)
tabs = ["Top Gainer", "Top Loser", "Teraktif"]
tw = (W - 72 - 24) // 3
for i, t in enumerate(tabs):
    x = 36 + i * (tw + 12)
    rrect(d, [x, 180, x + tw, 244], 20, BLUE if i == 0 else CARD)
    d.text((x + tw // 2, 212), t, font=font(28, True),
           fill=WHITE if i == 0 else GRAY, anchor="mm")
rows = [("BBCA", "Bank Central Asia", "10.525", "+2,31%", True),
        ("TLKM", "Telkom Indonesia", "3.420", "+1,79%", True),
        ("BBRI", "Bank Rakyat Indonesia", "4.890", "+1,24%", True),
        ("ASII", "Astra International", "5.150", "+0,98%", True),
        ("BMRI", "Bank Mandiri", "7.200", "+0,70%", True)]
y = 270
for code, name, price, chg, up in rows:
    rrect(d, [36, y, W - 36, y + 130], 24, CARD)
    d.text((64, y + 18), code, font=font(32, True), fill=WHITE)
    d.text((64, y + 62), name, font=font(24), fill=GRAY)
    d.text((W - 64, y + 30), price, font=font(32, True), fill=WHITE, anchor="ra")
    f = font(26, True)
    bb = d.textbbox((0, 0), chg, font=f)
    tw2 = bb[2] - bb[0]
    rrect(d, [W - 64 - tw2 - 36, y + 72, W - 64, y + 72 + 52], 18,
          (22, 80, 60) if up else (90, 30, 38))
    d.text((W - 64 - tw2 - 18, y + 80), chg, font=f, fill=GREEN if up else RED)
    y += 146
d.text((48, y + 10), "Data 13:10 WIB \u2022 memperbarui otomatis\u2026", font=font(24), fill=GREEN)
img.save(os.path.expanduser("~/workspace/bursaku/screenshots/ranking.png"))
print("ranking.png ok")
