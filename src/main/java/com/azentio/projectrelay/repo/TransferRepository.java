package com.azentio.projectrelay.repo;
import com.azentio.projectrelay.domain.Transfer;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.time.OffsetDateTime;import java.util.*;
public interface TransferRepository extends JpaRepository<Transfer,String>{
 @Query("select t from Transfer t where t.toAccount=:acct and t.occurredAt < :before and t.occurredAt >= :after order by t.occurredAt desc, t.transferId asc")
 List<Transfer> findInbound(@Param("acct") String acct,@Param("before") OffsetDateTime before,@Param("after") OffsetDateTime after);
}