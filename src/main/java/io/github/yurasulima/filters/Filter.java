package io.github.yurasulima.filters;

import io.github.yurasulima.types.Update;

/**
 * A predicate on an {@link Update}.
 * Filters are composable via {@code and()}, {@code or()}, {@code negate()}.
 */
@FunctionalInterface
public interface Filter {

    boolean test(Update update);

    default Filter and(Filter other) {
        return u -> this.test(u) && other.test(u);
    }

    default Filter or(Filter other) {
        return u -> this.test(u) || other.test(u);
    }

    default Filter negate() {
        return u -> !this.test(u);
    }
}