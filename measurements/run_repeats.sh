#!/bin/zsh
cd /Users/suyash/CareCampanion
export JAVA_HOME=$HOME/.carecompanion-toolchain/jdk
export ANDROID_HOME=$HOME/Android/sdk
M=measurements/raw
echo "run,wall_s,gradle_rc,tests,failures" > $M/test_repeats.csv
for i in 2 3; do
  S=$(date +%s)
  ./gradlew :app:testDebugUnitTest --rerun-tasks --console=plain > $M/testrun$i.log 2>&1
  RC=$?; E=$(date +%s)
  T=$(grep -ho 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml | grep -o '[0-9]*' | awk '{s+=$1} END{print s}')
  F=$(grep -ho 'failures="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml | grep -o '[0-9]*' | awk '{s+=$1} END{print s}')
  echo "$i,$((E-S)),$RC,$T,$F" >> $M/test_repeats.csv
  cp -r app/build/test-results/testDebugUnitTest $M/junit-run$i
done
# dated debug build for the Task 20 prototype record
S=$(date +%s)
./gradlew :app:assembleDebug --console=plain > $M/builddebug.log 2>&1
RC=$?; E=$(date +%s)
echo "assembleDebug,$((E-S)),$RC" >> $M/build_times.csv
ls -l app/build/outputs/apk/debug/ >> $M/build_times.csv
cat $M/test_repeats.csv; cat $M/build_times.csv
