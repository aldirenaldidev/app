#!/bin/bash
# Build AAB release "ShiftKu" dari hasil build.sh (tanpa Gradle)
# Pola: ~/workspace/sholat-dzikir-doa/build_aab.sh (disesuaikan untuk multi-dex)
set -e
APP=shiftku
SDK=~/android-sdk
BT=$SDK/build-tools/37.0.0
PJ=~/workspace/$APP/src
OUT=/tmp/apkbuild-$APP
AABW=/tmp/aabwork-$APP
REL=~/workspace/$APP/release
KS=$REL/$APP-release.keystore
KSPASS=$(cat $REL/keystore-password.txt)
AABOUT=$REL/$APP-release.aab
BTOOL=~/workspace/bin/bundletool.jar

export PATH=$HOME/jdk/bin:$PATH
export JAVA_HOME=$HOME/jdk

if [ ! -d "$OUT/compiled" ]; then
  echo "Jalankan build.sh dulu (butuh $OUT/compiled)"; exit 1
fi

echo "== [$APP] AAB: link proto =="
rm -rf $AABW && mkdir -p $AABW
$BT/aapt2 link --proto-format -o $AABW/base.apk \
  -I $SDK/platforms/android-36/android.jar \
  --manifest $PJ/AndroidManifest.xml \
  --min-sdk-version 24 --target-sdk-version 36 \
  --version-code 13 --version-name "1.2.1" \
  -A $PJ/assets \
  $OUT/compiled/*.zip 2>&1 | grep -vi warning | head -3 || true

echo "== [$APP] AAB: susun modul =="
rm -rf $AABW/apkx $AABW/module && mkdir -p $AABW/apkx $AABW/module/base
cd $AABW && unzip -q -o base.apk -d apkx
mkdir -p module/base/manifest module/base/dex
cp apkx/AndroidManifest.xml module/base/manifest/
cp apkx/resources.pb module/base/
cp -r apkx/res module/base/
[ -d apkx/assets ] && cp -r apkx/assets module/base/
# multi-dex: copy semua classes*.dex
for d in $OUT/classes*.dex; do cp "$d" module/base/dex/; done
echo "dex files: $(ls module/base/dex/)"
cd module/base && zip -q -r $AABW/base.zip manifest dex res assets resources.pb 2>/dev/null || \
  zip -q -r $AABW/base.zip manifest dex res resources.pb

echo "== [$APP] AAB: bundletool =="
cd $AABW && java -jar $BTOOL build-bundle --modules=base.zip --output=shiftku.aab
java -jar $BTOOL validate --bundle=shiftku.aab 2>&1 | head -3

echo "== [$APP] AAB: sign (jarsigner) =="
# AAB tidak bisa di-sign dengan apksigner, pakai jarsigner
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore "$KS" -storepass "$KSPASS" -keypass "$KSPASS" \
  -signedjar "$AABOUT" $AABW/shiftku.aab shiftku 2>&1 | grep -v "^Warning" | head -2
jarsigner -verify "$AABOUT" 2>&1 | head -1
ls -la $AABOUT
echo "DONE $APP AAB"
