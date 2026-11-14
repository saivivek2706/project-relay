package com.azentio.projectrelay.generator;
import java.io.*;import java.math.*;import java.nio.file.*;import java.time.*;import java.util.*;
public class Generator{
 public record Row(String id,String from,String to,BigDecimal amt,OffsetDateTime at){}
 public static void main(String[] args) throws Exception{
   long seed=args.length>0?Long.parseLong(args[0]):42; String out=args.length>1?args[1]:"seeded-transfers.jsonl"; String labels=args.length>2?args[2]:"labels.json";
   Random rnd=new Random(seed); List<Row> rows=new ArrayList<>(); List<List<String>> planted=new ArrayList<>(); int seq=1;
   OffsetDateTime base=OffsetDateTime.parse("2026-01-01T09:00:00Z");
   // 10 planted chains 3-6 hops
   for(int c=0;c<10;c++){ int hops=3+rnd.nextInt(4); List<String> ids=new ArrayList<>(); List<String> accts=new ArrayList<>(); for(int i=0;i<=hops;i++) accts.add(String.format("%010d",1000000000L+c*100+i*7+rnd.nextInt(5))); BigDecimal amt=new BigDecimal("200000.00"); OffsetDateTime at=base.plusHours(c*3); for(int i=0;i<hops;i++){ String id="PLANTED-"+c+"-"+i; BigDecimal next=amt.multiply(new BigDecimal("0.97")).setScale(2,RoundingMode.HALF_UP); rows.add(new Row(id,accts.get(i),accts.get(i+1),i==0?amt:next,at)); ids.add(id); amt=next; at=at.plusMinutes(5+rnd.nextInt(10)); } planted.add(ids); }
   // Decoys: 3 kinds
   // 1) hop gap 31-60 min (3-hop)
   { List<String> ids=new ArrayList<>(); OffsetDateTime at=base.plusHours(40); BigDecimal amt=new BigDecimal("200000.00"); for(int i=0;i<3;i++){ String id="DECOY-GAP-"+i; BigDecimal n=amt.multiply(new BigDecimal("0.97")).setScale(2,RoundingMode.HALF_UP); rows.add(new Row(id,String.format("%010d",2000000000L+i),String.format("%010d",2000000000L+i+1),i==0?amt:n,at)); ids.add(id); amt=n; at=at.plusMinutes(45);} }
   // 2) hop retention <0.90 (drop to 0.80)
   { OffsetDateTime at=base.plusHours(44); BigDecimal amt=new BigDecimal("200000.00"); for(int i=0;i<3;i++){ String id="DECOY-RET-"+i; BigDecimal n=i==1?amt.multiply(new BigDecimal("0.80")).setScale(2,RoundingMode.HALF_UP):amt.multiply(new BigDecimal("0.97")).setScale(2,RoundingMode.HALF_UP); rows.add(new Row(id,String.format("%010d",2100000000L+i),String.format("%010d",2100000000L+i+1),i==0?amt:n,at)); amt=n; at=at.plusMinutes(5);} }
   // 3) e2e <0.75 : 0.90^3=0.729
   { OffsetDateTime at=base.plusHours(48); BigDecimal amt=new BigDecimal("200000.00"); for(int i=0;i<3;i++){ String id="DECOY-E2E-"+i; BigDecimal n=amt.multiply(new BigDecimal("0.90")).setScale(2,RoundingMode.HALF_UP); rows.add(new Row(id,String.format("%010d",2200000000L+i),String.format("%010d",2200000000L+i+1),i==0?amt:n,at)); amt=n; at=at.plusMinutes(5);} }
   // background < min-amount, distinct accounts
   for(int i=0;i<5000;i++){ String f=String.format("%010d",3000000000L+i); String t=String.format("%010d",3000000001L+i); BigDecimal amt=new BigDecimal(1000+rnd.nextInt(50000)).setScale(2); rows.add(new Row("BG-"+(seq++),f,t,amt,base.plusMinutes(rnd.nextInt(10000)))); }
   rows.sort(Comparator.comparing(r->r.at()));
   try(BufferedWriter w=Files.newBufferedWriter(Path.of(out))){ for(Row r:rows) w.write(String.format("{\"transferId\":\"%s\",\"fromAccount\":\"%s\",\"toAccount\":\"%s\",\"amount\":%s,\"occurredAt\":\"%s\"}\n",r.id(),r.from(),r.to(),r.amt(),r.at())); }
   Files.writeString(Path.of(labels), "{\"planted\":"+planted.toString().replace(" ","")+ "}");
   System.out.println("wrote "+rows.size()+" rows, planted="+planted.size());
 }
}