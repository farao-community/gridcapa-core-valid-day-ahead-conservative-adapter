/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative;

import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.adapter.configuration.CoreValidD2ConservativeAdapterConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SuppressWarnings("hideutilityclassconstructor")
@SpringBootApplication
@EnableConfigurationProperties(CoreValidD2ConservativeAdapterConfiguration.class)
@EnableWebMvc
@EnableRetry
public class CoreValidD2ConservativeAdapterApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreValidD2ConservativeAdapterApplication.class, args);
    }
}
