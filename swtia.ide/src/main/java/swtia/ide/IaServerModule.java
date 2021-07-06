/*
 *
 * Copyright (C) 2021 University of Bamberg, Software Technologies Research Group
 * <https://www.uni-bamberg.de/>, <http://www.swt-bamberg.de/>
 *
 * This file is part of the Foundations of Heterogeneous Specifications Using
 * State Machines and Temporal Logic (IA-Toolset) project, which received financial
 * support by the German Research Foundation (DFG) under grant nos. LU 1748/3-2,
 * VO 615/12-2, see <https://www.swt-bamberg.de/research/interface-theories.html>.
 *
 * IA-Toolset is licensed under the GNU GENERAL PUBLIC LICENSE (Version 3), see
 * the LICENSE file at the project's top-level directory for details or consult
 * <http://www.gnu.org/licenses/>.
 *
 * IA-Toolset is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 *
 * IA-Toolset is a RESEARCH PROTOTYPE and distributed WITHOUT ANY
 * WARRANTY, without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * The following people contributed to the conception and realization of the
 * present IA-Toolset distribution (in alphabetic order by surname):
 *
 * - Tri Nguyen (https://github.com/trinnguyen)
 *
 */

package swtia.ide;

import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.xtext.ide.ExecutorServiceProvider;
import org.eclipse.xtext.ide.server.*;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.ResourceServiceProviderServiceLoader;
import org.eclipse.xtext.resource.containers.ProjectDescriptionBasedContainerManager;

import java.util.concurrent.ExecutorService;

public class IaServerModule extends ServerModule {
    @Override
    protected void configure() {
        binder().bind(ExecutorService.class).toProvider(ExecutorServiceProvider.class);

        bind(LanguageServer.class).to(IaRequestLanguageServer.class);
        bind(LanguageServerImpl.class).to(IaRequestLanguageServer.class);
        bind(IResourceServiceProvider.Registry.class).toProvider(ResourceServiceProviderServiceLoader.class);
        bind(IMultiRootWorkspaceConfigFactory.class).to(MultiRootWorkspaceConfigFactory.class);
        bind(IProjectDescriptionFactory.class).to(DefaultProjectDescriptionFactory.class);
        bind(IContainer.Manager.class).to(ProjectDescriptionBasedContainerManager.class);
    }
}
