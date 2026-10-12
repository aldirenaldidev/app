#!/usr/bin/env python3
"""Screenshot tambahan BursaKu: bottom nav 6 item, autocomplete, analisa,
   dan penanda umur data (tiered cache). Mock PIL gaya fintech."""
from PIL import Image, ImageDraw, ImageFont
import os

W = 824
BG = (11, 15, 26)
CARD = (21, 27, 46)
SUBCARD = (11, 15, 26)
WHITE = (255, 255, 255)
GRAY = (139, 147, 167)
GREEN = (22, 199, 132)
RED = (234, 57, 67)
BLUE = (59, 130, 246)
PURPLE = (139, 92, 246)
ORANGE = (255, 176, 32)
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

NAV_ITEMS = ["Pasar", "Ranking", "Watchlist", "Alert", "Portofolio", "Analisa"]

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
            gradient_pill(img, [cw * i + 8, y0 + 18, cw * (i + 1) - 8, y0 + h - 18],
                          30, BLUE, PURPLE)
        d.ellipse([cx - 16, y0 + 42, cx + 16, y0 + 74],
                  fill=WHITE if on else GRAY)
        d.text((cx, y0 + 92), label, font=font(26, bold=on),
               fill=WHITE if on else GRAY, anchor="ma")

def screen_title(d, y, title, subtitle):
    d.text((48, y), title, font=font(52, True), fill=WHITE)
    d.text((48, y + 66), subtitle, font=font(28), fill=GRAY)
    return y + 116

def search_row(d, y, typed):
    rrect(d, [48, y, W - 220, y + 104], 16, (30, 38, 60))
    d.text((76, y + 30), typed, font=font(34), fill=WHITE)
    rrect(d, [W - 196, y, W - 48, y + 104], 16, BLUE)
    d.text((W - 122, y + 52), "Analisa", font=font(32, True), fill=WHITE, anchor="mm")
    return y + 104

# ============ 1. pasar.png (dengan bottom nav 6 item) ============
img = Image.new("RGB", (W, 1960), BG)
d = ImageDraw.Draw(img)
for y in range(300):
    t = y / 300
    d.line([(0, y), (W, y)],
           fill=(int(59 + 80 * t), int(130 - 38 * t), 246))
d.text((48, 40), "BursaKu", font=font(72, True), fill=WHITE)
d.text((48, 125), "Info pasar saham Indonesia", font=font(30), fill=(230, 235, 255))
d.ellipse([48, 185, 72, 209], fill=GREEN)
d.text((84, 178), "BUKA", font=font(34, True), fill=WHITE)
d.text((210, 182), "Sesi 2", font=font(30), fill=(230, 235, 255))
y = 340
d.text((48, y), "Indeks", font=font(38, True), fill=WHITE)
y += 62
rrect(d, [36, y, W - 36, y + 330], 28, CARD)
d.text((64, y + 24), "IHSG", font=font(28), fill=GRAY)
d.text((64, y + 54), "6.148,41", font=font(60, True), fill=WHITE)
d.text((64, y + 140), "+138,91 (+2,31%)", font=font(26, True), fill=GREEN)
d.text((64, y + 200), "LQ45", font=font(28), fill=GRAY)
d.text((64, y + 230), "606,28", font=font(52, True), fill=WHITE)
y += 360
d.text((48, y), "Valas & Komoditas", font=font(38, True), fill=WHITE)
y += 62
for i, (lb, val) in enumerate([("USD/IDR", "Rp 16.420"), ("Emas", "US$ 2.910"), ("Brent", "US$ 74,20")]):
    x = 36 + i * 258
    rrect(d, [x, y, x + 242, y + 170], 28, CARD)
    d.text((x + 24, y + 20), lb, font=font(26), fill=GRAY)
    d.text((x + 24, y + 56), val, font=font(36, True), fill=WHITE)
    d.text((x + 24, y + 112), "+0,25%", font=font(26, True), fill=GREEN)
y += 200
rrect(d, [36, y, W - 36, y + 190], 28, CARD)
d.text((64, y + 30), "Berita Pasar", font=font(36, True), fill=WHITE)
d.text((64, y + 84), "Headline saham terkini  •  otomatis", font=font(28), fill=GRAY)
d.text((W - 100, y + 95), ">", font=font(48, True), fill=GRAY)
y += 220
d.text((W // 2, y), "Terakhir diperbarui 14:05 WIB \u2022 otomatis", font=font(26),
       fill=GRAY, anchor="ma")
y += 60
d.text((W // 2, y), "\u00a9 2026 aldire__", font=font(26), fill=GRAY, anchor="ma")
bottom_nav(img, 1960 - 190, 0)
img.save(OUT + "pasar.png")
print("pasar.png ok")

# ============ 2. autocomplete.png ============
img = Image.new("RGB", (W, 1750), BG)
d = ImageDraw.Draw(img)
y = 60
y = screen_title(d, y, "Watchlist", "Emiten pantauan pribadi \u2014 harga terisi otomatis")
rrect(d, [48, y, W - 200, y + 104], 16, (30, 38, 60))
d.text((76, y + 30), "BB", font=font(34), fill=WHITE)
rrect(d, [W - 176, y, W - 48, y + 104], 16, BLUE)
d.text((W - 112, y + 52), "+", font=font(44, True), fill=WHITE, anchor="mm")
y += 112
sugs = [("BBCA", "Bank Central Asia Tbk."), ("BBRI", "Bank Rakyat Indonesia Tbk."),
        ("BBNI", "Bank Negara Indonesia Tbk."), ("BBTN", "Bank Tabungan Negara Tbk.")]
dy = y + 8
rrect(d, [48, dy, W - 48, dy + len(sugs) * 118 + 24], 20, CARD)
for i, (code, name) in enumerate(sugs):
    yy = dy + 12 + i * 118
    if i == 0:
        rrect(d, [56, yy - 4, W - 56, yy + 106], 14, (35, 45, 70))
    d.text((80, yy + 8), code, font=font(36, True), fill=WHITE)
    d.text((80, yy + 56), name, font=font(28), fill=GRAY)
    if i < len(sugs) - 1:
        d.line([(64, yy + 110), (W - 64, yy + 110)], fill=(40, 48, 70), width=2)
y = dy + len(sugs) * 118 + 60
d.text((48, y), "Terdaftar", font=font(38, True), fill=WHITE)
y += 62
for code, name, price, chg, up in [("TLKM", "Telkom Indonesia Tbk.", "4.120", "+1,23%", True),
                                    ("ASII", "Astra International Tbk.", "5.350", "-0,56%", False)]:
    rrect(d, [36, y, W - 36, y + 170], 28, CARD)
    d.text((64, y + 24), code, font=font(36, True), fill=WHITE)
    d.text((64, y + 72), name, font=font(28), fill=GRAY)
    d.text((W - 280, y + 30), price, font=font(38, True), fill=WHITE, anchor="ra")
    d.text((W - 280, y + 88), chg, font=font(30, True), fill=GREEN if up else RED, anchor="ra")
    y += 190
bottom_nav(img, 1750 - 190, 2)
img.save(OUT + "autocomplete.png")
print("autocomplete.png ok")

# ============ 3. analisa.png ============
img = Image.new("RGB", (W, 2450), BG)
d = ImageDraw.Draw(img)
y = 60
y = screen_title(d, y, "Analisa Teknikal", "Skor & alasan BELI / TAHAN / JUAL dari 13 indikator")
y = search_row(d, y, "BBCA")
y += 16
d.text((48, y), "BBCA \u2014 Bank Central Asia Tbk. \u2022 13 indikator dievaluasi",
       font=font(28), fill=GRAY)
y += 52
rrect(d, [36, y, W - 36, y + 260], 28, CARD)
d.text((W // 2, y + 40), "BELI", font=font(76, True), fill=GREEN, anchor="ma")
d.text((W // 2, y + 140), "Skor sinyal: +5 dari 13 indikator", font=font(30), fill=GRAY, anchor="ma")
d.text((W // 2, y + 190), "8 bullish  \u2022  3 bearish  \u2022  2 netral", font=font(28), fill=GRAY, anchor="ma")
y += 290
d.text((48, y), "Alasan", font=font(38, True), fill=WHITE)
y += 62
rrect(d, [36, y, W - 36, y + 640], 28, CARD)
reasons = [
    ("\u2713", GREEN, "Harga di atas MA20 & MA50 \u2014 tren jangka pendek-menengah bullish"),
    ("\u2713", GREEN, "Golden Cross: MA50 memotong MA200 ke atas \u2014 tren jangka panjang membaik"),
    ("\u2713", GREEN, "MACD di atas garis sinyal \u2014 momentum menguat"),
    ("\u2713", GREEN, "ADX 32,1 (tren kuat) & harga di atas MA50 \u2014 tren bullish solid"),
    ("\u2717", RED, "RSI 76,2 (overbought) \u2014 rawan koreksi"),
    ("\u2717", RED, "Harga menyentuh upper band (9.850,0) \u2014 rawan jenuh beli"),
    ("\u2022", GRAY, "Stochastic %K 64,2 \u2014 netral"),
]
yy = y + 24
for b, c, t in reasons:
    d.text((64, yy), b, font=font(32, True), fill=c)
    words, line, lines = t.split(" "), "", []
    for wd in words:
        if d.textlength(line + " " + wd, font=font(28)) > 600:
            lines.append(line)
            line = wd
        else:
            line = (line + " " + wd).strip()
    lines.append(line)
    for li, ln in enumerate(lines):
        d.text((104, yy + li * 40), ln, font=font(28), fill=WHITE)
    yy += len(lines) * 40 + 18
y += 670
d.text((48, y), "Rincian Indikator", font=font(38, True), fill=WHITE)
y += 62
rrect(d, [36, y, W - 36, y + 480], 28, CARD)
rows = [("RSI(14)", "76,2", "-1", RED), ("Tren MA", "9.425,0", "+1", GREEN),
        ("MACD", "45,2", "+1", GREEN), ("Bollinger", "9.425,0", "-1", RED),
        ("Stochastic", "64,2", "0", GRAY), ("ADX(14)", "32,1", "+1", GREEN)]
yy = y + 20
for ind, val, sc, c in rows:
    d.text((64, yy), ind, font=font(30), fill=WHITE)
    d.text((330, yy), val, font=font(28), fill=GRAY)
    rrect(d, [W - 190, yy - 6, W - 110, yy + 44], 12,
          (22, 80, 60) if c == GREEN else (90, 30, 38) if c == RED else (42, 51, 72))
    d.text((W - 150, yy + 19), sc, font=font(26, True), fill=WHITE, anchor="mm")
    yy += 66
y += 510
d.text((48, y), "Analisa ini dihitung otomatis dari indikator teknikal untuk tujuan edukasi,",
       font=font(24), fill=GRAY)
d.text((48, y + 34), "BUKAN rekomendasi investasi.", font=font(24, True), fill=GRAY)
bottom_nav(img, 2450 - 190, 5)
img.save(OUT + "analisa.png")
print("analisa.png ok")

# ============ 4. detail.png (penanda umur data) ============
img = Image.new("RGB", (W, 2400), BG)
d = ImageDraw.Draw(img)
y = 60
d.text((48, y), "BBCA \u2014 Bank Central Asia", font=font(48, True), fill=WHITE)
y += 70
d.text((48, y), "9.425", font=font(64, True), fill=WHITE)
d.text((300, y + 22), "+1,24%", font=font(34, True), fill=GREEN)
y += 110
rrect(d, [36, y, W - 36, y + 300], 28, CARD)
d.text((64, y + 24), "Grafik 2 tahun \u2022 MA20/MA50 \u2022 RSI \u2022 MACD",
       font=font(28), fill=GRAY)
d.text((64, y + 130), "[candlestick + panel indikator]", font=font(30), fill=(70, 80, 100))
d.text((W - 64, y + 250), "\u2014 diperbarui 2 jam lalu", font=font(26), fill=GRAY, anchor="ra")
y += 330

def section(title, lines, age):
    global y
    d.text((48, y), title, font=font(38, True), fill=WHITE)
    y += 62
    h = 70 + len(lines) * 48 + 60
    rrect(d, [36, y, W - 36, y + h], 28, CARD)
    yy = y + 28
    for t in lines:
        d.text((64, yy), t, font=font(30), fill=(200, 208, 225))
        yy += 48
    d.text((W - 64, yy + 8), "\u2014 diperbarui " + age, font=font(26), fill=GRAY, anchor="ra")
    y += h + 24

section("Pemegang Saham",
        ["\u2022 Insider/pengendali: 60,81%", "\u2022 Institusi: 19,53%",
         "\u2022 Jumlah institusi: 376"], "12 hari lalu")
section("Valuasi & Pertumbuhan",
        ["\u2022 P/E (harga/EPS): 12,76x", "\u2022 P/BV: 2,74x",
         "\u2022 Pertumbuhan laba YoY: +4,9%"], "12 hari lalu")
section("Dividen",
        ["\u2022 Dividend yield: 4,61%", "\u2022 Dividen tahunan: Rp 281/saham",
         "\u2022 Ex-dividen terakhir: 31 Agu 2026"], "12 hari lalu")
section("Info Emiten",
        ["\u2022 Sektor: Financial Services", "\u2022 Industri: Banks \u2014 Regional"],
        "2 bulan lalu")
bottom_nav(img, 2400 - 190, -1)
img.save(OUT + "detail.png")
print("detail.png ok")
