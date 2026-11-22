package com.sankhya.analytics.customers.dto;

import com.sankhya.analytics.common.dto.ChartDataResponse;

import java.util.ArrayList;
import java.util.List;

public class CustomerIntelligenceResponse {

    private CustomerKpiResponse kpis;
    private ChartDataResponse segmentChart;
    private List<CustomerSegmentSummaryDto> segmentSummaries = new ArrayList<>();
    private List<RfmCustomerDto> rfmCustomers = new ArrayList<>();
    private List<RfmCustomerDto> topCustomers = new ArrayList<>();

    public CustomerIntelligenceResponse() {}

    public CustomerKpiResponse getKpis() {
        return kpis;
    }

    public void setKpis(CustomerKpiResponse kpis) {
        this.kpis = kpis;
    }

    public ChartDataResponse getSegmentChart() {
        return segmentChart;
    }

    public void setSegmentChart(ChartDataResponse segmentChart) {
        this.segmentChart = segmentChart;
    }

    public List<CustomerSegmentSummaryDto> getSegmentSummaries() {
        return segmentSummaries;
    }

    public void setSegmentSummaries(List<CustomerSegmentSummaryDto> segmentSummaries) {
        this.segmentSummaries = segmentSummaries;
    }

    public List<RfmCustomerDto> getRfmCustomers() {
        return rfmCustomers;
    }

    public void setRfmCustomers(List<RfmCustomerDto> rfmCustomers) {
        this.rfmCustomers = rfmCustomers;
    }

    public List<RfmCustomerDto> getTopCustomers() {
        return topCustomers;
    }

    public void setTopCustomers(List<RfmCustomerDto> topCustomers) {
        this.topCustomers = topCustomers;
    }
}
