package ${mapperPackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${entityName} Mapper，由 baseon-generator 生成的骨架，可按业务需要修改。
 */
@Mapper
public interface ${entityName}Mapper extends BaseMapper<${entityName}> {
}
