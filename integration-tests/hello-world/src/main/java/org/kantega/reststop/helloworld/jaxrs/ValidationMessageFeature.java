/*
 * Copyright 2018 Kantega AS
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
 */

package org.kantega.reststop.helloworld.jaxrs;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

/**
 *
 */
public class ValidationMessageFeature implements Feature {

    public static final String BV_SEND_ERROR_IN_RESPONSE
            = "jersey.config.beanValidation.enableOutputValidationErrorEntity.server";

    @Override
    public boolean configure(FeatureContext context) {
        context.property(BV_SEND_ERROR_IN_RESPONSE, "true");
        return true;
    }
}
