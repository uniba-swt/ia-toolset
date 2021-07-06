#!/bin/bash
cd "$(dirname "$0")" || exit
./gradlew clean assemble || exit

# create bin folder
mkdir bin
cd bin || exit

# copy the file
cp ../swtia/build/distributions/*.zip ./ || exit
cp ../swtia.ide/build/distributions/*.zip ./ || exit
echo 'finish building compiler and IDE server, bin:'
ls -lh .

# finish
cd ..