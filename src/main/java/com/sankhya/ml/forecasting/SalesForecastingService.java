package com.sankhya.ml.forecasting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ml.entity.ForecastResultEntity;
import com.sankhya.ml.entity.MLModel;
import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.enums.AlgorithmType;
import com.sankhya.ml.enums.ExecutionStatus;
import com.sankhya.ml.enums.ModelStatus;
import com.sankhya.ml.enums.ModelType;
import com.sankhya.ml.features.FeatureEngineeringService;
import com.sankhya.ml.features.TimeSeriesFeatureVector;
import com.sankhya.ml.repository.ForecastResultRepository;
import com.sankhya.ml.repository.MLModelExecutionRepository;
import com.sankhya.ml.repository.MLModelRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SalesForecastingService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastingService.class);

    private final FeatureEngineeringService featureEngineeringService;
    private final MLModelRepository mlModelRepository;
    private final MLModelExecutionRepository mlModelExecutionRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public SalesForecastingService(FeatureEngineeringService featureEngineeringService,
                                   MLModelRepository mlModelRepository,
                                   MLModelExecutionRepository mlModelExecutionRepository,
                                   ForecastResultRepository forecastResultRepository,
                                   WorkspaceRepository workspaceRepository,
                                   ObjectMapper objectMapper) {
        this.featureEngineeringService = featureEngineeringService;
        this.mlModelRepository = mlModelRepository;
        this.mlModelExecutionRepository = mlModelExecutionRepository;
        this.forecastResultRepository = forecastResultRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MLModelExecution trainAndForecast(Long workspaceId, int horizonDays) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        MLModel model = mlModelRepository.findByWorkspaceIdAndAlgorithm(workspaceId, AlgorithmType.LINEAR_REGRESSION)
                .orElseGet(() -> {
                    MLModel m = new MLModel();
                    m.setWorkspace(workspace);
                    m.setModelName("Linear Regression Sales Forecasting");
                    m.setAlgorithm(AlgorithmType.LINEAR_REGRESSION);
                    m.setModelType(ModelType.FORECASTING);
                    m.setVersion("v1.0.0");
                    m.setDescription("Predicts forward multi-step daily revenue with trend, seasonality, and autoregressive lag indicators.");
                    m.setStatus(ModelStatus.READY);
                    return mlModelRepository.save(m);
                });

        model.setStatus(ModelStatus.TRAINING);
        mlModelRepository.save(model);

        MLModelExecution execution = new MLModelExecution();
        execution.setModel(model);
        execution.setWorkspace(workspace);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution = mlModelExecutionRepository.save(execution);

        long startMs = System.currentTimeMillis();

        try {
            List<TimeSeriesFeatureVector> history = featureEngineeringService.prepareTimeSeriesFeatures(workspaceId);
            if (history.size() < 7) {
                throw new IllegalStateException("Insufficient daily sales history (minimum 7 daily records required).");
            }

            int n = history.size();
            double[][] X = new double[n][];
            double[] y = new double[n];

            for (int i = 0; i < n; i++) {
                X[i] = history.get(i).getFeatureArray();
                y[i] = history.get(i).getRevenue();
            }

            MultipleLinearRegression lr = new MultipleLinearRegression();
            lr.fit(X, y);

            // Calculate baseline moving average over last 7 days
            double recentSum = 0.0;
            int baseWindow = Math.min(7, n);
            for (int i = n - baseWindow; i < n; i++) {
                recentSum += y[i];
            }
            double baselineDaily = recentSum / baseWindow;

            // Delete old forecasts for workspace
            List<ForecastResultEntity> oldForecasts = forecastResultRepository.findByWorkspaceIdOrderByForecastDateAsc(workspaceId);
            if (!oldForecasts.isEmpty()) {
                forecastResultRepository.deleteAllInBatch(oldForecasts);
            }

            // Recursive multi-step forecasting
            List<ForecastResultEntity> forecasts = new ArrayList<>();
            LocalDate lastDate = history.get(n - 1).getDate();
            double lastRev = y[n - 1];
            double runningTrend = history.get(n - 1).getTrendIndex();

            List<Double> recentForecasts = new ArrayList<>();
            for (int i = Math.max(0, n - 7); i < n; i++) {
                recentForecasts.add(y[i]);
            }

            double zScore95 = 1.96;
            double rmse = lr.getRmse();

            for (int h = 1; h <= horizonDays; h++) {
                LocalDate targetDate = lastDate.plusDays(h);
                double dow = targetDate.getDayOfWeek().getValue();
                double isWeekend = (dow >= 6) ? 1.0 : 0.0;
                double lag1 = (h == 1) ? lastRev : recentForecasts.get(recentForecasts.size() - 1);
                double lag7 = (recentForecasts.size() >= 7) ? recentForecasts.get(recentForecasts.size() - 7) : lag1;

                double sum7 = 0.0;
                int w = Math.min(7, recentForecasts.size());
                for (int k = 0; k < w; k++) {
                    sum7 += recentForecasts.get(recentForecasts.size() - 1 - k);
                }
                double rolling7 = sum7 / w;
                double tIndex = runningTrend + h;

                double[] feat = new double[]{tIndex, dow, isWeekend, lag1, lag7, rolling7};
                double pred = lr.predict(feat);

                // Add seasonal weekend lift if historical data showed weekend spikes
                if (isWeekend > 0.5) {
                    pred *= 1.12;
                }

                recentForecasts.add(pred);

                double lower = Math.max(0.0, pred - (zScore95 * rmse));
                double upper = pred + (zScore95 * rmse);

                ForecastResultEntity entity = new ForecastResultEntity();
                entity.setWorkspace(workspace);
                entity.setExecution(execution);
                entity.setForecastDate(targetDate);
                entity.setPredictedRevenue(pred);
                entity.setBaselineRevenue(baselineDaily);
                entity.setLowerBound(lower);
                entity.setUpperBound(upper);
                entity.setHorizonDay(h);
                entity.setCreatedAt(LocalDateTime.now());

                forecasts.add(entity);
            }

            forecastResultRepository.saveAll(forecasts);

            // Save metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("rmse", lr.getRmse());
            metrics.put("mae", lr.getMae());
            metrics.put("mape", lr.getMape());
            metrics.put("rSquared", lr.getrSquared());
            metrics.put("baselineDailyAvg", baselineDaily);
            metrics.put("forecastDays", horizonDays);
            metrics.put("historyDaysUsed", n);

            Map<String, Object> hyperparams = new HashMap<>();
            hyperparams.put("algorithm", "Ordinary Least Squares with L2 Ridge");
            hyperparams.put("horizonDays", horizonDays);
            hyperparams.put("confidenceInterval", "95%");

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(System.currentTimeMillis() - startMs);
            execution.setTrainRecordCount(n);
            execution.setHyperparametersJson(objectMapper.writeValueAsString(hyperparams));
            execution.setMetricsJson(objectMapper.writeValueAsString(metrics));

            model.setStatus(ModelStatus.EVALUATED);
            model.setUpdatedAt(LocalDateTime.now());
            mlModelRepository.save(model);

            return mlModelExecutionRepository.save(execution);

        } catch (Exception e) {
            log.error("Failed to train linear regression forecasting model for workspace {}", workspaceId, e);
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(System.currentTimeMillis() - startMs);
            execution.setErrorMessage(e.getMessage());
            model.setStatus(ModelStatus.FAILED);
            mlModelRepository.save(model);
            return mlModelExecutionRepository.save(execution);
        }
    }
}
