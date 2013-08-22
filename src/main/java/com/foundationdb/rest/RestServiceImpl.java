/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.rest;

import com.foundationdb.http.HttpConductor;
import com.foundationdb.rest.resources.*;
import com.foundationdb.server.service.Service;
import com.foundationdb.server.service.config.ConfigurationService;
import com.foundationdb.server.service.dxl.DXLService;
import com.foundationdb.server.service.restdml.DirectService;
import com.foundationdb.server.service.restdml.RestDMLService;
import com.foundationdb.server.service.security.SecurityService;
import com.foundationdb.server.service.session.SessionService;
import com.foundationdb.server.service.transaction.TransactionService;
import com.foundationdb.server.store.Store;
import com.google.inject.Inject;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.eclipse.jetty.servlet.ServletHolder;

import java.util.Arrays;

public class RestServiceImpl implements RestService, Service {
    private final ConfigurationService configService;
	private final HttpConductor http;
    // Used by various resources
    private final ResourceRequirements reqs;

	private volatile ServletHolder servletHolder;
	

	@Inject
    public RestServiceImpl(ConfigurationService configService,
                           HttpConductor http,
                           RestDMLService restDMLService,
                           DirectService directService,
                           SessionService sessionService,
                           TransactionService transactionService,
                           SecurityService securityService,
                           DXLService dxlService,
                           Store store) {
        this.configService = configService;
		this.http = http;
        this.reqs = new ResourceRequirements(
            dxlService,
            restDMLService,
            directService,
            securityService,
            sessionService,
            transactionService,
            store,
            configService
        );
    }

    @Override
    public String getContextPath() {
        return configService.getProperty("fdbsql.rest.context_path");
    }

	@Override
	public void start() {
		registerConnector(http);
	}

	@Override
	public void stop() {
        http.unregisterHandler(servletHolder);
        this.servletHolder = null;
	}

	@Override
	public void crash() {
		stop();
	}

	private void registerConnector(HttpConductor http) {
        String path = getContextPath() + "/*";
        servletHolder = new ServletHolder(new ServletContainer(createResourceConfigV1()));
        http.registerHandler(servletHolder, path);
	}

    private ResourceConfig createResourceConfigV1() {
        DefaultResourceConfig config = new DefaultResourceConfig();
        config.getSingletons().addAll(Arrays.asList(
                new EntityResource(reqs),
                new FullTextResource(reqs),
                new ModelResource(reqs),
                new ProcedureCallResource(reqs),
                new SecurityResource(reqs),
                new SQLResource(reqs),
                new VersionResource(reqs),
                new DirectResource(reqs),
                new ViewResource(reqs),
                // This must be last to capture anything not handled above
                new DefaultResource()
        ));
        return config;
    }
}
