# BursaKu — Panduan

Aplikasi info pasar saham Indonesia ala RTI Business (data: Yahoo Finance).

## Instalasi
1. Salin `bursaku.apk` ke HP Android (min. Android 7.0) lalu instal.
2. Izinkan akses internet & notifikasi saat diminta (notifikasi untuk Alert Harga).

## Fitur
- **Pasar**: IHSG & LQ45 (grafik mini), status bursa BUKA/TUTUP (WIB),
  USD/IDR, Emas, Minyak Brent.
- **Ranking**: pindai 45 saham LQ45 → Top Gainer / Top Loser / Teraktif.
- **Watchlist**: tambah kode emiten (mis. BBCA), tap untuk detail.
- **Detail emiten**: grafik candlestick + pemilih indikator (MA, EMA, Bollinger,
  Ichimoku, SAR, RSI, MACD, Stochastic, ATR, CCI, Williams %R, OBV, ADX),
  info emiten, komposisi Pemegang Saham (agregat), dan data Keuangan
  (pendapatan, laba bersih, EPS, ROE/ROA, riwayat tahunan).
- **Alert Harga**: pasang target naik/turun per emiten → notifikasi otomatis
  tiap 15 menit saat jam bursa. Alert nonaktif otomatis setelah tersentuh.
- **Berita**: agregator RSS CNBC Indonesia & IDX Channel.
- **Portofolio**: catat transaksi beli/jual (lot & harga) → pantau
  harga rata-rata, nilai pasar, untung/rugi.
- **Tentang**: © 2026 aldire__ + tautan sosial developer.

## Catatan
- Data delay ±15 menit; bukan rekomendasi investasi.
- P/E & market cap tidak tersedia (API Yahoo membatasi) → tampil "—".
- Pindai ranking 45 emiten memakan ±1 menit (rate limit Yahoo).
- Backup keystore: `release/bursaku-release.keystore` (wajib untuk update berikutnya).

## Build ulang
`./build.sh` (butuh ~/android-sdk, JDK, /tmp/bt35) → `bursaku.apk`.
