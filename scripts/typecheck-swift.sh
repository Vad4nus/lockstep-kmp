#!/bin/sh
set -eu
framework_dir=$1
file=$2
target=${3:-17.0}
arch=${4:-$(uname -m)}
exec xcrun swiftc -typecheck -F "$framework_dir" \
  -target "${arch}-apple-ios${target}-simulator" \
  -sdk "$(xcrun --sdk iphonesimulator --show-sdk-path)" "$file"
