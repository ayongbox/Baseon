package ${converterPackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${entityName} 对象转换，由 baseon-generator 生成的骨架，可按业务需要修改。
 */
@Mapper(componentModel = "spring")
public interface ${entityName}Converter {

    /** 入参转实体 */
    ${entityName} toEntity(${entityName}Req req);

    /** 实体转列表视图 */
    ${entityName}VO toVO(${entityName} entity);

    /** 实体转详情响应 */
    ${entityName}Resp toResp(${entityName} entity);

    /** 实体列表转视图列表 */
    List<${entityName}VO> toVOList(List<${entityName}> entityList);
}
