/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.api.internal.plugins;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.internal.metaobject.*;
import org.gradle.internal.reflect.Instantiator;

import java.util.*;

public class DefaultConvention implements Convention, ExtensionContainerInternal {

    private final Map<String, Object> plugins = new LinkedHashMap<String, Object>();
    private final DefaultConvention.ExtensionsDynamicObject extensionsDynamicObject = new ExtensionsDynamicObject();
    private final ExtensionsStorage extensionsStorage = new ExtensionsStorage();
    private final ExtraPropertiesExtension extraProperties = new DefaultExtraPropertiesExtension();
    private final Instantiator instantiator;

    /**
     * This method should not be used in runtime code proper as means that the convention cannot create
     * dynamic extensions.
     *
     * It's here for backwards compatibility with our tests and for convenience.
     *
     * @see #DefaultConvention(org.gradle.internal.reflect.Instantiator)
     */
    public DefaultConvention() {
        this(null);
    }

    public DefaultConvention(Instantiator instantiator) {
        this.instantiator = instantiator;
        add(ExtraPropertiesExtension.EXTENSION_NAME, extraProperties);
    }

    public Map<String, Object> getPlugins() {
        return plugins;
    }

    public DynamicObject getExtensionsAsDynamicObject() {
        return extensionsDynamicObject;
    }

    private Instantiator getInstantiator() {
        if (instantiator == null) {
            throw new GradleException("request for DefaultConvention.instantiator when the object was constructed without a convention");
        }
        return instantiator;
    }

    public <T> T getPlugin(Class<T> type) {
        T value = findPlugin(type);
        if (value == null) {
            throw new IllegalStateException(String.format("Could not find any convention object of type %s.",
                    type.getSimpleName()));
        }
        return value;
    }

    public <T> T findPlugin(Class<T> type) throws IllegalStateException {
        List<T> values = new ArrayList<T>();
        for (Object object : plugins.values()) {
            if (type.isInstance(object)) {
                values.add(type.cast(object));
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalStateException(String.format("Found multiple convention objects of type %s.",
                    type.getSimpleName()));
        }
        return values.get(0);
    }

    public void add(String name, Object extension) {
        if (extension instanceof Class) {
            create(name, (Class<?>) extension);
        } else {
            extensionsStorage.add(name, extension);
        }
    }

    public <T> T create(String name, Class<T> type, Object... constructionArguments) {
        T instance = getInstantiator().newInstance(type, constructionArguments);
        add(name, instance);
        return instance;
    }

    public ExtraPropertiesExtension getExtraProperties() {
        return extraProperties;
    }

    public <T> T getByType(Class<T> type) {
        return extensionsStorage.getByType(type);
    }

    public <T> T findByType(Class<T> type) {
        return extensionsStorage.findByType(type);
    }

    public Object getByName(String name) {
        return extensionsStorage.getByName(name);
    }

    public Object findByName(String name) {
        return extensionsStorage.findByName(name);
    }

    public <T> void configure(Class<T> type, Action<? super T> action) {
        extensionsStorage.configureExtension(type, action);
    }

    public Map<String, Object> getAsMap() {
        return extensionsStorage.getAsMap();
    }

    public Object propertyMissing(String name) {
        return getByName(name);
    }

    public void propertyMissing(String name, Object value) {
        extensionsStorage.checkExtensionIsNotReassigned(name);
        add(name, value);
    }
    
    private class ExtensionsDynamicObject extends AbstractDynamicObject {
        @Override
        public String getDisplayName() {
            return "extensions";
        }

        @Override
        public boolean hasProperty(String name) {
            if (extensionsStorage.hasExtension(name)) {
                return true;
            }
            for (Object object : plugins.values()) {
                if (new BeanDynamicObject(object).hasProperty(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Map<String, Object> getProperties() {
            Map<String, Object> properties = new HashMap<String, Object>();
            List<Object> reverseOrder = new ArrayList<Object>(plugins.values());
            Collections.reverse(reverseOrder);
            for (Object object : reverseOrder) {
                properties.putAll(new BeanDynamicObject(object).getProperties());
            }
            properties.putAll(extensionsStorage.getAsMap());
            return properties;
        }

        @Override
        public void getProperty(String name, GetPropertyResult result) {
            Object extension = extensionsStorage.findByName(name);
            if (extension != null) {
                result.result(extension);
                return;
            }
            for (Object object : plugins.values()) {
                DynamicObject dynamicObject = new BeanDynamicObject(object).withNotImplementsMissing();
                dynamicObject.getProperty(name, result);
                if (result.isFound()) {
                    return;
                }
            }
        }

        public Object propertyMissing(String name) {
            return getProperty(name);
        }

        @Override
        public void setProperty(String name, Object value, SetPropertyResult result) {
            extensionsStorage.checkExtensionIsNotReassigned(name);
            for (Object object : plugins.values()) {
                BeanDynamicObject dynamicObject = new BeanDynamicObject(object).withNotImplementsMissing();
                dynamicObject.setProperty(name, value, result);
                if (result.isFound()) {
                    return;
                }
            }
        }

        public void propertyMissing(String name, Object value) {
            setProperty(name, value);
        }

        @Override
        public void invokeMethod(String name, InvokeMethodResult result, Object... args) {
            if (extensionsStorage.isConfigureExtensionMethod(name, args)) {
                result.result(extensionsStorage.configureExtension(name, args));
                return;
            }
            for (Object object : plugins.values()) {
                BeanDynamicObject dynamicObject = new BeanDynamicObject(object).withNotImplementsMissing();
                dynamicObject.invokeMethod(name, result, args);
                if (result.isFound()) {
                    return;
                }
            }
        }

        public Object methodMissing(String name, Object args) {
            return invokeMethod(name, (Object[])args);
        }
        
        @Override
        public boolean hasMethod(String name, Object... args) {
            if (extensionsStorage.isConfigureExtensionMethod(name, args)) {
                return true;
            }
            for (Object object : plugins.values()) {
                BeanDynamicObject dynamicObject = new BeanDynamicObject(object);
                if (dynamicObject.hasMethod(name, args)) {
                    return true;
                }
            }
            return false;
        }
    }
}