/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@ConfigurationProperties("core-valid-day-ahead-conservative-adapter")
public record CoreValidD2ConservativeAdapterConfiguration(String taskManagerTimestampUrl,
                                                          List<String> autoTriggerFiletypes,
                                                          List<String> whitelist) {
}
