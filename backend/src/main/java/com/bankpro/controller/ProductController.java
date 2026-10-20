package com.bankpro.controller;

import com.bankpro.dto.ProductRequest;
import com.bankpro.dto.ProductResponse;
import com.bankpro.entity.ProductCategory;
import com.bankpro.security.CurrentUserProvider;
import com.bankpro.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The catalogue is small (a few dozen products), so the list is not paginated. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CurrentUserProvider currentUser;

    public ProductController(ProductService productService, CurrentUserProvider currentUser) {
        this.productService = productService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) Boolean active,
                                      @RequestParam(required = false) ProductCategory category,
                                      @RequestParam(required = false) String q) {
        return productService.list(currentUser.get(), active, category, q);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(currentUser.get(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request, HttpServletRequest http) {
        return productService.create(currentUser.get(), request, http.getRemoteAddr());
    }

    /** Also used to activate/deactivate (no DELETE: leads and sales keep referring to old products). */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request,
                                  HttpServletRequest http) {
        return productService.update(currentUser.get(), id, request, http.getRemoteAddr());
    }
}
