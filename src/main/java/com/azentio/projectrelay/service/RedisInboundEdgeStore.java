package com.azentio.projectrelay.service;

import com.azentio.projectrelay.config.LayeringProperties;
import com.azentio.projectrelay.domain.Transfer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/** Redis sorted set per destination account, scored by event-time epoch milliseconds. */
@Component
public class RedisInboundEdgeStore implements InboundEdgeStore {
    private final StringRedisTemplate redis;
    private final LayeringProperties cfg;

    public RedisInboundEdgeStore(StringRedisTemplate redis, LayeringProperties cfg) {
        this.redis = redis;
        this.cfg = cfg;
    }

    static String key(String account) {
        return "relay:inbound:" + account;
    }

    long ttlSeconds() {
        return cfg.window().plus(cfg.maxHopGap()).plus(Duration.ofMinutes(5)).getSeconds();
    }

    @Override
    public void add(Transfer transfer) {
        String encodedId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(transfer.transferId.getBytes(StandardCharsets.UTF_8));
        // Keep the full ISO timestamp in the member; the sorted-set score is millisecond precision.
        String member = String.join("|", encodedId, transfer.fromAccount,
                transfer.amount.toPlainString(), transfer.occurredAt.toString());
        String key = key(transfer.toAccount);
        long score = transfer.occurredAt.toInstant().toEpochMilli();

        redis.opsForZSet().add(key, member, score);
        long cutoff = transfer.occurredAt.minus(cfg.window()).toInstant().toEpochMilli();
        redis.opsForZSet().removeRangeByScore(key, 0, cutoff - 1);
        redis.expire(key, Duration.ofSeconds(ttlSeconds()));
    }

    @Override
    public List<Transfer> inbound(String account, OffsetDateTime before, OffsetDateTime after) {
        var members = redis.opsForZSet().reverseRangeByScore(key(account),
                after.toInstant().toEpochMilli(), before.toInstant().toEpochMilli());
        List<Transfer> result = new ArrayList<>();
        if (members == null) return result;

        for (String member : members) {
            String[] fields = member.split("\\|", 4);
            if (fields.length != 4) continue;

            OffsetDateTime occurredAt = OffsetDateTime.parse(fields[3]);
            // Filter at full timestamp precision after the millisecond score range lookup.
            if (!occurredAt.isBefore(before) || occurredAt.isBefore(after)) continue;

            String transferId = new String(Base64.getUrlDecoder().decode(fields[0]),
                    StandardCharsets.UTF_8);
            result.add(new Transfer(transferId, fields[1], account,
                    new BigDecimal(fields[2]), occurredAt));
        }

        result.sort(Comparator.comparing((Transfer t) -> t.occurredAt).reversed()
                .thenComparing(t -> t.transferId));
        return result;
    }
}
