#!/bin/bash
cd "$(dirname "$0")" || exit
cd vscode-ext || exit

# replace IDE server
echo 'start replacing IDE server (assume the IDE server is already built)'
rm -rf lsp-server
unzip ../bin/ia-ide-server*.zip -d ./ || exit
mv ia-ide-server* lsp-server

# compile vscode
echo 'start compiling extension using npm'
(npm install && npm run package) || exit
mv *.vsix ../bin/

# finish
cd ../bin || exit
echo 'finish building vscode extension, bin:'
ls -lh
cd ..

# optional: install if available
code --install-extension bin/*.vsix || true