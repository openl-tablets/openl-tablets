package org.openl.vm;

import java.util.IdentityHashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.IDomain;
import org.openl.rules.cloner.Cloner;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.DomainUtils;

/**
 * Checks values against the domains of alias types and remembers what an execution has checked, so that a value
 * passed on from rule to rule is looked up in a domain once.
 * <p>
 * A value is remembered as the very object that was checked, together with its alias type. Only a value that cannot
 * change is remembered, one of the immutable types the {@link Cloner} shares: a text, a number, a date of
 * {@code java.time} and more. A mutable value, such as a {@link java.util.Date}, may leave the domain later, so it is
 * looked up every time.
 * <p>
 * Up to {@value #MAX_VALUES} values are kept; the next one makes the execution forget them all. An instance serves one
 * execution thread.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CheckedAliasValues {

    /**
     * Checks every value: for an environment that keeps none.
     */
    public static final CheckedAliasValues NONE = new CheckedAliasValues(0);

    static final int MAX_VALUES = 4096;

    private final Map<Object, DomainOpenClass> aliasTypes = new IdentityHashMap<>();
    private final int limit;

    CheckedAliasValues() {
        this(MAX_VALUES);
    }

    /**
     * Checks that {@code value} is in the domain of {@code aliasType}, unless this very value was found there before.
     *
     * @throws OutsideOfValidDomainException when the value is not in the domain
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void check(Object value, DomainOpenClass aliasType) {
        if (contains(value, aliasType)) {
            return;
        }
        IDomain domain = aliasType.getDomain();
        if (!domain.selectObject(value)) {
            throw new OutsideOfValidDomainException(value, aliasType.getName(), DomainUtils.toString(domain));
        }
        if (limit > 0 && Cloner.isImmutable(value.getClass())) {
            if (aliasTypes.size() >= limit) {
                aliasTypes.clear();
            }
            aliasTypes.put(value, aliasType);
        }
    }

    /**
     * Whether this very {@code value} was found before in the domain of {@code aliasType}.
     */
    boolean contains(Object value, DomainOpenClass aliasType) {
        return aliasTypes.get(value) == aliasType;
    }
}
