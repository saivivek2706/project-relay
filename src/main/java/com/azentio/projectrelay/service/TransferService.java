package com.azentio.projectrelay.service;

import com.azentio.projectrelay.config.LayeringProperties;
import com.azentio.projectrelay.domain.Alert;
import com.azentio.projectrelay.domain.Transfer;
import com.azentio.projectrelay.dto.TransferRequest;
import com.azentio.projectrelay.repo.AlertRepository;
import com.azentio.projectrelay.repo.TransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class TransferService {
    private final TransferRepository transfers;
    private final AlertRepository alerts;
    private final ChainDetector detector;
    private final InboundEdgeStore edgeStore;
    private final LayeringProperties cfg;

    @Autowired
    public TransferService(TransferRepository transfers, AlertRepository alerts,
                           ChainDetector detector, InboundEdgeStore edgeStore, LayeringProperties cfg) {
        this.transfers = transfers;
        this.alerts = alerts;
        this.detector = detector;
        this.edgeStore = edgeStore;
        this.cfg = cfg;
    }

    public record Decision(String transferId, String decision, Long alertId, boolean duplicate) {
        public Decision(String transferId, String decision, Long alertId) {
            this(transferId, decision, alertId, false);
        }
    }

    @Transactional
    public Decision ingest(TransferRequest req) {
        Optional<Transfer> existing = transfers.findById(req.transferId());
        if (existing.isPresent()) {
            return new Decision(req.transferId(), existing.get().decision,
                    existing.get().alertId, true);
        }

        Transfer transfer = new Transfer(req.transferId(), req.fromAccount(), req.toAccount(),
                req.amount().setScale(2, RoundingMode.UNNECESSARY), req.occurredAt());
        transfers.save(transfer);

        // Keep the configured hot edge store in sync with the SQL source of truth.
        edgeStore.add(transfer);
        Optional<ChainDetector.Chain> detected = detector.findBest(transfer,
                account -> edgeStore.inbound(account, transfer.occurredAt,
                        transfer.occurredAt.minus(cfg.window())));

        if (detected.isEmpty()) return new Decision(req.transferId(), "CLEAR", null);

        ChainDetector.Chain chain = detected.get();
        List<Transfer> chainTransfers = chain.transfers();
        List<String> ids = chainTransfers.stream().map(t -> t.transferId).toList();

        // Extend only an OPEN alert whose complete existing sequence is contained in the new one.
        for (Alert old : alerts.findByStatusOrderByUpdatedAtDesc("OPEN")) {
            if (ids.containsAll(old.chainIds())) {
                old.hops = ids.size();
                old.lastAmount = chainTransfers.get(chainTransfers.size() - 1).amount;
                old.endToEndRetention = chain.e2e();
                old.endAt = chainTransfers.get(chainTransfers.size() - 1).occurredAt;
                old.updatedAt = OffsetDateTime.now();
                old.setChainIds(ids);
                alerts.save(old);
                transfer.decision = "ALERT";
                transfer.alertId = old.alertId;
                transfers.save(transfer);
                return new Decision(req.transferId(), "ALERT", old.alertId);
            }
        }

        Alert alert = new Alert();
        alert.hops = ids.size();
        alert.firstAmount = chainTransfers.get(0).amount;
        alert.lastAmount = chainTransfers.get(chainTransfers.size() - 1).amount;
        alert.endToEndRetention = chain.e2e();
        alert.startAt = chainTransfers.get(0).occurredAt;
        alert.endAt = chainTransfers.get(chainTransfers.size() - 1).occurredAt;
        alert.status = "OPEN";
        alert.createdAt = OffsetDateTime.now();
        alert.updatedAt = alert.createdAt;
        alert.setChainIds(ids);
        alerts.save(alert);
        transfer.decision = "ALERT";
        transfer.alertId = alert.alertId;
        transfers.save(transfer);
        return new Decision(req.transferId(), "ALERT", alert.alertId);
    }

}
