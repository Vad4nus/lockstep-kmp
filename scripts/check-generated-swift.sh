#!/bin/sh
# Compares the Swift the plugin generates for the sample with the files kept in sample/swift-check/expected.
# Build the framework first; pass --update to accept what the plugin generates now.
set -eu
case $(uname -m) in
  arm64) target=iosSimulatorArm64 ;;
  *) target=iosX64 ;;
esac
generated=sample/shared/build/skie/binaries/debugFramework/DEBUG/$target/swift/generated/Lockstep
expected=sample/swift-check/expected
if [ ! -d "$generated" ]; then
  echo "No generated Swift in $generated: run ./gradlew :sample:shared:linkDebugFramework$(echo "$target" | cut -c1 | tr '[:lower:]' '[:upper:]')$(echo "$target" | cut -c2-)" >&2
  exit 1
fi
if [ "${1:-}" = "--update" ]; then
  rm -rf "$expected"
  mkdir -p "$expected"
  cp "$generated"/*.swift "$expected"/
  exit 0
fi
if ! diff -ru "$expected" "$generated"; then
  echo "The generated Swift differs from $expected. Review the diff, then run: scripts/check-generated-swift.sh --update" >&2
  exit 1
fi
