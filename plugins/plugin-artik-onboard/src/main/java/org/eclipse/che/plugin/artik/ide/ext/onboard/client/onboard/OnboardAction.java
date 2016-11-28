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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;

/**
 * Action for clicking SignUp button.
 *
 * @author Ann Shumilova
 * @author Oleksii Orel
 * @author Xiaoye Yu
 */
@Singleton
public class OnboardAction extends Action {

    private final OnboardPresenter presenter;

    @Inject
    public OnboardAction(OnboardPresenter presenter,
                         OnboardLocalizationConstant locale,
                         OnboardResources resources) {
        super(locale.onboardControlTitle(), "Show onboard application", null, resources.onboard());
        this.presenter = presenter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        presenter.showOnboard();
    }

}
