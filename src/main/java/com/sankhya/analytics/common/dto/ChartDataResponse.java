package com.sankhya.analytics.common.dto;

import java.util.ArrayList;
import java.util.List;

public class ChartDataResponse {

    private List<String> labels = new ArrayList<>();
    private List<ChartDataset> datasets = new ArrayList<>();

    public ChartDataResponse() {}

    public ChartDataResponse(List<String> labels, List<ChartDataset> datasets) {
        this.labels = labels != null ? labels : new ArrayList<>();
        this.datasets = datasets != null ? datasets : new ArrayList<>();
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<ChartDataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<ChartDataset> datasets) {
        this.datasets = datasets;
    }

    public void addDataset(String label, List<? extends Number> data) {
        this.datasets.add(new ChartDataset(label, data, "#1C6B00"));
    }

    public static class ChartDataset {
        private String label;
        private List<? extends Number> data;
        private String backgroundColor;
        private String borderColor;
        private boolean fill;

        public ChartDataset() {}

        public ChartDataset(String label, List<? extends Number> data, String color) {
            this.label = label;
            this.data = data;
            this.backgroundColor = color;
            this.borderColor = color;
            this.fill = false;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<? extends Number> getData() {
            return data;
        }

        public void setData(List<? extends Number> data) {
            this.data = data;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public String getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(String borderColor) {
            this.borderColor = borderColor;
        }

        public boolean isFill() {
            return fill;
        }

        public void setFill(boolean fill) {
            this.fill = fill;
        }
    }
}
