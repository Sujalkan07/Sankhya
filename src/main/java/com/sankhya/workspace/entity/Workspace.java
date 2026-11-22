package com.sankhya.workspace.entity;

import com.sankhya.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workspaces", indexes = {
        @Index(name = "idx_workspace_owner", columnList = "owner_id")
})
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Workspace name is required")
    @Size(min = 3, max = 100, message = "Workspace name must be between 3 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "store_format", length = 50)
    private String storeFormat = "Supermarket";

    @Column(name = "store_key", length = 50)
    private String storeKey = "STORE-001";

    @Column(name = "owner_name", length = 150)
    private String ownerName = "Sujal Kansal";

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country = "India";

    @Column(name = "onboarding_status", length = 50)
    private String onboardingStatus = "COMPLETED";

    @NotNull(message = "Workspace owner is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workspace_owner"))
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMembership> memberships = new ArrayList<>();

    public Workspace() {
    }

    public Workspace(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
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

    public void addMembership(WorkspaceMembership membership) {
        memberships.add(membership);
        membership.setWorkspace(this);
    }

    public void removeMembership(WorkspaceMembership membership) {
        memberships.remove(membership);
        membership.setWorkspace(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStoreFormat() {
        return storeFormat != null ? storeFormat : "Supermarket";
    }

    public void setStoreFormat(String storeFormat) {
        this.storeFormat = storeFormat;
    }

    public String getStoreKey() {
        return storeKey != null ? storeKey : "STORE-001";
    }

    public void setStoreKey(String storeKey) {
        this.storeKey = storeKey;
    }

    public String getOwnerName() {
        if (ownerName != null && !ownerName.isBlank()) {
            return ownerName;
        }
        return owner != null ? owner.getFullName() : "Sujal Kansal";
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getOnboardingStatus() {
        return onboardingStatus != null ? onboardingStatus : "COMPLETED";
    }

    public void setOnboardingStatus(String onboardingStatus) {
        this.onboardingStatus = onboardingStatus;
    }

    public boolean isOnboardingCompleted() {
        return "COMPLETED".equalsIgnoreCase(this.onboardingStatus);
    }

    public List<WorkspaceMembership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<WorkspaceMembership> memberships) {
        this.memberships = memberships;
    }
}
