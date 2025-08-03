#!/bin/bash

set -e

echo "Building Docker image..."
docker build -t translator-app:latest .

echo "Running build in container..."
docker run --rm \
  -v "$(pwd)":/home/vagrant/build/dev.davidv.translator/ \
  --user "$(id -u):$(id -g)" \
  translator-app:latest \
  ./gradlew assembleAarch64Release

echo "Build completed! APK files are in app/build/outputs/apk/"
