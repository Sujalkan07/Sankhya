package com.sankhya.ml.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ml.entity.MLModel;
import com.sankhya.ml.enums.AlgorithmType;
import com.sankhya.ml.repository.MLModelExecutionRepository;
import com.sankhya.ml.repository.MLModelRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModelRegistryService Unit Tests")
class ModelRegistryServiceTest {

    @Mock
    private MLModelRepository mlModelRepository;
    @Mock
    private MLModelExecutionRepository mlModelExecutionRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;

    private ModelRegistryService registryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        registryService = new ModelRegistryService(
                mlModelRepository,
                mlModelExecutionRepository,
                workspaceRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("ensureDefaultModelsForWorkspace registers 4 canonical ML models when absent")
    void testEnsureDefaultModels() {
        Workspace ws = new Workspace();
        ws.setId(1L);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(ws));
        when(mlModelRepository.findByWorkspaceIdAndAlgorithm(eq(1L), any(AlgorithmType.class))).thenReturn(Optional.empty());

        registryService.ensureDefaultModelsForWorkspace(1L);

        verify(mlModelRepository, times(4)).save(any(MLModel.class));
    }

    @Test
    @DisplayName("parseMetrics parses JSON string into map properly")
    void testParseMetrics() {
        String json = "{\"accuracy\":0.92,\"f1Score\":0.89,\"k\":4}";
        Map<String, Object> map = registryService.parseMetrics(json);

        assertNotNull(map);
        assertEquals(0.92, ((Number) map.get("accuracy")).doubleValue(), 1e-4);
        assertEquals(4, ((Number) map.get("k")).intValue());
    }
}
