package ${reqPackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${entityName} 创建/更新入参，由 baseon-generator 生成的骨架，可按业务需要修改。
 *
 * <p>仅承载可写业务字段；审计字段由服务端自动维护，不允许前端传入。
 */
public class ${entityName}Req {
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
