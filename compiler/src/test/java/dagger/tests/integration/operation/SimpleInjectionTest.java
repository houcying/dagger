/**
 * Copyright (C) 2013 Google, Inc.
 * Copyright (C) 2013 Square, Inc.
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
package dagger.tests.integration.operation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SimpleInjectionTest {
  @Test public void foo() {}
  /*
  static abstract class AbstractFoo {
    @Inject String blah;
  }

  static class Foo extends AbstractFoo { }

  @Module(injects = Foo.class)
  static class FooModule {
    @Provides String string() { return "blah"; }
  }

  @Module(injects = Foo.class)
  static class ProvidingFooModule {
    @Provides String string() { return "blah"; }
    @Provides Foo foo(String blah) {
      Foo foo = new Foo();
      foo.blah = blah;
      return foo;
    }
  }

  @Test public void memberInject_WithoutProvidesMethod() {
    Foo foo = new Foo();
    ObjectGraph.create(FooModule.class).inject(foo);
    ASSERT.that(foo.blah).equals("blah");
  }

  @Test public void membersInject_WithProvidesMethod() {
    Foo foo = new Foo();
    ObjectGraph.create(ProvidingFooModule.class).inject(foo);
    ASSERT.that(foo.blah).equals("blah");
  }

  @Test public void get_WithProvidesMethod() {
    Foo foo = ObjectGraph.create(ProvidingFooModule.class).get(Foo.class);
    ASSERT.that(foo.blah).equals("blah");
  }

  static class Bar { }

  @Module(injects = Bar.class)
  static class BarModule {
  }

  @Test public void membersInject_WithNonInjectable() {
    Bar bar = new Bar();
    ObjectGraph.create(BarModule.class).inject(bar);
  }

  @Module(injects = Bar.class)
  static class ProvidingBarModule {
    @Provides public Bar bar() { return new Bar(); }
  }

  @Test public void membersInject_WithProvidedNonInjectable() {
    Bar bar = ObjectGraph.create(ProvidingBarModule.class).get(Bar.class);
    ASSERT.that(bar).isNotNull();
  }
  */
}
