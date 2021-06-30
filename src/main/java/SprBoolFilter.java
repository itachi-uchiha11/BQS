
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Abhishek Sanoujam
 * Date: 6/18/13
 * Time: 3:24 PM
 */
public interface SprBoolFilter extends SprFilter {

    List<SprFilter> getMustFilters();

    List<SprFilter> getShouldFilters();

    List<SprFilter> getMustNotFilters();

    boolean cache();

    SprBoolFilter must(SprFilter sprFilter);

    SprBoolFilter should(SprFilter sprFilter);

    SprBoolFilter mustNot(SprFilter sprFilter);
}
