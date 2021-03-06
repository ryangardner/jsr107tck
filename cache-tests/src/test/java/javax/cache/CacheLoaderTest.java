/**
 *  Copyright 2011 Terracotta, Inc.
 *  Copyright 2011 Oracle, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.cache;

import org.junit.Rule;
import org.junit.Test;

import javax.cache.util.ExcludeListExcluder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for Cache.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class CacheLoaderTest extends TestSupport {

    /**
     * Rule used to exclude tests
     */
    @Rule
    public ExcludeListExcluder rule = new ExcludeListExcluder(this.getClass());

    /**
     * the time to wait for a future
     */
    protected static final long FUTURE_WAIT_MILLIS = 100;

    @Test
    public void load_NullKey() {
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).build();
        CacheLoader<Integer, Integer> cl = new MockCacheLoader<Integer, Integer>();
        try {
            cache.load(null);
            fail("should have thrown an exception - null key not allowed");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void load_Found() {
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).build();

        CacheLoader<Integer, Integer> cl = new MockCacheLoader<Integer, Integer>();
        Integer key = 1;
        cache.put(key, key);
        try {
            assertNull(cache.load(key));
        } catch (NullPointerException e) {
            fail("should not have thrown an exception - if key in store should return null");
        }
    }

    @Test
    public void load_NoCacheLoader() {
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).build();
        Integer key = 1;
        try {
            assertNull(cache.load(key));
        } catch (NullPointerException e) {
            fail("should not have thrown an exception - with no cache loader should return null");
        }
    }

    @Test
    public void load_NullValue() throws Exception {
        final Integer valueDefault = null;
        CacheLoader<Integer, Integer> clDefault = new MockCacheLoader<Integer, Integer>() {
            @Override
            public Cache.Entry<Integer, Integer> load(final Object key) {
                return new Cache.Entry<Integer, Integer>() {
                    public Integer getKey() {
                        return (Integer) key;
                    }

                    public Integer getValue() {
                        return null;
                    }
                };
            }
        };
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Future<Integer> future = cache.load(key);
        assertNotNull(future);
        try {
            assertEquals(valueDefault, future.get(FUTURE_WAIT_MILLIS, TimeUnit.MILLISECONDS));
            fail("should have thrown an exception - null value not allowed");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
            assertFalse(cache.containsKey(key));
        }
    }

    @Test
    public void load_DefaultCacheLoader() throws Exception {
        final Integer valueDefault = 123;
        CacheLoader<Integer, Integer> clDefault = new SimpleCacheLoader<Integer>(valueDefault);
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();

        Integer key = 1;
        Future<Integer> future = cache.load(key);
        assertNotNull(future);
        assertEquals(valueDefault, future.get(FUTURE_WAIT_MILLIS, TimeUnit.MILLISECONDS));
        assertTrue(cache.containsKey(key));
        assertEquals(valueDefault, cache.get(key));
    }

    @Test
    public void load_ExceptionPropagation() throws Exception {
        final RuntimeException expectedException = new RuntimeException("expected");
        CacheLoader<Integer, Integer> clDefault = new SimpleCacheLoader<Integer>(expectedException);
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Future<Integer> future = cache.load(key);
        assertNotNull(future);
        try {
            future.get(FUTURE_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            fail("expected exception");
        } catch (ExecutionException e) {
            assertEquals(expectedException, e.getCause());
        }
    }

    @Test
    public void loadAll_NotStarted() {
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).build();
        cache.stop();
        try {
            cache.loadAll(null);
            fail("should have thrown an exception - cache not started");
        } catch (IllegalStateException e) {
            //good
        }
    }

    @Test
    public void loadAll_NullKeys() {
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).build();
        try {
            cache.loadAll(null);
            fail("should have thrown an exception - keys null");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void loadAll_NullKey() throws Exception {
        CacheLoader<Integer, Integer> cl = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(cl).build();
        ArrayList<Integer> keys = new ArrayList<Integer>();
        keys.add(null);
        try {
            cache.loadAll(keys);
            fail("should have thrown an exception - keys contains null");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void loadAll_NullValue() throws Exception {
        CacheLoader<Integer, Integer> cl = new MockCacheLoader<Integer, Integer>() {
            @Override
            public Map<Integer, Integer> loadAll(Collection<? extends Integer> keys) {
                Map<Integer, Integer> map = new HashMap<Integer, Integer>();
                for (Integer key : keys) {
                    map.put(key, null);
                }
                return map;
            }
        };
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(cl).build();
        ArrayList<Integer> keys = new ArrayList<Integer>();
        keys.add(1);
        keys.add(2);
        Future<Map<Integer, Integer>> future = cache.loadAll(keys);
        try {
            Map<Integer, Integer> map = future.get(FUTURE_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            assertEquals(keys.size(), map.size());
            fail("should have thrown an exception - null value");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void loadAll_1Found1Not() throws Exception {
        CacheLoader<Integer, Integer> cl = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(cl).build();
        Integer keyThere = 1;
        cache.put(keyThere, keyThere);
        Integer keyNotThere = keyThere + 1;
        ArrayList<Integer> keys = new ArrayList<Integer>();
        keys.add(keyThere);
        keys.add(keyNotThere);
        Future<Map<Integer, Integer>> future = cache.loadAll(keys);
        Map<Integer, Integer> map = future.get(FUTURE_WAIT_MILLIS, TimeUnit.MILLISECONDS);
        assertEquals(1, map.size());
        assertEquals(keyNotThere, map.get(keyNotThere));
        assertEquals(keyThere, cache.get(keyThere));
        assertEquals(keyNotThere, cache.get(keyNotThere));
    }

    @Test
    public void loadAll_NoCacheLoader() throws Exception {
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).build();
        ArrayList<Integer> keys = new ArrayList<Integer>();
        keys.add(1);
        try {
            assertNull(cache.loadAll(keys));
        } catch (NullPointerException e) {
            fail("should not have thrown an exception - with no cache loader should return null");
        }
    }

    @Test
    public void loadAll_DefaultCacheLoader() throws Exception {
        ArrayList<Integer> keys = new ArrayList<Integer>();
        keys.add(1);
        keys.add(2);
        CacheLoader<Integer, Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();

        Future<Map<Integer, Integer>> future = cache.loadAll(keys);
        Map<Integer, Integer> map = future.get(FUTURE_WAIT_MILLIS, TimeUnit.MILLISECONDS);
        assertEquals(keys.size(), map.size());
        for (Integer key : keys) {
            assertEquals(key, map.get(key));
            assertEquals(key, cache.get(key));
        }
    }

    @Test
    public void loadAll_ExceptionPropagation() throws Exception {
        final RuntimeException expectedException = new RuntimeException("expected");
        CacheLoader<Integer, Integer> clDefault = new MockCacheLoader<Integer, Integer>() {
            @Override
            public Map<Integer, Integer> loadAll(Collection<? extends Integer> keys) {
                throw expectedException;
            }
        };
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        ArrayList<Integer> keys = new ArrayList<Integer>();
        keys.add(1);
        Future<Map<Integer, Integer>> future = cache.loadAll(keys);
        assertNotNull(future);
        try {
            future.get(FUTURE_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            fail("expected exception");
        } catch (ExecutionException e) {
            assertEquals(expectedException, e.getCause());
        }
    }

    @Test
    public void get_Stored() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();

        Integer key = 1;
        assertFalse(cache.containsKey(key));
        assertEquals(key, cache.get(key));
        assertTrue(cache.containsKey(key));

        // Confirm that result is stored (no 2nd load)
        clDefault.exception = new NullPointerException();
        assertEquals(key, cache.get(key));
    }

    @Test
    public void get_Exception() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();

        Integer key = 1;
        clDefault.exception = new NullPointerException();
        try {
            cache.get(key);
            fail("should have got an exception ");
        } catch (RuntimeException e) {
            assertEquals(clDefault.exception, e);
        }
    }

    @Test
    public void getAll() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();

        ArrayList<Integer> keysToGet = new ArrayList<Integer>();
        keysToGet.add(1);
        keysToGet.add(2);
        keysToGet.add(3);

        Map<Integer, Integer> map = cache.getAll(keysToGet);
        assertEquals(keysToGet.size(), map.size());
        for (Integer key : keysToGet) {
            assertTrue(map.containsKey(key));
            assertEquals(cache.get(key), map.get(key));
            assertEquals(key, map.get(key));
        }

        // Confirm that result is stored (no 2nd load)
        for (Integer key : keysToGet) {
            assertTrue(cache.containsKey(key));
        }
    }

    @Test
    public void containsKey() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        assertFalse(cache.containsKey(key));
        assertEquals(key, cache.get(key));
        assertTrue(cache.containsKey(key));
    }

    @Test
    public void putIfAbsent() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Integer value = key + 1;
        assertTrue(cache.putIfAbsent(key, value));
        assertEquals(value, cache.get(key));
    }

    @Test
    public void getAndRemove_NotExistent() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        assertNull(cache.getAndRemove(key));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void getAndRemove_There() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Integer value = key  + 1;
        cache.put(key, value);
        assertEquals(value, cache.getAndRemove(key));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void replace_3arg_Missing() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Integer newValue = key + 1;
        assertFalse(cache.replace(key, key, newValue));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void replace_3arg_Different() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Integer value1 = key  + 1;
        Integer value2 = value1 + 1;
        Integer value3 = value2 + 1;
        cache.put(key, value1);
        assertFalse(cache.replace(key, value2, value3));
        assertEquals(value1, cache.get(key));
    }

    @Test
    public void replace_3arg() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Integer value1 = key  + 1;
        Integer value2 = value1 + 1;
        Integer value3 = value2 + 1;
        cache.put(key, value2);
        assertTrue(cache.replace(key, value2, value3));
        assertEquals(value3, cache.get(key));
    }

    @Test
    public void replace_2arg_Missing() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        assertFalse(cache.replace(key, key));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void replace_2arg() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Integer value1 = key  + 1;
        Integer value2 = value1 + 1;
        Integer value3 = value2 + 1;
        cache.put(key, value2);
        assertTrue(cache.replace(key, value3));
        assertEquals(value3, cache.get(key));
    }

    @Test
    public void getAndReplace() {
        SimpleCacheLoader<Integer> clDefault = new SimpleCacheLoader<Integer>();
        Cache<Integer, Integer> cache = getCacheManager().
                <Integer, Integer>createCacheBuilder(getTestCacheName()).setCacheLoader(clDefault).build();
        Integer key = 1;
        Integer newValue = key + 1;
        assertNull(cache.getAndReplace(key, newValue));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void get_WithNonKeyKey() {
        ArrayList<Integer> key1 = new ArrayList<Integer>();
        key1.add(1);
        key1.add(2);
        LinkedList<Integer> key2 = new LinkedList<Integer>(key1);

        CacheLoader<ArrayList<Integer>, String> cacheLoader = new ArrayListCacheLoader();
        Cache<ArrayList<Integer>, String> cache = getCacheManager().
                <ArrayList<Integer>, String>createCacheBuilder(getTestCacheName()).setCacheLoader(cacheLoader).build();

        String value = cache.get(key2);
        assertEquals(cacheLoader.load(key2).getValue(), value);
    }

    // ---------- utilities ----------

    /**
     * A mock CacheLoader which simply throws UnsupportedOperationException on all methods.
     * @param <K>
     * @param <V>
     */
    protected static class MockCacheLoader<K, V> implements CacheLoader<K, V> {

        @Override
        public Cache.Entry<K, V> load(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<K, V> loadAll(Collection<? extends K> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canLoad(Object key) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A simple example of a Cache Loader which simply adds the key as the value.
     * @param <K>
     */
    private static class SimpleCacheLoader<K> implements CacheLoader<K, K> {
        private RuntimeException exception = null;
        private final K value;

        public SimpleCacheLoader() {
            this(null, null);
        }

        public SimpleCacheLoader(K value) {
            this(value, null);
        }

        public SimpleCacheLoader(RuntimeException exception) {
            this(null, exception);
        }

        public SimpleCacheLoader(K value, RuntimeException exception){
            this.value = value;
            this.exception = exception;

        }

        @Override
        public Cache.Entry<K, K> load(final Object key) {
            if (exception != null) {
                throw exception;
            }
            return new Cache.Entry<K, K>() {
                @Override
                public K getKey() {
                    return (K) key;
                }

                @Override
                public K getValue() {
                    return value == null ? (K) key : value;
                }
            };
        }

        @Override
        public Map<K, K> loadAll(Collection<? extends K> keys) {
            Map<K, K> map = new HashMap<K, K>();
            for (K key : keys) {
                map.put(key, key);
            }
            return map;
        }

        @Override
        public boolean canLoad(Object key) {
            return true;
        }
    }

    /**
     * A simple example of a Cache Loader
     */
    private static class ArrayListCacheLoader implements CacheLoader<ArrayList<Integer>, String> {

        @Override
        public Cache.Entry<ArrayList<Integer>, String> load(final Object key) {
            return new Cache.Entry<ArrayList<Integer>, String>() {
                @Override
                public ArrayList<Integer> getKey() {
                    return new ArrayList<Integer>((List) key);
                }

                @Override
                public String getValue() {
                    return key.toString();
                }
            };
        }

        @Override
        public Map<ArrayList<Integer>, String> loadAll(Collection<? extends ArrayList<Integer>> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canLoad(Object key) {
            return true;
        }
    }
}
