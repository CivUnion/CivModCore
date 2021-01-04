package vg.civcraft.mc.civmodcore.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Utility class that fills in the gaps of {@link CollectionUtils}.
 *
 * @author Protonull
 */
public final class MoreCollectionUtils {

	/**
	 * Creates a new collection with a given set of predefined elements, if any are given.
	 *
	 * @param <T> The type of the elements to store in the collection.
	 * @param constructor The constructor for the collection.
	 * @param elements The elements to add to the collection.
	 * @return Returns a new collection, or null if no constructor was given, or the constructor didn't produce a new
	 * collection.
	 */
	@SafeVarargs
	public static <T, K extends Collection<T>> K collect(final Supplier<K> constructor, final T... elements) {
		final K collection = Chainer.from(constructor).then(Supplier::get).get();
		if (collection == null) {
			return null;
		}
		CollectionUtils.addAll(collection, elements);
		return collection;
	}

    /**
     * <p>Tests whether there is at least one element in the given collection that passes the criteria of the given
     * predicate.</p>
     *
     * <p>Emulates: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/some</p>
     *
     * @param <T> The type of the collection's elements.
     * @param collection The collection to iterate.
     * @param predicate The element tester.
     * @return Returns true if at least one element passes the predicate test. Or false if the array fails the
     * {@link ArrayUtils#isEmpty(Object[]) isNullOrEmpty()} test, or true if the give predicate is null.
     */
    public static <T> boolean anyMatch(final Collection<T> collection, final Predicate<T> predicate) {
        if (CollectionUtils.isEmpty(collection)) {
            return false;
        }
        if (predicate == null) {
            return true;
        }
        for (final T element : collection) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Tests whether every element in an collection passes the criteria of the given predicate.</p>
     *
     * <p>Emulates: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/every</p>
     *
     * @param <T> The type of the collection's elements.
     * @param collection The collection to iterate.
     * @param predicate The element tester.
     * @return Returns true if no element fails the predicate test, or if the array fails the
     * {@link ArrayUtils#isEmpty(Object[]) isNullOrEmpty()} test, or if the give predicate is null.
     */
    public static <T> boolean allMatch(final Collection<T> collection, final Predicate<T> predicate) {
        if (CollectionUtils.isEmpty(collection)) {
            return true;
        }
        if (predicate == null) {
            return true;
        }
        for (final T element : collection) {
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes the element at the end of the given list.
     *
     * @param <T> The type of the list's elements.
     * @param list The list to remove the last element from.
     * @return Returns the element removed.
     */
    public static <T> T removeLastElement(final List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.remove(list.size() - 1);
    }

    /**
     * Retrieves a random element from an list of elements.
     *
     * @param <T> The type of element.
     * @param list The list to retrieve a value from.
     * @return Returns a random element, or null.
     */
    public static <T> T randomElement(final List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        final int size = list.size();
        if (size == 1) {
            return list.get(0);
        }
        return list.get(ThreadLocalRandom.current().nextInt(size));
    }

}
