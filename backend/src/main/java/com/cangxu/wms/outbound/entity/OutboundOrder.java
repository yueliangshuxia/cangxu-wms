package com.cangxu.wms.outbound.entity;

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
@TableName("outbound_order")
public class OutboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String no;

    private String customerName;

    private String priority;

    private String waveNo;

    private String status;

    private Integer itemCount;

    private Integer totalQty;

    private String promisedAt;

    private LocalDateTime createdAt;
}
