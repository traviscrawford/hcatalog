#!/bin/bash

jps | egrep "(NameNode|DataNode|SecondaryNameNode|JobTracker|TaskTracker|HiveMetaStore|HQuorumPeer|RunJar|HMaster)" | awk '{ print $1 }' | xargs kill

