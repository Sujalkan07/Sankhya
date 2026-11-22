package com.sankhya.workspace.dto;

import com.sankhya.common.enums.WorkspaceRole;
import com.sankhya.workspace.entity.Workspace;

import java.time.LocalDateTime;

public class WorkspaceResponse {

    private Long id;
    private String name;
    private String description;
    private String storeFormat;
    private String city;
    private String state;
    private String country;
    private String onboardingStatus;
    private Long ownerId;
    private String ownerName;
    private boolean isOwner;
    private WorkspaceRole userRole;
    private LocalDateTime createdAt;
    private boolean active;

    public WorkspaceResponse() {
    }

    public WorkspaceResponse(Long id, String name, String description, Long ownerId, String ownerName,
                             boolean isOwner, WorkspaceRole userRole, LocalDateTime createdAt, boolean active) {
        this(id, name, description, "Supermarket", null, null, "India", "COMPLETED",
                ownerId, ownerName, isOwner, userRole, createdAt, active);
    }

    public WorkspaceResponse(Long id, String name, String description, String storeFormat, String city,
                             String state, String country, String onboardingStatus, Long ownerId,
                             String ownerName, boolean isOwner, WorkspaceRole userRole,
                             LocalDateTime createdAt, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.storeFormat = storeFormat;
        this.city = city;
        this.state = state;
        this.country = country;
        this.onboardingStatus = onboardingStatus;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.isOwner = isOwner;
        this.userRole = userRole;
        this.createdAt = createdAt;
        this.active = active;
    }

    public static WorkspaceResponse fromEntity(Workspace workspace, Long currentUserId, Long activeWorkspaceId) {
        if (workspace == null) {
            return null;
        }
        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(currentUserId);
        boolean isActive = activeWorkspaceId != null && workspace.getId().equals(activeWorkspaceId);

        WorkspaceRole role = isOwner ? WorkspaceRole.OWNER :
                workspace.getMemberships().stream()
                        .filter(m -> m.getUser().getId().equals(currentUserId))
                        .map(m -> m.getMembershipRole())
                        .findFirst()
                        .orElse(WorkspaceRole.VIEWER);

        String ownerName = workspace.getOwner() != null ? workspace.getOwner().getFullName() : "Unknown";

        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getStoreFormat(),
                workspace.getCity(),
                workspace.getState(),
                workspace.getCountry(),
                workspace.getOnboardingStatus(),
                workspace.getOwner() != null ? workspace.getOwner().getId() : null,
                ownerName,
                isOwner,
                role,
                workspace.getCreatedAt(),
                isActive
        );
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

    public String getStoreFormat() {
        return storeFormat;
    }

    public void setStoreFormat(String storeFormat) {
        this.storeFormat = storeFormat;
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
        return onboardingStatus;
    }

    public void setOnboardingStatus(String onboardingStatus) {
        this.onboardingStatus = onboardingStatus;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public WorkspaceRole getUserRole() {
        return userRole;
    }

    public void setUserRole(WorkspaceRole userRole) {
        this.userRole = userRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
