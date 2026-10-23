package com.cangxu.wms.masterdata.entity;

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
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long locationId;

    private Integer qty;

    private Integer lockedQty;

    private LocalDateTime updatedAt;
}
