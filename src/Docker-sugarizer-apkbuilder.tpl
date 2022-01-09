FROM runmymind/docker-android-sdk:ubuntu-standalone
WORKDIR /

# Install tools
RUN apt-get update
RUN apt-get install -y sudo gnupg gnupg1 gnupg2 rsync

# Install node
RUN curl -sL https://deb.nodesource.com/setup_14.x | sudo -E bash -
RUN apt-get install -y nodejs

# Intall Cordova
RUN sudo npm install -g cordova@10.0 grunt-cli

# Sugarizer settings
RUN cordova create sugarizer-cordova
RUN rm -rf /sugarizer-cordova/config.xml /sugarizer-cordova/www
RUN mkdir /sugarizer-cordova/www

COPY make_android.sh /sugarizer-cordova
COPY exclude.android /sugarizer-cordova
COPY etoys_remote.index.html /sugarizer-cordova
COPY gradle-6.5-all.zip /sugarizer-cordova

ENV PATH="${PATH}:/opt/android-sdk-linux:/opt/android-sdk-linux/bin:/opt/gradle/gradle-6.8.3/bin"
ENV ANDROID_HOME="/opt/android-sdk-linux"
ENV ANDROID_SDK_ROOT="/opt/android-sdk-linux"
ENV JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64/"

# Install Gradle
WORKDIR /opt/
RUN mkdir gradle
WORKDIR /opt/gradle
RUN wget https://services.gradle.org/distributions/gradle-6.8.3-bin.zip
RUN unzip gradle-6.8.3-bin.zip
RUN rm gradle-6.8.3-bin.zip

# Download Android SDK 29
WORKDIR /opt/android-sdk-linux/build-tools/
RUN wget https://dl.google.com/android/repository/build-tools_r29.0.3-linux.zip
RUN unzip build-tools_r29.0.3-linux.zip
RUN mv android-10 29.0.3
RUN rm build-tools_r29.0.3-linux.zip
WORKDIR /opt/android-sdk-linux/platforms/
RUN wget https://dl.google.com/android/repository/platform-29_r05.zip
RUN unzip platform-29_r05.zip
RUN mv android-10 android-29
RUN rm platform-29_r05.zip


WORKDIR /sugarizer-cordova

ENTRYPOINT ["/bin/bash", "/sugarizer-cordova/make_android.sh"]
