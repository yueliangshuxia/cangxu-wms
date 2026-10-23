package com.cangxu.wms.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.cangxu.wms.audit.entity.AuditLog;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
