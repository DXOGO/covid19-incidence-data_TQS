package com.dxogo.hw1.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Cache {

    private Map<String, Object> cache;
    private Map<String, Long> t2t;
    
    private long max_time;

    private int requests = 0;
    private int hits = 0;
    private int misses = 0;

    public Cache() {
        max_time = 100 * 1000;
        cache = new HashMap<>();
        t2t = new HashMap<>();
    }

    public Cache(long max_time) {
        this.max_time = max_time * 1000;
        cache = new HashMap<>();
        t2t = new HashMap<>();
    }
    
    public void add( String key, Object object ) {
        cache.put(key, object);

        long time = System.currentTimeMillis() + max_time;
        t2t.put(key, time);
    }

    public boolean clean(String key) {
        if ( cache.containsKey(key) ){
            cache.remove(key);
            t2t.remove(key);
            return true;
        }
        return false;
    }
    
    public Object get(String key) {
        
        this.requests++;

        if ( cache.containsKey(key) && t2t.get(key) > System.currentTimeMillis()){
            this.hits++;
            return cache.get(key);

        } else if (cache.containsKey(key) && t2t.get(key) <= System.currentTimeMillis()) {
            clean(key);
        }

        this.misses++;
        return null;
    }

    public void clear() {
        cache.clear();
        t2t.clear();
    }

    public int size() {

        ArrayList<String> keys2remove = new ArrayList<>();

        for (String key : cache.keySet()) {
            if ( t2t.get(key) <= System.currentTimeMillis() ) {
                keys2remove.add(key);
            }
        }
        
        for (String key : keys2remove) clean(key);

        return this.cache.size();
    }

    public long getMax_time() { return this.max_time; }

    public void setMax_time(long max_time) { this.max_time = max_time; }

    public int getRequests() { return this.requests; }

    public int getHits() { return this.hits; }

    public int getMisses() { return this.misses; }


    @Override
    public String toString() {
        return "{" +
            "time_to_live='" + getMax_time() + "'" +
            ", requests='" + getRequests() + "'" +
            ", hits='" + getHits() + "'" +
            ", misses='" + getMisses() + "'" +
            "}";
    }

    public boolean isEmpty() { return this.size() == 0; }

}
