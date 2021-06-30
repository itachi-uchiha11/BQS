//package com.spr.shifu.query.request;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.spr.annotations.SprSafeHtml;
//import com.spr.enums.EnumLabel;
//import com.spr.utils.SprStringUtils;
//import com.spr.utils.SprinklrCollectionUtils;
//import com.spr.utils.StaticChecker;
//import com.spr.utils.ds.tree.TreeLikeStructure;
//import io.leangen.graphql.annotations.types.GraphQLType;
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import javax.validation.Valid;
//import org.apache.commons.collections.CollectionUtils;
//import org.apache.commons.collections.MapUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.codehaus.jackson.annotate.JsonIgnoreProperties;
//@JsonIgnoreProperties(ignoreUnknown = true)
//@GraphQLType(name = "GraphqlShifuFilter")
//public class Filter implements TreeLikeStructure<Filter>, Serializable {
//    static {
//        StaticChecker.checkInstanceMembersExistsForAllPublicStaticFinalStringFields(Filter.class);
//    }
//    public static final String FIELD_FIELD = "field";
//    public static final String FIELD_VALUES = "values";
//    @GraphQLType(name = "shifuFilterType")
//    public enum FilterType implements EnumLabel {
//        AND("And", true, false),
//        OR("Or", true, false),
//        NOT("Not", true, true),
//        IN("Containing"),
//        GT("Greater than"),
//        GTE("Greater than or Equal to"),
//        LT("Less than"),
//        LTE("Less than or Equal to"),
//        NIN("Not Containing", false, true),
//        BETWEEN("Between"),
//        STARTS_WITH("Starts With"),
//        ENDS_WITH("Ends With"),
//        TEXT_CONTAINS("Text Contains"),
//        CONTAINS("Contains"),
//        DOES_NOT_CONTAIN("Does not contain", false, true),
//        EQUALS("Equals"),
//        FILTER,
//        EXISTS("Exists"),
//        REGEX("Regex"),
//        LIMIT,
//        SEARCH,
//        MISSING("Missing", false, true),
//        ADHOC_SEARCH,
//        EXPRESSION,
//        GEO_DISTANCE,
//        NOT_EQUALS("Not Equals", false, true),
//        ADVANCED_QUERY,
//        NESTED("Nested", true, false),
//        ADVANCED_FILTER("Advanced Filter", true, false);
//        private String displayName;
//        private boolean isCompoundFilter;
//        private boolean negativeFilter;
//        FilterType() {
//        }
//        FilterType(String displayName) {
//            this(displayName, false, false);
//        }
//        FilterType(String displayName, boolean isCompoundFilter, boolean negativeFilter) {
//            this.displayName = displayName;
//            this.isCompoundFilter = isCompoundFilter;
//            this.negativeFilter = negativeFilter;
//        }
//        public String getDisplayName() {
//            return displayName;
//        }
//        public boolean isCompoundFilter() {
//            return isCompoundFilter;
//        }
//        public boolean isNegativeFilter() {
//            return negativeFilter;
//        }
//        @Override
//        public String getLabel() {
//            String displayName = getDisplayName();
//            if (StringUtils.isBlank(displayName)) {
//                displayName = SprStringUtils.capitalize(name(), '_');
//            }
//            return displayName;
//        }
//    }
//    public static final SprinklrCollectionUtils.Transformer<Filter, String> FIELD_TRANSFOMER = Filter::getField;
//    private FilterType filterType;
//    @SprSafeHtml
//    private String field;
//    // Actual values applied as filter.
//    @SprSafeHtml
//    private List<Object> values;
//    // Used in case of contextual filters so that user can select available values from this list. Resolved runtime from user. Main purpose is to show on ui.
//    // Suppose a particular custom property has 4 values A, B, C and D. User can toggle between the available options.
//    @SprSafeHtml
//    private List<Object> accessibleValues;
//    // is User filter
//    private boolean userFilter;
//    // this determines if a user can access all values or just his tagged values.
//    private boolean allValuesAllowed = true;
//    private boolean lockedWithValues;
//    private boolean hidden;
//    private boolean favourite;
//    private boolean mandatory;
//    private boolean locked;
//    @SprSafeHtml
//    private Map<String, Object> details = Maps.newHashMap();
//    @Valid
//    private List<Filter> filters;
//    private List<AdvancedFilterRequest> advancedFilters;
//    public Filter() {
//    }
//    public Filter(Filter filter) {
//        replaceWith(filter);
//    }
//    @Override
//    public String toString() {
//        return "Filter{" +
//                "filterType=" + filterType +
//                ", field='" + field + '\'' +
//                ", values=" + values +
//                ", accessibleValues=" + accessibleValues +
//                ", userFilter=" + userFilter +
//                ", allValuesAllowed=" + allValuesAllowed +
//                ", lockedWithValues=" + lockedWithValues +
//                ", hidden=" + hidden +
//                ", favourite=" + favourite +
//                ", mandatory=" + mandatory +
//                ", locked=" + locked +
//                ", details=" + details +
//                ", filters=" + filters +
//                ", advancedFilters=" + advancedFilters +
//                '}';
//    }
//    public void replaceWith(Filter filter) {
//        this.filterType = filter.filterType;
//        this.field = filter.field;
//        this.values = filter.values;
//        this.details = filter.details;
//        this.filters = filter.filters;
//        this.userFilter = filter.userFilter;
//        this.accessibleValues = filter.accessibleValues;
//        this.allValuesAllowed = filter.allValuesAllowed;
//        this.favourite = filter.favourite;
//        this.mandatory = filter.mandatory;
//        this.locked = filter.locked;
//        this.advancedFilters = filter.advancedFilters;
//        this.lockedWithValues = filter.lockedWithValues;
//        this.hidden = filter.hidden;
//    }
//    public Filter(FilterType filterType, String field, List<Object> values) {
//        this.filterType = filterType;
//        this.field = field;
//        this.values = values;
//    }
//    public Filter(FilterType filterType, String field) {
//        this.filterType = filterType;
//        this.field = field;
//    }
//    public FilterType getFilterType() {
//        return filterType;
//    }
//    @SuppressWarnings("UnusedDeclaration")
//    public void setFilterType(FilterType filterType) {
//        this.filterType = filterType;
//    }
//    public String getField() {
//        return field;
//    }
//    @SuppressWarnings("UnusedDeclaration")
//    public void setField(String field) {
//        this.field = field;
//    }
//    public List<Object> getValues() {
//        return values;
//    }
//    @SuppressWarnings("UnusedDeclaration")
//    public void setValues(List<Object> values) {
//        this.values = values;
//    }
//    public List<Object> getAccessibleValues() {
//        return accessibleValues;
//    }
//    public void setAccessibleValues(List<Object> accessibleValues) {
//        this.accessibleValues = accessibleValues;
//    }
//    public boolean isUserFilter() {
//        return userFilter;
//    }
//    public void setUserFilter(boolean userFilter) {
//        this.userFilter = userFilter;
//    }
//    public boolean isAllValuesAllowed() {
//        return allValuesAllowed;
//    }
//    public void setAllValuesAllowed(boolean allValuesAllowed) {
//        this.allValuesAllowed = allValuesAllowed;
//    }
//    public boolean isFavourite() {
//        return favourite;
//    }
//    public void setFavourite(boolean favourite) {
//        this.favourite = favourite;
//    }
//    public boolean isLocked() {
//        return locked;
//    }
//    public void setLocked(boolean locked) {
//        this.locked = locked;
//    }
//    public Map<String, Object> getDetails() {
//        return details;
//    }
//    public void setDetails(Map<String, Object> details) {
//        this.details = details;
//    }
//    public Object getDetail(String key) {
//        if (this.details == null) {
//            return null;
//        }
//        return this.details.get(key);
//    }
//    public List<Filter> getFilters() {
//        return filters;
//    }
//    public void setFilters(List<Filter> filters) {
//        this.filters = filters;
//    }
//    public boolean isMandatory() {
//        return mandatory;
//    }
//    public void setMandatory(boolean mandatory) {
//        this.mandatory = mandatory;
//    }
//    public Filter filters(List<Filter> filters) {
//        this.filters = filters;
//        return this;
//    }
//    public Filter addFilter(Filter filter) {
//        if (CollectionUtils.isEmpty(this.filters)) {
//            this.filters = Lists.newArrayList();
//        }
//        this.filters.add(filter);
//        return this;
//    }
//    public Filter addFilters(List<Filter> filters) {
//        if (CollectionUtils.isEmpty(filters)) {
//            return this;
//        }
//        if (CollectionUtils.isEmpty(this.filters)) {
//            this.filters = Lists.newArrayList();
//        }
//        this.filters.addAll(filters);
//        return this;
//    }
//    public Filter addDetail(String key, Object value) {
//        if (details == null) {
//            details = Maps.newHashMap();
//        }
//        details.put(key, value);
//        return this;
//    }
//    @SuppressWarnings("unused")
//    public Filter removeDetail(String key) {
//        if (details != null) {
//            details.remove(key);
//        }
//        return this;
//    }
//    @SuppressWarnings("Duplicates")
//    public Filter details(Map<String, Object> detailsToAppend) {
//        if (MapUtils.isNotEmpty(detailsToAppend)) {
//            if (this.details == null) {
//                this.details = Maps.newHashMap();
//            }
//            this.details.putAll(detailsToAppend);
//        }
//        return this;
//    }
//    public Filter field(String field) {
//        this.field = field;
//        return this;
//    }
//    public Filter filterType(FilterType filterType) {
//        this.filterType = filterType;
//        return this;
//    }
//    public Filter addValue(Object value) {
//        if (values == null) {
//            values = Lists.newArrayList();
//        }
//        values.add(value);
//        return this;
//    }
//    public Filter values(List<?> values) {
//        this.values = (List<Object>) values;
//        return this;
//    }
//    public Filter values(Collection<?> values) {
//        this.values = SprinklrCollectionUtils.toList((Collection<Object>) values);
//        return this;
//    }
//    public Filter mandatory(boolean mandatory) {
//        this.mandatory = mandatory;
//        return this;
//    }
//    public Filter addAdvancedFilter(AdvancedFilterRequest advancedFilter) {
//        if (advancedFilter == null) {
//            return this;
//        }
//        if (this.advancedFilters == null) {
//            this.advancedFilters = new ArrayList<>();
//        }
//        this.advancedFilters.add(advancedFilter);
//        return this;
//    }
//    public List<AdvancedFilterRequest> getAdvancedFilters() {
//        return advancedFilters;
//    }
//    public void setAdvancedFilters(List<AdvancedFilterRequest> advancedFilters) {
//        this.advancedFilters = advancedFilters;
//    }
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) {
//            return true;
//        }
//        if (o == null || getClass() != o.getClass()) {
//            return false;
//        }
//        final Filter filter = (Filter) o;
//        return userFilter == filter.userFilter && allValuesAllowed == filter.allValuesAllowed && favourite == filter.favourite &&
//                mandatory == filter.mandatory && filterType == filter.filterType && Objects.equals(field, filter.field) &&
//                Objects.equals(values, filter.values) && Objects.equals(accessibleValues, filter.accessibleValues) &&
//                Objects.equals(details, filter.details) && Objects.equals(filters, filter.filters) && locked == filter.locked && Objects
//                .equals(advancedFilters, filter.advancedFilters) && hidden == filter.hidden && lockedWithValues == filter.lockedWithValues;
//    }
//    @Override
//    public int hashCode() {
//        return Objects
//                .hash(filterType, field, values, accessibleValues, userFilter, allValuesAllowed, favourite, mandatory, details, filters, locked,
//                        advancedFilters);
//    }
//    @SuppressWarnings({"CloneDoesntCallSuperClone", "Duplicates"})
//    @Override
//    public Filter clone() {
//        final Filter filter = new Filter(this.filterType, this.field);
//        if (MapUtils.isNotEmpty(this.details)) {
//            filter.setDetails(Maps.newHashMap(this.details));
//        }
//        if (CollectionUtils.isNotEmpty(this.values)) {
//            filter.setValues(Lists.newArrayList(this.values));
//        }
//        if (CollectionUtils.isNotEmpty(this.filters)) {
//            filter.filters = new ArrayList<>();
//            this.filters.stream().map(Filter::clone).forEach(clonedFilter -> filter.filters.add(clonedFilter));
//        }
//        filter.userFilter = this.userFilter;
//        filter.accessibleValues = this.accessibleValues;
//        filter.allValuesAllowed = this.allValuesAllowed;
//        filter.locked = this.locked;
//        filter.favourite = this.favourite;
//        filter.mandatory = this.mandatory;
//        filter.lockedWithValues = this.lockedWithValues;
//        filter.hidden = this.hidden;
//        if (SprinklrCollectionUtils.isNotEmpty(advancedFilters)) {
//            for (AdvancedFilterRequest advancedFilter : advancedFilters) {
//                filter.addAdvancedFilter(advancedFilter.clone());
//            }
//        }
//        return filter;
//    }
//    @Override
//    public Collection<Filter> children(Filter data) {
//        return data.getFilters();
//    }
//    @Override
//    public Filter root() {
//        return this;
//    }
//    public boolean isLockedWithValues() {
//        return lockedWithValues;
//    }
//    public void setLockedWithValues(boolean lockedWithValues) {
//        this.lockedWithValues = lockedWithValues;
//    }
//    public boolean isHidden() {
//        return hidden;
//    }
//    public void setHidden(boolean hidden) {
//        this.hidden = hidden;
//    }
//}
