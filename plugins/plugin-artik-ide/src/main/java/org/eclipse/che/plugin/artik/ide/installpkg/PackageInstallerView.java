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

import com.google.gwt.user.client.ui.IsWidget;

import javax.validation.constraints.NotNull;

/**
 * The view of {@link PackageInstallerPresenter}.
 *
 * @author Lijuan Xue.
 */
public interface PackageInstallerView extends IsWidget {

    public interface ActionDelegate {
        /**
         * Performs any actions appropriate in response to the user having pressed the Cancel button.
         */
        void onCancelClicked();

        /**
         * Performs any actions appropriate in response to the user having pressed the Upload button.
         */
        void onInstallButtonClicked();

    }


    /**
     * Return package name.
     */
    String getPackageName();

    /**
     * Show dialog.
     */
    void showDialog();

    /**
     * Close dialog
     */
    void closeDialog();

    /**
     * Sets the delegate to receive events from this view.
     */
    void setDelegate(ActionDelegate delegate);
}
