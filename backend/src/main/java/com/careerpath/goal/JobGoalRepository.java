package com.careerpath.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobGoalRepository extends JpaRepository<JobGoal, UUID> {

    List<JobGoal> findByUserIdOrderByCreatedAtDesc(UUID userId);
}