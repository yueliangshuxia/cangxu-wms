package com.cangxu.wms.inbound.entity;

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
@TableName("inbound_order")
public class InboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String no;

    private String supplierName;

    private String dock;

    private LocalDateTime eta;

    private String status;

    private String operator;

    private LocalDateTime createdAt;
}
