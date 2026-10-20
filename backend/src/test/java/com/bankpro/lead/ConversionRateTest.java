package com.bankpro.lead;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Conversion rate = converted ÷ (converted + lost) × 100, with no divide-by-zero. */
class ConversionRateTest {

    private BigDecimal rate(long converted, long lost) throws Exception {
        Method m = com.bankpro.service.LeadService.class.getDeclaredMethod("conversionRate", long.class, long.class);
        m.setAccessible(true);
        return (BigDecimal) m.invoke(null, converted, lost);
    }

    @Test
    void computesRateOfClosedLeads() throws Exception {
        assertThat(rate(3, 1)).isEqualByComparingTo("75.0");
        assertThat(rate(1, 2)).isEqualByComparingTo("33.3");
        assertThat(rate(0, 4)).isEqualByComparingTo("0.0");
    }

    @Test
    void nothingClosedMeansNoRate() throws Exception {
        assertThat(rate(0, 0)).isNull();
    }
}
