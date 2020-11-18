FROM runmymind/docker-android-sdk:ubuntu-standalone
WORKDIR /

# Install tools
RUN apt-get update
RUN apt-get install -y sudo gnupg gnupg1 gnupg2 rsync

# Install gradle
RUN wget https://services.gradle.org/distributions/gradle-5.2.1-bin.zip
RUN unzip -d /opt/gradle gradle-5.2.1-bin.zip
RUN rm gradle-5.2.1-bin.zip

# Install node
RUN curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
RUN apt-get install -y nodejs

# Intall Cordova
RUN sudo npm install -g cordova@7.1 grunt-cli

# Sugarizer settings
RUN cordova create sugarizer-cordova
RUN rm -rf /sugarizer-cordova/config.xml /sugarizer-cordova/www
RUN mkdir /sugarizer-cordova/www

COPY make_android.sh /sugarizer-cordova
COPY exclude.android /sugarizer-cordova
COPY etoys_remote.index.html /sugarizer-cordova
COPY gradle-3.3-all.zip /sugarizer-cordova
RUN mkdir /root/.gradle
COPY .gradle/ /root/.gradle

ENV PATH="${PATH}:/opt/android-sdk-linux:/opt/android-sdk-linux/bin:/opt/gradle/gradle-5.2.1/bin"
ENV ANDROID_HOME="/opt/android-sdk-linux"
ENV ANDROID_SDK_ROOT="/opt/android-sdk-linux"

WORKDIR /sugarizer-cordova

ENTRYPOINT ["/bin/bash", "/sugarizer-cordova/make_android.sh"]
