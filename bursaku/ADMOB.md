# Panduan AdMob — BursaKu

Aplikasi BursaKu sudah terintegrasi Google AdMob (play-services-ads 23.0.0,
build manual tanpa Gradle). Saat ini memakai **ID TEST resmi Google** —
iklan yang tampil berlabel "Test Ad" dan aman diklik sendiri.

## Yang perlu diganti ke ID asli (satu file saja + manifest)

1. Buka [console AdMob](https://apps.admob.com) → **Apps → Add app**
   (platform Android, nama "BursaKu") → dapatkan **App ID**
   (format `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`).
2. Di aplikasi tersebut buat 2 **Ad unit**:
   - **Banner** → dapatkan Banner Ad Unit ID
   - **Interstitial** → dapatkan Interstitial Ad Unit ID
   (format `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`).
3. Edit `src/java/com/flexurio/bursaku/AdsConfig.java`, ganti:
   - `APP_ID` → App ID asli
   - `BANNER_AD_UNIT_ID` → Banner Ad Unit ID asli
   - `INTERSTITIAL_AD_UNIT_ID` → Interstitial Ad Unit ID asli
4. Edit `src/AndroidManifest.xml`, ganti nilai meta-data
   `com.google.android.gms.ads.APPLICATION_ID` dengan App ID yang sama.
5. Build ulang: `bash ~/workspace/bursaku/build.sh`

`TEST_MODE` di AdsConfig otomatis menjadi false bila App ID bukan ID test.

## Penempatan iklan

- **Banner**: bawah layar Pasar, Ranking, Watchlist, Berita
  (di atas bottom nav). Bila iklan gagal load (offline/diblokir),
  ruangnya otomatis disembunyikan — aplikasi tetap normal.
- **Interstitial**: maksimal 1x per 5 menit, hanya saat membuka
  detail emiten (tidak saat app start). Preload di background;
  bila gagal, dilewati diam-diam.

## Catatan kebijakan

- BursaKu BUKAN aplikasi anak → tidak memakai flag child-directed.
- Untuk publikasi Play Store, isi form **Data Safety**:
  Advertising ID dikumpulkan untuk periklanan (lihat store-listing.md).
- Jangan klik iklan produksi sendiri berulang-ulang —
  akun AdMob bisa ditangguhkan. Selama ID masih test, aman.
