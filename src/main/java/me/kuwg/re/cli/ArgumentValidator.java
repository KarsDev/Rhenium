package me.kuwg.re.cli;

import me.kuwg.re.cli.annotations.IncompatibleWith;
import me.kuwg.re.error.thrower.RThrower;

import java.lang.reflect.Field;

public final class ArgumentValidator {
    public static void validate(Arguments args) {
        Class<?> cls = args.getClass();

        for (Field field : cls.getDeclaredFields()) {
            IncompatibleWith incompatible = field.getAnnotation(IncompatibleWith.class);
            if (incompatible == null) continue;

            field.setAccessible(true);

            try {
                if (!(field.get(args) instanceof Boolean enabled) || !enabled) {
                    continue;
                }

                for (String otherName : incompatible.value()) {
                    Field other = cls.getDeclaredField(otherName);
                    other.setAccessible(true);

                    if (Boolean.TRUE.equals(other.get(args))) {
                        RThrower.throwError(
                                "Arguments -" + field.getName() +
                                        " and -" + otherName +
                                        " cannot be used together"
                        );
                        return;
                    }
                }
            } catch (ReflectiveOperationException e) {
                RThrower.throwError("Invalid argument configuration");
                return;
            }
        }
    }
}
