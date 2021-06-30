import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.transformations.simplification.*;

public class c1 {
    public static void main(String[] args) throws ParserException {
        System.out.println("String      Formula     CNF     NNF");
//        String[] string_l={"A&~A","(A&B)&((A&C))","(A&B)|(~(A&B))","(A|B)&(~(A|B))","(A&B)&(~(A&B))","(A|B)|(~(A|B))"};
        String[] string_l={"((E&W)|(E&X)|(E&Y)|(E&Z))","A&(A|B)&(C|D)","G&(A&X&Y|A&B&C|B&C&D|A&Z)","G&(A&(X&Y|B&C|Z)|B&C&D)","A|A&B|C"};
        for (String temp_string : string_l) {
            final FormulaFactory f = new FormulaFactory();
            final PropositionalParser p = new PropositionalParser(f);
            final Formula formula = p.parse(temp_string);
            String ff = formula.toString();
            final Formula nnf = formula.nnf();
            final Formula cnf = formula.cnf();
            String ff1 = nnf.toString();
            String ff2 = cnf.toString();
            System.out.print(temp_string + "      " + ff + "   ");
            System.out.print(ff1 + "   ");
            System.out.println(ff2);
            final BackboneSimplifier bbsimp = new BackboneSimplifier();
            final DistributiveSimplifier dissimp = new DistributiveSimplifier();
            final NegationSimplifier negsimp = new NegationSimplifier();
            final FactorOutSimplifier facsimp = new FactorOutSimplifier();

            final RatingFunction<Integer> rf = new DefaultRatingFunction();
            final AdvancedSimplifier advsimp = new AdvancedSimplifier(rf);

            final Formula transformed = bbsimp.apply(formula,true);
            final Formula transformed2 = negsimp.apply(formula,true);
            final Formula transformed3 = dissimp.apply(formula,true);
            final Formula transformed4 = facsimp.apply(formula,true);
            final Formula transformed5 = advsimp.apply(formula,true);
            System.out.println(transformed.toString());
            System.out.println(transformed2.toString());
            System.out.println(transformed3.toString());
            System.out.println(transformed4.toString());
            System.out.println(transformed5.toString());
        }
    }
}

//    Sir can we be given sample elasticsearch queries to analyse and check for possible optimizations
