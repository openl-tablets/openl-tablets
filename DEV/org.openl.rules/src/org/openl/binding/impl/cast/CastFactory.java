package org.openl.binding.impl.cast;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.cache.GenericKey;
import org.openl.conf.LibrariesRegistry;
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
@Slf4j
public class CastFactory implements ICastFactory {

    private static final String IGNORED_ERROR = "Ignored error: ";

    private static final Set<Class<?>> INTERFACES_IGNORABLE_IN_SEARCH_PARENT_CLASS = Set
            .of(Serializable.class, Cloneable.class, Comparable.class);

    private static final Predicate<IOpenClass> isNotIgnorableInParentSearch =
            e -> e != null && e.getInstanceClass() != null && !INTERFACES_IGNORABLE_IN_SEARCH_PARENT_CLASS
            .contains(e.getInstanceClass()) && e.getInstanceClass().getPackage() != null && !Objects
            .equals(e.getInstanceClass().getPackage().getName(), "java.lang.constant");


    public static final int NO_CAST_DISTANCE = 1;
    public static final int ALIAS_TO_TYPE_CAST_DISTANCE = 1;

    // USE ONLY EVEN NUMBERS FOR DISTANCES

    public static final int TYPE_TO_ALIAS_CAST_DISTANCE = 2;

    public static final int PRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE = 4;

    public static final int JAVA_BOXING_CAST_DISTANCE = 6;

    public static final int JAVA_UP_ARRAY_TO_ARRAY_CAST_DISTANCE = 8;
    public static final int JAVA_UP_CAST_DISTANCE = 8;

    public static final int JAVA_BOXING_UP_CAST_DISTANCE = 10;

    public static final int CAST_TO_ANY_DISTANCE = 12;

    public static final int STRING_ENUM_TO_CAST_DISTANCE = 16;

    public static final int PRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE = 18;

    public static final int NONPRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE = 22;

    public static final int JAVA_UNBOXING_CAST_DISTANCE = 24;

    public static final int ENUM_TO_STRING_CAST_DISTANCE = 26;

    public static final int NONPRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE = 28;

    public static final int AFTER_FIRST_WAVE_CASTS_DISTANCE = 30;

    public static final int JAVA_DOWN_CAST_DISTANCE = 60;
    public static final int PRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE = 62;
    public static final int NONPRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE = 64;
    public static final int NONPRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE = 66;
    public static final int PRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE = 68;

    public static final int ONE_ELEMENT_ARRAY_CAST_DISTANCE = 2000;
    public static final int ARRAY_ONE_ELEMENT_CAST_DISTANCE = 3000;

    public static final String AUTO_CAST_METHOD_NAME = "autocast";
    public static final String CAST_METHOD_NAME = "cast";
    public static final String DISTANCE_METHOD_NAME = "distance";

    /**
     * Method factory object. This factory allows to define cast operations thru java methods.
     */
    private IMethodFactory methodFactory;

    /**
     * Internal cache of cast operations.
     */
    private final ConcurrentHashMap<Object, IOpenCast> castCache = new ConcurrentHashMap<>();

    public static ICastFactory create() {
        var castFactory = new CastFactory();
        castFactory.methodFactory = new LibrariesRegistry().asMethodFactory();
        return castFactory;
    }

    public void setMethodFactory(IMethodFactory factory) {
        methodFactory = factory;
    }

    @Override
    public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
        var autoCastMethods = methodFactory.methods(AUTO_CAST_METHOD_NAME);
        return findClosestClass(openClass1, openClass2, this, autoCastMethods);
    }

    private static IOpenClass findClosestClass(IOpenClass openClass1,
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
            return OpenClassUtils.toWrapperIfPrimitive(openClass2);
        }
        if (NullOpenClass.the.equals(openClass2)) {
            return OpenClassUtils.toWrapperIfPrimitive(openClass1);
        }

        if (VOID.class.equals(openClass1.getInstanceClass())) {
            return openClass2;
        }
        if (VOID.class.equals(openClass2.getInstanceClass())) {
            return openClass1;
        }

        var moduleSpecificClosestClass = findModuleSpecificClosestClass(openClass1, openClass2);
        if (moduleSpecificClosestClass != null) {
            return moduleSpecificClosestClass;
        }

        if (openClass1 instanceof DomainOpenClass && !(openClass2 instanceof DomainOpenClass) || !(openClass1 instanceof DomainOpenClass) && openClass2 instanceof DomainOpenClass) {
            return findClosestClass(toJavaClassIfDomain(openClass1), toJavaClassIfDomain(openClass2), casts, methods);
        }

        var closestByCasts = chooseByImplicitCasts(casts, openClass1, openClass2);
        if (closestByCasts != null) {
            return closestByCasts;
        }

        if (openClass1 instanceof DomainOpenClass) {
            return findClosestClass(JavaOpenClass.getOpenClass(openClass1.getInstanceClass()),
                    JavaOpenClass.getOpenClass(openClass2.getInstanceClass()),
                    casts,
                    methods);
        }

        return findClosestClassByAutoCasts(openClass1, openClass2, casts, methods);
    }

    /**
     * Returns the closest class of two module specific types. It is looked up in the type whose module depends on
     * the module of the other type.
     *
     * @return the closest class, or {@code null} when it is not found or a type is not module specific
     */
    private static IOpenClass findModuleSpecificClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
        if (openClass1 instanceof ModuleSpecificType moduleSpecificType1 && openClass2 instanceof ModuleSpecificType moduleSpecificType2) {
            if (moduleSpecificType1.getModule()
                    .isDependencyModule(moduleSpecificType2.getModule(), new IdentityHashMap<>())) {
                return moduleSpecificType1.getClosestClass(moduleSpecificType2);
            } else {
                return moduleSpecificType2.getClosestClass(moduleSpecificType1);
            }
        }
        return null;
    }

    private static IOpenClass toJavaClassIfDomain(IOpenClass openClass) {
        return openClass instanceof DomainOpenClass ? JavaOpenClass.getOpenClass(openClass.getInstanceClass())
                : openClass;
    }

    /**
     * Chooses one of the classes by the casts between them. A class is chosen when the other class casts to it
     * implicitly and there is no back cast. Classes with casts in both directions are compared by the casts.
     *
     * @return the chosen class, or {@code null} when the casts do not decide
     */
    private static IOpenClass chooseByImplicitCasts(ICastFactory casts, IOpenClass openClass1, IOpenClass openClass2) {
        var cast1To2 = casts.getCast(openClass1, openClass2);
        var cast2To1 = casts.getCast(openClass2, openClass1);
        if (cast1To2 != null && cast1To2.isImplicit() && cast2To1 == null) {
            return openClass2;
        } else if (cast2To1 != null && cast2To1.isImplicit() && cast1To2 == null) {
            return openClass1;
        } else if (cast1To2 != null && cast2To1 != null) {
            return chooseByMutualCasts(cast1To2, cast2To1, openClass1, openClass2);
        }
        return null;
    }

    /**
     * Chooses between two classes that cast to each other. The class with the only implicit cast to it wins. When
     * both casts are implicit, the class with the shorter cast to it wins.
     *
     * @return the chosen class, or {@code null} when both casts are explicit
     */
    private static IOpenClass chooseByMutualCasts(IOpenCast cast1To2,
                                                  IOpenCast cast2To1,
                                                  IOpenClass openClass1,
                                                  IOpenClass openClass2) {
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
        return null;
    }

    /**
     * Finds the closest class among the classes that both classes convert to by the auto cast methods, or else their
     * parent class. Arrays are compared by their component types within the common dimension.
     */
    private static IOpenClass findClosestClassByAutoCasts(IOpenClass openClass1,
                                                          IOpenClass openClass2,
                                                          ICastFactory casts,
                                                          Iterable<IOpenMethod> methods) {
        var dim = 0;
        while (openClass1.isArray() && openClass2.isArray()) {
            openClass1 = openClass1.getComponentClass();
            openClass2 = openClass2.getComponentClass();
            dim++;
        }

        IOpenClass ret = chooseClosest(casts, findClosestCandidates(openClass1, openClass2, casts, methods));

        if (ret == null) {
            var c = findParentClassOrObject(openClass1, openClass2, casts);
            return dim > 0 ? ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(c, dim) : c;
        }

        // If one class is not primitive we use wrapper for prevent NPE
        if (openClass1.getInstanceClass() != null && openClass2.getInstanceClass() != null
                && (!openClass1.getInstanceClass().isPrimitive() || !openClass2.getInstanceClass().isPrimitive())
                && ret.getInstanceClass().isPrimitive()) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(ret.getInstanceClass()));
        }

        return dim > 0 ? ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(ret, dim) : ret;
    }

    /**
     * Collects the classes that both classes convert to by the auto cast methods, and keeps the ones with the
     * shortest implicit casts.
     */
    private static Set<IOpenClass> findClosestCandidates(IOpenClass openClass1,
                                                         IOpenClass openClass2,
                                                         ICastFactory casts,
                                                         Iterable<IOpenMethod> methods) {
        Iterator<IOpenMethod> itr = methods.iterator();
        Set<IOpenClass> openClass1Candidates = new LinkedHashSet<>();
        addClassToCandidates(openClass1, openClass1Candidates);
        var openClass2Candidates = new LinkedHashSet<IOpenClass>();
        addClassToCandidates(openClass2, openClass2Candidates);
        while (itr.hasNext()) {
            var method = itr.next();
            if (method.getSignature().getNumberOfParameters() == 2) {
                checkAndAddToCandidates(method, openClass1, openClass1Candidates);
                checkAndAddToCandidates(method, openClass2, openClass2Candidates);
            }
        }
        openClass1Candidates.retainAll(openClass2Candidates);

        var bestDistance = Integer.MAX_VALUE;
        var closestClasses = new LinkedHashSet<IOpenClass>();
        for (IOpenClass to : openClass1Candidates) {
            var distance = getDistance(casts, openClass1, openClass2, to);

            if (distance > bestDistance) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                closestClasses.clear();
            }
            closestClasses.add(to);
        }

        return closestClasses;
    }

    /**
     * Returns the parent class of two classes. Returns the Object class when there is no parent class, or when the
     * classes are module specific types of different modules.
     */
    private static IOpenClass findParentClassOrObject(IOpenClass openClass1,
                                                      IOpenClass openClass2,
                                                      ICastFactory casts) {
        IOpenClass c;
        if (openClass1 instanceof ModuleSpecificType type && openClass2 instanceof ModuleSpecificType type1 && type
                .getModule() != type1.getModule()) {
            c = JavaOpenClass.OBJECT;
        } else {
            c = casts.findParentClass(openClass1, openClass2);
            if (c == null) {
                c = JavaOpenClass.OBJECT;
            }
        }
        return c;
    }

    @Override
    public IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
        return findParentClass1(openClass1, openClass2);
    }

    private static IOpenClass findParentClass1(IOpenClass openClass1, IOpenClass openClass2) {
        if (openClass1 == null) {
            throw new IllegalArgumentException("openClass1 cannot be null");
        }
        if (openClass2 == null) {
            throw new IllegalArgumentException("openClass2 cannot be null");
        }
        if (NullOpenClass.isAnyNull(openClass1)) {
            if (NullOpenClass.isAnyNull(openClass2)) {
                return NullOpenClass.the;
            }
            return returnWithPrimitiveLogic(openClass2);
        }
        if (NullOpenClass.isAnyNull(openClass2)) {
            if (NullOpenClass.isAnyNull(openClass1)) {
                return NullOpenClass.the;
            }
            return returnWithPrimitiveLogic(openClass1);
        }

        if (openClass1.getInstanceClass() != null && openClass2.getInstanceClass() != null
                && (openClass1.getInstanceClass().isPrimitive() && !openClass2.getInstanceClass()
                .isPrimitive() || !openClass1.getInstanceClass().isPrimitive() && openClass2.getInstanceClass()
                .isPrimitive())) {
            openClass1 = returnWithPrimitiveLogic(openClass1);
            openClass2 = returnWithPrimitiveLogic(openClass2);
        }

        if (openClass1.isArray() && openClass2.isArray()) {
            return findParentArrayClass(openClass1, openClass2);
        }
        return findParentNonArrayClass(openClass1, openClass2);
    }

    /**
     * Finds the parent class of two array types as an array of the parent class of their component types.
     */
    private static IOpenClass findParentArrayClass(IOpenClass openClass1, IOpenClass openClass2) {
        var dim = 0;
        while (openClass1.isArray() && openClass2.isArray()) {
            openClass1 = openClass1.getComponentClass();
            openClass2 = openClass2.getComponentClass();
            dim++;
        }
        IOpenClass parentClass = findParentClass1(openClass1, openClass2);
        if (parentClass == null) {
            return null;
        }
        return ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(parentClass, dim);
    }

    private static IOpenClass findParentNonArrayClass(IOpenClass openClass1, IOpenClass openClass2) {
        if (openClass1.getInstanceClass() == null && openClass2.getInstanceClass() == null) {
            return openClass1;
        }

        // If class1 is NULL literal
        if (openClass1.getInstanceClass() == null) {
            return findParentClassWithNullLiteral(openClass2);
        }

        // If class2 is NULL literal
        if (openClass2.getInstanceClass() == null) {
            return findParentClassWithNullLiteral(openClass1);
        }

        if (openClass1.getInstanceClass().isPrimitive() || openClass2.getInstanceClass().isPrimitive()) { // If
            // one
            // is
            // primitive
            if (openClass1.equals(openClass2)) {
                return openClass1;
            }
            return null;
        }

        if (openClass1 instanceof ModuleSpecificType type && openClass2 instanceof ModuleSpecificType type1) {
            var t = type.getClosestClass(type1);
            if (t != null) {
                return t;
            }
        }
        return findCommonSuperType(openClass1, openClass2);
    }

    /**
     * Returns the parent class of the NULL literal and the given class, or {@code null} for a primitive class.
     */
    private static IOpenClass findParentClassWithNullLiteral(IOpenClass openClass) {
        if (openClass.getInstanceClass().isPrimitive()) {
            return null;
        } else {
            return openClass;
        }
    }

    /**
     * Finds the closest common superclass of two classes, then the closest common interface. Falls back to the
     * Object class.
     */
    private static IOpenClass findCommonSuperType(IOpenClass openClass1, IOpenClass openClass2) {
        var superClasses = new HashSet<IOpenClass>();
        var interfaces = new LinkedHashSet<IOpenClass>();
        collectSuperClasses(openClass1, superClasses, interfaces);
        if (superClasses.contains(openClass2)) {
            return openClass2;
        }
        if (!(openClass2 instanceof JavaOpenClass)) {
            JavaOpenClass javaOpenClass2 = JavaOpenClass.getOpenClass(openClass2.getInstanceClass());
            if (superClasses.contains(javaOpenClass2)) {
                return javaOpenClass2;
            }
        }
        var superClass = findSuperClassAmong(openClass2, superClasses);
        if (superClass != null) {
            return superClass;
        }
        addSuperInterfaces(interfaces);
        return findCommonInterface(openClass2, interfaces);
    }

    /**
     * Collects the class, its superclasses and their Java classes. Collects the interfaces that the class and its
     * superclasses implement directly.
     */
    private static void collectSuperClasses(IOpenClass openClass1,
                                            Set<IOpenClass> superClasses,
                                            Set<IOpenClass> interfaces) {
        superClasses.add(openClass1);
        if (!(openClass1 instanceof JavaOpenClass)) {
            superClasses.add(JavaOpenClass.getOpenClass(openClass1.getInstanceClass()));
        }
        var openClass = openClass1;
        if (openClass.isInterface()) {
            interfaces.add(openClass);
        }
        while (openClass != null && !JavaOpenClass.OBJECT.equals(openClass)) {
            IOpenClass next = null;
            for (IOpenClass x : openClass.superClasses()) {
                if (!x.isInterface()) {
                    superClasses.add(x);
                    if (!(x instanceof JavaOpenClass)) {
                        superClasses.add(JavaOpenClass.getOpenClass(x.getInstanceClass()));
                    }
                    next = x;
                } else {
                    interfaces.add(x);
                }
            }
            openClass = next;
        }
    }

    /**
     * Walks up the superclasses of the class and returns the first one that is among the given classes. The Object
     * class is not returned.
     *
     * @return the found superclass or {@code null}
     */
    private static IOpenClass findSuperClassAmong(IOpenClass openClass2, Set<IOpenClass> superClasses) {
        var openClass = openClass2;
        while (openClass != null && !JavaOpenClass.OBJECT.equals(openClass)) {
            IOpenClass next = null;
            for (IOpenClass x : openClass.superClasses()) {
                if (!x.isInterface()) {
                    var superClass = matchSuperClass(x, superClasses);
                    if (superClass != null) {
                        return superClass;
                    }
                    next = x;
                }
            }
            openClass = next;
        }
        return null;
    }

    private static IOpenClass matchSuperClass(IOpenClass x, Set<IOpenClass> superClasses) {
        if (!JavaOpenClass.OBJECT.equals(x)) {
            if (superClasses.contains(x)) {
                return x;
            }
            if (!(x instanceof JavaOpenClass)) {
                JavaOpenClass y = JavaOpenClass.getOpenClass(x.getInstanceClass());
                if (superClasses.contains(x)) {
                    return y;
                }
            }
        }
        return null;
    }

    /**
     * Adds the super interfaces of the interfaces, level by level, skipping the ones ignorable in the parent search.
     */
    private static void addSuperInterfaces(Set<IOpenClass> interfaces) {
        Queue<IOpenClass> queue = new ArrayDeque<>(interfaces);
        while (!queue.isEmpty()) {
            var queue1 = new LinkedHashSet<IOpenClass>();
            for (IOpenClass oc : queue) {
                oc.superClasses()
                        .stream()
                        .filter(IOpenClass::isInterface)
                        .filter(isNotIgnorableInParentSearch)
                        .filter(e -> !interfaces.contains(e))
                        .forEach(e -> {
                            interfaces.add(e);
                            queue1.add(e);
                        });
            }
            queue = new ArrayDeque<>(queue1);
        }
    }

    /**
     * Searches the interfaces of the class level by level and returns the first one without type parameters that is
     * among the given interfaces. Falls back to the Object class.
     */
    private static IOpenClass findCommonInterface(IOpenClass openClass2, Set<IOpenClass> interfaces) {
        Queue<IOpenClass> queue = new ArrayDeque<>();
        if (openClass2.isInterface()) {
            queue.add(openClass2);
        }
        openClass2.superClasses()
                .stream()
                .filter(IOpenClass::isInterface)
                .filter(isNotIgnorableInParentSearch)
                .forEach(queue::add);
        while (!queue.isEmpty()) {
            var queue1 = new LinkedHashSet<IOpenClass>();
            for (IOpenClass oc : queue) {
                if (oc.getInstanceClass().getTypeParameters().length == 0 && interfaces.contains(oc)) {
                    return oc;
                }
                oc.superClasses()
                        .stream()
                        .filter(IOpenClass::isInterface)
                        .filter(isNotIgnorableInParentSearch)
                        .filter(e -> !interfaces.contains(e))
                        .forEach(queue1::add);
            }
            queue = new ArrayDeque<>(queue1);
        }
        return JavaOpenClass.OBJECT;
    }

    private static IOpenClass returnWithPrimitiveLogic(IOpenClass openClass2) {
        if (openClass2.getInstanceClass() != null && openClass2.getInstanceClass().isPrimitive()) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass2.getInstanceClass()));
        } else {
            return openClass2;
        }
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
        var notConvertible = new LinkedHashSet<IOpenClass>();
        for (IOpenClass openClass : openClassCandidates) {
            if (ret == null) {
                ret = openClass;
            } else {
                ret = chooseNarrower(castFactory, ret, openClass, notConvertible);
            }
        }

        if (!notConvertible.isEmpty()) {
            var newCandidates = new LinkedHashSet<IOpenClass>(notConvertible);
            newCandidates.add(ret);

            if (newCandidates.size() == openClassCandidates.size()) {
                // Cannot filter out classes to choose a closest. Prevent infinite recursion.
                var message = "Cannot find closest cast: " + "have several candidate classes not convertible between each over: " + Arrays
                        .toString(newCandidates.toArray());
                throw new IllegalStateException(message);
            }

            return chooseClosest(castFactory, newCandidates);
        }

        return ret;
    }

    /**
     * Chooses the narrower of the closest candidate so far and the next candidate.
     *
     * <p>When the candidates are not convertible between each other, the next candidate is added to the not
     * convertible ones and the closest candidate is kept.
     *
     * @throws IllegalStateException when both candidates convert to each other with the same distance
     */
    private static IOpenClass chooseNarrower(ICastFactory castFactory,
                                             IOpenClass closest,
                                             IOpenClass openClass,
                                             Set<IOpenClass> notConvertible) {
        var cast = castFactory.getCast(closest, openClass);
        if (cast == null || !cast.isImplicit()) {
            cast = castFactory.getCast(openClass, closest);
            if (cast != null && cast.isImplicit()) {
                // Found narrower candidate. For example Integer is narrower than Double (when convert from
                // int).
                return openClass;
            }
            // Two candidate classes are not convertible between each over. For example Float and
            // BigInteger.
            // Compare second candidate with remaining candidates later.
            notConvertible.add(openClass);
            return closest;
        }
        var backCast = castFactory.getCast(openClass, closest);
        if (backCast != null && backCast.isImplicit()) {
            var distance = cast.getDistance();
            var backDistance = backCast.getDistance();

            if (distance > backDistance) {
                // Assume that a cast to openClass is narrower than a cast to closest.
                return openClass;
            } else if (distance == backDistance) {
                // We have a collision.
                var message = "Cannot find closest cast: have two candidate classes with same cast distance: " + closest
                        .getName() + " and " + openClass.getName();
                throw new IllegalStateException(message);
            } else {
                // Previous candidate is narrower. Keep it.
            }
        } else {
            // Previous candidate is narrower. Keep it.
        }
        return closest;
    }

    private static int getDistance(ICastFactory casts, IOpenClass from1, IOpenClass from2, IOpenClass to) {
        var cast1 = casts.getCast(from1, to);
        var cast2 = casts.getCast(from2, to);

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
                var t = ClassUtils.wrapperToPrimitive(openClass.getInstanceClass());
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
     * @param to   to type
     * @return cast operation if it has been found; null - otherwise
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
                return JavaUnboxingNullCast.getInstance(to.getInstanceClass());
            } else {
                return JavaNoCast.getInstance();
            }
        }

        if (from.getInstanceClass() == null || to.getInstanceClass() == null) {
            return null;
        }

        if (VOID.class == from.getInstanceClass()) {
            return CastToAnyOpenCast.getInstance();
        }
        /* END: This is very cheap operations, so no needs to cache it */
        Object key = GenericKey.getInstance(from, to);
        var cast = castCache.get(key);
        if (cast == CastNotFound.getInstance()) {
            return null;
        }
        if (cast != null) {
            return cast;
        }

        var typeCast = findCast(from, to);
        if (typeCast == null) {
            typeCast = CastNotFound.getInstance();
        }

        var saved = castCache.putIfAbsent(key, typeCast);
        if (saved != null) {
            // Concurrent modification happens
            // Return saved instance
            typeCast = saved;
        }

        return typeCast == CastNotFound.getInstance() ? null : typeCast;
    }

    private IOpenCast findCast(IOpenClass from, IOpenClass to) {
        var typeCast = findArrayCast(from, to);
        if (typeCast != null) {
            return typeCast;
        }

        typeCast = findAliasCast(from, to);
        if (typeCast == null && from instanceof DomainOpenClass && to instanceof DomainOpenClass && from != to) {
            return findOneElementArrayCast(from, to);
        }

        var javaCast = findJavaCast(from, to);
        // Select minimum between alias cast and java cast
        typeCast = selectBetterCast(typeCast, javaCast);

        var methodBasedCast = findMethodBasedCast(from, to, methodFactory);
        typeCast = selectBetterCast(typeCast, methodBasedCast);

        typeCast = typeCast == null ? findOneElementArrayCast(from, to) : typeCast;

        typeCast = typeCast == null ? findArrayOneElementCast(from, to) : typeCast;

        return typeCast;
    }

    private IOpenCast findArrayOneElementCast(IOpenClass from, IOpenClass to) {
        if (from.isArray() && !to.isArray() && !from.getComponentClass().isArray()) {
            var cast = getCast(from.getComponentClass(), to);
            if (cast != null) {
                return new ArrayOneElementCast(to, cast);
            }
        }
        return null;
    }

    private static IOpenCast selectBetterCast(IOpenCast castA, IOpenCast castB) {
        if (castA == null && castB == null) {
            return null;
        }
        if (castA == null) {
            return castB;
        }
        if (castB == null) {
            return castA;
        }

        var distanceA = castA.getDistance();
        var distanceB = castB.getDistance();

        return distanceA > distanceB ? castB : castA;
    }

    private static IOpenCast getUpCast(Class<?> from, Class<?> to) {
        if (from == to) {
            return JavaNoCast.getInstance();
        }
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
        if (Object.class == fromClass) {
            // Special case for casting when:
            // Object from = new SomeType[x]
            // SomeType[] to = from
            return new JavaDownCast(to, this);
        }
        if (!from.isArray()) {
            return null;
        }

        var t = to.getComponentClass();
        var f = from.getComponentClass();
        if (!f.isArray() && t.isArray()) {
            // to prevent Obj[] -> Obj[][] because of findOneElementArrayCast
            return null;
        }
        var arrayElementCast = getCast(f, t);
        if (arrayElementCast != null && !(arrayElementCast instanceof IArrayOneElementCast) && !(arrayElementCast instanceof IOneElementArrayCast)) {
            return new ArrayCast(t, arrayElementCast);
        }
        return null;
    }

    private IOpenCast findOneElementArrayCast(IOpenClass from, IOpenClass to) {
        // if "from" is assignable from "to" then downcast is preferable
        if (to.isArray() && !from.isAssignableFrom(to)) {
            var dimFrom = OpenClassUtils.getDimension(from);
            var dimTo = OpenClassUtils.getDimension(to);
            if (dimTo - dimFrom == 1) {
                var componentClass = to.getComponentClass();
                var cast = getCast(from, componentClass);
                if (cast != null) {
                    return new OneElementArrayCast(componentClass, cast);
                }
            }
        }
        return null;
    }

    /**
     * Checks that instance class of open class is primitive.
     *
     * @param openClass type to check
     * @return <code>true</code> if instance class is primitive type; <code>false</code> - otherwise
     */
    private static boolean isPrimitive(IOpenClass openClass) {
        return openClass != null && openClass.getInstanceClass() != null && openClass.getInstanceClass().isPrimitive();
    }

    /**
     * Finds appropriate cast type operation using cast rules of java language. If result type is not java class
     * <code>null</code> will be returned.
     *
     * @param from from type
     * @param to   to type
     * @return cast operation if conversion is found; null - otherwise
     */
    private IOpenCast findJavaCast(IOpenClass from, IOpenClass to) {
        // Try to find cast using instance classes.
        //
        Class<?> fromClass = from.getInstanceClass();
        Class<?> toClass = to.getInstanceClass();

        if (fromClass == toClass && from != to && from instanceof ADynamicClass && to instanceof ADynamicClass) {
            // Dynamic classes with the same instance class
            if (to.isAssignableFrom(from)) {
                return getUpCast(fromClass, toClass);
            }
            return null;
        }

        if (ConstrainerObject.class.isAssignableFrom(fromClass)) {
            return null;
        }

        if (to.isAssignableFrom(from)) {
            return getUpCast(fromClass, toClass);
        }

        var typeCast = findBoxingCast(from, to);

        if (typeCast != null) {
            return typeCast;
        }

        typeCast = findUnBoxingCast(from, to);

        if (typeCast != null) {
            return typeCast;
        }

        if (isAllowJavaDownCast(from, to)) {
            return new JavaDownCast(to, this);
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
     * @param to   wrapper type
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
        if (void.class == fromClass && Void.class == toClass) {
            return JavaBoxingCast.getInstance();
        }

        return null;
    }

    /**
     * Finds appropriate unboxing (wrapper object to primitive) cast operation.
     *
     * @param from wrapper type
     * @param to   primitive type
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
        if (Void.class == fromClass && void.class == toClass) {
            return JavaUnboxingCast.getInstance(fromClass);
        }

        return null;
    }

    /**
     * Finds cast operation for alias types. If both types are not alias types <code>null</code> will be returned.
     *
     * @param from from type
     * @param to   to type
     * @return alias cast operation if conversion is found; null - otherwise
     */
    private IOpenCast findAliasCast(IOpenClass from, IOpenClass to) {
        if (!from.isArray() && !to.isArray() && (from instanceof DomainOpenClass || to instanceof DomainOpenClass)) {
            if (from instanceof DomainOpenClass fromDomainOpenClass && to instanceof DomainOpenClass toDomainOpenClass && from != to) {
                return findAliasToAliasCast(fromDomainOpenClass, toDomainOpenClass);
            }
            return findAliasTypeCast(from, to);
        }

        return null;
    }

    private IOpenCast findAliasToAliasCast(DomainOpenClass fromDomainOpenClass, DomainOpenClass toDomainOpenClass) {
        var openCast = getCast(fromDomainOpenClass.getBaseClass(), toDomainOpenClass.getBaseClass());
        if (openCast != null) {
            if (openCast.isImplicit() && DomainOpenClass
                    .isFromValuesIncludedToValues(fromDomainOpenClass, toDomainOpenClass, openCast)) {
                return new AliasToAliasOpenCast(openCast);
            }
            if (isFromValuesIntersectedWithToValues(fromDomainOpenClass, toDomainOpenClass, openCast)) {
                return new AliasToAliasOpenCast(openCast, false);
            }
        }
        return null;
    }

    /**
     * Finds a cast between an alias type and a type that is not a distinct alias type: to or from the base type of
     * the alias, or through the Java class of the alias.
     */
    private IOpenCast findAliasTypeCast(IOpenClass from, IOpenClass to) {
        if (from instanceof DomainOpenClass class1 && !(to instanceof DomainOpenClass) && to
                .equals(class1.getBaseClass())) {
            return AliasToTypeCast.getInstance();
        }

        if (!(from instanceof DomainOpenClass) && to instanceof DomainOpenClass class1 && from
                .equals(class1.getBaseClass())) {
            return new TypeToAliasCast(to);
        }

        if (from instanceof DomainOpenClass && to.getInstanceClass().isAssignableFrom(from.getClass())) { // This is
            // not
            // typo
            return JavaUpCast.getInstance();
        }

        if (from instanceof DomainOpenClass && !(to instanceof DomainOpenClass)) {
            var openCast = this.findCast(JavaOpenClass.getOpenClass(from.getInstanceClass()), to);
            if (openCast != null) {
                return new AliasToTypeCast(openCast);
            }
        }

        if (to instanceof DomainOpenClass && !(from instanceof DomainOpenClass)) {
            var openCast = this.findCast(from, JavaOpenClass.getOpenClass(to.getInstanceClass()));
            if (openCast != null) {
                return new TypeToAliasCast(to, openCast);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static boolean isFromValuesIntersectedWithToValues(DomainOpenClass from,
                                                               DomainOpenClass to,
                                                               IOpenCast openCast) {
        var fromDomain = (IDomain<Object>) from.getDomain();
        var toDomain = (IDomain<Object>) to.getDomain();
        try {
            for (Object value : fromDomain) {
                if (toDomain.selectObject(openCast.convert(value))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Finds cast operation using {@link IMethodFactory} object.
     *
     * @param from          from type
     * @param to            to type
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
     * @param from          from type
     * @param to            to type
     * @param methodFactory {@link IMethodFactory} object
     * @return cast operation
     */
    private static IOpenCast findMethodCast(IOpenClass from, IOpenClass to, IMethodFactory methodFactory) {

        if (methodFactory == null) {
            return null;
        }

        // Is auto cast ?
        var auto = true;
        var distance = getAutoCastDistance(from, to);

        // Matching method
        var lookup = new CastMethodLookup(methodFactory, from, to);

        try {
            // Try to find matching auto cast method
            lookup.find(AUTO_CAST_METHOD_NAME);
            lookup.findByPrimitiveTypes(AUTO_CAST_METHOD_NAME);
        } catch (AmbiguousMethodException e) {
            log.debug(IGNORED_ERROR, e);
        }

        // If appropriate auto cast method is not found try to find explicit
        // cast method.
        //
        if (lookup.castCaller == null) {
            auto = false;
            try {
                lookup.find(CAST_METHOD_NAME);
                distance = getCastDistance(from, to);
                lookup.findByPrimitiveTypes(CAST_METHOD_NAME);
            } catch (AmbiguousMethodException e) {
                log.debug(IGNORED_ERROR, e);
            }
        }

        if (lookup.castCaller == null) {
            return null;
        }

        IMethodCaller distanceCaller = null;

        try {
            distanceCaller = methodFactory.getMethod(DISTANCE_METHOD_NAME,
                    new IOpenClass[]{lookup.fromOpenClass, lookup.toOpenClass});
        } catch (AmbiguousMethodException e) {
            log.debug(IGNORED_ERROR, e);
        }

        if (distanceCaller != null) {
            distance = (Integer) distanceCaller.invoke(null,
                    new Object[]{lookup.fromOpenClass.nullObject(), lookup.toOpenClass.nullObject()},
                    null);
        }

        return new MethodBasedCast(lookup.castCaller, auto, distance, to, lookup.toOpenClass.nullObject());
    }

    private static int getAutoCastDistance(IOpenClass from, IOpenClass to) {
        if (from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
            return PRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE;
        } else if (!from.getInstanceClass().isPrimitive() && to.getInstanceClass().isPrimitive()) {
            return NONPRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE;
        } else if (!from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
            return NONPRIMITIVE_TO_NONPRIMITIVE_AUTOCAST_DISTANCE;
        } else {
            return PRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE;
        }
    }

    private static int getCastDistance(IOpenClass from, IOpenClass to) {
        if (from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
            return PRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE;
        } else if (!from.getInstanceClass().isPrimitive() && to.getInstanceClass().isPrimitive()) {
            return NONPRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE;
        } else if (!from.getInstanceClass().isPrimitive() && !to.getInstanceClass().isPrimitive()) {
            return NONPRIMITIVE_TO_NONPRIMITIVE_CAST_DISTANCE;
        } else {
            return PRIMITIVE_TO_PRIMITIVE_CAST_DISTANCE;
        }
    }

    /**
     * Looks up a cast method of the method factory by name.
     *
     * <p>It remembers the found method and the parameter types it was last looked up with. The types of a lookup
     * are remembered even when it fails with {@link AmbiguousMethodException}.
     */
    private static final class CastMethodLookup {
        private final IMethodFactory methodFactory;
        private final IOpenClass from;
        private final IOpenClass to;
        private final Class<?> primitiveClassFrom;
        private final Class<?> primitiveClassTo;
        private IMethodCaller castCaller;
        private IOpenClass fromOpenClass;
        private IOpenClass toOpenClass;

        private CastMethodLookup(IMethodFactory methodFactory, IOpenClass from, IOpenClass to) {
            this.methodFactory = methodFactory;
            this.from = from;
            this.to = to;
            this.fromOpenClass = from;
            this.toOpenClass = to;
            this.primitiveClassFrom = ClassUtils.wrapperToPrimitive(from.getInstanceClass());
            this.primitiveClassTo = ClassUtils.wrapperToPrimitive(to.getInstanceClass());
        }

        private void find(String methodName) {
            castCaller = methodFactory.getMethod(methodName, new IOpenClass[]{from, to});
        }

        /**
         * Looks up the method with the primitive types of the wrapper types while no method is found.
         */
        private void findByPrimitiveTypes(String methodName) {
            // If from parameter is wrapper for primitive type try to find
            // cast method using 'from' as primitive type. In this case
            // we are emulate 2 operations: 1) unboxing operation 2)
            // cast operation.
            // For example:
            // <code>
            // Integer a = 1
            // double d = a
            // </code>
            // For OpenL we are omitting the check that 'to' type must be
            // primitive type for our case to simplify understanding type
            // operations in
            // engine by end-user.
            //
            if (castCaller == null && primitiveClassFrom != null) {
                IOpenClass openClassFrom = JavaOpenClass.getOpenClass(primitiveClassFrom);
                fromOpenClass = openClassFrom;
                toOpenClass = to;
                castCaller = methodFactory.getMethod(methodName, new IOpenClass[]{openClassFrom, to});
            }

            // If to parameter is wrapper for primitive type try to find
            // cast method using 'to' as primitive type. In this case
            // we are emulate 2 operations: 1) cast operation,
            // 2) boxing operation.
            // For example:
            // <code>
            // int a = 1
            // Double d = a
            // </code>
            // For OpenL we are omitting the check that 'from' type must be
            // primitive type for our case to simplify understanding type
            // operations in
            // engine by end-user.
            //
            if (castCaller == null && primitiveClassTo != null) {
                IOpenClass openClassTo = JavaOpenClass.getOpenClass(primitiveClassTo);
                castCaller = methodFactory.getMethod(methodName, new IOpenClass[]{from, openClassTo});
                fromOpenClass = from;
                toOpenClass = openClassTo;
            }

            if (castCaller == null && primitiveClassFrom != null && primitiveClassTo != null) {
                IOpenClass openClassFrom = JavaOpenClass.getOpenClass(primitiveClassFrom);
                IOpenClass openClassTo = JavaOpenClass.getOpenClass(primitiveClassTo);
                fromOpenClass = openClassFrom;
                toOpenClass = openClassTo;
                castCaller = methodFactory.getMethod(methodName, new IOpenClass[]{openClassFrom, openClassTo});
            }
        }
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
     * @param to   to type
     * @return <code>true</code> is downcast operation is allowed for given types; <code>false</code> - otherwise
     * @link http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc .html
     */
    private static boolean isAllowJavaDownCast(IOpenClass from, IOpenClass to) {

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
}
