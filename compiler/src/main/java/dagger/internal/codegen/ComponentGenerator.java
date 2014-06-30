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

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.squareup.javawriter.JavaWriter;

import dagger.Component;
import dagger.MembersInjector;
import dagger.internal.InstanceFactory;
import dagger.internal.MapProviderFactory;
import dagger.internal.ScopedProvider;
import dagger.internal.SetFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.inject.Provider;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static dagger.Provides.Type.MAP;
import static dagger.Provides.Type.SET;
import static dagger.Provides.Type.SET_VALUES;
import static dagger.internal.codegen.DependencyRequest.Kind.MEMBERS_INJECTOR;
import static dagger.internal.codegen.InjectionAnnotations.getMapKey;
import static dagger.internal.codegen.ProvisionBinding.Kind.COMPONENT;
import static dagger.internal.codegen.ProvisionBinding.Kind.PROVISION;
import static dagger.internal.codegen.SourceFiles.collectImportsFromDependencies;
import static dagger.internal.codegen.SourceFiles.factoryNameForProvisionBinding;
import static dagger.internal.codegen.SourceFiles.flattenVariableMap;
import static dagger.internal.codegen.SourceFiles.generateMembersInjectorNamesForBindings;
import static dagger.internal.codegen.SourceFiles.generateProviderNamesForBindings;
import static dagger.internal.codegen.SourceFiles.membersInjectorNameForMembersInjectionBinding;
import static dagger.internal.codegen.SourceFiles.providerUsageStatement;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.type.TypeKind.VOID;

/**
 * Generates the implementation of the abstract types annotated with {@link Component}.
 *
 * @author Gregory Kick
 * @since 2.0
 */
final class ComponentGenerator extends SourceFileGenerator<ComponentDescriptor> {
  private final Elements elements;
  private final Types types;
  private final Key.Factory keyFactory;

  ComponentGenerator(Filer filer, Elements elements, Types types, Key.Factory keyFactory) {
    super(filer);
    this.elements = checkNotNull(elements);
    this.types = checkNotNull(types);
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
  void write(ClassName componentName, JavaWriter writer, ComponentDescriptor input)
      throws IOException {
    writer.emitPackage(componentName.packageName());

    writeImports(writer, componentName, input);

    writer.emitAnnotation(Generated.class, stringLiteral(ComponentProcessor.class.getName()));
    writer.beginType(componentName.simpleName(), "class", EnumSet.of(PUBLIC, FINAL), null,
        input.componentDefinitionType().getQualifiedName().toString());

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

    writeModuleFields(writer, moduleNames);
    writeProviderFields(writer, providerNames);
    writeMembersInjectorFields(writer, membersInjectorNames);

    writeConstructor(writer, input.initializationOrdering(), resolvedProvisionBindings,
        resolvedMembersInjectionBindings, providerNames, moduleNames, membersInjectorNames);

    writeInterfaceMethods(writer, input.interfaceRequests(), providerNames, membersInjectorNames);

    writer.endType();
  }

  private void writeImports(JavaWriter writer, ClassName factoryClassName,
      ComponentDescriptor input) throws IOException {

    ImmutableSortedSet.Builder<ClassName> importsBuilder =
        ImmutableSortedSet.<ClassName>naturalOrder()
            .addAll(collectImportsFromDependencies(factoryClassName, input.interfaceRequests()))
            .add(ClassName.fromClass(Generated.class))
            .add(ClassName.fromClass(Provider.class));

    ClassName componentClassName = ClassName.fromTypeElement(input.componentDefinitionType());
    if (!componentClassName.enclosingSimpleNames().isEmpty()) {
      importsBuilder.add(componentClassName);
    }

    for (ProvisionBinding binding : input.resolvedProvisionBindings().values()) {
      if (binding.scope().isPresent()) {
        importsBuilder.add(ClassName.fromClass(ScopedProvider.class));
      }
      if (binding.bindingKind().equals(COMPONENT)) {
        importsBuilder.add(ClassName.fromClass(InstanceFactory.class));
      }
      if (binding.provisionType().equals(SET) || binding.provisionType().equals(SET_VALUES)) {
        importsBuilder.add(ClassName.fromClass(SetFactory.class));
      }
      if (binding.provisionType().equals(MAP)) {
        importsBuilder.add(ClassName.fromClass(MapProviderFactory.class));
      }
      if (binding.requiresMemberInjection()) {
        importsBuilder.add(ClassName.fromClass(MembersInjector.class));
      }
      for (TypeElement referencedType : MoreTypes.referencedTypes(binding.providedKey().type())) {
        ClassName className = ClassName.fromTypeElement(referencedType);
        if (!className.packageName().equals("java.lang")
            && !className.packageName().equals(factoryClassName.packageName()))
          importsBuilder.add(className);
      }
    }

    writer.emitImports(Collections2.transform(importsBuilder.build(), Functions.toStringFunction()))
        .emitEmptyLine();
  }

  private void writeModuleFields(JavaWriter writer,
      ImmutableBiMap<TypeElement, String> moduleDependencies) throws IOException {
    for (Entry<TypeElement, String> entry : moduleDependencies.entrySet()) {
      writer.emitField(entry.getKey().getQualifiedName().toString(), entry.getValue(),
          EnumSet.of(PRIVATE, FINAL));
    }
  }

  private void writeProviderFields(JavaWriter writer, ImmutableBiMap<Key, String> providerNames)
      throws IOException {
    for (Entry<Key, String> providerEntry : providerNames.entrySet()) {
      Key key = providerEntry.getKey();
      // TODO(gak): provide more elaborate information about which requests relate
      writer.emitJavadoc(key.toString())
          .emitField(providerTypeString(key), providerEntry.getValue(),
              EnumSet.of(PRIVATE, FINAL));
    }
    writer.emitEmptyLine();
  }

  private void writeMembersInjectorFields(JavaWriter writer,
      ImmutableBiMap<Key, String> membersInjectorNames) throws IOException {
    for (Entry<Key, String> providerEntry : membersInjectorNames.entrySet()) {
      Key key = providerEntry.getKey();
      // TODO(gak): provide more elaborate information about which requests relate
      writer.emitJavadoc(key.toString())
          .emitField(membersInjectorTypeString(key), providerEntry.getValue(),
              EnumSet.of(PRIVATE, FINAL));
    }
    writer.emitEmptyLine();
  }

  private void writeConstructor(final JavaWriter writer,  
      ImmutableList<FrameworkKey> initializationOrdering,
      ImmutableSetMultimap<Key, ProvisionBinding> resolvedProvisionBindings,
      ImmutableMap<Key, MembersInjectionBinding> resolvedMembersInjectionBindings,
      ImmutableBiMap<Key, String> providerNames,
      ImmutableBiMap<TypeElement, String> moduleNames,
      ImmutableBiMap<Key, String> membersInjectorNames)
          throws IOException {
    Map<String, String> variableMap =
        Maps.transformValues(moduleNames.inverse(), new Function<TypeElement, String>() {
      @Override
      public String apply(TypeElement input) {
        return writer.compressType(input.getQualifiedName().toString());
      }
    });

    writer.beginConstructor(EnumSet.of(PUBLIC), flattenVariableMap(variableMap),
        ImmutableList.<String>of());
    for (String variableName : variableMap.keySet()) {
      writer.beginControlFlow("if (%s == null)", variableName)
          .emitStatement("throw new NullPointerException(\"%s\")", variableName)
          .endControlFlow();
      writer.emitStatement("this.%1$s = %1$s", variableName);
    }

    for (FrameworkKey frameworkKey : initializationOrdering) {
      Key key = frameworkKey.key();
      if (frameworkKey.frameworkClass().equals(Provider.class)) {
        Set<ProvisionBinding> bindings = resolvedProvisionBindings.get(key);
        boolean setBinding;
        boolean mapBinding;
        if ((setBinding = ProvisionBinding.isSetBindingCollection(bindings)) == true) {
          ImmutableList.Builder<String> setFactoryParameters = ImmutableList.builder();
          for (ProvisionBinding binding : bindings) {
            setFactoryParameters.add(initializeFactoryForBinding(
                writer, binding, moduleNames, providerNames,membersInjectorNames));
          }
          writer.emitStatement("this.%s = SetFactory.create(%n%s)",
              providerNames.get(key),
              Joiner.on(",\n").join(setFactoryParameters.build()));
        } else if ((mapBinding = ProvisionBinding.isMapBindingCollection(bindings)) == true) {
          ImmutableList.Builder<String> mapFactoryParameters = ImmutableList.builder();
         
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

              mapFactoryParameters.add(providerNames.get(key)); 
              mapFactoryParameters.add(keyType.toString());
              mapFactoryParameters.add(valueType.toString()); 
              mapFactoryParameters.add(Integer.toString(bindings.size()));

              isFirstBinding = false;
            }
            mapFactoryParameters.add(map.entrySet().iterator().next().getValue().toString());
            mapFactoryParameters.add(initializeFactoryForBinding(
                writer, binding, moduleNames, providerNames,membersInjectorNames));
          }
          
          StringBuilder mapPattern = new StringBuilder("this.%s = MapProviderFactory.<%s, %s>builder(%s)");
          for (int i = 0; i < mapFactoryParameters.build().size() - 4; i += 2) {
            mapPattern.append("%n.put(%s, %s)");
          }
          mapPattern.append("%n.build()");
          writer.emitStatement(mapPattern.toString(),
              mapFactoryParameters.build().toArray());  
        } else if (ProvisionBinding.isNotACollection(setBinding, mapBinding, bindings)) {
          ProvisionBinding binding = Iterables.getOnlyElement(bindings);
          writer.emitStatement("this.%s = %s",
              providerNames.get(key),
              initializeFactoryForBinding(
                  writer, binding, moduleNames, providerNames, membersInjectorNames));
        }
      } else if (frameworkKey.frameworkClass().equals(MembersInjector.class)) {
        writer.emitStatement("this.%s = %s",
            membersInjectorNames.get(key),
            initializeMembersInjectorForBinding(writer, resolvedMembersInjectionBindings.get(key),
                providerNames, membersInjectorNames));
      } else {
        throw new IllegalStateException(
            "unknown framework class: " + frameworkKey.frameworkClass());
      }
    }

    writer.endConstructor().emitEmptyLine();
  }

  private String initializeFactoryForBinding(JavaWriter writer, ProvisionBinding binding,
      ImmutableBiMap<TypeElement, String> moduleNames,
      ImmutableBiMap<Key, String> providerNames,
      ImmutableBiMap<Key, String> membersInjectorNames) {
    if (binding.bindingKind().equals(COMPONENT)) {
      return String.format("InstanceFactory.<%s>create(this)",
          writer.compressType(Util.typeToString(binding.providedKey().type())));
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
      return String.format(
          binding.scope().isPresent() ? "ScopedProvider.create(new %s(%s))" : "new %s(%s)",
          writer.compressType(factoryNameForProvisionBinding(binding).toString()),
          Joiner.on(", ").join(parameters));
    }
  }

  private static String initializeMembersInjectorForBinding(JavaWriter writer,
      MembersInjectionBinding binding,
      ImmutableBiMap<Key, String> providerNames,
      ImmutableBiMap<Key, String> membersInjectorNames) {
    List<String> parameters = getDependencyParameters(binding.dependencySet(),
        providerNames, membersInjectorNames);
    return String.format("new %s(%s)",
        writer.compressType(membersInjectorNameForMembersInjectionBinding(binding).toString()),
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

  private void writeInterfaceMethods(JavaWriter writer,
      ImmutableList<DependencyRequest> interfaceRequests,
      ImmutableBiMap<Key, String> providerNames,
      ImmutableBiMap<Key, String> membersInjectorNames) throws IOException {
    for (DependencyRequest interfaceRequest : interfaceRequests) {
      ExecutableElement requestElement = (ExecutableElement) interfaceRequest.requestElement();
      beginMethodOverride(writer, requestElement);
      if (interfaceRequest.kind().equals(MEMBERS_INJECTOR)) {
        String membersInjectorName = membersInjectorNames.get(interfaceRequest.key());
        Name parameterName =
            Iterables.getOnlyElement(requestElement.getParameters()).getSimpleName();
        writer.emitStatement("%s.injectMembers(%s)", membersInjectorName, parameterName);
        if (!requestElement.getReturnType().getKind().equals(VOID)) {
          writer.emitStatement("return %s", parameterName);
        }
      } else {
        // provision requests
        String providerName = providerNames.get(interfaceRequest.key());

        // look up the provider in the Key->name map and invoke.  Done.
        writer.emitStatement("return "
            + providerUsageStatement(providerName, interfaceRequest.kind()));
      }
      writer.endMethod();
    }
  }

  private JavaWriter beginMethodOverride(JavaWriter writer, ExecutableElement methodElement)
      throws IOException {
    String returnTypeString = writer.compressType(returnTypeString(methodElement.getReturnType()));
    String methodName = methodElement.getSimpleName().toString();
    Set<Modifier> modifiers = Sets.difference(methodElement.getModifiers(), EnumSet.of(ABSTRACT));
    ImmutableList.Builder<String> parametersBuilder = ImmutableList.builder();
    for (VariableElement parameterElement : methodElement.getParameters()) {
      parametersBuilder.add(writer.compressType(Util.typeToString(parameterElement.asType())),
          parameterElement.getSimpleName().toString());
    }
    ImmutableList.Builder<String> thrownTypesBuilder = ImmutableList.builder();
    for (TypeMirror thrownTypeMirror : methodElement.getThrownTypes()) {
      thrownTypesBuilder.add(writer.compressType(Util.typeToString(thrownTypeMirror)));
    }
    return writer.emitAnnotation(Override.class)
        .beginMethod(
            returnTypeString,
            methodName,
            modifiers,
            parametersBuilder.build(),
            thrownTypesBuilder.build());
  }

  private String returnTypeString(TypeMirror returnType) {
    TypeKind returnTypeKind = returnType.getKind();
    if (returnTypeKind.equals(VOID)) {
      return "void";
    } else if (returnTypeKind.isPrimitive()) {
      // TypeMirrors don't have names but PrimitiveType#toString() reflects the simple type name.
      return returnType.toString();
    }
    // Util.typeToString() does boxing, so cannot be used for primitive types.
    return Util.typeToString(returnType);
  }

  private String providerTypeString(Key key) {
    return Util.typeToString(types.getDeclaredType(
        elements.getTypeElement(Provider.class.getCanonicalName()), key.type()));
  }

  private String membersInjectorTypeString(Key key) {
    return Util.typeToString(types.getDeclaredType(
        elements.getTypeElement(MembersInjector.class.getCanonicalName()), key.type()));
  }
}
