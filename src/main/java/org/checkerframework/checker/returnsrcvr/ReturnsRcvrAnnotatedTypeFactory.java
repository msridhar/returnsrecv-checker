package org.checkerframework.checker.returnsrcvr;

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

import javax.lang.model.element.AnnotationMirror;

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
            return super.visitExecutable(t, p);
        }
    }
}
