package com.paoperez.categoryservice;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.hamcrest.Matchers.containsString;

import java.util.Collection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest
public class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService service;

    private String validId;
    private String validName;
    private Category validCategory;
    private String validCategoryStr;
    private String blankId;
    private String blankName;
    private Category blankCategory;
    private String nonExistingId;

    @BeforeEach
    void init() throws Exception {
        validId = "A";
        validName = "Blog";
        validCategory = Category.builder().id(validId).name(validName).build();
        validCategoryStr = objectMapper.writeValueAsString(validCategory);
        blankId = " ";
        blankName = " ";
        blankCategory = Category.builder().name(blankName).build();
        nonExistingId = "Z";
    }

    @AfterEach
    void cleanup() {
    }

    @Test
    public void getAllCategoriesShouldReturnOk() throws Exception {
        final Category otherCategory = Category.builder().id("B").name("Tutorial").build();
        final Collection<Category> categories = ImmutableList.of(validCategory, otherCategory);
        when(service.getAllCategories()).thenReturn(categories);

        this.mockMvc.perform(get("/categories").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(categories)));

        verify(service, times(1)).getAllCategories();
    }

    @Test
    public void getValidCategoryShouldReturnOk() throws Exception {
        when(service.getCategory(validId)).thenReturn(validCategory);

        this.mockMvc.perform(get("/categories/{id}", validId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().string(validCategoryStr));

        verify(service, times(1)).getCategory(validId);
    }

    @Test
    public void getNonexistingCategoryIdShouldReturnNotFound() throws Exception {
        when(service.getCategory(nonExistingId)).thenThrow(new CategoryNotFoundException(nonExistingId));

        this.mockMvc.perform(get("/categories/{id}", nonExistingId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("Category with id Z not found."));

        verify(service, times(1)).getCategory(nonExistingId);
    }

    @Test
    public void getBlankCategoryIdShouldReturnBadRequest() throws Exception {
        this.mockMvc.perform(get("/categories/{id}", blankId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value(containsString("must not be blank")));

        verify(service, times(0)).getCategory(blankId);
    }

    @Test
    public void createValidCategoryShouldReturnCreated() throws Exception {
        final Category newCategory = Category.builder().name(validName).build();
        final String createdLocation = "http://localhost/categories/" + validId;
        when(service.createCategory(newCategory)).thenReturn(validCategory);

        this.mockMvc
                .perform(post("/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated()).andExpect(content().string(validCategoryStr))
                .andExpect(header().string(LOCATION, createdLocation));

        verify(service, times(1)).createCategory(newCategory);
    }

    @Test
    public void createExistingCategoryShouldReturnConflict() throws Exception {
        final Category existingCategory = Category.builder().name(validName).build();
        when(service.createCategory(existingCategory)).thenThrow(new CategoryAlreadyExistsException(validName));

        this.mockMvc
                .perform(post("/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingCategory)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.name()))
                .andExpect(jsonPath("$.message").value("Category with name Blog already exists."));

        verify(service, times(1)).createCategory(existingCategory);
    }

    @Test
    public void createBlankCategoryShouldReturnBadRequest() throws Exception {
        this.mockMvc
                .perform(post("/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankCategory)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value(containsString("name must not be blank")));

        verify(service, times(0)).createCategory(blankCategory);
    }

    @Test
    public void updateValidCategoryShouldReturnNoContent() throws Exception {
        this.mockMvc.perform(
                put("/categories/{id}", validId).contentType(MediaType.APPLICATION_JSON).content(validCategoryStr))
                .andExpect(status().isNoContent());

        verify(service, times(1)).updateCategory(validId, validCategory);
    }

    @Test
    public void updateBlankCategoryShouldReturnBadRequest() throws Exception {
        this.mockMvc
                .perform(put("/categories/{id}", validId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankCategory)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value(containsString("name must not be blank")));

        verify(service, times(0)).updateCategory(validId, blankCategory);
    }

    @Test
    public void updateNonexistingCategoryShouldReturnNotFound() throws Exception {
        final Category nonExistingCategory = Category.builder().id(nonExistingId).name(validName).build();
        doThrow(new CategoryNotFoundException(nonExistingId)).when(service).updateCategory(nonExistingId,
                nonExistingCategory);

        this.mockMvc
                .perform(put("/categories/{id}", nonExistingId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistingCategory)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("Category with id Z not found."));

        verify(service, times(1)).updateCategory(nonExistingId, nonExistingCategory);
    }

    @Test
    public void deleteValidCategoryShouldReturnNoContent() throws Exception {
        this.mockMvc.perform(delete("/categories/{id}", validId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteCategory(validId);
    }

    @Test
    public void deleteNonexistingCategoryShouldReturnNotFound() throws Exception {
        doThrow(new CategoryNotFoundException(nonExistingId)).when(service).deleteCategory(nonExistingId);

        this.mockMvc.perform(delete("/categories/{id}", nonExistingId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("Category with id Z not found."));

        verify(service, times(1)).deleteCategory(nonExistingId);
    }

    @Test
    public void deleteBlankCategoryShouldReturnBadRequest() throws Exception {
        this.mockMvc.perform(delete("/categories/{id}", blankId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.message").value(containsString("must not be blank")));

        verify(service, times(0)).deleteCategory(blankId);
    }
}
