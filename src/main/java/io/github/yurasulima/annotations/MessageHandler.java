package io.github.yurasulima.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageHandler {
    String[] commands() default {};
    String prefix() default "/";
    String[] textEquals() default {};
    String[] textStartsWith() default {};
    String[] textMatches() default {};
    boolean hasPhoto() default false;
    boolean hasDocument() default false;
}
