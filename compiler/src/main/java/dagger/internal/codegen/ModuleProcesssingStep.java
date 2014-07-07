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

import com.google.auto.common.MoreElements;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import dagger.Module;
import dagger.Provides;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import static javax.lang.model.element.ElementKind.METHOD;

/**
 * An annotation processor for generating Dagger implementation code based on the {@link Module}
 * (and {@link Provides}) annotation.
 *
 * @author Gregory Kick
 * @since 2.0
 */
public final class ModuleProcesssingStep implements ProcessingStep {
  private final Messager messager;
  private final ModuleValidator moduleValidator;
  private final ProvidesMethodValidator providesMethodValidator;
  private final ProvisionBinding.Factory provisionBindingFactory;
  private final FactoryGenerator factoryGenerator;

  ModuleProcesssingStep(
      Messager messager,
      ModuleValidator moduleValidator,
      ProvidesMethodValidator providesMethodValidator,
      ProvisionBinding.Factory provisionBindingFactory,
      FactoryGenerator factoryGenerator) {
    this.messager = messager;
    this.moduleValidator = moduleValidator;
    this.providesMethodValidator = providesMethodValidator;
    this.provisionBindingFactory = provisionBindingFactory;
    this.factoryGenerator = factoryGenerator;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // first, check and collect all provides methods
    ImmutableSet.Builder<ExecutableElement> validProvidesMethodsBuilder = ImmutableSet.builder();
    for (Element providesElement : roundEnv.getElementsAnnotatedWith(Provides.class)) {
      if (providesElement.getKind().equals(METHOD)) {
        ExecutableElement providesMethodElement = (ExecutableElement) providesElement;
        ValidationReport<ExecutableElement> methodReport =
            providesMethodValidator.validate(providesMethodElement);
        methodReport.printMessagesTo(messager);
        if (methodReport.isClean()) {
          validProvidesMethodsBuilder.add(providesMethodElement);
        }
      }
    }
    ImmutableSet<ExecutableElement> validProvidesMethods = validProvidesMethodsBuilder.build();
    // process each module
    for (Element moduleElement : roundEnv.getElementsAnnotatedWith(Module.class)) {
      ValidationReport<TypeElement> report =
          moduleValidator.validate(MoreElements.asType(moduleElement));
      report.printMessagesTo(messager);

      if (report.isClean()) {
        ImmutableSet.Builder<ExecutableElement> moduleProvidesMethodsBuilder =
            ImmutableSet.builder();
        List<ExecutableElement> moduleMethods =
            ElementFilter.methodsIn(moduleElement.getEnclosedElements());
        for (ExecutableElement methodElement : moduleMethods) {
          if (methodElement.getAnnotation(Provides.class) != null) {
            moduleProvidesMethodsBuilder.add(methodElement);
          }
        }
        ImmutableSet<ExecutableElement> moduleProvidesMethods =
            moduleProvidesMethodsBuilder.build();

        if (Sets.difference(moduleProvidesMethods, validProvidesMethods).isEmpty()) {
          // all of the provides methods in this module are valid!
          // time to generate some factories!
          ImmutableSet<ProvisionBinding> bindings = FluentIterable.from(moduleProvidesMethods)
              .transform(new Function<ExecutableElement, ProvisionBinding>() {
                @Override
                public ProvisionBinding apply(ExecutableElement providesMethod) {
                  return provisionBindingFactory.forProvidesMethod(providesMethod);
                }
              })
              .toSet();

          try {
            for (ProvisionBinding binding : bindings) {
              factoryGenerator.generate(binding);
            }
          } catch (SourceFileGenerationException e) {
            e.printMessageTo(messager);
          }
        }
      }
    }

    return false;
  }
}
