#!/bin/bash

resources=src/test/resources
for f in ${resources}/*.template; do cat ${f} | sed "s|{{PWD}}|${PWD}|" > ${resources}/$(basename ${f} .template); done
