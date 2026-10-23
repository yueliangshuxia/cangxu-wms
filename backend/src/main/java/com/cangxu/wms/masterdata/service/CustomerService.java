package com.cangxu.wms.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.masterdata.dto.CustomerRequest;
import com.cangxu.wms.masterdata.entity.Customer;
import com.cangxu.wms.masterdata.mapper.CustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final AuditService auditService;

    public CustomerService(CustomerMapper customerMapper, AuditService auditService) {
        this.customerMapper = customerMapper;
        this.auditService = auditService;
    }

    public PageResult<Customer> page(long page, long size, String keyword, String status) {
        Page<Customer> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<Customer> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(Customer::getCode, keyword)
                    .or().like(Customer::getName, keyword)
                    .or().like(Customer::getContact, keyword));
        }
        if (StringUtils.hasText(status)) {
            query.eq(Customer::getStatus, status);
        }
        query.orderByAsc(Customer::getCode);
        Page<Customer> result = customerMapper.selectPage(pageRequest, query);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Customer get(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return customer;
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        ensureCodeFree(request.code(), null);
        Customer customer = toEntity(request);
        customerMapper.insert(customer);
        auditService.logCurrent("MASTERDATA", "创建客户", "customer:" + customer.getCode(), "SUCCESS", customer.getName());
        return customer;
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        get(id);
        ensureCodeFree(request.code(), id);
        Customer customer = toEntity(request);
        customer.setId(id);
        customerMapper.updateById(customer);
        auditService.logCurrent("MASTERDATA", "更新客户", "customer:" + customer.getCode(), "SUCCESS", customer.getName());
        return customer;
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = get(id);
        customerMapper.deleteById(id);
        auditService.logCurrent("MASTERDATA", "删除客户", "customer:" + customer.getCode(), "WARN", customer.getName());
    }

    private void ensureCodeFree(String code, Long excludeId) {
        Long count = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCode, code)
                .ne(excludeId != null, Customer::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "客户编码已存在：" + code);
        }
    }

    private Customer toEntity(CustomerRequest request) {
        return Customer.builder()
                .code(request.code())
                .name(request.name())
                .channel(request.channel())
                .contact(request.contact())
                .phone(request.phone())
                .status(request.status())
                .build();
    }
}
