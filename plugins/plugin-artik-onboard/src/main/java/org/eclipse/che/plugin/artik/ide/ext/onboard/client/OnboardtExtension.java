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
package org.eclipse.che.plugin.artik.ide.ext.onboard.client;


import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.plugin.artik.ide.ext.onboard.client.onboard.OnboardAction;


import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Xiaoye Yu
 */
@Singleton
@Extension(title = "Onboard Extension", version = "3.0.0")
public class OnboardtExtension {


    @Inject
    public OnboardtExtension(ActionManager actionManager,
                             final OnboardAction onboardAction) {
        // Compose Help menu
        DefaultActionGroup helpGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_HELP);
        actionManager.registerAction("onboardAbout", onboardAction);

        helpGroup.addSeparator();
        helpGroup.add(onboardAction);
    }
}
