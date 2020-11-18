/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.api.internal.attributes;

import org.gradle.api.Action;
import org.gradle.api.attributes.AttributeValue;
import org.gradle.api.attributes.MultipleCandidatesDetails;

class SelectAllCompatibleRule<T> implements Action<MultipleCandidatesDetails<T>> {
    private static final SelectAllCompatibleRule RULE = new SelectAllCompatibleRule();

    @SuppressWarnings("unchecked")
    public static <T> void apply(MultipleCandidatesDetails<T> details) {
        RULE.execute(details);
    }

    private SelectAllCompatibleRule() {}

    @Override
    public void execute(MultipleCandidatesDetails<T> details) {
        for (AttributeValue<T> candidate : details.getCandidateValues()) {
            details.closestMatch(candidate);
        }
    }
}
