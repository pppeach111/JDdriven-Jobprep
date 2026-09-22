package com.careerpath.evidence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceSkillLinkRepository extends JpaRepository<EvidenceSkillLink, UUID> {

    /** 某用户在某技能上的全部证据链接（跨目标；证据归属用户）。 */
    List<EvidenceSkillLink> findBySkillIdAndEvidence_UserIdOrderByCreatedAtAsc(String skillId, UUID userId);

    List<EvidenceSkillLink> findByEvidence_IdOrderByCreatedAtAsc(UUID evidenceId);
}