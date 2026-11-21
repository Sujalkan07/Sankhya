package com.sankhya.workspace.entity;

import com.sankhya.common.enums.WorkspaceRole;
import com.sankhya.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_workspace_user", columnNames = {"workspace_id", "user_id"})
}, indexes = {
        @Index(name = "idx_membership_user", columnList = "user_id"),
        @Index(name = "idx_membership_workspace", columnList = "workspace_id")
})
public class WorkspaceMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Workspace is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_workspace"))
    private Workspace workspace;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_user"))
    private User user;

    @NotNull(message = "Membership role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_role", nullable = false, length = 30)
    private WorkspaceRole membershipRole = WorkspaceRole.MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public WorkspaceMembership() {
    }

    public WorkspaceMembership(Workspace workspace, User user, WorkspaceRole membershipRole) {
        this.workspace = workspace;
        this.user = user;
        this.membershipRole = membershipRole != null ? membershipRole : WorkspaceRole.MEMBER;
    }

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public WorkspaceRole getMembershipRole() {
        return membershipRole;
    }

    public void setMembershipRole(WorkspaceRole membershipRole) {
        this.membershipRole = membershipRole;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
