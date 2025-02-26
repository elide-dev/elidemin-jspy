#!/bin/bash

WARMUPS="10"
RUNS="25"

TEST_JS="test.js"
TEST_PY="test.py"

echo "----- speed ------------------------------------------------------------"

hyperfine --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
	-n 'help' "./build/native/nativeOptimizedCompile/labs --help" \
	-n 'js' "./build/native/nativeOptimizedCompile/labs js $TEST_JS" \
	-n 'py' "./build/native/nativeOptimizedCompile/labs py $TEST_PY";

hyperfine --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
	-n 'elide' "./build/native/nativeOptimizedCompile/labs js $TEST_JS" \
	-n 'bun' "bun run $TEST_JS" \
	-n 'node' "node $TEST_JS" \
	-n 'deno' "deno $TEST_JS";

hyperfine --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
	-n 'elide' "./build/native/nativeOptimizedCompile/labs py $TEST_PY" \
	-n 'cpython' "python3 $TEST_PY" \
	-n 'pypy' "pypy3 $TEST_PY";


echo "----- size -------------------------------------------------------------"

cd ./build/native/nativeOptimizedCompile;
du -hsL `which node` `which bun` `which deno` `which python3` `which pypy3`
echo "----"
du -hsLc resources labs;
du -h /tmp/elide-auxcache.bin || echo "No aux cache present.";
cd -;
