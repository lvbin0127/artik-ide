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
package org.eclipse.che.plugin.artik.ide.ext.onboard.client.inject;

import com.google.gwt.inject.client.AbstractGinModule;
import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.plugin.artik.ide.ext.onboard.client.onboard.AccountServiceClient;
import org.eclipse.che.plugin.artik.ide.ext.onboard.client.onboard.AccountServiceClientImpl;
import org.eclipse.che.plugin.artik.ide.ext.onboard.client.onboard.OnboardView;
import org.eclipse.che.plugin.artik.ide.ext.onboard.client.onboard.OnboardViewImpl;

/**
 * @author Xiaoye Yu
 */
@ExtensionGinModule
public class OnboardGinModule extends AbstractGinModule {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(OnboardView.class).to(OnboardViewImpl.class);
        bind(AccountServiceClient.class).to(AccountServiceClientImpl.class);
    }
}
