/* Wirid setelah sholat — embedded (fetch diblokir di WebView file://). */
"use strict";
const WIRID_DATA = [
 {
  "ar": "أَسْتَغْفِرُ اللهَ",
  "lt": "Astaghfirullah (dibaca 3 kali)",
  "t": "Istighfar (3x)",
  "tr": "Aku memohon ampun kepada Allah."
 },
 {
  "ar": "اللَّهُمَّ أَنْتَ السَّلاَمُ وَمِنْكَ السَّلاَمُ، تَبَارَكْتَ يَا ذَا الْجَلاَلِ وَالإِكْرَامِ",
  "lt": "Allahumma antas-salam wa minkas-salam, tabarakta ya dzal jalali wal ikram",
  "t": "Doa Setelah Salam",
  "tr": "Ya Allah, Engkau Maha Pemberi keselamatan, dan dari-Mu keselamatan. Maha Suci Engkau, wahai Dzat yang memiliki keagungan dan kemuliaan."
 },
 {
  "ar": "سُبْحَانَ اللهِ",
  "lt": "Subhanallah (dibaca 33 kali)",
  "t": "Tasbih (33x)",
  "tr": "Maha Suci Allah."
 },
 {
  "ar": "الْحَمْدُ لِلَّهِ",
  "lt": "Alhamdulillah (dibaca 33 kali)",
  "t": "Tahmid (33x)",
  "tr": "Segala puji bagi Allah."
 },
 {
  "ar": "اللهُ أَكْبَرُ",
  "lt": "Allahu Akbar (dibaca 33 kali)",
  "t": "Takbir (33x)",
  "tr": "Allah Maha Besar."
 },
 {
  "ar": "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
  "lt": "La ilaha illallah wahdahu la syarika lah, lahul mulku wa lahul hamdu wa huwa 'ala kulli syai'in qadir",
  "t": "Tahlil",
  "tr": "Tiada Tuhan selain Allah semata, tiada sekutu bagi-Nya. Bagi-Nya kerajaan dan pujian, dan Dia Maha Kuasa atas segala sesuatu."
 },
 {
  "ar": "اللهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ، لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ، لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ، مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ، يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ، وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ، وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ، وَلَا يَئُودُهُ حِفْظُهُمَا، وَهُوَ الْعَلِيُّ الْعَظِيمُ",
  "lt": "Allahu la ilaha illa huwal hayyul qayyum... (Al-Baqarah: 255)",
  "t": "Ayat Kursi (Al-Baqarah: 255)",
  "tr": "Allah, tiada Tuhan selain Dia, Yang Maha Hidup lagi Maha Berdiri Sendiri. Tidak mengantuk dan tidak tidur. Milik-Nya apa yang di langit dan di bumi... dan Dia Maha Tinggi lagi Maha Agung."
 },
 {
  "ar": "قُلْ هُوَ اللهُ أَحَدٌ، اللهُ الصَّمَدُ، لَمْ يَلِدْ وَلَمْ يُولَدْ، وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
  "lt": "Qul huwallahu ahad... (Al-Ikhlas: 1-4)",
  "t": "Al-Ikhlas",
  "tr": "Katakanlah: Dialah Allah Yang Maha Esa. Allah tempat bergantung. Dia tidak beranak dan tidak diperanakkan. Dan tidak ada sesuatu pun yang setara dengan-Nya."
 },
 {
  "ar": "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ، مِنْ شَرِّ مَا خَلَقَ، وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ، وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ، وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
  "lt": "Qul a'udzu birabbil falaq... (Al-Falaq: 1-5)",
  "t": "Al-Falaq",
  "tr": "Katakanlah: Aku berlindung kepada Tuhan yang menguasai subuh, dari kejahatan makhluk-Nya, dari kejahatan malam apabila telah gelap gulita, dari kejahatan wanita-wanita tukang sihir, dan dari kejahatan pendengki apabila ia dengki."
 },
 {
  "ar": "قُلْ أَعُوذُ بِرَبِّ النَّاسِ، مَلِكِ النَّاسِ، إِلَهِ النَّاسِ، مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ، الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ، مِنَ الْجِنَّةِ وَالنَّاسِ",
  "lt": "Qul a'udzu birabbin nas... (An-Nas: 1-6)",
  "t": "An-Nas",
  "tr": "Katakanlah: Aku berlindung kepada Tuhan manusia, Raja manusia, sesembahan manusia, dari kejahatan bisikan setan yang bersembunyi, yang membisikkan ke dalam dada manusia, dari golongan jin dan manusia."
 },
 {
  "t": "Doa Selamat",
  "ar": "اللَّهُمَّ إِنَّا نَسْأَلُكَ سَلَامَةً فِي الدِّينِ وَعَافِيَةً فِي الْجَسَدِ وَزِيَادَةً فِي الْعِلْمِ وَبَرَكَةً فِي الرِّزْقِ وَتَوْبَةً قَبْلَ الْمَوْتِ وَرَحْمَةً عِنْدَ الْمَوْتِ وَمَغْفِرَةً بَعْدَ الْمَوْتِ، اللَّهُمَّ هَوِّنْ عَلَيْنَا فِي سَكَرَاتِ الْمَوْتِ وَالنَّجَاةَ مِنَ النَّارِ وَالْعَفْوَ عِنْدَ الْحِسَابِ",
  "lt": "Allahumma inna nasaluka salaamatan fiddiin, wa 'aafiyatan fil jasad, wa ziyaadatan fil 'ilmi, wa barakatan fir rizqi, wa taubatan qablal maut, wa rahmatan 'indal maut, wa maghfiratan ba'dal maut. Allahumma hawwin 'alainaa fii sakaraatil maut, wan najaata minan naar, wal 'afwa 'indal hisaab.",
  "tr": "Ya Allah, kami memohon kepada-Mu keselamatan dalam agama, kesehatan dalam badan, tambahan ilmu, keberkahan rezeki, taubat sebelum mati, rahmat saat mati, dan ampunan setelah mati. Ya Allah, mudahkanlah kami dalam sakaratul maut, selamatkan dari api neraka, dan ampunilah saat hisab."
 },
 {
  "ar": "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
  "lt": "Rabbana atina fid-dunya hasanah, wa fil-akhirati hasanah, wa qina 'adzaban-nar",
  "t": "Doa Sapu Jagad",
  "tr": "Ya Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat, dan lindungilah kami dari azab neraka."
 }
];
