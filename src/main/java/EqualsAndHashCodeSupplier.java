interface EqualsAndHashCodeSupplier<Filter> {

    boolean areEqual(Filter f1, Filter f2);

    int hashCode(Filter f1);

}
