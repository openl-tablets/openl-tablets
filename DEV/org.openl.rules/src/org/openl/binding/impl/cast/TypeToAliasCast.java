package org.openl.binding.impl.cast;

import java.lang.reflect.Array;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.types.impl.DomainOpenClass;
import org.openl.vm.CheckedAliasValues;
import org.openl.vm.IRuntimeEnv;

/**
 * Class provides feature to convert alias data type to underlying type.
 * <p>
 * Alias data type is special type in OpenL engine. This type used by engine as helpful mechanism to provide for users
 * ability to use types with predefined values. For example, person age is int value, but it cannot be negative. For
 * this reason user can define alias type Age with underlying type IntRange and define appropriate values for age field
 * ( e.g. [0 .. 150]). If user can try to set value which is not contained in range runtime exception will be thrown.
 * For simplicity, alias type is just runtime validation of variable values.
 *
 * @see IOpenCast
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TypeToAliasCast implements IOpenCast, INestedCastOpenCast {

    /**
     * Result type of object after conversion.
     */
    private final DomainOpenClass toClass;
    @Getter
    private int distance = CastFactory.TYPE_TO_ALIAS_CAST_DISTANCE;
    private IOpenCast typeCast;
    @Getter
    private boolean implicit = true;

    TypeToAliasCast(DomainOpenClass to, IOpenCast typeCast) {
        this.toClass = to;
        this.typeCast = typeCast;
        this.distance = typeCast.getDistance() - 1;// This cast has higher priority
        this.implicit = typeCast.isImplicit();
    }

    @Override
    public IOpenCast getNestedOpenCast() {
        return typeCast;
    }

    @Override
    public boolean hasNestedOpenCast() {
        return typeCast != null;
    }

    @Override
    public @Nullable Object convert(Object from) {
        return convert(from, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A value the execution found in the domain before, the very same object, is not looked up again. Without an
     * environment every value is looked up.
     */
    @Override
    public @Nullable Object convert(Object from, @Nullable IRuntimeEnv env) {
        var checked = Optional.ofNullable(env)
                .map(IRuntimeEnv::getCheckedAliasValues)
                .orElse(CheckedAliasValues.NONE);
        return checkInDomain(typeCast != null ? typeCast.convert(from, env) : from, checked);
    }

    private @Nullable Object checkInDomain(@Nullable Object from, CheckedAliasValues checked) {
        if (from == null) {
            if (toClass.getInstanceClass().isPrimitive()) {
                from = Array.get(Array.newInstance(toClass.getInstanceClass(), 1), 0);
            } else {
                return null;
            }
        }
        checked.check(from, toClass);
        return from;
    }

}
