package com.cangxu.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("outbound_item")
public class OutboundItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    private String sku;

    private String productName;

    private Integer qty;

    private Long locationId;

    private String locationCode;
}
