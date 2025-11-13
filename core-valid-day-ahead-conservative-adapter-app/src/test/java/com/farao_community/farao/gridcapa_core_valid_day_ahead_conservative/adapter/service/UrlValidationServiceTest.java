/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.adapter.service;

import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.adapter.configuration.CoreValidD2ConservativeAdapterConfiguration;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.adapter.exception.CoreValidD2ConservativeAdapterException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class UrlValidationServiceTest {
    @Test
    void whitelistExceptionTest() {
        final CoreValidD2ConservativeAdapterConfiguration configuration = Mockito.mock(CoreValidD2ConservativeAdapterConfiguration.class);
        Mockito.when(configuration.whitelist()).thenReturn(List.of("http://test/", "https://test/"));
        final UrlValidationService service = new UrlValidationService(configuration);

        Assertions.assertThatExceptionOfType(CoreValidD2ConservativeAdapterException.class)
                .isThrownBy(() -> service.openUrlStream("ftp://test/test.xml"))
                .withMessage("URL 'ftp://test/test.xml' is not part of application's whitelisted urls: http://test/, https://test/");
    }

    @Test
    void readExceptionTest() {
        final CoreValidD2ConservativeAdapterConfiguration configuration = Mockito.mock(CoreValidD2ConservativeAdapterConfiguration.class);
        Mockito.when(configuration.whitelist()).thenReturn(List.of("test/"));
        final UrlValidationService service = new UrlValidationService(configuration);

        Assertions.assertThatExceptionOfType(CoreValidD2ConservativeAdapterException.class)
                .isThrownBy(() -> service.openUrlStream("test/test.xml"))
                .withMessage("Cannot download file resource from URL 'test/test.xml'");
    }
}
