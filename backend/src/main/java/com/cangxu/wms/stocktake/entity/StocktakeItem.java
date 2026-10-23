package com.cangxu.wms.stocktake.entity;

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
@TableName("stocktake_item")
public class StocktakeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String sku;

    private String productName;

    private String locationCode;

    private Integer bookQty;

    private Integer actualQty;

    private Integer diffQty;

    private String reason;

    private String handleAction;
}
