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

package org.kie.flyway.impl;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.kie.flyway.KieFlywayConstants;
import org.kie.flyway.KieModuleFlywayConfigLoader;
import org.kie.flyway.model.KieFlywayModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

public class DefaultKieModuleFlywayConfigLoader implements KieModuleFlywayConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultKieModuleFlywayConfigLoader.class);

    private ClassLoader classLoader;

    public DefaultKieModuleFlywayConfigLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public List<KieFlywayModuleConfig> loadModuleConfigs() {
        return Optional.ofNullable(this.classLoader).orElse(this.getClass().getClassLoader())
                .resources(KieFlywayConstants.KIE_FLYWAY_DESCRIPTOR_FILE_LOCATION)
                .map(this::toModuleFlywayConfig)
                .filter(Objects::nonNull)
                .toList();
    }

    public KieFlywayModuleConfig toModuleFlywayConfig(URL resourceUrl) {
        LOGGER.debug("Loading configuration from {}", resourceUrl);
        JavaPropsMapper mapper = new JavaPropsMapper();

        try {
            Properties properties = new Properties();
            properties.load(resourceUrl.openStream());

            KieFlywayModuleConfig module = mapper.readPropertiesAs(properties, KieFlywayModuleConfig.class);

            LOGGER.debug("Successfully loaded configuration for module {}", module.getModule());

            return module;
        } catch (IOException e) {
            LOGGER.warn("Could not load configuration from {}", resourceUrl, e);
            throw new RuntimeException("Could not load ModuleFlywayConfig", e);
        }
    }
}
