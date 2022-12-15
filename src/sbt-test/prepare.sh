#!/bin/bash

for submodule_path in $(git submodule status | cut -c2- | cut -d\  -f2); do echo "gitdir: ${PWD}/.git/modules/${submodule_path}" > ${submodule_path}/.git; done
