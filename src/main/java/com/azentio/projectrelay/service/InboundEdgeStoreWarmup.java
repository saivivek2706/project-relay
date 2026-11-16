package com.azentio.projectrelay.service;

import com.azentio.projectrelay.config.LayeringProperties;
import com.azentio.projectrelay.repo.TransferRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/** Rehydrates the hot edge index from the durable transfer table after a process restart. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class InboundEdgeStoreWarmup implements ApplicationRunner {
    private final TransferRepository transfers;
    private final InboundEdgeStore edgeStore;
    private final LayeringProperties cfg;

    public InboundEdgeStoreWarmup(TransferRepository transfers,
                                  InboundEdgeStore edgeStore,
                                  LayeringProperties cfg) {
        this.transfers = transfers;
        this.edgeStore = edgeStore;
        this.cfg = cfg;
    }

    @Override
    public void run(ApplicationArguments args) {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(cfg.window());
        transfers.findByOccurredAtAfterOrderByOccurredAtAsc(cutoff).forEach(edgeStore::add);
    }
}
