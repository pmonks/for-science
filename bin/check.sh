#!/usr/bin/env bash

echo "ℹ️  Checking dependencies..."
clj -M:outdated

echo "ℹ️  Compiling code..."
clj -M:check

echo "ℹ️  Linting (clj-kondo)..."
clj -M:kondo

echo "ℹ️  Linting (eastwood)..."
clj -M:eastwood
