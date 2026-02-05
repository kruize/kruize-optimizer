/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.kruize.optimizer;

import com.kruize.optimizer.utils.TargetLabelUtils;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Executes initialization steps upon service startup.
 */
@ApplicationScoped
public class Startup {

    private static final Logger LOG = Logger.getLogger(Startup.class);

    @Inject
    TargetLabelUtils targetLabelUtils;

    /**
     * Executes at the end of application startup.
     *
     * @param ev The Quarkus StartupEvent.
     */
    void onStart(@Observes StartupEvent ev) {
        // initialization of target labels
        targetLabelUtils.getTargetLabels();
        LOG.info("Kruize Optimizer Service is STARTED!");
    }
}
