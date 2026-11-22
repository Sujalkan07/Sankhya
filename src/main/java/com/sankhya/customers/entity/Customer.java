package com.sankhya.customers.entity;

import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.workspace.entity.Workspace;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workspace_customer_id", columnNames = {"workspace_id", "customer_id"})
        },
        indexes = {
                @Index(name = "idx_customer_workspace", columnList = "workspace_id"),
                @Index(name = "idx_customer_code", columnList = "customer_id"),
                @Index(name = "idx_customer_segment", columnList = "customer_segment")
        })
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "customer_segment", length = 50)
    private String customerSegment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_import_id")
    private DatasetImport datasetImport;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Customer() {}

    public Customer(Workspace workspace, String customerId) {
        this.workspace = workspace;
        this.customerId = customerId;
    }

    public Customer(Workspace workspace, Dataset dataset, String customerId) {
        this.workspace = workspace;
        this.dataset = dataset;
        this.customerId = customerId;
    }

    public Customer(Workspace workspace, String customerId, String customerName, String gender,
                    Integer age, String city, LocalDate registrationDate, String customerSegment,
                    DatasetImport datasetImport) {
        this.workspace = workspace;
        this.customerId = customerId;
        this.customerName = customerName;
        this.gender = gender;
        this.age = age;
        this.city = city;
        this.registrationDate = registrationDate;
        this.customerSegment = customerSegment;
        this.datasetImport = datasetImport;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
    }

    public String getSegment() {
        return customerSegment;
    }

    public void setSegment(String segment) {
        this.customerSegment = segment;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetImport getDatasetImport() {
        return datasetImport;
    }

    public void setDatasetImport(DatasetImport datasetImport) {
        this.datasetImport = datasetImport;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
