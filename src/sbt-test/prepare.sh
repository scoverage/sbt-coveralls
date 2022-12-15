#!/bin/bash

for submodule_path in $(git submodule status | cut -d\  -f3); do echo "gitdir: ${PWD}/.git/modules/${submodule_path}" > ${submodule_path}/.git; done
