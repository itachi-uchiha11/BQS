import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.transformations.simplification.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class SimplifierComparator {
    static final double not_probability = 0.0;
    static List<Formula> generate_variables(int no_variables, int no_distinct_strings, FormulaFactory f){
        List<Formula> vars = new ArrayList<>(no_variables);
        for (int i = 0 ; i < no_variables; ++i){
            int rnd = new Random().nextInt(no_distinct_strings);
            if (Math.random()< not_probability){
                vars.add(f.literal("var"+i, false));
            }
            else {
                vars.add(f.variable("var"+i));
            }
        }
        return vars;
    }
    static Formula generate(int no_variables, int no_distinct_strings, FormulaFactory f){
        List<Formula> vars = generate_variables(no_variables, no_distinct_strings, f);
        List<Formula> and_vars = new ArrayList<>(no_variables/2);
        List<Formula> or_vars = new ArrayList<>(no_variables/2);
        for (int i = 0; i < no_variables/2; ++i){
            and_vars.add(vars.get(i));
            or_vars.add(vars.get(no_variables/2 + i));
        }
        Formula f1 = f.or(and_vars);
        Formula f2 = f.or(or_vars);
        return f.or(f1,f2);
    }
    public static void main(String[] args) throws ParserException {
//        List<Double>lst = new ArrayList<>();
//        List<Double>lst2 = new ArrayList<>();
//        final BackboneSimplifier bbsimp = new BackboneSimplifier();
//        FactorOutSimplifier factorOutSimplifier = new FactorOutSimplifier();
//        NegationSimplifier negationSimplifier = new NegationSimplifier();
//        final RatingFunction<Integer> rf = new DefaultRatingFunction();
//        final AdvancedSimplifier advsimp = new AdvancedSimplifier(rf);
//        for (int n=10;n<=400;n+=30) {
//            final FormulaFactory f = new FormulaFactory();
//            Formula formula = generate(n, n*5, f);
//            long start = System.nanoTime();
//            final Formula transformed = negationSimplifier.apply(factorOutSimplifier.apply(bbsimp.apply(formula, false),false),false);
//            long start2 = System.nanoTime();
//            final Formula transformed5 = advsimp.apply(formula, false);
//            long start3 = System.nanoTime();
//            System.out.print(n);
//            System.out.print(",");
//            lst.add((double)(start2-start)/1000000.0);
//            lst2.add((double)(start3-start2)/1000000.0);
//        }
//        System.out.println(" ");
//        for(Double x:lst){
//            System.out.print(x);
//            System.out.print(",");
//        }
//        System.out.println(" ");
//        for(Double x:lst2){
//            System.out.print(x);
//            System.out.print(",");
        String s = "(~A&(~B|~C|~D))";
        final FormulaFactory f=new FormulaFactory();
        final PropositionalParser p=new PropositionalParser(f);
        final Formula formula=p.parse(s);
        NegationSimplifier negationSimplifier = new NegationSimplifier();
        Formula ff = negationSimplifier.apply(formula,false);
        System.out.println(ff);


//
//        }
    }
}