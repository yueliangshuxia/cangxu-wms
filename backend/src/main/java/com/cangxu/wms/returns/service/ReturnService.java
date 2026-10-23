package com.cangxu.wms.returns.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.outbound.mapper.OutboundOrderMapper;
import com.cangxu.wms.returns.dto.CreateReturnRequest;
import com.cangxu.wms.returns.dto.InspectReturnRequest;
import com.cangxu.wms.returns.dto.ReturnStats;
import com.cangxu.wms.returns.entity.ReturnOrder;
import com.cangxu.wms.returns.mapper.ReturnOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReturnService {

    private static final Set<String> DISPOSITIONS = Set.of("resell", "repair", "scrap");

    private final ReturnOrderMapper returnOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final AuditService auditService;

    public ReturnService(ReturnOrderMapper returnOrderMapper,
                         OutboundOrderMapper outboundOrderMapper,
                         AuditService auditService) {
        this.returnOrderMapper = returnOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.auditService = auditService;
    }

    public PageResult<ReturnOrder> page(long page, long size, String status, String keyword) {
        Page<ReturnOrder> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<ReturnOrder> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(ReturnOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(ReturnOrder::getNo, keyword)
                    .or().like(ReturnOrder::getCustomerName, keyword)
                    .or().like(ReturnOrder::getProductName, keyword)
                    .or().like(ReturnOrder::getSku, keyword));
        }
        query.orderByDesc(ReturnOrder::getId);
        Page<ReturnOrder> result = returnOrderMapper.selectPage(pageRequest, query);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ReturnStats summary() {
        List<ReturnOrder> all = returnOrderMapper.selectList(null);
        long total = all.size();
        long pending = all.stream().filter(o -> "PENDING_INSPECTION".equals(o.getStatus())).count();
        long ready = all.stream().filter(o -> "READY_TO_PUTAWAY".equals(o.getStatus())).count();
        long inStock = all.stream().filter(o -> "IN_STOCK".equals(o.getStatus())).count();
        long scrapped = all.stream().filter(o -> "SCRAPPED".equals(o.getStatus())).count();

        long returnQty = all.stream().mapToLong(o -> o.getQty() == null ? 0 : o.getQty()).sum();
        long outboundQty = outboundOrderMapper.selectList(null).stream()
                .mapToLong(o -> o.getTotalQty() == null ? 0 : o.getTotalQty()).sum();
        long resell = all.stream().filter(o -> "resell".equals(o.getDisposition())).count();
        long repair = all.stream().filter(o -> "repair".equals(o.getDisposition())).count();
        long scrap = all.stream().filter(o -> "scrap".equals(o.getDisposition())).count();

        Map<String, Long> reasons = new LinkedHashMap<>();
        for (ReturnOrder order : all) {
            String reason = StringUtils.hasText(order.getReason()) ? order.getReason() : "其他";
            reasons.merge(reason, 1L, Long::sum);
        }
        List<ReturnStats.ReasonCount> reasonCounts = reasons.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new ReturnStats.ReasonCount(entry.getKey(), entry.getValue()))
                .toList();

        return new ReturnStats(
                total, pending, ready, inStock, scrapped,
                percent(returnQty, outboundQty),
                percent(resell, total),
                percent(repair, total),
                percent(scrap, total),
                reasonCounts);
    }

    private double percent(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    @Transactional
    public ReturnOrder create(CreateReturnRequest request) {
        String no = "RT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%03d", returnOrderMapper.selectCount(null) + 1);
        ReturnOrder order = ReturnOrder.builder()
                .no(no)
                .customerName(request.customerName())
                .productName(request.productName())
                .sku(request.sku())
                .qty(request.qty())
                .reason(StringUtils.hasText(request.reason()) ? request.reason() : "其他")
                .status("PENDING_INSPECTION")
                .appliedAt(StringUtils.hasText(request.appliedAt())
                        ? request.appliedAt() : LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")))
                .createdAt(LocalDateTime.now())
                .build();
        returnOrderMapper.insert(order);
        auditService.logCurrent("RETURNS", "创建退货单", "return:" + no, "SUCCESS", order.getReason());
        return order;
    }

    @Transactional
    public ReturnOrder inspect(Long id, InspectReturnRequest request) {
        String disposition = request.disposition().toLowerCase();
        if (!DISPOSITIONS.contains(disposition)) {
            throw new BusinessException("不支持的处置结论：" + request.disposition());
        }
        ReturnOrder order = returnOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "退货单不存在");
        }
        order.setDisposition(disposition);
        order.setNote(request.note());
        order.setStatus(switch (disposition) {
            case "scrap" -> "SCRAPPED";
            case "repair" -> "IN_STOCK";
            default -> "READY_TO_PUTAWAY";
        });
        returnOrderMapper.updateById(order);
        auditService.logCurrent("RETURNS", "退货质检", "return:" + order.getNo(), "SUCCESS", disposition);
        return order;
    }
}
