package com.cangxu.wms.stocktake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cangxu.wms.audit.service.AuditService;
import com.cangxu.wms.common.BusinessException;
import com.cangxu.wms.common.PageResult;
import com.cangxu.wms.stocktake.dto.CreateStocktakeRequest;
import com.cangxu.wms.stocktake.entity.StocktakeItem;
import com.cangxu.wms.stocktake.entity.StocktakeTask;
import com.cangxu.wms.stocktake.mapper.StocktakeItemMapper;
import com.cangxu.wms.stocktake.mapper.StocktakeTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class StocktakeService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final StocktakeTaskMapper taskMapper;
    private final StocktakeItemMapper itemMapper;
    private final AuditService auditService;

    public StocktakeService(StocktakeTaskMapper taskMapper,
                            StocktakeItemMapper itemMapper,
                            AuditService auditService) {
        this.taskMapper = taskMapper;
        this.itemMapper = itemMapper;
        this.auditService = auditService;
    }

    public PageResult<StocktakeTask> page(long page, long size, String status, String keyword) {
        Page<StocktakeTask> pageRequest = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<StocktakeTask> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(StocktakeTask::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(StocktakeTask::getNo, keyword)
                    .or().like(StocktakeTask::getScope, keyword)
                    .or().like(StocktakeTask::getOwner, keyword));
        }
        query.orderByDesc(StocktakeTask::getId);
        Page<StocktakeTask> result = taskMapper.selectPage(pageRequest, query);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Map<String, Long> summary() {
        return Map.of(
                "total", taskMapper.selectCount(null),
                "pending", taskMapper.selectCount(new LambdaQueryWrapper<StocktakeTask>().eq(StocktakeTask::getStatus, "PENDING")),
                "inProgress", taskMapper.selectCount(new LambdaQueryWrapper<StocktakeTask>().eq(StocktakeTask::getStatus, "IN_PROGRESS")),
                "done", taskMapper.selectCount(new LambdaQueryWrapper<StocktakeTask>().eq(StocktakeTask::getStatus, "DONE"))
        );
    }

    public List<StocktakeItem> items(Long taskId) {
        getTask(taskId);
        return itemMapper.selectList(new LambdaQueryWrapper<StocktakeItem>()
                .eq(StocktakeItem::getTaskId, taskId)
                .orderByAsc(StocktakeItem::getId));
    }

    public List<StocktakeItem> diffs() {
        return itemMapper.selectList(new LambdaQueryWrapper<StocktakeItem>()
                .ne(StocktakeItem::getDiffQty, 0)
                .orderByAsc(StocktakeItem::getId));
    }

    @Transactional
    public StocktakeTask create(CreateStocktakeRequest request) {
        String no = "CC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%03d", taskMapper.selectCount(null) + 1);
        int total = request.scope().contains("全仓") ? 1280
                : request.scope().startsWith("D") ? 60
                : request.scope().startsWith("E") ? 40 : 120;
        StocktakeTask task = StocktakeTask.builder()
                .no(no)
                .scope(request.scope())
                .type(StringUtils.hasText(request.type()) ? request.type() : "循环盘点")
                .owner(StringUtils.hasText(request.owner()) ? request.owner() : "周可")
                .status("PENDING")
                .total(total)
                .counted(0)
                .diffCount(0)
                .startedAt(StringUtils.hasText(request.plannedAt())
                        ? request.plannedAt() : LocalDateTime.now().plusHours(2).format(DISPLAY_TIME))
                .createdAt(LocalDateTime.now())
                .build();
        taskMapper.insert(task);
        auditService.logCurrent("STOCKTAKE", "创建盘点任务", "stocktake:" + no, "SUCCESS", task.getScope());
        return task;
    }

    public StocktakeTask start(Long id) {
        StocktakeTask task = getTask(id);
        if ("DONE".equals(task.getStatus())) {
            throw new BusinessException("已完成的盘点任务不能重新开始");
        }
        task.setStatus("IN_PROGRESS");
        task.setStartedAt(LocalDateTime.now().format(DISPLAY_TIME));
        taskMapper.updateById(task);
        auditService.logCurrent("STOCKTAKE", "开始盘点", "stocktake:" + task.getNo(), "SUCCESS", null);
        return task;
    }

    public StocktakeTask scan(Long id, String code) {
        StocktakeTask task = getTask(id);
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new BusinessException("只有进行中的盘点任务可以扫码");
        }
        int counted = Math.min(task.getTotal(), task.getCounted() + 1);
        task.setCounted(counted);
        if (counted >= task.getTotal()) {
            task.setStatus("DONE");
        }
        taskMapper.updateById(task);
        auditService.logCurrent("STOCKTAKE", "扫码盘点", "stocktake:" + task.getNo(), "SUCCESS",
                StringUtils.hasText(code) ? code : null);
        return task;
    }

    private StocktakeTask getTask(Long id) {
        StocktakeTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "盘点任务不存在");
        }
        return task;
    }
}
