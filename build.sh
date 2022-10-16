#!/bin/bash

MVN=$(which mvn)

function cleanup {
  if [[ ! -z $MVN ]]; then
    rm -f *.jar
    rm -rf one
  fi  
}

trap cleanup EXIT

javac --add-modules jdk.internal.vm.ci \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED \
      --add-exports jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED \
      --add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
      --source 11 --target 11 \
      -d . src/one/nalim/*.java

jar cfm nalim-1.0.jar MANIFEST.MF one
jar cf nalim-1.0-sources.jar -C src one

if [[ ! -z $MVN ]]; then
  cmd="mvn install:install-file -DgroupId=com.nc -DartifactId=nalim -Dversion=1.0 -DgeneratePom=true -Dpackaging=jar -Dfile=nalim-1.0.jar -Dsources=nalim-1.0-sources.jar"
  echo "Install Cmd: $cmd"
  eval $cmd
fi  
