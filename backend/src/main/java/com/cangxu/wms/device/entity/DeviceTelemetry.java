package com.cangxu.wms.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device_telemetry")
public class DeviceTelemetry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceCode;

    private String metric;

    @TableField("metric_value")
    private BigDecimal metricValue;

    private String unit;

    private LocalDateTime collectedAt;
}
