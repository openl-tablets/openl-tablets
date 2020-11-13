package org.openl.binding.impl.cast;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.ThrowableVoidCast.ThrowableVoid;
import org.openl.cache.GenericKey;
import org.openl.domain.IDomain;
import org.openl.ie.constrainer.ConstrainerObject;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.ComponentTypeArrayOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.OpenClassUtils;

/**
 * Base implementation of {@link ICastFactory} abstraction that used by engine for type conversion operations.
 *
 * @author snshor, Yury Molchan, Marat Kamalov
 */
public class CastFactory implements ICastFactory {

    public static final int NO_CAST_DISTANCE = 1;
    public static final int ALIAS_TO_TYPE_CAST_DISTANCE = 1;

    // USE ONLY EVEN NUMBERS FOR DISTANCES

    public static final int TYPE_TO_ALIAS_CAST_DISTANCE = 2;
    public static final int JAVA_UP_ARRAY_TO_ARRAY_CAST_DISTANCE = 4;
    public static final int JAVA_UP_CAST_DISTANCE = 6;

    public static final int THROWABLE_VOID_CAST_DISTANCE = 8;

    public static final int PRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE = 10;

    public static final int STRING_ENUM_TO_CAST_DISTANCE = 12;

    public static final int JAVA_BOXING_CAST_DISTANCE = 14;

    public static final int JAVA_BOXING_UP_CAST_DISTANCE = 16;

    public static final int PRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE = 18;

    public static final int JAVA_UNBOXING_CAST_DISTANCE = 22;

    public static final int NONPRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE = 24;

    public static final int ENUM_TO_STRING_CAST_DISTANCE = 26;

    public static final int NONPRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE = 28;

    public static final int AFTER_FIRST_WAVE_CASTS_DISTANCE = 30;

    public static final int JAVA_DOWN_CAST_DISTANCE = 60;
    public static final int PRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE = 62;
    public static final int NONPRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE = 64;
    public static final int NONPRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE = 66;
    public static final int PRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE = 68;

    public static final int ARRAY_CAST_DISTANCE = 1000;
    public static final int ONE_ELEMENT_ARRAY_CAST_DISTANCE = 2000;
    public static final int ARRAY_ONE_ELEMENT_CAST_DISTANCE = 3000;

    public static final String AUTO_CAST_METHOD_NAME = "autocast";
    public static final String CAST_METHOD_NAME = "cast";
    public static final String DISTANCE_METHOD_NAME = "distance";

    /**
     * Method factory object. This factory allows to define cast operations thru java methods.
     */
    private IMethodFactory methodFactory;
    private ICastFactory globalCastFactory;

    /**
     * Internal cache of cast operations.
     */
    private final ConcurrentHashMap<Object, IOpenCast> castCache = new ConcurrentHashMap<>();

    public void setMethodFactory(IMethodFactory factory) {
        methodFactory = factory;
    }

    @Override
    public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
        Iterable<IOpenMethod> autocastMethods = methodFactory.methods(AUTO_CAST_METHOD_NAME);
        return findClosestClass(openClass1, openClass2, this, autocastMethods);
    }

    private static IOpenClass getWrapperIfPrimitive(IOpenClass openClass) {
        if (openClass.getInstanceClass() != null && openClass.getInstanceClass().isPrimitive()) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass.getInstanceClass()));
        }
        return openClass;
    }

    public static IOpenClass findClosestClass(IOpenClass openClass1,
            IOpenClass openClass2,
            ICastFactory casts,
            Iterable<IOpenMethod> methods) {
        if (openClass1 == null) {
            throw new IllegalArgumentException("openClass1 cannot be null");
        }
        if (openClass2 == null) {
            throw new IllegalArgumentException("openClass2 cannot be null");
        }

        if (NullOpenClass.the.equals(openClass1)) {
            return getWrapperIfPrimitive(openClass2);
        }
        if (NullOpenClass.the.equals(openClass2)) {
            return getWrapperIfPrimitive(openClass1);
        }

        if (ThrowableVoid.class.equals(openClass1.getInstanceClass())) {
            return openClass2;
        }
        if (ThrowableVoid.class.equals(openClass2.getInstanceClass())) {
            return openClass1;
        }

        if (openClass1 instanceof DomainOpenClass) {
            return findClosestClass(JavaOpenClass.getOpenClass(openClass1.getInstanceClass()),
                openClass2,
                casts,
                methods);
        }
        if (openClass2 instanceof DomainOpenClass) {
            return findClosestClass(openClass1,
                JavaOpenClass.getOpenClass(openClass2.getInstanceClass()),
                casts,
                methods);
        }

        IOpenCast cast1To2 = casts.getCast(openClass1, openClass2);
        IOpenCast cast2To1 = casts.getCast(openClass2, openClass1);
        if (cast1To2 != null && cast2To1 != null) {

            if (!cast1To2.isImplicit() && cast2To1.isImplicit()) {
                return openClass1;
            }
            if (!cast2To1.isImplicit() && cast1To2.isImplicit()) {
                return openClass2;
            }
            // For example NoCast
            if (cast1To2.isImplicit() && cast2To1.isImplicit()) {
                return cast1To2.getDistance() < cast2To1.getDistance() ? openClass2 : openClass1;
            }
        }

        int dim = 0;
        while (openClass1.isArray() && openClass2.isArray()) {
            openClass1 = openClass1.getComponentClass();
            openClass2 = openClass2.getComponentClass();
            dim++;
        }

        // Use one element to array cast
        if (openClass1.isArray() && !openClass1.getComponentClass().isArray() && !openClass2.isArray()) {
            return findClosestClass(openClass1.getComponentClass(), openClass2, casts, methods);
        }
        if (openClass2.isArray() && !openClass2.getComponentClass().isArray() && !openClass1.isArray()) {
            IOpenClass t = findClosestClass(openClass1, openClass2.getComponentClass(), casts, methods);
            return ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(t, 1);
        }

        Iterator<IOpenMethod> itr = methods.iterator();
        Set<IOpenClass> openClass1Candidates = new LinkedHashSet<>();

        addClassToCandidates(openClass1, openClass1Candidates);
        Set<IOpenClass> openClass2Candidates = new LinkedHashSet<>();
        addClassToCandidates(openClass2, openClass2Candidates);
        while (itr.hasNext()) {
            IOpenMethod method = itr.next();
            if (method.getSignature().getNumberOfParameters() == 2) {
                checkAndAddToCandidates(method, openClass1, openClass1Candidates);
                checkAndAddToCandidates(method, openClass2, openClass2Candidates);
            }
        }
        openClass1Candidates.retainAll(openClass2Candidates);

        int bestDistance = Integer.MAX_VALUE;
        Set<IOpenClass> closestClasses = new LinkedHashSet<>();
        for (IOpenClass to : openClass1Candidates) {
            int distance = getDistance(casts, openClass1, openClass2, to);

            if (distance > bestDistance) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                closestClasses.clear();
            }
            closestClasses.add(to);
        }

        openClass1Candidates = closestClasses;

        IOpenClass ret = chooseClosest(casts, openClass1Candidates);

        if (ret == null) {
            IOpenClass c = OpenClassUtils.findParentClass(openClass1, openClass2);
            if (c == null) {
                c = JavaOpenClass.OBJECT;
            }
            return dim > 0 ? ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(c, dim) : c;
        }

        // If one class is not primitive we use wrapper for prevent NPE
        if (openClass1.getInstanceClass() != null && openClass2.getInstanceClass() != null) {
            if (!openClass1.getInstanceClass().isPrimitive() || !openClass2.getInstanceClass().isPrimitive()) {
                if (ret.getInstanceClass().isPrimitive()) {
                    return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(ret.getInstanceClass()));
                }
            }
        }

        return dim > 0 ? ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(ret, dim) : ret;
    }

    private static void checkAndAddToCandidates(IOpenMethod method,
            IOpenClass openClass,
            Set<IOpenClass> openClassCandidates) {
        if (method.getSignature().getParameterType(0).equals(openClass)) {
            addClassToCandidates(method.getSignature().getParameterType(1), openClassCandidates);
        } else {
            if (method.getSignature().getParameterType(0).getInstanceClass().isPrimitive()) {
                IOpenClass t = JavaOpenClass.getOpenClass(
                    ClassUtils.primitiveToWrapper(method.getSignature().getParameterType(0).getInstanceClass()));
                if (t.equals(openClass)) {
                    addClassToCandidates(method.getSignature().getParameterType(1), openClassCandidates);
                }
            }
        }
    }

    private static IOpenClass chooseClosest(ICastFactory castFactory, Collection<IOpenClass> openClassCandidates) {
        IOpenClass ret = null;
        Collection<IOpenClass> notConvertible = new LinkedHashSet<>();
        for (IOpenClass openClass : openClassCandidates) {
            if (ret == null) {
                ret = openClass;
            } else {
                IOpenCast cast = castFactory.getCast(ret, openClass);
                if (cast == null || !cast.isImplicit()) {
                    cast = castFactory.getCast(openClass, ret);
                    if (cast != null && cast.isImplicit()) {
                        // Found narrower candidate. For example Integer is narrower than Double (when convert from
                        // int).
                        ret = openClass;
                    } else {
                        // Two candidate classes are not convertible between each over. For example Float and
                        // BigInteger.
                        // Compare second candidate with remaining candidates later.
                        notConvertible.add(openClass);
                    }
                } else {
                    IOpenCast backCast = castFactory.getCast(openClass, ret);
                    if (backCast != null && backCast.isImplicit()) {
                        int distance = cast.getDistance();
                        int backDistance = backCast.getDistance();

                        if (distance > backDistance) {
                            // Assume that a cast to openClass is narrower than a cast to ret.
                            ret = openClass;
                        } else if (distance == backDistance) {
                            // We have a collision.
                            String message = "Cannot find closest cast: have two candidate classes with same cast distance: " + ret
                                .getName() + " and " + openClass.getName();
                            throw new IllegalStateException(message);
                        } else {
                            // Previous candidate is narrower. Keep it.
                        }
                    } else {
                        // Previous candidate is narrower. Keep it.
                    }
                }
            }
        }

        if (!notConvertible.isEmpty()) {
            Collection<IOpenClass> newCandidates = new LinkedHashSet<>(notConvertible);
            newCandidates.add(ret);

            if (newCandidates.size() == openClassCandidates.size()) {
                // Cannot filter out classes to choose a closest. Prevent infinite recursion.
                String message = "Cannot find closest cast: have several candidate classes not convertible between each over: " + Arrays
                    .toString(newCandidates.toArray());
                throw new IllegalStateException(message);
            }

            return chooseClosest(castFactory, newCandidates);
        }

        return ret;
    }

    private static int getDistance(ICastFactory casts, IOpenClass from1, IOpenClass from2, IOpenClass to) {
        IOpenCast cast1 = casts.getCast(from1, to);
        IOpenCast cast2 = casts.getCast(from2, to);

        int distance;
        if (cast1 == null || !cast1.isImplicit() || cast2 == null || !cast2.isImplicit()) {
            distance = Integer.MAX_VALUE;
        } else {
            distance = Math.max(cast1.getDistance(), cast2.getDistance());
        }
        return distance;
    }

    private static void addClassToCandidates(IOpenClass openClass, Set<IOpenClass> candidates) {
        if (openClass.getInstanceClass() != null) {
            candidates.add(openClass);
            if (openClass.getInstanceClass().isPrimitive()) {
                candidates.add(JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass.getInstanceClass())));
            } else {
                Class<?> t = ClassUtils.wrapperToPrimitive(openClass.getInstanceClass());
                if (t != null) {
                    candidates.add(JavaOpenClass.getOpenClass(t));
                }
            }
        }
    }

    /**
     * Gets cast operation for given types. This is method is using internal cache for cast operations.
     *
     * @param from from type
     * @param to to type
     * @return cast operation if it have been found; null - otherwise
     */
    @Override
    public IOpenCast getCast(IOpenClass from, IOpenClass to) {
        /* BEGIN: This is very cheap operations, so no needs to cache it */
        if (from == to || from.equals(to)) {
            return JavaNoCast.getInstance();
        }

        if (NullOpenClass.the.equals(to)) {
            return null;
        }

        if (NullOpenClass.the.equals(from)) {
            if (isPrimitive(to)) {
                return null;
            } else {
                return JavaUpCast.getInstance();
            }
        }

        if (ThrowableVoid.class.equals(from.getInstanceClass())) {
            return ThrowableVoidCast.getInstance();
        }
        /* END: This is very cheap operations, so no needs to cache it */
        Object key = GenericKey.getInstance(from, to);
        IOpenCast cast = castCache.get(key);
        if (cast == CastNotFound.getInstance()) {
            return null;
        }
        if (cast != null) {
            return cast;
        }

        IOpenCast typeCast = findCast(from, to);
        if (typeCast == null) {
            typeCast = CastNotFound.getInstance();
        }

        IOpenCast saved = castCache.putIfAbsent(key, typeCast);
        if (saved != null) {
            // Concurrent modification happens
            // Return saved instance
            typeCast = saved;
        }

        return typeCast == CastNotFound.getInstance() ? null : typeCast;
    }

    private IOpenCast findCast(IOpenClass from, IOpenClass to) {
        IOpenCast typeCast = findArrayCast(from, to);
        if (typeCast != null) {
            return typeCast;
        }

        typeCast = findAliasCast(from, to);
        if (typeCast == null && from instanceof DomainOpenClass && to instanceof DomainOpenClass && from != to) {
            return findOneElementArrayCast(from, to);
        }

        IOpenCast javaCast = findJavaCast(from, to);
        // Select minimum between alias cast and java cast
        typeCast = selectBetterCast(from, to, typeCast, javaCast);

        IOpenCast methodBasedCast = findMethodBasedCast(from, to, methodFactory);
        typeCast = selectBetterCast(from, to, typeCast, methodBasedCast);

        typeCast = typeCast == null ? findOneElementArrayCast(from, to) : typeCast;

        typeCast = typeCast == null ? findArrayOneElementCast(from, to) : typeCast;

        return typeCast;
    }

    private IOpenCast findArrayOneElementCast(IOpenClass from, IOpenClass to) {
        if (from.isArray() && !to.isArray() && !from.getComponentClass().isArray()) {
            IOpenCast cast = getCast(from.getComponentClass(), to);
            if (cast != null) {
                return new ArrayOneElementCast(to, cast);
            }
        }
        return null;
    }

    private IOpenCast selectBetterCast(IOpenClass from, IOpenClass to, IOpenCast castA, IOpenCast castB) {
        if (castA == null && castB == null) {
            return null;
        }
        if (castA == null) {
            return castB;
        }
        if (castB == null) {
            return castA;
        }

        int distanceA = castA.getDistance();
        int distanceB = castB.getDistance();

        return distanceA > distanceB ? castB : castA;
    }

    private IOpenCast getUpCast(Class<?> from, Class<?> to) {
        if (from.isArray() && to.isArray()) {
            return JavaUpArrayCast.getInstance();
        }
        return JavaUpCast.getInstance();
    }

    private IOpenCast findArrayCast(IOpenClass from, IOpenClass to) {
        if (!to.isArray()) {
            return null;
        }
        Class<?> fromClass = from.getInstanceClass();
        if (to.isAssignableFrom(from) && !(to instanceof DomainOpenClass)) {
            // Improve for up cast
            return getUpCast(fromClass, to.getInstanceClass());
        }
        if (Object.class.equals(fromClass)) {
            // Special case for casting when:
            // Object from = new SomeType[x]
            // SomeType[] to = from
            return new JavaDownCast(to, this);
        }
        if (!from.isArray()) {
            return null;
        }

        IOpenClass t = to.getComponentClass();
        IOpenClass f = from.getComponentClass();
        if (!f.isArray() && t.isArray()) {
            // to prevent Obj[] -> Obj[][] because of findOneElementArrayCast
            return null;
        }
        IOpenCast arrayElementCast = getCast(f, t);
        if (arrayElementCast != null && !(arrayElementCast instanceof IArrayOneElementCast) && !(arrayElementCast instanceof IOneElementArrayCast)) {
            return new ArrayCast(t, arrayElementCast);
        }
        return null;
    }

    private IOpenCast findOneElementArrayCast(IOpenClass from, IOpenClass to) {
        if (!from.isArray() && to.isArray() && !to.getComponentClass().isArray()) {
            IOpenClass componentClass = to.getComponentClass();
            IOpenCast cast = getCast(from, componentClass);
            if (cast != null) {
                return new OneElementArrayCast(componentClass, cast);
            }
        }
        return null;
    }

    public ICastFactory getGlobalCastFactory() {
        if (globalCastFactory == null) {
            return this;
        }
        return globalCastFactory;
    }

    public void setGlobalCastFactory(ICastFactory globalCastFactory) {
        this.globalCastFactory = globalCastFactory;
    }

    /**
     * Checks that instance class of open class is primitive.
     *
     * @param openClass type to check
     * @return <code>true</code> if instance class is primitive type; <code>false</code> - otherwise
     */
    private boolean isPrimitive(IOpenClass openClass) {
        return openClass != null && openClass.getInstanceClass() != null && openClass.getInstanceClass().isPrimitive();
    }

    /**
     * Finds appropriate cast type operation using cast rules of java language. If result type is not java class
     * <code>null</code> will be returned.
     *
     * @param from from type
     * @param to to type
     * @return cast operation if conversion is found; null - otherwise
     */
    private IOpenCast findJavaCast(IOpenClass from, IOpenClass to) {
        // Try to find cast using instance classes.
        //
        Class<?> fromClass = from.getInstanceClass();
        Class<?> toClass = to.getInstanceClass();

        if (fromClass == toClass && from != to && from instanceof ADynamicClass && to instanceof ADynamicClass) { // Dynamic
            // classes
            // with
            // the
            // same
            // instance
            // class
            return null;
        }

        if (ConstrainerObject.class.isAssignableFrom(fromClass)) {
            return null;
        }

        if (to.isAssignableFrom(from)) {
            return getUpCast(fromClass, toClass);
        }

        IOpenCast typeCast = findBoxingCast(from, to);

        if (typeCast != null) {
            return typeCast;
        }

        typeCast = findUnBoxingCast(from, to);

        if (typeCast != null) {
            return typeCast;
        }

        if (isAllowJavaDownCast(from, to)) {
            return new JavaDownCast(to, getGlobalCastFactory());
        }

        if (fromClass.isEnum() && toClass == String.class) {
            return EnumToStringCast.getInstance();
        }
        if (String.class == fromClass && toClass.isEnum()) {
            return new StringToEnumCast(toClass);
        }
        return null;
    }

    /**
     * Finds appropriate auto boxing (primitive to wrapper object) cast operation.
     *
     * @param from primitive type
     * @param to wrapper type
     * @return auto boxing cast operation if conversion is found; null - otherwise
     */
    private IOpenCast findBoxingCast(IOpenClass from, IOpenClass to) {

        if (from == null || to == null || !isPrimitive(from) || isPrimitive(to)) {
            return null;
        }

        Class<?> fromClass = from.getInstanceClass();
        Class<?> toClass = to.getInstanceClass();

        if (fromClass.equals(ClassUtils.wrapperToPrimitive(toClass))) {
            return JavaBoxingCast.getInstance();
        }

        if (toClass.isAssignableFrom(ClassUtils.primitiveToWrapper(fromClass))) {
            return JavaBoxingUpCast.getInstance();
        }

        // Apache ClassUtils has error in 2.6
        if (void.class.equals(fromClass) && Void.class.equals(toClass)) {
            return JavaBoxingCast.getInstance();
        }

        return null;
    }

    /**
     * Finds appropriate unboxing (wrapper object to primitive) cast operation.
     *
     * @param from wrapper type
     * @param to primitive type
     * @return unboxing cast operation if conversion is found; null - otherwise
     */
    private IOpenCast findUnBoxingCast(IOpenClass from, IOpenClass to) {

        if (from == null || to == null || isPrimitive(from) || !isPrimitive(to)) {
            return null;
        }

        Class<?> fromClass = from.getInstanceClass();
        Class<?> toClass = to.getInstanceClass();

        if (toClass.equals(ClassUtils.wrapperToPrimitive(fromClass))) {
            return JavaUnboxingCast.getInstance(fromClass);
        }

        // Apache ClassUtils has error in 2.6
        if (Void.class.equals(fromClass) && void.class.equals(toClass)) {
            return JavaUnboxingCast.getInstance(fromClass);
        }

        return null;
    }

    /**
     * Finds cast operation for alias types. If both types are not alias types <code>null</code> will be returned.
     *
     * @param from from type
     * @param to to type
     * @return alias cast operation if conversion is found; null - otherwise
     */
    private IOpenCast findAliasCast(IOpenClass from, IOpenClass to) {
        if (!from.isArray() && !to.isArray() && (from instanceof DomainOpenClass || to instanceof DomainOpenClass)) {
            if (from instanceof DomainOpenClass && to instanceof DomainOpenClass && from != to) {
                DomainOpenClass fromDomainOpenClass = (DomainOpenClass) from;
                DomainOpenClass toDomainOpenClass = (DomainOpenClass) to;
                IOpenCast openCast = getCast(fromDomainOpenClass.getBaseClass(), toDomainOpenClass.getBaseClass());
                if (openCast != null) {
                    if (openCast.isImplicit() && isFromValuesIncludedToValues(fromDomainOpenClass,
                        toDomainOpenClass,
                        openCast)) {
                        return new AliasToAliasOpenCast(openCast);
                    }
                    if (isFromValuesIntersectedWithToValues(fromDomainOpenClass, toDomainOpenClass, openCast)) {
                        return new AliasToAliasOpenCast(openCast, false);
                    }
                }
                return null;
            }
            if (from instanceof DomainOpenClass && !(to instanceof DomainOpenClass) && to
                .equals(((DomainOpenClass) from).getBaseClass())) {
                return AliasToTypeCast.getInstance();
            }

            if (!(from instanceof DomainOpenClass) && to instanceof DomainOpenClass && from
                .equals(((DomainOpenClass) to).getBaseClass())) {
                return new TypeToAliasCast(to);
            }

            if (from instanceof DomainOpenClass && to.getInstanceClass().isAssignableFrom(from.getClass())) { // This is
                // not
                // typo
                return JavaUpCast.getInstance();
            }

            if (from instanceof DomainOpenClass && !(to instanceof DomainOpenClass)) {
                IOpenCast openCast = this.findCast(JavaOpenClass.getOpenClass(from.getInstanceClass()), to);
                if (openCast != null) {
                    return new AliasToTypeCast(openCast);
                }
            }

            if (to instanceof DomainOpenClass && !(from instanceof DomainOpenClass)) {
                IOpenCast openCast = this.findCast(from, JavaOpenClass.getOpenClass(to.getInstanceClass()));
                if (openCast != null) {
                    return new TypeToAliasCast(to, openCast);
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean isFromValuesIncludedToValues(DomainOpenClass from, DomainOpenClass to, IOpenCast openCast) {
        IDomain<Object> fromDomain = (IDomain<Object>) from.getDomain();
        IDomain<Object> toDomain = (IDomain<Object>) to.getDomain();
        for (Object value : fromDomain) {
            if (!toDomain.selectObject(openCast.convert(value))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean isFromValuesIntersectedWithToValues(DomainOpenClass from, DomainOpenClass to, IOpenCast openCast) {
        IDomain<Object> fromDomain = (IDomain<Object>) from.getDomain();
        IDomain<Object> toDomain = (IDomain<Object>) to.getDomain();
        for (Object value : fromDomain) {
            if (toDomain.selectObject(openCast.convert(value))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds cast operation using {@link IMethodFactory} object.
     *
     * @param from from type
     * @param to to type
     * @param methodFactory {@link IMethodFactory} object
     * @return cast operation
     */
    private IOpenCast findMethodBasedCast(IOpenClass from, IOpenClass to, IMethodFactory methodFactory) {

        IOpenCast typeCast = findMethodCast(from, to, methodFactory);

        if (typeCast != null) {
            return typeCast;
        }

        typeCast = findMethodCast(from, to, from);

        if (typeCast != null) {
            return typeCast;
        }

        typeCast = findMethodCast(from, to, to);

        if (typeCast != null) {
            return typeCast;
        }

        return null;
    }

    /**
     * Finds cast operation using {@link IMethodFactory} object.
     *
     * @param from from type
     * @param to to type
     * @param methodFactory {@link IMethodFactory} object
     * @return cast operation
     */
    private IOpenCast findMethodCast(IOpenClass from, IOpenClass to, IMethodFactory methodFactory) {

        if (methodFactory == null) {
            return null;
        }

        // Is auto cast ?
        boolean auto = true;
        int distance;
        if (from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
            distance = PRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE;
        } else if (!from.getInstanceClass().isPrimitive() && to.getInstanceClass().isPrimitive()) {
            distance = NONPRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE;
        } else if (!from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
            distance = NONPRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE;
        } else {
            distance = PRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE;
        }

        // Matching method
        IMethodCaller castCaller = null;

        IOpenClass fromOpenClass = from;
        IOpenClass toOpenClass = to;

        Class<?> primitiveClassFrom = ClassUtils.wrapperToPrimitive(from.getInstanceClass());
        Class<?> primitiveClassTo = ClassUtils.wrapperToPrimitive(to.getInstanceClass());

        try {
            // Try to find matching auto cast method
            castCaller = methodFactory.getMethod(AUTO_CAST_METHOD_NAME, new IOpenClass[] { from, to });

            if (castCaller == null) {
                // If from parameter is wrapper for primitive type try to find
                // auto cast method using 'from' as primitive type. In this case
                // we are emulate 2 operations: 1) unboxing operation 2)
                // autocast operation.
                // For example:
                // <code>
                // Integer a = 1;
                // double d = a;
                // </code>
                // For OpenL we are omitting the check that 'to' type must be
                // primitive type for our case to simplify understanding type
                // operations in
                // engine by end-user.
                //
                if (primitiveClassFrom != null) {
                    IOpenClass openClassFrom = JavaOpenClass.getOpenClass(primitiveClassFrom);
                    fromOpenClass = openClassFrom;
                    toOpenClass = to;
                    castCaller = methodFactory.getMethod(AUTO_CAST_METHOD_NAME, new IOpenClass[] { openClassFrom, to });
                }
            }

            if (castCaller == null) {
                // If to parameter is wrapper for primitive type try to find
                // auto cast method using 'to' as primitive type. In this case
                // we are emulate 2 operations: 1) autocast operation,
                // 2) boxing operation.
                // For example:
                // <code>
                // int a = 1;
                // Double d = a;
                // </code>
                // For OpenL we are omitting the check that 'from' type must be
                // primitive type for our case to simplify understanding type
                // operations in
                // engine by end-user.
                //
                if (primitiveClassTo != null) {
                    IOpenClass openClassTo = JavaOpenClass.getOpenClass(primitiveClassTo);
                    castCaller = methodFactory.getMethod(AUTO_CAST_METHOD_NAME, new IOpenClass[] { from, openClassTo });
                    fromOpenClass = from;
                    toOpenClass = openClassTo;
                }
            }

            if (castCaller == null && primitiveClassFrom != null && primitiveClassTo != null) {
                IOpenClass openClassFrom = JavaOpenClass.getOpenClass(primitiveClassFrom);
                IOpenClass openClassTo = JavaOpenClass.getOpenClass(primitiveClassTo);
                fromOpenClass = openClassFrom;
                toOpenClass = openClassTo;
                castCaller = methodFactory.getMethod(AUTO_CAST_METHOD_NAME,
                    new IOpenClass[] { openClassFrom, openClassTo });
            }
        } catch (AmbiguousMethodException ex) {
            // Ignore exception.
            //
        }

        // If appropriate auto cast method is not found try to find explicit
        // cast method.
        //
        if (castCaller == null) {
            auto = false;
            try {
                castCaller = methodFactory.getMethod(CAST_METHOD_NAME, new IOpenClass[] { from, to });
                if (from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
                    distance = PRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE;
                } else if (!from.getInstanceClass().isPrimitive() && to.getInstanceClass().isPrimitive()) {
                    distance = NONPRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE;
                } else if (!from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
                    distance = NONPRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE;
                } else {
                    distance = PRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE;
                }

                if (castCaller == null && primitiveClassFrom != null) {
                    IOpenClass openClassFrom = JavaOpenClass.getOpenClass(primitiveClassFrom);
                    fromOpenClass = openClassFrom;
                    toOpenClass = to;
                    castCaller = methodFactory.getMethod(CAST_METHOD_NAME, new IOpenClass[] { openClassFrom, to });
                }

                if (castCaller == null && primitiveClassTo != null) {
                    IOpenClass openClassTo = JavaOpenClass.getOpenClass(primitiveClassTo);
                    castCaller = methodFactory.getMethod(CAST_METHOD_NAME, new IOpenClass[] { from, openClassTo });
                    fromOpenClass = from;
                    toOpenClass = openClassTo;
                }

                if (castCaller == null && primitiveClassFrom != null && primitiveClassTo != null) {
                    IOpenClass openClassFrom = JavaOpenClass.getOpenClass(primitiveClassFrom);
                    IOpenClass openClassTo = JavaOpenClass.getOpenClass(primitiveClassTo);
                    fromOpenClass = openClassFrom;
                    toOpenClass = openClassTo;
                    castCaller = methodFactory.getMethod(CAST_METHOD_NAME,
                        new IOpenClass[] { openClassFrom, openClassTo });
                }

            } catch (AmbiguousMethodException ex) {
                // Ignore exception.
                //
            }
        }

        if (castCaller == null) {
            return null;
        }

        IMethodCaller distanceCaller = null;

        try {
            distanceCaller = methodFactory.getMethod(DISTANCE_METHOD_NAME,
                new IOpenClass[] { fromOpenClass, toOpenClass });
        } catch (AmbiguousMethodException ignored) {
        }

        if (distanceCaller != null) {
            distance = (Integer) distanceCaller
                .invoke(null, new Object[] { fromOpenClass.nullObject(), toOpenClass.nullObject() }, null);
        }

        return new MethodBasedCast(castCaller, auto, distance, to, toOpenClass.nullObject());
    }

    /**
     * The following conversions are called the narrowing reference conversions:
     * <p>
     * From any class type S to any class type T, provided that S is a superclass of T. (An important special case is
     * that there is a narrowing conversion from the class type Object to any other class type.) From any class type S
     * to any interface type K, provided that S is not final and does not implement K. (An important special case is
     * that there is a narrowing conversion from the class type Object to any interface type.) From type Object to any
     * array type. From type Object to any interface type. From any interface type J to any class type T that is not
     * final. From any interface type J to any class type T that is final, provided that T implements J. From any
     * interface type J to any interface type K, provided that J is not a subinterface of K and there is no method name
     * m such that J and K both contain a method named m with the same signature but different return types. From any
     * array type SC[] to any array type TC[], provided that SC and TC are reference types and there is a narrowing
     * conversion from SC to TC.
     *
     * @param from from type
     * @param to to type
     * @return <code>true</code> is downcast operation is allowed for given types; <code>false</code> - otherwise
     * @link http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc .html
     */
    private boolean isAllowJavaDownCast(IOpenClass from, IOpenClass to) {

        if (from.isAssignableFrom(to)) {
            return true;
        }

        Class<?> fromClass = from.getInstanceClass();
        Class<?> toClass = to.getInstanceClass();

        if (!fromClass.isPrimitive() && !Modifier.isFinal(fromClass.getModifiers()) && to.isInterface()) {
            return true;
        }

        return !toClass.isPrimitive() && !Modifier.isFinal(toClass.getModifiers()) && from.isInterface();

    }

    public IMethodFactory getMethodFactory() {
        return methodFactory;
    }
}
