#!/usr/bin/env bash
set -Eeuo pipefail

adb shell rm -rf /sdcard/Download/OmniParity
adb shell mkdir -p /sdcard/Download/OmniParity/empty
adb shell mkdir -p /sdcard/Download/OmniParity/duplicates
adb shell mkdir -p /sdcard/Download/OmniParity/large
adb shell mkdir -p /sdcard/DCIM/.thumbnails

adb shell 'printf parity-fixture > /sdcard/Download/OmniParity/duplicates/a.txt'
adb shell 'cp /sdcard/Download/OmniParity/duplicates/a.txt /sdcard/Download/OmniParity/duplicates/b.txt'
adb shell 'printf erutxif-ytirap > /sdcard/Download/OmniParity/duplicates/same-size-different.txt'
adb shell 'dd if=/dev/zero of=/sdcard/Download/OmniParity/large/parity-large.bin bs=1048576 count=12 status=none'
adb shell 'printf thumb-parity > /sdcard/DCIM/.thumbnails/parity-thumb.bin'
adb shell sync
