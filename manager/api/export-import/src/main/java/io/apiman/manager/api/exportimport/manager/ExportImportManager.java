/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.manager.api.exportimport.manager;

import io.apiman.manager.api.config.Version;
import io.apiman.manager.api.core.IStorage;
import io.apiman.manager.api.core.logging.ApimanLogger;
import io.apiman.manager.api.core.logging.IApimanLogger;
import io.apiman.manager.api.exportimport.json.IExportImportFactory;
import io.apiman.manager.api.exportimport.json.JsonFileExportImportFactory;
import io.apiman.manager.api.exportimport.read.IImportReader;
import io.apiman.manager.api.exportimport.write.IExportWriter;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
@ApplicationScoped
public class ExportImportManager {
    
    @Inject
    private Version version;
    @Inject
    private ExportImportConfigParser config;
    @Inject
    private IStorage storage;
    @Inject @ApimanLogger(ExportImportManager.class)
    IApimanLogger logger;

    private Map<ExportImportProviderType, IExportImportFactory> eiFactories = new HashMap<>();
    private ExportImportProviderType provider;

    // TODO We should have some kind of automated registration of these & factory pattern. This is interim.
    {
        eiFactories.put(ExportImportProviderType.JSON, new JsonFileExportImportFactory());
    }

    /**
     * Constructor.
     */
    public ExportImportManager() {
    }
    
    @PostConstruct
    protected void postConstruct() {
        provider = config.getProvider();
    }

    public boolean isImportExport() {
        return config.isImportExport();
    }
    
    public void doImportExport() {
        if(config.getFunction() == ExportImportFunction.IMPORT) {
            doImport();
        } else if (config.getFunction() == ExportImportFunction.EXPORT) {
            doExport();
        }
    }

    private void doImport() {
        IImportReader reader = eiFactories.get(provider).createReader(config);
        reader.setDispatcher(new StorageImportDispatcher(storage, logger));
        try {
            reader.read();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doExport() {
        IExportWriter writer = eiFactories.get(provider).createWriter(config);
        new StorageExporter(version, writer, storage, null).export();
    }
}
