package org.ngolibrary.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class BookCategoryRequest {
    @NotBlank
    @Size(max = 20)
    private String categoryNumber;
    @NotBlank
    @Size(max = 100)
    private String name;

    public String getCategoryNumber() {
        return categoryNumber;
    }

    public void setCategoryNumber(String categoryNumber) {
        this.categoryNumber = categoryNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
