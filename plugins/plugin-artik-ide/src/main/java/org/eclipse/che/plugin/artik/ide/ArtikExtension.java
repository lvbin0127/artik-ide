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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.eclipse.che.ide.api.keybinding.KeyBuilder;
import org.eclipse.che.ide.api.machine.events.WsAgentStateEvent;
import org.eclipse.che.ide.api.machine.events.WsAgentStateHandler;
import org.eclipse.che.plugin.artik.ide.apidocs.DocsPartPresenter;
import org.eclipse.che.plugin.artik.ide.apidocs.ShowDocsAction;
import org.eclipse.che.plugin.artik.ide.command.macro.ReplicationFolderMacroRegistrar;
import org.eclipse.che.plugin.artik.ide.command.options.EditCompilationOptionsAction;
import org.eclipse.che.plugin.artik.ide.installpkg.PackageInstallerContextMenuAction;
import org.eclipse.che.plugin.artik.ide.keyworddoc.ShowKeywordDocsAction;
import org.eclipse.che.plugin.artik.ide.manage.ManageArtikDevicesAction;
import org.eclipse.che.plugin.artik.ide.profile.ArtikProfileContextMenuGroup;
import org.eclipse.che.plugin.artik.ide.profile.DevelopmentModeManager;
import org.eclipse.che.plugin.artik.ide.profile.TurnDevelopmentModeContextMenuAction;
import org.eclipse.che.plugin.artik.ide.profile.TurnProductionModeContextMenuAction;
import org.eclipse.che.plugin.artik.ide.resourcemonitor.ResourceMonitor;
import org.eclipse.che.plugin.artik.ide.scp.PushToDeviceManager;
import org.eclipse.che.plugin.artik.ide.updatesdk.UpdateSDKAction;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_CONSOLES_TREE_CONTEXT_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_MAIN_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_MAIN_TOOLBAR;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_PROJECT;
import static org.eclipse.che.ide.api.constraints.Anchor.AFTER;

/**
 * Artik extension entry point.
 *
 * @author Dmitry Shnurenko
 * @author Artem Zatsarynnyi
 */
@Singleton
@Extension(title = "Artik", version = "1.0.0")
public class ArtikExtension {

    public static final String ARTIK_GROUP_MAIN_MENU       = "Artik";
    public static final String ARTIK_GROUP_MAIN_MENU_ID    = "artik";

    public final String SHOW_KEYWORD_DOCS_ACTION_ID = "showKeywordDocsAction";

    @Inject
    public ArtikExtension(EventBus eventBus,
                          final PushToDeviceManager pushToDeviceManager,
                          final DevelopmentModeManager developmentModeManager,
                          IconRegistry iconRegistry,
                          ArtikResources artikResources,
                          final DocsPartPresenter docsPartPresenter) {
        artikResources.getCss().ensureInjected();

        eventBus.addHandler(WsAgentStateEvent.TYPE, new WsAgentStateHandler() {
            @Override
            public void onWsAgentStarted(WsAgentStateEvent wsAgentStateEvent) {
                docsPartPresenter.open();
                pushToDeviceManager.fetchSshMachines();
                developmentModeManager.fetchDevices();
            }

            @Override
            public void onWsAgentStopped(WsAgentStateEvent wsAgentStateEvent) {
            }
        });

        iconRegistry.registerIcon(new Icon("artik.machine.icon", artikResources.artikIcon()));
    }

    @Inject
    private void prepareActions(ManageArtikDevicesAction manageDevicesAction,
                                ActionManager actionManager,
                                UpdateSDKAction updateSDKAction,
                                ShowDocsAction showDocsAction,
                                ShowKeywordDocsAction showKeywordDocsAction,
                                KeyBindingAgent keyBindingAgent,
                                ResourceMonitor resourceMonitor,
                                ArtikProfileContextMenuGroup artikProfileContextMenuGroup,
                                TurnDevelopmentModeContextMenuAction turnDevelopmentModeContextMenuAction,
                                TurnProductionModeContextMenuAction turnProductionModeContextMenuAction,
                                PackageInstallerContextMenuAction packageInstallerContextMenuAction,
                                EditCompilationOptionsAction editCompilationOptionsAction,
                                ReplicationFolderMacroRegistrar replicationFolderMacroRegistrar) {
        final DefaultActionGroup artikGroup = new DefaultActionGroup(ARTIK_GROUP_MAIN_MENU, true, actionManager);
        actionManager.registerAction(ARTIK_GROUP_MAIN_MENU_ID, artikGroup);

        final DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(GROUP_MAIN_MENU);
        mainMenu.add(artikGroup);
        artikGroup.add(updateSDKAction);
        artikGroup.add(showDocsAction);
        artikGroup.add(showKeywordDocsAction);

        actionManager.registerAction("manageArtikDevices", manageDevicesAction);
        actionManager.registerAction("updateSDKAction", updateSDKAction);
        actionManager.registerAction("showDocsAction", showDocsAction);

        actionManager.registerAction(SHOW_KEYWORD_DOCS_ACTION_ID, showKeywordDocsAction);


        DefaultActionGroup profileGroup = new DefaultActionGroup("Profile", true, actionManager);
        actionManager.registerAction("artikProfileGroup", profileGroup);
        artikGroup.add(profileGroup);

        // Consoles tree context menu group
        DefaultActionGroup consolesTreeContextMenu = (DefaultActionGroup)actionManager.getAction(GROUP_CONSOLES_TREE_CONTEXT_MENU);

        actionManager.registerAction("artikProfileContextMenuGroup", artikProfileContextMenuGroup);

        consolesTreeContextMenu.add(artikProfileContextMenuGroup);

        artikProfileContextMenuGroup.add(turnDevelopmentModeContextMenuAction);
        artikProfileContextMenuGroup.add(turnProductionModeContextMenuAction);
        artikProfileContextMenuGroup.add(packageInstallerContextMenuAction);

        DefaultActionGroup projectMainMenu = (DefaultActionGroup)actionManager.getAction(GROUP_PROJECT);
        projectMainMenu.addSeparator();
        projectMainMenu.add(editCompilationOptionsAction);

        DefaultActionGroup actionsToolbarGroup = new ManageDeviceActionGroup(actionManager);
        actionsToolbarGroup.add(manageDevicesAction);
        DefaultActionGroup mainToolbarGroup = (DefaultActionGroup)actionManager.getAction(GROUP_MAIN_TOOLBAR);
        mainToolbarGroup.add(actionsToolbarGroup, new Constraints(AFTER, "resourceOperation"));

        keyBindingAgent.getGlobal().addKey(new KeyBuilder().action().charCode('q').build(), SHOW_KEYWORD_DOCS_ACTION_ID);
    }

    private class ManageDeviceActionGroup extends DefaultActionGroup {

        ManageDeviceActionGroup(ActionManager actionManager) {
            super(actionManager);
        }

        @Override
        public void update(ActionEvent e) {
            e.getPresentation().setEnabledAndVisible(true);
        }
    }
}
