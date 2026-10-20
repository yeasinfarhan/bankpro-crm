package com.bankpro;

import com.bankpro.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: boots the whole application against real MySQL 8.4,
 * which proves that the Flyway migrations run and that Hibernate accepts the schema.
 */
class BankProApplicationTests extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayCreatesAllCoreTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);

        assertThat(tables).extracting(String::toLowerCase).contains(
                "users", "branches", "customers", "banking_products", "customer_products", "leads", "sales",
                "follow_up_activities", "customer_interactions", "sales_targets", "notifications", "audit_logs");
    }

    @Test
    void referenceDataIsSeeded() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM branches", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM banking_products WHERE active = TRUE", Integer.class))
                .isEqualTo(8);
    }

    @Test
    void databaseRejectsSecondSaleForSameLead() {
        // The UNIQUE(lead_id) constraint is our last line of defence against duplicate conversions.
        Integer uniqueOnLead = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'sales'
                  AND index_name = 'uk_sales_lead' AND non_unique = 0
                """, Integer.class);
        assertThat(uniqueOnLead).isEqualTo(1);
    }
}
