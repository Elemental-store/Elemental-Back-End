package com.elemental.backend.dto;

import java.time.LocalDateTime;

public class CategoryResponse {

    private Long          id;
    private Long          parentId;
    private String        name;
    private String        description;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public CategoryResponse(Long id, Long parentId, String name, String description,
                            LocalDateTime createdDate, LocalDateTime updatedDate) {
        this.id          = id;
        this.parentId    = parentId;
        this.name        = name;
        this.description = description;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public Long getParentId()                  { return parentId; }
    public void setParentId(Long parentId)     { this.parentId = parentId; }

    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }

    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }

    public LocalDateTime getCreatedDate()      { return createdDate; }
    public void setCreatedDate(LocalDateTime d){ this.createdDate = d; }

    public LocalDateTime getUpdatedDate()      { return updatedDate; }
    public void setUpdatedDate(LocalDateTime d){ this.updatedDate = d; }
}
