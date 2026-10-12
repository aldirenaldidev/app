# 📱 Kumpulan Aplikasi Flexurio

Source code aplikasi Android & web dashboard karya **aldire__**.

## Daftar Aplikasi

| Aplikasi | Deskripsi | Folder |
|----------|-----------|--------|
| **ShiftKu** | Manajemen jadwal shift tim (Firebase, absensi selfie, laporan) | `shiftku/` |
| **DompetKu** | Pencatat keuangan + AI asisten offline | `dompetku/` |
| **Cek BSU** | Panduan BSU & cek desil (nonresmi) | `cekbsu/` |
| **SholatKu** | Jadwal sholat, dzikir, doa | `sholatku/` |
| **BursaKu** | Saham Indonesia ala RTI | `bursaku/` |
| **TVKu** | Streaming TV Indonesia & internasional | `tvku/` |
| **BolaKu** | Info sepak bola: skor, klasemen, transfer | `bolaku/` |
| **KartuKu** | Pembaca e-money via NFC | `kartuku/` |
| **DoaKu** | Kumpulan doa | `doaku/` |
| **ArisanKu** | Manajemen arisan lengkap | `arisanku/` |
| **E-Payslip** | Slip gaji digital + email otomatis | `epayslip/` |
| **ShiftKu Dashboard** | Web dashboard HR untuk komputer (premium) | `shiftku-dashboard/` |
| **Belajar Ceria** | Aplikasi belajar anak usia 5 tahun | `belajar-ceria/` |

## Build

Aplikasi Android di-build manual tanpa Gradle (aapt2 + javac + d8):

```bash
cd <nama-aplikasi>
./build.sh
```

## Catatan

- File `.apk`/`.aab`, keystore, dan kredensial **tidak** di-upload ke repo ini.
- Dependensi AAR di-download otomatis saat build (lihat `aar/resolve_all.py`).
- AdMob memakai test IDs — ganti dengan ID asli sebelum rilis.

© aldire__ — Instagram [@aldire__](https://instagram.com/aldire__)
