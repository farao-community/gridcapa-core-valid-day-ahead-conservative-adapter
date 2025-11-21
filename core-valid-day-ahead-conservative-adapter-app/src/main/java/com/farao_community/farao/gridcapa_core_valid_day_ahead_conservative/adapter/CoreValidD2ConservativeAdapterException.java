/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.adapter;

public class CoreValidD2ConservativeAdapterException extends RuntimeException {

    public CoreValidD2ConservativeAdapterException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public CoreValidD2ConservativeAdapterException(final String message) {
        super(message);
    }
}
