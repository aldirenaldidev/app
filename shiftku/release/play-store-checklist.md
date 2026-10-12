# Checklist Rilis Play Store — ShiftKu

> Package: `com.flexurio.shiftku` · v1.2.1 (versionCode 13)
> Tanggal disiapkan: 12 Oktober 2026

---

## ✅ SUDAH SIAP

| Item | Status | File |
|------|--------|------|
| APK release-signed | ✅ Valid | `~/workspace/shiftku/shiftku.apk` |
| **AAB release-signed** | ✅ **Valid (8,1 MB)** | **`~/workspace/shiftku/release/shiftku-release.aab`** |
| apksigner verify (APK) | ✅ VALID (V3.0) | — |
| jarsigner verify (AAB) | ✅ jar verified | — |
| bundletool validate | ✅ Valid | — |
| Privacy Policy (ID + EN) | ✅ Selesai | `release/privacy-policy.html` |
| Store Listing | ✅ Selesai | `release/store-listing.md` |
| App Icon 512×512 | ✅ Selesai | `release/graphics/icon_512x512.png` |
| Feature Graphic 1024×500 | ✅ Selesai | `release/graphics/feature_graphic_1024x500.png` |
| Keystore | ✅ Ada + backup | `release/shiftku-release.keystore` (+ `.bak`) |
| Password keystore | ✅ Tersimpan | `release/keystore-password.txt` |
| Target SDK 36 | ✅ | minSdk 24 |

## ⚠️ PERLU PERHATIAN SEBELUM RILIS

| Item | Status | Keterangan |
|------|--------|------------|
| **AdMob IDs** | 🔴 TEST IDs | Wajib diganti dengan ID asli dari AdMob Console sebelum rilis produksi. Saat ini masih pakai test IDs. |
| **Privacy Policy URL** | 🔴 Belum hosting | File HTML sudah jadi, perlu di-upload ke hosting (GitHub Pages / Firebase Hosting) untuk dapat URL publik. |
| **Screenshot HP asli** | 🟡 Mockup saja | Screenshot yang ada adalah render mockup. Disarankan ambil screenshot asli dari HP untuk listing yang lebih meyakinkan. |
| **Content Rating** | ⬜ Belum diisi | Isi kuesioner rating di Play Console (perkiraan: Everyone). |
| **Target Audience** | ⬜ Belum diset | Set 18+ di Play Console (bukan aplikasi anak). |
| **Data Safety Form** | ⬜ Belum diisi | Wajib diisi di Play Console: jenis data yang dikumpulkan (lihat privacy policy). |
| **Closed Testing** | ⬜ Belum mulai | Akun personal baru wajib closed test 14 hari dengan min. 12 tester. |
| **AAB (bukan APK)** | ✅ **Sudah build** | `release/shiftku-release.aab` (8,1 MB, signed, validated). Script: `build_aab.sh` |

## 📱 SCREENSHOT YANG TERSEDIA

Sumber: `~/workspace/shiftku/screenshots/`

| File | Fitur | Ukuran |
|------|-------|--------|
| `calendar.png` | Kalender Shift | 400×800 |
| `absensi_v12_locked.png` | Absensi (terkunci) | 780×1688 |
| `absensi_v12_done.png` | Absensi (selesai) | 780×1688 |
| `laporan_v12.png` | Laporan Kehadiran | 780×1688 |
| `kelola_pengguna.png` | Kelola Pengguna | 390×844 |
| `pengumuman_v12.png` | Pengumuman | 780×1688 |
| `lembur_v12.png` | Lembur | 780×1688 |
| `izin_v12.png` | Izin/Sakit | 780×1688 |
| `ganti_password.png` | Ganti Password | — |
| `login_roles.png` | Login | — |

> **Catatan:** Play Store butuh min. 2 screenshot HP (maks 8). Ukuran bebas 320–3840px. Screenshot di atas bisa dipakai, tetapi disarankan ambil ulang dari HP asli dengan resolusi konsisten (mis. 1080×2400).

## 📐 SPESIFIKASI GRAFIS PLAY STORE

| Aset | Ukuran Wajib | Status |
|------|--------------|--------|
| App icon | 512×512 PNG | ✅ `release/graphics/icon_512x512.png` |
| Feature graphic | 1024×500 JPG/PNG | ✅ `release/graphics/feature_graphic_1024x500.png` |
| Phone screenshots | 320–3840px (min 2) | 🟡 Ada (mockup, disarankan ambil ulang) |
| 7-inch tablet | opsional | ⬜ — |
| 10-inch tablet | opsional | ⬜ — |

## 🔐 KEYSTORE — PENTING!

- **File:** `~/workspace/shiftku/release/shiftku-release.keystore`
- **Backup:** `~/workspace/shiftku/release/shiftku-release.keystore.bak`
- **Password:** `~/workspace/shiftku/release/keystore-password.txt`
- ⚠️ **User WAJIB mem-backup keystore + password di tempat aman.** Jika hilang, aplikasi tidak bisa di-update di Play Store selamanya.

## 📋 LANGKAH SELANJUTNYA (di Play Console)

1. Buat aplikasi baru → isi nama, bahasa (Indonesia), kategori (Bisnis)
2. Upload AAB: `release/shiftku-release.aab` ✅ (sudah siap)
3. Isi Store Listing (copy dari `store-listing.md`)
4. Upload grafis (icon, feature graphic, screenshots)
5. Isi Content Rating, Target Audience, Data Safety
6. Masukkan URL Privacy Policy (setelah di-hosting)
7. Ganti AdMob test IDs dengan yang asli (di code, rebuild AAB)
8. Setup Closed Testing → undang 12+ tester → tunggu 14 hari
9. Ajukan untuk review produksi

---
© 2026 aldire__
