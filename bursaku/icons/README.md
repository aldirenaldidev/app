# Icon Set — BursaKu

## Set yang dipakai: Lucide

**Alasan pemilihan:**
- Memenuhi kriteria user (dari video TikTok @akhmuhammadi): minimalis, modern, konsisten,
  ketebalan garis seragam (stroke 2px), ujung membulat (rounded linecaps/joins), mudah dikenali.
- Paling lengkap untuk kebutuhan FINTECH/SAHAM: ada `chart-candlestick` (tidak ada di
  Feather/Phosphor), `trending-up/down`, `wallet`, `newspaper`, `list-filter`, dll.
- 1.800+ ikon berbasis grid 24x24; aktif dikembangkan (versi diunduh: **v1.52.0**).
- Ada resmi di Figma Community (file + plugin official tim Lucide).
- **Ringan**: total 21 SVG = kurang dari 9 KB (batas 200 KB), `stroke="currentColor"`
  sehingga warna diatur dari kode (cocok untuk tema pink ke ungu tua BursaKu).

## Lisensi

**ISC License** — permissive, bebas dipakai untuk proyek pribadi maupun komersial
**tanpa atribusi wajib**. (Ikon turunan Feather di dalamnya berlisensi MIT.)

Sumber resmi: https://github.com/lucide-icons/lucide dan https://lucide.dev/license

## Sumber Figma Community

- File: https://www.figma.com/community/file/939851755929765537/Lucide-Icons
- Plugin: https://www.figma.com/community/plugin/939567362549682242/Lucide-Icons

> SVG di folder ini diunduh dari paket resmi `lucide-static@1.52.0`
> (https://cdn.jsdelivr.net/npm/lucide-static@1.52.0/) — identik dengan ikon di Figma Community.

## Daftar ikon (icons/lucide/)

Semua stroke-based, viewBox 24x24, stroke-width 2, rounded caps, stroke="currentColor".

| File | Nama Lucide | Dipakai untuk |
|------|-------------|---------------|
| `chart.svg` | chart-line | Logo/chart umum |
| `candlestick.svg` | chart-candlestick | Grafik candlestick |
| `tren-naik.svg` | trending-up | Top gainer / harga naik |
| `tren-turun.svg` | trending-down | Top loser / harga turun |
| `dompet.svg` | wallet | Portofolio |
| `lonceng.svg` | bell | Alert harga |
| `berita.svg` | newspaper | Berita pasar |
| `search.svg` | search | Cari emiten |
| `bintang.svg` | star | Watchlist/favorit |
| `pengaturan.svg` | settings | Pengaturan |
| `info.svg` | info | Info/detail |
| `pin.svg` | map-pin | Pin (umum) |
| `bagikan.svg` | share-2 | Bagikan |
| `filter.svg` | list-filter | Filter ranking |
| `plus.svg` | plus | Tambah |
| `minus.svg` | minus | Kurang/hapus |
| `panah-kanan.svg` | chevron-right | Navigasi kanan |
| `jam.svg` | clock | Jam/status pasar |
| `kalender.svg` | calendar | Kalender/dividen |
| `home.svg` | house | Beranda (tab) |
| `list.svg` | list | Daftar/watchlist |

## Inspirasi desain dashboard fintech/saham (Figma Community, gratis)

> Hanya untuk referensi visual — file .fig TIDAK diunduh (berat), sesuai arahan.

1. **Viola UI** — UI kit Figma khusus fintech (investing, trading, wealth, portfolio);
   45 layar, light + dark mode, satu dataset konsisten.
   Ref: https://medium.com/@kseniyasavitsina/every-number-agrees-a-figma-ui-kit-for-fintech-built-on-one-dataset-58da09e73bd2
   (cari "Viola UI" di Figma Community untuk file-nya)
2. **Stock Market Mobile App (FTDesign)** — aplikasi saham mobile: home (5 versi),
   portfolio (2 versi), exchange, history; light + dark.
   Ref: https://figmafreebie.com/figma-ui-kit-stock-market-mobile-app-by-ftdesign-fintech-uikit-free-figma-template.html
3. **Fintech Finance App (Tsavsar)** — 16 layar aplikasi keuangan mobile, dark theme
   monokrom, cocok untuk referensi dark fintech.
   Ref: https://www.freefigmatemplates.com/gallery/fintech-finance-app

## Catatan teknis untuk build

- SVG memakai `stroke="currentColor"` — warna ikon mengikuti `android:tint` /
  `ImageView.setColorFilter()` atau CSS `color` bila dipakai di WebView.
- Untuk Android native: konversi ke VectorDrawable (Android Studio: New > Vector Asset
  > Local file) atau pakai langsung via WebView.
