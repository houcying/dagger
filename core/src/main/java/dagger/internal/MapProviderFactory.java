/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger.internal;

import dagger.Factory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

/**
 * A {@link Factory} implementation used to implement {@link Map} bindings. This factory always
 * returns a new {@link Map<K, Provider<V>>} instance for each call to {@link #get} (as required by {@link Factory}).
 *
 * @author Chenying Hou
 * @since 2.0
 * 
 * @param <V>
 * @param <K>
 */
public class MapProviderFactory<K, V> implements Factory<Map<K, Provider<V>>> {
  private final LinkedHashMap<K, Provider<V>> contributingMap;

  /**
   * A builder to help build the {@link MapProviderFactory}
   */
  public static class Builder<K, V> {
    private final LinkedHashMap<K, Provider<V>> mapBuilder;

    public Builder(int size) {
     //TODO(houcy): consider which way to initialize mapBuilder is better
      this.mapBuilder = new LinkedHashMap<K, Provider<V>>(size);
    }
    /**
     * Returns a new {@link MapProviderFactory} 
     */
    public MapProviderFactory<K, V> build() {
      return new MapProviderFactory<K, V>(this.mapBuilder);
    }
    /**
     * Associate k with providerOfValue in {@link Builder}
     */
    public Builder<K, V> put(K key, Provider<V> providerOfValue) {
      if (key == null) {
        throw new NullPointerException("The key is null");
      }
      if (providerOfValue == null) {
        throw new NullPointerException("The provider of the value is null");
      }

      this.mapBuilder.put(key, providerOfValue);
      return this;
    }
  }

  /**
   * Returns a new {@link Builder} 
   */
  public static <K, V> Builder<K, V> builder(int size) {
    return new Builder<K, V>(size);
  }

  private MapProviderFactory(LinkedHashMap<K, Provider<V>> contributingMap) {
    this.contributingMap = contributingMap;
  }

  /**
   * Returns a {@link Map<K, Provider<V>>} whose iteration order is that of the elements given by each of the
   * providers, which are invoked in the order given at creation.
   *
   */
  @Override
  public Map<K, Provider<V>> get() {
    if (this.contributingMap.isEmpty()) {
      throw new IllegalStateException("An empty map was provided");
    }
    return Collections.unmodifiableMap(this.contributingMap);
  }
}
