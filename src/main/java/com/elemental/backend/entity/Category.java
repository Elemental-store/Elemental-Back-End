package com.elemental.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "category")
public class Category {

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 50)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

    }
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public LocalDateTime getCreateDate() {
        return createdAt;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createdAt = createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updatedAt;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updatedAt = updateDate;
    }

    public Category getParent() { return parent; }

    public void setParent(Category parent) { this.parent = parent; }
}
