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
package dagger.internal.codegen;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.truth0.Truth.ASSERT;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
/**
 * TODO: Insert description here. (generated by houcy)
 */
public class MapBindingModuleProcessorTest {
  @Ignore @Test public void proviesValueWithEnumKey() {
    JavaFileObject moduleFile = JavaFileObjects.forSourceLines("test.TestModule",
        "package test;",
        "",
        "import static dagger.Provides.Type.MAP;",
        "",
        "import dagger.Module;",
        "import dagger.Provides;",

        "",
        "@Module",
        "final class MapModuleOne {",
        "  @Provides(type = MAP) @EnumKey(Admin) Handler provideAdminHandler() { return new AdminHandler(); }",
        "}");
    JavaFileObject enumKeyFile = JavaFileObjects.forSourceLines("test.EnumKey", 
        "package test;",
        "",
        "@MapKey",
        "public @interface EnumKey {",
        "  enum PathEnum {",
        "    Admin(\"/admin\"),",
        "    Login(\"/login\");",
        "    private final String path;",
        "    PathEnum(String path) {",
        "      this.path = path;",
        "    }",
        "  }",
        "  PathEnum value();",
        "}");
    JavaFileObject factoryFile = JavaFileObjects.forSourceLines("TestModule$$ProvideAdminHandlerFactory",
        "package test;",
        "",
        "import dagger.Factory;",
        "import javax.annotation.Generated;",
        "",
        "@Generated(\"dagger.internal.codegen.ComponentProcessor\")",
        "public final class TestModule$$ProvideAdminHandlerFactory implements Factory<Handler> {",
        "  private final TestModule module;",
        "",
        "  public TestModule$$ProvideLoginHandlerFactory(TestModule module) {",
        "    assert module != null;",
        "    this.module = module;",
        "  }",
        "",
        //or @Provides(type = MAP) @EnumKey(Admin) public Handler get() {}
        "  @Override public Handler get() {",
        "    return module.provideAdminHandler();",
        "  }",
        "}");
    ASSERT.about(javaSource())
        .that((JavaFileObject) ImmutableList.of(moduleFile, enumKeyFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(factoryFile);
  }

  @Ignore @Test public void proviesValueWithStringKey() {
    JavaFileObject moduleFile = JavaFileObjects.forSourceLines("test.TestModule",
        "package test;",
        "",
        "import static dagger.Provides.Type.MAP;",
        "",
        "import dagger.Module;",
        "import dagger.Provides;",
        "",
        "@Module",
        "final class MapModuleTwo {",
        "  @Provides(type = MAP) @StringKey(\"login\") Handler provideLoginHandler() { return new LoginHandler(); }",
        "}");
    JavaFileObject stringKeyFile = JavaFileObjects.forSourceLines("test.StringKey", 
        "package test;",
        "",
        "@MapKey",
        "public @interface StringKey {",
        "  String value();",
        "}");
    JavaFileObject factoryFile = JavaFileObjects.forSourceLines("TestModule$$ProvideLoginHandlerFactory",
        "package test;",
        "",
        "import dagger.Factory;",
        "import javax.annotation.Generated;",
        "",
        "@Generated(\"dagger.internal.codegen.ComponentProcessor\")",
        "public final class TestModule$$ProvideLoginHandlerFactory implements Factory<Handler> {",
        "  private final TestModule module;",
        "",
        "  public TestModule$$ProvideLoginHandlerFactory(TestModule module) {",
        "    assert module != null;",
        "    this.module = module;",
        "  }",
        "",
        "  @Override public Handler get() {",
        "    return module.provideLoginHandler();",
        "  }",
        "}");
    ASSERT.about(javaSource())
        .that((JavaFileObject) ImmutableList.of(moduleFile, stringKeyFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(factoryFile);
  }
}
