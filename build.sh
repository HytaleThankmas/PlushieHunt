#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MVN="${MVN:-$HOME/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn}"
if [[ ! -x "$MVN" ]]; then MVN=mvn; fi
"$MVN" -f "$SCRIPT_DIR/pom.xml" clean package "$@"
