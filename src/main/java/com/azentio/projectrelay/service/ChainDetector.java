package com.azentio.projectrelay.service;
import com.azentio.projectrelay.config.LayeringProperties;import com.azentio.projectrelay.domain.Transfer;import org.springframework.stereotype.Service;
import java.math.*;import java.util.*;import java.util.function.Function;
@Service public class ChainDetector{
 private final LayeringProperties cfg; public ChainDetector(LayeringProperties c){cfg=c;}
 public record Chain(List<Transfer> transfers, BigDecimal e2e){ }
 public Optional<Chain> findBest(Transfer last, Function<String,List<Transfer>> inbound){
   List<Chain> found=new ArrayList<>(); Deque<Transfer> path=new ArrayDeque<>(); path.addLast(last);
   Set<String> seenAccts=new HashSet<>(); seenAccts.add(last.fromAccount); seenAccts.add(last.toAccount);
   dfs(last,path,seenAccts,found,inbound,new int[]{0});
   if(found.isEmpty()) return Optional.empty();
   found.sort(Comparator.comparingInt((Chain c)->c.transfers().size()).reversed().thenComparing(c->c.transfers().get(0).occurredAt).thenComparing(c->c.transfers().get(0).transferId));
   return Optional.of(found.get(0));
 }
 private void dfs(Transfer cur,Deque<Transfer> path,Set<String> seen,List<Chain> found,Function<String,List<Transfer>> inbound,int[] expansions){
   if(path.size()>=cfg.minHops()){ check(path).ifPresent(found::add); }
   if(path.size()>=cfg.maxHops()) return;
   // prune by window: earliest must be within window of last (cur is last overall? actually path head is earliest, tail is last)
   List<Transfer> cands=inbound.apply(cur.fromAccount);
   for(Transfer prev: cands){
     if(++expansions[0]>cfg.maxExpansions()) return;
     if(!prev.occurredAt.isBefore(cur.occurredAt)) continue;
     if(java.time.Duration.between(prev.occurredAt,cur.occurredAt).compareTo(cfg.maxHopGap())>0) continue;
     if(prev.amount.compareTo(cfg.minAmount())<0) continue;
     // hop retention prev->cur : cur.amount/prev.amount >= minHop
     if(cur.amount.divide(prev.amount,4,RoundingMode.HALF_UP).compareTo(cfg.minHopRetention())<0) continue;
     if(seen.contains(prev.fromAccount)) continue; // no account twice: prev.to == cur.from already seen, prev.from must be new
     // window check vs final last transfer (path last)
     Transfer last=path.peekLast(); if(java.time.Duration.between(prev.occurredAt,last.occurredAt).compareTo(cfg.window())>0) continue;
     path.addFirst(prev); seen.add(prev.fromAccount); dfs(prev,path,seen,found,inbound,expansions); path.removeFirst(); seen.remove(prev.fromAccount);
     if(expansions[0]>cfg.maxExpansions()) return;
   }
 }
 private Optional<Chain> check(Deque<Transfer> path){
   List<Transfer> list=new ArrayList<>(path); // earliest first
   // min-amount already enforced for predecessors, check first and last? spec: t1.amount >= min-amount
   if(list.get(0).amount.compareTo(cfg.minAmount())<0) return Optional.empty();
   // all hop retentions already checked except need verify all pairs (dfs ensures)
   BigDecimal e2e=list.get(list.size()-1).amount.divide(list.get(0).amount,4,RoundingMode.HALF_UP);
   if(e2e.compareTo(cfg.minEndToEndRetention())<0) return Optional.empty();
   if(java.time.Duration.between(list.get(0).occurredAt,list.get(list.size()-1).occurredAt).compareTo(cfg.window())>0) return Optional.empty();
   // no account twice
   Set<String> s=new HashSet<>(); for(Transfer t:list){ if(!s.add(t.fromAccount)) return Optional.empty(); } if(!s.add(list.get(list.size()-1).toAccount)) return Optional.empty();
   return Optional.of(new Chain(list,e2e));
 }
}