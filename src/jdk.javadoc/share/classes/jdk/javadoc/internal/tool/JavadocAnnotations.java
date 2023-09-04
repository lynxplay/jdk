package jdk.javadoc.internal.tool;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotations;
import com.sun.tools.javac.code.TypeMetadata;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

public class JavadocAnnotations extends TypeAnnotations {

    public static void preRegister(final Context context) {
        context.put(typeAnnosKey, (Context.Factory<TypeAnnotations>) JavadocAnnotations::new);
    }

    protected JavadocAnnotations(final Context context) {
        super(context);
    }

    @Override
    protected TypeAnnotationPositions newPositionsScanner(final boolean signOnly) {
        return new JavadocTypeAnnotationPositions(signOnly);
    }

    private class JavadocTypeAnnotationPositions extends TypeAnnotationPositions {

        protected JavadocTypeAnnotationPositions(final boolean sigOnly) {
            super(sigOnly);
        }

        // We aim to filter out potential duplicate annotations from the declarative
        // annotation set of the symbol after TypeAnnotationPositions separated the annotations into their final
        // places.
        //
        // As TYPE_USE is the generally newer system and is additionally inlined in the javadoc, it is favoured
        // in case an annotation is emitted twice for a single symbol, once as a type and once as a declarative
        // annotation.
        @Override
        protected void separateAnnotationsKinds(final JCTree pos,
                                                final JCTree typetree,
                                                final Type type,
                                                final Symbol sym,
                                                final TypeAnnotationPosition typeAnnotationPosition) {
            super.separateAnnotationsKinds(pos, typetree, type, sym, typeAnnotationPosition);

            // Collect the existing type attributes as defined on the typetree passed to the method.
            final List<Attribute.TypeCompound> typeAttributes = typetree.type != null ? typetree.type.getMetadata(
                    TypeMetadata.Annotations.class, a -> a.annotationBuffer().toList(), List.nil()
            ) : List.nil();

            List<Attribute.Compound> filteredDeclarationAttributues = List.nil();

            // Filter the declaration attributes, only adding those to the "remaining" list that do not have
            // a duplicate type in the fetched type attributes found in the tree type.
            outer:
            for (final Attribute.Compound declarationAttribute : sym.getDeclarationAttributes()) {
                for (final Attribute.TypeCompound typeAttribute : typeAttributes) {
                    if (typeAttribute.type.equals(declarationAttribute.type)) continue outer;
                }

                filteredDeclarationAttributues = filteredDeclarationAttributues.append(declarationAttribute);
            }

            // Write the now filter list of declaration annotations.
            sym.resetAnnotations();
            sym.setDeclarationAttributes(filteredDeclarationAttributues);
        }
    }
}
