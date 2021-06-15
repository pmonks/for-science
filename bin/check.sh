#!/usr/bin/env bash

echo "ℹ️  Checking dependencies..."
clojure -Srepro -M:outdated

echo "ℹ️  Compiling code..."
clojure -Srepro -M:check

echo "ℹ️  Linting (clj-kondo)..."
clojure -Srepro -M:kondo

echo "ℹ️  Linting (eastwood)..."
clojure -Srepro -M:eastwood
