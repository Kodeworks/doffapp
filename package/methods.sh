#!/usr/bin/env bash

GIT_ROOT=$(git rev-parse --show-toplevel)

build_all() {
    cd $GIT_ROOT
    mvn -q -T 4 clean install -P=$1
    cd -
}