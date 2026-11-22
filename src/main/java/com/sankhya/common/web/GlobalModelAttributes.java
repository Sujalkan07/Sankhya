package com.sankhya.common.web;

import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal;

/**
 * Exposes shared formatting helpers across all Thymeleaf views.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private static final InrFormatter INR_FORMATTER = new InrFormatter();

    @ModelAttribute("inr")
    public InrFormatter inrFormatter() {
        return INR_FORMATTER;
    }

    public static class InrFormatter {
        public String fmt(BigDecimal amount) {
            return AnalyticsMathUtils.formatInr(amount);
        }

        public String fmt(Number amount) {
            if (amount == null) {
                return AnalyticsMathUtils.formatInr(BigDecimal.ZERO);
            }
            if (amount instanceof BigDecimal bd) {
                return AnalyticsMathUtils.formatInr(bd);
            }
            return AnalyticsMathUtils.formatInr(BigDecimal.valueOf(amount.doubleValue()));
        }
    }
}
