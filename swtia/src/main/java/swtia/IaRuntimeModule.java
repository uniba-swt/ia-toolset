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

package swtia;

import org.eclipse.xtext.conversion.IValueConverterService;
import org.eclipse.xtext.formatting2.IFormatter2;
import org.eclipse.xtext.service.SingletonBinding;
import swtia.conversion.CustomValueConverterService;
import swtia.formatting2.IaFormatter;
import swtia.startup.StandaloneApp;
import swtia.validation.IaValidator;
import swtia.validation.ModelValidatorProxy;

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
public class IaRuntimeModule extends AbstractIaRuntimeModule {
    @Override
    public Class<? extends IValueConverterService> bindIValueConverterService() {
        return CustomValueConverterService.class;
    }

    public Class<? extends StandaloneApp> bindStandaloneApp() {
        return StandaloneApp.class;
    }

    @SingletonBinding(eager=true)
    public Class<? extends IaValidator> bindIaValidator() {
        return ModelValidatorProxy.class;
    }

    public Class<? extends IFormatter2> bindIFormatter2() {
        return IaFormatter.class;
    }
}
