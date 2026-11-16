package com.azentio.projectrelay;

import com.azentio.projectrelay.config.LayeringProperties;
import com.azentio.projectrelay.domain.Transfer;
import com.azentio.projectrelay.service.RedisInboundEdgeStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class RedisInboundEdgeStoreIntegrationTest {
    @Test
    void trimsOldEventTimeEntriesAndSetsKeyTtl() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        LettuceConnectionFactory connection = new LettuceConnectionFactory(host, port);
        connection.afterPropertiesSet();
        try {
            StringRedisTemplate template = new StringRedisTemplate(connection);
            template.afterPropertiesSet();
            var cfg = new LayeringProperties(3, 6, Duration.ofMinutes(30), Duration.ofHours(2),
                    new BigDecimal("100000.00"), new BigDecimal("0.9000"),
                    new BigDecimal("0.7500"), "redis", 5000);
            RedisInboundEdgeStore store = new RedisInboundEdgeStore(template, cfg);
            OffsetDateTime now = OffsetDateTime.now().withNano(0);
            String destination = "1234567890";

            store.add(new Transfer("OLD", "1111111111", destination,
                    new BigDecimal("200000.00"), now.minusHours(3)));
            store.add(new Transfer("OLD-BOUNDARY", "4444444444", destination,
                    new BigDecimal("195000.00"), now.minusHours(2).plusNanos(50)));
            store.add(new Transfer("RECENT", "2222222222", destination,
                    new BigDecimal("190000.00"), now));
            store.add(new Transfer("AFTER-BOUND", "3333333333", destination,
                    new BigDecimal("180000.00"), now.plusNanos(100)));

            List<Transfer> inbound = store.inbound(destination, now.plusNanos(50), now.minusHours(2));
            assertEquals(List.of("RECENT"), inbound.stream().map(t -> t.transferId).toList());
            Long ttl = template.getExpire("relay:inbound:" + destination, TimeUnit.SECONDS);
            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= Duration.ofHours(2).plusMinutes(35).getSeconds());
        } finally {
            connection.destroy();
        }
    }
}
