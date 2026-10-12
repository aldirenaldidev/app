#!/usr/bin/env python3
"""Generate Cek BSU single-page WebView app (index.html) dari mockup HTML."""
import os, re

SRC = os.path.expanduser("~/workspace/cekbsu/mockups/src")
OUT = os.path.expanduser("~/workspace/cekbsu/src/assets/www/index.html")
os.makedirs(os.path.dirname(OUT), exist_ok=True)

def load(name):
    with open(os.path.join(SRC, name), encoding="utf-8") as f:
        return f.read()

def extract_css(html):
    m = re.search(r"<style>(.*?)</style>", html, re.S)
    return m.group(1) if m else ""

def extract_body_inner(html):
    # ambil isi antara <body>...</body>
    m = re.search(r"<body>(.*?)</body>", html, re.S)
    return m.group(1) if m else ""

def strip_chrome(inner):
    """Hapus statusbar, appbar, bottomnav mockup — diganti komponen app."""
    inner = re.sub(r'<div class="statusbar">.*?</div>', '', inner, flags=re.S)
    # appbar mockup selalu berpola: <div class="appbar"><div class="row"> ... </div></div>
    # hapus dengan pencocokan div berimbang
    inner = remove_balanced(inner, '<div class="appbar">')
    inner = re.sub(r'<div class="bottomnav">.*?</div>\s*$', '', inner, flags=re.S)
    return inner.strip()

def remove_balanced(html, start_tag):
    """Hapus blok mulai dari start_tag sampai penutup div yang berimbang."""
    i = html.find(start_tag)
    if i < 0:
        return html
    depth = 0
    j = i
    n = len(html)
    while j < n:
        if html.startswith("<div", j):
            depth += 1
            j = html.find(">", j) + 1
        elif html.startswith("</div>", j):
            depth -= 1
            j += 6
            if depth == 0:
                break
        else:
            j += 1
    return html[:i] + html[j:]

CSS = extract_css(load("01_beranda.html"))

# --- konten tiap layar (tanpa chrome mockup) ---
beranda_content = strip_chrome(extract_body_inner(load("01_beranda.html")))
syarat_content  = strip_chrome(extract_body_inner(load("02_syarat.html")))
jadwal_content  = strip_chrome(extract_body_inner(load("03_jadwal.html")))
tautan_content  = strip_chrome(extract_body_inner(load("04_tautan.html")))
faq_content     = strip_chrome(extract_body_inner(load("05_faq.html")))
desil_content   = strip_chrome(extract_body_inner(load("06_desil.html")))

# --- layar Berita (baru, statis) ---
berita_content = """
<div class="disclaimer"><span class="ic">📰</span>
<p><b>Info terkini.</b> Kabar resmi BSU hanya dari kanal pemerintah. Aplikasi ini merangkum tautan berita resmi.</p></div>
<div class="card"><h3>📰 Berita & Update Resmi</h3>
<div class="link"><div class="li" style="background:#0F2E7A">KN</div><div class="lt"><b>Berita Kemnaker RI</b><span>kemnaker.go.id/berita</span></div><button class="open" data-ext="https://kemnaker.go.id/berita">Buka ↗</button></div>
<div class="link"><div class="li" style="background:#16A34A">BJ</div><div class="lt"><b>Berita BPJS Ketenagakerjaan</b><span>bpjsketenagakerjaan.go.id/berita</span></div><button class="open" data-ext="https://www.bpjsketenagakerjaan.go.id/berita.html">Buka ↗</button></div>
<div class="link"><div class="li" style="background:#7C3AED">KM</div><div class="lt"><b>Berita Kemensos RI</b><span>kemensos.go.id</span></div><button class="open" data-ext="https://kemensos.go.id">Buka ↗</button></div>
</div>
<div class="note"><p>💡 <b>Tips:</b> Waspadai judul berita bombastis dari situs tidak dikenal. Selalu verifikasi ke situs resmi di atas.</p></div>
"""

# --- layar Tentang (baru) ---
tentang_content = """
<div class="card" style="text-align:center;padding:28px 16px">
<div class="logo" style="width:72px;height:72px;font-size:24px;margin:0 auto 14px;border-radius:20px">BSU</div>
<h3 style="font-size:18px">Cek BSU</h3>
<p style="font-size:12px;color:#64748B">Versi 1.0 • Panduan Bantuan Subsidi Upah</p>
</div>
<div class="card"><h3>Kenapa aplikasi ini dibuat</h3>
<p>Aplikasi ini dibuat untuk membantu para pekerja memahami program Bantuan Subsidi Upah (BSU) — syarat penerima, jadwal penyaluran, cara cek status, hingga klasifikasi desil — dalam satu tempat yang ringkas dan mudah dibaca.</p>
<p style="margin-top:8px"><b>Penting:</b> Ini <b>bukan</b> aplikasi resmi pemerintah. Seluruh data resmi hanya berasal dari kanal Kemnaker, BPJS Ketenagakerjaan, dan Kemensos.</p></div>
<div class="card" style="text-align:center">
<h3>Pengembang</h3>
<p style="font-size:14px">© aldire__</p>
<button class="open" data-ext="https://instagram.com/aldire__" style="margin-top:10px;font-size:14px;padding:10px 22px">📷 Instagram @aldire__</button>
</div>
<div class="disclaimer"><span class="ic">⚠️</span>
<p><b>Bukan aplikasi resmi pemerintah.</b> Aplikasi panduan informasi — data resmi hanya di <b>bsu.kemnaker.go.id</b>.</p></div>
"""

# --- definisi layar: id -> (judul, subjudul, konten, nav_aktif) ---
SCREENS = [
    ("beranda", "Cek BSU", "Panduan Bantuan Subsidi Upah 2026", beranda_content, "beranda", True),
    ("syarat",  "Syarat Penerima", "Kriteria calon penerima BSU 2026", syarat_content, "syarat", False),
    ("jadwal",  "Jadwal & Tahapan", "Alur penyaluran BSU", jadwal_content, None, False),
    ("tautan",  "Tautan Resmi", "Hanya buka situs & aplikasi resmi", tautan_content, None, False),
    ("berita",  "Berita & Update", "Kabar resmi dari kanal pemerintah", berita_content, None, False),
    ("faq",     "FAQ", "Pertanyaan yang sering ditanyakan", faq_content, "faq", False),
    ("desil",   "Cek Desil", "Klasifikasi kesejahteraan DTSEN", desil_content, "desil", False),
    ("tentang", "Tentang", "Info aplikasi & pengembang", tentang_content, None, False),
]

NAV = [("beranda","🏠","Beranda"),("syarat","✅","Syarat"),("desil","📊","Desil"),("faq","❓","FAQ")]

# ubah tombol "Buka" di tautan menjadi data-ext (link eksternal)
def fix_links(html):
    pairs = [
        ("bsu.kemnaker.go.id", "https://bsu.kemnaker.go.id"),
        ("bpjsketenagakerjaan.go.id", "https://www.bpjsketenagakerjaan.go.id"),
        ("Google Play Store — Jamsostek Mobile", "https://play.google.com/store/search?q=jmo%20bpjs%20ketenagakerjaan&c=apps"),
        ("posindonesia.go.id", "https://www.posindonesia.co.id"),
        ("cekbansos.kemensos.go.id", "https://cekbansos.kemensos.go.id"),
        ("kemnaker.go.id", "https://kemnaker.go.id"),
    ]
    # pola mockup: <button class="open">Buka ↗</button> mengikuti tiap .link
    # ganti berurutan sesuai kemunculan
    urls = ["https://bsu.kemnaker.go.id",
            "https://www.bpjsketenagakerjaan.go.id",
            "https://play.google.com/store/search?q=jmo%20bpjs%20ketenagakerjaan&c=apps",
            "https://www.posindonesia.co.id",
            "https://cekbansos.kemensos.go.id",
            "https://kemnaker.go.id"]
    out = html
    for u in urls:
        out = out.replace('<button class="open">Buka ↗</button>',
                          '<button class="open" data-ext="%s">Buka ↗</button>' % u, 1)
    return out

tautan_fixed = fix_links(tautan_content)

# menu grid beranda: jadikan bisa diklik
menu_js = """
<div class="menu-grid">
<div class="menu" data-go="syarat"><div class="mi" style="background:#16A34A">✅</div><span>Syarat Penerima</span></div>
<div class="menu" data-go="jadwal"><div class="mi" style="background:#1E56C8">🗓️</div><span>Jadwal Pencairan</span></div>
<div class="menu" data-go="desil"><div class="mi" style="background:#7C3AED">📊</div><span>Cek Desil</span></div>
<div class="menu" data-go="tautan"><div class="mi" style="background:#D7263D">🔗</div><span>Tautan Resmi</span></div>
<div class="menu" data-go="berita"><div class="mi" style="background:#F59E0B">📰</div><span>Berita & Update</span></div>
<div class="menu" data-go="faq"><div class="mi" style="background:#0EA5E9">❓</div><span>FAQ</span></div>
</div>
"""
beranda_fixed = re.sub(r'<div class="menu-grid">.*?</div>\s*</div>\s*(?=<div class="sect">)',
                       menu_js.strip(), beranda_content, flags=re.S)
# fallback: ganti blok menu-grid secara sederhana
if 'data-go=' not in beranda_fixed:
    beranda_fixed = re.sub(r'<div class="menu-grid">.*?</div>\s*(?=<div class="sect">Cara)',
                           menu_js, beranda_content, flags=re.S)

# FAQ accordion: sembunyikan jawaban kecuali pertama, tambah toggle
faq_fixed = faq_content
# bungkus jawaban 2..5 agar bisa di-toggle (mockup hanya menampilkan jawaban pertama)
faq_fixed = faq_fixed.replace(
    '<div class="faq"><div class="q"><span>Berapa nominal BSU yang diterima?</span><span class="chev">▸</span></div></div>',
    '<div class="faq"><div class="q"><span>Berapa nominal BSU yang diterima?</span><span class="chev">▸</span></div><div class="a" style="display:none">Total <b>Rp 600.000</b> per penerima (Rp 300.000 × 2 bulan), disalurkan sekaligus melalui rekening bank Himbara, rekening terdaftar, atau PosPay bagi yang belum punya rekening.</div></div>')
faq_fixed = faq_fixed.replace(
    '<div class="faq"><div class="q"><span>Kapan BSU 2026 cair?</span><span class="chev">▸</span></div></div>',
    '<div class="faq"><div class="q"><span>Kapan BSU 2026 cair?</span><span class="chev">▸</span></div><div class="a" style="display:none">Jadwal resmi <b>belum diumumkan</b> pemerintah. Pantau pengumuman di bsu.kemnaker.go.id dan kanal resmi Kemnaker. Waspadai kabar tanggal pasti dari sumber tidak resmi.</div></div>')
faq_fixed = faq_fixed.replace(
    '<div class="faq"><div class="q"><span>Data saya tidak muncul / tidak sesuai, apa yang harus dilakukan?</span><span class="chev">▸</span></div></div>',
    '<div class="faq"><div class="q"><span>Data saya tidak muncul / tidak sesuai, apa yang harus dilakukan?</span><span class="chev">▸</span></div><div class="a" style="display:none">Pastikan NIK valid dan Anda peserta aktif BPJS Ketenagakerjaan. Hubungi HRD perusahaan untuk memastikan data kepesertaan, atau datang ke kantor BPJS Ketenagakerjaan terdekat.</div></div>')
faq_fixed = faq_fixed.replace(
    '<div class="faq"><div class="q"><span>Apakah aplikasi Cek BSU ini resmi dari pemerintah?</span><span class="chev">▸</span></div></div>',
    '<div class="faq"><div class="q"><span>Apakah aplikasi Cek BSU ini resmi dari pemerintah?</span><span class="chev">▸</span></div><div class="a" style="display:none"><b>Bukan.</b> Cek BSU adalah aplikasi panduan informasi independen. Data resmi hanya di bsu.kemnaker.go.id dan kanal pemerintah lainnya.</div></div>')

screens_html = []
for sid, title, sub, content, nav_active, is_home in SCREENS:
    if sid == "tautan":
        content = tautan_fixed
    elif sid == "beranda":
        content = beranda_fixed
    elif sid == "faq":
        content = faq_fixed
    back_btn = '' if is_home else '<div class="back" data-back>‹</div>'
    info_btn = '<div class="back" data-go="tentang" style="margin-left:auto">ⓘ</div>' if is_home else ''
    # konten mockup sudah dibungkus <div class="content"> sendiri — jangan bungkus ganda
    screens_html.append(
        '<section class="screen" id="scr-%s" style="display:none">'
        '<div class="appbar"><div class="row">%s<div><h1>%s</h1><p>%s</p></div>%s</div></div>'
        '%s</section>' % (sid, back_btn, title, sub, info_btn, content))

nav_html = "".join(
    '<div class="nav-it%s" data-nav="%s"><span class="ni">%s</span>%s</div>'
    % (" active" if nid == "beranda" else "", nid, icon, label)
    for nid, icon, label in NAV)

EXTRA_CSS = """
.screen{display:block}
.menu{cursor:pointer}
.faq .q{cursor:pointer}
.faq .a{display:block}
.about-logo{width:72px;height:72px;border-radius:20px;background:#fff;display:flex;align-items:center;justify-content:center;font-weight:800;color:#0F2E7A;font-size:24px;margin:0 auto 14px;box-shadow:0 2px 12px rgba(15,46,122,.15)}
"""

JS = """
function go(id){
  document.querySelectorAll('.screen').forEach(function(s){s.style.display='none';});
  var el=document.getElementById('scr-'+id);
  if(el){el.style.display='block';}
  document.querySelectorAll('.nav-it').forEach(function(n){
    n.classList.toggle('active', n.getAttribute('data-nav')===id);
  });
  window.scrollTo(0,0);
}
document.addEventListener('click',function(e){
  var t=e.target;
  var goEl=t.closest('[data-go]');
  if(goEl){go(goEl.getAttribute('data-go'));return;}
  var navEl=t.closest('[data-nav]');
  if(navEl){go(navEl.getAttribute('data-nav'));return;}
  if(t.closest('[data-back]')){go('beranda');return;}
  var ext=t.closest('[data-ext]');
  if(ext){
    var url=ext.getAttribute('data-ext');
    if(window.AndroidBridge&&window.AndroidBridge.openUrl){window.AndroidBridge.openUrl(url);}
    else{window.open(url,'_blank');}
    return;
  }
  var q=t.closest('.faq .q');
  if(q){
    var a=q.parentElement.querySelector('.a');
    var chev=q.querySelector('.chev');
    if(a){
      var open=a.style.display!=='none';
      a.style.display=open?'none':'block';
      if(chev)chev.textContent=open?'▸':'▾';
    }
  }
});
go('beranda');
"""

html = """<!DOCTYPE html><html lang="id"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Cek BSU</title><style>
%s
%s
</style></head><body style="width:100%%;max-width:480px">
%s
<div class="bottomnav" style="width:100%%;max-width:480px">%s</div>
<script>%s</script>
</body></html>""" % (CSS, EXTRA_CSS, "\n".join(screens_html), nav_html, JS)

# body mockup memakai width:390px fixed — longgarkan untuk layar HP
html = html.replace("width:390px;min-height:844px;margin:0 auto",
                    "width:100%;min-height:100vh;margin:0 auto")

with open(OUT, "w", encoding="utf-8") as f:
    f.write(html)
print("written:", OUT, len(html), "bytes")
