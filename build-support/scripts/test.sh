#!/bin/bash

function run_cmd() {
  echo "Running command: ${cmd}"
  ${cmd}
  if [ $? != 0 ]; then
    echo "Failed!"
    exit 1
  fi
}

umask 0022

cmd='ant clean package test -Dtest.junit.output.format=xml -Dmvn.profile=hadoop20'
run_cmd

cmd='ant clean package -Dmvn.profile=hadoop23'
run_cmd

