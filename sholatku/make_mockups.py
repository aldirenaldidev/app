#!/usr/bin/env python3
"""Generator mockup desain UI SholatKu — gaya aplikasi top Play Store.

Bahasa visual: latar hijau zamrud PEKAT + ikon EMAS datar + ilustrasi masjid emas
sebagai hero + tipografi besar tebal. (render PIL, bukan screenshot perangkat)
"""
import os, math
import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")
os.makedirs(OUT, exist_ok=True)

W, H = 1080, 1920

# ---- Palet (gaya ref Play Store) ----
EM_TOP    = (14, 122, 82)     # hijau zamrud atas
EM_BOT    = (5, 58, 40)       # hijau zamrud pekat bawah
EM_DEEP   = (4, 42, 29)       # untuk status bar / nav
GOLD      = (212, 175, 55)    # emas datar
GOLD_LT   = (238, 205, 110)   # emas terang
GOLD_PALE = (253, 244, 214)   # emas pucat (kartu highlight)
WHITE     = (255, 255, 255)
CARD      = (255, 255, 255)
INK       = (24, 38, 32)
GRAY      = (110, 125, 118)
LGRAY     = (215, 224, 220)
MINT_TX   = (191, 227, 208)   # teks sekunder di atas hijau
SHADOW    = (3, 30, 20)

# ---- Font ----
FDN = "/usr/share/fonts/truetype/noto"
def font(path, size):
    return ImageFont.truetype(path, size)

F_REG  = f"{FDN}/NotoSans-Regular.ttf"
F_MED  = f"{FDN}/NotoSans-Medium.ttf"
F_BOLD = f"{FDN}/NotoSans-Bold.ttf"
F_XB   = f"{FDN}/NotoSans-ExtraBold.ttf"
F_AR   = f"{FDN}/NotoNaskhArabic-Bold.ttf"

def ar(t):
    return get_display(arabic_reshaper.reshape(t))

# ---- Helper ----
def vgrad(draw, box, c1, c2):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        t = (y - y0) / max(1, (y1 - y0))
        c = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
        draw.line([(x0, y), (x1, y)], fill=c)

def bg_base():
    img = Image.new("RGB", (W, H), EM_BOT)
    d = ImageDraw.Draw(img)
    vgrad(d, [0, 0, W, H], EM_TOP, EM_BOT)
    return img, d

def card(d, box, r=34, fill=CARD):
    x0, y0, x1, y1 = box
    d.rounded_rectangle([x0, y0 + 12, x1, y1 + 12], r, fill=SHADOW)
    d.rounded_rectangle(box, r, fill=fill)

def tcenter(d, cx, y, text, font, fill=WHITE):
    bb = d.textbbox((0, 0), text, font=font)
    w = bb[2] - bb[0]
    d.text((cx - w / 2, y), text, font=font, fill=fill)

def tright(d, x1, y, text, font, fill=WHITE):
    bb = d.textbbox((0, 0), text, font=font)
    w = bb[2] - bb[0]
    d.text((x1 - w, y), text, font=font, fill=fill)

def wrap(d, text, fnt, maxw):
    words, lines, cur = text.split(), [], ""
    for wd in words:
        t = (cur + " " + wd).strip()
        if d.textbbox((0, 0), t, font=fnt)[2] > maxw:
            lines.append(cur); cur = wd
        else:
            cur = t
    lines.append(cur)
    return lines


# ---- Helper ikon minimalis (gaya icon-set profesional) ----
def _sw(u):
    """Ketebalan garis SERAGAM: ~1/6 dari setengah ukuran ikon."""
    return max(4, int(round(u * 0.17)))

def rseg(d, x1, y1, x2, y2, w, color):
    """Segmen garis dengan ujung membulat (round caps)."""
    d.line([(x1, y1), (x2, y2)], fill=color, width=w)
    r = w / 2.0
    d.ellipse([x1 - r, y1 - r, x1 + r, y1 + r], fill=color)
    d.ellipse([x2 - r, y2 - r, x2 + r, y2 + r], fill=color)

def rpoly(d, pts, w, color, close=False):
    """Polyline dengan sambungan membulat (round joins)."""
    seq = list(pts) + ([pts[0]] if close else [])
    d.line(seq, fill=color, width=w, joint="curve")
    r = w / 2.0
    for (x, y) in seq:
        d.ellipse([x - r, y - r, x + r, y + r], fill=color)

def moon_path(d, cx, cy, u, w, color):
    """Bulan sabit outline: busur luar + busur dalam yang bertemu di ujung.
    Busur dalam digambar via polyline agar tidak bergantung pada perilaku
    wrap-around sudut di PIL."""
    R = 0.72 * u
    dd = 0.35 * R
    rr = 1.0 * R
    d.arc([cx - R, cy - R, cx + R, cy + R], start=80, end=280, width=w,
          fill=color)
    pts = []
    a = 260.0
    while a < 460.0:
        rad = math.radians(a)
        pts.append((cx + dd + rr * math.cos(rad), cy + rr * math.sin(rad)))
        a += 4.0
    rad = math.radians(460.0)
    pts.append((cx + dd + rr * math.cos(rad), cy + rr * math.sin(rad)))
    rpoly(d, pts, w, color)

# ---- Ilustrasi masjid EMAS (hero) ----
# ---- Ilustrasi masjid EMAS ala Masjid Kubah Emas Dian Al-Mahri (stilasi vektor) ----
def mosque_gold(cx, base_y, s=1.0, color=GOLD):
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    CUT = (5, 58, 40, 255)  # potongan jendela/pintu (warna dasar gelap)

    def arch(wx, top_y, wdt, hgt):
        d.rectangle([wx - wdt / 2, top_y, wx + wdt / 2, top_y + hgt], fill=CUT)
        d.pieslice([wx - wdt / 2, top_y - wdt / 2, wx + wdt / 2, top_y + wdt / 2],
                   180, 360, fill=CUT)

    def dome(kx, dy, r):
        # kubah setengah lingkaran + finial mungil
        d.pieslice([kx - r, dy - r, kx + r, dy + r], 180, 360, fill=color)
        fw = max(3, int(6 * s))
        d.rectangle([kx - fw / 2, dy - r - 30 * s, kx + fw / 2, dy - r + 4 * s],
                    fill=color)
        fr = max(4, int(9 * s))
        d.ellipse([kx - fr, dy - r - 30 * s - 2 * fr, kx + fr, dy - r - 30 * s],
                  fill=color)

    # --- 4 menara ramping (2 tinggi di sisi luar, 2 sedang di dalam)
    for off, th in [(-440, 470), (-315, 380), (315, 380), (440, 470)]:
        tx = cx + off * s
        tw = 40 * s
        top = base_y - th * s
        d.rectangle([tx - tw / 2, top, tx + tw / 2, base_y], fill=color)
        d.rectangle([tx - tw / 2 - 13 * s, top + th * s * 0.32,
                     tx + tw / 2 + 13 * s, top + th * s * 0.32 + 15 * s], fill=color)
        d.rectangle([tx - tw / 2 - 13 * s, top + th * s * 0.62,
                     tx + tw / 2 + 13 * s, top + th * s * 0.62 + 15 * s], fill=color)
        dome(tx, top, 28 * s)

    # --- badan utama + lis atap
    bw, bh = 620 * s, 200 * s
    d.rectangle([cx - bw / 2, base_y - bh, cx + bw / 2, base_y], fill=color)
    d.rectangle([cx - bw / 2 - 16 * s, base_y - bh - 18 * s,
                 cx + bw / 2 + 16 * s, base_y - bh], fill=color)

    # --- drum + kubah EMAS besar di tengah
    dw, dh = 320 * s, 60 * s
    d.rectangle([cx - dw / 2, base_y - bh - dh, cx + dw / 2, base_y - bh], fill=color)
    dome(cx, base_y - bh - dh, 150 * s)

    # --- 4 kubah emas kecil (2 kiri, 2 kanan)
    for sx in (-1, 1):
        for off in (215, 350):
            kx = cx + sx * off * s
            d.rectangle([kx - 55 * s, base_y - bh - 70 * s,
                         kx + 55 * s, base_y - bh], fill=color)
            dome(kx, base_y - bh - 70 * s, 62 * s)

    # --- pintu utama lengkung di tengah
    arch(cx, base_y - 150 * s, 110 * s, 150 * s)
    # --- deretan jendela lengkung
    for i in range(4):
        for sx in (-1, 1):
            wx = cx + sx * (150 + i * 68) * s
            arch(wx, base_y - 120 * s, 46 * s, 80 * s)
    return lay


def paste_rgba(base, lay):
    base.paste(lay, (0, 0), lay)

# ---- Ikon EMAS datar (gaya ref Play Store) ----
# ---- Ikon minimalis EMAS (gaya icon-set: stroke seragam, rounded) ----
def gold_icon(d, cx, cy, kind, s=1.0, color=GOLD):
    u = 46 * s
    w = _sw(u)
    if kind == "jadwal":  # jam
        d.ellipse([cx - u, cy - u, cx + u, cy + u], outline=color, width=w)
        rseg(d, cx, cy, cx, cy - 0.5 * u, w, color)
        rseg(d, cx, cy, cx + 0.36 * u, cy + 0.22 * u, w, color)
        r = w * 0.85
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    elif kind == "kiblat":  # kompas
        d.ellipse([cx - u, cy - u, cx + u, cy + u], outline=color, width=w)
        rpoly(d, [(cx, cy - 0.55 * u), (cx + 0.2 * u, cy),
                  (cx, cy + 0.55 * u), (cx - 0.2 * u, cy)], w, color, close=True)
        d.polygon([(cx, cy - 0.55 * u), (cx + 0.2 * u, cy), (cx, cy)], fill=color)
    elif kind == "tasbih":  # manik tasbih
        for a in range(0, 360, 36):
            px = cx + 0.68 * u * math.cos(math.radians(a))
            py = cy + 0.68 * u * math.sin(math.radians(a))
            r = 0.115 * u
            d.ellipse([px - r, py - r, px + r, py + r], fill=color)
    elif kind == "doa":  # buku terbuka
        rpoly(d, [(cx - u, cy - 0.45 * u), (cx, cy - 0.25 * u),
                  (cx, cy + 0.55 * u), (cx - u, cy + 0.35 * u)], w, color)
        rpoly(d, [(cx + u, cy - 0.45 * u), (cx, cy - 0.25 * u),
                  (cx, cy + 0.55 * u), (cx + u, cy + 0.35 * u)], w, color)
        rseg(d, cx, cy - 0.25 * u, cx, cy + 0.55 * u, w, color)
    elif kind == "dzikir":  # matahari terbit
        rseg(d, cx - u, cy + 0.4 * u, cx + u, cy + 0.4 * u, w, color)
        d.arc([cx - 0.55 * u, cy - 0.15 * u, cx + 0.55 * u, cy + 0.95 * u],
              start=180, end=360, width=w, fill=color)
        for deg in (-60, -30, 0, 30, 60):
            rad = math.radians(deg)
            x1 = cx + 0.74 * u * math.sin(rad)
            y1 = cy + 0.4 * u - 0.74 * u * math.cos(rad)
            x2 = cx + 0.94 * u * math.sin(rad)
            y2 = cy + 0.4 * u - 0.94 * u * math.cos(rad)
            rseg(d, x1, y1, x2, y2, w, color)
    elif kind == "asmaul":  # bulan sabit
        moon_path(d, cx, cy, u, w, color)
    elif kind == "kalender":  # kalender
        d.rounded_rectangle([cx - u, cy - 0.72 * u, cx + u, cy + 0.72 * u],
                            int(w * 1.4), outline=color, width=w)
        rseg(d, cx - u, cy - 0.22 * u, cx + u, cy - 0.22 * u, w, color)
        rseg(d, cx - 0.45 * u, cy - 1.0 * u, cx - 0.45 * u, cy - 0.48 * u, w, color)
        rseg(d, cx + 0.45 * u, cy - 1.0 * u, cx + 0.45 * u, cy - 0.48 * u, w, color)
        for gx in (-0.42, 0.0, 0.42):
            for gy in (0.16, 0.46):
                r = w * 0.6
                d.ellipse([cx + gx * u - r, cy + gy * u - r,
                           cx + gx * u + r, cy + gy * u + r], fill=color)
    elif kind == "wudhu":  # tetes air
        rpoly(d, [(cx, cy - 0.75 * u), (cx + 0.4 * u, cy - 0.08 * u),
                  (cx + 0.52 * u, cy + 0.22 * u), (cx + 0.34 * u, cy + 0.54 * u),
                  (cx, cy + 0.68 * u), (cx - 0.34 * u, cy + 0.54 * u),
                  (cx - 0.52 * u, cy + 0.22 * u), (cx - 0.4 * u, cy - 0.08 * u)],
               w, color, close=True)
    elif kind == "sholat":  # kubah
        d.arc([cx - 0.7 * u, cy - 0.55 * u, cx + 0.7 * u, cy + 0.85 * u],
              start=180, end=360, width=w, fill=color)
        rseg(d, cx - 0.7 * u, cy + 0.15 * u, cx + 0.7 * u, cy + 0.15 * u, w, color)
        rseg(d, cx - 0.7 * u, cy + 0.15 * u, cx - 0.7 * u, cy + 0.62 * u, w, color)
        rseg(d, cx + 0.7 * u, cy + 0.15 * u, cx + 0.7 * u, cy + 0.62 * u, w, color)
        rseg(d, cx - 0.7 * u, cy + 0.62 * u, cx + 0.7 * u, cy + 0.62 * u, w, color)
        rseg(d, cx, cy - 0.88 * u, cx, cy - 0.55 * u, w, color)
        r = w * 0.85
        d.ellipse([cx - r, cy - 0.88 * u - r, cx + r, cy - 0.88 * u + r], fill=color)


def bell_icon(d, cx, cy, s=1.0, color=GOLD):
    u = 30 * s
    w = _sw(u)
    rpoly(d, [(cx - 0.58 * u, cy + 0.35 * u), (cx - 0.5 * u, cy - 0.05 * u),
              (cx - 0.32 * u, cy - 0.42 * u), (cx, cy - 0.55 * u),
              (cx + 0.32 * u, cy - 0.42 * u), (cx + 0.5 * u, cy - 0.05 * u),
              (cx + 0.58 * u, cy + 0.35 * u)], w, color)
    rseg(d, cx - 0.64 * u, cy + 0.35 * u, cx + 0.64 * u, cy + 0.35 * u, w, color)
    d.ellipse([cx - 0.15 * u, cy + 0.42 * u, cx + 0.15 * u, cy + 0.72 * u],
              outline=color, width=w)
    rseg(d, cx, cy - 0.55 * u, cx, cy - 0.74 * u, w, color)


def status_bar(d):
    d.rectangle([0, 0, W, 64], fill=EM_DEEP)
    d.text((48, 12), "09:41", font=font(F_BOLD, 34), fill=WHITE)
    x = W - 48
    d.rounded_rectangle([x - 70, 22, x, 44], 6, outline=WHITE, width=4)
    d.rectangle([x - 64, 28, x - 20, 38], fill=WHITE)
    for i, h in enumerate([14, 20, 26]):
        d.rectangle([x - 110 - i * 16, 44 - h, x - 100 - i * 16, 44], fill=WHITE)

def mockup_tag(d):
    tag = "MOCKUP \u2014 bukan screenshot perangkat"
    f = font(F_BOLD, 26)
    bb = d.textbbox((0, 0), tag, font=f)
    tw = bb[2] - bb[0] + 60
    x0 = (W - tw) / 2
    d.rounded_rectangle([x0, 76, x0 + tw, 124], 24, fill=GOLD)
    tcenter(d, W / 2, 82, tag, f, fill=(90, 60, 8))

def nav_icon(d, cx, cy, idx, active):
    c = GOLD if active else (140, 175, 158)
    u = 30
    w = _sw(u)
    if idx == 0:  # beranda
        rpoly(d, [(cx - 0.62 * u, cy + 0.55 * u), (cx - 0.62 * u, cy - 0.05 * u),
                  (cx, cy - 0.62 * u), (cx + 0.62 * u, cy - 0.05 * u),
                  (cx + 0.62 * u, cy + 0.55 * u)], w, c)
        rseg(d, cx - 0.62 * u, cy + 0.55 * u, cx + 0.62 * u, cy + 0.55 * u, w, c)
        rpoly(d, [(cx - 0.2 * u, cy + 0.55 * u), (cx - 0.2 * u, cy + 0.12 * u),
                  (cx + 0.2 * u, cy + 0.12 * u), (cx + 0.2 * u, cy + 0.55 * u)],
               w, c)
    elif idx == 1:  # jadwal
        d.ellipse([cx - u, cy - u, cx + u, cy + u], outline=c, width=w)
        rseg(d, cx, cy, cx, cy - 0.5 * u, w, c)
        rseg(d, cx, cy, cx + 0.36 * u, cy + 0.22 * u, w, c)
        r = w * 0.85
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=c)
    elif idx == 2:  # tasbih
        for a in range(0, 360, 45):
            px = cx + 0.68 * u * math.cos(math.radians(a))
            py = cy + 0.68 * u * math.sin(math.radians(a))
            r = 0.115 * u
            d.ellipse([px - r, py - r, px + r, py + r], fill=c)
    elif idx == 3:  # doa
        rpoly(d, [(cx - u, cy - 0.45 * u), (cx, cy - 0.25 * u),
                  (cx, cy + 0.55 * u), (cx - u, cy + 0.35 * u)], w, c)
        rpoly(d, [(cx + u, cy - 0.45 * u), (cx, cy - 0.25 * u),
                  (cx, cy + 0.55 * u), (cx + u, cy + 0.35 * u)], w, c)
        rseg(d, cx, cy - 0.25 * u, cx, cy + 0.55 * u, w, c)
    else:  # tentang (info)
        d.ellipse([cx - u, cy - u, cx + u, cy + u], outline=c, width=w)
        r = w * 0.95
        d.ellipse([cx - r, cy - 0.38 * u - r, cx + r, cy - 0.38 * u + r], fill=c)
        rseg(d, cx, cy - 0.02 * u, cx, cy + 0.5 * u, w, c)


def bottom_nav(d, active):
    y0 = H - 172
    d.rectangle([0, y0, W, H], fill=EM_DEEP)
    d.line([(0, y0), (W, y0)], fill=GOLD, width=3)
    for i, name in enumerate(["Beranda", "Jadwal", "Tasbih", "Doa", "Tentang"]):
        cx = W * (i + 0.5) / 5
        nav_icon(d, cx, y0 + 54, i, i == active)
        tcenter(d, cx, y0 + 96, name, font(F_BOLD, 28),
                fill=GOLD if i == active else (140, 175, 158))
    tcenter(d, W / 2, H - 38, "Mockup desain \u2014 bukan screenshot perangkat",
            font(F_REG, 24), fill=(140, 175, 158))

# =====================================================================
def screen_home():
    img, d = bg_base()
    status_bar(d)
    mockup_tag(d)

    # tanggal Hijriah emas + Masehi
    tcenter(d, W / 2, 150, "15 Dzulqa'dah 1447", font(F_BOLD, 38), fill=GOLD_LT)
    tcenter(d, W / 2, 200, "Sabtu, 10 Oktober 2026 \u2022 Jakarta",
            font(F_REG, 32), fill=MINT_TX)

    # hero: masjid emas besar
    paste_rgba(img, mosque_gold(W / 2, 640, s=1.0))

    # sholat berikutnya — tipografi BESAR tebal
    tcenter(d, W / 2, 680, "SHOLAT BERIKUTNYA", font(F_BOLD, 30), fill=MINT_TX)
    tcenter(d, W / 2, 716, "Dzuhur", font(F_XB, 120), fill=WHITE)
    tcenter(d, W / 2, 850, "01 : 24 : 36", font(F_XB, 64), fill=GOLD_LT)
    tcenter(d, W / 2, 930, "Adzan pukul 11.48 \u2022 Pengingat aktif",
            font(F_REG, 30), fill=MINT_TX)

    # grid menu fitur: ikon EMAS datar + label putih
    feats = [("Jadwal\nSholat", "jadwal"), ("Arah\nKiblat", "kiblat"),
             ("Tasbih\nDigital", "tasbih"), ("Doa\nHarian", "doa"),
             ("Dzikir Pagi\n& Petang", "dzikir"), ("Asmaul\nHusna", "asmaul"),
             ("Kalender\nHijriah", "kalender"), ("Panduan\nWudhu", "wudhu"),
             ("Panduan\nSholat", "sholat")]
    f = font(F_BOLD, 27)
    for i, (label, kind) in enumerate(feats):
        r, c = divmod(i, 3)
        cx = 48 + c * 336 + 152
        cy = 1030 + r * 218
        gold_icon(d, cx, cy - 30, kind, s=1.0)
        for j, ln in enumerate(label.split("\n")):
            tcenter(d, cx, cy + 38 + j * 36, ln, f, fill=WHITE)
    bottom_nav(d, 0)
    img.save(f"{OUT}/home.png")

# =====================================================================
PRAYERS = [("Subuh", "04.32", False), ("Terbit", "05.47", False),
           ("Dzuhur", "11.48", True), ("Ashar", "15.02", False),
           ("Maghrib", "17.52", False), ("Isya", "19.05", False)]

def screen_jadwal():
    img, d = bg_base()
    status_bar(d)
    mockup_tag(d)
    d.text((72, 150), "Jadwal Sholat", font=font(F_XB, 64), fill=WHITE)
    d.text((72, 232), "Jakarta \u2022 Sabtu, 10 Oktober 2026",
           font=font(F_REG, 34), fill=MINT_TX)

    y = 330
    for name, t, hl in PRAYERS:
        card(d, [48, y, W - 48, y + 158], r=30,
             fill=GOLD_PALE if hl else CARD)
        if hl:
            d.rounded_rectangle([48, y, W - 48, y + 158], 30, outline=GOLD, width=5)
        # ikon lonceng pengingat
        bell_icon(d, 150, y + 79, s=1.0, color=GOLD if True else GRAY)
        d.text((220, y + 34), name, font=font(F_BOLD, 46),
               fill=(6, 95, 70) if hl else INK)
        if hl:
            d.text((220, y + 100), "Berikutnya \u2022 01:24:36 lagi",
                   font=font(F_REG, 32), fill=GRAY)
        else:
            d.text((220, y + 100), "Pengingat adzan aktif",
                   font=font(F_REG, 32), fill=GRAY)
        tright(d, W - 120, y + 52, t, font(F_XB, 52),
               fill=(6, 95, 70) if hl else INK)
        y += 182
    d.text((72, y + 8), "Metode perhitungan: Kemenag RI.",
           font=font(F_REG, 30), fill=MINT_TX)
    bottom_nav(d, 1)
    img.save(f"{OUT}/jadwal.png")

# =====================================================================
def screen_tasbih():
    img, d = bg_base()
    status_bar(d)
    mockup_tag(d)
    tcenter(d, W / 2, 150, "Tasbih Digital", font=font(F_XB, 60), fill=WHITE)
    tcenter(d, W / 2, 228, "Ketuk tombol untuk berdzikir",
            font=font(F_REG, 34), fill=MINT_TX)

    # kartu dzikir (terang di atas hijau pekat)
    card(d, [48, 330, W - 48, 620])
    tcenter(d, W / 2, 356, "DZIKIR SAAT INI", font=font(F_BOLD, 28), fill=GRAY)
    d.line([(120, 410), (W - 120, 410)], fill=GOLD, width=4)
    tcenter(d, W / 2, 428,
            ar("\u0633\u064f\u0628\u0652\u062d\u064e\u0627\u0646\u064e \u0627\u0644\u0644\u064e\u0651\u0647\u0650"),
            font(F_AR, 72), fill=(6, 95, 70))
    tcenter(d, W / 2, 540, "Subhanallah \u2014 Maha Suci Allah",
            font=font(F_REG, 34), fill=GRAY)

    # lingkaran counter besar aksen emas
    cx, cy, r = W / 2, 930, 215
    d.ellipse([cx - r - 16, cy - r - 16, cx + r + 16, cy + r + 16], fill=(4, 42, 29))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=CARD)
    d.arc([cx - r + 14, cy - r + 14, cx + r - 14, cy + r - 14],
          start=-90, end=-90 + 360 * 0.33, fill=GOLD, width=26)
    tcenter(d, cx, cy - 95, "33", font=font(F_XB, 150), fill=(6, 95, 70))
    tcenter(d, cx, cy + 70, "dari target 100", font=font(F_REG, 34), fill=GRAY)

    # tombol KETUK emas
    d.rounded_rectangle([200, 1210, W - 200, 1370], 80, fill=GOLD)
    tcenter(d, W / 2, 1248, "KETUK", font=font(F_XB, 56), fill=(4, 42, 29))

    tcenter(d, W / 2, 1410, "Target dzikir", font=font(F_BOLD, 34), fill=WHITE)
    bw = 240
    for i, o in enumerate(["33", "100", "1000"]):
        x0 = W / 2 - 1.5 * (bw + 24) + i * (bw + 24)
        sel = (o == "33")
        d.rounded_rectangle([x0, 1460, x0 + bw, 1540], 40,
                            fill=GOLD if sel else (4, 42, 29),
                            outline=GOLD, width=4)
        tcenter(d, x0 + bw / 2, 1472, o, font=font(F_BOLD, 36),
                fill=(4, 42, 29) if sel else WHITE)
    bottom_nav(d, 2)
    img.save(f"{OUT}/tasbih.png")

# =====================================================================
DOAS = [
    ("Doa Sebelum Makan",
     "\u0627\u064e\u0644\u0644\u0647\u064f\u0645\u064e\u0651 \u0628\u064e\u0627\u0631\u0650\u0643\u0652 \u0644\u064e\u0646\u064e\u0627 \u0641\u0650\u064a\u0645\u064e\u0627 \u0631\u064e\u0632\u064e\u0642\u0652\u062a\u064e\u0646\u064e\u0627",
     "Allahumma baarik lanaa fiimaa razaqtanaa",
     "Ya Allah, berkahilah rezeki yang Engkau berikan kepada kami."),
    ("Doa Sesudah Makan",
     "\u0627\u064e\u0644\u0652\u062d\u064e\u0645\u0652\u062f\u064f \u0644\u0650\u0644\u064e\u0651\u0647\u0650",
     "Alhamdulillaah",
     "Segala puji bagi Allah."),
    ("Doa Sebelum Tidur",
     "\u0628\u0650\u0627\u0633\u0652\u0645\u0650\u0643\u064e \u0627\u0644\u0644\u0647\u064f\u0645\u064e\u0651 \u0623\u064e\u062d\u0652\u064a\u064e\u0627 \u0648\u064e\u0623\u064e\u0645\u064f\u0648\u062a\u064f",
     "Bismikallaahumma ahyaa wa amuut",
     "Dengan nama-Mu ya Allah aku hidup dan aku mati."),
    ("Doa Bangun Tidur",
     "\u0627\u064e\u0644\u0652\u062d\u064e\u0645\u0652\u062f\u064f \u0644\u0650\u0644\u064e\u0651\u0647\u0650 \u0627\u0644\u064e\u0651\u0630\u0650\u064a \u0623\u064e\u062d\u0652\u064a\u064e\u0627\u0646\u064e\u0627",
     "Alhamdulillaahilladzii ahyaanaa",
     "Segala puji bagi Allah yang telah menghidupkan kami."),
]

def screen_doa():
    img, d = bg_base()
    status_bar(d)
    mockup_tag(d)
    tcenter(d, W / 2, 150, "Doa Harian", font=font(F_XB, 60), fill=WHITE)
    tcenter(d, W / 2, 228, "Kumpulan doa sehari-hari",
            font=font(F_REG, 34), fill=MINT_TX)

    card(d, [48, 320, W - 48, 410], r=45)
    d.ellipse([96, 346, 140, 390], outline=GRAY, width=6)
    rseg(d, 130, 380, 152, 402, 6, GRAY)
    d.text((170, 346), "Cari doa...", font=font(F_REG, 34), fill=GRAY)

    y = 448
    for title, arabic, latin, trans in DOAS:
        h = 330
        if y + h > H - 200:
            break
        card(d, [48, y, W - 48, y + h], r=30)
        d.rounded_rectangle([72, y + 24, 96, y + 120], 12, fill=GOLD)
        d.text((120, y + 28), title, font=font(F_BOLD, 40), fill=(6, 95, 70))
        tcenter(d, W / 2, y + 96, ar(arabic), font=font(F_AR, 52), fill=INK)
        d.text((72, y + 180), latin, font=font(F_REG, 32), fill=GRAY)
        for j, ln in enumerate(wrap(d, trans, font(F_REG, 30), W - 220)[:2]):
            d.text((72, y + 228 + j * 40), ln, font=font(F_REG, 30), fill=INK)
        y += h + 24
    bottom_nav(d, 3)
    img.save(f"{OUT}/doa.png")

# =====================================================================
def screen_tentang():
    img, d = bg_base()
    status_bar(d)
    mockup_tag(d)
    tcenter(d, W / 2, 150, "Tentang", font=font(F_XB, 60), fill=WHITE)

    # ikon aplikasi: kubah outline + bulan sabit (bahasa icon-set)
    ix, iy, ir = W / 2, 420, 110
    d.rounded_rectangle([ix - ir, iy - ir, ix + ir, iy + ir], 48, fill=(4, 42, 29))
    d.rounded_rectangle([ix - ir, iy - ir, ix + ir, iy + ir], 48, outline=GOLD, width=6)
    uw, ww = 52, _sw(52)
    d.arc([ix - uw, iy - 30 - uw, ix + uw, iy - 30 + uw], start=180, end=360, width=ww,
          fill=GOLD)
    rseg(d, ix - uw, iy - 30, ix + uw, iy - 30, ww, GOLD)
    rseg(d, ix - uw, iy - 30, ix - uw, iy + 44, ww, GOLD)
    rseg(d, ix + uw, iy - 30, ix + uw, iy + 44, ww, GOLD)
    rseg(d, ix - uw, iy + 44, ix + uw, iy + 44, ww, GOLD)
    rseg(d, ix, iy - 30 - uw - 26, ix, iy - 30 - uw, ww, GOLD)
    moon_path(d, ix + 52, iy - 30 - uw - 34, 26, _sw(26), GOLD)

    tcenter(d, W / 2, 560, "SholatKu", font=font(F_XB, 64), fill=WHITE)
    tcenter(d, W / 2, 640, "Sholat, Dzikir & Doa \u2022 v1.0",
            font=font(F_REG, 34), fill=MINT_TX)
    tcenter(d, W / 2, 700, "\u00a9 aldire__", font=font(F_BOLD, 38), fill=GOLD_LT)

    d.rounded_rectangle([240, 770, W - 240, 870], 50, fill=(4, 42, 29), outline=GOLD,
                        width=4)
    tcenter(d, W / 2, 792, "@aldire__", font=font(F_BOLD, 44), fill=GOLD_LT)

    card(d, [48, 920, W - 48, 1560], r=30)
    d.text((96, 950), "Kenapa aplikasi ini dibuat", font=font(F_BOLD, 42),
           fill=(6, 95, 70))
    d.line([(96, 1010), (W - 96, 1010)], fill=GOLD, width=4)
    para = ("SholatKu dibuat untuk membantu umat Muslim menjalankan "
            "ibadah harian dengan lebih mudah: jadwal sholat yang akurat, "
            "pengingat adzan, tasbih digital, serta kumpulan doa dan dzikir "
            "dalam satu aplikasi yang ringan dan gratis.")
    for j, ln in enumerate(wrap(d, para, font(F_REG, 34), W - 220)):
        d.text((96, 1040 + j * 52), ln, font=font(F_REG, 34), fill=INK)
    d.text((96, 1040 + len(wrap(d, para, font(F_REG, 34), W - 220)) * 52 + 24),
           "Jadwal sholat memakai metode Kemenag RI.",
           font=font(F_REG, 30), fill=GRAY)
    bottom_nav(d, 4)
    img.save(f"{OUT}/tentang.png")

# =====================================================================
if __name__ == "__main__":
    screen_home()
    screen_jadwal()
    screen_tasbih()
    screen_doa()
    screen_tentang()
    print("Mockup tersimpan di:", OUT)
    for f in sorted(os.listdir(OUT)):
        print(" -", f)
