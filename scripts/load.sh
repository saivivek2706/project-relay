#!/usr/bin/env bash
# Usage: BASE_URL=http://localhost:8080 ./scripts/load.sh [count] [concurrency]
# Requires: service running with LAYERING_STORE=redis, generator output seeded.jsonl
set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"; COUNT="${1:-10000}"; CONC="${2:-8}"
python3 - <<'PY' > /tmp/load_input.jsonl
import random,json,datetime
rnd=random.Random(7); base=datetime.datetime(2026,1,1,9,tzinfo=datetime.timezone.utc)
# hub account: 300 inbound within one window (small amounts so it stresses search, not detection)
for i in range(300):
    print(json.dumps({"transferId":f"HUB-{i}","fromAccount":f"{4000000000+i:010d}","toAccount":"4999999999","amount":1000.00,"occurredAt":(base+datetime.timedelta(minutes=i%100)).isoformat().replace("+00:00","Z")}))
for i in range(int(__import__('os').environ.get('COUNT','10000'))-300):
    print(json.dumps({"transferId":f"LOAD-{i}","fromAccount":f"{5000000000+i:010d}","toAccount":f"{5000000001+i:010d}","amount":round(1000+rnd.random()*50000,2),"occurredAt":(base+datetime.timedelta(minutes=rnd.randrange(10000))).isoformat().replace("+00:00","Z")}))
PY
export COUNT
# 8 concurrent clients via xargs -P
cat /tmp/load_input.jsonl | xargs -P "$CONC" -I{} sh -c 's=$(date +%s%N); curl -s -o /dev/null -w "%{time_total}\n" -X POST "$0/api/transfers" -H "Content-Type: application/json" -d "$1" >> /tmp/load_lat.txt' "$BASE" {}
python3 - <<'PY'
import statistics
vals=sorted(float(x) for x in open('/tmp/load_lat.txt') if x.strip())
def p(q): return vals[min(len(vals)-1,int(q*len(vals)))]*1000
print(f"n={len(vals)} p50={p(0.50):.1f}ms p95={p(0.95):.1f}ms p99={p(0.99):.1f}ms")
PY
