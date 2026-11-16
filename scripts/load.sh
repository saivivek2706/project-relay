#!/usr/bin/env bash
# Usage: BASE_URL=http://localhost:8080 ./scripts/load.sh [count>=10000] [concurrency>=8]
# Start the service with LAYERING_STORE=redis before running this script.
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
COUNT="${1:-10000}"
CONC="${2:-8}"
if (( COUNT < 10000 )); then echo "count must be at least 10000" >&2; exit 2; fi
if (( CONC < 8 )); then echo "concurrency must be at least 8" >&2; exit 2; fi

INPUT="/tmp/project-relay-load.jsonl"
LATENCIES="/tmp/project-relay-load-latencies.txt"
RUN_ID="$(date +%s)"
: > "$LATENCIES"

python3 - "$COUNT" "$RUN_ID" > "$INPUT" <<'PY'
import datetime, json, random, sys
count = int(sys.argv[1])
run_id = sys.argv[2]
rnd = random.Random(7)
base = datetime.datetime(2026, 1, 1, 9, tzinfo=datetime.timezone.utc)

def emit(obj):
    print(json.dumps(obj, separators=(",", ":")))

# Prime the Redis sorted set with 300 inbound edges before evaluating the hub.
for i in range(300):
    emit({
        "transferId": f"HUB-IN-{run_id}-{i}",
        "fromAccount": f"{4_000_000_000+i:010d}",
        "toAccount": "4999999999",
        "amount": "1000.00",
        "occurredAt": (base + datetime.timedelta(minutes=i % 100)).isoformat().replace("+00:00", "Z")
    })

# This evaluation sees all 300 inbound edges for the busy account.
emit({
    "transferId": f"HUB-OUT-{run_id}",
    "fromAccount": "4999999999",
    "toAccount": "4999999998",
    "amount": "970.00",
    "occurredAt": (base + datetime.timedelta(minutes=101)).isoformat().replace("+00:00", "Z")
})

for i in range(count - 301):
    account = 5_000_000_000 + i * 2
    emit({
        "transferId": f"LOAD-{run_id}-{i}",
        "fromAccount": f"{account:010d}",
        "toAccount": f"{account+1:010d}",
        "amount": f"{1000+rnd.random()*49000:.2f}",
        "occurredAt": (base + datetime.timedelta(minutes=rnd.randrange(10000))).isoformat().replace("+00:00", "Z")
    })
PY

post_one() {
  local payload="$1"
  curl -fsS -o /dev/null -w "%{time_total}\n" \
    -X POST "$BASE/api/transfers" \
    -H "Content-Type: application/json" -d "$payload"
}

# Send the first 301 records in order so the hub is populated before its outgoing edge.
while IFS= read -r payload; do post_one "$payload" >> "$LATENCIES"; done < <(head -n 301 "$INPUT")

# Replay the remaining records with the requested concurrency.
tail -n +302 "$INPUT" | xargs -P "$CONC" -I{} sh -c '
  curl -fsS -o /dev/null -w "%{time_total}\n" \
    -X POST "$0/api/transfers" -H "Content-Type: application/json" -d "$1"
' "$BASE" "{}" >> "$LATENCIES"

python3 - "$LATENCIES" <<'PY'
import math, platform, sys
values = sorted(float(line.strip()) for line in open(sys.argv[1]) if line.strip())
def percentile(q):
    return values[max(0, math.ceil(q * len(values)) - 1)] * 1000
print(f"requests={len(values)} p50={percentile(.50):.2f}ms p95={percentile(.95):.2f}ms p99={percentile(.99):.2f}ms")
print(f"machine={platform.platform()} cpu_count={__import__('os').cpu_count()}")
PY
