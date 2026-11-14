package com.azentio.projectrelay.service;
import com.azentio.projectrelay.config.LayeringProperties;import org.springframework.context.annotation.*;import org.springframework.data.redis.core.StringRedisTemplate;
@Configuration public class EdgeStoreConfig{
 @Bean @Primary public InboundEdgeStore inboundEdgeStore(LayeringProperties cfg, InMemoryInboundEdgeStore mem, RedisInboundEdgeStore redis, SqlInboundEdgeStore sql){
   return switch(cfg.store()){ case "redis"->redis; case "sql"->sql; default->mem; };
 }
}