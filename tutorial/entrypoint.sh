#! /bin/bash

wget -qO- https://github.com/McShelby/hugo-theme-relearn/archive/main.zip | unzip -d themes -
hugo server --bind=0.0.0.0 --port=80 --poll=700