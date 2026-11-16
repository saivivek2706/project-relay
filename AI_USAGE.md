# AI usage disclosure

AI assistance was used during implementation and review of Project Relay. The candidate remains responsible for understanding and defending every design and code change.

## Tools used

- ChatGPT (GPT-6) for requirement decomposition, static code review, implementation suggestions, and documentation drafting.
- GitHub integration to inspect repository files and apply focused commits on the dedicated `fix/project-relay-compliance` branch.

## Representative prompts

- "Check this code. Have we implemented all as per the requirement given?"
- "Can you fix all the issues?"
- Review the Project Relay assignment's acceptance criteria and compare them against the repository.
- Correct bounded chain search, deterministic tie-breaking, idempotent REST behavior, Redis-backed inbound lookup, Kafka DLQ handling, seeded replay, parity tests, and documentation.

## Issues found during review and corrections made

- The detector's constructor did not match the tests, and the detector did not publish the bounded-search truncation metric. A MeterRegistry-aware constructor, bounded expansion tracking, a counter, and masked truncation logging were added.
- Hop retention above 1.0000 was not rejected, and the minimum amount was being applied to intermediate hops. The detector now enforces the inclusive upper bound and applies the minimum amount to the first transfer only.
- Duplicate transfer requests were always returned as HTTP 201. The response now uses HTTP 200 for an existing transfer ID and preserves its original decision.
- The transfer service queried SQL directly instead of using the configured inbound edge store. It now routes detection through the selected store and updates that store after accepting a transfer.
- The Kafka consumer could acknowledge before the DLQ send completed and could log validation details containing sensitive values. It now waits for the DLQ send and logs a sanitized message.
- The generator's labels output was not valid JSON and its decoy set did not match the required two-hop pass-through. The generator was revised to write valid labels and include the required decoys and a busy hub.
- The plain Spring test suite did not consistently activate the H2 test profile. Test classes were updated to activate it; the Kafka test explicitly enables its embedded listener.
- The load script did not reliably prime the hub before measuring it and could include stale latency samples. It was revised to prime the hub first, enforce the minimum request/concurrency counts, and calculate percentiles from a fresh sample file.

## Verification status

GitHub Actions executed the automated checks on commit `a20946531ae187bb696582b45907b1a7a670bb69`:
- `mvn test`: 22 tests passed.
- `npm test` and `npm run build`: passed.
- Seeded replay: TP=10, FP=0, FN=0; precision=100.00%, recall=100.00% on the 5,303-transfer fixed-seed synthetic dataset.
- The Redis integration test was skipped in that run because the runner had no Docker socket. The test and CI job were then changed to use a separately installed Redis service; the latest service-backed integration run is still queued and must be checked before merge.

The latest branch adds the external-Redis integration harness and documentation updates; the current CI run is the final gate for those additions. The load test has not yet been executed, so no p50/p95/p99 result or p95-target claim is made. The required investigator-view screenshot also still needs to be captured from a running UI.

## Candidate review checklist

Before submission, the candidate should:
1. Run `mvn test` and fix any failures.
2. Run `npm ci && npm test && npm run build` in `frontend/`.
3. Run Docker Compose and verify PostgreSQL, Kafka and Redis connectivity.
4. Run the Redis load test and record p50/p95/p99 plus machine details.
5. Capture the required 4+ hop chain-view screenshot.
6. Review each changed file and be prepared to explain the detector, store selection, event ordering assumption, idempotency, and audit state machine without AI assistance.
