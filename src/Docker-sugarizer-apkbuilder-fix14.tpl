# TODO: Mix this file with the initial tpl file (failed to update)
FROM llaske/sugarizer-apkbuilder:oldest
WORKDIR /

# Clean unused files
WORKDIR  /sugarizer-cordova/
RUN rm gradle-6.5-all.zip 
WORKDIR /opt/gradle
RUN rm -rf gradle-6.8.3

# Install node
RUN apt-get remove -y nodejs
RUN curl -sL https://deb.nodesource.com/setup_16.x | sudo -E bash -
RUN apt-get install -y nodejs

# Install Cordova 12
WORKDIR /
RUN sudo npm install -g cordova@12.0.0 grunt-cli

# Fix make_android
COPY make_android.sh /sugarizer-cordova

# Install Java 17
RUN apt install -y openjdk-17-jdk openjdk-17-jre
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64/"

# Install Gradle 8.7
WORKDIR /opt/gradle
RUN wget https://services.gradle.org/distributions/gradle-8.7-bin.zip
RUN unzip gradle-8.7-bin.zip
RUN rm gradle-8.7-bin.zip
ENV PATH="${PATH}:/opt/gradle/gradle-8.7/bin"

# Download Android SDK 34
WORKDIR /opt/android-sdk-linux/build-tools/
RUN wget https://dl.google.com/android/repository/build-tools_r34-linux.zip
RUN unzip build-tools_r34-linux.zip
RUN mv android-14 34.0.0
RUN rm build-tools_r34-linux.zip

WORKDIR /sugarizer-cordova

ENTRYPOINT ["/bin/bash", "/sugarizer-cordova/make_android.sh"]
