package com.azentio.projectrelay.service;
import com.azentio.projectrelay.domain.Transfer;import com.azentio.projectrelay.repo.TransferRepository;import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;import java.util.List;
/** Reference implementation: Task 1 SQL lookup. Used by the parity test and as fallback. */
@Component public class SqlInboundEdgeStore implements InboundEdgeStore{
 private final TransferRepository repo; public SqlInboundEdgeStore(TransferRepository r){repo=r;}
 public void add(Transfer t){ /* SQL is the store; nothing to cache */ }
 public List<Transfer> inbound(String acct, OffsetDateTime before, OffsetDateTime after){ return repo.findInbound(acct,before,after); }
}