package ${entityPackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${tableComment?has_content?then(tableComment, tableName)} 实体。
 *
 * <p>由 baseon-generator 生成的骨架，主键、<#if tenantTable>租户、</#if>创建/更新人与时间、
 * 逻辑删除标记等审计字段继承自 ${entityParent}，可按业务需要修改。
 */
@TableName("${tableName}")
public class ${entityName} extends ${entityParent} {
<#list entityFields as field>

<#if field.comment?? && field.comment?has_content>
    /** ${field.comment} */
</#if>
    private ${field.javaType} ${field.propertyName};
</#list>
<#list entityFields as field>

    public ${field.javaType} get${field.propertyName?cap_first}() {
        return ${field.propertyName};
    }

    public void set${field.propertyName?cap_first}(${field.javaType} ${field.propertyName}) {
        this.${field.propertyName} = ${field.propertyName};
    }
</#list>
}
