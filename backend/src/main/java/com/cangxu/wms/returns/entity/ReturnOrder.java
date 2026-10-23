package com.cangxu.wms.returns.entity;

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
@TableName("return_order")
public class ReturnOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String no;

    private String customerName;

    private String productName;

    private String sku;

    private Integer qty;

    private String reason;

    private String status;

    private String appliedAt;

    private String disposition;

    private String note;

    private LocalDateTime createdAt;
}
