#!/usr/bin/env python3
"""Generate Cek BSU mockup HTML screens (390x844 phone)."""
import os

OUT = os.path.expanduser("~/workspace/cekbsu/mockups/src")
os.makedirs(OUT, exist_ok=True)

CSS = """
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;background:#F4F6FB;color:#1E293B;width:390px;min-height:844px;margin:0 auto}
.statusbar{height:44px;background:#0F2E7A;color:#fff;display:flex;align-items:center;justify-content:space-between;padding:0 22px;font-size:13px;font-weight:600}
.appbar{background:linear-gradient(135deg,#0F2E7A 0%,#1E56C8 100%);color:#fff;padding:14px 18px 20px}
.appbar .row{display:flex;align-items:center;gap:12px}
.logo{width:46px;height:46px;border-radius:14px;background:#fff;display:flex;align-items:center;justify-content:center;font-weight:800;color:#0F2E7A;font-size:15px;flex-shrink:0}
.appbar h1{font-size:19px;font-weight:700}
.appbar p{font-size:12px;opacity:.85;margin-top:2px}
.appbar .back{width:34px;height:34px;border-radius:10px;background:rgba(255,255,255,.18);display:flex;align-items:center;justify-content:center;font-size:18px;color:#fff}
.content{padding:16px 16px 96px}
.card{background:#fff;border-radius:16px;padding:16px;box-shadow:0 2px 12px rgba(15,46,122,.07);margin-bottom:14px}
.card h3{font-size:15px;font-weight:700;margin-bottom:8px}
.card p{font-size:13px;line-height:1.65;color:#475569}
.hero{background:linear-gradient(135deg,#0F2E7A,#2B6CE5);border-radius:20px;padding:20px;color:#fff;margin-bottom:14px;box-shadow:0 8px 24px rgba(15,46,122,.28);position:relative;overflow:hidden}
.hero::after{content:'';position:absolute;right:-40px;top:-40px;width:160px;height:160px;border-radius:50%;background:rgba(255,255,255,.08)}
.hero .badge{display:inline-block;background:#D7263D;color:#fff;font-size:11px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:10px}
.hero h2{font-size:15px;font-weight:600;opacity:.9}
.hero .nominal{font-size:34px;font-weight:800;margin:4px 0}
.hero .sub{font-size:12.5px;opacity:.85}
.disclaimer{background:#FFFBEB;border:1.5px solid #F59E0B;border-radius:14px;padding:12px 14px;margin-bottom:14px;display:flex;gap:10px}
.disclaimer .ic{font-size:20px;flex-shrink:0}
.disclaimer p{font-size:12px;line-height:1.6;color:#92400E}
.disclaimer b{color:#78350F}
.menu-grid{display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;margin-bottom:6px}
.menu{background:#fff;border-radius:16px;padding:16px 8px;text-align:center;box-shadow:0 2px 12px rgba(15,46,122,.07)}
.menu .mi{width:46px;height:46px;border-radius:14px;margin:0 auto 8px;display:flex;align-items:center;justify-content:center;font-size:22px;color:#fff;font-weight:700}
.menu span{font-size:11.5px;font-weight:600;color:#334155;line-height:1.35;display:block}
.check{display:flex;gap:12px;align-items:flex-start;padding:13px 0;border-bottom:1px solid #F1F5F9}
.check:last-child{border-bottom:none;padding-bottom:2px}
.check .ck{width:30px;height:30px;border-radius:50%;background:#DCFCE7;color:#16A34A;display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;flex-shrink:0}
.check b{font-size:13.5px;display:block;margin-bottom:3px}
.check p{font-size:12px;color:#64748B;line-height:1.55}
.tl{position:relative;padding-left:34px}
.tl::before{content:'';position:absolute;left:11px;top:10px;bottom:10px;width:2px;background:#E2E8F0}
.tl-item{position:relative;padding:0 0 20px}
.tl-item:last-child{padding-bottom:2px}
.tl-dot{position:absolute;left:-34px;top:2px;width:24px;height:24px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;color:#fff}
.st{display:inline-block;font-size:10.5px;font-weight:700;padding:4px 10px;border-radius:12px;margin-top:6px}
.st.done{background:#DCFCE7;color:#15803D}.st.run{background:#DBEAFE;color:#1D4ED8}.st.wait{background:#F1F5F9;color:#64748B}.st.info{background:#FEF3C7;color:#B45309}
.tl-item b{font-size:14px}.tl-item p{font-size:12.5px;color:#64748B;line-height:1.6;margin-top:3px}
.link{display:flex;align-items:center;gap:12px;padding:13px 0;border-bottom:1px solid #F1F5F9}
.link:last-child{border-bottom:none}
.link .li{width:46px;height:46px;border-radius:14px;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:800;font-size:16px;flex-shrink:0}
.link .lt{flex:1}.link .lt b{font-size:13.5px;display:block}.link .lt span{font-size:11.5px;color:#64748B}
.open{background:#EFF6FF;color:#1D4ED8;font-size:12px;font-weight:700;padding:8px 14px;border-radius:10px;border:none}
.faq{border-bottom:1px solid #F1F5F9;padding:14px 0}
.faq:last-child{border:none}
.faq .q{display:flex;justify-content:space-between;align-items:center;font-size:13.5px;font-weight:700;gap:8px}
.faq .chev{color:#94A3B8;font-size:14px;flex-shrink:0}
.faq .a{font-size:12.5px;color:#475569;line-height:1.7;margin-top:8px}
table.desil{width:100%;border-collapse:collapse;font-size:12px;margin-top:8px}
table.desil th{background:#0F2E7A;color:#fff;padding:9px 8px;text-align:left;font-size:11.5px}
table.desil th:first-child{border-radius:10px 0 0 0}table.desil th:last-child{border-radius:0 10px 0 0}
table.desil td{padding:9px 8px;border-bottom:1px solid #F1F5F9;line-height:1.5;vertical-align:top}
table.desil tr.hl td{background:#EFF6FF}
.pill{display:inline-block;font-size:10px;font-weight:700;padding:3px 9px;border-radius:10px}
.pill.g{background:#DCFCE7;color:#15803D}.pill.y{background:#FEF3C7;color:#B45309}.pill.r{background:#F1F5F9;color:#64748B}
.steps{counter-reset:s}
.step{display:flex;gap:12px;margin-bottom:14px}
.step .n{width:28px;height:28px;border-radius:50%;background:#0F2E7A;color:#fff;font-size:13px;font-weight:700;display:flex;align-items:center;justify-content:center;flex-shrink:0}
.step p{font-size:12.5px;line-height:1.65;color:#475569}.step p b{color:#1E293B}
.bottomnav{position:fixed;bottom:0;left:50%;transform:translateX(-50%);width:390px;background:#fff;border-top:1px solid #E2E8F0;display:flex;padding:10px 0 22px;box-shadow:0 -2px 16px rgba(0,0,0,.05)}
.nav-it{flex:1;text-align:center;font-size:10.5px;color:#94A3B8;font-weight:600}
.nav-it .ni{font-size:21px;display:block;margin-bottom:3px}
.nav-it.active{color:#0F2E7A}
.note{background:#EFF6FF;border-radius:14px;padding:12px 14px;margin-bottom:14px}
.note p{font-size:12px;line-height:1.65;color:#1E40AF}
.sect{font-size:14px;font-weight:700;margin:18px 2px 10px;color:#0F2E7A}
"""

def page(title, appbar, content, nav_active="Beranda"):
    nav = [("Beranda","🏠"),("Syarat","✅"),("Desil","📊"),("FAQ","❓")]
    nav_html = "".join(
        f'<div class="nav-it{" active" if n==nav_active else ""}"><span class="ni">{i}</span>{n}</div>'
        for n,i in nav)
    return f"""<!DOCTYPE html><html lang="id"><head><meta charset="utf-8">
<meta name="viewport" content="width=390">
<title>{title}</title><style>{CSS}</style></head><body>
<div class="statusbar"><span>16:37</span><span>📶 📡 🔋 98%</span></div>
<div class="appbar">{appbar}</div>
<div class="content">{content}</div>
<div class="bottomnav">{nav_html}</div>
</body></html>"""

def logo_html():
    return '<div class="logo">BSU</div>'

# ---------- 1. BERANDA ----------
beranda_appbar = f'<div class="row">{logo_html()}<div><h1>Cek BSU</h1><p>Panduan Bantuan Subsidi Upah 2026</p></div></div>'
menus = [
    ("Syarat","✅","#16A34A","Syarat Penerima"),
    ("Jadwal","🗓️","#1E56C8","Jadwal Pencairan"),
    ("Desil","📊","#7C3AED","Cek Desil"),
    ("Tautan","🔗","#D7263D","Tautan Resmi"),
    ("Berita","📰","#F59E0B","Berita & Update"),
    ("FAQ","❓","#0EA5E9","FAQ"),
]
menu_html = "".join(
    f'<div class="menu"><div class="mi" style="background:{c}">{i}</div><span>{t}</span></div>'
    for i,c,t in [(m[1],m[2],m[3]) for m in menus])
beranda_content = f"""
<div class="hero">
  <span class="badge">PROGRAM PEMERINTAH</span>
  <h2>Total Bantuan per Penerima</h2>
  <div class="nominal">Rp 600.000</div>
  <div class="sub">Rp 300.000 × 2 bulan &nbsp;•&nbsp; disalurkan via rekening / PosPay</div>
</div>
<div class="disclaimer"><span class="ic">⚠️</span>
<p><b>Bukan aplikasi resmi pemerintah.</b> Cek BSU adalah aplikasi panduan informasi. Data resmi hanya di <b>bsu.kemnaker.go.id</b>.</p></div>
<div class="sect">Menu Utama</div>
<div class="menu-grid">{menu_html}</div>
<div class="sect">Cara Cek Status Penerima</div>
<div class="card">
  <div class="step"><div class="n">1</div><p>Buka situs resmi <b>bsu.kemnaker.go.id</b></p></div>
  <div class="step"><div class="n">2</div><p>Masukkan <b>NIK 16 digit</b> sesuai KTP</p></div>
  <div class="step"><div class="n">3</div><p>Isi <b>kode captcha</b>, lalu klik <b>Cek Status</b></p></div>
</div>
<div class="note"><p>💡 <b>Tips:</b> Tidak perlu daftar mandiri — data calon penerima diambil langsung dari BPJS Ketenagakerjaan. Waspada tautan palsu yang meminta data pribadi.</p></div>
"""
pages = {"01_beranda": page("Cek BSU — Beranda", beranda_appbar, beranda_content, "Beranda")}

# ---------- 2. SYARAT ----------
syarat_appbar = '<div class="row"><div class="back">‹</div><div><h1>Syarat Penerima</h1><p>Kriteria calon penerima BSU 2026</p></div></div>'
syarat_items = [
    ("Warga Negara Indonesia","Memiliki NIK KTP elektronik yang valid dan terdaftar di Dukcapil."),
    ("Peserta aktif BPJS Ketenagakerjaan","Kategori Pekerja Penerima Upah (PU), terdaftar dan rutin membayar iuran hingga batas waktu yang ditentukan."),
    ("Gaji maksimal Rp 3.500.000/bulan","Atau mengikuti UMP/UMK wilayah bila lebih tinggi, sesuai ketentuan Kemnaker."),
    ("Belum menerima bansos lain","Diprioritaskan bagi yang belum menerima PKH atau bantuan sosial reguler lain pada periode yang sama."),
    ("Bukan ASN, TNI, atau Polri","Program ini khusus pekerja/buruh sektor formal di luar aparatur negara."),
]
syarat_html = "".join(
    f'<div class="check"><div class="ck">✓</div><div><b>{t}</b><p>{d}</p></div></div>'
    for t,d in syarat_items)
syarat_content = f"""
<div class="card"><h3>📋 5 Kriteria Utama</h3><p>Acuan ketentuan Kemnaker pada penyaluran sebelumnya. Ketentuan resmi 2026 mengikuti pengumuman pemerintah.</p>
<div style="margin-top:6px">{syarat_html}</div></div>
<div class="note"><p>ℹ️ <b>Tidak ada pendaftaran mandiri.</b> Perusahaan memastikan karyawan terdaftar aktif di BPJS Ketenagakerjaan — verifikasi dilakukan otomatis oleh sistem.</p></div>
"""
pages["02_syarat"] = page("Cek BSU — Syarat", syarat_appbar, syarat_content, "Syarat")

# ---------- 3. JADWAL ----------
jadwal_appbar = '<div class="row"><div class="back">‹</div><div><h1>Jadwal & Tahapan</h1><p>Alur penyaluran BSU</p></div></div>'
tahapan = [
    ("#16A34A","1","Pendataan peserta BPJS","BPJS Ketenagakerjaan menghimpun data pekerja penerima upah yang memenuhi batas gaji.","done","Selesai"),
    ("#1D4ED8","2","Verifikasi & validasi Kemnaker","Data disaring: NIK valid, iuran aktif, bukan ASN/TNI/Polri, belum terima bansos lain.","run","Berjalan"),
    ("#64748B","3","Penetapan daftar penerima","Kemnaker menetapkan daftar final penerima BSU 2026.","wait","Menunggu"),
    ("#64748B","4","Penyaluran ke rekening","Dana Rp 600.000 disalurkan sekaligus via bank Himbara / rekening terdaftar / PosPay.","wait","Menunggu"),
    ("#64748B","5","Cek status mandiri","Pekerja mengecek status di bsu.kemnaker.go.id dengan NIK.","wait","Menunggu"),
]
tl_html = "".join(
    f'<div class="tl-item"><div class="tl-dot" style="background:{c}">{n}</div><b>{t}</b><p>{d}</p><span class="st {s}">{lbl}</span></div>'
    for c,n,t,d,s,lbl in tahapan)
jadwal_content = f"""
<div class="disclaimer"><span class="ic">📢</span>
<p><b>Jadwal resmi BSU 2026 belum diumumkan</b> pemerintah. Tahapan di bawah mengacu pada pola penyaluran sebelumnya — pantau kanal resmi.</p></div>
<div class="card"><h3>Alur Penyaluran</h3><div class="tl" style="margin-top:12px">{tl_html}</div></div>
"""
pages["03_jadwal"] = page("Cek BSU — Jadwal", jadwal_appbar, jadwal_content, "Beranda")

# ---------- 4. TAUTAN RESMI ----------
tautan_appbar = '<div class="row"><div class="back">‹</div><div><h1>Tautan Resmi</h1><p>Hanya buka situs & aplikasi resmi</p></div></div>'
links = [
    ("BK","Cek Status BSU","bsu.kemnaker.go.id","#0F2E7A"),
    ("BJ","BPJS Ketenagakerjaan","bpjsketenagakerjaan.go.id","#16A34A"),
    ("JM","Aplikasi JMO","Google Play Store — Jamsostek Mobile","#1E56C8"),
    ("PP","PosPay / Kantor Pos","posindonesia.co.id","#F59E0B"),
    ("KM","Cek Bansos Kemensos","cekbansos.kemensos.go.id","#7C3AED"),
    ("KN","Kemnaker RI","kemnaker.go.id","#D7263D"),
]
link_html = "".join(
    f'<div class="link"><div class="li" style="background:{c}">{a}</div><div class="lt"><b>{t}</b><span>{u}</span></div><button class="open">Buka ↗</button></div>'
    for a,t,u,c in links)
tautan_content = f"""
<div class="card"><h3>🔗 Daftar Tautan</h3><div style="margin-top:4px">{link_html}</div></div>
<div class="disclaimer"><span class="ic">🛡️</span>
<p><b>Waspada penipuan!</b> Situs resmi tidak pernah meminta transfer uang, pulsa, atau kode OTP. Laporkan tautan mencurigakan ke kanal resmi Kemnaker.</p></div>
"""
pages["04_tautan"] = page("Cek BSU — Tautan Resmi", tautan_appbar, tautan_content, "Beranda")

# ---------- 5. FAQ ----------
faq_appbar = '<div class="row"><div class="back">‹</div><div><h1>FAQ</h1><p>Pertanyaan yang sering ditanyakan</p></div></div>'
faqs = [
    ("Apakah saya perlu mendaftar BSU secara mandiri?",
     "Tidak perlu. Data calon penerima diambil langsung dari BPJS Ketenagakerjaan. Pastikan perusahaan mendaftarkan Anda sebagai peserta aktif dan iuran dibayar rutin.", True),
    ("Berapa nominal BSU yang diterima?",
     "Rp 600.000 per penerima, yaitu Rp 300.000 per bulan untuk 2 bulan, disalurkan sekaligus melalui rekening bank atau PosPay.", False),
    ("Kapan BSU 2026 cair?",
     "Hingga saat ini belum ada pengumuman resmi dari Kemnaker mengenai jadwal pencairan BSU 2026. Waspadai kabar bohong (hoaks) di media sosial.", False),
    ("Data saya tidak muncul / tidak sesuai, apa yang harus dilakukan?",
     "1) Pastikan NIK valid di Dukcapil. 2) Konfirmasi ke HRD bahwa Anda terdaftar aktif di BPJS Ketenagakerjaan. 3) Jika data bansos salah, ajukan pembaruan via kelurahan/dinas sosial.", False),
    ("Apakah aplikasi Cek BSU ini resmi dari pemerintah?",
     "Bukan. Cek BSU adalah aplikasi panduan informasi independen dan tidak berafiliasi dengan Kemnaker, BPJS, atau Kemensos. Selalu verifikasi di situs resmi.", False),
]
faq_html = "".join(
    f'<div class="faq"><div class="q"><span>{q}</span><span class="chev">{"▾" if open else "▸"}</span></div>'
    + (f'<div class="a">{a}</div>' if open else "") + '</div>'
    for q,a,open in faqs)
faq_content = f'<div class="card"><h3>❓ Tanya Jawab</h3><div style="margin-top:6px">{faq_html}</div></div>'
pages["05_faq"] = page("Cek BSU — FAQ", faq_appbar, faq_content, "FAQ")

# ---------- 6. CEK DESIL ----------
desil_appbar = '<div class="row"><div class="back">‹</div><div><h1>Cek Desil</h1><p>Klasifikasi kesejahteraan DTSEN</p></div></div>'
desil_rows = [
    ("1","Sangat miskin","PKH, BPNT, beras","g","Sangat tinggi"),
    ("2–3","Miskin","PKH, BPNT","g","Tinggi"),
    ("4","Rentan miskin","PKH, BPNT","y","Sedang"),
    ("5","Menengah bawah","PBI Jaminan Kesehatan","y","Terbatas"),
    ("6–10","Menengah – mampu","Umumnya tidak menerima","r","Bukan prioritas"),
]
desil_tr = "".join(
    f'<tr class="hl"><td><b>Desil {d}</b></td><td>{k}</td><td>{b}</td><td><span class="pill {p}">{pr}</span></td></tr>'
    for d,k,b,p,pr in desil_rows)
desil_content = f"""
<div class="card"><h3>📊 Apa itu Desil?</h3>
<p>Desil adalah pengelompokan tingkat kesejahteraan keluarga dalam <b>Data Tunggal Sosial Ekonomi Nasional (DTSEN)</b> — integrasi DTKS, Regsosek, dan P3KE yang diselaraskan BPS.</p>
<p style="margin-top:8px">Masyarakat dibagi ke dalam <b>10 kelompok</b>: Desil 1 = kesejahteraan terendah, Desil 10 = tertinggi. Desil menentukan kelayakan berbagai program bansos pemerintah.</p></div>
<div class="card"><h3>Tabel Kategori Desil</h3>
<table class="desil"><tr><th>Desil</th><th>Kategori</th><th>Potensi Bansos</th><th>Prioritas</th></tr>{desil_tr}</table>
<p style="margin-top:10px;font-size:12px;color:#64748B">ℹ️ BSU mengacu pada data kepesertaan <b>BPJS Ketenagakerjaan</b>, sedangkan desil menjadi acuan utama bansos Kemensos (PKH, BPNT). Keduanya saling melengkapi dalam verifikasi.</p></div>
<div class="sect">Cara Cek Desil Anda</div>
<div class="card">
<div class="step"><div class="n">1</div><p><b>Situs Kemensos</b> — buka <b>cekbansos.kemensos.go.id</b>, isi wilayah, nama sesuai KTP, NIK, dan captcha, lalu klik <b>Cari Data</b>.</p></div>
<div class="step"><div class="n">2</div><p><b>Aplikasi "Cek Bansos"</b> — unduh di Play Store, daftar dengan foto KTP & swafoto, buka menu <b>Profil</b> untuk melihat desil.</p></div>
<div class="step"><div class="n">3</div><p><b>Kelurahan / Dinas Sosial</b> — datang dengan KTP & KK untuk pengecekan atau pembaruan data bila dinilai tidak sesuai.</p></div>
</div>
<div class="disclaimer"><span class="ic">⚠️</span>
<p><b>Hanya panduan.</b> Data desil resmi dikelola <b>Kemensos/BPS</b> dan bersifat dinamis. Aplikasi ini tidak menyimpan atau memproses NIK Anda.</p></div>
"""
pages["06_desil"] = page("Cek BSU — Cek Desil", desil_appbar, desil_content, "Desil")

for name, html in pages.items():
    with open(os.path.join(OUT, name + ".html"), "w") as f:
        f.write(html)
    print("wrote", name)

print("TOTAL:", len(pages))
