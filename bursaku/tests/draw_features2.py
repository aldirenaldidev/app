#!/usr/bin/env python3
"""Screenshot fitur baru BursaKu: heatmap, intraday, widget, top dividen, ekspor portofolio.
Mock PIL gaya fintech (headless Chromium sandbox rusak)."""
from PIL import Image, ImageDraw, ImageFont
import os

W = 824
BG = (11, 15, 26)
CARD = (21, 27, 46)
WHITE = (255, 255, 255)
GRAY = (139, 147, 167)
GREEN = (22, 199, 132)
RED = (234, 57, 67)
BLUE = (59, 130, 246)
PURPLE = (139, 92, 246)
NEU = (58, 67, 86)
OUT = os.path.expanduser("~/workspace/bursaku/screenshots/")

def font(sz, bold=False):
    for p in ["/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else ""),
              "/usr/share/fonts/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else "")]:
        if os.path.exists(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()

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

def heat_color(chg):
    t = max(-5.0, min(5.0, chg)) / 5.0
    def mix(a, b, k):
        return tuple(int(a[i] + (b[i] - a[i]) * k + 0.5) for i in range(3))
    return mix(NEU, GREEN, t) if t >= 0 else mix(NEU, RED, -t)

NAV_ITEMS = ["Pasar", "Ranking", "Watchlist", "Alert", "Portofolio", "Analisa", "Heatmap"]

def bottom_nav(img, y0, active):
    d = ImageDraw.Draw(img)
    h = 190
    rrect(d, [0, y0, W, y0 + h], 0, CARD)
    d.rounded_rectangle([0, y0, W, y0 + 44], radius=20, fill=CARD)
    d.rectangle([0, y0 + 22, W, y0 + h], fill=CARD)
    n = len(NAV_ITEMS)
    cw = W // n
    for i, label in enumerate(NAV_ITEMS):
        cx = cw * i + cw // 2
        on = (i == active)
        if on:
            gradient_pill(img, [cw * i + 6, y0 + 18, cw * (i + 1) - 6, y0 + h - 18],
                          30, BLUE, PURPLE)
        d.ellipse([cx - 15, y0 + 42, cx + 15, y0 + 72],
                  fill=WHITE if on else GRAY)
        d.text((cx, y0 + 92), label, font=font(22, bold=on),
               fill=WHITE if on else GRAY, anchor="ma")

def screen_title(d, y, title, subtitle):
    d.text((48, y), title, font=font(52, True), fill=WHITE)
    d.text((48, y + 66), subtitle, font=font(28), fill=GRAY)
    return y + 116

# ============ 1. heatmap.png ============
img = Image.new("RGB", (W, 1900), BG)
d = ImageDraw.Draw(img)
y = 60
y = screen_title(d, y, "Heatmap Pasar", "Pergerakan 45 emiten LQ45 sekilas")
# legenda
d.text((48, y), "\u25bc Turun", font=font(26), fill=RED)
for x in range(200, 560):
    t = (x - 200) / 360
    c = heat_color(-5 + 10 * t)
    d.line([(x, y), (x, y + 18)], fill=c)
d.text((576, y - 4), "Naik \u25b2", font=font(26), fill=GREEN)
y += 56
cells = [("BBCA", 2.31), ("BBRI", 1.84), ("BMRI", 1.52), ("TLKM", 0.93),
         ("ASII", 0.71), ("ADRO", 0.44), ("INDF", 0.12), ("UNTR", -0.08),
         ("SMGR", -0.35), ("KLBF", -0.62), ("PGAS", -1.05), ("ANTM", -1.88),
         ("BBNI", 3.05), ("BRIS", -2.44), ("ARTO", 4.12)]
cols, cw, chh, gap = 3, (W - 96 - 2 * 16) // 3, 210, 16
for i, (code, chg) in enumerate(cells):
    r, c = divmod(i, cols)
    x = 48 + c * (cw + gap)
    yy = y + r * (chh + gap)
    rrect(d, [x, yy, x + cw, yy + chh], 24, heat_color(chg))
    d.text((x + cw // 2, yy + 62), code, font=font(38, True), fill=WHITE, anchor="ma")
    d.text((x + cw // 2, yy + 118), ("%+.2f%%" % chg).replace(".", ","),
           font=font(32, True), fill=WHITE, anchor="ma")
y += 5 * (chh + gap) + 24
d.text((W // 2, y), "Diperbarui 14:05 WIB \u2022 otomatis", font=font(26),
       fill=GRAY, anchor="ma")
bottom_nav(img, 1900 - 190, 6)
img.save(OUT + "heatmap.png")
print("heatmap.png ok")

# ============ 2. intraday.png ============
img = Image.new("RGB", (W, 1750), BG)
d = ImageDraw.Draw(img)
y = 60
y = screen_title(d, y, "BBCA", "Bank Central Asia Tbk")
rrect(d, [36, y, W - 36, y + 150], 28, CARD)
d.text((64, y + 24), "Rp 10.100", font=font(56, True), fill=WHITE)
rrect(d, [470, y + 44, 680, y + 108], 20, (20, 60, 45))
d.text((575, y + 76), "+1,00%", font=font(30, True), fill=GREEN, anchor="mm")
y += 178
# segmented timeframe
rrect(d, [36, y, W - 36, y + 96], 20, (11, 15, 26))
rrect(d, [44, y + 8, W // 2 - 8, y + 88], 16, (30, 38, 60))
d.text((W // 4, y + 48), "Harian", font=font(30), fill=GRAY, anchor="mm")
rrect(d, [W // 2 + 8, y + 8, W - 44, y + 88], 16, BLUE)
d.text((3 * W // 4, y + 48), "Intraday", font=font(30, True), fill=WHITE, anchor="mm")
y += 116
# mock chart intraday (naik-turun sesi)
rrect(d, [36, y, W - 36, y + 560], 28, CARD)
cx0, cx1, cy0, cy1 = 70, W - 70, y + 40, y + 500
pts = []
import math
for i in range(60):
    px = cx0 + i * (cx1 - cx0) / 59
    py = (cy0 + cy1) // 2 - int(120 * math.sin(i / 6.0) + 40 * math.sin(i / 2.3))
    pts.append((px, py))
for i in range(len(pts) - 1):
    d.line([pts[i], pts[i + 1]], fill=GREEN, width=5)
# volume bars
for i in range(0, 60, 2):
    px = cx0 + i * (cx1 - cx0) / 59
    h = 20 + (i * 7) % 50
    d.line([(px, cy1), (px, cy1 - h)], fill=(22, 199, 132, 120), width=8)
d.text((70, y + 566), "09:00", font=font(24), fill=GRAY)
d.text((W - 130, y + 566), "15:15", font=font(24), fill=GRAY)
y += 620
d.text((W // 2, y), "Intraday 5 menit \u2022 sesi terakhir bila bursa tutup",
       font=font(26), fill=GRAY, anchor="ma")
bottom_nav(img, 1750 - 190, -1)
img.save(OUT + "intraday.png")
print("intraday.png ok")

# ============ 3. widget.png (mock home screen + widget) ============
img = Image.new("RGB", (W, 1750), (24, 28, 44))
d = ImageDraw.Draw(img)
d.text((48, 60), "Widget BursaKu di layar utama", font=font(40, True), fill=WHITE)
y = 170
rrect(d, [36, y, W - 36, y + 430], 32, CARD)
d.text((64, y + 28), "IHSG", font=font(28, True), fill=GRAY)
d.text((64, y + 58), "6.148,41", font=font(52, True), fill=WHITE)
rrect(d, [330, y + 78, 540, y + 134], 18, (20, 60, 45))
d.text((435, y + 106), "+2,31%", font=font(28, True), fill=GREEN, anchor="mm")
yy = y + 160
for code, price, chg in [("BBCA", "Rp 10.100", "+1,00%"), ("TLKM", "Rp 3.420", "-0,58%"),
                          ("BBRI", "Rp 4.880", "+2,31%"), ("ASII", "Rp 5.150", "+0,39%")]:
    d.text((64, yy), code, font=font(30, True), fill=WHITE)
    d.text((400, yy), price, font=font(30), fill=WHITE, anchor="ra")
    d.text((W - 64, yy), chg.replace(".", ","), font=font(30, True),
           fill=GREEN if chg.startswith("+") else RED, anchor="ra")
    yy += 52
d.text((W // 2, y + 398), "Diperbarui 14:05 WIB \u2022 BursaKu", font=font(24),
       fill=GRAY, anchor="ma")
y += 470
d.text((48, y), "Ketuk widget untuk membuka aplikasi.", font=font(28), fill=GRAY)
img.save(OUT + "widget.png")
print("widget.png ok")

# ============ 4. topdividen.png ============
img = Image.new("RGB", (W, 1900), BG)
d = ImageDraw.Draw(img)
y = 60
y = screen_title(d, y, "Ranking Saham LQ45", "Top dividen berdasarkan dividend yield")
# segmented 4 tab
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
divs = [("BBCA", "Bank Central Asia Tbk", "Rp 10.100 \u2022 Rp 281/thn", "4,61%"),
        ("TLKM", "Telkom Indonesia Tbk", "Rp 3.420 \u2022 Rp 178/thn", "5,20%"),
        ("ASII", "Astra International Tbk", "Rp 5.150 \u2022 Rp 268/thn", "5,20%"),
        ("UNTR", "United Tractors Tbk", "Rp 28.400 \u2022 Rp 1.512/thn", "5,32%"),
        ("ADRO", "Adaro Energy Tbk", "Rp 2.890 \u2022 Rp 172/thn", "5,95%")]
for i, (code, name, sub, yld) in enumerate(divs):
    rrect(d, [36, y, W - 36, y + 150], 24, CARD)
    d.text((64, y + 18), str(i + 1), font=font(30, True), fill=GRAY)
    d.text((120, y + 14), code, font=font(34, True), fill=WHITE)
    d.text((120, y + 58), name, font=font(26), fill=GRAY)
    d.text((120, y + 96), sub, font=font(26), fill=(200, 208, 225))
    rrect(d, [W - 200, y + 48, W - 64, y + 102], 18, (20, 60, 45))
    d.text((W - 132, y + 75), yld, font=font(28, True), fill=GREEN, anchor="mm")
    y += 162
d.text((W // 2, y + 8), "Dividen diperbarui 14:05 WIB \u2022 otomatis (30 hari)",
       font=font(26), fill=GRAY, anchor="ma")
bottom_nav(img, 1900 - 190, 1)
img.save(OUT + "topdividen.png")
print("topdividen.png ok")

# ============ 5. portofolio.png (dengan tombol ekspor) ============
img = Image.new("RGB", (W, 1750), BG)
d = ImageDraw.Draw(img)
y = 60
y = screen_title(d, y, "Portofolio", "Catat transaksi beli/jual \u2014 1 lot = 100 lembar")
rrect(d, [36, y, W - 36, y + 92], 18, BLUE)
d.text((W // 2, y + 46), "\u2b07 Ekspor Portofolio (CSV)", font=font(30, True),
       fill=WHITE, anchor="mm")
y += 112
rrect(d, [36, y, W - 36, y + 190], 28, CARD)
d.text((64, y + 24), "Total Nilai Pasar", font=font(26), fill=GRAY)
d.text((64, y + 58), "Rp 24,5 Jt", font=font(48, True), fill=WHITE)
rrect(d, [W - 330, y + 60, W - 64, y + 130], 20, (20, 60, 45))
d.text((W - 197, y + 95), "+Rp 2,1 Jt (+9,4%)", font=font(28, True), fill=GREEN, anchor="mm")
y += 210
d.text((48, y), "Kepemilikan", font=font(36, True), fill=WHITE)
y += 60
for code, det in [("BBCA", "100 lot \u2022 Avg Rp 9.500"), ("TLKM", "50 lot \u2022 Avg Rp 3.300")]:
    rrect(d, [36, y, W - 36, y + 140], 24, CARD)
    d.text((64, y + 20), code, font=font(34, True), fill=WHITE)
    d.text((64, y + 64), det, font=font(28), fill=GRAY)
    y += 152
bottom_nav(img, 1750 - 190, 4)
img.save(OUT + "portofolio.png")
print("portofolio.png ok")
