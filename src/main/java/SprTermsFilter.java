

/**
 * User: Abhishek Sanoujam
 * Date: 6/18/13
 * Time: 1:56 AM
 * <p/>
 * A filter for a field matching a certain value
 */
public interface SprTermsFilter extends SprFilter {
    String getFieldName();
    Object[] getFieldValue();
}
