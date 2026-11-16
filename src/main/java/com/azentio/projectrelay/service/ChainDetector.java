package com.azentio.projectrelay.service;

import com.azentio.projectrelay.config.LayeringProperties;
import com.azentio.projectrelay.domain.Transfer;
import com.azentio.projectrelay.util.Masking;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

@Service
public class ChainDetector {
    private static final Logger log = LoggerFactory.getLogger(ChainDetector.class);
    private static final BigDecimal MAX_HOP_RETENTION = new BigDecimal("1.0000");

    private final LayeringProperties cfg;
    private final MeterRegistry meters;

    public ChainDetector(LayeringProperties cfg) {
        this(cfg, null);
    }

    public ChainDetector(LayeringProperties cfg, MeterRegistry meters) {
        this.cfg = cfg;
        this.meters = meters;
    }

    public record Chain(List<Transfer> transfers, BigDecimal e2e) { }

    private static final class SearchState {
        int expansions;
        boolean truncated;
        String account;
    }

    public Optional<Chain> findBest(Transfer last, Function<String, List<Transfer>> inbound) {
        List<Chain> found = new ArrayList<>();
        Deque<Transfer> path = new ArrayDeque<>();
        path.addLast(last);

        Set<String> visited = new HashSet<>();
        visited.add(last.fromAccount);
        visited.add(last.toAccount);

        SearchState state = new SearchState();
        dfs(last, path, visited, found, inbound, state);

        if (state.truncated) {
            if (meters != null) meters.counter("layering.search.truncated").increment();
            log.warn("Layering search truncated at account {}", Masking.mask(state.account));
        }

        return found.stream().max(
                Comparator.comparingInt((Chain c) -> c.transfers().size())
                        .thenComparing(c -> c.transfers().get(0).occurredAt,
                                Comparator.reverseOrder())
                        .thenComparing(c -> c.transfers().get(0).transferId,
                                Comparator.reverseOrder()));
    }

    private void dfs(Transfer current,
                     Deque<Transfer> path,
                     Set<String> visited,
                     List<Chain> found,
                     Function<String, List<Transfer>> inbound,
                     SearchState state) {
        if (state.truncated) return;

        if (path.size() >= cfg.minHops()) {
            validate(path).ifPresent(found::add);
        }
        if (path.size() >= cfg.maxHops()) return;

        List<Transfer> candidates = inbound.apply(current.fromAccount);
        for (Transfer previous : candidates) {
            if (state.expansions >= cfg.maxExpansions()) {
                state.truncated = true;
                state.account = current.fromAccount;
                return;
            }
            state.expansions++;

            if (!previous.occurredAt.isBefore(current.occurredAt)) continue;
            Duration gap = Duration.between(previous.occurredAt, current.occurredAt);
            if (gap.compareTo(cfg.maxHopGap()) > 0) continue;

            BigDecimal retention = current.amount.divide(previous.amount, 4, RoundingMode.HALF_UP);
            if (retention.compareTo(cfg.minHopRetention()) < 0
                    || retention.compareTo(MAX_HOP_RETENTION) > 0) continue;

            // previous.fromAccount becomes the next node to the left; it must be new.
            if (visited.contains(previous.fromAccount)) continue;

            Transfer chainEnd = path.peekLast();
            if (Duration.between(previous.occurredAt, chainEnd.occurredAt)
                    .compareTo(cfg.window()) > 0) continue;

            path.addFirst(previous);
            visited.add(previous.fromAccount);
            dfs(previous, path, visited, found, inbound, state);
            visited.remove(previous.fromAccount);
            path.removeFirst();

            if (state.truncated) return;
        }
    }

    private Optional<Chain> validate(Deque<Transfer> path) {
        List<Transfer> chain = new ArrayList<>(path);
        Transfer first = chain.get(0);
        Transfer last = chain.get(chain.size() - 1);

        // min-amount applies to t1 only, not to every intermediate transfer.
        if (first.amount.compareTo(cfg.minAmount()) < 0) return Optional.empty();

        if (Duration.between(first.occurredAt, last.occurredAt).compareTo(cfg.window()) > 0) {
            return Optional.empty();
        }

        Set<String> accounts = new HashSet<>();
        for (Transfer transfer : chain) {
            if (!accounts.add(transfer.fromAccount)) return Optional.empty();
        }
        if (!accounts.add(last.toAccount)) return Optional.empty();

        BigDecimal endToEnd = last.amount.divide(first.amount, 4, RoundingMode.HALF_UP);
        if (endToEnd.compareTo(cfg.minEndToEndRetention()) < 0) return Optional.empty();

        return Optional.of(new Chain(List.copyOf(chain), endToEnd));
    }
}
