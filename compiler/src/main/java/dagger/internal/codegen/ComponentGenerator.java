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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static dagger.internal.codegen.DependencyRequest.Kind.MEMBERS_INJECTOR;
import static dagger.internal.codegen.InjectionAnnotations.getMapKey;
import static dagger.internal.codegen.ProvisionBinding.Kind.COMPONENT;
import static dagger.internal.codegen.ProvisionBinding.Kind.PROVISION;
import static dagger.internal.codegen.SourceFiles.factoryNameForProvisionBinding;
import static dagger.internal.codegen.SourceFiles.frameworkTypeUsageStatement;
import static dagger.internal.codegen.SourceFiles.generateMembersInjectorNamesForBindings;
import static dagger.internal.codegen.SourceFiles.generateProviderNamesForBindings;
import static dagger.internal.codegen.SourceFiles.membersInjectorNameForMembersInjectionBinding;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.type.TypeKind.VOID;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dagger.Component;
import dagger.MembersInjector;
import dagger.internal.InstanceFactory;
import dagger.internal.ScopedProvider;
import dagger.internal.SetFactory;
import dagger.internal.codegen.writer.ClassName;
import dagger.internal.codegen.writer.ClassWriter;
import dagger.internal.codegen.writer.ConstructorWriter;
import dagger.internal.codegen.writer.FieldWriter;
import dagger.internal.codegen.writer.JavaWriter;
import dagger.internal.codegen.writer.MethodWriter;
import dagger.internal.codegen.writer.ParameterizedTypeName;
import dagger.internal.codegen.writer.Snippet;
import dagger.internal.codegen.writer.StringLiteral;
import dagger.internal.codegen.writer.TypeName;
import dagger.internal.codegen.writer.TypeReferences;
import dagger.internal.codegen.writer.VoidName;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.inject.Provider;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Generates the implementation of the abstract types annotated with {@link Component}.
 *
 * @author Gregory Kick
 * @since 2.0
 */
final class ComponentGenerator extends SourceFileGenerator<ComponentDescriptor> {
  private final Key.Factory keyFactory;

  ComponentGenerator(Filer filer, Key.Factory keyFactory) {
    super(filer);
    this.keyFactory = keyFactory;
  }

  @Override
  ClassName nameGeneratedType(ComponentDescriptor input) {
    ClassName componentDefinitionClassName =
        ClassName.fromTypeElement(input.componentDefinitionType());
    return componentDefinitionClassName.topLevelClassName().peerNamed(
        "Dagger_" + componentDefinitionClassName.classFileName());
  }

  @Override
  Iterable<? extends Element> getOriginatingElements(ComponentDescriptor input) {
    return ImmutableSet.of(input.componentDefinitionType());
  }

  @Override
  Optional<? extends Element> getElementForErrorReporting(ComponentDescriptor input) {
    return Optional.of(input.componentDefinitionType());
  }

  @Override
  JavaWriter write(ClassName componentName, ComponentDescriptor input)  {
    JavaWriter writer = JavaWriter.inPackage(componentName.packageName());

    ClassWriter componentWriter = writer.addClass(componentName.simpleName());
    componentWriter.annotate(Generated.class).setValue(ComponentProcessor.class.getCanonicalName());
    componentWriter.addModifiers(PUBLIC, FINAL);
    componentWriter.addImplementedType(input.componentDefinitionType());

    ImmutableSetMultimap<Key, ProvisionBinding> resolvedProvisionBindings =
        input.resolvedProvisionBindings();
    ImmutableMap<Key, MembersInjectionBinding> resolvedMembersInjectionBindings =
        input.resolvedMembersInjectionBindings();

    ImmutableBiMap<Key, String> providerNames =
        generateProviderNamesForBindings(resolvedProvisionBindings);
    ImmutableBiMap<Key, String> membersInjectorNames =
        generateMembersInjectorNamesForBindings(resolvedMembersInjectionBindings);

    ImmutableBiMap<TypeElement, String> moduleNames =
        ImmutableBiMap.copyOf(Maps.asMap(input.moduleDependencies(), Functions.compose(
            CaseFormat.UPPER_CAMEL.converterTo(LOWER_CAMEL),
            new Function<TypeElement, String>() {
              @Override public String apply(TypeElement input) {
                return input.getSimpleName().toString();
              }
            })));

    ConstructorWriter constructorWriter = componentWriter.addConstructor();
    constructorWriter.addModifiers(PUBLIC);
    for (Entry<TypeElement, String> entry : moduleNames.entrySet()) {
      componentWriter.addField(entry.getKey(), entry.getValue())
          .addModifiers(PRIVATE, FINAL);
      constructorWriter.addParameter(entry.getKey(), entry.getValue());
      constructorWriter.body()
          .addSnippet("if (%s == null) {", entry.getValue())
          .addSnippet("  throw new NullPointerException(%s);",
              StringLiteral.forValue(entry.getValue()))
          .addSnippet("}")
          .addSnippet("this.%1$s = %1$s;", entry.getValue());

    }

    for (Entry<Key, String> providerEntry : providerNames.entrySet()) {
      Key key = providerEntry.getKey();
      // TODO(gak): provide more elaborate information about which requests relate
      TypeName providerTypeReferece = ParameterizedTypeName.create(
          ClassName.fromClass(Provider.class),
          TypeReferences.forTypeMirror(key.type()));
      FieldWriter providerField =
          componentWriter.addField(providerTypeReferece, providerEntry.getValue());
      providerField.addModifiers(PRIVATE, FINAL);
    }
    for (Entry<Key, String> providerEntry : membersInjectorNames.entrySet()) {
      Key key = providerEntry.getKey();
      // TODO(gak): provide more elaborate information about which requests relate
      TypeName membersInjectorTypeReferece = ParameterizedTypeName.create(
          ClassName.fromClass(MembersInjector.class),
          TypeReferences.forTypeMirror(key.type()));
      FieldWriter membersInjectorField =
          componentWriter.addField(membersInjectorTypeReferece, providerEntry.getValue());
      membersInjectorField.addModifiers(PRIVATE, FINAL);
    }

    for (FrameworkKey frameworkKey : input.initializationOrdering()) {
      Key key = frameworkKey.key();
      if (frameworkKey.frameworkClass().equals(Provider.class)) {
        Set<ProvisionBinding> bindings = resolvedProvisionBindings.get(key);
        boolean setBinding;
        boolean mapBinding;
        if ((setBinding = ProvisionBinding.isSetBindingCollection(bindings)) == true) {
          ImmutableList.Builder<Snippet> setFactoryParameters = ImmutableList.builder();
          for (ProvisionBinding binding : bindings) {
            setFactoryParameters.add(initializeFactoryForBinding(
                binding, moduleNames, providerNames,membersInjectorNames));
          }
          constructorWriter.body().addSnippet("this.%s = %s.create(%n%s);",
              providerNames.get(key),
              ClassName.fromClass(SetFactory.class),
              Snippet.makeParametersSnippet(setFactoryParameters.build()));
        } else if ((mapBinding = ProvisionBinding.isMapBindingCollection(bindings)) == true) {
          ImmutableList.Builder<Snippet> mapFactoryParameters = ImmutableList.builder();
          boolean isFirstBinding = true;
          for (ProvisionBinding binding : bindings) {
            ExecutableElement e = (ExecutableElement) binding.bindingElement();
            ImmutableSet<? extends AnnotationMirror> annotationmirrors = getMapKey(e);
            Map<? extends ExecutableElement, ? extends AnnotationValue> map = annotationmirrors.iterator().next().getElementValues();
            //get the key and value type of the map 
            if (isFirstBinding) {
              DeclaredType declaredMapType = (DeclaredType) binding.providedKey().type();
              List<? extends TypeMirror> mapArgs = declaredMapType.getTypeArguments();
              TypeMirror keyType =  mapArgs.get(0);
              DeclaredType declaredValueType = (DeclaredType) mapArgs.get(1);
              List<? extends TypeMirror> mapValueArgs = declaredValueType.getTypeArguments();
              TypeMirror valueType = mapValueArgs.get(0);

             /* mapFactoryParameters.add(Snippet.create(providerNames.get(key), null)); 
              mapFactoryParameters.add(Snippet.create(keyType.toString(), null));
              mapFactoryParameters.add(Snippet.create(valueType.toString(), null)); 
              mapFactoryParameters.add(Snippet.create(Integer.toString(bindings.size()), null));*/

              isFirstBinding = false;
            }
            mapFactoryParameters.add(Snippet.create(map.entrySet().iterator().next().getValue().toString(), null));
            mapFactoryParameters.add(initializeFactoryForBinding(
                binding, moduleNames, providerNames,membersInjectorNames));
          }
          
          StringBuilder mapPattern = new StringBuilder("this.%s = MapProviderFactory.<%s, %s>builder(%s)");
          for (int i = 0; i < mapFactoryParameters.build().size() - 4; i += 2) {
            mapPattern.append("%n.put(%s, %s)");
          }
          mapPattern.append("%n.build()");
          constructorWriter.body().addSnippet(mapPattern.toString(),
              mapFactoryParameters.build().toArray()); 
        } else if (ProvisionBinding.isNotACollection(setBinding, mapBinding, bindings)) {
          ProvisionBinding binding = Iterables.getOnlyElement(bindings);
          constructorWriter.body().addSnippet("this.%s = %s;",
              providerNames.get(key),
              initializeFactoryForBinding(
                  binding, moduleNames, providerNames, membersInjectorNames));
        }
      } else if (frameworkKey.frameworkClass().equals(MembersInjector.class)) {
        constructorWriter.body().addSnippet("this.%s = %s;",
            membersInjectorNames.get(key),
            initializeMembersInjectorForBinding(resolvedMembersInjectionBindings.get(key),
                providerNames, membersInjectorNames));
      } else {
        throw new IllegalStateException(
            "unknown framework class: " + frameworkKey.frameworkClass());
      }
    }

    for (DependencyRequest interfaceRequest : input.interfaceRequests()) {
      ExecutableElement requestElement = (ExecutableElement) interfaceRequest.requestElement();
      MethodWriter interfaceMethod = requestElement.getReturnType().getKind().equals(VOID)
          ? componentWriter.addMethod(VoidName.VOID, requestElement.getSimpleName().toString())
          : componentWriter.addMethod(requestElement.getReturnType(),
              requestElement.getSimpleName().toString());
      interfaceMethod.annotate(Override.class);
      interfaceMethod.addModifiers(PUBLIC);
      if (interfaceRequest.kind().equals(MEMBERS_INJECTOR)) {
        String membersInjectorName = membersInjectorNames.get(interfaceRequest.key());
        VariableElement parameter = Iterables.getOnlyElement(requestElement.getParameters());
        Name parameterName = parameter.getSimpleName();
        interfaceMethod.addParameter(
            TypeReferences.forTypeMirror(parameter.asType()), parameterName.toString());
        interfaceMethod.body()
            .addSnippet("%s.injectMembers(%s);", membersInjectorName, parameterName);
        if (!requestElement.getReturnType().getKind().equals(VOID)) {
          interfaceMethod.body().addSnippet("return %s;", parameterName);
        }
      } else {
        // provision requests
        String providerName = providerNames.get(interfaceRequest.key());

        // look up the provider in the Key->name map and invoke.  Done.
        interfaceMethod.body().addSnippet("return %s;",
            frameworkTypeUsageStatement(providerName, interfaceRequest.kind()));
      }
    }

    return writer;
  }

  private Snippet initializeFactoryForBinding(ProvisionBinding binding,
      ImmutableBiMap<TypeElement, String> moduleNames,
      ImmutableBiMap<Key, String> providerNames,
      ImmutableBiMap<Key, String> membersInjectorNames) {
    if (binding.bindingKind().equals(COMPONENT)) {
      return Snippet.format("%s.<%s>create(this)",
          ClassName.fromClass(InstanceFactory.class),
          TypeReferences.forTypeMirror(binding.providedKey().type()));
    } else {
      List<String> parameters = Lists.newArrayListWithCapacity(binding.dependencies().size() + 1);
      if (binding.bindingKind().equals(PROVISION)) {
        parameters.add(moduleNames.get(binding.bindingTypeElement()));
      }
      if (binding.requiresMemberInjection()) {
        String membersInjectorName =
            membersInjectorNames.get(keyFactory.forType(binding.providedKey().type()));
        if (membersInjectorName != null) {
          parameters.add(membersInjectorName);
        } else {
          throw new UnsupportedOperationException("Non-generated MembersInjector");
        }
      }
      parameters.addAll(
          getDependencyParameters(binding.dependencies(), providerNames, membersInjectorNames));
      return binding.scope().isPresent()
          ? Snippet.format("%s.create(new %s(%s))",
              ClassName.fromClass(ScopedProvider.class),
              factoryNameForProvisionBinding(binding).toString(),
              Joiner.on(", ").join(parameters))
          : Snippet.format("new %s(%s)",
              factoryNameForProvisionBinding(binding).toString(),
              Joiner.on(", ").join(parameters));
    }
  }

  private static Snippet initializeMembersInjectorForBinding(
      MembersInjectionBinding binding,
      ImmutableBiMap<Key, String> providerNames,
      ImmutableBiMap<Key, String> membersInjectorNames) {
    List<String> parameters = getDependencyParameters(binding.dependencySet(),
        providerNames, membersInjectorNames);
    return Snippet.format("new %s(%s)",
       membersInjectorNameForMembersInjectionBinding(binding).toString(),
        Joiner.on(", ").join(parameters));
  }

  private static List<String> getDependencyParameters(Iterable<DependencyRequest> dependencies,
      ImmutableBiMap<Key, String> providerNames,
      ImmutableBiMap<Key, String> membersInjectorNames) {
    ImmutableList.Builder<String> parameters = ImmutableList.builder();
    for (DependencyRequest dependency : dependencies) {
        parameters.add(dependency.kind().equals(MEMBERS_INJECTOR)
            ? membersInjectorNames.get(dependency.key())
            : providerNames.get(dependency.key()));
    }
    return parameters.build();
  }
}
