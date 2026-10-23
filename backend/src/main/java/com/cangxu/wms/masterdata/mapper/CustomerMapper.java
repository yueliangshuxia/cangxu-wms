package com.cangxu.wms.masterdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.cangxu.wms.masterdata.entity.Customer;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
