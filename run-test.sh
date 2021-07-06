#!/bin/bash
cd "$(dirname "$0")" || exit
./gradlew swtia:clean swtia:test || exit

# create bin folder
mkdir bin
cd bin || exit

# copy the file
cp -r ../swtia/build/reports ./ || exit
tar zcf test-reports.tar.gz reports || exit
rm -rf reports