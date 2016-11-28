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

import org.eclipse.che.ide.api.mvp.View;

/**
 * View for displaying Samsung account registration.
 *
 * @author Xiaoye Yu
 */
public interface OnboardView extends View<OnboardView.ActionDelegate> {
    interface ActionDelegate {

        /**
         * Performs any support appropriate in response to the user having pressed the OK button
         */
        void onOkClicked();
    }

    /**
     * Close view.
     */
    void close();

    /**
     * Show About dialog.
     */
    void showDialog();

    void getURL(String jumpUrl);


}
