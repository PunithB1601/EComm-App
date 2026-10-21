package com.ecomm.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name="roles")
public class Role {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
    @Enumerated(EnumType.STRING) @Column(nullable=false, unique=true, length=30)
    private RoleName name;

    public RoleName getName() { return name; }
    public void setName(RoleName name) { this.name = name; }
}
