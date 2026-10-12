# Icon Set — SholatKu

## Set yang dipakai: Lucide

**Alasan pemilihan:**
- Memenuhi semua kriteria user (dari video TikTok @akhmuhammadi): minimalis, modern,
  konsisten, ketebalan garis seragam (stroke 2px), ujung membulat (rounded linecaps/joins),
  mudah dikenali, tidak jadul.
- 1.800+ ikon berbasis grid 24×24 — jauh lebih lengkap dari Feather (~280, sudah tidak aktif
  dikembangkan) dan konsisten dibanding Tabler/Phosphor untuk kebutuhan aplikasi ini.
- Aktif dikembangkan (rilis tiap beberapa hari; versi yang diunduh: **v1.52.0**, 4 Okt 2026).
- Ada resmi di Figma Community (file + plugin official dari tim Lucide).

## Lisensi

**ISC License** — permissive, bebas dipakai untuk proyek pribadi maupun komersial
**tanpa atribusi wajib**. (Ikon turunan Feather di dalamnya berlisensi MIT.)

Sumber resmi: https://github.com/lucide-icons/lucide · https://lucide.dev/license

## Sumber Figma Community

- File: https://www.figma.com/community/file/939851755929765537/Lucide-Icons
- Plugin: https://www.figma.com/community/plugin/939567362549682242/Lucide-Icons

> SVG di folder ini diunduh dari paket resmi `lucide-static@1.52.0`
> (https://cdn.jsdelivr.net/npm/lucide-static@1.52.0/) — identik dengan ikon di Figma Community.

## Daftar ikon (`icons/lucide/`)

Semua stroke-based, viewBox 24×24, stroke-width 2, rounded caps.

| File | Nama Lucide | Dipakai untuk |
|------|-------------|---------------|
| `clock.svg` | clock | Jadwal (tab) |
| `calendar.svg` | calendar | Imsakiyah (tab) |
| `compass.svg` | compass | Kiblat (tab) |
| `grid.svg` | layout-grid | Lainnya (tab) |
| `search.svg` | search | Pencarian kota |
| `pin-lokasi.svg` | map-pin | Pin lokasi |
| `lonceng.svg` | bell | Notifikasi adzan |
| `volume.svg` | volume-2 | Volume pengingat |
| `bagikan.svg` | share-2 | Bagikan jadwal |
| `masukan.svg` | message-circle | Kirim masukan |
| `bulan-sabit.svg` | moon | Puasa sunnah / dzikir |
| `bintang.svg` | star | Favorit |
| `home.svg` | house | Beranda |
| `info.svg` | info | Tentang aplikasi |
| `gembok.svg` | lock | Kebijakan privasi |
| `buku-doa.svg` | book-open | Doa harian |
| `tetes-air.svg` | droplet | Panduan wudhu |
| `matahari.svg` | sun | Waktu sholat (ilustrasi) |

**Total: 18 ikon terunduh, semua valid.**

## Ikon yang TIDAK tersedia di Lucide

- **Tasbih** — tidak ada di Lucide (maupun Feather/Tabler/Phosphor).
  Rekomendasi: digambar manual sebagai SVG dengan gaya stroke yang sama
  (lingkaran manik + tali, stroke 2px, rounded) agar konsisten dengan set ini.

## Catatan untuk build Android

- SVG Lucide memakai `stroke="currentColor"` — warna diatur dari kode (cocok untuk
  tema pink-biru: `#6A82FB` / `#FF6F91`).
- Untuk VectorDrawable Android, konversi via Android Studio (Vector Asset) atau
  simpan sebagai SVG di WebView apa adanya.
