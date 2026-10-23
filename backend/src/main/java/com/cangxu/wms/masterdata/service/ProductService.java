package com.cangxu.wms.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.masterdata.dto.ProductRequest;
import com.cangxu.wms.masterdata.entity.Product;
import com.cangxu.wms.masterdata.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final AuditService auditService;

    public ProductService(ProductMapper productMapper, AuditService auditService) {
        this.productMapper = productMapper;
        this.auditService = auditService;
    }

    public PageResult<Product> page(long page, long size, String keyword, String category, String status) {
        Page<Product> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(Product::getSku, keyword)
                    .or().like(Product::getName, keyword)
                    .or().like(Product::getSpec, keyword));
        }
        if (StringUtils.hasText(category)) {
            query.eq(Product::getCategory, category);
        }
        if (StringUtils.hasText(status)) {
            query.eq(Product::getStatus, status);
        }
        query.orderByDesc(Product::getUpdatedAt);
        Page<Product> result = productMapper.selectPage(pageRequest, query);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Product get(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return product;
    }

    @Transactional
    public Product create(ProductRequest request) {
        ensureSkuFree(request.sku(), null);
        Product product = toEntity(request);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(product);
        auditService.logCurrent("MASTERDATA", "创建商品", "product:" + product.getSku(), "SUCCESS", product.getName());
        return product;
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product existing = get(id);
        ensureSkuFree(request.sku(), id);
        Product product = toEntity(request);
        product.setId(id);
        product.setCreatedAt(existing.getCreatedAt());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        auditService.logCurrent("MASTERDATA", "更新商品", "product:" + product.getSku(), "SUCCESS", product.getName());
        return product;
    }

    @Transactional
    public void delete(Long id) {
        Product product = get(id);
        productMapper.deleteById(id);
        auditService.logCurrent("MASTERDATA", "删除商品", "product:" + product.getSku(), "WARN", product.getName());
    }

    private void ensureSkuFree(String sku, Long excludeId) {
        Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSku, sku)
                .ne(excludeId != null, Product::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "SKU 已存在：" + sku);
        }
    }

    private Product toEntity(ProductRequest request) {
        return Product.builder()
                .sku(request.sku())
                .name(request.name())
                .category(request.category())
                .spec(request.spec())
                .unit(request.unit())
                .tempType(request.tempType())
                .safetyStock(request.safetyStock())
                .status(request.status())
                .imageUrl(request.imageUrl())
                .build();
    }
}
