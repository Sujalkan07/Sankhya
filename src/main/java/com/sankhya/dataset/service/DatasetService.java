package com.sankhya.dataset.service;

import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.dataset.schema.CanonicalField;
import com.sankhya.dataset.schema.DatasetSchemaDefinition;
import com.sankhya.workspace.entity.Workspace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatasetService {

    private final DatasetRepository datasetRepository;

    public DatasetService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @Transactional(readOnly = true)
    public List<Dataset> getDatasetsByWorkspace(Long workspaceId) {
        return datasetRepository.findByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public Optional<Dataset> getDatasetById(Long id, Long workspaceId) {
        return datasetRepository.findByIdAndWorkspaceId(id, workspaceId);
    }

    @Transactional(readOnly = true)
    public Optional<Dataset> getDatasetByType(DatasetType datasetType, Long workspaceId) {
        return datasetRepository.findByWorkspaceIdAndDatasetType(workspaceId, datasetType);
    }

    @Transactional
    public Dataset findOrCreateDataset(Workspace workspace, DatasetType datasetType) {
        return datasetRepository.findByWorkspaceIdAndDatasetType(workspace.getId(), datasetType)
                .orElseGet(() -> {
                    Dataset dataset = new Dataset(
                            workspace,
                            datasetType,
                            datasetType.getDisplayName(),
                            "Workspace dataset for " + datasetType.getDisplayName()
                    );
                    return datasetRepository.save(dataset);
                });
    }

    public byte[] generateTemplateCsv(DatasetType datasetType) {
        List<CanonicalField> fields = DatasetSchemaDefinition.getFields(datasetType);
        String headerLine = fields.stream()
                .map(CanonicalField::canonicalName)
                .collect(Collectors.joining(","));

        StringBuilder sb = new StringBuilder();
        sb.append(headerLine).append("\n");

        switch (datasetType) {
            case UNIFIED_MART -> {
                sb.append("BILL-0000204,2026-09-21 09:00:00,CUST-115481,Rhea More,Bengaluru,REGULAR,SKU-0607,Shredded Mozzarella,Dairy Eggs,Packaged Cheese,MeadowVale Dairy,44.0,60.25,1,60.25,0.0,Cash,Aarav Sharma,2026-11-22,90,45,HEALTHY\n");
                sb.append("BILL-0000204,2026-09-21 09:00:00,CUST-115481,Rhea More,Bengaluru,REGULAR,SKU-0790,Organic Thyme,Produce,Fresh Herbs,Green Basket Select,110.63,168.12,1,168.12,0.0,Cash,Aarav Sharma,2026-11-22,74,37,HEALTHY\n");
            }
            case PRODUCT_CATALOG -> {
                sb.append("PROD_001,Amul Taaza Homogenised Toned Milk 1L,Dairy,Amul,68.00,74.00,Litres,Dairy & Chilled,true,10\n");
                sb.append("PROD_002,Aashirvaad Shudh Chakki Atta 5kg,Staples & Grains,ITC,210.00,245.00,Kilograms,Grocery,true,180\n");
                sb.append("PROD_003,Tata Salt Vaccum Evaporated 1kg,Staples & Grains,Tata Consumer,22.00,28.00,Kilograms,Grocery,true,720\n");
                sb.append("PROD_004,Maggi 2-Minute Masala Noodles 280g,Packaged Foods,Nestle,48.00,56.00,Units,Snacks & Instant,true,270\n");
                sb.append("PROD_005,Fortune Sunlite Refined Sunflower Oil 1L,Edible Oils,Adani Wilmar,130.00,152.00,Litres,Cooking Essentials,true,270\n");
            }
            case CUSTOMER_REGISTRY -> {
                sb.append("CUST_0001,Rajesh Sharma,rajesh.sharma@example.com,+91 98200 12345,VIP,Mumbai,2023-01-15,38,Male\n");
                sb.append("CUST_0002,Priya Patel,priya.patel@example.com,+91 98200 23456,REGULAR,Ahmedabad,2023-03-22,29,Female\n");
                sb.append("CUST_0003,Amit Verma,amit.verma@example.com,+91 98200 34567,OCCASIONAL,Delhi,2023-06-10,45,Male\n");
                sb.append("CUST_0004,Sunita Rao,sunita.rao@example.com,+91 98200 45678,REGULAR,Bengaluru,2023-08-05,34,Female\n");
                sb.append("CUST_0005,Vikram Singh,vikram.singh@example.com,+91 98200 56789,NEW,Pune,2024-01-12,25,Male\n");
            }
            case SALES_TRANSACTIONS -> {
                sb.append("TXN_10001,2024-01-15 10:30:00,CUST_0001,PROD_001,2,74.00,0.00,148.00,UPI,STORE_MUM_01,Rohan K,COMPLETED\n");
                sb.append("TXN_10001,2024-01-15 10:30:00,CUST_0001,PROD_004,1,56.00,0.00,56.00,UPI,STORE_MUM_01,Rohan K,COMPLETED\n");
                sb.append("TXN_10002,2024-01-15 11:15:00,CUST_0002,PROD_002,1,245.00,10.00,235.00,CARD,STORE_MUM_01,Sneha P,COMPLETED\n");
                sb.append("TXN_10003,2024-01-15 12:00:00,CUST_0003,PROD_003,3,28.00,0.00,84.00,CASH,STORE_MUM_01,Rohan K,COMPLETED\n");
                sb.append("TXN_10003,2024-01-15 12:00:00,CUST_0003,PROD_005,1,152.00,5.00,147.00,CASH,STORE_MUM_01,Rohan K,COMPLETED\n");
            }
            case INVENTORY_STOCK -> {
                sb.append("INV_001,2024-01-15,PROD_001,50,30,25,55,68.00,20,WH_SECTION_A\n");
                sb.append("INV_002,2024-01-15,PROD_002,40,20,15,45,210.00,15,WH_SECTION_B\n");
                sb.append("INV_003,2024-01-15,PROD_003,80,50,30,100,22.00,30,WH_SECTION_C\n");
                sb.append("INV_004,2024-01-15,PROD_004,60,40,35,65,48.00,25,WH_SECTION_D\n");
                sb.append("INV_005,2024-01-15,PROD_005,35,25,20,40,130.00,10,WH_SECTION_E\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
