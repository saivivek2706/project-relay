package com.azentio.projectrelay.service;
import com.azentio.projectrelay.config.LayeringProperties;import com.azentio.projectrelay.domain.Transfer;import org.springframework.data.redis.core.StringRedisTemplate;import org.springframework.stereotype.Component;
import java.math.BigDecimal;import java.time.*;import java.util.*;
/** Redis: ZSET per destination account, score = occurredAt epoch millis, member = transferId|from|amount|epoch.
 *  Key: relay:inbound:{toAccount}  TTL: window + max-hop-gap + 5m slack (2h35m by default).
 *  On every add: ZADD, ZREMRANGEBYSCORE below (now-window), EXPIRE. Lookup: ZREVRANGEBYSCORE in (after, before). */
@Component public class RedisInboundEdgeStore implements InboundEdgeStore{
 private final StringRedisTemplate redis; private final LayeringProperties cfg;
 public RedisInboundEdgeStore(StringRedisTemplate r, LayeringProperties c){redis=r;cfg=c;}
 static String key(String acct){ return "relay:inbound:"+acct; }
 long ttlSeconds(){ return cfg.window().plus(cfg.maxHopGap()).plus(Duration.ofMinutes(5)).getSeconds(); }
 public void add(Transfer t){
   String member=String.join("|",t.transferId,t.fromAccount,t.amount.toPlainString(),String.valueOf(t.occurredAt.toInstant().toEpochMilli()));
   String k=key(t.toAccount); double score=t.occurredAt.toInstant().toEpochMilli();
   redis.opsForZSet().add(k,member,score);
   redis.opsForZSet().removeRangeByScore(k,0, t.occurredAt.minus(cfg.window()).toInstant().toEpochMilli()-1);
   redis.expire(k,Duration.ofSeconds(ttlSeconds()));
 }
 public List<Transfer> inbound(String acct, OffsetDateTime before, OffsetDateTime after){
   var members=redis.opsForZSet().reverseRangeByScore(key(acct), after.toInstant().toEpochMilli(), before.toInstant().toEpochMilli()-1);
   List<Transfer> out=new ArrayList<>(); if(members==null) return out;
   for(String m: members){ String[] p=m.split("\\|"); if(p.length!=4) continue; out.add(new Transfer(p[0],p[1],acct,new BigDecimal(p[2]),OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(p[3])),ZoneOffset.UTC))); }
   return out;
 }
}