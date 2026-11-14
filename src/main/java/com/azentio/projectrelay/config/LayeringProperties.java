package com.azentio.projectrelay.config;
import org.springframework.boot.context.properties.ConfigurationProperties;import java.math.BigDecimal;import java.time.Duration;
@ConfigurationProperties(prefix="layering") public record LayeringProperties(int minHops,int maxHops,Duration maxHopGap,Duration window,BigDecimal minAmount,BigDecimal minHopRetention,BigDecimal minEndToEndRetention,String store,int maxExpansions){
 public LayeringProperties(){this(3,6,Duration.ofMinutes(30),Duration.ofHours(2),new BigDecimal("100000.00"),new BigDecimal("0.90"),new BigDecimal("0.75"),"memory",5000);}
}