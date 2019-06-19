package org.checkerframework.checker.returnsrcvr;

import java.lang.annotation.Annotation;
import java.nio.file.Path;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.checker.returnsrcvr.qual.MaybeThis;
import org.checkerframework.checker.returnsrcvr.qual.This;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.*;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

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
        return new ListTypeAnnotator(
                super.createTypeAnnotator(), new ReturnsRcvrTypeAnnotator(this));
    }
    
    @Override
    public TreeAnnotator createTreeAnnotator() {
      return new ListTreeAnnotator(
          super.createTreeAnnotator(), new ReturnsRcvrTreeAnnotator(this));
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
            
            if(isAutoValueBuilderSetter(t.getElement())){
            	returnType.replaceAnnotation(THIS_ANNOT);
            	AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
                receiverType.replaceAnnotation(THIS_ANNOT);
            }
            
            
            
            return super.visitExecutable(t, p);
        }

        private boolean isAutoValueBuilderSetter(Element element) {
            MethodTree methodTree = (MethodTree) declarationFromElement(element);
            if (methodTree == null) {
              return false;
            }

            ClassTree enclosingClass = TreeUtils.enclosingClass(getPath(methodTree));

            if (enclosingClass == null) {
              return false;
            }
            
            boolean inAutoValueBuilder = hasAnnotation(enclosingClass, AutoValue.Builder.class);

            if (!inAutoValueBuilder) {
                // see if superclass is an AutoValue Builder, to handle generated code
                // NOTE: this doesn't work yet.  we are getting a null superclassTree unexpectedly.
                // need to figure out how to get annotations given a TypeMirror
                TypeElement typeElement = TreeUtils.elementFromDeclaration(enclosingClass);
                TypeMirror superclass = typeElement.getSuperclass();
                ClassTree superclassTree =
                    (ClassTree) declarationFromElement(TypesUtils.getTypeElement(superclass));
                inAutoValueBuilder = superclassTree != null && hasAnnotation(superclassTree, AutoValue.Builder.class);
            }

            if (inAutoValueBuilder) {
                Element classElem = TreeUtils.elementFromTree(enclosingClass);
                Tree returnType = methodTree.getReturnType();
                return returnType != null && classElem.equals(TreeUtils.elementFromTree(returnType));
            }
            
            return false;
          }
        
    }
    
    private class ReturnsRcvrTreeAnnotator extends TreeAnnotator {
    	
        public ReturnsRcvrTreeAnnotator(final AnnotatedTypeFactory atypeFactory) {
          super(atypeFactory);
        }
        
    }
    
    private static boolean hasAnnotation(
            ClassTree enclosingClass, Class<? extends Annotation> annotClass) {
        return enclosingClass.getModifiers().getAnnotations().stream()
                .map(TreeUtils::annotationFromAnnotationTree)
                .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
    }


}
