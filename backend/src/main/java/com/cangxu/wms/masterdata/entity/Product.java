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
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sku;

    private String name;

    private String category;

    private String spec;

    private String unit;

    private String tempType;

    private Integer safetyStock;

    private String status;

    private String imageUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
