package io.github.ayongbox.baseon.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ayongbox.baseon.demo.entity.TenantUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantUserMapper extends BaseMapper<TenantUser> {
}
