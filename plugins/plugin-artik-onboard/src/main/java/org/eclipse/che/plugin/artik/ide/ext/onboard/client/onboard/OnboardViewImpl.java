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

import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.ui.window.Window;


/**
 * UI for {@link OnboardView}.
 *
 * @author Xiaoye Yu
 */
@Singleton
public class OnboardViewImpl extends Window implements OnboardView {
    interface OnboardViewImplUiBinder extends UiBinder<Widget, OnboardViewImpl> {
    }

    private static String newURL = "https://accounts.artik.cloud/signup?client_id=82da6e2e45fd4838b468e79e967f1bf7";
    private ActionDelegate delegate;

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    NamedFrame frame = new NamedFrame("haha");

    @Inject
    public OnboardViewImpl(
            OnboardViewImplUiBinder uiBinder,
            OnboardResources aboutResources) {


        this.setWidget(uiBinder.createAndBindUi(this));
        this.ensureDebugId("onboardView-window");
        setWidget(frame);
        frame.setVisible(false);
    }

    @Override
    public void getURL(String jumpUrl) {

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.hide();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
        //  this.show();
        com.google.gwt.user.client.Window.open(newURL, frame.getName(), "dialog=yes,height=520,width=750,location=no,directories=yes,resizable=no");
    }
}
