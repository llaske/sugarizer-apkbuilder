
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
echo n | cordova platform add android@9.0.0
cordova plugin add cordova-plugin-inappbrowser@5.0.0
cordova plugin add cordova-plugin-camera@5.0.1
cordova plugin add cordova-plugin-file@6.0.2
cordova plugin add cordova-plugin-device@2.0.3
cordova plugin add cordova-plugin-device-motion@2.0.1
cordova plugin add cordova-plugin-dialogs@2.0.2
cordova plugin add cordova-plugin-file-transfer@1.7.1
cordova plugin add cordova-plugin-fullscreen@1.3.0
cordova plugin add cordova-plugin-ios-longpress-fix@1.1.0
cordova plugin add cordova-plugin-media@5.0.3
cordova plugin add cordova-plugin-media-capture@3.0.3
cordova plugin add cordova-plugin-network-information@2.0.2
cordova plugin add cordova-plugin-qrscanner@3.0.1
cordova plugin add cordova-plugin-splashscreen@6.0.0
cordova plugin add cordova-plugin-add-swift-support@2.0.2
cordova plugin add cordova-plugin-vibration@3.1.1
cordova plugin add cordova-plugin-whitelist@1.3.4
cordova plugin add cordova-plugin-ionic-keyboard@2.2.0
cordova plugin add https://github.com/manusimpson/Phonegap-Android-VolumeControl.git

echo --- Reading arguments
minsize=false
full=false
release=false
sign=false
os=false
excluded=false

for i in $*; do
	if [ ${i%%=*} = "exclude-activities" ]
	then
		activities=${i#*=}
		lastActivity=${activities##*,}
		remaining=true
		excluded=true
		cp exclude.android exclude.bak.android
		echo -e "\n" >> exclude.android
		cp ../sugarizer/activities.json ../sugarizer/activities.bak.json
		while [ $remaining == true ]
		do
			activity=${activities%%,*}
			activities=${activities#*,}
			if [ ${#activity} -gt 0 ]
			then
				sed -i "/${activity}/d" ../sugarizer/activities.json
				echo "activities/$activity.activity/" >> exclude.android
			fi
			if [ $activity = $lastActivity ]
			then
				remaining=false
			fi
		done
		sed -i "$(( $( wc -l < ../sugarizer/activities.json) -1 ))s/,$//" ../sugarizer/activities.json
	elif [ $i = "minsize" ]
	then
		minsize=true
	elif [ $i = "full" ]
	then
		full=true
	elif [ $i = "release" ]
	then
		release=true
	elif [ $i = "sign" ]
	then
		sign=true
	elif [ $i = "os" ]
	then
		os=true
	fi
done

echo --- Detect Sugarizeros
if [ $os == true ]; then
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

if [ $excluded == true ]
then
	rm ../sugarizer/activities.json
	mv ../sugarizer/activities.bak.json ../sugarizer/activities.json
fi

cp etoys_remote.index.html www/activities/Etoys.activity/index.html
if [ $minsize == true ]; then
	rm -rf www/activities/Abecedarium.activity/audio/en/*
	rm -rf www/activities/Abecedarium.activity/audio/fr/*
	rm -rf www/activities/Abecedarium.activity/audio/es/*
	rm -rf www/activities/Abecedarium.activity/images/database/*
	rm -rf www/activities/Scratch.activity/static/internal-assets/*
	sed -i -e 's/class="offlinemode"//' www/activities/Scratch.activity/index.html
fi
if [ $full != true ]; then
	echo --- Minimize Javascript files
	cd www
	npm install grunt grunt-contrib-jshint grunt-contrib-nodeunit grunt-terser
	grunt -v
	rm -rf node_modules
	cd ..
fi
if [ $excluded == true ]
then
	rm exclude.android
	mv exclude.bak.android exclude.android
fi

mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-xxhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-xxxhdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-ldpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-mdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-hdpi
mkdir -p ../sugarizer-cordova/platforms/android/res/mipmap-xhdpi
cp ../sugarizer/res/icon/android/icon-144-xxhdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-xxhdpi/icon.png
cp ../sugarizer/res/icon/android/icon-192-xxxhdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-xxxhdpi/icon.png
cp ../sugarizer/res/icon/android/icon-36-ldpi.png ../sugarizer-cordova/platforms/android/res/mipmap-ldpi/icon.png
cp ../sugarizer/res/icon/android/icon-48-mdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-mdpi/icon.png
cp ../sugarizer/res/icon/android/icon-72-hdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-hdpi/icon.png
cp ../sugarizer/res/icon/android/icon-96-xhdpi.png ../sugarizer-cordova/platforms/android/res/mipmap-xhdpi/icon.png

rm -f platforms/android/build/outputs/apk/*.apk
export CORDOVA_ANDROID_GRADLE_DISTRIBUTION_URL=file:///sugarizer-cordova/gradle-6.5-all.zip
sed -i -e "s/startsWith('1.8.')/startsWith('11.')/" platforms/android/cordova/lib/check_reqs.js
if [ $release == true -o $sign == true ]; then
	echo --- Build Cordova release version
	FILENAME=release/app-release-unsigned.apk
	cordova build android --release
else
	echo --- Build Cordova debug version
	FILENAME=debug/app-debug.apk
	cordova build android
fi


echo --- Sign release version
if [ $sign == true ]; then
	cd platforms/android/app/build/outputs/apk/release
	/opt/android-sdk-linux/build-tools/29.0.3/zipalign -v 4 app-release-unsigned.apk app-release-aligned.apk
	/opt/android-sdk-linux/build-tools/29.0.3/apksigner sign --ks /output/${SUGARIZER_KEYSTOREFILE} --ks-pass env:SUGARIZER_STOREPASS --out app-release-signed.apk app-release-aligned.apk
	/opt/android-sdk-linux/build-tools/29.0.3/apksigner verify app-release-signed.apk
	cd ../../../../..
	FILENAME=release/app-release-signed.apk
fi

echo --- Copy APK to output
if [ $os == true ]; then
	cp /sugarizer-cordova/platforms/android/app/build/outputs/apk/$FILENAME /output/sugarizeros.apk
else
	cp /sugarizer-cordova/platforms/android/app/build/outputs/apk/$FILENAME /output/sugarizer.apk
fi

date
