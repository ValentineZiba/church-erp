package com.churchos.church_erp.tenant.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsNullWhenNothingSet() {
        assertThat(TenantContext.getCurrentTenantSlug()).isNull();
    }

    @Test
    void returnsWhateverWasSet() {
        TenantContext.setCurrentTenantSlug("gracechapel");

        assertThat(TenantContext.getCurrentTenantSlug()).isEqualTo("gracechapel");
    }

    @Test
    void clearRemovesTheValue() {
        TenantContext.setCurrentTenantSlug("gracechapel");

        TenantContext.clear();

        assertThat(TenantContext.getCurrentTenantSlug()).isNull();
    }

    @Test
    void doesNotLeakAcrossThreads() throws InterruptedException {
        TenantContext.setCurrentTenantSlug("gracechapel");

        String[] seenOnOtherThread = new String[1];
        Thread other = new Thread(() -> seenOnOtherThread[0] = TenantContext.getCurrentTenantSlug());
        other.start();
        other.join();

        assertThat(seenOnOtherThread[0]).isNull();
        assertThat(TenantContext.getCurrentTenantSlug()).isEqualTo("gracechapel");
    }
}
