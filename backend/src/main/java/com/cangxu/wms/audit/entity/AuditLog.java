package com.cangxu.wms.audit.entity;

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
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String roleCode;

    private String module;

    private String action;

    private String target;

    private String ip;

    private String result;

    private String detail;

    private LocalDateTime createdAt;
}
