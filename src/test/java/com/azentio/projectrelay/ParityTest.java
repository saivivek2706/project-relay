package com.azentio.projectrelay;

import com.azentio.projectrelay.config.LayeringProperties;
import com.azentio.projectrelay.domain.Transfer;
import com.azentio.projectrelay.dto.TransferRequest;
import com.azentio.projectrelay.generator.Generator;
import com.azentio.projectrelay.repo.TransferRepository;
import com.azentio.projectrelay.service.ChainDetector;
import com.azentio.projectrelay.service.InMemoryInboundEdgeStore;
import com.azentio.projectrelay.service.SqlInboundEdgeStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ParityTest {
    @Autowired LayeringProperties cfg;
    @Autowired SqlInboundEdgeStore sql;
    @Autowired TransferRepository transfers;
    @Autowired ObjectMapper mapper;

    @Test
    void memoryAndSqlLookupProduceExactlyTheSameDetectedChains() throws Exception {
        Path file = Path.of("/tmp/seeded-parity.jsonl");
        Generator.main(new String[]{"42", file.toString(), "/tmp/labels-parity.json"});

        InMemoryInboundEdgeStore memory = new InMemoryInboundEdgeStore(cfg);
        List<Transfer> all = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            TransferRequest request = mapper.readValue(line, TransferRequest.class);
            Transfer transfer = new Transfer(request.transferId(), request.fromAccount(),
                    request.toAccount(), request.amount(), request.occurredAt());
            all.add(transfer);
            memory.add(transfer);
        }
        transfers.saveAll(all);
        transfers.flush();

        ChainDetector detector = new ChainDetector(cfg, new SimpleMeterRegistry());
        Set<List<String>> memoryChains = detectAll(detector, all,
                account -> memory.inbound(account,
                        all.get(all.size() - 1).occurredAt.plusSeconds(1),
                        all.get(0).occurredAt.minus(cfg.window())));

        Set<List<String>> sqlChains = detectAll(detector, all,
                account -> {
                    // The lookup's time bounds are supplied per evaluation below.
                    return sql.inbound(account, evaluationBefore.get(),
                            evaluationBefore.get().minus(cfg.window()));
                });

        assertEquals(memoryChains, sqlChains,
                "memory edge store and Task 1 SQL reference must return identical chain sequences");
        assertTrue(memoryChains.stream().anyMatch(chain -> chain.get(0).startsWith("PLANTED-")));
    }

    private final ThreadLocal<java.time.OffsetDateTime> evaluationBefore = new ThreadLocal<>();

    private Set<List<String>> detectAll(ChainDetector detector, List<Transfer> all,
                                        java.util.function.Function<String, List<Transfer>> lookup) {
        Set<List<String>> chains = new HashSet<>();
        for (Transfer transfer : all) {
            evaluationBefore.set(transfer.occurredAt);
            detector.findBest(transfer, lookup).ifPresent(chain ->
                    chains.add(chain.transfers().stream().map(t -> t.transferId).toList()));
        }
        evaluationBefore.remove();
        return chains;
    }
}
