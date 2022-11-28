package rig.ruuter.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
    String value() default "";

    String[] extraTags() default {};

    boolean longTask() default false;

    double[] percentiles() default {};

    boolean histogram() default false;

    String description() default "";
}
