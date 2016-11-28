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
package org.eclipse.che.plugin.artik.ide.ext.onboard.client.onboard;


import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.notification.NotificationManager;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;

/**
 * Presenter for displaying Samsung account registration.
 *
 * @author Xiaoye Yu
 */
@Singleton
public class OnboardPresenter implements OnboardView.ActionDelegate {
    private OnboardView view;
    private AccountServiceClient accountClient;
    private NotificationManager notificationManager;

    @Inject
    public OnboardPresenter(OnboardView view,
                            AccountServiceClient accountClient,
                            NotificationManager notificationManager
    ) {
        this.view = view;
        this.accountClient = accountClient;
        this.notificationManager = notificationManager;
        view.setDelegate(this);

    }

    /**
     * Show Samsung account registration window.
     */
    public void showOnboard() {

        view.showDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOkClicked() {
        view.close();
    }
}
