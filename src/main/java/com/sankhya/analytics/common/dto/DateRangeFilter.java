package com.sankhya.analytics.common.dto;

import com.sankhya.analytics.common.enums.DatePreset;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class DateRangeFilter {

    private DatePreset preset = DatePreset.LAST_30_DAYS;
    private LocalDate startDate;
    private LocalDate endDate;
    private String category;
    private String productId;

    public DateRangeFilter() {
        resolvePresetBoundaries();
    }

    public DateRangeFilter(DatePreset preset) {
        this.preset = preset != null ? preset : DatePreset.LAST_30_DAYS;
        resolvePresetBoundaries();
    }

    public DateRangeFilter(LocalDate startDate, LocalDate endDate) {
        this.preset = DatePreset.CUSTOM;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static DateRangeFilter of(DatePreset preset, LocalDate startDate, LocalDate endDate) {
        if (preset == DatePreset.CUSTOM || (startDate != null && endDate != null)) {
            DateRangeFilter filter = new DateRangeFilter(startDate, endDate);
            filter.resolvePresetBoundaries();
            return filter;
        }
        return new DateRangeFilter(preset != null ? preset : DatePreset.LAST_30_DAYS);
    }

    public void resolvePresetBoundaries() {
        LocalDate today = LocalDate.now();
        switch (preset) {
            case TODAY -> {
                this.startDate = today;
                this.endDate = today;
            }
            case YESTERDAY -> {
                this.startDate = today.minusDays(1);
                this.endDate = today.minusDays(1);
            }
            case LAST_7_DAYS -> {
                this.startDate = today.minusDays(6);
                this.endDate = today;
            }
            case LAST_30_DAYS -> {
                this.startDate = today.minusDays(29);
                this.endDate = today;
            }
            case LAST_90_DAYS -> {
                this.startDate = today.minusDays(89);
                this.endDate = today;
            }
            case THIS_MONTH -> {
                this.startDate = today.with(TemporalAdjusters.firstDayOfMonth());
                this.endDate = today;
            }
            case LAST_MONTH -> {
                LocalDate prevMonth = today.minusMonths(1);
                this.startDate = prevMonth.with(TemporalAdjusters.firstDayOfMonth());
                this.endDate = prevMonth.with(TemporalAdjusters.lastDayOfMonth());
            }
            case THIS_QUARTER -> {
                int month = today.getMonthValue();
                int firstMonthOfQuarter = ((month - 1) / 3) * 3 + 1;
                this.startDate = LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
                this.endDate = today;
            }
            case THIS_YEAR -> {
                this.startDate = today.with(TemporalAdjusters.firstDayOfYear());
                this.endDate = today;
            }
            case CUSTOM -> {
                if (this.startDate == null) this.startDate = today.minusDays(29);
                if (this.endDate == null) this.endDate = today;
                if (this.startDate.isAfter(this.endDate)) {
                    LocalDate tmp = this.startDate;
                    this.startDate = this.endDate;
                    this.endDate = tmp;
                }
            }
        }
    }

    public LocalDateTime getStartDateTime() {
        return startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
    }

    public LocalDateTime getEndDateTime() {
        return endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
    }

    public DateRangeFilter getComparisonFilter() {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate compEnd = startDate.minusDays(1);
        LocalDate compStart = compEnd.minusDays(days - 1);
        DateRangeFilter comp = new DateRangeFilter(compStart, compEnd);
        comp.setCategory(this.category);
        comp.setProductId(this.productId);
        return comp;
    }

    public DateRangeFilter getPrecedingComparisonFilter() {
        return getComparisonFilter();
    }

    public String getComparisonLabel() {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return "vs preceding " + days + " days";
    }

    public DatePreset getPreset() {
        return preset;
    }

    public void setPreset(DatePreset preset) {
        this.preset = preset;
        if (preset != DatePreset.CUSTOM) {
            resolvePresetBoundaries();
        }
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = (category != null && !category.isBlank()) ? category.trim() : null;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = (productId != null && !productId.isBlank()) ? productId.trim() : null;
    }
}
