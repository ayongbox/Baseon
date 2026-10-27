package io.github.ayongbox.baseon.generator.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NamingConverterTest {

    @Test
    void should_strip_t_prefix_and_convert_to_upper_camel() {
        assertEquals("TenantUser", NamingConverter.toEntityName("t_tenant_user"));
    }

    @Test
    void should_strip_tb_prefix_and_convert_to_upper_camel() {
        assertEquals("Order", NamingConverter.toEntityName("tb_order"));
    }

    @Test
    void should_convert_plain_table_name_to_upper_camel() {
        assertEquals("User", NamingConverter.toEntityName("user"));
        assertEquals("OrderItem", NamingConverter.toEntityName("order_item"));
    }

    @Test
    void should_convert_column_name_to_lower_camel() {
        assertEquals("createTime", NamingConverter.toPropertyName("create_time"));
        assertEquals("id", NamingConverter.toPropertyName("id"));
        assertEquals("tenantId", NamingConverter.toPropertyName("tenant_id"));
    }

    @Test
    void should_convert_entity_name_to_variable_name() {
        assertEquals("tenantUser", NamingConverter.toVariableName("TenantUser"));
        assertEquals("dict", NamingConverter.toVariableName("Dict"));
    }

    @Test
    void should_convert_entity_name_to_request_path() {
        assertEquals("tenant-users", NamingConverter.toRequestPath("TenantUser"));
        assertEquals("dicts", NamingConverter.toRequestPath("Dict"));
    }

    @Test
    void should_reject_blank_snake_name() {
        assertThrows(IllegalArgumentException.class, () -> NamingConverter.toPropertyName(" "));
    }
}
