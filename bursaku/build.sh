#!/bin/bash
# Build manual aplikasi BursaKu + AdMob (aapt2 + javac + d8, tanpa Gradle)
# Termasuk integrasi AAR play-services-ads manual (lihat ~/AGENTS.md).
set -e
APP=bursaku
PKG=com.flexurio.bursaku
SDK=~/android-sdk
BT=$SDK/build-tools/37.0.0
D8=$BT/d8
PJ=~/workspace/$APP/src
AAR=~/workspace/aplikasi-anak/aar/full_ext
JARS=~/workspace/aplikasi-anak/aar/full
OUT=/tmp/apkbuild-$APP
GENR=~/workspace/aplikasi-anak/aar/gen_lib_r.py
KS=~/workspace/$APP/release/$APP-release.keystore
KSPASS=$(cat ~/workspace/$APP/release/keystore-password.txt)
ALIAS=bursaku
APKOUT=~/workspace/$APP/$APP.apk
rm -rf $OUT && mkdir -p $OUT/compiled $OUT/gen $OUT/classes $OUT/libR
export PATH=$HOME/jdk/bin:$PATH
export JAVA_HOME=$HOME/jdk

echo "== [$APP] 1. aapt2 compile (app + AAR) =="
$BT/aapt2 compile --dir $PJ/res -o $OUT/compiled/res.zip
for d in $AAR/*/; do
  n=$(basename $d)
  if [ -d "$d/res" ]; then
    $BT/aapt2 compile --dir "$d/res" -o $OUT/compiled/$n.zip 2>&1 | grep -vi warning | head -3
  fi
done

echo "== [$APP] 2. aapt2 link =="
$BT/aapt2 link -o $OUT/base.apk \
  -I $SDK/platforms/android-36/android.jar \
  --manifest $PJ/AndroidManifest.xml \
  --min-sdk-version 24 --target-sdk-version 36 \
  --version-code 6 --version-name "2.0.2" \
  -A $PJ/assets \
  --java $OUT/gen \
  --output-text-symbols $OUT/R.txt \
  $OUT/compiled/*.zip 2>&1 | grep -vi warning | head -5

echo "== [$APP] 3. generate library R.java =="
python3 $GENR $OUT/R.txt $AAR $OUT/libR

echo "== [$APP] 4. javac =="
CP="$SDK/platforms/android-36/android.jar"
for j in $AAR/*/classes.jar; do CP="$CP:$j"; done
for j in $JARS/*.jar; do CP="$CP:$j"; done
javac -source 8 -target 8 -nowarn -classpath "$CP" -d $OUT/classes \
  $(find $PJ/java $OUT/gen $OUT/libR -name "*.java") 2>&1 | grep -v "^Note" | head -20
echo "javac: $(find $OUT/classes -name '*.class' | wc -l) classes"

echo "== [$APP] 5. d8 (build-tools 37) =="
# listenablefuture-1.0.jar adalah subset guava -> kecualikan (duplikat class)
$D8 --lib $SDK/platforms/android-36/android.jar --min-api 24 --output $OUT \
  $(find $OUT/classes -name '*.class') $AAR/*/classes.jar \
  $(ls $JARS/*.jar | grep -v listenablefuture)
echo "dex: $(ls -la $OUT/classes.dex | awk '{print $5}') bytes"

echo "== [$APP] 6. sign =="
cd $OUT && zip -q -j base.apk classes.dex && cd $PJ
$BT/zipalign -f 4 $OUT/base.apk $OUT/aligned.apk
$BT/apksigner sign --ks $KS --ks-pass pass:"$KSPASS" --key-pass pass:"$KSPASS" --out $APKOUT $OUT/aligned.apk
$BT/apksigner verify --print-certs $APKOUT | head -1
ls -la $APKOUT
echo "DONE $APP"
