package org.checkerframework.checker.returnsrcvr;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.returnsrcvr.qual.MaybeThis;
import org.checkerframework.checker.returnsrcvr.qual.This;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class ReturnsRcvrAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  AnnotationMirror THIS_ANNOT;

  public ReturnsRcvrAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    THIS_ANNOT = AnnotationBuilder.fromClass(elements, This.class);
    // we have to call this explicitly
    this.postInit();
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(super.createTypeAnnotator(), new ReturnsRcvrTypeAnnotator(this));
  }

  private class ReturnsRcvrTypeAnnotator extends TypeAnnotator {

    public ReturnsRcvrTypeAnnotator(AnnotatedTypeFactory typeFactory) {
      super(typeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {

      AnnotatedTypeMirror returnType = t.getReturnType();
      AnnotationMirror maybeThisAnnot = AnnotationBuilder.fromClass(elements, MaybeThis.class);
      AnnotationMirror retAnnotation = returnType.getAnnotationInHierarchy(maybeThisAnnot);
      if (retAnnotation != null && AnnotationUtils.areSame(retAnnotation, THIS_ANNOT)) {
        // add @This to the receiver type
        AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
        receiverType.replaceAnnotation(THIS_ANNOT);
      }

      if (isBuilderSetter(t.getElement())) {
        returnType.replaceAnnotation(THIS_ANNOT);
        AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
        receiverType.replaceAnnotation(THIS_ANNOT);
      }

      return super.visitExecutable(t, p);
    }

    /**
     * Checks if the given element is a setter in either an AutoValue builder or a Lombok builder
     */
    private boolean isBuilderSetter(ExecutableElement element) {
      MethodTree methodTree = (MethodTree) declarationFromElement(element);
      if (methodTree == null) {
        return false;
      }

      ClassTree enclosingType = TreeUtils.enclosingClass(getPath(methodTree));

      if (enclosingType == null) {
        return false;
      }

      boolean inAutoValueBuilder = hasAnnotation(enclosingType, AutoValue.Builder.class);
      boolean inLombokBuilder =
          hasAnnotation(enclosingType, lombok.Generated.class)
              && enclosingType.getSimpleName().toString().endsWith("Builder");

      // if we are in an AutoValue Builder, this will be the element for the abstract Builder class
      Element builderClassElem = TreeUtils.elementFromTree(enclosingType);

      if (!inAutoValueBuilder && !inLombokBuilder) {
        // see if superclass is an AutoValue Builder, to handle generated code
        TypeElement typeElement = TreeUtils.elementFromDeclaration(enclosingType);
        TypeMirror superclass = typeElement.getSuperclass();
        // if enclosingType is an interface, the superclass has TypeKind NONE
        if (!superclass.getKind().equals(TypeKind.NONE)) {
          // update builderClassElem to be for the superclass for this case
          builderClassElem = TypesUtils.getTypeElement(superclass);
          inAutoValueBuilder = builderClassElem.getAnnotation(AutoValue.Builder.class) != null;
        }
      }

      if (inAutoValueBuilder || inLombokBuilder) {
        Tree returnType = methodTree.getReturnType();
        return returnType != null && builderClassElem.equals(TreeUtils.elementFromTree(returnType));
      }

      return false;
    }
  }

  private static boolean hasAnnotation(
      ClassTree enclosingClass, Class<? extends Annotation> annotClass) {
    return enclosingClass.getModifiers().getAnnotations().stream()
        .map(TreeUtils::annotationFromAnnotationTree)
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }
}
