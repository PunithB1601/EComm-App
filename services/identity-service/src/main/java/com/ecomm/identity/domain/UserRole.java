package com.ecomm.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name="user_roles")
public class UserRole {
    @EmbeddedId
    private UserRoleId id;

    public UserRoleId getId() { return id; }
    public void setId(UserRoleId id) { this.id = id; }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @MapsId("userId") @JoinColumn(name="user_id")
    private User user;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @MapsId("roleId") @JoinColumn(name="role_id")
    private Role role;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
