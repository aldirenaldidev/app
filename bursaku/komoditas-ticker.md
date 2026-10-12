# Ticker Komoditas BursaKu (Yahoo Finance)

Diverifikasi 2026-10-10 via `https://query1.finance.yahoo.com/v8/finance/chart/<TICKER>`.
Harga contoh per 10 Okt 2026 (untuk referensi mockup; harga live diambil API saat runtime).

## VALID — dipakai di aplikasi

| Nama Indonesia | Ticker | Nama instrumen (Yahoo) | Satuan | Contoh harga |
|---|---|---|---|---|
| USD/IDR | USDIDR=X | US Dollar/Indonesian Rupiah | Rp | 16.420 |
| Emas | GC=F | Gold Futures (COMEX) | US$ | 4.216,30 |
| Perak | SI=F | Silver Futures (COMEX) | US$ | 61,05 |
| Brent | BZ=F | Brent Crude Oil Futures (ICE) | US$ | 104,72 |
| WTI | CL=F | WTI Crude Oil Futures (NYMEX) | US$ | 91,85 |
| Tembaga | HG=F | Copper Futures (COMEX) | US$ | 6,69 |
| Batubara | MTF=F | Coal (API2) CIF ARA Futures | US$ | 104,75 |
| Nikel | NICK.L | WisdomTree Nickel ETC (London) | US$ | 13,49 |
| Timah | TINM.L | WisdomTree Tin ETC (London) | US$ | 121,87 |

Catatan:
- Nikel & Timah memakai ETC London (bukan futures LME) karena Yahoo tidak menyediakan kontrak LME Nikel/Timah.
- Batubara memakai API2 (Rotterdam) via MTF=F.

## TIDAK VALID di Yahoo Finance

| Komoditas | Ticker dicoba | Hasil |
|---|---|---|
| Nikel | NI=F, JJN | tidak ada data (JJN delisted) |
| Timah | SN=F, JJT | tidak ada data (JJT delisted) |
| Minyak Sawit (CPO) | FCPO=F | tidak ada data — Yahoo tidak menyediakan CPO BMD |

CPO/Minyak Sawit: tidak tersedia di Yahoo Finance → TIDAK ditampilkan di aplikasi (jangan janjikan di UI).
