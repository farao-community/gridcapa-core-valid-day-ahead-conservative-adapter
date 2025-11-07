/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.adapter.app;

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
    private final CoreValidD2ConservativeClient coreValidD2ConservativeClient;
    private final MinioAdapter minioAdapter;

    public CoreValidD2ConservativeAdapterListener(final CoreValidD2ConservativeClient coreValidD2ConservativeClient, final MinioAdapter minioAdapter) {
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
        try {
            if (isReadyOrFinished(taskDto)) {
                LOGGER.info("Handling {} run request on TS {} ", launchType, taskDto.getTimestamp());
                final CoreValidD2ConservativeRequest request = coreValidReqMapper.apply(taskDto);
                coreValidD2ConservativeClient.run(request);
            } else {
                LOGGER.warn("Failed to handle {} run request on timestamp {} because it is not ready yet",
                            launchType,
                            taskDto.getTimestamp());
            }
        } catch (final Exception e) {
            throw new CoreValidD2ConservativeAdapterException(String.format("Error during handling of %s run request on TS %s",
                                                              launchType, taskDto.getTimestamp()), e);
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

    CoreValidD2ConservativeRequest getCoreValidD2ConservativeRequest(final TaskDto taskDto,
                                                 final boolean isLaunchedAutomatically) {
        final String id = taskDto.getId().toString();
        final OffsetDateTime offsetDateTime = taskDto.getTimestamp();
        final List<ProcessFileDto> processFiles = taskDto.getInputs();
        CoreValidD2ConservativeFileResource cnecRam = null;
        CoreValidD2ConservativeFileResource vertice = null;
        for (final ProcessFileDto processFileDto : processFiles) {
            final String fileType = processFileDto.getFileType();
            final String fileUrl = minioAdapter.generatePreSignedUrlFromFullMinioPath(processFileDto.getFilePath(), 1);
            final String fileName = processFileDto.getFilename();
            switch (fileType) {
                case "CNEC-RAM" -> cnecRam = new CoreValidD2ConservativeFileResource(fileName, fileUrl);
                case "VERTICE" -> vertice = new CoreValidD2ConservativeFileResource(fileName, fileUrl);
                default -> throw new IllegalStateException("Unexpected value: " + fileType);
            }
        }
        return new CoreValidD2ConservativeRequest(
                id,
                getCurrentRunId(taskDto, isLaunchedAutomatically),
                offsetDateTime,
                cnecRam,
                vertice,
                isLaunchedAutomatically
        );
    }

    private String getCurrentRunId(final TaskDto taskDto,
                                   final boolean isLaunchedAutomatically) {
        final List<ProcessRunDto> runHistory = taskDto.getRunHistory();
        if (runHistory == null || runHistory.isEmpty()) {
            final String launchType = isLaunchedAutomatically ? AUTOMATIC : MANUAL;
            LOGGER.warn("Failed to handle {} run request on timestamp {} because it has no run history",
                        launchType,
                        taskDto.getTimestamp());
            throw new CoreValidD2ConservativeAdapterException("Failed to handle %s run request on timestamp because it has no run history"
                                                        .formatted(launchType));
        }
        runHistory.sort((o1, o2) -> o2.getExecutionDate().compareTo(o1.getExecutionDate()));
        return runHistory.getFirst().getId().toString();
    }
}
