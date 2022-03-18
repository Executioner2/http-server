package com.ranni.util;

import java.util.*;

/**
 * Title: HttpServer
 * Description: Iterator的包装类，不支持无参构造
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-07 21:54
 */
public class Enumerator implements Enumeration {
    private Iterator iterator;

    public Enumerator(Collection collection) {
        this(collection.iterator());
    }

    public Enumerator(Map map) {
        this(map.values().iterator());
    }

    public Enumerator(Iterator iterator) {
        super();
        this.iterator = iterator;
    }

    @Override
    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    @Override
    public Object nextElement() throws NoSuchElementException {
        return iterator.next();
    }
}
