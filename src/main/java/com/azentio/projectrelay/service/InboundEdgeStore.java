package com.azentio.projectrelay.service;
import com.azentio.projectrelay.domain.Transfer;import java.time.OffsetDateTime;import java.util.List;
/** Hot backwards-lookup: inbound transfers into an account in a time range, newest first. */
public interface InboundEdgeStore{
 void add(Transfer t);
 List<Transfer> inbound(String toAccount, OffsetDateTime before, OffsetDateTime after);
}