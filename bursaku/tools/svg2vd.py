#!/usr/bin/env python3
"""Konversi Lucide SVG -> Android VectorDrawable.
Menangani: path, rect, circle, line, polyline, polygon.
Stroke: hitam (di-tint saat runtime seperti ikon lama).
"""
import re, sys, math, os

def num(x):
    s = ('%f' % float(x)).rstrip('0').rstrip('.')
    return '0' if s in ('-0', '') else s

def rect_to_path(x, y, w, h, rx=0):
    x, y, w, h = map(float, (x, y, w, h))
    rx = float(rx or 0)
    if rx <= 0:
        return f"M{num(x)},{num(y)}H{num(x+w)}V{num(y+h)}H{num(x)}Z"
    r = min(rx, w/2, h/2)
    return (f"M{num(x+r)},{num(y)}H{num(x+w-r)}"
            f"A{num(r)},{num(r)} 0 0 1 {num(x+w)},{num(y+r)}"
            f"V{num(y+h-r)}A{num(r)},{num(r)} 0 0 1 {num(x+w-r)},{num(y+h)}"
            f"H{num(x+r)}A{num(r)},{num(r)} 0 0 1 {num(x)},{num(y+h-r)}"
            f"V{num(y+r)}A{num(r)},{num(r)} 0 0 1 {num(x+r)},{num(y)}Z")

def circle_to_path(cx, cy, r):
    cx, cy, r = map(float, (cx, cy, r))
    return (f"M{num(cx-r)},{num(cy)}"
            f"A{num(r)},{num(r)} 0 1 0 {num(cx+r)},{num(cy)}"
            f"A{num(r)},{num(r)} 0 1 0 {num(cx-r)},{num(cy)}Z")

def line_to_path(x1, y1, x2, y2):
    return f"M{num(x1)},{num(y1)}L{num(x2)},{num(y2)}"

def points_to_path(pts, close):
    nums = [num(v) for v in re.findall(r'-?\d+\.?\d*', pts)]
    d = ''
    for i in range(0, len(nums)-1, 2):
        d += ('M' if i == 0 else 'L') + nums[i] + ',' + nums[i+1]
    return d + ('Z' if close else '')

def convert(svg_path):
    src = open(svg_path, encoding='utf-8').read()
    # ambil viewBox
    m = re.search(r'viewBox="([\d.\- ]+)"', src)
    vb = m.group(1) if m else '0 0 24 24'
    paths = []
    # <path d="...">
    for d in re.findall(r'<path[^>]*d="([^"]+)"', src):
        paths.append(d.replace('\n', ' ').strip())
    # <rect .../>
    for attrs in re.findall(r'<rect([^>]*)/?>', src):
        g = dict(re.findall(r'(\w[\w-]*)="([^"]*)"', attrs))
        paths.append(rect_to_path(g.get('x',0), g.get('y',0),
                                  g.get('width',0), g.get('height',0),
                                  g.get('rx',0)))
    # <circle .../>
    for attrs in re.findall(r'<circle([^>]*)/?>', src):
        g = dict(re.findall(r'(\w[\w-]*)="([^"]*)"', attrs))
        if 'r' in g:
            paths.append(circle_to_path(g.get('cx',0), g.get('cy',0), g['r']))
    # <line .../>
    for attrs in re.findall(r'<line([^>]*)/?>', src):
        g = dict(re.findall(r'(\w[\w-]*)="([^"]*)"', attrs))
        paths.append(line_to_path(g.get('x1',0), g.get('y1',0),
                                  g.get('x2',0), g.get('y2',0)))
    # <polyline>/<polygon>
    for tag, close in (('polyline', False), ('polygon', True)):
        for attrs in re.findall(r'<%s([^>]*)' % tag + r'/?>', src):
            g = dict(re.findall(r'(\w[\w-]*)="([^"]*)"', attrs))
            if 'points' in g:
                paths.append(points_to_path(g['points'], close))
    if not paths:
        raise ValueError('tidak ada path di ' + svg_path)
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- Lucide Icons v1.52.0 (ISC) - dikonversi ke VectorDrawable -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    android:width="24dp" android:height="24dp"',
           '    android:viewportWidth="24" android:viewportHeight="24">']
    for p in paths:
        out.append('    <path')
        out.append('        android:fillColor="#00000000"')
        out.append('        android:strokeColor="#000000"')
        out.append('        android:strokeWidth="2"')
        out.append('        android:strokeLineCap="round"')
        out.append('        android:strokeLineJoin="round"')
        out.append('        android:pathData="%s" />' % p)
    out.append('</vector>')
    return '\n'.join(out) + '\n'

# mapping: svg -> drawable target
MAP = {
    'chart.svg': 'ic_nav_pasar.xml',
    'tren-naik.svg': 'ic_nav_ranking.xml',
    'bintang.svg': 'ic_nav_watchlist.xml',
    'lonceng.svg': 'ic_nav_alert.xml',
    'dompet.svg': 'ic_nav_portfolio.xml',
    'candlestick.svg': 'ic_nav_analisa.xml',
    'layout-grid.svg': 'ic_nav_heatmap.xml',
}

def main():
    svg_dir, out_dir = sys.argv[1], sys.argv[2]
    for svg, target in MAP.items():
        sp = os.path.join(svg_dir, svg)
        if not os.path.exists(sp):
            print('LEWATI (tidak ada):', svg); continue
        xml = convert(sp)
        tp = os.path.join(out_dir, target)
        open(tp, 'w', encoding='utf-8').write(xml)
        print('OK:', target, '<-', svg)

if __name__ == '__main__':
    main()
