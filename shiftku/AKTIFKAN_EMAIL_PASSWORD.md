# Aktifkan Login Email/Password di Firebase Console

Aplikasi **ShiftKu** menampilkan error:

> `Gagal login: ... [ CONFIGURATION_NOT_FOUND ]`

Artinya: **metode login Email/Password belum diaktifkan** di Firebase Console
untuk project `shiftku-bc2e0`. Ikuti langkah berikut (cukup sekali saja):

## Langkah-langkah

1. Buka https://console.firebase.google.com
2. Login dengan akun Google pemilik project
3. Pilih project **shiftku-bc2e0**
4. Di menu kiri, klik **Build → Authentication**
5. Klik tab **Sign-in method**
6. Pada daftar "Additional providers", klik **Email/Password**
7. Geser toggle **Enable** menjadi ON (biru)
8. Klik **Save**

## Verifikasi

1. Tutup aplikasi ShiftKu sepenuhnya di HP, lalu buka ulang
2. Saat splash, akun admin default dibuat otomatis:
   - Email: `admin@shiftku.app`
   - Password awal: `ShiftKu2026!`
3. Login dengan kredensial di atas
4. Aplikasi langsung meminta **ganti password** (wajib, tidak bisa di-skip)

## Catatan

- Jika login masih gagal setelah mengaktifkan, tunggu ±5 menit
  (perubahan konfigurasi Firebase kadang butuh propagasi singkat),
  lalu coba lagi.
- Jangan menonaktifkan Email/Password selama aplikasi dipakai —
  semua akun (admin, leader, user) memakai metode ini.
- Password admin default ada di file `KREDENSIAL_ADMIN.txt`.
  Segera ganti setelah login pertama.
