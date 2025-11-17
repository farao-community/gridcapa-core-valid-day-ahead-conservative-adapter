/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessEventDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.resource.CoreValidD2ConservativeRequest;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.starter.CoreValidD2ConservativeClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel_external at rte-france.com>}
 */
@SpringBootTest
class CoreValidD2ConservativeAdapterListenerTest {

    @MockitoBean
    private CoreValidD2ConservativeClient coreValidD2ConservativeClient;

    @MockitoBean
    private MinioAdapter minioAdapter;

    @Captor
    ArgumentCaptor<CoreValidD2ConservativeRequest> argumentCaptor;

    @Autowired
    private CoreValidD2ConservativeAdapterListener coreValidD2ConservativeAdapterListener;
    private String cnecRamFileType;
    private String verticeFileType;

    private String cnecRamFileName;
    private String verticeFileName;

    private String cnecRamFilePath;
    private String verticeFilePath;
    private String cnecRamFileUrl;
    private String verticeFileUrl;

    public TaskDto createTaskDtoWithStatus(final TaskStatus status) {
        final UUID id = UUID.randomUUID();
        final OffsetDateTime timestamp = OffsetDateTime.parse("2025-10-02T14:30Z");
        final List<ProcessFileDto> processFiles = new ArrayList<>();

        processFiles.add(new ProcessFileDto(cnecRamFilePath, cnecRamFileType, ProcessFileStatus.VALIDATED, cnecRamFileName, "docId1", timestamp));
        processFiles.add(new ProcessFileDto(verticeFilePath, verticeFileType, ProcessFileStatus.VALIDATED, verticeFileName, "docId2", timestamp));
        final List<ProcessEventDto> processEvents = new ArrayList<>();
        final List<ProcessRunDto> runHistory = new ArrayList<>();
        runHistory.add(new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now(), processFiles));
        return new TaskDto(id, timestamp, status, processFiles, null, Collections.emptyList(), processEvents, runHistory, Collections.emptyList());
    }

    @BeforeEach
    void setUp() {
        cnecRamFileType = "CNEC-RAM";
        verticeFileType = "VERTICES";

        cnecRamFileName = "cnec-ram";
        verticeFileName = "vertices";

        cnecRamFilePath = "/CNEC-RAM";
        verticeFilePath = "/VERTICES";

        cnecRamFileUrl = "file://CNEC-RAM/cnecram";
        verticeFileUrl = "file://VERTICES/vertices";

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cnecRamFilePath, 1)).thenReturn(cnecRamFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(verticeFilePath, 1)).thenReturn(verticeFileUrl);
    }

    @Test
    void testGetManualCoreValidRequest() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        final CoreValidD2ConservativeRequest coreValidRequest = coreValidD2ConservativeAdapterListener.getManualCoreValidD2ConservativeRequest(taskDto);
        Assertions.assertEquals(taskDto.getId().toString(), coreValidRequest.getId());
        Assertions.assertEquals(cnecRamFileName, coreValidRequest.getCnecRam().getFilename());
        Assertions.assertEquals(cnecRamFileUrl, coreValidRequest.getCnecRam().getUrl());
        Assertions.assertEquals(verticeFileName, coreValidRequest.getVertices().getFilename());
        Assertions.assertEquals(verticeFileUrl, coreValidRequest.getVertices().getUrl());
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetManualCoreValidRequestThrowsException() {
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.READY, List.of(), null, List.of(), List.of(), List.of(), List.of());

        Assertions.assertThrows(
                CoreValidD2ConservativeAdapterException.class,
                () -> coreValidD2ConservativeAdapterListener.getManualCoreValidD2ConservativeRequest(taskDto),
                "Failed to handle manual run request on timestamp because it has no run history");
    }

    @Test
    void testGetAutomaticCoreValidRequest() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        final CoreValidD2ConservativeRequest coreValidRequest = coreValidD2ConservativeAdapterListener.getAutomaticCoreValidD2ConservativeRequest(taskDto);
        Assertions.assertTrue(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetCoreValidRequestWithIncorrectFiles() {
        final UUID id = UUID.randomUUID();
        final OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        final List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cnecRamFilePath, cnecRamFileType, ProcessFileStatus.VALIDATED, cnecRamFileName, "docId1", timestamp));
        processFiles.add(new ProcessFileDto(verticeFilePath, "VORTICE", ProcessFileStatus.VALIDATED, verticeFileName, "docId2", timestamp));
        final List<ProcessEventDto> processEvents = new ArrayList<>();
        final TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, null, Collections.emptyList(), processEvents, Collections.emptyList(), Collections.emptyList());
        Assertions.assertThrows(IllegalStateException.class, () -> coreValidD2ConservativeAdapterListener.getManualCoreValidD2ConservativeRequest(taskDto));

    }

    @Test
    void consumeReadyAutoTask() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        coreValidD2ConservativeAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidD2ConservativeClient).run(argumentCaptor.capture());
        final CoreValidD2ConservativeRequest coreValidRequest = argumentCaptor.getValue();
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"READY", "SUCCESS", "ERROR"})
    void consumeReadyTask(final TaskStatus taskStatus) {
        final TaskDto taskDto = createTaskDtoWithStatus(taskStatus);
        coreValidD2ConservativeAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidD2ConservativeClient).run(argumentCaptor.capture());
        final CoreValidD2ConservativeRequest coreValidRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeCreatedTask() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreValidD2ConservativeAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidD2ConservativeClient, Mockito.never()).run(argumentCaptor.capture());
    }

    @Test
    void consumeTaskThrowsException() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        Mockito.doThrow(RuntimeException.class).when(coreValidD2ConservativeClient).run(Mockito.any());
        final Consumer<TaskDto> taskDtoConsumer = coreValidD2ConservativeAdapterListener.consumeTask();
        Assertions.assertThrows(
                CoreValidD2ConservativeAdapterException.class,
                () -> taskDtoConsumer.accept(taskDto),
                "Error during handling manual run request on TS 2025-10-02T14:30Z");
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"READY", "SUCCESS", "ERROR"})
    void consumeSuccessAutoTask(final TaskStatus taskStatus) {
        final TaskDto taskDto = createTaskDtoWithStatus(taskStatus);
        coreValidD2ConservativeAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidD2ConservativeClient).run(argumentCaptor.capture());
        final CoreValidD2ConservativeRequest coreValidD2ConservativeRequest = argumentCaptor.getValue();
        assert coreValidD2ConservativeRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeCreatedAutoTask() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreValidD2ConservativeAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidD2ConservativeClient, Mockito.never()).run(argumentCaptor.capture());
    }

    @Test
    void consumeAutoTaskThrowsException() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        Mockito.doThrow(RuntimeException.class).when(coreValidD2ConservativeClient).run(Mockito.any());
        final Consumer<TaskDto> taskDtoConsumer = coreValidD2ConservativeAdapterListener.consumeAutoTask();
        Assertions.assertThrows(
                CoreValidD2ConservativeAdapterException.class,
                () -> taskDtoConsumer.accept(taskDto),
                "Error during handling manual run request on TS 2025-10-02T14:30Z");
    }
}
