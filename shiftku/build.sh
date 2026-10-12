#!/bin/bash
# Build manual APK release ShiftKu + AdMob + Firebase (aapt2 + javac + d8, tanpa Gradle)
set -e
set -o pipefail
APP=shiftku
PKG=com.flexurio.shiftku
ALIAS=shiftku
SDK=~/android-sdk
BT=$SDK/build-tools/37.0.0
D8=$BT/d8
PJ=~/workspace/$APP/src
AAR=~/workspace/aplikasi-anak/aar/full_ext
JARS=~/workspace/aplikasi-anak/aar/full
FB_AAR=~/workspace/aplikasi-anak/aar/fb_ext
FB_JARS=~/workspace/aplikasi-anak/aar/fb
OUT=/tmp/apkbuild-$APP
GENR=~/workspace/aplikasi-anak/aar/gen_lib_r.py
REL=~/workspace/$APP/release
KS=$REL/$APP-release.keystore
APKOUT=~/workspace/$APP/$APP.apk

export PATH=$HOME/jdk/bin:$PATH
export JAVA_HOME=$HOME/jdk

# Keystore (sekali saja) — user WAJIB mem-backup file ini + passwordnya
if [ ! -f "$KS" ]; then
  echo "== [$APP] 0. keystore baru =="
  mkdir -p $REL
  KSPASS=$(openssl rand -base64 18 | tr -d '/+=' | head -c 20)
  printf '%s' "$KSPASS" > $REL/keystore-password.txt
  chmod 600 $REL/keystore-password.txt
  keytool -genkeypair -keystore "$KS" -storepass "$KSPASS" -alias $ALIAS \
    -keypass "$KSPASS" -keyalg RSA -keysize 2048 -validity 10950 \
    -dname "CN=ShiftKu, OU=aldire__, O=Personal, C=ID" 2>&1 | tail -1 || true
fi
KSPASS=$(cat $REL/keystore-password.txt)

rm -rf $OUT && mkdir -p $OUT/compiled $OUT/gen $OUT/classes $OUT/libR $OUT/lib

echo "== [$APP] 1. aapt2 compile (app + AAR + Firebase AAR, dedup) =="
$BT/aapt2 compile --dir $PJ/res -o $OUT/compiled/res.zip
# dedup AAR: bila artefak sama ada di full_ext & fb_ext, pakai fb_ext
declare -A RSEEN
for d in $FB_AAR/*/ $AAR/*/; do
  n=$(basename $d)
  a=$(echo "$n" | sed -E 's/-[0-9][0-9A-Za-z._-]*$//')
  if [ -n "${RSEEN[$a]}" ]; then continue; fi
  RSEEN[$a]=1
  if [ -d "$d/res" ]; then
    $BT/aapt2 compile --dir "$d/res" -o $OUT/compiled/$n.zip 2>&1 | grep -vi warning | head -3 || true
  fi
done

echo "== [$APP] 2. aapt2 link =="
$BT/aapt2 link -o $OUT/base.apk \
  -I $SDK/platforms/android-36/android.jar \
  --manifest $PJ/AndroidManifest.xml \
  --min-sdk-version 24 --target-sdk-version 36 \
  --version-code 13 --version-name "1.2.1" \
  -A $PJ/assets \
  --java $OUT/gen \
  --output-text-symbols $OUT/R.txt \
  $OUT/compiled/*.zip 2>&1 | grep -vi warning | head -5 || true

echo "== [$APP] 3. generate library R.java =="
python3 $GENR $OUT/R.txt $AAR $OUT/libR > /tmp/genr.log 2>&1
python3 $GENR $OUT/R.txt $FB_AAR $OUT/libR >> /tmp/genr.log 2>&1
echo "gen_lib_r: $(tail -1 /tmp/genr.log)"

# ---- dedup: untuk artefak yang sama di full/ dan fb/, pakai versi fb ----
declare -A SEEN
artname() { # "foo-1.2.3.jar" -> "foo"
  local b=$(basename "$1")
  echo "$b" | sed -E 's/-[0-9][0-9A-Za-z._-]*\.(jar|aar)$//'
}
D8JARS=""
for j in $FB_JARS/*.jar; do
  a=$(artname "$j")
  # listenablefuture adalah subset guava -> kecualikan
  if [ "$a" = "listenablefuture" ]; then continue; fi
  SEEN[$a]=1; D8JARS="$D8JARS $j"
done
for j in $JARS/*.jar; do
  a=$(artname "$j")
  if [ -z "${SEEN[$a]}" ] && [ "$a" != "listenablefuture" ]; then
    SEEN[$a]=1; D8JARS="$D8JARS $j"
  fi
done
D8AARS=""
for d in $FB_AAR/*/; do
  if [ -f "$d/classes.jar" ]; then
    a=$(basename "$d" | sed -E 's/-[0-9][0-9A-Za-z._-]*$//')
    SEEN["aar:$a"]=1; D8AARS="$D8AARS $d/classes.jar"
  fi
done
for d in $AAR/*/; do
  if [ -f "$d/classes.jar" ]; then
    a=$(basename "$d" | sed -E 's/-[0-9][0-9A-Za-z._-]*$//')
    if [ -z "${SEEN[aar:$a]}" ]; then
      SEEN["aar:$a"]=1; D8AARS="$D8AARS $d/classes.jar"
    fi
  fi
done

echo "== [$APP] 3b. generate FirebaseConfig.java dari google-services.json =="
mkdir -p $OUT/fbgen/com/flexurio/shiftku
python3 ~/workspace/$APP/gen_firebase_config.py ~/workspace/$APP/google-services.json $OUT/fbgen/com/flexurio/shiftku

echo "== [$APP] 4. javac =="
CP="$SDK/platforms/android-36/android.jar"
for j in $D8AARS; do CP="$CP:$j"; done
for j in $D8JARS; do CP="$CP:$j"; done
javac -source 8 -target 8 -nowarn -classpath "$CP" -d $OUT/classes \
  $(find $PJ/java $OUT/gen $OUT/libR $OUT/fbgen -name "*.java") 2>&1 | grep -v "^Note" | head -20 || true
echo "javac: $(find $OUT/classes -name '*.class' | wc -l) classes"

echo "== [$APP] 5. d8 (build-tools 37) =="
$D8 --lib $SDK/platforms/android-36/android.jar --min-api 24 --output $OUT \
  $(find $OUT/classes -name '*.class') $D8AARS $D8JARS
echo "dex files: $(ls $OUT/classes*.dex | tr '\n' ' ')"

echo "== [$APP] 5b. kumpulkan jni .so (Firestore native) =="
for d in $FB_AAR/*/ $AAR/*/; do
  if [ -d "$d/jni" ]; then
    for abi in $(ls "$d/jni"); do
      mkdir -p "$OUT/lib/$abi"
      cp -n "$d/jni/$abi/"*.so "$OUT/lib/$abi/" 2>/dev/null || true
    done
  fi
done
find $OUT/lib -name "*.so" | head -10

echo "== [$APP] 6. sign =="
cd $OUT && zip -q -j base.apk classes*.dex && cd $PJ
if [ -n "$(find $OUT/lib -name '*.so' 2>/dev/null)" ]; then
  cd $OUT && zip -q -r base.apk lib && cd $PJ
fi
$BT/zipalign -f 4 $OUT/base.apk $OUT/aligned.apk
$BT/apksigner sign --ks $KS --ks-pass pass:"$KSPASS" --key-pass pass:"$KSPASS" \
  --out $APKOUT $OUT/aligned.apk
$BT/apksigner verify --print-certs $APKOUT | head -1
ls -la $APKOUT
echo "DONE $APP"
