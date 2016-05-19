#!/bin/bash
set -e

PROJ=$(dirname `realpath $0`)
REFERENCE_FILE=$PROJ/target/torrent-1.0-SNAPSHOT-jar-with-dependencies.jar
FILE_SUFFIX=downloads/`basename "$REFERENCE_FILE"`
CLIENTS=5

TMP=$(mktemp -d)
echo Working in $TMP

if [ x"$JAVA_HOME" == x"" ]; then
  JAVA=$JAVA_HOME/bin/java
else
  JAVA=`which java`
fi

cd "$PROJ"
mvn assembly:assembly 

JAR=$PROJ/target/torrent-1.0-SNAPSHOT-jar-with-dependencies.jar
SERVER="\"$JAVA\" -Dtorrent.part_size=102400 -Dtorrent.update_interval=100 -Dtorrent.retry_delay=100 -cp \"$JAR\" ru.spbau.mit.TorrentTrackerMain"
CLIENT="\"$JAVA\" -Dtorrent.part_size=102400 -Dtorrent.update_interval=100 -Dtorrent.retry_delay=100 -cp \"$JAR\" ru.spbau.mit.TorrentClientMain"

echo -e "\e[1m========== STARTING SERVER ==========\e[0m"
mkdir "$TMP/server"
cd "$TMP/server"
eval "$SERVER &"
SERVPID=$!

for ((i=1; $i <= $CLIENTS; i++)); do
  echo -e "\e[1m========== CONFIGURING CLIENT $i ==========\e[0m"
  mkdir "$TMP/client$i"
  cd "$TMP/client$i"
  if [ $i == 1 ]; then 
    eval $CLIENT newfile 127.0.0.1 "$REFERENCE_FILE"
  else
    eval $CLIENT get 127.0.0.1 1
  fi
done
for ((i=1; $i <= $CLIENTS; i++)); do
  echo -e "\e[1m========== STARTING CLIENT $i ==========\e[0m"
  cd "$TMP/client$i"
  eval "$CLIENT run 127.0.0.1 &"
  PIDS="$PIDS $!"
done

echo -e "\e[1m========== WORKING ==========\e[0m"
for ((step=1; $step <= 5; step++)); do
  sleep 2
  echo -e "\e[33;1m========== RESTARTING SERVER ==========\e[0m"
  kill $SERVPID >/dev/null 2>&1 || true
  kill -9 $SERVPID >/dev/null 2>&1 || true
  cd "$TMP/server"
  eval "$SERVER &"
  SERVPID=$!
done

echo -e "\e[1m========== KILLING ALL ==========\e[0m"
kill $SERVPID $PIDS >/dev/null 2>&1 || true
kill -9 $SERVPID $PIDS >/dev/null 2>&1 || true

fail=0
for ((i=1; $i <= $CLIENTS; i++)); do
  if ! diff "$TMP/client$i/$FILE_SUFFIX" "$REFERENCE_FILE" >/dev/null 2>&1; then
    echo -e "\e[31;1mFAIL\e[0m Bad file content for client $i"
    fail=1
  fi
done

if [ $fail == 0 ]; then
  echo -e "\e[32;1mSUCCESS\e[0m"
fi
rm -rf $TMP || true
