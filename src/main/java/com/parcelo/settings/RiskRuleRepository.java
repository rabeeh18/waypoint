package com.parcelo.settings;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleRepository extends JpaRepository<RiskRule, String> {

    List<RiskRule> findAllByOrderBySortOrderAsc();
}
