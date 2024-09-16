/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kie.flyway;

import java.util.Objects;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.kie.flyway.impl.DefaultKieModuleFlywayConfigLoader;
import org.kie.flyway.model.KieFlywayModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieFlywayInitializer {
    private static final String KIE_FLYWAY_BASELINE_VERSION = "0.0";

    private static final String KIE_FLYWAY_BASELINE_MESSAGE_TEMPLATE = "Kie Flyway Baseline - %s";

    private static final String KIE_FLYWAY_INDEX_TABLE_INDEX_TEMPLATE = "kie-flyway-history-%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(KieFlywayInitializer.class);

    private final KieModuleFlywayConfigLoader configLoader;
    private final DataSource dataSource;
    private final String databaseType;

    private KieFlywayInitializer(KieModuleFlywayConfigLoader configLoader, DataSource dataSource, String databaseType) {
        this.configLoader = configLoader;
        this.dataSource = dataSource;
        this.databaseType = databaseType;
    }

    public void migrate() {
        this.configLoader.loadModuleConfigs().forEach(this::runFlyway);
    }

    private void runFlyway(KieFlywayModuleConfig config) {
        LOGGER.debug("Running Flyway for module: {}", config.getModule());
        String[] locations = config.getDBScriptLocations(databaseType);

        if (Objects.isNull(locations)) {
            LOGGER.warn("Cannot run Flyway migration for module `{}`, cannot find SQL Script locations for db `{}`", config.getModule(), databaseType);
            throw new KieFlywayException("Cannot run Flyway migration for module `" + config.getModule() + "`, cannot find SQL Script locations for db `" + databaseType + "`");
        }

        Flyway.configure()
                .table(KIE_FLYWAY_INDEX_TABLE_INDEX_TEMPLATE.formatted(config.getModule()))
                .dataSource(dataSource)
                .baselineVersion(KIE_FLYWAY_BASELINE_VERSION)
                .baselineDescription(KIE_FLYWAY_BASELINE_MESSAGE_TEMPLATE.formatted(config.getModule()))
                .baselineOnMigrate(true)
                .locations(locations)
                .load()
                .migrate();

        LOGGER.debug("Flyway migration complete.");
    }

    public static class Builder {
        private KieModuleFlywayConfigLoader configLoader;
        private DataSource dataSource;
        private String databaseType;

        public static Builder get() {
            return new Builder();
        }

        public Builder withClassLoader(ClassLoader classLoader) {
            this.configLoader = new DefaultKieModuleFlywayConfigLoader(classLoader);
            return this;
        }

        public Builder withConfigLoader(KieModuleFlywayConfigLoader configLoader) {
            this.configLoader = configLoader;
            return this;
        }

        public Builder withDatasource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder withDbType(String databaseType) {
            this.databaseType = databaseType;
            return this;
        }

        public KieFlywayInitializer build() {
            if (Objects.isNull(dataSource)) {
                throw new KieFlywayException("Cannot create KieFlywayInitializer migration, dataSource is null.");
            }

            if (Objects.isNull(databaseType)) {
                throw new KieFlywayException("Cannot create KieFlywayInitializer migration, database type is null.");
            }

            if (Objects.isNull(configLoader)) {
                throw new KieFlywayException("Cannot create KieFlywayInitializer migration, no `configLoader` configured, please configure it or either a Classloader to fetch the application config");
            }

            return new KieFlywayInitializer(configLoader, dataSource, databaseType);
        }
    }

}
