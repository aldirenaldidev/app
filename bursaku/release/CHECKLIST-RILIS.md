# CHECKLIST RILIS — BursaKu ke Google Play Store

File rilis siap di `~/workspace/bursaku/release/`:
- `bursaku-release.aab` — App Bundle release (versionCode 2, versionName 1.1,
  targetSdk 36, ter-sign, tervalidasi bundletool)
- `icon-512.png` — ikon Play listing 512×512
- `feature-graphic.png` — feature graphic 1024×500
- `privacy-policy.html` — draf kebijakan privasi (ID + EN)
- `store-listing.md` — teks listing + panduan Data Safety
- `../ADMOB.md` — panduan ganti ID AdMob

## Yang HANYA bisa dilakukan Tuan Muda (tidak bisa diwakilkan)

- [ ] **1. Akun Play Developer** — daftar di play.google.com/console dengan
  **Gmail pribadi** (sekali bayar $25). Wajib akun baru untuk publikasi.
- [ ] **2. Ganti AdMob ID** — ikuti `ADMOB.md`: buat App + 2 Ad Unit di
  apps.admob.com, ganti 3 konstanta di `AdsConfig.java` + meta-data
  `APPLICATION_ID` di manifest, lalu build ulang (`build.sh` +
  `build-aab.sh`). JANGAN upload dengan ID test ke produksi.
- [ ] **3. Host privacy policy** — upload `privacy-policy.html` ke URL publik
  milik sendiri (mis. GitHub Pages / hosting pribadi). Tempel URL-nya di
  Play Console (wajib).
- [ ] **4. Upload AAB** — Play Console → aplikasi baru → Production/Testing →
  upload `bursaku-release.aab`. Tambahkan: `icon-512.png`,
  `feature-graphic.png`, minimal 2 screenshot HP (ada di `screenshots/`).
- [ ] **5. Isi Data Safety** — panduan jawaban ada di `store-listing.md`
  (Advertising ID untuk iklan; tidak ada data sensitif lain).
- [ ] **6. Rating konten** — isi kuesioner rating di Play Console
  (perkiraan: Everyone).
- [ ] **7. BACKUP KEYSTORE** — salin `release/bursaku-release.keystore`
  + `release/keystore-password.txt` ke tempat AMAN di luar HP/laptop
  (mis. USB + cloud terenkripsi). **Hilang = tidak bisa update aplikasi
  selamanya.** Ini yang paling penting.

## Catatan teknis

- targetSdk 36 (Android 16) — memenuhi syarat Play per 31 Agu 2026
  (aplikasi baru wajib API 36+). minSdk 24.
- AAB tervalidasi: `bundletool validate` OK, `build-apks --mode=universal` OK.
- Untuk update berikutnya: naikkan versionCode di `build.sh`
  (`--version-code`), build ulang APK + AAB dengan keystore yang SAMA.
