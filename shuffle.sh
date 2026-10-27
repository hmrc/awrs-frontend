#!/bin/bash -e

for i in {1..1000}
do
    printf "\n test run $i"
  sbt testShuffle
done