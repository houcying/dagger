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

import static org.truth0.Truth.ASSERT;

import dagger.Factory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Provider;

@RunWith(JUnit4.class)
@SuppressWarnings("unchecked")
public class MapProviderFactoryTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void nullKey() {
    thrown.expect(NullPointerException.class);
    MapProviderFactory.<String, Integer>builder(1)
    .put(null, incrementingIntegerProvider(1));
  }

  @Test
  public void nullValue() {
    thrown.expect(NullPointerException.class);
    MapProviderFactory.<String, Integer>builder(1)
    .put("Hello", null);
  }
  
  @Test
  public void emptyMap() {
    thrown.expect(IllegalStateException.class);
    MapProviderFactory.<String, Integer>builder(0).build().get();
  }
  
  @Test
  public void invokesProvidersEverytTime() {
    Provider<Integer> p1 = incrementingIntegerProvider(10);
    Provider<Integer> p2 = incrementingIntegerProvider(20);
    Provider<Integer> p3 = incrementingIntegerProvider(30);
    
    Factory<Map<String, Provider<Integer>>> factory = MapProviderFactory.<String, Integer>builder(3)
        .put("one", p1)
        .put("two", p2)
        .put("three", p3)
        .build();
    
    int base = 10;
    for (int i = 0; i < 3; i++) {
      Iterator<Entry<String, Provider<Integer>>> iterator = factory.get().entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Provider<Integer>> entry = iterator.next();  
        ASSERT.that(entry.getValue().get()).is(base);
        base += 10;
      }
      base = 11 + i;
    }
    
  }
  
  @Test
  public void iterationOrder() {
    Provider<Integer> p1 = incrementingIntegerProvider(10);
    Provider<Integer> p2 = incrementingIntegerProvider(20);
    Provider<Integer> p3 = incrementingIntegerProvider(30);
    Provider<Integer> p4 = incrementingIntegerProvider(40);
    Provider<Integer> p5 = incrementingIntegerProvider(50);
    
    Factory<Map<String, Provider<Integer>>> factory = MapProviderFactory.<String, Integer>builder(4)
        .put("two", p2)
        .put("one", p1)
        .put("three", p3)
        .put("one", p5)
        .put("four", p4)
        .build();

    Map<String, Provider<Integer>> expectedMap = new LinkedHashMap<String, Provider<Integer>>();
    expectedMap.put("two", p2);
    expectedMap.put("one", p1);
    expectedMap.put("three", p3);
    expectedMap.put("one", p5);
    expectedMap.put("four", p4);
    ASSERT.that(factory.get().entrySet()).iteratesAs(expectedMap.entrySet());
   /* ASSERT.that(factory.get().entrySet()).iteratesAs(new AbstractMap.SimpleEntry<String, Provider<Integer>>("two", p2), 
        new AbstractMap.SimpleEntry<String, Provider<Integer>>("one", p1),
        new AbstractMap.SimpleEntry<String, Provider<Integer>>("three", p3),
        new AbstractMap.SimpleEntry<String, Provider<Integer>>("four", p4));*/
  }
  
  
  private static Provider<Integer> incrementingIntegerProvider(int seed) {
    final AtomicInteger value = new AtomicInteger(seed);
    return new Provider<Integer>() {
      @Override
      public Integer get() {
        return value.getAndIncrement();
      }
    };
  }
}
