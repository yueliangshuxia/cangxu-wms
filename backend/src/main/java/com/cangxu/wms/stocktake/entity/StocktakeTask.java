package com.cangxu.wms.stocktake.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("stocktake_task")
public class StocktakeTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String no;

    private String scope;

    private String type;

    private String owner;

    private String status;

    private Integer total;

    private Integer counted;

    private Integer diffCount;

    private String startedAt;

    private LocalDateTime createdAt;
}
