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

        transfers.deleteAll();
        InMemoryInboundEdgeStore memory = new InMemoryInboundEdgeStore(cfg);
        ChainDetector detector = new ChainDetector(cfg, new SimpleMeterRegistry());
        Set<List<String>> memoryChains = new HashSet<>();
        Set<List<String>> sqlChains = new HashSet<>();

        // Replay chronologically so both stores see exactly the same event-time history.
        for (String line : Files.readAllLines(file)) {
            TransferRequest request = mapper.readValue(line, TransferRequest.class);
            Transfer transfer = new Transfer(request.transferId(), request.fromAccount(),
                    request.toAccount(), request.amount(), request.occurredAt());

            transfers.saveAndFlush(transfer);
            memory.add(transfer);

            detector.findBest(transfer, account -> memory.inbound(account,
                    transfer.occurredAt, transfer.occurredAt.minus(cfg.window())))
                    .ifPresent(chain -> memoryChains.add(
                            chain.transfers().stream().map(t -> t.transferId).toList()));

            detector.findBest(transfer, account -> sql.inbound(account,
                    transfer.occurredAt, transfer.occurredAt.minus(cfg.window())))
                    .ifPresent(chain -> sqlChains.add(
                            chain.transfers().stream().map(t -> t.transferId).toList()));
        }

        assertEquals(memoryChains, sqlChains,
                "memory edge store and Task 1 SQL reference must return identical chain sequences");
        assertTrue(memoryChains.stream().anyMatch(chain -> chain.get(0).startsWith("PLANTED-")));
    }
}
