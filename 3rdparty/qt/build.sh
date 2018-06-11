#!/bin/bash

_DIR="$(realpath $(dirname $0))"

_BUILD_DIR="$_DIR/build-$ANDROID_ARCH"
mkdir -p "$_BUILD_DIR" || exit 1
cd "$_BUILD_DIR" || exit 1

_OPENSSL_LIBDIR="$(realpath ../../openssl/install-$ANDROID_ARCH/lib)"
_OPENSSL_INCDIR="$(realpath ../../openssl/install-$ANDROID_ARCH/include)"

OPENSSL_LIBS="-L$_OPENSSL_LIBDIR -lssl -lcrypto" ../qtbase/configure \
    -v \
    -confirm-license \
    -opensource \
    -prefix "$(realpath ../install-$ANDROID_ARCH)" \
    -xplatform android-g++ \
    -nomake tests \
    -nomake examples \
    -android-ndk "$ANDROID_NDK_ROOT" \
    -android-sdk "$ANDROID_SDK_ROOT" \
    -android-ndk-host linux-x86_64 \
    -android-toolchain-version 4.9 \
    -android-arch "$ANDROID_ARCH" \
    -android-ndk-platform android-16 \
    -no-dbus \
    -no-gui \
    -no-opengl \
    -no-widgets \
    -no-feature-animation \
    -no-feature-bearermanagement \
    -no-feature-big_codecs \
    -no-feature-codecs \
    -no-feature-commandlineparser \
    -no-feature-datestring \
    -no-feature-datetimeparser \
    -no-feature-dom \
    -no-feature-filesystemwatcher \
    -no-feature-ftp \
    -no-feature-library \
    -no-feature-localserver \
    -no-feature-mimetype \
    -no-feature-networkdiskcache \
    -no-feature-process \
    -no-feature-processenvironment \
    -no-feature-regularexpression \
    -no-feature-sharedmemory \
    -no-feature-statemachine \
    -no-feature-temporaryfile \
    -no-feature-textdate \
    -no-feature-translation \
    -no-feature-xmlstream \
    -openssl-linked \
    -I"$_OPENSSL_INCDIR" || exit 1

make $MAKEOPTS || exit 1
make install $MAKEOPTS || exit 1