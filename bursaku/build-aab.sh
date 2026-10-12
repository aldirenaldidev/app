#!/bin/bash
# Build AAB release BursaKu dari source yang sama (manual aapt2 + bundletool).
# Prasyarat: ~/workspace/bursaku/build.sh sudah sukses (menghasilkan
# /tmp/apkbuild-bursaku/compiled/*.zip dan classes.dex).
set -e
APP=bursaku
SDK=~/android-sdk
BT=$SDK/build-tools/37.0.0
PJ=~/workspace/$APP/src
OUT=/tmp/apkbuild-$APP
AABW=/tmp/aabwork-$APP
KS=~/workspace/$APP/release/$APP-release.keystore
KSPASS=$(cat ~/workspace/$APP/release/keystore-password.txt)
AABOUT=~/workspace/$APP/release/$APP-release.aab
export PATH=$HOME/jdk/bin:$PATH
export JAVA_HOME=$HOME/jdk

if [ ! -f "$OUT/classes.dex" ]; then
  echo "Jalankan build.sh dulu (butuh classes.dex)"; exit 1
fi

echo "== [AAB] 1. aapt2 link --proto-format =="
rm -rf $AABW && mkdir -p $AABW
$BT/aapt2 link --proto-format -o $AABW/base.apk \
  -I $SDK/platforms/android-36/android.jar \
  --manifest $PJ/AndroidManifest.xml \
  --min-sdk-version 24 --target-sdk-version 36 \
  --version-code 6 --version-name "2.0.2" \
  -A $PJ/assets \
  $OUT/compiled/*.zip 2>&1 | grep -vi warning | head -5

echo "== [AAB] 2. susun modul base =="
rm -rf $AABW/apkx $AABW/module && mkdir -p $AABW/apkx $AABW/module/base
cd $AABW && unzip -q -o base.apk -d apkx
mkdir -p module/base/manifest module/base/dex
cp apkx/AndroidManifest.xml module/base/manifest/
cp apkx/resources.pb module/base/
cp -r apkx/res module/base/
[ -d apkx/assets ] && cp -r apkx/assets module/base/
cp $OUT/classes.dex module/base/dex/

echo "== [AAB] 3. bundletool build-bundle =="
cd module/base && zip -q -r $AABW/base.zip manifest dex res assets resources.pb
cd $AABW && java -jar /tmp/bundletool.jar build-bundle \
  --modules=base.zip --output=bursaku.aab
java -jar /tmp/bundletool.jar validate --bundle=bursaku.aab 2>&1 | head -3

echo "== [AAB] 4. sign =="
$BT/apksigner sign --ks $KS --ks-pass pass:"$KSPASS" \
  --key-pass pass:"$KSPASS" --min-sdk-version 24 \
  --out $AABOUT bursaku.aab
jarsigner -verify $AABOUT 2>&1 | head -2
ls -la $AABOUT
echo "DONE AAB"
