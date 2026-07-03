#!/usr/bin/env sh
# Compile and launch the UNO CLI, forwarding any options (e.g. --human --bots 2).
set -eu
mvn -q compile exec:java -Dexec.mainClass="uno.Main" -Dexec.args="$*"
