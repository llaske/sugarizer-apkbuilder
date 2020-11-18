/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts;

import org.gradle.api.artifacts.ModuleIdentifier;

public class DefaultModuleIdentifier implements ModuleIdentifier {
    private final String group;
    private final String name;

    public DefaultModuleIdentifier(String group, String name) {
        assert group != null : "group cannot be null";
        assert name != null : "name cannot be null";
        this.group = group;
        this.name = name;
    }

    public static ModuleIdentifier newId(String group, String name) {
        return new DefaultModuleIdentifier(group, name);
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", group, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        DefaultModuleIdentifier other = (DefaultModuleIdentifier) obj;
        if (!group.equals(other.group)) {
            return false;
        }
        if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return group.hashCode() ^ name.hashCode();
    }
}
