/*******************************************************************************
 * Copyright (c) 2016 Samsung Electronics Co., Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - Initial implementation
 *   Samsung Electronics Co., Ltd. - Initial implementation
 *******************************************************************************/
package org.eclipse.che.plugin.machine.artik;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.WorkspaceIdProvider;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineLogMessage;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.core.util.LoggingUncaughtExceptionHandler;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.AbstractLineConsumer;
import org.eclipse.che.api.core.util.AbstractMessageConsumer;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.FileLineConsumer;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.MessageConsumer;
import org.eclipse.che.api.core.util.WebsocketLineConsumer;
import org.eclipse.che.api.core.util.WebsocketMessageConsumer;
import org.eclipse.che.api.machine.server.MachineInstanceProviders;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineLogMessageImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.event.MachineProcessEvent;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.eclipse.che.commons.schedule.executor.ThreadPullLauncher;
import org.eclipse.che.plugin.artik.shared.dto.ArtikDeviceStatusEventDto;
import org.eclipse.che.plugin.machine.ssh.SshMachineRecipe;
import org.slf4j.Logger;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.che.api.core.model.machine.MachineStatus.RUNNING;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.plugin.artik.shared.Constants.ARTIK_DEVICE_STATUS_CHANNEL;
import static org.eclipse.che.plugin.machine.artik.ArtikDevice.Status.CONNECTED;
import static org.eclipse.che.plugin.machine.artik.ArtikDevice.Status.ERROR;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Facade for Artik device operations
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class ArtikDeviceManager {
    private static final Logger LOG  = getLogger(ArtikDeviceManager.class);
    private static final Gson   GSON = new Gson();

    private final EventService             eventService;
    private final ThreadPullLauncher       launcher;
    private final ArtikTerminalLauncher    artikTerminalLauncher;
    private final String                   machineLogsDir;
    private final MachineInstanceProviders machineInstanceProviders;
    private final ExecutorService          executor;

    private Map<String, ArtikDevice>         instances;
    private Map<String, DeviceHealthChecker> checkers;

    @Inject
    public ArtikDeviceManager(EventService eventService,
                              ThreadPullLauncher launcher,
                              ArtikTerminalLauncher artikTerminalLauncher,
                              MachineInstanceProviders machineInstanceProviders,
                              @Named("artik.device.logs.location") String artikDeviceLogsDir) {
        this.eventService = eventService;
        this.launcher = launcher;
        this.artikTerminalLauncher = artikTerminalLauncher;
        this.machineLogsDir = artikDeviceLogsDir;
        this.machineInstanceProviders = machineInstanceProviders;

        instances = new ConcurrentHashMap<>();
        checkers = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ArtikDeviceManager-%d")
                                                                           .setUncaughtExceptionHandler(
                                                                                 LoggingUncaughtExceptionHandler.getInstance())
                                                                           .setDaemon(false)
                                                                           .build());
    }

    /**
     * Retrieves device instance that allows to execute commands in a device.
     *
     * @param deviceId
     *         ID of requested device
     * @return instance of requested device
     * @throws NotFoundException
     *         if workspace is not running
     */
    public MachineDto getDeviceById(String deviceId) throws NotFoundException {
        final Instance machine = instances.get(deviceId).getInstance();
        if (machine == null) {
            throw new NotFoundException(format("Machine with ID '%s' is not found", deviceId));
        }
        return ArtikDtoConverter.asDto(machine);
    }

    /**
     * Execute a command in device.
     *
     * @param deviceId
     *         ID of requested device
     * @param command
     *         command that should be executed in device
     * @param outputChannel
     *         channel for command output
     * @return {@link org.eclipse.che.api.machine.server.spi.InstanceProcess} that represents started process in device
     * @throws NotFoundException
     *         if device with specified id not found
     * @throws MachineException
     *         if other error occur
     * @throws BadRequestException
     *         if value of required parameter is invalid
     */
    InstanceProcess exec(String deviceId,
                         Command command,
                         @Nullable String outputChannel) throws NotFoundException, MachineException, BadRequestException {
        requiredNotNull(deviceId, "Machine ID is required");
        requiredNotNull(command, "Command is required");
        requiredNotNull(command.getCommandLine(), "Command line is required");
        requiredNotNull(command.getName(), "Command name is required");
        requiredNotNull(command.getType(), "Command type is required");

        Instance instance = instances.get(deviceId).getInstance();
        if (instance == null) {
            throw new NotFoundException(format("Machine with ID '%s' is not found", deviceId));
        }
        InstanceProcess instanceProcess = instance.createProcess(command, outputChannel);

        final int pid = instanceProcess.getPid();
        final LineConsumer processLogger = getProcessLogger(deviceId, pid, outputChannel);

        executor.execute(ThreadLocalPropagateContext.wrap(() -> {
            try {
                eventService.publish(newDto(MachineProcessEvent.class)
                                             .withEventType(MachineProcessEvent.EventType.STARTED)
                                             .withMachineId(deviceId)
                                             .withProcessId(pid));

                instanceProcess.start(processLogger);

                eventService.publish(newDto(MachineProcessEvent.class)
                                             .withEventType(MachineProcessEvent.EventType.STOPPED)
                                             .withMachineId(deviceId)
                                             .withProcessId(pid));
            } catch (ConflictException | MachineException error) {
                eventService.publish(newDto(MachineProcessEvent.class)
                                             .withEventType(MachineProcessEvent.EventType.ERROR)
                                             .withMachineId(deviceId)
                                             .withProcessId(pid)
                                             .withError(error.getLocalizedMessage()));

                try {
                    processLogger.writeLine(String.format("[ERROR] %s", error.getMessage()));
                } catch (IOException ignored) {
                }
            } finally {
                try {
                    processLogger.close();
                } catch (IOException ignored) {
                }
            }
        }));
        return instanceProcess;
    }

    /**
     * Get list of active processes from device
     *
     * @param deviceId
     *         id of machine to get processes information from
     * @return list of {@link org.eclipse.che.api.machine.server.spi.InstanceProcess}
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    List<InstanceProcess> getProcessesById(String deviceId) throws MachineException, NotFoundException {
        final Instance machine = instances.get(deviceId).getInstance();
        if (machine == null) {
            throw new NotFoundException(format("Machine with ID '%s' is not found", deviceId));
        }
        return machine.getProcesses();
    }


    /**
     * Get all created devices. Each device has status {@link MachineStatus#RUNNING} or {@link MachineStatus#DESTROYING}
     *
     * @return list of alive devices
     */
    public List<MachineDto> getDevices() {
        return instances.values()
                        .stream()
                        .map(artikDevice -> ArtikDtoConverter.asDto(artikDevice.getInstance()))
                        .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Stops process in device
     *
     * @param deviceId
     *         if of the device where process should be stopped
     * @param processId
     *         id of the process that should be stopped in device
     * @throws NotFoundException
     *         if machine or process with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    void stopProcess(String deviceId, int processId) throws NotFoundException, MachineException {
        final ArtikDevice device = instances.get(deviceId);
        if (device == null) {
            return;
        }
        device.getInstance().getProcess(processId).kill();
    }


    /**
     * Disconnect a device.
     *
     * @param deviceId
     *         ID of device that should be disconnected
     * @param remove
     *         is {@code true} when device should be removed from the storage
     * @return disconnected device
     * @throws NotFoundException
     *         if device is not found
     * @throws MachineException
     *         if other error occurs
     */
    MachineDto disconnect(String deviceId, boolean remove) throws MachineException, NotFoundException {
        final ArtikDevice device = instances.get(deviceId);
        final Instance instance = device.getInstance();

        final DeviceHealthChecker deviceHealthChecker = checkers.get(deviceId);
        if (deviceHealthChecker != null) {
            deviceHealthChecker.stop();
        }

        if (remove) {
            for (InstanceProcess process : instance.getProcesses()) {
                process.kill();
            }
            instances.remove(deviceId);
            checkers.remove(deviceId);
        } else {
            device.disconnect();
            if (ArtikDevice.Status.CONNECTED.equals(device.getStatus())) {
                device.setStatus(ArtikDevice.Status.DISCONNECTED);
            }
        }

        return ArtikDtoConverter.asDto(instance);
    }

    /**
     * Creates new device.
     *
     * @param deviceId
     *         id of the device
     * @return configuration of the connected device
     * @throws ServerException
     *         if some exception occurs during starting
     * @throws NotFoundException
     *         if device is not found in running workspace
     */
    MachineDto connectById(String deviceId) throws ServerException, NotFoundException {
        final ArtikDevice device = instances.get(deviceId);
        if (device == null) {
            throw new NotFoundException(format("Device with ID '%s' is not found", deviceId));
        }
        device.connect();
        Instance instance = device.getInstance();
        if (ArtikDevice.Status.DISCONNECTED.equals(device.getStatus())) {
            artikTerminalLauncher.launch(instance);
        } else if (ArtikDevice.Status.ERROR.equals(device.getStatus())) {
            instance = createNewInstance(deviceId, instance);
        }

        DeviceHealthChecker deviceHealthChecker = checkers.get(deviceId);
        if (deviceHealthChecker != null) {
            deviceHealthChecker.start();
        } else {
            deviceHealthChecker = new DeviceHealthChecker(device);
            checkers.put(deviceId, deviceHealthChecker);
            launcher.scheduleWithFixedDelay(deviceHealthChecker, 2L, 10L, SECONDS);
        }

        return ArtikDtoConverter.asDto(instance);
    }

    private Instance createNewInstance(String deviceId, Instance instance) throws NotFoundException, ServerException {
        MachineImpl machine = MachineImpl.builder()
                                         .setConfig(instance.getConfig())
                                         .setWorkspaceId(WorkspaceIdProvider.getWorkspaceId())
                                         .setStatus(MachineStatus.CREATING)
                                         .setOwner(instance.getOwner())
                                         .setId(deviceId)
                                         .build();

        final InstanceProvider provider = machineInstanceProviders.getProvider(instance.getConfig().getType());
        final LineConsumer machineLogger = getDeviceLogger(getMessageConsumer(), instance.getConfig().getName());
        final Instance newInstance = provider.createInstance(machine, machineLogger);

        artikTerminalLauncher.launch(newInstance);
        machine.setStatus(RUNNING);

        final ArtikDevice artikDevice = new ArtikDevice(newInstance, CONNECTED);
        instances.put(deviceId, artikDevice);

        return newInstance;
    }

    /**
     * Creates new device.
     *
     * @param deviceConfig
     *         configuration of device to connect
     * @return connected device
     * @throws ServerException
     *         if some exception occurs during connecting
     */
    MachineDto connect(MachineConfigDto deviceConfig) throws ServerException {
        final String creator = EnvironmentContext.getCurrent().getSubject().getUserId();
        final String deviceId = generateDeviceId();

        MachineImpl machine = MachineImpl.builder()
                                         .setConfig(deviceConfig)
                                         .setWorkspaceId(WorkspaceIdProvider.getWorkspaceId())
                                         .setStatus(MachineStatus.CREATING)
                                         .setOwner(creator)
                                         .setId(deviceId)
                                         .build();
        try {
            final InstanceProvider provider = machineInstanceProviders.getProvider(deviceConfig.getType());
            final LineConsumer machineLogger = getDeviceLogger(getMessageConsumer(), deviceConfig.getName());
            final Instance instance = provider.createInstance(machine, machineLogger);

            artikTerminalLauncher.launch(instance);

            machine.setStatus(RUNNING);
            final ArtikDevice artikDevice = new ArtikDevice(instance, CONNECTED);

            instances.put(deviceId, artikDevice);

            eventService.publish(newDto(ArtikDeviceStatusEventDto.class)
                                         .withEventType(ArtikDeviceStatusEventDto.EventType.CONNECTED)
                                         .withDeviceName(deviceConfig.getName())
                                         .withDeviceId(deviceId));

            final DeviceHealthChecker deviceHealthChecker = new DeviceHealthChecker(artikDevice);
            checkers.put(deviceId, deviceHealthChecker);
            launcher.scheduleWithFixedDelay(deviceHealthChecker, 2L, 10L, SECONDS);

            return ArtikDtoConverter.asDto(instance);
        } catch (ApiException e) {
            eventService.publish(newDto(ArtikDeviceStatusEventDto.class)
                                         .withEventType(ArtikDeviceStatusEventDto.EventType.ERROR)
                                         .withDeviceName(deviceConfig.getName())
                                         .withDeviceId(deviceId));

            throw new ServerException(e);
        }
    }

    /**
     * Restores created devices.
     *
     * @param devicesConfigs
     *         list of configurations
     * @return list of restored devices
     * @throws ServerException
     *         if some exception occurs during creating
     */
    List<MachineDto> restoreDevices(List<MachineConfigDto> devicesConfigs) throws ServerException {
        List<MachineDto> devices = new LinkedList<>();
        for (MachineConfigDto deviceConfig : devicesConfigs) {
            final String creator = EnvironmentContext.getCurrent().getSubject().getUserId();
            String deviceId = generateDeviceId();

            MachineImpl machine = MachineImpl.builder()
                                             .setConfig(deviceConfig)
                                             .setWorkspaceId(WorkspaceIdProvider.getWorkspaceId())
                                             .setStatus(MachineStatus.CREATING)
                                             .setOwner(creator)
                                             .setId(deviceId)
                                             .build();
            try {
                final InstanceProvider provider = machineInstanceProviders.getProvider(deviceConfig.getType());
                final LineConsumer machineLogger = getDeviceLogger(getMessageConsumer(), deviceConfig.getName());
                final Instance instance = provider.createInstance(machine, machineLogger);

                artikTerminalLauncher.launch(instance);

                final ArtikDevice artikDevice = new ArtikDevice(instance, ArtikDevice.Status.DISCONNECTED);
                artikDevice.disconnect();
                instances.put(deviceId, artikDevice);

                devices.add(ArtikDtoConverter.asDto(instance));
            } catch (ApiException e) {
                throw new ServerException(e);
            }
        }

        return devices;
    }

    /**
     * Gets process reader from device by specified id.
     *
     * @param deviceId
     *         device id whose process reader will be returned
     * @param pid
     *         process id
     * @return reader for specified process on device
     * @throws NotFoundException
     *         if device with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public Reader getProcessLogReader(String deviceId, int pid) throws NotFoundException, MachineException {
        final File processLogsFile = getProcessLogsFile(deviceId, pid);
        if (processLogsFile.isFile()) {
            try {
                return Files.newBufferedReader(processLogsFile.toPath(), Charset.defaultCharset());
            } catch (IOException e) {
                throw new MachineException(
                        String.format("Unable read log file for process '%s' of device '%s'. %s", pid, deviceId, e.getMessage()));
            }
        }
        throw new NotFoundException(String.format("Logs for process '%s' of device '%s' are not available", pid, deviceId));
    }

    private LineConsumer getProcessLogger(String machineId, int pid, String outputChannel) throws MachineException {
        return getLineConsumerLogger(getProcessFileLogger(machineId, pid), outputChannel);
    }

    private FileLineConsumer getProcessFileLogger(String machineId, int pid) throws MachineException {
        try {
            return new FileLineConsumer(getProcessLogsFile(machineId, pid));
        } catch (IOException e) {
            throw new MachineException(String.format("Unable create log file for process '%s' of device '%s'. %s",
                                                     pid,
                                                     machineId,
                                                     e.getMessage()));
        }
    }

    private File getProcessLogsFile(String machineId, int pid) {
        final File file = new File(machineLogsDir, machineId);
        if (!(file.exists() || file.mkdirs())) {
            throw new IllegalStateException("Unable create directory " + machineLogsDir);
        }
        return new File(file, Integer.toString(pid));
    }

    private LineConsumer getLineConsumerLogger(LineConsumer fileLogger, String outputChannel) throws MachineException {
        if (outputChannel != null) {
            return new CompositeLineConsumer(fileLogger, new WebsocketLineConsumer(outputChannel));
        }
        return fileLogger;
    }

    private String generateDeviceId() {
        return NameGenerator.generate("artik-device", 16);
    }

    private LineConsumer getDeviceLogger(MessageConsumer<MachineLogMessage> deviceStatusLogger,
                                         String machineName) throws ServerException {
        return new ArtikDeviceConsumer(deviceStatusLogger, machineName);
    }

    private MessageConsumer<MachineLogMessage> getMessageConsumer() throws ServerException {
        WebsocketMessageConsumer<MachineLogMessage> deviceMessageConsumer = new WebsocketMessageConsumer<>(ARTIK_DEVICE_STATUS_CHANNEL);
        return new ArtikMessageConsumer(deviceMessageConsumer);
    }

    private void requiredNotNull(Object object, String message) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(message + " required");
        }
    }

    private static class ArtikMessageConsumer extends AbstractMessageConsumer<MachineLogMessage> {
        private final WebsocketMessageConsumer<MachineLogMessage> deviceMessageConsumer;

        ArtikMessageConsumer(WebsocketMessageConsumer<MachineLogMessage> deviceMessageConsumer) {
            this.deviceMessageConsumer = deviceMessageConsumer;
        }

        @Override
        public void consume(MachineLogMessage message) throws IOException {
            deviceMessageConsumer.consume(message);
        }
    }

    private static class ArtikDeviceConsumer extends AbstractLineConsumer {
        private final MessageConsumer<MachineLogMessage> deviceStatusLogger;
        private final String                             machineName;

        ArtikDeviceConsumer(MessageConsumer<MachineLogMessage> deviceStatusLogger, String machineName) {
            this.deviceStatusLogger = deviceStatusLogger;
            this.machineName = machineName;
        }

        @Override
        public void writeLine(String line) throws IOException {
            deviceStatusLogger.consume(new MachineLogMessageImpl(machineName, line));
        }
    }

    /**
     * Mechanism for verifying state of the connection to the device.
     */
    private class DeviceHealthChecker implements Runnable {
        private volatile boolean stop;

        private ArtikDevice device;
        private String      host;

        public DeviceHealthChecker(ArtikDevice device) {
            this.device = device;
            this.stop = false;

            final String content = device.getInstance().getConfig().getSource().getContent();
            final SshMachineRecipe deviceRecipe = GSON.fromJson(content, SshMachineRecipe.class);
            this.host = deviceRecipe.getHost();
        }

        @Override
        public void run() {
            if (!stop) {
                checkConnection(device);
            }
        }

        /**
         * Starts checking of the connection.
         */
        public void start() {
            this.stop = false;
        }

        /**
         * Stops checking of the connection
         */
        public void stop() {
            this.stop = true;
        }

        private void checkConnection(ArtikDevice device) {
            final Instance instance = device.getInstance();
            try {
                InetAddress address = InetAddress.getByName(host);
                if (!address.isReachable(5_000)) {
                    eventService.publish(newDto(ArtikDeviceStatusEventDto.class)
                                                 .withEventType(ArtikDeviceStatusEventDto.EventType.DISCONNECTED)
                                                 .withDeviceName(instance.getConfig().getName())
                                                 .withDeviceId(instance.getId()));
                    device.setStatus(ERROR);
                }
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }
}
