# check
HAS_MCRL2=false
HAS_Z3=false
if pbessolve --version ; then
  echo "[x] pbessolve (mCRL2) is installed"
  HAS_MCRL2=true
else
  echo "[ ] pbessolve (mCRL2) is not installed"
fi

if z3 --version ; then
  echo "[x] z3 is installed"
  HAS_Z3=true
else
  echo "[ ] z3 is not installed"
fi

if "$HAS_MCRL2" = true && "$HAS_Z3" = true ; then
  echo "mCRL2 and z3 are installed, stop."
  exit 0
fi

# make sure Ubuntu
if [ ! -f /etc/lsb-release ]; then
  echo "Support Ubuntu only"
  exit 1
fi

# install mCRL2
if [ "$HAS_MCRL2" = false ]; then
  echo "Start installing mCRL2"
  (sudo apt update && \
  sudo apt install cmake -y && \
  sudo apt install libboost-dev -y && \
  sudo apt install libgl1-mesa-dev -y && \
  sudo apt install python-psutil -y && \
  sudo apt install python-yaml -y && \
  sudo apt install qtbase5-dev -y && \
  sudo apt install libqt5opengl5-dev -y && \
  sudo apt install git -y) || exit 1
  (curl -L https://github.com/mCRL2org/mCRL2/releases/download/mcrl2-202106.0/mcrl2-202106.0_x86_64.deb -o mcrl2.deb && sudo dpkg -i mcrl2.deb && rm mcrl2.deb) || exit 1
fi

# install z3
if [ "$HAS_Z3" = false ]; then
  echo "Start installing z3"
  (sudo apt update && sudo apt install z3 -y) || exit 1
fi