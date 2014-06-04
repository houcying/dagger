/*
 * Copyright (C) 2012 Google, Inc.
 * Copyright (C) 2012 Square, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A {@code Binding<T>} which contains contributors (other bindings marked with
 * {@code @Provides} {@code @OneOf}), to which it delegates provision
 * requests on an as-needed basis.
 * @param <V>
 * @param <K>
 */
public final class MapBinding<K, V> extends Binding<Map<K, V>> {

  public static <K, V> void add(BindingsGroup bindings, String setKey, Binding<?> binding) {
    prepareMapBinding(bindings, setKey, binding).contributors.add(Linker.scope(binding));
  }

  @SuppressWarnings("unchecked")
  private static <K, V> MapBinding<K, V> prepareMapBinding(
      BindingsGroup bindings, String setKey, Binding<?> binding) {
    Binding<?> previous = bindings.get(setKey);
    MapBinding<K, V> MapBinding;
    if (previous instanceof MapBinding) {
      MapBinding = (MapBinding<K, V>) previous;
      MapBinding.setLibrary(MapBinding.library() && binding.library());
      return MapBinding;
    } else if (previous != null) {
      throw new IllegalArgumentException("Duplicate:\n    " + previous + "\n    " + binding);
    } else {
      MapBinding = new MapBinding<K, V>(setKey, binding.requiredBy);
      MapBinding.setLibrary(binding.library());
      bindings.contributeMapBinding(setKey, MapBinding);
      return (MapBinding<K, V>) bindings.get(setKey); // BindingMap.put() copies MapBindings.
    }
  }

  /**
   * A {@link MapBinding} with whose contributing bindings this set-binding provides a union
   * view.
   */
  private final MapBinding<K, V> parent;

  /**
   * A {@link Set} of {@link Binding} instances which contribute values to the injected set.
   */
  private final List<Binding<?>> contributors;

  /**
   * Creates a new {@code MapBinding} with the given "provides" key, and the requiredBy object
   * for traceability.
   */
  public MapBinding(String key, Object requiredBy) {
    super(key, null, false, requiredBy);
    parent = null;
    contributors = new ArrayList<Binding<?>>();
  }

  /**
   * Creates a new {@code MapBinding} with all of the contributing bindings of the provided
   * original {@code MapBinding}.
   */
  public MapBinding(MapBinding<K, V> original) {
    super(original.provideKey, null, false, original.requiredBy);
    parent = original;
    this.setLibrary(original.library());
    this.setDependedOn(original.dependedOn());
    contributors = new ArrayList<Binding<?>>();
  }

  @Override public void attach(Linker linker) {
    for (Binding<?> contributor : contributors) {
      contributor.attach(linker);
    }
  }

  public int size() {
    int size = 0;
    for (MapBinding<K, V> binding = this; binding != null; binding = binding.parent) {
      size += binding.contributors.size();
    }
    return size;
  }

  @SuppressWarnings("unchecked") // Only Binding<T> and Set<T> are added to contributors.
  @Override public Map<K, V> get() {
    Map<K, V> result = new HashMap<K, V>();
    for (MapBinding<K, V> MapBinding = this; MapBinding != null; MapBinding = MapBinding.parent) {
      for (int i = 0, size = MapBinding.contributors.size(); i < size; i++) {
        Binding<?> contributor = MapBinding.contributors.get(i);
        Object contribution = contributor.get(); // Let runtime exceptions through.
        if (contributor.provideKey.equals(provideKey)) {        //?????? 
          result.addAll((Map<K, V>) contribution);
        } else {
          result.add((Entry<K, V>) contribution);
        }
      }
    }
    return Collections.unmodifiableSet(new LinkedHashSet<T>(result));
  }

  @Override public void getDependencies(
      Set<Binding<?>> getBindings, Set<Binding<?>> injectMembersBindings) {
    for (MapBinding<T> binding = this; binding != null; binding = binding.parent) {
      getBindings.addAll(binding.contributors);
    }
  }

  @Override public void injectMembers(Set<T> t) {
    throw new UnsupportedOperationException("Cannot inject members on a contributed Set<T>.");
  }

  @Override public String toString() {
    boolean first = true;
    StringBuilder builder = new StringBuilder("MapBinding[");
    for (MapBinding<T> MapBinding = this; MapBinding != null; MapBinding = MapBinding.parent) {
      for (int i = 0, size = MapBinding.contributors.size(); i < size; i++) {
        if (!first) {
          builder.append(",");
        }
        builder.append(MapBinding.contributors.get(i));
        first = false;
      }
    }
    builder.append("]");
    return builder.toString();
  }
}
