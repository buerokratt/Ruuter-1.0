package rig.ruuter.util;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.lang.String.format;

@Slf4j
public class HelperFunctionUtils {

    private static final ImmutableMap<String, HelperFunction> HELPER_FUNCTIONS =
        new Reflections("commons", new SubTypesScanner(false))
            .getSubTypesOf(Object.class)
            .stream()
            .map(Class::getMethods)
            .flatMap(Stream::of)
            .filter(method -> Modifier.isStatic(method.getModifiers()))
            .collect(toImmutableMap(Method::getName, method -> args -> method.invoke(null, args)));

    public static final String UNABLE_TO_FIND_FUNCTION = "Unable to find function \"%s\" from helper functions";

    private HelperFunctionUtils() {
    }

    public static HelperFunction get(String functionName) {
        HelperFunction helperFunction = HELPER_FUNCTIONS.get(functionName);
        if (helperFunction == null) {
            String format = format(UNABLE_TO_FIND_FUNCTION, functionName);
            log.error(format);
            throw new IllegalArgumentException(format);
        }
        return helperFunction;
    }

    public static Object callFunction(String functionName, Object arguments) {
        try {
            if (arguments instanceof List<?> && !((List<?>) arguments).isEmpty()) {
                return get(functionName).apply(((List<?>) arguments).toArray());
            } else if (arguments instanceof Map<?, ?> && ((Map<?, ?>) arguments).size() > 0) {
                return get(functionName).apply(((Map<?, ?>) arguments).values().toArray());
            }
            return get(functionName).apply();
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error(format("Unable to call function %s", functionName), e);
            return e.getMessage();
        }
    }
}
