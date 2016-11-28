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
package org.eclipse.che.plugin.artik.ide.profile;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.dialogs.CancelCallback;
import org.eclipse.che.ide.api.dialogs.ConfirmCallback;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.machine.events.MachineStateEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.commons.exception.UnmarshallerException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.extension.machine.client.processes.panel.ProcessesPanelPresenter;
import org.eclipse.che.ide.util.UUID;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.eclipse.che.plugin.artik.ide.ArtikExtension;
import org.eclipse.che.plugin.artik.ide.ArtikResources;
import org.eclipse.che.plugin.artik.ide.machine.DeviceServiceClient;
import org.eclipse.che.plugin.artik.ide.installpkg.PackageInstallerAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.gwt.json.client.JSONParser.parseLenient;
import static org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.createFromAsyncRequest;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

/**
 * Manager for switching profile at Artik devices.
 *
 * @author Vitaliy Guliy
 */
@Singleton
public class DevelopmentModeManager implements MachineStateEvent.Handler {

    private static final String REPLICATION_FOLDER      = "replicationFolder";
    private static final String DEFAULT_PROJECTS_FOLDER = "projects";

    private final ActionManager           actionManager;
    private final ArtikModeActionFactory  artikModeActionFactory;
    private final MessageBusProvider      messageBusProvider;
    private final DeviceServiceClient     deviceServiceClient;
    private final DtoFactory              dtoFactory;
    private final ArtikResources          resources;
    private final ProcessesPanelPresenter processesPanelPresenter;
    private final DialogFactory           dialogFactory;
    private final NotificationManager     notificationManager;

    /**
     * A set of ssh machines.
     * Use machine name for a key and machine ID for a value.
     */
    private final Map<String, Machine> sshMachines;

    private final Map<String, DefaultActionGroup> menus;

    private AsyncCallback<String> commandCallback;

    private StatusNotification progressNotification;

    @Inject
    public DevelopmentModeManager(ActionManager actionManager,
                                  ArtikModeActionFactory artikModeActionFactory,
                                  EventBus eventBus,
                                  MessageBusProvider messageBusProvider,
                                  DtoFactory dtoFactory,
                                  DeviceServiceClient deviceServiceClient,
                                  ArtikResources resources,
                                  ProcessesPanelPresenter processesPanelPresenter,
                                  DialogFactory dialogFactory,
                                  NotificationManager notificationManager) {
        this.actionManager = actionManager;
        this.artikModeActionFactory = artikModeActionFactory;
        this.messageBusProvider = messageBusProvider;
        this.dtoFactory = dtoFactory;
        this.deviceServiceClient = deviceServiceClient;
        this.resources = resources;
        this.processesPanelPresenter = processesPanelPresenter;
        this.dialogFactory = dialogFactory;
        this.notificationManager = notificationManager;

        sshMachines = new HashMap<>();
        menus = new HashMap<>();

        eventBus.addHandler(MachineStateEvent.TYPE, this);
    }

    private native void log(String msg) /*-{
        console.log(msg);
    }-*/;

    private static String format(final String format, final Object... args) {
        final String pattern = "%s";

        int start;
        int last = 0;
        int argsIndex = 0;

        final StringBuilder result = new StringBuilder();
        while ((start = format.indexOf(pattern, last)) != -1) {
            result.append(format.substring(last, start));
            result.append(args[argsIndex++]);
            last = start + pattern.length();
        }
        result.append(format.substring(last));
        return result.toString();
    }

    public void turnOnDevelopmentMode(final String machineName) {
        final String title = "Development Mode";
        final String content = "Development mode will install software and dependencies for Artik IDE.<br>" +
                               "Are you sure you want to turn on development mode for <b>" + machineName + "</b>?";
        final String yesMessage = "Yes";
        final String cancelMessage = "Cancel";

        final ConfirmCallback confirmCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                String notificationMessage = "Turning on development mode for " + machineName;
                progressNotification = notificationManager.notify(notificationMessage, PROGRESS, FLOAT_MODE);

                Machine machine = sshMachines.get(machineName);
                String cmd = resources.turnOnDevelopmentProfileCommand().getText();
                executeCommand(cmd, machine).then(new Operation<String>() {
                    @Override
                    public void apply(String arg) throws OperationException {
                        String message = "Development mode has been turned on for " + machineName;
                        progressNotification.setTitle(message);
                        progressNotification.setStatus(SUCCESS);
                    }
                });
            }
        };
        final CancelCallback cancelCallback = null;

        dialogFactory.createConfirmDialog(title, content, yesMessage, cancelMessage, confirmCallback, cancelCallback)
                     .show();
    }

    /**
     * Fetches all devices converts them to actions and adds to push to device action group.
     */
    public void fetchDevices() {
        final DefaultActionGroup artikGroup = (DefaultActionGroup)actionManager.getAction(ArtikExtension.ARTIK_GROUP_MAIN_MENU_ID);
        artikGroup.addSeparator();

        deviceServiceClient.getDevices().then(new Operation<List<MachineDto>>() {
            @Override
            public void apply(List<MachineDto> machines) throws OperationException {
                for (Machine machine : machines) {
                    if (!MachineStatus.RUNNING.equals(machine.getStatus())) {
                        continue;
                    }
                    addArtikProfileMenu(machine);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.error(getClass(), arg.getMessage());
            }
        });
    }

    private void addArtikProfileMenu(Machine machine) {
        if (sshMachines.containsKey(machine.getConfig().getName())) {
            return;
        }
        sshMachines.put(machine.getConfig().getName(), machine);

        final DefaultActionGroup profileGroup = (DefaultActionGroup)actionManager.getAction("artikProfileGroup");

        final DefaultActionGroup machineMenu = new DefaultActionGroup(machine.getConfig().getName(), true, actionManager);
        menus.put(machine.getConfig().getName(), machineMenu);

        profileGroup.add(machineMenu);

        TurnDevelopmentModeAction turnDevelopmentModeAction = artikModeActionFactory.turnDevelopmentModeAction(machine.getConfig().getName());
        machineMenu.add(turnDevelopmentModeAction);

        TurnProductionModeAction turnProductionModeAction = artikModeActionFactory.turnProductionModeAction(machine.getConfig().getName());
        machineMenu.add(turnProductionModeAction);

        PackageInstallerAction packageInstallerAction = artikModeActionFactory.packageInstallerAction(sshMachines.get(machine.getConfig().getName()));
        machineMenu.add(packageInstallerAction);
    }

    private void removeArtikProfileMenu(String machineName) {
        sshMachines.remove(machineName);

        final DefaultActionGroup profileGroup = (DefaultActionGroup)actionManager.getAction("artikProfileGroup");

        DefaultActionGroup machineMenu = menus.remove(machineName);
        if (machineMenu == null) {
            return;
        }

        profileGroup.remove(machineMenu);
    }

    @Override
    public void onMachineCreating(MachineStateEvent event) {
    }

    @Override
    public void onMachineRunning(MachineStateEvent event) {
        addArtikProfileMenu(event.getMachine());
    }

    @Override
    public void onMachineDestroyed(MachineStateEvent event) {
        removeArtikProfileMenu(event.getMachine().getConfig().getName());
    }

    /**
     * Executes a command and returns first message from output as a result.
     */
    private Promise<String> executeCommand(final String cmd, final Machine device) {
        final String deviceName = device.getConfig().getName();
        final String chanel = "process:output:" + UUID.uuid();

        try {
            final MessageBus machineMessageBus = messageBusProvider.getMachineMessageBus();
            machineMessageBus.subscribe(chanel, new SubscriptionHandler<String>(new CommandOutputUnmarshaller()) {
                @Override
                protected void onMessageReceived(String message) {
                    if ("[STDOUT] >>> end <<<".equals(message)) {
                        machineMessageBus.unsubscribeSilently(chanel, this);
                        processesPanelPresenter.printMachineOutput(deviceName, "");
                        commandCallback.onSuccess(message);
                    } else {
                        if (message.startsWith("[STDOUT] ")) {
                            processesPanelPresenter.printMachineOutput(deviceName, message.substring(9));
                        } else if (message.startsWith("[STDERR] ")) {
                            processesPanelPresenter.printMachineOutput(deviceName, message.substring(9), "red");
                        } else {
                            processesPanelPresenter.printMachineOutput(deviceName, message);
                        }
                    }
                }

                @Override
                protected void onErrorReceived(Throwable throwable) {
                    machineMessageBus.unsubscribeSilently(chanel, this);
                }
            });
        } catch (WebSocketException e) {
            commandCallback.onFailure(new Exception(e));
        }

        final Promise<String> promise = createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<String>() {
            @Override
            public void makeCall(AsyncCallback<String> callback) {
                commandCallback = callback;
            }
        });

        final Command command = dtoFactory.createDto(CommandDto.class)
                                          .withName("name")
                                          .withType("custom")
                                          .withCommandLine(cmd);

        deviceServiceClient.executeCommand(device.getId(), command, chanel);

        return promise;
    }

    private class CommandOutputUnmarshaller implements Unmarshallable<String> {

        private String payload;

        @Override
        public void unmarshal(Message response) throws UnmarshallerException {
            payload = response.getBody();
        }

        @Override
        public String getPayload() {
            return payload;
        }

    }

    public void turnOnProductionMode(final String machineName) {
        final String title = "Production Mode";
        final String message = "Production mode will uninstall software and dependencies for Artik IDE, and delete projects backups.<br>" +
                               "It cannot be undone!<br>" +
                               "Are you sure you want to turn on production mode for <b>" + machineName + "</b>?";
        final String yesMessage = "Yes";
        final String noMessage = "Cancel";
        final ConfirmCallback confirmCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                final String message = "Turning on production mode for " + machineName;
                progressNotification = notificationManager.notify(message, PROGRESS, FLOAT_MODE);

                final Machine machine = sshMachines.get(machineName);

                final MachineConfig config = machine.getConfig();
                Log.debug(DevelopmentModeManager.this.getClass(), "config: " + config);

                final MachineSource source = config.getSource();
                Log.debug(DevelopmentModeManager.this.getClass(), "source: " + source);

                final String location = source.getLocation();
                Log.debug(DevelopmentModeManager.this.getClass(), "location: " + location);

                final String content = source.getContent();
                final JSONObject jsonObject = parseLenient(content).isObject();

                final JSONValue replicationFolderValue = jsonObject.get(REPLICATION_FOLDER);
                final JSONString replicationFolderString = replicationFolderValue.isString();

                final String replicationFolder = replicationFolderString.stringValue();
                Log.debug(DevelopmentModeManager.this.getClass(), "replicationFolder: " + replicationFolder);


                final String commandText = resources.turnOnProductionProfileCommand().getText();
                final String command = format(commandText, replicationFolder + "/" + DEFAULT_PROJECTS_FOLDER);
                Log.debug(DevelopmentModeManager.this.getClass(), "command: " + command);

                executeCommand(command, machine).then(new ExecuteCommandOperation(machineName))
                                                        .catchError(new ErrorOperation());

            }
        };
        final CancelCallback cancelCallback = null;

        dialogFactory.createConfirmDialog(title, message, yesMessage, noMessage, confirmCallback, cancelCallback)
                     .show();

    }

    private class ErrorOperation implements Operation<PromiseError> {
        @Override
        public void apply(PromiseError promiseError) throws OperationException {
            String message = "Production mode has not been turned on due to internal error.";
            progressNotification.setTitle(message);
            progressNotification.setStatus(FAIL);
        }
    }

    private class ExecuteCommandOperation implements Operation<String> {
        String name;

        public ExecuteCommandOperation(String machineName) {
            name = machineName;
        }

        @Override
        public void apply(String arg) throws OperationException {
            String message = "Production mode has been turned on for " + name;
            progressNotification.setTitle(message);
            progressNotification.setStatus(SUCCESS);
        }
    }

}
