package com.zainab.roamSafe.config;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Keeps scheduled beans out of lazy initialization.
 *
 * Production sets spring.main.lazy-initialization=true so Tomcat binds its port
 * quickly, but that means a bean is only built when something asks for it - and
 * nothing ever asks for a bean that exists purely to run on a timer. Its
 * @Scheduled methods are then never registered with the scheduler, so the job
 * silently never runs. Nothing fails; the work simply doesn't happen, which is
 * the hardest kind of bug to notice.
 *
 * That would have quietly disabled advisory refreshes, police data refreshes,
 * daily score snapshots and incident ingestion on every deployment, while all of
 * them worked in development where lazy initialization is off.
 *
 * Detection is by annotation rather than by listing the four services, so a
 * scheduled job added later is covered without anyone remembering this file.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    static LazyInitializationExcludeFilter eagerlyCreateScheduledBeans() {
        return (beanName, beanDefinition, beanType) -> beanType != null && hasScheduledMethod(beanType);
    }

    private static boolean hasScheduledMethod(Class<?> type) {
        try {
            return Arrays.stream(type.getMethods()).anyMatch(SchedulingConfig::isScheduled)
                    || Arrays.stream(type.getDeclaredMethods()).anyMatch(SchedulingConfig::isScheduled);
        } catch (Throwable t) {
            // A type whose methods can't be inspected is not worth failing over;
            // treat it as unscheduled and let lazy init handle it as before.
            return false;
        }
    }

    private static boolean isScheduled(Method method) {
        return method.isAnnotationPresent(Scheduled.class) || method.isAnnotationPresent(Schedules.class);
    }
}
