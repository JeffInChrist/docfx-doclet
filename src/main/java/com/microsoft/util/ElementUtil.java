package com.microsoft.util;

import com.microsoft.model.ExceptionItem;
import com.microsoft.model.MethodParameter;
import com.microsoft.model.Return;
import com.microsoft.model.TypeParameter;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree.Kind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import jdk.javadoc.doclet.DocletEnvironment;
import org.apache.commons.lang3.StringUtils;

public class ElementUtil {

    private static Map<ElementKind, String> elementKindLookup = new HashMap<>() {{
        put(ElementKind.PACKAGE, "Namespace");
        put(ElementKind.CLASS, "Class");
        put(ElementKind.ENUM, "Enum");
        put(ElementKind.ENUM_CONSTANT, "Enum constant");
        put(ElementKind.INTERFACE, "Interface");
        put(ElementKind.ANNOTATION_TYPE, "Interface");
        put(ElementKind.CONSTRUCTOR, "Constructor");
        put(ElementKind.METHOD, "Method");
        put(ElementKind.FIELD, "Field");
    }};
    private static Map<String, String> typeParamsLookup = new HashMap<>();
    private static Random random = new Random(21);
    private static DocletEnvironment environment;

    public ElementUtil(DocletEnvironment environment) {
        this.environment = environment;
    }

    public static String extractType(Element element) {
        return elementKindLookup.get(element.getKind());
    }

    public static List<TypeElement> extractSortedElements(Element element) {
        // Need to apply sorting, because order of result items for Element.getEnclosedElements() depend on JDK implementation
        List<TypeElement> elements = ElementFilter.typesIn(element.getEnclosedElements());
        elements.sort((o1, o2) ->
            StringUtils.compare(String.valueOf(o1.getSimpleName()), String.valueOf(o2.getSimpleName()))
        );
        return elements;
    }

    public static List<PackageElement> extractPackageElements(Set<? extends Element> elements) {
        return ElementFilter.packagesIn(elements).stream().sorted((o1, o2) ->
            StringUtils.compare(String.valueOf(o1.getSimpleName()), String.valueOf(o2.getSimpleName()))
        ).collect(Collectors.toList());
    }

    public static List<TypeParameter> extractTypeParameters(TypeElement element) {
        List<TypeParameter> result = new ArrayList<>();
        for (TypeParameterElement typeParameter : element.getTypeParameters()) {
            String key = String.valueOf(typeParameter);
            if (!typeParamsLookup.containsKey(key)) {
                typeParamsLookup.put(key, generateRandomHexString());
            }
            String value = typeParamsLookup.get(key);
            result.add(new TypeParameter(key, value));
        }
        return result;
    }

    private static String generateRandomHexString() {
        return Integer.toHexString(random.nextInt());
    }

    public static String convertFullNameToOverload(String fullName) {
        return fullName.replaceAll("\\(.*", "*");
    }

    public static String extractSuperclass(TypeElement classElement) {
        TypeMirror superclass = classElement.getSuperclass();
        if (superclass.getKind() == TypeKind.NONE) {
            return "java.lang.Object";
        }
        return String.valueOf(superclass);
    }

    public static List<ExceptionItem> extractExceptions(ExecutableElement methodElement) {
        return methodElement.getThrownTypes().stream()
            .map(o -> new ExceptionItem(String.valueOf(o), "-=TBD=-"))  // TODO: Determine exception description
            .collect(Collectors.toList());
    }

    public static List<MethodParameter> extractParameters(ExecutableElement element) {
        return element.getParameters().stream().map(o -> new MethodParameter(
            String.valueOf(o.getSimpleName()),
            String.valueOf(o.asType()),
            "-=TBD=-"                                                   // TODO: Determine parameter description
        )).collect(Collectors.toList());
    }

    public static String extractPackageContent(PackageElement packageElement) {
        return "package " + packageElement.getQualifiedName();
    }

    public static String extractClassContent(TypeElement classElement, String shortNameWithGenericsSupport) {
        String type = elementKindLookup.get(classElement.getKind());
        return String.format("%s %s %s",
            classElement.getModifiers().stream().map(String::valueOf)
                .filter(modifier -> !("Interface".equals(type) && "abstract".equals(modifier)))
                .filter(modifier -> !("Enum".equals(type) && ("static".equals(modifier) || "final".equals(modifier))))
                .collect(Collectors.joining(" ")),
            StringUtils.lowerCase(type), shortNameWithGenericsSupport);
    }

    public static Return extractReturn(ExecutableElement methodElement) {
        return new Return(String.valueOf(methodElement.getReturnType()),
            extractReturnDescription(methodElement));
    }

    public static Return extractReturn(VariableElement fieldElement) {
        return new Return(String.valueOf(fieldElement.asType()));
    }

    public static String extractComment(Element element) {
        return getDocCommentTree(element).map(docTree -> String.valueOf(docTree.getFullBody())).orElse(null);
    }

    public static String extractPackageName(Element element) {
        return String.valueOf(environment.getElementUtils().getPackageOf(element));
    }

    public static Optional<DocCommentTree> getDocCommentTree(Element element) {
        return Optional.ofNullable(environment.getDocTrees().getDocCommentTree(element));
    }

    private static String extractReturnDescription(ExecutableElement methodElement) {
        return getDocCommentTree(methodElement).map(docTree -> docTree.getBlockTags().stream()
            .filter(o -> o.getKind() == Kind.RETURN)
            .map(String::valueOf)
            .map(o -> StringUtils.remove(o, "@return"))
            .map(StringUtils::trim)
            .findFirst().orElse(null)
        ).orElse(null);
    }
}
