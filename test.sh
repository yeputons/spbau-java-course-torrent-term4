#!/bin/sh
set -e

PROJ=$(dirname `realpath $0`)
REFERENCE_FILE=$PROJ/target/torrent-1.0-SNAPSHOT-jar-with-dependencies.jar
FILE_SUFFIX=downloads/`basename "$REFERENCE_FILE"`
CLIENTS=5

TMP=$(mktemp -d)
echo Working in $TMP

JAVA=$JAVA_HOME/bin/java

cd "$PROJ"
mvn assembly:assembly 

JAR=$PROJ/target/torrent-1.0-SNAPSHOT-jar-with-dependencies.jar
SERVER="\"$JAVA\" -Dtorrent.part_size=102400 -cp \"$JAR\" ru.spbau.mit.TorrentTrackerMain"
CLIENT="\"$JAVA\" -Dtorrent.part_size=102400 -cp \"$JAR\" ru.spbau.mit.TorrentClientMain"

mkdir "$TMP/server"
cd "$TMP/server"
eval "$SERVER &"
PIDS=$!

for ((i=1; $i <= $CLIENTS; i++)); do
  mkdir "$TMP/client$i"
  cd "$TMP/client$i"
  if [ $i == 1 ]; then 
    eval $CLIENT newfile 127.0.0.1 "$REFERENCE_FILE"
  else
    eval $CLIENT get 127.0.0.1 1
  fi
  eval "$CLIENT run 127.0.0.1 &"
  PIDS="$PIDS $!"
done

sleep 10

kill $PIDS >/dev/null 2>&1 || true
kill -9 $PIDS >/dev/null 2>&1 || true

fail=0
for ((i=1; $i <= $CLIENTS; i++)); do
  if ! diff "$TMP/client2/$FILE_SUFFIX" "$REFERENCE_FILE" >/dev/null 2>&1; then
    echo -e "\e[31;1mFAIL\e[0m Bad file content for client $i"
    fail=1
  fi
done
if [ $fail == 0 ]; then
  echo -e "\e[32;1mSUCCESS\e[0m"
fi

rm -rf $TMP || true
