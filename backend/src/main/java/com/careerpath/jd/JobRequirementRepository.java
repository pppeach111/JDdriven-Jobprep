package com.careerpath.jd;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRequirementRepository extends JpaRepository<JobRequirement, UUID> {

    List<JobRequirement> findByGoalId(UUID goalId);

    List<JobRequirement> findBySourceJdId(UUID sourceJdId);

    void deleteBySourceJdId(UUID sourceJdId);
}