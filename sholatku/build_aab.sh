#!/bin/bash
# Build AAB release "SholatKu" dari hasil link build.sh (tanpa Gradle)
set -e
APP=sholat-dzikir-doa
PKG=com.flexurio.sholatku
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

if [ ! -f "$OUT/base.apk" ]; then
  echo "Jalankan build.sh dulu (butuh $OUT/base.apk)"; exit 1
fi

echo "== [$APP] AAB: link proto =="
rm -rf $AABW && mkdir -p $AABW
$BT/aapt2 link --proto-format -o $AABW/base.apk \
  -I $SDK/platforms/android-36/android.jar \
  --manifest $PJ/AndroidManifest.xml \
  --min-sdk-version 24 --target-sdk-version 36 \
  --version-code 14 --version-name "2.0.12" \
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
cp $OUT/classes.dex module/base/dex/
cd module/base && zip -q -r $AABW/base.zip manifest dex res assets resources.pb

echo "== [$APP] AAB: bundletool =="
cd $AABW && java -jar $BTOOL build-bundle --modules=base.zip --output=sholatku.aab
java -jar $BTOOL validate --bundle=sholatku.aab 2>&1 | head -3

echo "== [$APP] AAB: sign =="
$BT/apksigner sign --ks $KS --ks-pass pass:"$KSPASS" --key-pass pass:"$KSPASS" \
  --min-sdk-version 24 --out $AABOUT sholatku.aab
jarsigner -verify $AABOUT 2>&1 | head -1
ls -la $AABOUT
echo "DONE $APP AAB"
