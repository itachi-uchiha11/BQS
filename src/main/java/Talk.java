//public class Talk {
//    //Example 1
//    public static Boolean valueOf(boolean b) {
//        return b ? Boolean.TRUE : Boolean.FALSE;
//    }
//    //Example 2
//    public class NutritionFacts {
//        private final int servingSize;
//        private final int servings;
//        private final int calories;
//        private final int fat;
//        private final int sodium;
//        private final int carbohydrate;
//
//        public NutritionFacts(int servingSize, int servings) {
//            this(servingSize, servings, 0);
//        }
//
//        public NutritionFacts(int servingSize, int servings,
//                              int calories) {
//            this(servingSize, servings, calories, 0);
//        }
//
//        public NutritionFacts(int servingSize, int servings,
//                              int calories, int fat) {
//            this(servingSize, servings, calories, fat, 0);
//        }
//
//        public NutritionFacts(int servingSize, int servings,
//                              int calories, int fat, int sodium) {
//            this(servingSize, servings, calories, fat, sodium, 0);
//        }
//        public NutritionFacts(int servingSize, int servings,
//                              int calories, int fat, int sodium, int carbohydrate) {
//            this.servingSize = servingSize;
//            this.servings = servings;
//            this.calories = calories;
//            this.fat = fat;
//            this.sodium = sodium;
//            this.carbohydrate = carbohydrate;
//        }
//    }
//    //Called as : NutritionFacts cocaCola = new NutritionFacts(240, 8, 100, 0, 35, 27);
//
//    //Example 3
//    // JavaBeans Pattern - allows inconsistency, mandates mutability
//    public class NutritionFacts2 {
//        private int servingSize = -1;
//        private int servings = -1;
//        private int calories = 0;
//        private int fat = 0;
//        private int sodium = 0;
//        private int carbohydrate = 0;
//        public NutritionFacts2() {
//        }
//        public void setServingSize(int val) {
//            servingSize = val;
//        }
//        public void setServings(int val) {
//            servings = val;
//        }
//        public void setCalories(int val) {
//            calories = val;
//        }
//        public void setFat(int val) {
//            fat = val;
//        }
//        public void setSodium(int val) {
//            sodium = val;
//        }
//        public void setCarbohydrate(int val) {
//            carbohydrate = val;
//        }
//    }
//
////        Called As :
////        NutritionFacts cocaCola = new NutritionFacts();
////        cocaCola.setServingSize(240);
////        cocaCola.setServings(8);
////        cocaCola.setCalories(100);
////        cocaCola.setSodium(35);
////        cocaCola.setCarbohydrate(27);
//
//    //Example 4
//    public static class NutritionFacts3 {
//        private final int servingSize;
//        private final int servings;
//        private final int calories;
//        private final int fat;
//        private final int sodium;
//        private final int carbohydrate;
//        public static class Builder {
//            private final int servingSize;
//            private final int servings;
//            private int calories;
//            private int fat;
//            private int sodium;
//            private int carbohydrate  = 0;
//            public Builder(int servingSize, int servings) {
//                this.servingSize = servingSize;
//                this.servings    = servings;
//            }
//            public Builder calories(int val)
//            { calories = val;      return this; }
//            public Builder fat(int val)
//            { fat = val;           return this; }
//            public Builder sodium(int val)
//            { sodium = val;        return this; }
//            public Builder carbohydrate(int val)
//            { carbohydrate = val;  return this; }
//            public NutritionFacts3 build() {
//                return new NutritionFacts3(this);
//            } }
//        private NutritionFacts3(Builder builder) {
//            servingSize  = builder.servingSize;
//            //... similarly rest
//        }
//    }
////    Called As : NutritionFacts cocaCola = new NutritionFacts.Builder(240, 8).calories(100).sodium(35).carbohydrate(27).build();
//
//    //Example 5
//    // Singleton with public final field
//    public class Elvis {
//        public static final Elvis INSTANCE = new Elvis();
//        private Elvis() {}
//        public void leaveTheBuilding() {}
//    }
//
//    //Example 6
//    // Singleton with static factory
//    public class Elvis2 {
//        private static final Elvis INSTANCE = new Elvis2();
//        private Elvis2() {}
//        public static Elvis2 getInstance() { return INSTANCE; }
//        public void leaveTheBuilding() {}
//    }
//
//    //Example 7
//    public enum Elvis3 {
//        INSTANCE;
//        public void leaveTheBuilding() {}
//    }
//
//    //Example 8
//    // Non instantiable utility class
//    public class UtilityClass {
//        // Suppress default constructor for non instantiability
//        private UtilityClass() {
//            throw new AssertionError();
//        }
//    }
//
//    //Example 9
//    // Inappropriate use of static utility - inflexible & untestable!
//    public class SpellChecker {
//        private static final Lexicon dictionary = ...;
//        private SpellChecker() {} // Noninstantiable
//        public static boolean isValid(String word) { ... }
//        public static List<String> suggestions(String typo) { ... }
//    }
//
//    // Inappropriate use of singleton - inflexible & untestable!
//    public class SpellChecker {
//        private final Lexicon dictionary = ...;
//        private SpellChecker(...) {}
//        public static INSTANCE = new SpellChecker(...);
//        public boolean isValid(String word) { ... }
//        public List<String> suggestions(String typo) { ... }
//    }
//
//    // Dependency injection provides flexibility and testability
//    public class SpellChecker {
//        private final Lexicon dictionary;
//        public SpellChecker(Lexicon dictionary) { this.dictionary = Objects.requireNonNull(dictionary);
//        }
//        public boolean isValid(String word) { ... }
//        public List<String> suggestions(String typo) { ... }
//    }
//
//    //Example 10
//    // Performance can be greatly improved!
//    static boolean isRomanNumeral(String s) {
//        return s.matches("^(?=.)M*(C[MD]|D?C{0,3})"+ "(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$");
//    }
//
//
//    // Reusing expensive object for improved performance
//    public class RomanNumerals {
//        private static final Pattern ROMAN = Pattern.compile(
//                "^(?=.)M*(C[MD]|D?C{0,3})"
//                        + "(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$");
//        static boolean isRomanNumeral(String s) {
//            return ROMAN.matcher(s).matches();
//        }
//    }
//
//    // Hideously slow! Can you spot the object creation?
//    private static long sum() {
//        Long sum = 0L;
//        for (long i = 0; i <= Integer.MAX_VALUE; i++)
//            sum += i;
//        return sum; }
//
//    //Example 11
//    // Can you spot the "memory leak"?
//    public class Stack {
//        private Object[] elements;
//        private int size = 0;
//        private static final int DEFAULT_INITIAL_CAPACITY = 16;
//        public Stack() {
//            elements = new Object[DEFAULT_INITIAL_CAPACITY];
//        }
//        public void push(Object e) {
//            ensureCapacity();
//            elements[size++] = e;
//        }
//        public Object pop() {
//            if (size == 0)
//                throw new EmptyStackException();
//            return elements[--size];
//        }
//        /**
//         * Ensure space for at least one more element, roughly
//         * doubling the capacity each time the array needs to grow.
//         */
//        private void ensureCapacity() {
//            if (elements.length == size)
//                elements = Arrays.copyOf(elements, 2 * size + 1);
//        } }
//
//    //Example 12
//    // try-finally is ugly when used with more than one resource!
//    static void copy(String src, String dst) throws IOException {
//        InputStream in = new FileInputStream(src);
//        try {
//            OutputStream out = new FileOutputStream(dst);
//            try {
//                byte[] buf = new byte[BUFFER_SIZE];
//                int n;
//                while ((n = in.read(buf)) >= 0)
//                    out.write(buf, 0, n);
//            } finally {
//                out.close();
//            }
//        } finally {
//            in.close();
//        } }
//
//    //Example 13
//    // try-with-resources on multiple resources - short and sweet
//    static void copy(String src, String dst) throws IOException {
//        try (InputStream in = new FileInputStream(src);
//             OutputStream out = new FileOutputStream(dst)) {
//            byte[] buf = new byte[BUFFER_SIZE];
//            int n;
//            while ((n = in.read(buf)) >= 0)
//                out.write(buf, 0, n);
//        }
//    }
//
//}
