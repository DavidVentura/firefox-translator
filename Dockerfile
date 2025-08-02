FROM openjdk:17-jdk-slim

# Set environment variables
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_NDK_VERSION=27.0.12077973
ENV ANDROID_HOME=$ANDROID_SDK_ROOT
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Install system dependencies
RUN apt-get update && \
    apt-get install -y \
        make \
        g++ \
        libc-dev \
        wget \
        unzip \
        git \
        cmake \
        ninja-build && \
    rm -rf /var/lib/apt/lists/*

# Create Android SDK directory
RUN mkdir -p $ANDROID_SDK_ROOT

# Download and install Android command line tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdtools.zip && \
    unzip cmdtools.zip -d $ANDROID_SDK_ROOT && \
    mv $ANDROID_SDK_ROOT/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools-temp && \
    mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    mv $ANDROID_SDK_ROOT/cmdline-tools-temp/* $ANDROID_SDK_ROOT/cmdline-tools/latest/ && \
    rm -rf $ANDROID_SDK_ROOT/cmdline-tools-temp && \
    rm cmdtools.zip

# Accept licenses and install Android SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" && \
    sdkmanager "ndk;${ANDROID_NDK_VERSION}" && \
    sdkmanager "cmake;3.22.1"

# Set NDK environment variable
ENV ANDROID_NDK_ROOT=$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION

# Set working directory
WORKDIR /build/build/dev.davidv.translator

# some commands run `git rev-parse --short` and may get different
# lengths depending on git version or repo status
RUN git config --system core.abbrev 10

RUN echo "sdk.dir=${ANDROID_SDK_ROOT}" > local.properties
# Default command to build the project
CMD ["./gradlew", "assembleRelease"]
