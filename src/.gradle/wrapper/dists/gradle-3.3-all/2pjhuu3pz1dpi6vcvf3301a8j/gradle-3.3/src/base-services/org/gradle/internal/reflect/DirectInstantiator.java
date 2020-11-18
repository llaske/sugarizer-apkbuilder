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
package org.gradle.internal.reflect;

import com.google.common.annotations.VisibleForTesting;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.WeakHashMap;

public class DirectInstantiator implements Instantiator {

    public static final Instantiator INSTANCE = new DirectInstantiator();

    private final ConstructorCache constructorCache = new ConstructorCache();

    public static <T> T instantiate(Class<? extends T> type, Object... params) {
        return INSTANCE.newInstance(type, params);
    }

    private DirectInstantiator() {
    }

    public <T> T newInstance(Class<? extends T> type, Object... params) {
        try {
            Constructor<?> match = doGetConstructor(type, constructorCache.get(type), params);
            return type.cast(match.newInstance(params));
        } catch (InvocationTargetException e) {
            throw new ObjectInstantiationException(type, e.getCause());
        } catch (Exception e) {
            throw new ObjectInstantiationException(type, e);
        }
    }

    private <T> Constructor<?> doGetConstructor(Class<? extends T> type, Constructor<?>[] constructors, Object[] params) {
        Constructor<?> match = null;
        if (constructors.length > 0) {
            for (Constructor<?> constructor : constructors) {
                if (isMatch(constructor, params)) {
                    if (match != null) {
                        throw new IllegalArgumentException(String.format("Found multiple public constructors for %s which accept parameters %s.", type, Arrays.toString(params)));
                    }
                    match = constructor;
                }
            }
        }
        if (match == null) {
            throw new IllegalArgumentException(String.format("Could not find any public constructor for %s which accepts parameters %s.", type, Arrays.toString(params)));
        }
        return match;
    }

    private static boolean isMatch(Constructor<?> constructor, Object... params) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes.length != params.length) {
            return false;
        }
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.isPrimitive()) {
                if (!JavaReflectionUtil.getWrapperTypeForPrimitiveType(parameterType).isInstance(param)) {
                    return false;
                }
            } else {
                if (param != null && !parameterType.isInstance(param)) {
                    return false;
                }
            }
        }
        return true;
    }

    @VisibleForTesting
    public static class ConstructorCache {
        private final Object lock = new Object();
        private final WeakHashMap<Class<?>, WeakReference<Constructor<?>[]>> cache = new WeakHashMap<Class<?>, WeakReference<Constructor<?>[]>>();

        public Constructor<?>[] get(Class<?> key) {
            WeakReference<Constructor<?>[]> cached;
            synchronized (lock) {
                cached = cache.get(key);
            }
            if (cached != null) {
                Constructor<?>[] ctrs = cached.get();
                if (ctrs != null) {
                    return ctrs;
                }
            }
            return getAndCache(key);
        }

        private Constructor<?>[] getAndCache(Class<?> key) {
            Constructor<?>[] ctors = key.getConstructors();
            WeakReference<Constructor<?>[]> value = new WeakReference<Constructor<?>[]>(ctors);
            synchronized (lock) {
                cache.put(key, value);
            }
            return ctors;
        }
    }
}
