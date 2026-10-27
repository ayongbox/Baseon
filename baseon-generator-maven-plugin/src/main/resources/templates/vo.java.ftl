package ${voPackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${entityName} 列表视图，由 baseon-generator 生成的骨架，可按业务需要修改。
 */
public class ${entityName}VO {

    /** 主键 */
    private Long id;
<#list entityFields as field>

<#if field.comment?? && field.comment?has_content>
    /** ${field.comment} */
</#if>
    private ${field.javaType} ${field.propertyName};
</#list>

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
<#list entityFields as field>

    public ${field.javaType} get${field.propertyName?cap_first}() {
        return ${field.propertyName};
    }

    public void set${field.propertyName?cap_first}(${field.javaType} ${field.propertyName}) {
        this.${field.propertyName} = ${field.propertyName};
    }
</#list>
}
