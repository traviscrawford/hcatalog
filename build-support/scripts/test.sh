#!/bin/bash

function run_cmd() {
  echo "Running command: ${cmd}"
  ${cmd}
  if [ $? != 0 ]; then
    echo "Failed!"
    exit 1
  fi
}

cmd='ant clean package test -Dmvn.profile=hadoop20'
run_cmd

cmd='ant clean package -Dmvn.profile=hadoop23'
run_cmd

