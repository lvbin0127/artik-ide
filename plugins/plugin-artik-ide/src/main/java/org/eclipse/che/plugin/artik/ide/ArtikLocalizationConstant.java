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
package org.eclipse.che.plugin.artik.ide;

import com.google.gwt.i18n.client.Messages;

public interface ArtikLocalizationConstant extends Messages {

    @Key("manage.artik.devices.action")
    String manageArtikDevicesAction();

    @Key("manage.artik.devices.view.title")
    String manageArtikDevicesViewTitle();

    @Key("manage.artik.devices.view.device.definition")
    String manageArtikDevicesViewDeviceDefiinition();

    @Key("manage.artik.devices.view.new.device.hint")
    String manageArtikDevicesViewNewDeviceHint();

    @Key("manage.artik.devices.view.no.hosts")
    String manageArtikDevicesViewNoHosts();

    @Key("manage.artik.devices.view.name")
    String manageArtikDevicesViewName();

    @Key("manage.artik.devices.view.host")
    String manageArtikDevicesViewHost();

    @Key("manage.artik.devices.view.port")
    String manageArtikDevicesViewPort();

    @Key("manage.artik.devices.view.login")
    String manageArtikDevicesViewLogin();

    @Key("manage.artik.devices.view.replication.folder")
    String manageArtikDevicesViewReplicationFolder();

    @Key("manage.artik.devices.view.password")
    String manageArtikDevicesViewPassword();

    @Key("device.save.success")
    String deviceSaveSuccess();

    @Key("device.save.error")
    String deviceSaveError();

    @Key("device.connect.progress")
    String deviceConnectProgress(String device);

    @Key("device.connect.success")
    String deviceConnectSuccess(String device);

    @Key("device.connect.error")
    String deviceConnectError(String device);

    @Key("device.disconnect.success")
    String deviceDisconnectSuccess(String device);

    @Key("device.disconnect.error")
    String deviceDisconnectError(String device);

    @Key("device.delete.confirm")
    String deviceDeleteConfirm(String device);

    @Key("device.delete.success")
    String deviceDeleteSuccess(String device);

    @Key("device.delete.error")
    String deviceDeleteError(String device);

    @Key("showApiDoc.action.title")
    String showApiDocActionTitle();

    @Key("showApiDoc.action.description")
    String showApiDocActionDescription();

    @Key("showApiDoc.page.title")
    String showApiDocPageTitle();

    @Key("showApiDoc.page.tooltip")
    String showApiDocPageTooltip();

    //##########install package############
    @Key("installPackage.action.title")
    String installPackageActionTitle();

    @Key("installPackage.action.description")
    String installPackageActionActionDescription();

    @Key("installPackage.title")
    String installPackageTitle();

    @Key("installPackage.overwrite")
    String installPackageOverwrite();

    @Key("installButton")
    String installButton();

    @Key("onlinePackageNameLable")
    String onlinePackageNameLable();

    @Key("chooseLocalPackageLable")
    String chooseLocalPackageLable();

    @Key("installPackageNoteLable")
    String installPackageNoteLable();
    //###################e###############

    @Key("updateSDK.action.title")
    String updateSDKActionTitle();

    @Key("updateSDK.action.description")
    String updateSDKActionDescription();

    @Key("updateSDK.view.title")
    String updateSDKViewTitle();

    @Key("updateSDK.view.button.updateAll.title")
    String updateSDKViewButtonInstallTitle();

    @Key("updateSDK.view.button.cancel.title")
    String updateSDKViewButtonCancelTitle();

    @Key("updateSDK.view.column.target.title")
    String updateSDKViewColumnTargetTitle();

    @Key("updateSDK.view.column.currentVersion.title")
    String updateSDKViewColumnInstalledVersionTitle();

    @Key("updateSDK.view.label.selectVersion.title")
    String updateSDKViewLabelSelectVersionTitle();

    @Key("updateSDK.view.loader.message")
    String updateSDKViewLoaderMessage();

    @Key("updateSDK.notification.getVersions.title")
    String updateSDKNotificationGetVersionsTitle();

    @Key("updateSDK.notification.getVersions.fail.message")
    String updateSDKNotificationGetVersionsFailMessage();

    @Key("updateSDK.notification.getInstalledVersion.fail.message")
    String updateSDKNotificationGetInstalledVersionFailMessage();

    @Key("updateSDK.notification.updating.message")
    String updateSDKNotificationUpdatingMessage();

    @Key("updateSDK.notification.update.fail.message")
    String updateSDKNotificationUpdateFailMessage();

    @Key("showKeywordDoc.action.title")
    String showKeywordDocActionTitle();

    @Key("showKeywordDoc.action.description")
    String showKeywordDocActionDescription();

    @Key("restart.view.text")
    String restartViewText();

    @Key("restart.view.button.restartNow")
    String restartViewButtonRestartNow();

    @Key("restart.view.button.restartLater")
    String restartViewButtonRestartLater();

    @Key("push.to.device.description")
    String pushToDeviceDescription();

    @Key("push.to.device")
    String pushToDevice();

    @Key("push.to.device.success")
    String pushToDeviceSuccess(String fileName, String targetPath);

    @Key("push.to.device.fail")
    String pushToDeviceFail(String fileName, String targetPath);

    @Key("choose.device.label")
    String chooseDeviceLabel();

    @Key("target.path")
    String targetPath();

    @Key("choose.target")
    String chooseTarget();

    @Key("cancel.button")
    String cancelButton();


    @Key("artikProfile.action.title")
    String artikProfileActionTitle();

    @Key("turnDevelopmentMode.action.title")
    String turnDevelopmentModeActionTitle();

    @Key("turnDevelopmentMode.action.description")
    String turnDevelopmentModeActionDescription();

    @Key("turnProductionMode.action.title")
    String turnProductionModeActionTitle();

    @Key("turnProductionMode.action.description")
    String turnProductionModeActionDescription();


    @Key("editCompilationOptions.action.title")
    String editCompilationOptionsActionTitle();

    @Key("editCompilationOptions.action.description")
    String editCompilationOptionsActionDescription();

    @Key("editCompilationOptions.view.title")
    String editCompilationOptionsViewTitle();

    @Key("editCompilationOptions.view.binaryName.title")
    String editCompilationOptionsViewBinaryNameTitle();

    @Key("editCompilationOptions.view.options.title")
    String editCompilationOptionsViewOptionsTitle();

    @Key("debug.action.description")
    String debugActionDescription();

    @Key("run.action.description")
    String runActionDescription();

    @Key("reconnection.dialog.title")
    String reconnectionDialogTitle();

    @Key("reconnection.dialog.content")
    String reconnectionDialogContent(String deviceName);
}
