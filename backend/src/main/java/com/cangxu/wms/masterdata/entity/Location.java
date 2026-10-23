package com.cangxu.wms.masterdata.entity;

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
@TableName("location")
public class Location {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long zoneId;

    private String code;

    private Integer rowNo;

    private Integer colNo;

    private Integer capacity;

    private String status;

    private String currentSku;
}
