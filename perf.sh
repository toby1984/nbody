#!/bin/bash

perf stat -e branch-instructions -e branch-misses -e cache-misses -e cache-references -e cpu-cycles -e instructions -e ref-cycles -e stalled-cycles-backend -e stalled-cycles-frontend -e cpu-clock -e cpu-migrations -e L1-dcache-load-misses -e L1-dcache-loads -e L1-dcache-prefetch-misses -e L1-dcache-store-misses -e L1-dcache-stores -e L1-icache-load-misses -e LLC-loads -e LLC-prefetches -e LLC-stores -e branch-load-misses -e branch-loads -e dTLB-load-misses -e dTLB-loads -e dTLB-store-misses -e dTLB-stores -e iTLB-load-misses -e iTLB-loads "$@"
