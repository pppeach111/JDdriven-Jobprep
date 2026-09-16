package com.careerpath.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSkillEstimateRepository extends JpaRepository<UserSkillEstimate, UUID> {

    List<UserSkillEstimate> findByUserIdAndGoalId(UUID userId, UUID goalId);

    Optional<UserSkillEstimate> findByUserIdAndGoalIdAndSkillId(UUID userId, UUID goalId, String skillId);

    void deleteByUserIdAndGoalId(UUID userId, UUID goalId);
}