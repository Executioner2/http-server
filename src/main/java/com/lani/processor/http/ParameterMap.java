package com.lani.processor.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-02 21:37
 */
public class ParameterMap extends HashMap {
    private boolean locked = true;

    public ParameterMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ParameterMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ParameterMap() {
        super();
    }

    public ParameterMap(Map m) {
        super(m);
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Object get(Object key) {
        return super.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    @Override
    public Object put(Object key, Object value) {
        if (locked) throw new IllegalStateException("parameterMap 已锁定");
        return super.put(key, value);
    }

    @Override
    public void putAll(Map m) {
        if (locked) throw new IllegalStateException("parameterMap 已锁定");
        super.putAll(m);
    }

    @Override
    public Object remove(Object key) {
        if (locked) throw new IllegalStateException("parameterMap 已锁定");
        return super.remove(key);
    }

    @Override
    public void clear() {
        if (locked) throw new IllegalStateException("parameterMap 已锁定");
        super.clear();
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (locked) throw new IllegalStateException("parameterMap 已锁定");
        return super.remove(key, value);
    }
}
