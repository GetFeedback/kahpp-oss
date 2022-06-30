#!/bin/sh
set -e

if [ -n "${DEBUG+x}" ]; then set -x; fi

cd "$(dirname "$0")" || exit 101 # Can't follow jar directory
if [ -z "$JAR_FILE" ]; then JAR_FILE=$(pwd)/$(basename "$0"); fi

# `exec` will ensure java will become the main process, this way the application
# can control its lifecycle and handle POSIX signals as desired.
# We're not quoting variables here as when they're not set an empty single quotes
# string is rendered, causing `java -jar` to fail due to unknown arguments.
exec "$(type -p java)" -Dsun.misc.URLClassPath.disableJarChecking=true $JAVA_OPTS \
 -jar $JAR_FILE $RUN_ARGS "$@"
