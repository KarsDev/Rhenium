package me.kuwg.re.cli.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IncompatibleWith {
    String[] value();
}
