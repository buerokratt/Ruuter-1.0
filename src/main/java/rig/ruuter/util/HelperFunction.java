package rig.ruuter.util;

import java.lang.reflect.InvocationTargetException;

public interface HelperFunction {
    Object apply(Object... args) throws InvocationTargetException, IllegalAccessException;
}
