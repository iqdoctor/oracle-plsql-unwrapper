#!/bin/sh
set -eu
if [ -n "${JAVA_HOME:-}" ]; then PATH="$JAVA_HOME/bin:$PATH"; export PATH; fi
ROOT=$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
./mvnw -q -DskipTests package
mkdir -p tool/backend/build
javac -source 8 -target 8 -cp "oracle-plsql-unwrap-spi/target/classes:oracle-plsql-unwrap-v2/target/classes" -d tool/backend/build tool/backend/src/ToolBridge.java
