package com.careerpath.jd;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SourceJdRepository extends JpaRepository<SourceJd, UUID> {

    List<SourceJd> findByGoalIdOrderByCreatedAtDesc(UUID goalId);
}