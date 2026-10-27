package io.github.ayongbox.baseon.starter.swagger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger/OpenAPI 文档配置，前缀 {@code baseon.swagger}。
 */
@ConfigurationProperties(prefix = "baseon.swagger")
public class BaseonSwaggerProperties {

    /**
     * 总开关：false 时不装配 OpenAPI 定制 Bean。默认 true。
     *
     * <p>注意：该开关只管本模块的定制 Bean，不管 springdoc 端点本身；
     * 生产环境的端点硬关闭由 {@link ProdProfileSwaggerDisabler} 兜底。
     */
    private boolean enabled = true;

    /**
     * 文档标题。默认 "Baseon API"。
     */
    private String title = "Baseon API";

    /**
     * 文档描述。
     */
    private String description = "Baseon 脚手架接口文档";

    /**
     * 文档版本号。默认 "1.0.0"。
     */
    private String version = "1.0.0";

    /**
     * Swagger UI 中 Servers 下拉项的说明文字，用于替代 springdoc 自动生成的
     * "Generated server url"。留空时取 {@code spring.application.name}，
     * 多服务聚合场景下便于辨认当前文档属于哪个服务。
     */
    private String serverDescription;

    /**
     * 生产环境 profile 名单：激活 profile 与该列表存在交集时，
     * 定制层不装配 OpenAPI Bean，端点层强制关闭 api-docs 与 swagger-ui。
     * 默认 ["prod"]，可按企业实际追加 "production"、"pre" 等。
     */
    private List<String> prodProfiles = new ArrayList<>(List.of("prod"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getProdProfiles() {
        return prodProfiles;
    }

    public void setProdProfiles(List<String> prodProfiles) {
        this.prodProfiles = prodProfiles;
    }

    public String getServerDescription() {
        return serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }
}
