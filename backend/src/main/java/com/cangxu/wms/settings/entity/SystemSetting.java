package com.cangxu.wms.settings.entity;

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
@TableName("system_setting")
public class SystemSetting {

    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    private String settingValue;

    private String groupName;
}
