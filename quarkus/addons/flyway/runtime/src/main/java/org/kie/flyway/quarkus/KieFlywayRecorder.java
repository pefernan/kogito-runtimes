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

package org.kie.flyway.quarkus;

import java.util.Objects;

import javax.sql.DataSource;

import org.kie.flyway.KieFlywayInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.runtime.annotations.Recorder;

import jakarta.enterprise.inject.spi.CDI;

@Recorder
public class KieFlywayRecorder {

    private static final Logger log = LoggerFactory.getLogger(KieFlywayRecorder.class);

    public void run(String datasourceName, String dbKind) {

        log.debug("Starting Quarkus Flyway migration in datasource `{}`", datasourceName);
        DataSources datasources = CDI.current().select(DataSources.class).get();

        DataSource ds = datasources.getDataSource(datasourceName);

        if (Objects.isNull(ds)) {
            log.warn("Couldn't find datasource `{}`", datasourceName);
            return;
        }

        KieFlywayInitializer.Builder.get()
                .withDatasource(ds)
                .withDbType(dbKind)
                .withClassLoader(Thread.currentThread().getContextClassLoader())
                .build().migrate();
    }

}
