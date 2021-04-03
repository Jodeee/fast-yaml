package com.pine.fast.plugin.suggestion.clazz;

import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.misc.PsiCustomUtil;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;
import com.pine.fast.plugin.suggestion.completion.FileType;
import com.pine.fast.plugin.suggestion.completion.SuggestionDocumentationHelper;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Is a placeholder for classes that are known leafs with no additional navigation support For e.g, all numbers,
 * character & string classes from jdk
 */
public class DummyClassMetadata extends ClassMetadata {

    private SuggestionNodeType nodeType;

    DummyClassMetadata(@NotNull SuggestionNodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    protected void init(Module module) {
    }

    @Nullable
    @Override
    protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
        if (!isEmpty(pathSegment)) {
            switch (nodeType) {
                case BYTE:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Byte.parseByte(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case SHORT:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Short.parseShort(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case INT:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Integer.parseInt(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case LONG:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Long.parseLong(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case FLOAT:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Float.parseFloat(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case DOUBLE:
                    //noinspection EmptyCatchBlock
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Double.parseDouble(pathSegment);
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    } catch (NumberFormatException e) {
                    }
                    break;
                case CHAR:
                    if (pathSegment.length() == 1) {
                        return new DummyKeySuggestionDocumentationHelper(pathSegment);
                    }
                    break;
                case STRING:
                    return new DummyKeySuggestionDocumentationHelper(pathSegment);
            }
        }
        return null;
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix) {
        return null;
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix, @Nullable Set<String> siblingsToExclude) {
        return null;
    }

    @Nullable
    @Override
    protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
                                                               List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                               int pathSegmentStartIndex) {
        return null;
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
        return null;
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                       @Nullable Set<String> siblingsToExclude) {
        return null;
    }

    @Override
    protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
                                                                    List<SuggestionNode> matchesRootTillMe, String prefix,
                                                                    @Nullable Set<String> siblingsToExclude) {
        return null;
    }

    @Nullable
    @Override
    protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                                String originalValue) {
        return null;
    }

    @Override
    public boolean doCheckIsLeaf(Module module) {
        return true;
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType() {
        return nodeType;
    }

    @Nullable
    @Override
    public PsiType getPsiType(Module module) {
        switch (nodeType) {
            case BYTE:
                return PsiType.BYTE;
            case SHORT:
                return PsiType.SHORT;
            case INT:
                return PsiType.INT;
            case LONG:
                return PsiType.LONG;
            case FLOAT:
                return PsiType.FLOAT;
            case DOUBLE:
                return PsiType.DOUBLE;
            case CHAR:
                return PsiType.CHAR;
            case STRING:
                return PsiCustomUtil.safeGetValidType(module, JAVA_LANG_STRING);
            case UNKNOWN_CLASS:
            default:
                return null;
        }
    }


    private class DummyKeySuggestionDocumentationHelper implements SuggestionDocumentationHelper {

        private final String value;

        DummyKeySuggestionDocumentationHelper(String value) {
            this.value = value;
        }

        @Nullable
        @Override
        public String getOriginalName() {
            return value;
        }

        @NotNull
        @Override
        public Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                                List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
            Suggestion.SuggestionBuilder builder = Suggestion.builder()
                    .suggestionToDisplay(GenericUtil.dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors))
                    .numOfAncestors(numOfAncestors).matchesTopFirst(matchesRootTillMe)
                    .icon(nodeType.getIcon()).fileType(fileType);

            PsiType psiType = getPsiType(module);
            if (psiType != null) {
                builder.shortType(PsiCustomUtil.toClassNonQualifiedName(psiType));
            }
            return builder.build();
        }

        @Override
        public boolean supportsDocumentation() {
            return true;
        }

        @NotNull
        @Override
        public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
            /*
             * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
             * <p>Long description</p>
             */
            StringBuilder builder =
                    new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

            PsiType psiType = getPsiType(module);
            if (psiType != null) {
                String classFqn = PsiCustomUtil.toClassFqn(psiType);
                StringBuilder linkBuilder = new StringBuilder();
                createHyperlink(linkBuilder, classFqn, classFqn, false);
                builder.append(" (").append(linkBuilder.toString()).append(")");
            }

            return builder.toString();
        }

        @NotNull
        @Override
        public SuggestionNodeType getSuggestionNodeType(Module module) {
            return nodeType;
        }
    }

}