package com.pine.fast.plugin.suggestion.metadata.json;

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static java.util.Comparator.comparing;
import static java.util.Objects.compare;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.module.Module;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.misc.Icons;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;
import com.pine.fast.plugin.suggestion.clazz.MetadataProxy;
import com.pine.fast.plugin.suggestion.clazz.MetadataProxyInvokerWithReturnValue;
import com.pine.fast.plugin.suggestion.completion.FileType;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-property-attributes
 */
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataProperty
        implements Comparable<SpringConfigurationMetadataProperty> {

    /**
     * The full name of the PROPERTY. Names are in lower-case period-separated form (for example, server.servlet.path).
     * This attribute is mandatory.
     */
    @Setter
    @Getter
    private String name;
    @Nullable
    @Setter
    @SerializedName("type")
    private String className;
    @Nullable
    @Setter
    private String description;
    /**
     * The class name of the source that contributed this PROPERTY. For example, if the PROPERTY were from a class
     * annotated with @ConfigurationProperties, this attribute would contain the fully qualified name of that class. If
     * the source type is unknown, it may be omitted.
     */
    @Nullable
    @Setter
    private String sourceType;
    /**
     * Specify whether the PROPERTY is deprecated. If the field is not deprecated or if that information is not known,
     * it may be omitted. The next table offers more detail about the springConfigurationMetadataDeprecation attribute.
     */
    @Nullable
    @Setter
    private SpringConfigurationMetadataDeprecation deprecation;
    /**
     * The default value, which is used if the PROPERTY is not specified. If the type of the PROPERTY is an ARRAY, it
     * can be an ARRAY of value(s). If the default value is unknown, it may be omitted.
     */
    @Nullable
    @Setter
    private Object defaultValue;

    /**
     * Represents either the only hint associated (or) key specific hint when the property represents a map
     */
    @Nullable
    @Expose(deserialize = false)
    private SpringConfigurationMetadataHint genericOrKeyHint;

    /**
     * If the property of type map, the property can have both keys & values. This hint represents value
     */
    @Nullable
    @Expose(deserialize = false)
    private SpringConfigurationMetadataHint valueHint;

    /**
     * Responsible for all suggestion queries that needs to be matched against a class
     */
    @Nullable
    private MetadataProxy delegate;

    @Nullable
    private SuggestionNodeType nodeType;
    private boolean delegateCreationAttempted;

    /**
     * 是否追加冒号
     */
    @Setter
    @Getter
    private Boolean isAppendColon;

    /**
     * 原名(选中后敲击回车的值)
     */
    @Setter
    @Getter
    private String originalName;

    @Nullable
    public List<SuggestionNode> findChildDeepestKeyMatch(Module module,
                                                         List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                         int pathSegmentStartIndex) {
        if (!isLeaf(module)) {
            if (isMapWithPredefinedKeys()) { // map
                assert genericOrKeyHint != null;
                String pathSegment = pathSegments[pathSegmentStartIndex];
                SpringConfigurationMetadataHintValue valueHint =
                        genericOrKeyHint.findHintValueWithName(pathSegment);
                if (valueHint != null) {
                    matchesRootTillParentNode.add(new HintAwareSuggestionNode(valueHint));
                    boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
                    if (lastPathSegment) {
                        return matchesRootTillParentNode;
                    } else {
                        if (!isMapWithPredefinedValues()) {
                            return doWithDelegateOrReturnNull(module, delegate -> delegate
                                    .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                                            pathSegmentStartIndex));
                        }
                    }
                }
            } else {
                return doWithDelegateOrReturnNull(module, delegate -> delegate
                        .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                                pathSegmentStartIndex));
            }
        }
        return null;
    }

    @Nullable
    public SortedSet<Suggestion> findChildKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillMe, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                       @Nullable Set<String> siblingsToExclude) {
        boolean lastPathSegment = querySegmentPrefixStartIndex == querySegmentPrefixes.length - 1;
        if (lastPathSegment && !isLeaf(module)) {
            if (isMapWithPredefinedKeys()) { // map
                assert genericOrKeyHint != null;
                String querySegment = querySegmentPrefixes[querySegmentPrefixStartIndex];
                Collection<SpringConfigurationMetadataHintValue> matches =
                        genericOrKeyHint.findHintValuesWithPrefix(querySegment);
                Stream<SpringConfigurationMetadataHintValue> matchesStream =
                        getMatchesAfterExcludingSiblings(genericOrKeyHint, matches, siblingsToExclude);

                return matchesStream.map(hintValue -> {
                    HintAwareSuggestionNode suggestionNode = new HintAwareSuggestionNode(hintValue);
                    return hintValue
                            .buildSuggestionForKey(fileType, matchesRootTillMe, numOfAncestors, suggestionNode);
                }).collect(toCollection(TreeSet::new));
            } else {
                return doWithDelegateOrReturnNull(module, delegate -> delegate
                        .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                                querySegmentPrefixes, querySegmentPrefixStartIndex, siblingsToExclude));
            }
        }
        return null;
    }

    @NotNull
    public Suggestion buildKeySuggestion(Module module, FileType fileType,
                                         List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
        Suggestion.SuggestionBuilder builder = Suggestion.builder().suggestionToDisplay(
                GenericUtil.dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors))
                .description(description)
                .shortType(GenericUtil.shortenedType(className))
                .defaultValue(getDefaultValueAsStr())
                .numOfAncestors(numOfAncestors)
                .matchesTopFirst(matchesRootTillMe)
//                .icon(getSuggestionNodeType(module).getIcon())
                .icon(Icons.DEFAULT_ICON)
                .isAppendColon(isAppendColon);
        if (deprecation != null) {
            builder.deprecationLevel(deprecation.getLevel() != null ?
                    deprecation.getLevel() :
                    SpringConfigurationMetadataDeprecationLevel.warning);
        }
        return builder.fileType(fileType).build();
    }

    @NotNull
    public Suggestion buildKeySuggestion2(Module module, FileType fileType,
                                          List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
        Suggestion.SuggestionBuilder builder = Suggestion.builder().suggestionToDisplay(
                GenericUtil.dotDelimitedNames(matchesRootTillMe, numOfAncestors))
                .description(description)
                .shortType(GenericUtil.shortenedType(className))
                .defaultValue(getDefaultValueAsStr())
                .numOfAncestors(numOfAncestors)
                .matchesTopFirst(matchesRootTillMe)
//                .icon(getSuggestionNodeType(module).getIcon())
                .icon(Icons.DEFAULT_ICON)
                .isAppendColon(isAppendColon);
        if (deprecation != null) {
            builder.deprecationLevel(deprecation.getLevel() != null ?
                    deprecation.getLevel() :
                    SpringConfigurationMetadataDeprecationLevel.warning);
        }
        return builder.fileType(fileType).build();
    }

    public boolean isLeaf(Module module) {
        return isLeafWithKnownValues() || getSuggestionNodeType(module).representsLeaf()
                || doWithDelegateOrReturnDefault(module, delegate -> delegate.isLeaf(module), true);
    }

    @NotNull
    public SuggestionNodeType getSuggestionNodeType(Module module) {
        if (nodeType == null) {
            if (className != null) {
                refreshDelegate(module);

                if (delegate != null) {
                    nodeType = delegate.getSuggestionNodeType(module);
                }

                if (nodeType == null) {
                    nodeType = SuggestionNodeType.UNKNOWN_CLASS;
                }
            } else {
                nodeType = SuggestionNodeType.UNDEFINED;
            }
        }

        return nodeType;
    }

    public void refreshDelegate(Module module) {
        if (className != null) {
            // Lets update the delegate information only if anything has changed from last time we saw this
            // In the previous refresh, class could not be found. Now class is available in the classpath
        }
        delegateCreationAttempted = true;
    }

    @Override
    public int compareTo(@NotNull SpringConfigurationMetadataProperty o) {
        return compare(this, o, comparing(thiz -> thiz.name));
    }

    /**
     * @return true if the property is deprecated & level is error, false otherwise
     */
    public boolean isDeprecatedError() {
        return deprecation != null
                && deprecation.getLevel() == SpringConfigurationMetadataDeprecationLevel.error;
    }

    public SortedSet<Suggestion> findSuggestionsForValues(Module module, FileType fileType,
                                                          List<SuggestionNode> matchesRootTillContainerProperty, String prefix,
                                                          @Nullable Set<String> siblingsToExclude) {
        assert isLeaf(module);
        if (nodeType == SuggestionNodeType.VALUES) {
            Collection<SpringConfigurationMetadataHintValue> matches =
                    requireNonNull(genericOrKeyHint).findHintValuesWithPrefix(prefix);
            if (!isEmpty(matches)) {
                Stream<SpringConfigurationMetadataHintValue> matchesStream =
                        getMatchesAfterExcludingSiblings(genericOrKeyHint, matches, siblingsToExclude);

                return matchesStream.map(match -> match
                        .buildSuggestionForValue(fileType, matchesRootTillContainerProperty,
                                getDefaultValueAsStr())).collect(toCollection(TreeSet::new));
            }
        } else {
            return doWithDelegateOrReturnNull(module, delegate -> delegate
                    .findValueSuggestionsForPrefix(module, fileType, matchesRootTillContainerProperty, prefix,
                            siblingsToExclude));
        }

        return null;
    }

    public void setGenericOrKeyHint(SpringConfigurationMetadataHint genericOrKeyHint) {
        this.genericOrKeyHint = genericOrKeyHint;
        updateNodeType();
    }

    public void setValueHint(SpringConfigurationMetadataHint valueHint) {
        this.valueHint = valueHint;
        updateNodeType();
    }

    private Stream<SpringConfigurationMetadataHintValue> getMatchesAfterExcludingSiblings(
            @NotNull SpringConfigurationMetadataHint hintFindValueAgainst,
            Collection<SpringConfigurationMetadataHintValue> matches,
            @Nullable Set<String> siblingsToExclude) {
        Stream<SpringConfigurationMetadataHintValue> matchesStream;
        if (siblingsToExclude != null) {
            Set<SpringConfigurationMetadataHintValue> exclusionMembers =
                    siblingsToExclude.stream().map(hintFindValueAgainst::findHintValueWithName)
                            .collect(toSet());
            matchesStream = matches.stream().filter(value -> !exclusionMembers.contains(value));
        } else {
            matchesStream = matches.stream();
        }
        return matchesStream;
    }

    private void updateNodeType() {
        if (isMapWithPredefinedKeys() || isMapWithPredefinedValues()) {
            nodeType = SuggestionNodeType.MAP;
        } else if (isLeafWithKnownValues()) {
            nodeType = SuggestionNodeType.VALUES;
        }
    }

    private boolean isMapWithPredefinedValues() {
        return valueHint != null && valueHint.representsValueOfMap();
    }

    private boolean isMapWithPredefinedKeys() {
        return genericOrKeyHint != null && genericOrKeyHint.representsKeyOfMap();
    }

    private boolean isLeafWithKnownValues() {
        return !isMapWithPredefinedKeys() && !isMapWithPredefinedValues() && genericOrKeyHint != null
                && genericOrKeyHint.hasPredefinedValues();
    }

    @Contract("_, _, !null -> !null; _, _, null -> null")
    private <T> T doWithDelegateOrReturnDefault(Module module,
                                                MetadataProxyInvokerWithReturnValue<T> invoker, T defaultValue) {
        MetadataProxy delegate = getDelegate(module);
        if (delegate != null) {
            return invoker.invoke(delegate);
        }
        return defaultValue;
    }

    @Nullable
    private <T> T doWithDelegateOrReturnNull(Module module,
                                             MetadataProxyInvokerWithReturnValue<T> invoker) {
        return doWithDelegateOrReturnDefault(module, invoker, null);
    }

    private String getDefaultValueAsStr() {
        if (defaultValue != null && !(defaultValue instanceof Array)
                && !(defaultValue instanceof Collection)) {
            if (className != null && defaultValue instanceof Double) {
                // if defaultValue is a number, its being parsed by gson as double & we will see an incorrect fraction when we take toString()
                switch (className) {
                    case "java.lang.Integer":
                        return Integer.toString(((Double) defaultValue).intValue());
                    case "java.lang.Byte":
                        return Byte.toString(((Double) defaultValue).byteValue());
                    case "java.lang.Short":
                        return Short.toString(((Double) defaultValue).shortValue());
                }
            }
            return defaultValue.toString();
        }
        return null;
    }

    @Nullable
    private MetadataProxy getDelegate(Module module) {
        if (!delegateCreationAttempted) {
            refreshDelegate(module);
        }
        return delegate;
    }


    class HintAwareSuggestionNode implements SuggestionNode {

        private final SpringConfigurationMetadataHintValue target;

        /**
         * @param target hint value
         */
        HintAwareSuggestionNode(SpringConfigurationMetadataHintValue target) {
            this.target = target;
        }

        @Nullable
        @Override
        public List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                              List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                              int pathSegmentStartIndex) {
            throw new IllegalAccessError("Should never be called");
        }

        @Nullable
        @Override
        public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                      int querySegmentPrefixStartIndex) {
            return null;
        }

        @Override
        public SortedSet<Suggestion> findKeySuggestionsForContains(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String querySegmentPrefixes) {

            return null;
        }


        @Nullable
        @Override
        public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                      int querySegmentPrefixStartIndex, @Nullable Set<String> siblingsToExclude) {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalName() {
            return target.toString();
        }


        @Nullable
        @Override
        public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                                   List<SuggestionNode> matchesRootTillMe, String prefix,
                                                                   @Nullable Set<String> siblingsToExclude) {
            if (isMapWithPredefinedValues()) {
                assert valueHint != null;
                Collection<SpringConfigurationMetadataHintValue> matches =
                        valueHint.findHintValuesWithPrefix(prefix);
                if (!isEmpty(matches)) {
                    Stream<SpringConfigurationMetadataHintValue> matchesStream =
                            getMatchesAfterExcludingSiblings(valueHint, matches, siblingsToExclude);
                    return matchesStream.map(match -> match
                            .buildSuggestionForValue(fileType, matchesRootTillMe, getDefaultValueAsStr())).collect(toCollection(TreeSet::new));
                }
            } else {
                return doWithDelegateOrReturnNull(module, delegate -> delegate
                        .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix));
            }
            return null;
        }

        @Override
        public boolean isLeaf(Module module) {
            if (isLeafWithKnownValues() || isMapWithPredefinedValues()) {
                return true;
            }
            // whether the node is a leaf or not depends on the value of the map that containing property points to
            return false;
        }

        @Override
        public boolean isMetadataNonProperty() {
            return false;
        }

    }

}
