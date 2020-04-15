/*
 * Copyright TROVE4J
 *
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 */
package org.predict4all.nlp.ngram.trie.map;

import gnu.trove.function.TObjectFunction;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.PrimeFinder;
import gnu.trove.impl.hash.THash;
import gnu.trove.impl.hash.TIntHash;
import gnu.trove.impl.hash.TPrimitiveHash;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectProcedure;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Custom implementation copied from {@link TIntObjectHashMap} but with less attribute to reduce the heap size in Trie.<br>
 * Source is copied from class hierarchy (with manually merging methods):
 * <ul>
 * <li>{@link THash}</li>
 * <li>{@link TPrimitiveHash}</li>
 * <li>{@link TIntHash}</li>
 * <li>{@link TIntObjectHashMap}</li>
 * </ul>
 * The implementation is modified to keep the minimum attribute count on this Map because this TrieNodeMap will be created a lot of time !
 * <a href="https://bitbucket.org/trove4j/trove/src/master/">Original source code</a>
 *
 * @author Mathieu THEBAUD
 */
public class TrieNodeMap<V> {

    /**
     * flag indicating that a slot in the hashtable is available
     */
    public static final byte FREE = 0;

    /**
     * flag indicating that a slot in the hashtable is occupied
     */
    public static final byte FULL = 1;

    /**
     * flag indicating that the value of a slot in the hashtable was deleted
     */
    public static final byte REMOVED = 2;

    /**
     * the current number of occupied slots in the hash.
     */
    protected transient int _size;

    /**
     * the current number of free slots in the hash.
     */
    protected transient int _free;

    /**
     * the set of ints
     */
    public transient int[] _set;

    protected boolean consumeFreeSlot;

    /**
     * The maximum number of elements allowed without allocating more
     * space.
     */
    protected int _maxSize;

    /**
     * flags indicating whether each position in the hash is
     * FREE, FULL, or REMOVED
     */
    public transient byte[] _states;

    /* constants used for state flags */

    /**
     * the values of the map
     */
    protected transient V[] _values;

    /**
     * Creates a new <code>THash</code> instance with a prime capacity
     * at or near the minimum needed to hold <code>initialCapacity</code>
     * elements with load factor <code>loadFactor</code> without triggering
     * a rehash.
     */
    public TrieNodeMap() {
        setUp(HashFunctions.fastCeil(TrieNodeMapConstant.INITIAL_CAPACITY / TrieNodeMapConstant.LOAD_FACTOR));
        setUp(HashFunctions.fastCeil(TrieNodeMapConstant.INITIAL_CAPACITY / TrieNodeMapConstant.LOAD_FACTOR));
        Arrays.fill(_set, TrieNodeMapConstant.NO_ENTRY_VALUE);
    }

    /**
     * Tells whether this set is currently holding any elements.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEmpty() {
        return 0 == _size;
    }

    /**
     * Returns the number of distinct elements in this collection.
     *
     * @return an <code>int</code> value
     */
    public int size() {
        return _size;
    }

    /**
     * Compresses the hashtable to the minimum prime size (as defined
     * by PrimeFinder) that will hold all of the elements currently in
     * the table. If you have done a lot of <code>remove</code>
     * operations and plan to do a lot of queries or insertions or
     * iteration, it is a good idea to invoke this method. Doing so
     * will accomplish two things:
     * <br>
     * <ol>
     * <li>You'll free memory allocated to the table but no
     * longer needed because of the remove()s.</li>
     * <li>You'll get better query/insert/iterator performance
     * because there won't be any <code>REMOVED</code> slots to skip
     * over when probing for indices in the table.</li>
     * </ol>
     */
    public void compact() {
        // P4A : added compact check : compact the map only if needed
        if (_maxSize > _size + 1) {
            // need at least one free spot for open addressing
            rehash(PrimeFinder.nextPrime(Math.max(_size + 1, HashFunctions.fastCeil(size() / TrieNodeMapConstant.LOAD_FACTOR) + 1)));
            computeMaxSize(capacity());
        }
    }

    /**
     * This simply calls {@link #compact compact}. It is included for
     * symmetry with other collection classes. Note that the name of this
     * method is somewhat misleading (which is why we prefer
     * <code>compact</code>) as the load factor may require capacity above
     * and beyond the size of this collection.
     *
     * @see #compact
     */
    public final void trimToSize() {
        compact();
    }

    /**
     * Delete the record at <code>index</code>. Reduces the size of the
     * collection by one.
     *
     * @param index an <code>int</code> value
     */
    protected void removeAt(int index) {
        _values[index] = null;
        _set[index] = TrieNodeMapConstant.NO_ENTRY_VALUE;
        _states[index] = REMOVED;
        _size--;
    }

    /**
     * Empties the collection.
     */
    public void clear() {
        _size = 0;
        _free = capacity();
        Arrays.fill(_set, 0, _set.length, TrieNodeMapConstant.NO_ENTRY_VALUE);
        Arrays.fill(_states, 0, _states.length, FREE);
        Arrays.fill(_values, 0, _values.length, null);
    }

    /**
     * initializes the hashtable to a prime capacity which is at least
     * <code>initialCapacity + 1</code>.
     *
     * @param initialCapacity an <code>int</code> value
     * @return the actual capacity chosen
     */
    @SuppressWarnings({"unchecked"})
    protected int setUp(int initialCapacity) {
        final int capacity = PrimeFinder.nextPrime(initialCapacity);
        computeMaxSize(capacity);
        _states = new byte[capacity];
        _set = new int[capacity];
        _values = (V[]) new Object[capacity];
        return capacity;
    }

    /**
     * Computes the values of maxSize. There will always be at least
     * one free slot required.
     *
     * @param capacity an <code>int</code> value
     */
    protected void computeMaxSize(int capacity) {
        // need at least one free slot for open addressing
        _free = capacity - _size; // reset the free element count
        _maxSize = Math.min(capacity - 1, (int) (capacity * TrieNodeMapConstant.LOAD_FACTOR));
    }

    /**
     * After an insert, this hook is called to adjust the size/free
     * values of the set and to perform rehashing if necessary.
     *
     * @param usedFreeSlot the slot
     */
    protected final void postInsertHook(boolean usedFreeSlot) {
        if (usedFreeSlot) {
            _free--;
        }

        // rehash whenever we exhaust the available space in the table
        if (++_size > _maxSize || _free == 0) {
            // choose a new capacity suited to the new state of the table
            // if we've grown beyond our maximum size, double capacity;
            // if we've exhausted the free spots, rehash to the same capacity,
            // which will free up any stale removed slots for reuse.
            int newCapacity = _size > _maxSize ? PrimeFinder.nextPrime(capacity() << 1) : capacity();
            rehash(newCapacity);
            computeMaxSize(capacity());
        }
    }

    protected int calculateGrownCapacity() {
        return capacity() << 1;
    }

    /**
     * Returns the capacity of the hash table. This is the true
     * physical capacity, without adjusting for the load factor.
     *
     * @return the physical capacity of the hash table.
     */
    public int capacity() {
        return _states.length;
    }

    /**
     * Searches the set for <code>val</code>
     *
     * @param val an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    public boolean contains(int val) {
        return index(val) >= 0;
    }

    /**
     * Executes <code>procedure</code> for each element in the set.
     *
     * @param procedure a <code>TObjectProcedure</code> value
     * @return false if the loop over the set terminated because
     * the procedure returned false for some value.
     */
    public boolean forEach(TIntProcedure procedure) {
        byte[] states = _states;
        int[] set = _set;
        for (int i = set.length; i-- > 0; ) {
            if (states[i] == FULL && !procedure.execute(set[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Locates the index of <code>val</code>.
     *
     * @param val an <code>int</code> value
     * @return the index of <code>val</code> or -1 if it isn't in the set.
     */
    protected int index(int val) {
        int hash, index, length;

        final byte[] states = _states;
        final int[] set = _set;
        length = states.length;
        hash = HashFunctions.hash(val) & 0x7fffffff;
        index = hash % length;
        byte state = states[index];

        if (state == FREE)
            return -1;

        if (state == FULL && set[index] == val)
            return index;

        return indexRehashed(val, index, hash, state);
    }

    int indexRehashed(int key, int index, int hash, byte state) {
        // see Knuth, p. 529
        int length = _set.length;
        int probe = 1 + (hash % (length - 2));
        final int loopIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += length;
            }
            state = _states[index];
            //
            if (state == FREE)
                return -1;

            //
            if (key == _set[index] && state != REMOVED)
                return index;
        } while (index != loopIndex);

        return -1;
    }

    /**
     * Locates the index at which <code>val</code> can be inserted. if
     * there is already a value equal()ing <code>val</code> in the set,
     * returns that value as a negative integer.
     *
     * @param val an <code>int</code> value
     * @return an <code>int</code> value
     */
    protected int insertKey(int val) {
        int hash, index;

        hash = HashFunctions.hash(val) & 0x7fffffff;
        index = hash % _states.length;
        byte state = _states[index];

        consumeFreeSlot = false;

        if (state == FREE) {
            consumeFreeSlot = true;
            insertKeyAt(index, val);

            return index; // empty, all done
        }

        if (state == FULL && _set[index] == val) {
            return -index - 1; // already stored
        }

        // already FULL or REMOVED, must probe
        return insertKeyRehash(val, index, hash, state);
    }

    int insertKeyRehash(int val, int index, int hash, byte state) {
        // compute the double hash
        final int length = _set.length;
        int probe = 1 + (hash % (length - 2));
        final int loopIndex = index;
        int firstRemoved = -1;

        /**
         * Look until FREE slot or we start to loop
         */
        do {
            // Identify first removed slot
            if (state == REMOVED && firstRemoved == -1)
                firstRemoved = index;

            index -= probe;
            if (index < 0) {
                index += length;
            }
            state = _states[index];

            // A FREE slot stops the search
            if (state == FREE) {
                if (firstRemoved != -1) {
                    insertKeyAt(firstRemoved, val);
                    return firstRemoved;
                } else {
                    consumeFreeSlot = true;
                    insertKeyAt(index, val);
                    return index;
                }
            }

            if (state == FULL && _set[index] == val) {
                return -index - 1;
            }

            // Detect loop
        } while (index != loopIndex);

        // We inspected all reachable slots and did not find a FREE one
        // If we found a REMOVED slot we return the first one found
        if (firstRemoved != -1) {
            insertKeyAt(firstRemoved, val);
            return firstRemoved;
        }

        // Can a resizing strategy be found that resizes the set?
        throw new IllegalStateException("No free or removed slots available. Key set full?!!");
    }

    void insertKeyAt(int index, int val) {
        _set[index] = val; // insert value
        _states[index] = FULL;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    protected void rehash(int newCapacity) {
        int oldCapacity = _set.length;

        int oldKeys[] = _set;
        V oldVals[] = _values;
        byte oldStates[] = _states;

        _set = new int[newCapacity];
        _values = (V[]) new Object[newCapacity];
        _states = new byte[newCapacity];

        for (int i = oldCapacity; i-- > 0; ) {
            if (oldStates[i] == FULL) {
                int o = oldKeys[i];
                int index = insertKey(o);
                _values[index] = oldVals[i];
            }
        }
    }

    // Query Operations

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(int key) {
        return contains(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object val) {
        byte[] states = _states;
        V[] vals = _values;

        // special case null values so that we don't have to
        // perform null checks before every call to equals()
        if (null == val) {
            for (int i = vals.length; i-- > 0; ) {
                if (states[i] == FULL && null == vals[i]) {
                    return true;
                }
            }
        } else {
            for (int i = vals.length; i-- > 0; ) {
                if (states[i] == FULL && (val == vals[i] || val.equals(vals[i]))) {
                    return true;
                }
            }
        } // end of else
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public V get(int key) {
        int index = index(key);
        return index < 0 ? null : _values[index];
    }

    // Modification Operations

    /**
     * {@inheritDoc}
     */
    public V put(int key, V value) {
        int index = insertKey(key);
        return doPut(value, index);
    }

    /**
     * {@inheritDoc}
     */
    public V putIfAbsent(int key, V value) {
        int index = insertKey(key);
        if (index < 0)
            return _values[-index - 1];
        return doPut(value, index);
    }

    private V doPut(V value, int index) {
        V previous = null;
        boolean isNewMapping = true;
        if (index < 0) {
            index = -index - 1;
            previous = _values[index];
            isNewMapping = false;
        }

        _values[index] = value;

        if (isNewMapping) {
            postInsertHook(consumeFreeSlot);
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     */
    public V remove(int key) {
        V prev = null;
        int index = index(key);
        if (index >= 0) {
            prev = _values[index];
            removeAt(index); // clear key,state; adjust size
        }
        return prev;
    }

    // Bulk Operations

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends Integer, ? extends V> map) {
        Set<? extends Map.Entry<? extends Integer, ? extends V>> set = map.entrySet();
        for (Map.Entry<? extends Integer, ? extends V> entry : set) {
            put(entry.getKey(), entry.getValue());
        }
    }

    // Views

    /**
     * {@inheritDoc}
     */
    public int[] keys() {
        int[] keys = new int[size()];
        int[] k = _set;
        byte[] states = _states;

        for (int i = k.length, j = 0; i-- > 0; ) {
            if (states[i] == FULL) {
                keys[j++] = k[i];
            }
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    public int[] keys(int[] dest) {
        if (dest.length < _size) {
            dest = new int[_size];
        }

        int[] k = _set;
        byte[] states = _states;

        for (int i = k.length, j = 0; i-- > 0; ) {
            if (states[i] == FULL) {
                dest[j++] = k[i];
            }
        }
        return dest;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] values() {
        Object[] vals = new Object[size()];
        V[] v = _values;
        byte[] states = _states;

        for (int i = v.length, j = 0; i-- > 0; ) {
            if (states[i] == FULL) {
                vals[j++] = v[i];
            }
        }
        return vals;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public V[] values(V[] dest) {
        if (dest.length < _size) {
            dest = (V[]) java.lang.reflect.Array.newInstance(dest.getClass().getComponentType(), _size);
        }

        V[] v = _values;
        byte[] states = _states;

        for (int i = v.length, j = 0; i-- > 0; ) {
            if (states[i] == FULL) {
                dest[j++] = (V) v[i];
            }
        }
        return dest;
    }

    /**
     * {@inheritDoc}
     */
    public boolean forEachKey(TIntProcedure procedure) {
        return forEach(procedure);
    }

    /**
     * {@inheritDoc}
     */
    public boolean forEachValue(TObjectProcedure<? super V> procedure) {
        byte[] states = _states;
        V[] values = _values;
        for (int i = values.length; i-- > 0; ) {
            if (states[i] == FULL && !procedure.execute(values[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean forEachValue(Consumer<? super V> procedure) {
        byte[] states = _states;
        V[] values = _values;
        for (int i = values.length; i-- > 0; ) {
            if (states[i] == FULL)
                procedure.accept(values[i]);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean forEachEntry(TIntObjectProcedure<? super V> procedure) {
        byte[] states = _states;
        int[] keys = _set;
        V[] values = _values;
        for (int i = keys.length; i-- > 0; ) {
            if (states[i] == FULL && !procedure.execute(keys[i], values[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean retainEntries(TIntObjectProcedure<? super V> procedure) {
        boolean modified = false;
        byte[] states = _states;
        int[] keys = _set;
        V[] values = _values;

        // Temporarily disable compaction. This is a fix for bug #1738760
        for (int i = keys.length; i-- > 0; ) {
            if (states[i] == FULL && !procedure.execute(keys[i], values[i])) {
                removeAt(i);
                modified = true;
            }
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     */
    public void transformValues(TObjectFunction<V, V> function) {
        byte[] states = _states;
        V[] values = _values;
        for (int i = values.length; i-- > 0; ) {
            if (states[i] == FULL) {
                values[i] = function.execute(values[i]);
            }
        }
    }

    // Comparison and hashing
    public String toString() {
        final StringBuilder buf = new StringBuilder("{");
        forEachEntry(new TIntObjectProcedure<V>() {
            private boolean first = true;

            public boolean execute(int key, Object value) {
                if (first)
                    first = false;
                else buf.append(",");

                buf.append(key);
                buf.append("=");
                buf.append(value);
                return true;
            }
        });
        buf.append("}");
        return buf.toString();
    }

    public static int oneInstanceCount = 0;

    public void testAll() {
        if (this.size() == 1) {
            oneInstanceCount++;
        }
    }

}
