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

package org.kie.kogito.codegen.faultTolerance;

import org.drools.codegen.common.rest.RestAnnotator;
import org.kie.kogito.codegen.api.Generator;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.JavaKogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.QuarkusKogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.SpringBootKogitoBuildContext;
import org.kie.kogito.codegen.faultTolerance.impl.QuarkusFaultToleranceAnnotator;
import org.kie.kogito.codegen.faultTolerance.impl.SpringBootFaultToleranceAnnotator;
import org.kie.kogito.codegen.process.ProcessCodegenException;
import org.kie.kogito.codegen.process.util.CodegenUtil;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

public class FaultToleranceUtil {

    public static final String FAULT_TOLERANCE_ENABLED = "faultToleranceEnabled";

    private FaultToleranceUtil() {
        // This is a utility class and shouldn't be instantiated.
    }

    public static boolean isFaultToleranceEnabled(Generator generator, KogitoBuildContext context) {
        boolean isFaultToleranceEnabled = CodegenUtil.getProperty(generator, context, FAULT_TOLERANCE_ENABLED, Boolean::parseBoolean, false);

        return !JavaKogitoBuildContext.CONTEXT_NAME.equals(context.name()) && isFaultToleranceEnabled;
    }

    public static void annotateWithFaultTolerance(CompilationUnit compilationUnit, KogitoBuildContext context) {
        FaultToleranceAnnotator annotator = lookFaultToleranceAnnotatorForContext(context);

        RestAnnotator restAnnotator = context.getRestAnnotator();
        compilationUnit.findAll(MethodDeclaration.class)
                .stream().filter(restAnnotator::isRestAnnotated)
                .forEach(annotator::addFaultToleranceAnnotations);

    }

    public static FaultToleranceAnnotator lookFaultToleranceAnnotatorForContext(KogitoBuildContext context) {
        if (QuarkusKogitoBuildContext.CONTEXT_NAME.equals(context.name())) {
            return new QuarkusFaultToleranceAnnotator(context);
        } else if (SpringBootKogitoBuildContext.CONTEXT_NAME.equals(context.name())) {
            return new SpringBootFaultToleranceAnnotator(context);
        } else {
            throw new ProcessCodegenException("Kogito Fault Tolerance not allowed for context: " + context.name());
        }
    }

}
