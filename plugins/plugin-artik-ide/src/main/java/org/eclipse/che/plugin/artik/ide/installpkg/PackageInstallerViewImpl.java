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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.eclipse.che.ide.api.machine.WsAgentURLModifier;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.plugin.artik.ide.ArtikLocalizationConstant;
//import org.eclipse.che.ide.upload.file.PackageInstallerView;

import javax.validation.constraints.NotNull;

/**
 * The implementation of {@link PackageInstallerView}.
 *
 * @author Roman Nikitenko.
 * @author Lijuan Xue
 */
public class PackageInstallerViewImpl extends Window implements PackageInstallerView {

    public interface PackageInstallerViewBinder extends UiBinder<Widget, PackageInstallerViewImpl> {
    }

    Button btnCancel;
    Button btnInstall;

    @UiField
    TextBox pkgName;

    ActionDelegate delegate;

    /**
     * Create view.
     */
    @Inject
    public PackageInstallerViewImpl(PackageInstallerViewBinder packageInstallerViewBinder,
                                    ArtikLocalizationConstant locale) {

        this.setTitle(locale.installPackageTitle());
        setWidget(packageInstallerViewBinder.createAndBindUi(this));

        btnCancel = createButton(locale.cancelButton(), "file-uploadFile-cancel", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        addButtonToFooter(btnCancel);

        btnInstall = createButton(locale.installButton(), "file-uploadFile-upload", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                delegate.onInstallButtonClicked();
            }
        });
        addButtonToFooter(btnInstall);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPackageName() {
        return this.pkgName.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
//        addFile();
        this.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeDialog() {
        this.pkgName.setValue("");
        this.hide();
        this.onClose();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getName() {
        return pkgName.getText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }


}
