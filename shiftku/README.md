# ShiftKu

Aplikasi manajemen jadwal shift tim dengan Firebase.

**Package:** `com.flexurio.shiftku` | **Versi:** 1.2.1

## Fitur
- Kalender shift tim dengan popup tukar shift & ajukan cuti
- Login Firebase Auth dengan 3 level: User, Leader, Admin
- Admin default + wajib ganti password saat login pertama
- Kelola Pengguna (khusus Admin): tambah/ubah role/reset password/hapus
- Absensi dengan selfie wajib + lokasi (nama daerah) + check in/out terkunci
- Pola Timesheet: Check In / Tambah Aktivitas / Check Out + feed aktivitas
- Tukar shift (pilih anggota dari dropdown + alasan wajib)
- Cuti, Izin/Sakit (dengan foto surat dokter), Lembur
- Pengumuman, Pengingat shift (notifikasi), Laporan kehadiran (export Excel/PDF rapi)
- Export jadwal Excel/PDF, Widget home screen

## Build
Build manual tanpa Gradle (aapt2 + javac + d8):
```bash
./build.sh
```
Lihat `AKTIFKAN_EMAIL_PASSWORD.md` untuk setup Firebase Auth.

© aldire__
