
echo -e "   _____                        _              "
echo -e "  / ____|                      (_)             "
echo -e " | (___  _   _  __ _  __ _ _ __ _ _______ _ __ "
echo -e "  \\___ \\| | | |/ _\` |/ _\` | '__| |_  / _ \\ '__|"
echo -e "  ____) | |_| | (_| | (_| | |  | |/ /  __/ |   "
echo -e " |_____/ \\__,_|\\__, |\\__,_|_|  |_/___\\___|_|   "
echo -e "                __/ |                          "
echo -e "               |___/                           "
echo -e "APK builder"
if [ ! -f "/sugarizer/config.xml" ]
then
	echo "ERROR: Can't find /sugarizer repository"
	exit
fi
if [ ! -f "/cordova-plugin-sugarizeros/plugin.xml" ]
then
	echo "ERROR: Can't find /cordova-plugin-sugarizeros repository"
	exit
fi
if [ ! -d "/output" ]
then
	echo "ERROR: Can't find /output directory"
	exit
fi

date

echo -- Install plugins
cd /sugarizer-cordova
cp ../sugarizer/config.xml .
mkdir -p ../sugarizer-cordova/res
cp -r ../sugarizer/res/* ../sugarizer-cordova/res
sed -i -e "s/org.olpc-france.sugarizer/org.olpc_france.sugarizer/" config.xml
sed -i -e "s/\"landscape\"/\"userLandscape\"/" config.xml
echo n | cordova platform add android@6.3.0
cordova plugin remove cordova-plugin-camera --force
cordova plugin remove cordova-plugin-compat --force
cordova plugin remove cordova-plugin-device --force
cordova plugin remove cordova-plugin-device-motion --force
cordova plugin remove cordova-plugin-dialogs --force
cordova plugin remove cordova-plugin-file --force
cordova plugin remove cordova-plugin-fullscreen --force
cordova plugin remove cordova-plugin-ios-longpress-fix --force
cordova plugin remove cordova-plugin-media --force
cordova plugin remove cordova-plugin-network-information --force
cordova plugin remove cordova-plugin-qrscanner --force
cordova plugin remove cordova-plugin-splashscreen --force
cordova plugin remove cordova-plugin-vibration --force
cordova plugin remove cordova-plugin-whitelist --force
cordova plugin remove com.develcode.plugins.volumeControl --force
cordova plugin remove cordova-plugin-file-transfer --force
cordova plugin remove cordova-plugin-swift-support --force
cordova plugin add cordova-plugin-camera@2.4.0
cordova plugin add cordova-plugin-file@4.3.1
cordova plugin add cordova-plugin-device@1.1.0
cordova plugin add cordova-plugin-device-motion@1.2.0
cordova plugin add cordova-plugin-dialogs@1.2.0
cordova plugin add cordova-plugin-file-transfer@1.6.0
cordova plugin add cordova-plugin-fullscreen@1.1.0
cordova plugin add cordova-plugin-ios-longpress-fix@1.1.0
cordova plugin add cordova-plugin-media@1.0.1
cordova plugin add cordova-plugin-network-information@1.1.0
cordova plugin add cordova-plugin-qrscanner@2.5.0
cordova plugin add cordova-plugin-splashscreen@4.1.0
cordova plugin add cordova-plugin-swift-support@3.1.1
cordova plugin add cordova-plugin-vibration@2.1.1
cordova plugin add cordova-plugin-whitelist@1.3.1
cordova plugin add https://github.com/manusimpson/Phonegap-Android-VolumeControl.git

echo --- Detect Sugarizeros
if [ "$1" == "os" -o "$2" == "os" ]; then
  sed -i -e "s/&SugarizerOS/ -->/" config.xml
  sed -i -e "s/SugarizerOS&/<!-- /" config.xml
  sed -i -e "s/org.olpc_france.sugarizer/org.olpc_france.sugarizeros/" config.xml
  sed -i -e "s/<name>Sugarizer/<name>Sugarizer OS/" config.xml
  cordova plugin add ../cordova-plugin-sugarizeros
else
  cordova plugin remove cordova-plugin-sugarizeros
fi

echo --- Deleting previous content...
cd www
rm -rf *
cd ..

echo --- Copying content
rsync -av --exclude-from='exclude.android' ../sugarizer/* www
cp etoys_remote.index.html www/activities/Etoys.activity/index.html
if [ "$1" != "full" -a "$2" != "full" ]; then
  echo --- Minimize
  cd ../sugarizer
  npm install grunt grunt-contrib-jshint grunt-contrib-nodeunit grunt-contrib-uglify
  grunt -v
  cd ../sugarizer-cordova
  cp -r ../sugarizer/build/* www/
fi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-xxhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-xxxhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-ldpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-mdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-hdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-xhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-land-hdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-land-ldpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-land-mdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-land-xhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-land-xxhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-land-xxxhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-port-hdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-port-ldpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-port-mdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-port-xhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-port-xxhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/drawable-port-xxxhdpi
cp ../sugarizer/res/icon/android/icon-144-xxhdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-xxhdpi/icon.png
cp ../sugarizer/res/icon/android/icon-192-xxxhdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-xxxhdpi/icon.png
cp ../sugarizer/res/icon/android/icon-36-ldpi.png ../sugarizer-cordova/platforms/android/res/mipmap-ldpi/icon.png
cp ../sugarizer/res/icon/android/icon-48-mdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-mdpi/icon.png
cp ../sugarizer/res/icon/android/icon-72-hdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-hdpi/icon.png
cp ../sugarizer/res/icon/android/icon-96-xhdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-xhdpi/icon.png
cp ../sugarizer/res/splash/android/drawable-land-hdpi.png ../sugarizer-cordova/platforms/android/res/drawable-land-hdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-land-ldpi.png ../sugarizer-cordova/platforms/android/res/drawable-land-ldpi/screen.png
cp ../sugarizer/res/splash/android/drawable-land-mdpi.png ../sugarizer-cordova/platforms/android/res/drawable-land-mdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-land-xhdpi.png ../sugarizer-cordova/platforms/android/res/drawable-land-xhdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-land-xxhdpi.png ../sugarizer-cordova/platforms/android/res/drawable-land-xxhdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-land-xxxhdpi.png ../sugarizer-cordova/platforms/android/res/drawable-land-xxxhdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-port-hdpi.png ../sugarizer-cordova/platforms/android/res/drawable-port-hdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-port-ldpi.png ../sugarizer-cordova/platforms/android/res/drawable-port-ldpi/screen.png
cp ../sugarizer/res/splash/android/drawable-port-mdpi.png ../sugarizer-cordova/platforms/android/res/drawable-port-mdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-port-xhdpi.png ../sugarizer-cordova/platforms/android/res/drawable-port-xhdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-port-xxhdpi.png ../sugarizer-cordova/platforms/android/res/drawable-port-xxhdpi/screen.png
cp ../sugarizer/res/splash/android/drawable-port-xxxhdpi.png ../sugarizer-cordova/platforms/android/res/drawable-port-xxxhdpi/screen.png

echo --- Build Cordova debug version
cordova build android

echo --- Copy APK to output
if [ "$1" == "os" -o "$2" == "os" ]; then
	cp /sugarizer-cordova/platforms/android/build/outputs/apk/android-debug.apk /output/sugarizeros.apk
else
	cp /sugarizer-cordova/platforms/android/build/outputs/apk/android-debug.apk /output/sugarizer.apk
fi

date
