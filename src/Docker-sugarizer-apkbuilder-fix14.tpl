# TODO: Mix this file with the initial tpl file (failed to update)
FROM llaske/sugarizer-apkbuilder:oldest
WORKDIR /

# Clean unused files
WORKDIR  /sugarizer-cordova/
RUN rm gradle-6.5-all.zip 
WORKDIR /opt/gradle
RUN rm -rf gradle-6.8.3

# Install Cordova 12
WORKDIR /
RUN sudo npm install -g cordova@12.0.0 grunt-cli

# Fix make_android
COPY make_android.sh /sugarizer-cordova

# Install Gradle 7.1.1
WORKDIR /opt/gradle
RUN wget https://services.gradle.org/distributions/gradle-7.1.1-bin.zip
RUN unzip gradle-7.1.1-bin.zip
RUN rm gradle-7.1.1-bin.zip
ENV PATH="${PATH}:/opt/gradle/gradle-7.1.1/bin"

# Download Android SDK 33
WORKDIR /opt/android-sdk-linux/build-tools/
RUN wget https://dl.google.com/android/repository/build-tools_r33.0.2-linux.zip
RUN unzip build-tools_r33.0.2-linux.zip
RUN mv android-13 33.0.2
RUN rm build-tools_r33.0.2-linux.zip

WORKDIR /sugarizer-cordova

ENTRYPOINT ["/bin/bash", "/sugarizer-cordova/make_android.sh"]
