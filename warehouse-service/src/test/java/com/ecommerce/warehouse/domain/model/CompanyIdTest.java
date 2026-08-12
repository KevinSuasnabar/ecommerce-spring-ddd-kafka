package com.ecommerce.warehouse.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyIdTest {

    @Test
    void wrapsAUuid() {
        UUID uuid = UUID.randomUUID();

        CompanyId companyId = new CompanyId(uuid);

        assertThat(companyId.value()).isEqualTo(uuid);
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new CompanyId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Company id must not be null");
    }

    @Test
    void equalValuesAreEqual() {
        UUID uuid = UUID.randomUUID();

        assertThat(new CompanyId(uuid)).isEqualTo(new CompanyId(uuid));
    }
}
