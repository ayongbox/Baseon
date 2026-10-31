package io.github.ayongbox.baseon.starter.datascope.it;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 集成测试 Mapper：继承 MP BaseMapper，获得 selectList 等通用查询。
 */
@Mapper
public interface DataScopeDemoMapper extends BaseMapper<DataScopeDemoEntity> {
}
