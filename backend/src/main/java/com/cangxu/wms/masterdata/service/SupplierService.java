package com.cangxu.wms.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.masterdata.dto.SupplierRequest;
import com.cangxu.wms.masterdata.entity.Supplier;
import com.cangxu.wms.masterdata.mapper.SupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final AuditService auditService;

    public SupplierService(SupplierMapper supplierMapper, AuditService auditService) {
        this.supplierMapper = supplierMapper;
        this.auditService = auditService;
    }

    public PageResult<Supplier> page(long page, long size, String keyword, String status) {
        Page<Supplier> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<Supplier> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(Supplier::getCode, keyword)
                    .or().like(Supplier::getName, keyword)
                    .or().like(Supplier::getContact, keyword));
        }
        if (StringUtils.hasText(status)) {
            query.eq(Supplier::getStatus, status);
        }
        query.orderByAsc(Supplier::getCode);
        Page<Supplier> result = supplierMapper.selectPage(pageRequest, query);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Supplier get(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        return supplier;
    }

    @Transactional
    public Supplier create(SupplierRequest request) {
        ensureCodeFree(request.code(), null);
        Supplier supplier = toEntity(request);
        supplierMapper.insert(supplier);
        auditService.logCurrent("MASTERDATA", "创建供应商", "supplier:" + supplier.getCode(), "SUCCESS", supplier.getName());
        return supplier;
    }

    @Transactional
    public Supplier update(Long id, SupplierRequest request) {
        get(id);
        ensureCodeFree(request.code(), id);
        Supplier supplier = toEntity(request);
        supplier.setId(id);
        supplierMapper.updateById(supplier);
        auditService.logCurrent("MASTERDATA", "更新供应商", "supplier:" + supplier.getCode(), "SUCCESS", supplier.getName());
        return supplier;
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = get(id);
        supplierMapper.deleteById(id);
        auditService.logCurrent("MASTERDATA", "删除供应商", "supplier:" + supplier.getCode(), "WARN", supplier.getName());
    }

    private void ensureCodeFree(String code, Long excludeId) {
        Long count = supplierMapper.selectCount(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getCode, code)
                .ne(excludeId != null, Supplier::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "供应商编码已存在：" + code);
        }
    }

    private Supplier toEntity(SupplierRequest request) {
        return Supplier.builder()
                .code(request.code())
                .name(request.name())
                .contact(request.contact())
                .phone(request.phone())
                .category(request.category())
                .status(request.status())
                .build();
    }
}
