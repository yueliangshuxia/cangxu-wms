package com.cangxu.wms.device.entity;

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
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String type;

    private String location;

    private String status;

    private Integer battery;

    private String signalStrength;

    private LocalDateTime lastHeartbeat;
}
