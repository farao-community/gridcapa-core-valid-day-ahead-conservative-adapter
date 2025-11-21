/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.adapter;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.resource.CoreValidD2ConservativeFileResource;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.resource.CoreValidD2ConservativeRequest;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.starter.CoreValidD2ConservativeClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.farao_community.farao.gridcapa.task_manager.api.TaskStatus.ERROR;
import static com.farao_community.farao.gridcapa.task_manager.api.TaskStatus.READY;
import static com.farao_community.farao.gridcapa.task_manager.api.TaskStatus.SUCCESS;

@Component
public class CoreValidD2ConservativeAdapterListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidD2ConservativeAdapterListener.class);
    public static final String AUTOMATIC = "automatic";
    public static final String MANUAL = "manual";
    public static final String CNEC_RAM = "CNEC-RAM";
    public static final String VERTICES = "VERTICES";
    private final CoreValidD2ConservativeClient coreValidD2ConservativeClient;
    private final MinioAdapter minioAdapter;

    public CoreValidD2ConservativeAdapterListener(final CoreValidD2ConservativeClient coreValidD2ConservativeClient,
                                                  final MinioAdapter minioAdapter) {
        this.coreValidD2ConservativeClient = coreValidD2ConservativeClient;
        this.minioAdapter = minioAdapter;
    }

    @Bean
    public Consumer<TaskDto> consumeTask() {
        return this::handleManualTask;
    }

    @Bean
    public Consumer<TaskDto> consumeAutoTask() {
        return this::handleAutoTask;
    }

    private void handleAutoTask(final TaskDto taskDto) {
        handleTask(taskDto, this::getAutomaticCoreValidD2ConservativeRequest, AUTOMATIC);
    }

    private void handleManualTask(final TaskDto taskDto) {
        handleTask(taskDto, this::getManualCoreValidD2ConservativeRequest, MANUAL);
    }

    private void handleTask(final TaskDto taskDto,
                            final Function<TaskDto, CoreValidD2ConservativeRequest> coreValidReqMapper,
                            final String launchType) {
        final OffsetDateTime timestamp = taskDto.getTimestamp();
        try {
            if (isReadyOrFinished(taskDto)) {
                LOGGER.info("Handling {} run request on TS {} ", launchType, timestamp);
                final CoreValidD2ConservativeRequest request = coreValidReqMapper.apply(taskDto);
                coreValidD2ConservativeClient.run(request);
            } else {
                LOGGER.warn("Failed to handle {} run request on timestamp {} because it is not ready yet",
                            launchType, timestamp);
            }
        } catch (final Exception e) {
            throw new CoreValidD2ConservativeAdapterException(String.format("Error during handling of %s run request on TS %s",
                                                                            launchType, timestamp), e);
        }

    }

    private static boolean isReadyOrFinished(final TaskDto taskDto) {
        final TaskStatus status = taskDto.getStatus();
        return status == READY || status == SUCCESS || status == ERROR;
    }

    CoreValidD2ConservativeRequest getManualCoreValidD2ConservativeRequest(final TaskDto taskDto) {
        return getCoreValidD2ConservativeRequest(taskDto, false);
    }

    CoreValidD2ConservativeRequest getAutomaticCoreValidD2ConservativeRequest(final TaskDto taskDto) {
        return getCoreValidD2ConservativeRequest(taskDto, true);
    }

    private CoreValidD2ConservativeRequest getCoreValidD2ConservativeRequest(final TaskDto taskDto,
                                                                             final boolean isAuto) {
        CoreValidD2ConservativeFileResource cnecRam = null;
        CoreValidD2ConservativeFileResource vertices = null;

        for (final ProcessFileDto input : taskDto.getInputs()) {
            final String fileType = input.getFileType();
            final String fileUrl = minioAdapter.generatePreSignedUrlFromFullMinioPath(input.getFilePath(), 1);
            final String fileName = input.getFilename();
            switch (fileType) {
                case CNEC_RAM -> cnecRam = new CoreValidD2ConservativeFileResource(fileName, fileUrl);
                case VERTICES -> vertices = new CoreValidD2ConservativeFileResource(fileName, fileUrl);
                default -> throw new IllegalStateException("Unexpected value: " + fileType);
            }
        }
        return new CoreValidD2ConservativeRequest(
                taskDto.getId().toString(),
                getCurrentRunId(taskDto, isAuto),
                taskDto.getTimestamp(),
                cnecRam,
                vertices,
                isAuto,
                taskDto.getParameters()
        );
    }

    private String getCurrentRunId(final TaskDto taskDto,
                                   final boolean isAuto) {
        final List<ProcessRunDto> runHistory = taskDto.getRunHistory();
        if (runHistory == null || runHistory.isEmpty()) {
            final String launchType = isAuto ? AUTOMATIC : MANUAL;
            LOGGER.warn("Failed to handle {} run request on timestamp {} because it has no run history",
                        launchType,
                        taskDto.getTimestamp());
            throw new CoreValidD2ConservativeAdapterException("Failed to handle %s run request on timestamp because it has no run history"
                                                                      .formatted(launchType));
        }
        runHistory.sort((run1, run2) ->
                                run2.getExecutionDate().compareTo(run1.getExecutionDate()));

        return runHistory.getFirst().getId().toString();
    }
}
