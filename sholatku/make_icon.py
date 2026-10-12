#!/usr/bin/env python3
"""Ikon launcher SholatKu: kubah emas di atas hijau zamrud."""
import os
from PIL import Image, ImageDraw

BASE = os.path.dirname(os.path.abspath(__file__))
EMERALD = (5, 58, 40)
GOLD = (212, 175, 55)
GOLD_LT = (232, 200, 100)

def make_icon(size):
    s = size
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # latar rounded square
    r = int(s * 0.22)
    d.rounded_rectangle([0, 0, s, s], r, fill=EMERALD)
    cx = s / 2
    # menara kiri kanan
    for sx in (-1, 1):
        tx = cx + sx * s * 0.30
        tw = s * 0.055
        d.rectangle([tx - tw/2, s*0.30, tx + tw/2, s*0.80], fill=GOLD)
        d.pieslice([tx - tw, s*0.30 - tw*2, tx + tw, s*0.30], 180, 360, fill=GOLD)
        d.ellipse([tx - s*0.02, s*0.20, tx + s*0.02, s*0.24], fill=GOLD_LT)
    # kubah utama
    kw = s * 0.44
    d.pieslice([cx - kw/2, s*0.34, cx + kw/2, s*0.34 + kw*0.75], 180, 360, fill=GOLD)
    d.rectangle([cx - kw/2, s*0.34 + kw*0.36, cx + kw/2, s*0.78], fill=GOLD)
    # finial + sabit kecil
    d.rectangle([cx - s*0.012, s*0.22, cx + s*0.012, s*0.34], fill=GOLD_LT)
    d.ellipse([cx - s*0.045, s*0.155, cx + s*0.045, s*0.245], fill=GOLD_LT)
    d.ellipse([cx - s*0.02, s*0.145, cx + s*0.07, s*0.235], fill=EMERALD)
    # pintu
    pw = s * 0.10
    d.rounded_rectangle([cx - pw/2, s*0.58, cx + pw/2, s*0.78], max(1, int(pw/4)), fill=EMERALD)
    return img

dens = {"mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192}
for name, px in dens.items():
    p = os.path.join(BASE, "src", "res", name, "ic_launcher.png")
    make_icon(px).save(p)
    print("icon:", p)
