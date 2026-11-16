package com.azentio.projectrelay;

import com.azentio.projectrelay.config.LayeringProperties;
import com.azentio.projectrelay.domain.Transfer;
import com.azentio.projectrelay.service.ChainDetector;
import com.azentio.projectrelay.service.InMemoryInboundEdgeStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TruncationTest {
    @Test
    void capStopsEvaluationRetainsBestChainAndOtherChainsRemainDetectable() {
        var cfg = new LayeringProperties(3, 6, Duration.ofMinutes(30), Duration.ofHours(2),
                new BigDecimal("100000.00"), new BigDecimal("0.90"),
                new BigDecimal("0.75"), "memory", 50);
        var registry = new SimpleMeterRegistry();
        var detector = new ChainDetector(cfg, registry);
        var store = new InMemoryInboundEdgeStore(cfg);
        OffsetDateTime base = OffsetDateTime.parse("2026-01-01T09:00:00Z");

        // More than 200 inbound edges in one window force the expansion cap.
        for (int i = 0; i < 300; i++) {
            store.add(new Transfer("H" + i, String.format("%010d", 4_000_000_000L + i),
                    "4999999999", new BigDecimal("200000.00"), base.plusMinutes(i % 100)));
        }
        store.add(new Transfer("HUB-CHAIN-1", "1000000011", "1000000012",
                new BigDecimal("200000.00"), base.plusMinutes(90)));
        store.add(new Transfer("HUB-CHAIN-2", "1000000012", "4999999999",
                new BigDecimal("194000.00"), base.plusMinutes(100)));
        var hubOut = new Transfer("HUB-OUT", "4999999999", "4999999998",
                new BigDecimal("190000.00"), base.plusMinutes(101));

        var best = detector.findBest(hubOut, account -> store.inbound(account,
                hubOut.occurredAt, hubOut.occurredAt.minus(cfg.window())));
        assertTrue(best.isPresent(), "best chain found before truncation must be retained");
        assertEquals(List.of("HUB-CHAIN-1", "HUB-CHAIN-2", "HUB-OUT"),
                best.get().transfers().stream().map(t -> t.transferId).toList());
        assertTrue(registry.counter("layering.search.truncated").count() >= 1);

        store.add(new Transfer("P1", "1000000001", "1000000002",
                new BigDecimal("200000.00"), base));
        store.add(new Transfer("P2", "1000000002", "1000000003",
                new BigDecimal("194000.00"), base.plusMinutes(5)));
        var p3 = new Transfer("P3", "1000000003", "1000000004",
                new BigDecimal("188000.00"), base.plusMinutes(10));
        var other = detector.findBest(p3, account -> store.inbound(account,
                p3.occurredAt, p3.occurredAt.minus(cfg.window())));
        assertTrue(other.isPresent());
        assertEquals(3, other.get().transfers().size());
    }
}
