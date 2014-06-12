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

import static java.util.Collections.unmodifiableMap;

import dagger.Factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;


public final class MapFactory<K, V> implements Factory<Map<K, V>> {
  /**
   * Returns a new factory that creates {@link Set} instances that from the union of the given
   * {@link Provider} instances.
   */
  public static <K, V> Factory<Map<K, V>> create(Provider<Map<K, V>> first,
      @SuppressWarnings("unchecked") Provider<Map<K, V>>... rest) {
    if (first == null) {
      throw new NullPointerException();
    }
    if (rest == null) {
      throw new NullPointerException();
    }
    Set<Provider<Map<K, V>>> contributingProviders 
                             = newLinkedHashSetWithExpectedSize(1 + rest.length);
    contributingProviders.add(first);
    for (Provider<Map<K, V>> provider : rest) {
      if (provider == null) {
        throw new NullPointerException();
      }
      contributingProviders.add(provider);
    }
    return new MapFactory<K, V>(contributingProviders);
  }

  private final Set<Provider<Map<K, V>>> contributingProviders;

  private MapFactory(Set<Provider<Map<K, V>>> contributingProviders) {
    this.contributingProviders = contributingProviders;
  }

  /**
   * Returns a {@link Set} whose iteration order is that of the elements given by each of the
   * providers, which are invoked in the order given at creation.
   *
   * @throws NullPointerException if any of the delegate {@link Set} instances or elements therein
   *     are {@code null}
   */
  @Override
  public Map<K, V> get() {
    List<Map<K, V>> providedSets = new ArrayList<Map<K, V>>(contributingProviders.size());
    for (Provider<Map<K, V>> provider : contributingProviders) {
      Map<K, V> providedSet = provider.get();
      if (providedSet == null) {
        throw new NullPointerException(provider + " returned null");
      }
      providedSets.add(providedSet);
    }
    int size = 0;
    for (Map<K, V> providedSet : providedSets) {
      size += providedSet.size();
    }
    Map<K, V> result = newLinkedHashMapWithExpectedSize(size);

    for (Map<K, V> s : providedSets) {
      for (Map.Entry<K, V> entry : s.entrySet()) {
        if (entry == null) {
          throw new NullPointerException("a null element was provided");
        }
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return unmodifiableMap(result);
  }

  // TODO(gak): consider whether (expectedSize, 1.0f) is better for this use case since callers are
  // typically only going to iterate
  private static <E> LinkedHashSet<E> newLinkedHashSetWithExpectedSize(int expectedSize) {
    int initialCapacity = (expectedSize < 3)
        ? expectedSize + 1
        : (expectedSize < (1 << (Integer.SIZE - 2)))
            ? expectedSize + expectedSize / 3
            : Integer.MAX_VALUE;
    return new LinkedHashSet<E>(initialCapacity);
  }

  // TODO(gak): consider whether (expectedSize, 1.0f) is better for this use case since callers are
  // typically only going to iterate
  private static <K, V> LinkedHashMap<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
    int initialCapacity = (expectedSize < 3)
        ? expectedSize + 1
        : (expectedSize < (1 << (Integer.SIZE - 2)))
            ? expectedSize + expectedSize / 3
            : Integer.MAX_VALUE;
    return new LinkedHashMap<K, V>(initialCapacity);
  }
}
