package org.openl.conf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.rules.binding.TableProperties;
import org.openl.rules.calc.AnySpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.enumeration.CaProvincesEnum;
import org.openl.rules.enumeration.CaRegionsEnum;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.DTEmptyResultProcessingEnum;
import org.openl.rules.enumeration.LanguagesEnum;
import org.openl.rules.enumeration.OriginsEnum;
import org.openl.rules.enumeration.RecalculateEnum;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.StringRange;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Resolves Java types to OpenClass types by a simple or canonical class name.
 *
 * @author Yury Molchan
 */
public class TypeResolver implements INameSpacedTypeFactory {

    private static final HashMap<String, IOpenClass> CORE_CLASSES = new HashMap<>();

    static {
        // For backward compatibility with old types in rules
        CORE_CLASSES.put("BigDecimalValue", JavaOpenClass.getOpenClass(BigDecimal.class));
        CORE_CLASSES.put("DoubleValue", JavaOpenClass.getOpenClass(Double.class));
        CORE_CLASSES.put("DoubleValuePercent", JavaOpenClass.getOpenClass(Double.class));
        CORE_CLASSES.put("FloatValue", JavaOpenClass.getOpenClass(Float.class));
        CORE_CLASSES.put("BigIntegerValue", JavaOpenClass.getOpenClass(BigInteger.class));
        CORE_CLASSES.put("LongValue", JavaOpenClass.getOpenClass(Long.class));
        CORE_CLASSES.put("IntValue", JavaOpenClass.getOpenClass(Integer.class));
        CORE_CLASSES.put("ShortValue", JavaOpenClass.getOpenClass(Short.class));
        CORE_CLASSES.put("ByteValue", JavaOpenClass.getOpenClass(Byte.class));
        CORE_CLASSES.put("StringValue", JavaOpenClass.getOpenClass(String.class));
        CORE_CLASSES.put("ObjectValue", JavaOpenClass.getOpenClass(Object.class));

        CORE_CLASSES.put("org.openl.meta.BigDecimalValue", JavaOpenClass.getOpenClass(BigDecimal.class));
        CORE_CLASSES.put("org.openl.meta.DoubleValue", JavaOpenClass.getOpenClass(Double.class));
        CORE_CLASSES.put("org.openl.meta.DoubleValuePercent", JavaOpenClass.getOpenClass(Double.class));
        CORE_CLASSES.put("org.openl.meta.FloatValue", JavaOpenClass.getOpenClass(Float.class));
        CORE_CLASSES.put("org.openl.meta.BigIntegerValue", JavaOpenClass.getOpenClass(BigInteger.class));
        CORE_CLASSES.put("org.openl.meta.LongValue", JavaOpenClass.getOpenClass(Long.class));
        CORE_CLASSES.put("org.openl.meta.IntValue", JavaOpenClass.getOpenClass(Integer.class));
        CORE_CLASSES.put("org.openl.meta.ShortValue", JavaOpenClass.getOpenClass(Short.class));
        CORE_CLASSES.put("org.openl.meta.ByteValue", JavaOpenClass.getOpenClass(Byte.class));
        CORE_CLASSES.put("org.openl.meta.StringValue", JavaOpenClass.getOpenClass(String.class));
        CORE_CLASSES.put("org.openl.meta.ObjectValue", JavaOpenClass.getOpenClass(Object.class));

        // Primitives
        putClass(int.class);
        putClass(long.class);
        putClass(char.class);
        putClass(short.class);
        putClass(byte.class);
        putClass(double.class);
        putClass(float.class);
        putClass(boolean.class);
        putClass(void.class);

        // java.lang: Standard final types
        putClass(Integer.class);
        putClass(Long.class);
        putClass(Character.class);
        putClass(Short.class);
        putClass(Byte.class);
        putClass(Double.class);
        putClass(Float.class);
        putClass(Boolean.class);
        putClass(Void.class);
        putClass(String.class);

        // java.lang: Base classes
        putClass(Number.class);
        putClass(Object.class);
        putClass(Record.class);
        putClass(Class.class);
        putClass(Enum.class);

        // java.lang: Exceptions
        putClass(AbstractMethodError.class);
        putClass(ArithmeticException.class);
        putClass(ArrayIndexOutOfBoundsException.class);
        putClass(ArrayStoreException.class);
        putClass(AssertionError.class);
        putClass(BootstrapMethodError.class);
        putClass(ClassCastException.class);
        putClass(ClassCircularityError.class);
        putClass(ClassFormatError.class);
        putClass(ClassNotFoundException.class);
        putClass(CloneNotSupportedException.class);
        putClass(EnumConstantNotPresentException.class);
        putClass(Error.class);
        putClass(Exception.class);
        putClass(ExceptionInInitializerError.class);
        putClass(IllegalAccessError.class);
        putClass(IllegalAccessException.class);
        putClass(IllegalArgumentException.class);
        putClass(IllegalCallerException.class);
        putClass(IllegalMonitorStateException.class);
        putClass(IllegalStateException.class);
        putClass(IllegalThreadStateException.class);
        putClass(IncompatibleClassChangeError.class);
        putClass(IndexOutOfBoundsException.class);
        putClass(InstantiationError.class);
        putClass(InstantiationException.class);
        putClass(InternalError.class);
        putClass(InterruptedException.class);
        putClass(LayerInstantiationException.class);
        putClass(LinkageError.class);
        putClass(MatchException.class);
        putClass(NegativeArraySizeException.class);
        putClass(NoClassDefFoundError.class);
        putClass(NoSuchFieldError.class);
        putClass(NoSuchFieldException.class);
        putClass(NoSuchMethodError.class);
        putClass(NoSuchMethodException.class);
        putClass(NullPointerException.class);
        putClass(NumberFormatException.class);
        putClass(OutOfMemoryError.class);
        putClass(ReflectiveOperationException.class);
        putClass(RuntimeException.class);
        putClass(SecurityException.class);
        putClass(StackOverflowError.class);
        putClass(StringIndexOutOfBoundsException.class);
        putClass(Throwable.class);
        putClass(TypeNotPresentException.class);
        putClass(UnknownError.class);
        putClass(UnsatisfiedLinkError.class);
        putClass(UnsupportedClassVersionError.class);
        putClass(UnsupportedOperationException.class);
        putClass(VerifyError.class);
        putClass(VirtualMachineError.class);
        putClass(WrongThreadException.class);

        // java.lang: Annotations
        putClass(Deprecated.class);
        putClass(FunctionalInterface.class);
        putClass(Override.class);
        putClass(SafeVarargs.class);
        putClass(SuppressWarnings.class);

        // java.lang: Interfaces
        putClass(Appendable.class);
        putClass(AutoCloseable.class);
        putClass(CharSequence.class);
        putClass(Cloneable.class);
        putClass(Comparable.class);
        putClass(Iterable.class);
        putClass(ProcessHandle.class);
        putClass(Readable.class);
        putClass(Runnable.class);

        // java.lang
        putClass(ClassLoader.class);
        putClass(ClassValue.class);
        putClass(InheritableThreadLocal.class);
        putClass(Math.class);
        putClass(Module.class);
        putClass(ModuleLayer.class);
        putClass(Package.class);
        putClass(Process.class);
        putClass(ProcessBuilder.class);
        putClass(Runtime.class);
        putClass(RuntimePermission.class);
        putClass(StackTraceElement.class);
        putClass(StackWalker.class);
        putClass(StrictMath.class);
        putClass(StringBuffer.class);
        putClass(StringBuilder.class);
        putClass(System.class);
        putClass(Thread.class);
        putClass(ThreadGroup.class);
        putClass(ThreadLocal.class);

        // java.util
        putClass(AbstractCollection.class);
        putClass(AbstractList.class);
        putClass(AbstractMap.class);
        putClass(AbstractQueue.class);
        putClass(AbstractSequentialList.class);
        putClass(AbstractSet.class);
        putClass(ArrayDeque.class);
        putClass(ArrayList.class);
        putClass(Arrays.class);
        putClass(Base64.class);
        putClass(BitSet.class);
        putClass(Calendar.class);
        putClass(Collection.class);
        putClass(Collections.class);
        putClass(Comparator.class);
        putClass(ConcurrentModificationException.class);
        putClass(Currency.class);
        putClass(Date.class);
        putClass(Deque.class);
        putClass(Dictionary.class);
        putClass(DoubleSummaryStatistics.class);
        putClass(DuplicateFormatFlagsException.class);
        putClass(EmptyStackException.class);
        putClass(Enumeration.class);
        putClass(EnumMap.class);
        putClass(EnumSet.class);
        putClass(EventListener.class);
        putClass(EventListenerProxy.class);
        putClass(EventObject.class);
        putClass(FormatFlagsConversionMismatchException.class);
        putClass(Formattable.class);
        putClass(FormattableFlags.class);
        putClass(Formatter.class);
        putClass(FormatterClosedException.class);
        putClass(GregorianCalendar.class);
        putClass(HashMap.class);
        putClass(HashSet.class);
        putClass(Hashtable.class);
        putClass(IdentityHashMap.class);
        putClass(IllegalFormatCodePointException.class);
        putClass(IllegalFormatConversionException.class);
        putClass(IllegalFormatException.class);
        putClass(IllegalFormatFlagsException.class);
        putClass(IllegalFormatPrecisionException.class);
        putClass(IllegalFormatWidthException.class);
        putClass(IllformedLocaleException.class);
        putClass(InputMismatchException.class);
        putClass(IntSummaryStatistics.class);
        putClass(InvalidPropertiesFormatException.class);
        putClass(Iterator.class);
        putClass(LinkedHashMap.class);
        putClass(LinkedHashSet.class);
        putClass(LinkedList.class);
        putClass(List.class);
        putClass(ListIterator.class);
        putClass(ListResourceBundle.class);
        putClass(Locale.class);
        putClass(LongSummaryStatistics.class);
        putClass(Map.class);
        putClass(MissingFormatArgumentException.class);
        putClass(MissingFormatWidthException.class);
        putClass(MissingResourceException.class);
        putClass(NavigableMap.class);
        putClass(NavigableSet.class);
        putClass(NoSuchElementException.class);
        putClass(Objects.class);
        putClass(Optional.class);
        putClass(OptionalDouble.class);
        putClass(OptionalInt.class);
        putClass(OptionalLong.class);
        putClass(PrimitiveIterator.class);
        putClass(PriorityQueue.class);
        putClass(Properties.class);
        putClass(PropertyPermission.class);
        putClass(PropertyResourceBundle.class);
        putClass(Queue.class);
        putClass(Random.class);
        putClass(RandomAccess.class);
        putClass(ResourceBundle.class);
        putClass(Scanner.class);
        putClass(ServiceConfigurationError.class);
        putClass(ServiceLoader.class);
        putClass(Set.class);
        putClass(SimpleTimeZone.class);
        putClass(SortedMap.class);
        putClass(SortedSet.class);
        putClass(Spliterator.class);
        putClass(Spliterators.class);
        putClass(SplittableRandom.class);
        putClass(Stack.class);
        putClass(StringJoiner.class);
        putClass(StringTokenizer.class);
        putClass(Timer.class);
        putClass(TimerTask.class);
        putClass(TimeZone.class);
        putClass(TooManyListenersException.class);
        putClass(TreeMap.class);
        putClass(TreeSet.class);
        putClass(UnknownFormatConversionException.class);
        putClass(UnknownFormatFlagsException.class);
        putClass(UUID.class);
        putClass(Vector.class);
        putClass(WeakHashMap.class);

        //java.math
        putClass(BigDecimal.class);
        putClass(BigInteger.class);
        putClass(MathContext.class);
        putClass(RoundingMode.class);

        // OpenL types
        putClass(CharRange.class);
        putClass(DateRange.class);
        putClass(IntRange.class);
        putClass(StringRange.class);
        putClass(DoubleRange.class);

        putClass(SpreadsheetResult.class);
        putClass(AnySpreadsheetResult.class);
        putClass(TableProperties.class);

        // OpenL generated enums
        putClass(CaProvincesEnum.class);
        putClass(CaRegionsEnum.class);
        putClass(CountriesEnum.class);
        putClass(CurrenciesEnum.class);
        putClass(LanguagesEnum.class);
        putClass(RegionsEnum.class);
        putClass(OriginsEnum.class);
        putClass(UsRegionsEnum.class);
        putClass(UsStatesEnum.class);
        putClass(DTEmptyResultProcessingEnum.class);
        putClass(RecalculateEnum.class);
        putClass(ValidateDTEnum.class);
    }

    private final ClassLoader classLoader;

    private final ConcurrentHashMap<String, IOpenClass> aliases = new ConcurrentHashMap<>();

    private final Collection<String> packages;

    public TypeResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
        packages = Collections.emptyList();
    }

    public TypeResolver(ClassLoader classLoader, Collection<Class<?>> classes, Collection<String> packages) {
        this.classLoader = classLoader;
        if (!classes.isEmpty()) {
            for (var cls : classes) {
                String alias = cls.getSimpleName();
                this.aliases.put(alias, JavaOpenClass.getOpenClass(cls));
            }
        }
        this.packages = packages;
    }

    private static void putClass(Class<?> clazz) {
        CORE_CLASSES.put(clazz.getSimpleName(), JavaOpenClass.getOpenClass(clazz));
        CORE_CLASSES.put(clazz.getCanonicalName(), JavaOpenClass.getOpenClass(clazz));
    }

    private final ConcurrentHashMap<String, IOpenClass> found = new ConcurrentHashMap<>();

    public IOpenClass getType(String name) throws AmbiguousTypeException {
        Set<IOpenClass> foundTypes = new HashSet<>();

        var cls = CORE_CLASSES.get(name);
        if (cls == null) {
            // If a type absent in the core classes, try to find in Java class loaders.
            cls = found.get(name);
            if (cls == null) {
                cls = loadClass(classLoader, name);
                if (cls == null) {
                    cls = NullOpenClass.the;
                }
                found.put(name, cls);
            }
        }
        if (cls != NullOpenClass.the) {
            foundTypes.add(cls);
        }

        var cls2 = aliases.get(name);
        if (cls2 == null) {
            for (var pckg : packages) {
                cls2 = loadClass(classLoader, pckg + "." + name);
                if (cls2 != null) {
                    break;
                }
            }
            if (cls2 == null) {
                cls2 = NullOpenClass.the;
            }
            aliases.put(name, cls2);
        }

        if (cls2 != NullOpenClass.the) {
            foundTypes.add(cls2);
        }

        switch (foundTypes.size()) {
            case 0:
                return null;
            case 1:
                return foundTypes.iterator().next();
            default:
                throw new AmbiguousTypeException(name, new ArrayList<>(foundTypes));
        }
    }

    private static IOpenClass loadClass(ClassLoader classLoader, String fullName) {
        // TODO add security ability to block access to system classes.
        try {
            Class<?> c = classLoader.loadClass(fullName);
            return JavaOpenClass.getOpenClass(c);
        } catch (ClassNotFoundException ignored) {
            // Type is not found in the package. Search in another.
        } catch (NoClassDefFoundError e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                // Type is found but cannot be loaded because of absent dependent class.
                String noClassMessage = e.getCause().getMessage();
                String message = String
                        .format("Cannot load type '%s' because of absent type '%s'.", fullName, noClassMessage);
                throw RuntimeExceptionWrapper.wrap(message, e);
            }
            // NoClassDefFoundError can also be thrown in these cases:
            // 1. Class was compiled in one package but it was moved manually to another package in file system
            // without changing package in class binary
            // 2. Class was compiled with one name but was manually renamed in file system to another name
            // 3. If File System is case insensitive and we are trying to find the class org.work.address but
            // exists
            // the class org.work.Address
            // In all these cases NoClassDefFoundError will be thrown instead of ClassNotFoundException and
            // message
            // will be like:
            // java.lang.NoClassDefFoundError: org/work/address (wrong name: org/work/Address)
            // We just skip such classes and continue searching them in another packages.
        } catch (UnsupportedClassVersionError e) {
            // Type is found but it's compiled using newer version of JDK
            String message = String.format(
                    "Cannot load the class '%s' that was compiled using newer version of JDK than current JRE (%s)",
                    fullName,
                    System.getProperty("java.version"));
            throw RuntimeExceptionWrapper.wrap(message, e);
        } catch (Exception | LinkageError e) {
            throw RuntimeExceptionWrapper.wrap("Cannot load type: " + fullName, e);
        }
        return null;
    }
}
