package com.flexurio.dompetku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-kategorisasi transaksi 100% offline dari teks deskripsi Bahasa Indonesia.
 * Top-level class agar aman untuk d8. Semua method defensif.
 *
 * Cara pakai: SmartCategorizer.categorize(db, "beli kopi 15000")
 *   -> {tipe:"out", kategori:"Makan", confidence:87, src:"keyword", amount:15000}
 */
public final class SmartCategorizer {
    private SmartCategorizer() {}

    public static class Result {
        public String tipe = "out";
        public String kategori = "Lainnya";
        public int confidence = 40;
        public String src = "default"; // keyword | learned | trigger | default
        public long amount = 0;
    }

    // {keyword, tipe, kategori, bobot}
    private static final String[][] KW = {
        // ===== PEMASUKAN =====
        {"gajian","in","Gaji","5"},{"gaji","in","Gaji","5"},{"salary","in","Gaji","4"},
        {"upah","in","Gaji","4"},{"honor","in","Gaji","4"},{"honorarium","in","Gaji","4"},
        {"bonus","in","Gaji","4"},{"thr","in","Gaji","4"},{"insentif","in","Gaji","4"},
        {"tantiem","in","Gaji","4"},{"tunjangan","in","Gaji","3"},
        {"freelance","in","Gaji","4"},{"projek","in","Gaji","3"},{"proyek","in","Gaji","3"},
        {"fee","in","Gaji","3"},{"komisi","in","Gaji","4"},{"jasa","in","Gaji","2"},
        {"endorse","in","Gaji","3"},{"dagang","in","Gaji","3"},{"jualan","in","Gaji","3"},
        {"untung","in","Gaji","3"},{"laba","in","Gaji","3"},
        {"hadiah","in","Lainnya","3"},{"kiriman","in","Lainnya","3"},
        {"transfer masuk","in","Lainnya","4"},{"refund","in","Lainnya","3"},
        {"cashback","in","Lainnya","3"},{"cash back","in","Lainnya","3"},
        // ===== MAKAN / MINUM =====
        {"makan siang","out","Makan","5"},{"makan malam","out","Makan","5"},
        {"sarapan","out","Makan","4"},{"makan","out","Makan","4"},{"nasi","out","Makan","3"},
        {"kopi","out","Makan","4"},{"cafe","out","Makan","3"},{"kafe","out","Makan","3"},
        {"warteg","out","Makan","4"},{"warung","out","Makan","3"},
        {"restoran","out","Makan","3"},{"resto","out","Makan","3"},
        {"jajan","out","Makan","4"},{"snack","out","Makan","3"},{"cemilan","out","Makan","3"},
        {"mie","out","Makan","3"},{"ayam","out","Makan","2"},{"bakso","out","Makan","4"},
        {"soto","out","Makan","4"},{"rendang","out","Makan","3"},{"sate","out","Makan","4"},
        {"martabak","out","Makan","4"},{"gorengan","out","Makan","3"},
        {"roti","out","Makan","3"},{"kue","out","Makan","3"},
        {"minuman","out","Makan","3"},{"jus","out","Makan","3"},{"teh","out","Makan","2"},
        {"boba","out","Makan","4"},{"es teh","out","Makan","3"},
        {"starbucks","out","Makan","4"},{"janji jiwa","out","Makan","4"},
        {"kopi kenangan","out","Makan","4"},{"fore","out","Makan","3"},
        {"mcd","out","Makan","4"},{"mcdonald","out","Makan","4"},{"kfc","out","Makan","4"},
        {"pizza","out","Makan","3"},{"burger","out","Makan","3"},
        {"gofood","out","Makan","4"},{"grabfood","out","Makan","4"},{"shopeefood","out","Makan","4"},
        {"gudeg","out","Makan","3"},{"pecel","out","Makan","3"},{"lalapan","out","Makan","3"},
        {"dimsum","out","Makan","3"},{"ramen","out","Makan","3"},{"sushi","out","Makan","3"},
        {"bento","out","Makan","3"},{"lunch","out","Makan","3"},{"dinner","out","Makan","3"},
        {"breakfast","out","Makan","3"},{"kuliner","out","Makan","3"},
        {"padang","out","Makan","3"},{"seafood","out","Makan","3"},{"daging","out","Makan","2"},
        {"sayur","out","Makan","2"},{"buah","out","Makan","2"},
        // ===== TRANSPORT =====
        {"ojek online","out","Transport","5"},{"ojek","out","Transport","5"},
        {"ojol","out","Transport","5"},{"gojek","out","Transport","4"},{"grab","out","Transport","4"},
        {"taksi","out","Transport","4"},{"taxi","out","Transport","4"},
        {"bensin","out","Transport","5"},{"bbm","out","Transport","4"},
        {"pertamax","out","Transport","4"},{"pertalite","out","Transport","4"},
        {"solar","out","Transport","3"},{"dexlite","out","Transport","3"},
        {"parkir","out","Transport","5"},{"tol","out","Transport","4"},
        {"bus","out","Transport","3"},{"transjakarta","out","Transport","4"},
        {"kereta","out","Transport","4"},{"krl","out","Transport","4"},
        {"mrt","out","Transport","4"},{"lrt","out","Transport","4"},
        {"angkot","out","Transport","4"},{"travel","out","Transport","3"},
        {"tiket pesawat","out","Transport","5"},{"tiket kereta","out","Transport","4"},
        {"pesawat","out","Transport","4"},
        {"servis motor","out","Transport","4"},{"servis mobil","out","Transport","4"},
        {"servis","out","Transport","3"},{"ganti oli","out","Transport","4"},
        {"cuci motor","out","Transport","3"},{"cuci mobil","out","Transport","3"},
        {"bengkel","out","Transport","3"},{"spbu","out","Transport","3"},
        {"go ride","out","Transport","4"},{"grab bike","out","Transport","4"},
        // ===== BELANJA =====
        {"belanja","out","Belanja","4"},{"baju","out","Belanja","4"},
        {"celana","out","Belanja","4"},{"kaos","out","Belanja","3"},{"kemeja","out","Belanja","3"},
        {"sepatu","out","Belanja","4"},{"sandal","out","Belanja","3"},
        {"tas","out","Belanja","3"},{"dompet","out","Belanja","3"},
        {"shopee","out","Belanja","4"},{"tokopedia","out","Belanja","4"},
        {"lazada","out","Belanja","4"},{"tiktok shop","out","Belanja","4"},
        {"blibli","out","Belanja","4"},{"mall","out","Belanja","3"},
        {"indomaret","out","Belanja","3"},{"alfamart","out","Belanja","3"},
        {"alfamidi","out","Belanja","3"},{"supermarket","out","Belanja","3"},
        {"minimarket","out","Belanja","3"},
        {"skincare","out","Belanja","4"},{"skin care","out","Belanja","4"},
        {"kosmetik","out","Belanja","4"},{"make up","out","Belanja","4"},
        {"parfum","out","Belanja","3"},
        {"hp","out","Belanja","3"},{"handphone","out","Belanja","3"},
        {"smartphone","out","Belanja","3"},{"laptop","out","Belanja","4"},
        {"komputer","out","Belanja","3"},{"elektronik","out","Belanja","3"},
        {"furniture","out","Belanja","3"},{"perabot","out","Belanja","3"},
        {"sofa","out","Belanja","3"},{"kasur","out","Belanja","3"},
        {"sprei","out","Belanja","2"},{"bantal","out","Belanja","2"},
        // ===== TAGIHAN =====
        {"token listrik","out","Tagihan","5"},{"listrik","out","Tagihan","5"},
        {"pln","out","Tagihan","4"},{"air","out","Tagihan","3"},{"pdam","out","Tagihan","4"},
        {"internet","out","Tagihan","4"},{"wifi","out","Tagihan","4"},
        {"indihome","out","Tagihan","4"},{"first media","out","Tagihan","4"},
        {"pulsa","out","Tagihan","5"},{"paket data","out","Tagihan","4"},
        {"kuota","out","Tagihan","4"},{"telkomsel","out","Tagihan","4"},
        {"simpati","out","Tagihan","4"},{"xl","out","Tagihan","3"},{"axis","out","Tagihan","3"},
        {"indosat","out","Tagihan","4"},{"tri","out","Tagihan","3"},{"smartfren","out","Tagihan","3"},
        {"bpjs","out","Tagihan","4"},{"iuran","out","Tagihan","3"},
        {"cicilan","out","Tagihan","5"},{"kredit","out","Tagihan","4"},
        {"kpr","out","Tagihan","4"},{"kos","out","Tagihan","4"},
        {"kontrakan","out","Tagihan","4"},{"sewa","out","Tagihan","3"},
        {"apartemen","out","Tagihan","3"},{"pajak","out","Tagihan","4"},
        {"pbb","out","Tagihan","4"},{"stnk","out","Tagihan","3"},
        {"asuransi","out","Tagihan","4"},{"premi","out","Tagihan","4"},
        {"langganan","out","Tagihan","3"},{"netflix","out","Tagihan","4"},
        {"spotify","out","Tagihan","4"},{"youtube premium","out","Tagihan","4"},
        {"disney","out","Tagihan","3"},{"viu","out","Tagihan","3"},
        {"google one","out","Tagihan","3"},{"icloud","out","Tagihan","3"},
        // ===== KESEHATAN =====
        {"obat","out","Kesehatan","5"},{"apotek","out","Kesehatan","4"},
        {"dokter","out","Kesehatan","5"},{"rumah sakit","out","Kesehatan","5"},
        {"klinik","out","Kesehatan","4"},{"puskesmas","out","Kesehatan","4"},
        {"vitamin","out","Kesehatan","4"},{"suplemen","out","Kesehatan","3"},
        {"masker","out","Kesehatan","3"},{"lab","out","Kesehatan","3"},
        {"check up","out","Kesehatan","4"},{"checkup","out","Kesehatan","4"},
        {"medical","out","Kesehatan","3"},{"gigi","out","Kesehatan","3"},
        {"bidan","out","Kesehatan","3"},
        // ===== HIBURAN =====
        {"tiket nonton","out","Hiburan","5"},{"nonton","out","Hiburan","4"},
        {"bioskop","out","Hiburan","4"},{"cinema","out","Hiburan","4"},
        {"xxi","out","Hiburan","4"},{"cgv","out","Hiburan","4"},
        {"top up","out","Hiburan","4"},{"topup","out","Hiburan","4"},
        {"diamond","out","Hiburan","4"},{"mobile legend","out","Hiburan","4"},
        {"free fire","out","Hiburan","4"},{"steam","out","Hiburan","3"},
        {"playstation","out","Hiburan","3"},{"nintendo","out","Hiburan","3"},
        {"karaoke","out","Hiburan","4"},{"ktv","out","Hiburan","3"},
        {"konser","out","Hiburan","4"},{"tiket konser","out","Hiburan","5"},
        {"liburan","out","Hiburan","4"},{"wisata","out","Hiburan","4"},
        {"healing","out","Hiburan","3"},{"hotel","out","Hiburan","4"},
        {"villa","out","Hiburan","4"},{"penginapan","out","Hiburan","3"},
        {"traveling","out","Hiburan","3"},{"travelling","out","Hiburan","3"},
        {"piknik","out","Hiburan","3"},{"futsal","out","Hiburan","3"},
        {"badminton","out","Hiburan","3"},{"gym","out","Hiburan","3"},
        {"fitness","out","Hiburan","3"},{"renang","out","Hiburan","3"},
        {"game","out","Hiburan","3"},
        // ===== PENDIDIKAN -> Lainnya? user hanya punya 6 kategori + baru =====
        {"spp","out","Lainnya","4"},{"ukt","out","Lainnya","4"},
        {"kursus","out","Lainnya","3"},{"les","out","Lainnya","3"},
        {"bimbel","out","Lainnya","3"},{"buku","out","Lainnya","3"},
        {"sekolah","out","Lainnya","3"},{"kuliah","out","Lainnya","3"},
        // ===== SOSIAL =====
        {"sedekah","out","Lainnya","4"},{"donasi","out","Lainnya","4"},
        {"zakat","out","Lainnya","4"},{"infak","out","Lainnya","4"},
        {"infaq","out","Lainnya","4"},{"amal","out","Lainnya","3"},
        {"wakaf","out","Lainnya","3"},{"qurban","out","Lainnya","3"},
        {"kurban","out","Lainnya","3"},{"arisan","out","Lainnya","3"},
        {"kondangan","out","Lainnya","4"},{"hajatan","out","Lainnya","3"},
        {"nikahan","out","Lainnya","3"},{"sumbangan","out","Lainnya","3"},
        // ===== TABUNGAN =====
        {"nabung","out","Tabungan","5"},{"menabung","out","Tabungan","5"},
        {"tabungan","out","Tabungan","4"},
        {"deposito","out","Tabungan","4"},{"investasi","out","Tabungan","4"},
        {"saham","out","Tabungan","4"},{"reksadana","out","Tabungan","4"},
        {"reksa dana","out","Tabungan","4"},{"emas","out","Tabungan","4"},
        {"antam","out","Tabungan","3"},{"obligasi","out","Tabungan","3"},
        {"sukuk","out","Tabungan","3"},{"crypto","out","Tabungan","3"},
        {"bitcoin","out","Tabungan","3"},
        {"dana darurat","out","Darurat","5"},{"dana cadangan","out","Darurat","4"},
        {"darurat","out","Darurat","3"},
    };

    // ===== Kamus tambahan: brand, slang, singkatan konteks, ratusan kata =====
    private static final String[][] KW2 = {
        // MAKAN: brand & jajanan
        {"ngopi","out","Makan","4"},{"nongkrong","out","Makan","3"},{"bukber","out","Makan","4"},
        {"sahur","out","Makan","3"},{"takjil","out","Makan","3"},{"buka puasa","out","Makan","4"},
        {"geprek","out","Makan","4"},{"ayam geprek","out","Makan","4"},
        {"pecel lele","out","Makan","4"},{"lele","out","Makan","3"},
        {"indomie","out","Makan","4"},{"mie ayam","out","Makan","3"},{"seblak","out","Makan","4"},
        {"cilok","out","Makan","3"},{"cimol","out","Makan","3"},{"batagor","out","Makan","4"},
        {"siomay","out","Makan","4"},{"pempek","out","Makan","4"},{"tekwan","out","Makan","3"},
        {"lontong","out","Makan","3"},{"opor","out","Makan","3"},{"gulai","out","Makan","3"},
        {"tongseng","out","Makan","3"},{"rawon","out","Makan","4"},{"coto","out","Makan","3"},
        {"serabi","out","Makan","3"},{"klepon","out","Makan","3"},{"onde-onde","out","Makan","3"},
        {"getuk","out","Makan","3"},{"lupis","out","Makan","3"},
        {"es cendol","out","Makan","3"},{"cendol","out","Makan","3"},{"es doger","out","Makan","3"},
        {"es teler","out","Makan","3"},{"sop buah","out","Makan","3"},
        {"rujak","out","Makan","3"},{"asinan","out","Makan","3"},{"gado-gado","out","Makan","4"},
        {"karedok","out","Makan","3"},{"lotek","out","Makan","3"},
        {"tahu tek","out","Makan","3"},{"soto ayam","out","Makan","3"},{"soto betawi","out","Makan","3"},
        {"bakmi","out","Makan","3"},{"kwetiau","out","Makan","3"},{"bihun","out","Makan","3"},
        {"steak","out","Makan","3"},{"shabu","out","Makan","3"},{"yakiniku","out","Makan","3"},
        {"all you can eat","out","Makan","4"},{"ayce","out","Makan","4"},{"buffet","out","Makan","3"},
        {"prasmanan","out","Makan","3"},{"catering","out","Makan","3"},
        {"rice bowl","out","Makan","3"},{"fried chicken","out","Makan","3"},
        {"richeese","out","Makan","3"},{"solaria","out","Makan","3"},{"hokben","out","Makan","3"},
        {"marugame","out","Makan","3"},{"yoshinoya","out","Makan","3"},{"sushi tei","out","Makan","3"},
        {"pepper lunch","out","Makan","3"},{"pancake","out","Makan","3"},{"waffle","out","Makan","3"},
        {"croffle","out","Makan","3"},{"roti bakar","out","Makan","3"},
        {"pisang goreng","out","Makan","3"},{"molen","out","Makan","3"},{"risoles","out","Makan","3"},
        {"pastel","out","Makan","3"},{"lemper","out","Makan","3"},{"arem-arem","out","Makan","3"},
        {"bubur ayam","out","Makan","3"},{"bubur","out","Makan","3"},
        {"combro","out","Makan","3"},{"misro","out","Makan","3"},{"bakwan","out","Makan","3"},
        {"perkedel","out","Makan","3"},{"mendoan","out","Makan","3"},
        {"ayam penyet","out","Makan","3"},{"penyet","out","Makan","3"},
        {"ikan bakar","out","Makan","3"},{"cumi","out","Makan","3"},{"udang","out","Makan","3"},
        {"kepiting","out","Makan","3"},{"bandeng","out","Makan","3"},{"gurame","out","Makan","3"},
        {"nila","out","Makan","3"},{"patin","out","Makan","3"},
        {"telur","out","Makan","2"},{"telor","out","Makan","2"},{"ceplok","out","Makan","3"},
        {"dadar","out","Makan","3"},{"abon","out","Makan","2"},{"dendeng","out","Makan","3"},
        {"empal","out","Makan","3"},{"semur","out","Makan","3"},
        {"sayur asem","out","Makan","3"},{"lodeh","out","Makan","3"},{"capcay","out","Makan","3"},
        {"fuyunghai","out","Makan","3"},{"koloke","out","Makan","3"},
        {"nasi goreng","out","Makan","4"},{"nasgor","out","Makan","4"},
        {"nasi uduk","out","Makan","4"},{"uduk","out","Makan","3"},
        {"nasi kuning","out","Makan","3"},{"nasi liwet","out","Makan","3"},
        {"nasi campur","out","Makan","3"},{"nasi timbel","out","Makan","3"},
        {"nasi box","out","Makan","3"},{"snack box","out","Makan","3"},{"tumpeng","out","Makan","4"},
        // MINUMAN
        {"air mineral","out","Makan","3"},{"aqua","out","Makan","3"},
        {"le minerale","out","Makan","3"},{"teh botol","out","Makan","3"},{"teh pucuk","out","Makan","3"},
        {"susu","out","Makan","3"},{"ultra milk","out","Makan","3"},{"milo","out","Makan","3"},
        {"bear brand","out","Makan","3"},{"yakult","out","Makan","3"},{"yogurt","out","Makan","3"},
        {"pocari","out","Makan","3"},{"pocari sweat","out","Makan","3"},
        {"you c1000","out","Makan","3"},{"kratingdaeng","out","Makan","3"},
        {"extra joss","out","Makan","3"},{"kopi tubruk","out","Makan","4"},{"tubruk","out","Makan","3"},
        {"espresso","out","Makan","3"},{"americano","out","Makan","3"},{"latte","out","Makan","3"},
        {"cappuccino","out","Makan","3"},{"mocha","out","Makan","3"},
        {"kopi susu","out","Makan","4"},{"es kopi","out","Makan","4"},{"cold brew","out","Makan","3"},
        {"matcha","out","Makan","3"},{"thai tea","out","Makan","4"},{"green tea","out","Makan","3"},
        {"taro","out","Makan","3"},{"chocolate","out","Makan","3"},{"ovomaltine","out","Makan","3"},
        {"jus alpukat","out","Makan","3"},{"jus mangga","out","Makan","3"},{"jus jeruk","out","Makan","3"},
        {"es kelapa","out","Makan","3"},{"degan","out","Makan","3"},{"dawet","out","Makan","3"},
        {"bajigur","out","Makan","3"},{"bandrek","out","Makan","3"},{"sekoteng","out","Makan","3"},
        {"ronde","out","Makan","3"},{"wedang jahe","out","Makan","3"},{"wedang","out","Makan","3"},
        {"beras kencur","out","Makan","3"},{"temulawak","out","Makan","3"},
        {"mixue","out","Makan","4"},{"chatime","out","Makan","4"},{"xiboba","out","Makan","4"},
        {"teguk","out","Makan","4"},{"tomoro","out","Makan","4"},{"point coffee","out","Makan","4"},
        {"excelso","out","Makan","3"},{"maxx coffee","out","Makan","3"},
        {"haus","out","Makan","4"},{"es teh indonesia","out","Makan","4"},{"teh solo","out","Makan","3"},
        {"kulo","out","Makan","3"},{"lawson","out","Makan","3"},{"family mart","out","Makan","3"},
        {"circle k","out","Makan","3"},
        // TRANSPORT tambahan
        {"bluebird","out","Transport","4"},{"silverbird","out","Transport","3"},
        {"indriver","out","Transport","4"},{"maxim","out","Transport","4"},
        {"grabcar","out","Transport","4"},{"gocar","out","Transport","4"},
        {"tiket","out","Transport","2"},{"traveloka","out","Transport","3"},
        {"kai","out","Transport","3"},{"commuter","out","Transport","3"},
        {"jaklingko","out","Transport","3"},{"transjakarta","out","Transport","3"},
        {"damri","out","Transport","3"},{"primajasa","out","Transport","3"},
        {"sinar jaya","out","Transport","3"},{"rosalia indah","out","Transport","3"},
        {"gunung harta","out","Transport","3"},{"garuda","out","Transport","3"},
        {"lion","out","Transport","3"},{"batik","out","Transport","3"},{"citilink","out","Transport","3"},
        {"airasia","out","Transport","3"},{"super air jet","out","Transport","3"},
        {"wings","out","Transport","3"},{"kapal","out","Transport","3"},{"feri","out","Transport","3"},
        {"pelni","out","Transport","3"},{"busway","out","Transport","3"},
        {"becak","out","Transport","3"},{"bajaj","out","Transport","3"},
        {"mikrolet","out","Transport","3"},{"kopaja","out","Transport","3"},{"metromini","out","Transport","3"},
        {"mrt jakarta","out","Transport","4"},{"lrt jabodebek","out","Transport","4"},
        {"commuter line","out","Transport","4"},{"prameks","out","Transport","3"},
        {"argo","out","Transport","3"},{"lodaya","out","Transport","3"},{"kahuripan","out","Transport","3"},
        {"pasundan","out","Transport","3"},{"matarmaja","out","Transport","3"},
        {"jne","out","Transport","3"},{"jnt","out","Transport","3"},{"sicepat","out","Transport","3"},
        {"anteraja","out","Transport","3"},{"paxel","out","Transport","3"},
        {"pos indonesia","out","Transport","3"},{"ninja","out","Transport","3"},
        // BELANJA tambahan
        {"uniqlo","out","Belanja","4"},{"h&m","out","Belanja","4"},{"zara","out","Belanja","4"},
        {"pull&bear","out","Belanja","3"},{"bershka","out","Belanja","3"},
        {"cotton on","out","Belanja","3"},{"giordano","out","Belanja","3"},
        {"levis","out","Belanja","3"},{"hoodie","out","Belanja","3"},{"sweater","out","Belanja","3"},
        {"jaket","out","Belanja","3"},{"blazer","out","Belanja","3"},
        {"topi","out","Belanja","3"},{"kacamata","out","Belanja","3"},
        {"jam tangan","out","Belanja","4"},{"casio","out","Belanja","3"},{"fossil","out","Belanja","3"},
        {"charles & keith","out","Belanja","3"},{"pedro","out","Belanja","3"},
        {"bata","out","Belanja","3"},{"converse","out","Belanja","4"},{"vans","out","Belanja","4"},
        {"nike","out","Belanja","4"},{"adidas","out","Belanja","4"},{"puma","out","Belanja","3"},
        {"new balance","out","Belanja","3"},{"asics","out","Belanja","3"},{"skechers","out","Belanja","3"},
        {"lottemart","out","Belanja","3"},{"lotte","out","Belanja","3"},
        {"hypermart","out","Belanja","3"},{"transmart","out","Belanja","3"},
        {"superindo","out","Belanja","3"},{"ranch market","out","Belanja","3"},
        {"foodhall","out","Belanja","3"},{"guardian","out","Belanja","3"},{"watsons","out","Belanja","3"},
        {"wardah","out","Belanja","4"},{"emina","out","Belanja","3"},{"pixy","out","Belanja","3"},
        {"parfum","out","Belanja","3"},{"make over","out","Belanja","3"},
        {"the body shop","out","Belanja","3"},{"innisfree","out","Belanja","3"},
        {"skintific","out","Belanja","4"},{"somethinc","out","Belanja","3"},
        {"whitelab","out","Belanja","3"},{"avoskin","out","Belanja","3"},
        {"ms glow","out","Belanja","3"},{"scarlett","out","Belanja","3"},
        {"tupperware","out","Belanja","3"},{"ikea","out","Belanja","4"},
        {"informa","out","Belanja","3"},{"ace hardware","out","Belanja","3"},
        {"mr diy","out","Belanja","3"},{"miniso","out","Belanja","4"},{"daiso","out","Belanja","3"},
        {"kkv","out","Belanja","3"},{"gramedia","out","Belanja","3"},
        {"ongkir","out","Belanja","3"},{"toko","out","Belanja","2"},
        // TAGIHAN tambahan
        {"ovo","out","Tagihan","3"},{"gopay","out","Tagihan","3"},{"dana","out","Tagihan","3"},
        {"linkaja","out","Tagihan","3"},{"shopeepay","out","Tagihan","3"},
        {"emoney","out","Tagihan","4"},{"e-money","out","Tagihan","4"},
        {"flazz","out","Tagihan","4"},{"brizzi","out","Tagihan","4"},{"tapcash","out","Tagihan","4"},
        {"top up ovo","out","Tagihan","3"},{"top up gopay","out","Tagihan","3"},
        {"admin bank","out","Tagihan","3"},{"biaya admin","out","Tagihan","3"},
        {"bi fast","out","Tagihan","3"},{"virtual account","out","Tagihan","3"},
        {"qris","out","Tagihan","3"},{"ipl","out","Tagihan","4"},
        {"iuran keamanan","out","Tagihan","3"},{"iuran sampah","out","Tagihan","3"},
        {"retribusi","out","Tagihan","3"},{"bpkb","out","Tagihan","3"},
        {"perpanjang sim","out","Tagihan","3"},{"sim","out","Tagihan","3"},
        {"paspor","out","Tagihan","3"},{"materai","out","Tagihan","3"},
        {"notaris","out","Tagihan","3"},{"balik nama","out","Tagihan","3"},
        // KESEHATAN tambahan
        {"apotik","out","Kesehatan","4"},{"kimia farma","out","Kesehatan","3"},
        {"k24","out","Kesehatan","3"},{"halodoc","out","Kesehatan","4"},
        {"alodokter","out","Kesehatan","4"},{"vaksin","out","Kesehatan","4"},
        {"vaksinasi","out","Kesehatan","4"},{"booster","out","Kesehatan","3"},
        {"swab","out","Kesehatan","4"},{"pcr","out","Kesehatan","4"},{"antigen","out","Kesehatan","4"},
        {"rontgen","out","Kesehatan","3"},{"usg","out","Kesehatan","3"},
        {"fisioterapi","out","Kesehatan","3"},{"akupunktur","out","Kesehatan","3"},
        {"bekam","out","Kesehatan","3"},{"pijat","out","Kesehatan","3"},{"refleksi","out","Kesehatan","3"},
        {"behel","out","Kesehatan","3"},{"scaling","out","Kesehatan","3"},
        {"tambal gigi","out","Kesehatan","4"},{"cabut gigi","out","Kesehatan","4"},
        {"softlens","out","Kesehatan","3"},{"tetes mata","out","Kesehatan","3"},
        {"counterpain","out","Kesehatan","3"},{"salonpas","out","Kesehatan","3"},
        {"koyo","out","Kesehatan","3"},{"minyak kayu putih","out","Kesehatan","3"},
        {"tolak angin","out","Kesehatan","4"},{"antangin","out","Kesehatan","4"},
        {"imboost","out","Kesehatan","3"},{"stimuno","out","Kesehatan","3"},
        {"sangobion","out","Kesehatan","3"},{"neurobion","out","Kesehatan","3"},
        {"diabetasol","out","Kesehatan","3"},{"ensure","out","Kesehatan","3"},
        // HIBURAN tambahan
        {"prime video","out","Hiburan","3"},{"apple tv","out","Hiburan","3"},
        {"wetv","out","Hiburan","3"},{"vidio","out","Hiburan","3"},{"mola","out","Hiburan","3"},
        {"timezone","out","Hiburan","3"},{"dufan","out","Hiburan","4"},{"ancol","out","Hiburan","4"},
        {"taman mini","out","Hiburan","3"},{"ragunan","out","Hiburan","3"},
        {"kebun binatang","out","Hiburan","3"},{"sea world","out","Hiburan","3"},
        {"waterboom","out","Hiburan","3"},{"jungleland","out","Hiburan","3"},
        {"trans studio","out","Hiburan","3"},{"jatim park","out","Hiburan","3"},
        {"museum","out","Hiburan","3"},{"nobar","out","Hiburan","3"},
        {"basket","out","Hiburan","3"},{"voli","out","Hiburan","3"},{"tenis","out","Hiburan","3"},
        {"golf","out","Hiburan","3"},{"bowling","out","Hiburan","3"},
        {"billiard","out","Hiburan","3"},{"biliar","out","Hiburan","3"},
        {"hiking","out","Hiburan","3"},{"camping","out","Hiburan","3"},
        {"mendaki","out","Hiburan","3"},{"gunung","out","Hiburan","3"},{"pantai","out","Hiburan","3"},
        {"snorkeling","out","Hiburan","3"},{"diving","out","Hiburan","3"},{"surfing","out","Hiburan","3"},
        {"jetski","out","Hiburan","3"},{"rafting","out","Hiburan","3"},{"arung jeram","out","Hiburan","4"},
        {"paralayang","out","Hiburan","3"},{"go kart","out","Hiburan","3"},{"gokart","out","Hiburan","3"},
        {"atv","out","Hiburan","3"},{"paintball","out","Hiburan","3"},{"berkuda","out","Hiburan","3"},
        {"mabar","out","Hiburan","4"},{"pub","out","Hiburan","3"},{"dugem","out","Hiburan","3"},
        {"live music","out","Hiburan","3"},{"festival","out","Hiburan","3"},
        // GAJI tambahan
        {"uang lembur","in","Gaji","4"},{"lembur","in","Gaji","3"},
        {"uang makan","in","Gaji","3"},{"tunjangan hari raya","in","Gaji","4"},
        {"pensiun","in","Gaji","3"},{"pesangon","in","Gaji","4"},
        {"gaji ke 13","in","Gaji","4"},{"bonus tahunan","in","Gaji","4"},
        {"part time","in","Gaji","3"},{"magang","in","Gaji","3"},{"internship","in","Gaji","3"},
        {"side job","in","Gaji","4"},{"side hustle","in","Gaji","3"},
        {"dropship","in","Gaji","3"},{"reseller","in","Gaji","3"},
        {"affiliate","in","Gaji","4"},{"shopee affiliate","in","Gaji","4"},
        {"tiktok affiliate","in","Gaji","4"},{"paid promote","in","Gaji","3"},
        {"monetisasi","in","Gaji","3"},{"adsense","in","Gaji","4"},
        {"royalti","in","Gaji","3"},{"royalty","in","Gaji","3"},
        {"dividen","in","Gaji","4"},{"bagi hasil","in","Gaji","3"},
        {"bunga bank","in","Gaji","3"},{"bunga deposito","in","Gaji","3"},
        {"beasiswa","in","Lainnya","4"},
        // TABUNGAN tambahan
        {"bibit","out","Tabungan","4"},{"ajaib","out","Tabungan","4"},
        {"stockbit","out","Tabungan","3"},{"ipot","out","Tabungan","3"},
        {"bareksa","out","Tabungan","4"},{"tanamduit","out","Tabungan","4"},
        {"nabung saham","out","Tabungan","4"},{"nabung rutin","out","Tabungan","4"},
        {"autodebet","out","Tabungan","3"},{"tabungan berencana","out","Tabungan","4"},
        {"sbn","out","Tabungan","3"},{"sukuk ritel","out","Tabungan","3"},
        {"savings bond","out","Tabungan","3"},{"reksadana pasar uang","out","Tabungan","4"},
        {"reksadana saham","out","Tabungan","4"},{"etf","out","Tabungan","3"},
        {"p2p lending","out","Tabungan","3"},{"akseleran","out","Tabungan","3"},
        {"koinworks","out","Tabungan","3"},{"amartha","out","Tabungan","3"},
        {"emas digital","out","Tabungan","4"},{"tabungan emas","out","Tabungan","4"},
        {"pegadaian","out","Tabungan","3"},{"treasury","out","Tabungan","3"},
        {"syariah","out","Tabungan","3"},{"properti","out","Tabungan","3"},
        {"tanah","out","Tabungan","3"},{"kavling","out","Tabungan","3"},
        // DARURAT tambahan
        {"emergency","out","Darurat","3"},{"mendesak","out","Darurat","3"},
        {"kecelakaan","out","Darurat","4"},{"musibah","out","Darurat","4"},
        {"bencana","out","Darurat","3"},{"banjir","out","Darurat","3"},
        {"kebakaran","out","Darurat","3"},{"gempa","out","Darurat","3"},
        // LAINNYA: pendidikan & sosial tambahan
        {"praktikum","out","Lainnya","3"},{"skripsi","out","Lainnya","3"},
        {"wisuda","out","Lainnya","4"},{"toga","out","Lainnya","3"},
        {"ospek","out","Lainnya","3"},{"study tour","out","Lainnya","3"},
        {"karyawisata","out","Lainnya","3"},{"fotokopi","out","Lainnya","3"},
        {"print","out","Lainnya","3"},{"cetak","out","Lainnya","3"},{"jilid","out","Lainnya","3"},
        {"toefl","out","Lainnya","4"},{"ielts","out","Lainnya","4"},
        {"utbk","out","Lainnya","4"},{"beasiswa","in","Lainnya","4"},
        {"takziah","out","Lainnya","3"},{"melayat","out","Lainnya","3"},
        {"karangan bunga","out","Lainnya","3"},{"papan bunga","out","Lainnya","3"},
        {"buket","out","Lainnya","3"},{"parcel","out","Lainnya","3"},{"hampers","out","Lainnya","3"},
        {"angpao","out","Lainnya","3"},{"amplop","out","Lainnya","3"},
        {"salam tempel","out","Lainnya","3"},{"nafkah","out","Lainnya","4"},
        {"kirim orang tua","out","Lainnya","4"},{"kirim ibu","out","Lainnya","4"},
        {"kirim ayah","out","Lainnya","4"},
    };

    // Singkatan umum -> kata baku (diterapkan saat tokenisasi)
    private static final String[][] ABBR = {
        {"mkn","makan"},{"mnum","minum"},{"bnsin","bensin"},{"lstrk","listrik"},
        {"byr","bayar"},{"tlp","telepon"},{"mtr","motor"},{"mbl","mobil"},
        {"obt","obat"},{"dktr","dokter"},{"trf","transfer"},{"tf","transfer"},
        {"mabar","game"},{"sklh","sekolah"},{"klh","kuliah"},
    };

    private static String expandAbbr(String w) {
        for (String[] a : ABBR) if (a[0].equals(w)) return a[1];
        return w;
    }

    /** Jarak Levenshtein untuk toleransi typo ringan. */
    static int lev(String a, String b) {
        try {
            int n = a.length(), m = b.length();
            if (Math.abs(n - m) > 2) return 99;
            int[] prev = new int[m + 1], cur = new int[m + 1];
            for (int j = 0; j <= m; j++) prev[j] = j;
            for (int i = 1; i <= n; i++) {
                cur[0] = i;
                for (int j = 1; j <= m; j++) {
                    int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                    cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + c);
                }
                int[] t = prev; prev = cur; cur = t;
            }
            return prev[m];
        } catch (Throwable t) {
            return 99;
        }
    }

    // Kata pemicu tipe (token, bukan substring agar aman)
    private static final String[] FORCE_IN = {
        "gajian","gaji","salary","upah","honor","bonus","thr","insentif","komisi",
        "fee","freelance","diterima","refund","cashback"
    };
    private static final String[] FORCE_OUT = {
        "bayar","beli","belanja","jajan"
    };

    private static final String[] STOP = {
        "di","ke","dari","dan","yang","untuk","dengan","pada","ini","itu","saya","aku",
        "the","a","an","sudah","telah","lagi","saja","aja","dong","ya","nih","tuh",
        "buat","bikin","kasih","beri","dapat"
    };

    private static boolean isStop(String w) {
        for (String s : STOP) if (s.equals(w)) return true;
        return false;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.US).trim().replaceAll("\\s+", " ");
    }

    /** Pecah teks jadi token kata (huruf saja), tanpa angka. Singkatan diurai. */
    static List<String> tokens(String text) {
        List<String> out = new ArrayList<String>();
        try {
            String t = norm(text).replaceAll("[^a-z ]", " ");
            for (String w : t.split(" ")) {
                w = w.trim();
                if (w.length() >= 2 && !isStop(w)) out.add(expandAbbr(w));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    /** Gabungan KW + KW2. */
    static List<String[]> allKw() {
        List<String[]> all = new ArrayList<String[]>(KW.length + KW2.length);
        for (String[] k : KW) all.add(k);
        for (String[] k : KW2) all.add(k);
        return all;
    }

    /** Ambil angka terakhir di teks sebagai nominal. Dukung 15rb, 1,5jt, 2 juta, 5.000.000 */
    static long parseAmount(String text) {
        try {
            if (text == null) return 0;
            String t = text.toLowerCase(Locale.US);
            Pattern p = Pattern.compile("(\\d[\\d.,]*)\\s*(rb|ribu|k|jt|juta)?\\b");
            Matcher m = p.matcher(t);
            String num = null, suf = null;
            while (m.find()) { num = m.group(1); suf = m.group(2); }
            if (num == null) return 0;
            String s = num;
            if (s.contains(".") && s.contains(",")) {
                s = s.replace(".", "").replace(",", ".");
            } else if (s.contains(",")) {
                if (s.matches(".*,[0-9]{3}$") && s.length() > 4) s = s.replace(",", "");
                else s = s.replace(",", ".");
            } else if (s.contains(".")) {
                if (s.matches("^[0-9]{1,3}(\\.[0-9]{3})+$")) s = s.replace(".", "");
            }
            double v = Double.parseDouble(s);
            long mult = 1;
            if (suf != null) {
                if (suf.equals("rb") || suf.equals("ribu") || suf.equals("k")) mult = 1000;
                else if (suf.equals("jt") || suf.equals("juta")) mult = 1000000;
            }
            long r = Math.round(v * mult);
            return r > 0 ? r : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** Kategorisasi utama. db boleh null (tanpa kata yang dipelajari). */
    public static Result categorize(DbHelper db, String text) {
        Result r = new Result();
        try {
            r.amount = parseAmount(text);
            String n = norm(text);
            if (n.isEmpty()) return r;
            // uraikan singkatan juga di teks (utk pencocokan regex/frasa)
            try {
                for (String[] ab : ABBR) {
                    n = n.replaceAll("\\b" + Pattern.quote(ab[0]) + "\\b",
                            Matcher.quoteReplacement(ab[1]));
                }
            } catch (Throwable ignored) {}
            List<String> toks = tokens(text);

            // 1) kata yang dipelajari dari koreksi user (bobot tinggi)
            Map<String, int[]> learned = new HashMap<String, int[]>(); // word -> {tipeIdx, bobot}
            Map<String, String[]> learnedMeta = new HashMap<String, String[]>();
            if (db != null) {
                try {
                    for (DbHelper.Word w : db.getLearnedWords()) {
                        learnedMeta.put(w.word, new String[]{w.tipe, w.kategori});
                        learned.put(w.word, new int[]{w.weight});
                    }
                } catch (Throwable ignored) {}
            }

            // 2) skor tiap (tipe|kategori)
            Map<String, Double> score = new HashMap<String, Double>();
            Map<String, String> srcOf = new HashMap<String, String>();
            boolean hitLearned = false;
            java.util.Set<String> exactTokens = new java.util.HashSet<String>();

            List<String[]> all = allKw();
            Map<String, Integer> distOf = new HashMap<String, Integer>(); // jarak edit min per key (0 = cocok persis)
            // pass 1: frasa multi-kata dulu (lebih spesifik); katanya dikonsumsi
            java.util.Set<String> consumed = new java.util.HashSet<String>();
            for (String[] k : all) {
                String kw = k[0];
                if (kw.indexOf(' ') < 0) continue;
                if (n.contains(kw)) {
                    String key = k[1] + "|" + k[2];
                    double w = Double.parseDouble(k[3]) + 2.0; // bonus spesifisitas
                    Double cur = score.get(key);
                    score.put(key, (cur == null ? 0 : cur) + w);
                    if (!srcOf.containsKey(key)) srcOf.put(key, "keyword");
                    distOf.put(key, 0);
                    for (String part : kw.split(" ")) consumed.add(part);
                }
            }
            // pass 2: kata tunggal (lewati yang sudah dikonsumsi frasa)
            for (String[] k : all) {
                String kw = k[0];
                if (kw.indexOf(' ') >= 0) continue;
                if (consumed.contains(kw)) continue;
                boolean hit = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b").matcher(n).find();
                if (hit) {
                    String key = k[1] + "|" + k[2];
                    double w = Double.parseDouble(k[3]);
                    // bonus jika keyword muncul sebagai token persis
                    if (toks.contains(kw)) { w += 1.0; exactTokens.add(kw); }
                    Double cur = score.get(key);
                    score.put(key, (cur == null ? 0 : cur) + w);
                    if (!srcOf.containsKey(key)) srcOf.put(key, "keyword");
                    distOf.put(key, 0);
                }
            }
            // 2b) fuzzy: toleransi typo ringan utk token yang belum cocok
            for (String tok : toks) {
                if (exactTokens.contains(tok) || consumed.contains(tok) || tok.length() < 4) continue;
                String[] bestK = null;
                int bestD = 99;
                for (String[] k : all) {
                    String kw = k[0];
                    if (kw.indexOf(' ') >= 0 || kw.length() < 4) continue;
                    if (Math.abs(kw.length() - tok.length()) > 2) continue;
                    int d = lev(tok, kw);
                    int thr = kw.length() <= 5 ? 1 : 2;
                    if (d <= thr && d < bestD) { bestD = d; bestK = k; }
                }
                if (bestK != null) {
                    String key = bestK[1] + "|" + bestK[2];
                    double w = Double.parseDouble(bestK[3]) * 0.6;
                    Double cur = score.get(key);
                    score.put(key, (cur == null ? 0 : cur) + w);
                    if (!srcOf.containsKey(key)) srcOf.put(key, "keyword");
                    Integer pd = distOf.get(key);
                    if (pd == null || bestD < pd) distOf.put(key, bestD);
                }
            }
            // kata dipelajari: bobot = 4 + weight tersimpan
            for (Map.Entry<String, int[]> e : learned.entrySet()) {
                String w = e.getKey();
                boolean hit = Pattern.compile("\\b" + Pattern.quote(w) + "\\b").matcher(n).find();
                if (hit) {
                    String[] meta = learnedMeta.get(w);
                    String key = meta[0] + "|" + meta[1];
                    double bw = 4.0 + e.getValue()[0];
                    Double cur = score.get(key);
                    score.put(key, (cur == null ? 0 : cur) + bw);
                    srcOf.put(key, "learned");
                    hitLearned = true;
                }
            }

            // 3) pemicu tipe paksa
            boolean fIn = false, fOut = false;
            for (String t : toks) {
                for (String fi : FORCE_IN) if (fi.equals(t)) { fIn = true; break; }
                for (String fo : FORCE_OUT) if (fo.equals(t)) { fOut = true; break; }
            }
            // frasa pemicu
            if (n.contains("transfer masuk") || n.contains("terima ") || n.contains("dapat ")) fIn = true;

            // 4) pemenang (seri -> jarak edit terkecil menang)
            String best = null;
            double bestScore = 0, total = 0;
            int bestDist = 99;
            for (Map.Entry<String, Double> e : score.entrySet()) {
                total += e.getValue();
                double s = e.getValue();
                int dd = distOf.containsKey(e.getKey()) ? distOf.get(e.getKey()) : 99;
                if (s > bestScore + 1e-9 || (Math.abs(s - bestScore) < 1e-9 && dd < bestDist)) {
                    bestScore = s; best = e.getKey(); bestDist = dd;
                }
            }

            if (best != null) {
                String[] parts = best.split("\\|");
                r.tipe = parts[0];
                r.kategori = parts[1];
                r.src = srcOf.get(best);
                // paksa tipe bila ada trigger berlawanan dengan skor tipis;
                // kategori ikut direset agar konsisten (in->Lainnya)
                if (fIn && r.tipe.equals("out") && bestScore < 6) {
                    r.tipe = "in"; r.kategori = "Lainnya"; r.src = "trigger";
                }
                if (fOut && r.tipe.equals("in") && bestScore < 6) {
                    r.tipe = "out"; r.kategori = "Lainnya"; r.src = "trigger";
                }
                int conf = 55 + (int) Math.round(bestScore * 7);
                if (hitLearned) conf += 5;
                r.confidence = Math.max(55, Math.min(98, conf));
            } else {
                r.tipe = fIn ? "in" : "out";
                r.kategori = "Lainnya";
                r.src = (fIn || fOut) ? "trigger" : "default";
                r.confidence = (fIn || fOut) ? 60 : 40;
            }
        } catch (Throwable ignored) {}
        return r;
    }

    /** Simpan koreksi user: kata penting dari teks -> (tipe, kategori). */
    public static void learn(DbHelper db, String text, String tipe, String kategori) {
        try {
            if (db == null || text == null || kategori == null) return;
            if (!"in".equals(tipe)) tipe = "out";
            for (String w : tokens(text)) {
                if (w.length() < 3) continue;
                // jangan pelajari kata yang sudah ada di kamus bawaan / trigger
                if (isBuiltin(w) || isTrigger(w)) continue;
                db.learnWord(w, tipe, kategori);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isTrigger(String w) {
        for (String t : FORCE_IN) if (t.equals(w)) return true;
        for (String t : FORCE_OUT) if (t.equals(w)) return true;
        return false;
    }

    private static boolean isBuiltin(String w) {
        for (String[] k : KW) {
            if (k[0].equals(w)) return true;
            if (k[0].indexOf(' ') >= 0) {
                for (String part : k[0].split(" ")) if (part.equals(w)) return true;
            }
        }
        for (String[] k : KW2) {
            if (k[0].equals(w)) return true;
            if (k[0].indexOf(' ') >= 0) {
                for (String part : k[0].split(" ")) if (part.equals(w)) return true;
            }
        }
        return false;
    }
}
