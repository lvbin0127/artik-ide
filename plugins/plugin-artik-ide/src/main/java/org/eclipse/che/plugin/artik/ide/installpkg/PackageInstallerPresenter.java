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
package org.eclipse.che.plugin.artik.ide.installpkg;

import com.google.common.base.Optional;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.machine.MachineServiceClient;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationListener;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.commons.exception.UnmarshallerException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.extension.machine.client.processes.panel.ProcessesPanelPresenter;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.resources.reveal.RevealResourceEvent;
import org.eclipse.che.ide.util.UUID;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.StringUnmarshallerWS;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.eclipse.che.plugin.artik.ide.machine.DeviceServiceClient;

import javax.validation.constraints.Null;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.createFromAsyncRequest;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

/**
 * The purpose of this class is install package to the target machine
 *
 * @author Lijuan Xue
 */
@Singleton
public class PackageInstallerPresenter implements PackageInstallerView.ActionDelegate {

    private final PackageInstallerView view;
    private final AppContext appContext;
    private final NotificationManager notificationManager;
    private Container container;
    private Machine machine;
    private DtoFactory dtoFactory;
    private final DeviceServiceClient deviceServiceClient;
    private final MessageBusProvider      messageBusProvider;
    private final ProcessesPanelPresenter processesPanelPresenter;
    private AsyncCallback<String> commandCallback;

    private StatusNotification progressNotification;

    @Inject
    public PackageInstallerPresenter(PackageInstallerView view,
                                     AppContext appContext,
                                     NotificationManager notificationManager,
                                     DtoFactory dtoFactory,
                                     DeviceServiceClient deviceServiceClient,
                                     MessageBusProvider messageBusProvider,
                                     ProcessesPanelPresenter processesPanelPresenter) {
        this.appContext = appContext;
        this.view = view;
        this.notificationManager = notificationManager;
        this.dtoFactory = dtoFactory;
        this.deviceServiceClient = deviceServiceClient;
        this.view.setDelegate(this);
        this.messageBusProvider = messageBusProvider;
        this.processesPanelPresenter = processesPanelPresenter;
    }

    /**
     * Show dialog.
     */
    public void showDialog(Machine machine) {
        this.machine = machine;
        view.showDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancelClicked() {
        view.closeDialog();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onInstallButtonClicked() {
        String packageName = view.getPackageName();
        progressNotification = notificationManager.notify("Installing package: " + packageName + " on the target machine: " + machine.getConfig().getName() + ".", StatusNotification.Status.PROGRESS, FLOAT_MODE);
        if (packageName != null && !packageName.isEmpty()) {
            String command = "dnf install " + packageName + " -y \n"
                    + "# Special marker line. Don't modify it.\n"
                    + "echo \">>> end <<<\"";

            executeCommand(command, machine).then(new Operation<String>() {
                @Override
                public void apply(String arg) throws OperationException {
                    String message = "Installing process completed.";
                    progressNotification.setTitle(message);
                    progressNotification.setStatus(SUCCESS);
                }
            });

            view.closeDialog();
        }
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

    private Promise<String> executeCommand(final String cmd, final Machine machine) {
        final String deviceName = machine.getConfig().getName();
        final String chanel = "process:output:" + UUID.uuid();
        try {
            final MessageBus messageBus = messageBusProvider.getMachineMessageBus();
            messageBus.subscribe(chanel, new SubscriptionHandler<String>(new CommandOutputUnmarshaller()) {
                @Override
                protected void onMessageReceived(String message) {
                    if ("[STDOUT] >>> end <<<".equals(message)) {
                        messageBus.unsubscribeSilently(chanel, this);
                        processesPanelPresenter.printMachineOutput(deviceName, "");
                        commandCallback.onSuccess(message);

                    } else {
                        processesPanelPresenter.printMachineOutput(deviceName, message);
                    }
                    notificationManager.setVisible(false);
                }

                @Override
                protected void onErrorReceived(Throwable throwable) {
                    messageBus.unsubscribeSilently(chanel, this);
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

        deviceServiceClient.executeCommand(machine.getId(), command, chanel);
        return promise;
    }
}
