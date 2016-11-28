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

import org.eclipse.che.api.promises.client.Promise;

/**
 * Client for Samsung account registration service.
 *
 * @author Artem Zatsarynnyi
 */
public interface AccountServiceClient {

    /**
     * Returns link to the documentation page for the specified keyword.
     *
     * @return a promise that resolves to the page's link, or rejects with an error
     */
    Promise<String> getAccountURL();
}
