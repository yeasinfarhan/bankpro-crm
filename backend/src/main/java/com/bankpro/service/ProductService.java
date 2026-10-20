package com.bankpro.service;

import com.bankpro.audit.AuditAction;
import com.bankpro.audit.AuditService;
import com.bankpro.dto.ProductRequest;
import com.bankpro.dto.ProductResponse;
import com.bankpro.entity.BankingProduct;
import com.bankpro.entity.ProductCategory;
import com.bankpro.entity.Role;
import com.bankpro.exception.FieldConflictException;
import com.bankpro.exception.InvalidFieldException;
import com.bankpro.exception.NotFoundException;
import com.bankpro.mapper.ProductMapper;
import com.bankpro.repository.BankingProductRepository;
import com.bankpro.security.AuthenticatedUser;
import com.bankpro.validation.ContactNormalizer;
import com.bankpro.validation.SearchText;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Product catalogue. Everyone can browse active products; only ADMIN sees inactive ones and can change the catalogue
 * (enforced by @PreAuthorize in the controller AND re-checked here).
 */
@Service
@Transactional
public class ProductService {

    /** Rates are only meaningful (and only shown, as fictional samples) for deposit-type products. */
    private static final Set<ProductCategory> RATE_ALLOWED = EnumSet.of(ProductCategory.DEPOSIT, ProductCategory.ACCOUNT);

    private final BankingProductRepository products;
    private final AuditService audit;

    public ProductService(BankingProductRepository products, AuditService audit) {
        this.products = products;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(AuthenticatedUser caller, Boolean active, ProductCategory category, String q) {
        Boolean activeFilter = caller.role() == Role.ADMIN ? active : Boolean.TRUE;
        return products.search(activeFilter, category, SearchText.containsPattern(q)).stream()
                .map(ProductMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(AuthenticatedUser caller, Long id) {
        return ProductMapper.toResponse(findVisible(caller, id));
    }

    public ProductResponse create(AuthenticatedUser caller, ProductRequest req, String ip) {
        requireAdmin(caller);
        if (req.code() == null || req.code().isBlank()) {
            throw new InvalidFieldException("code", "Product code is required");
        }
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (products.existsByCode(code)) {
            throw new FieldConflictException("code", "A product with this code already exists.");
        }
        BankingProduct product = new BankingProduct(code);
        apply(product, req, true);
        products.save(product);
        audit.record(caller.id(), AuditAction.PRODUCT_CREATED, "PRODUCT", product.getId(), Map.of("code", code), ip);
        return ProductMapper.toResponse(product);
    }

    public ProductResponse update(AuthenticatedUser caller, Long id, ProductRequest req, String ip) {
        requireAdmin(caller);
        BankingProduct product = products.findById(id).orElseThrow(() -> new NotFoundException("Product not found."));
        boolean wasActive = product.isActive();
        apply(product, req, wasActive);
        audit.record(caller.id(), AuditAction.PRODUCT_UPDATED, "PRODUCT", id,
                Map.of("code", product.getCode(), "active", product.isActive()), ip);
        return ProductMapper.toResponse(product);
    }

    private void apply(BankingProduct product, ProductRequest req, boolean defaultActive) {
        if (req.illustrativeRate() != null && !RATE_ALLOWED.contains(req.category())) {
            throw new InvalidFieldException("illustrativeRate",
                    "Illustrative rates can only be set for account and deposit products.");
        }
        List<String> benefits = req.benefits() == null ? List.of()
                : req.benefits().stream().map(ContactNormalizer::clean).filter(Objects::nonNull).toList();
        product.update(ContactNormalizer.clean(req.name()), req.category(), req.description().trim(),
                ContactNormalizer.clean(req.eligibilityNotes()), benefits, req.illustrativeRate(),
                req.active() == null ? defaultActive : req.active());
    }

    private BankingProduct findVisible(AuthenticatedUser caller, Long id) {
        return products.findById(id)
                .filter(p -> p.isActive() || caller.role() == Role.ADMIN)
                .orElseThrow(() -> new NotFoundException("Product not found."));
    }

    private static void requireAdmin(AuthenticatedUser caller) {
        if (caller.role() != Role.ADMIN) {
            throw new com.bankpro.exception.ForbiddenException("Only administrators can change the product catalogue.");
        }
    }
}
