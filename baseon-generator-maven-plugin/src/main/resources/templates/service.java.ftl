package ${servicePackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${entityName} 服务，由 baseon-generator 生成的骨架，可按业务需要修改。
 */
public interface ${entityName}Service {

    /** 创建并返回详情 */
    ${entityName}Resp create(${entityName}Req req);

    /** 按主键更新并返回最新详情 */
    ${entityName}Resp update(Long id, ${entityName}Req req);

    /** 按主键删除（逻辑删除行为由 starter-mybatis-plus 配置承载） */
    void deleteById(Long id);

    /** 按主键查询详情 */
    ${entityName}Resp getById(Long id);

    /** 全量列表 */
    List<${entityName}VO> list();

    /** 分页查询 */
    PageResult<${entityName}VO> page(PageQuery query);
}
