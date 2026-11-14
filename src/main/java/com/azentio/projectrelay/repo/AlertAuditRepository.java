package com.azentio.projectrelay.repo;
import com.azentio.projectrelay.domain.AlertAudit;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface AlertAuditRepository extends JpaRepository<AlertAudit,Long>{ List<AlertAudit> findByAlertIdOrderByCreatedAtAsc(Long id); }