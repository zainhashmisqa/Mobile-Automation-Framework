package com.azm.qa.mobile.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;

/**
 * Attaches {@link RetryAnalyzer} to every {@code @Test} method automatically, so retry
 * behaviour is opt-out (via {@code @Test(retryAnalyzer = ...)} on a specific method) rather
 * than something every new test class has to remember to wire up itself.
 */
public final class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                           Constructor testConstructor, Method testMethod) {
        Class<? extends IRetryAnalyzer> current = annotation.getRetryAnalyzerClass();
        if (current == null || current == IRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
