package com.dxogo.hw1.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


import com.dxogo.hw1.cache.Cache;
import com.dxogo.hw1.exception.ResourceNotFoundException;
import com.dxogo.hw1.model.Country;
import com.dxogo.hw1.model.LastSixMonths;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@SuppressWarnings("unchecked")
public class ReportsService {

    @Autowired RestTemplate template;

    private static final Logger log = LoggerFactory.getLogger(ReportsService.class);

    private Cache cache = new Cache(); // change the time to live here (add as argument in the constructor)
    
    // get world data
    public Country getWorldData() throws ResourceNotFoundException {

        String cacheKey = "world";

        Object cache_world = cache.get( cacheKey );
        if (cache_world != null) {
            log.info("> [CACHE] Getting world data.");
            return (Country) cache_world;
        }

        log.info("> [REQUEST] Getting world data.");
        
        ResponseEntity<Country[]> response = template.getForEntity("/npm-covid-data/world", Country[].class);
        Country world = (response.getBody())[0];

        
        cache.add(cacheKey, world);
        log.info("> [WORLD CACHE SAVED]");
        
        return world;
    }

    public TreeMap<String, String> getMapCountryISO() throws IOException, InterruptedException {

        String cacheKey = "createMapCountryIso";

        Object cache_map = cache.get( cacheKey );
        if (cache_map != null) {
            log.info("> [CACHE] Getting HashMap <ISO, Country>");
            return (TreeMap<String, String>) cache_map;
        }

        log.info("> [REQUEST] Getting HashMap <ISO, Country>");
        
        TreeMap<String,String> map = new TreeMap<>();


        ResponseEntity<JSONObject[]> response = template.getForEntity("npm-covid-data/countries-name-ordered", JSONObject[].class);
        JSONObject[] data = response.getBody();

        for (JSONObject c : data){

            String country = c.get("Country").toString();
            String iso = c.get("ThreeLetterSymbol").toString();

            map.put(iso, country);
        }
        
        cache.add(cacheKey, map);
        log.info("> [MAPPING CACHE SAVED]");

        return map;
    }


    // get country from iso
    public String getCountryFromISO(String iso) throws ResourceNotFoundException, IOException, InterruptedException {        
        if (this.getMapCountryISO().containsKey(iso.toLowerCase())) { return this.getMapCountryISO().get(iso.toLowerCase()); } 
        return null;
    }

    // get iso from country
    public String getIsoFromCountry(String country) throws IOException, InterruptedException {

        country = country.toLowerCase();

        HashMap<String, String> map = new HashMap<>();
        
        for(HashMap.Entry<String, String> entry : this.getMapCountryISO().entrySet()){
            map.put(entry.getValue().toLowerCase(), entry.getKey());
        }

        if (map.containsKey(country)) {
            return map.get(country);
        } else { return null; }
    }

    // today data of 1 country
    public Country countryDataToday(String iso_code, String country) throws IOException, InterruptedException {
        
        String cacheKey = iso_code +"_"+ country;

        Object cache_today = cache.get( cacheKey );
        if (cache_today != null) {
            log.info("> [CACHE] Getting country data", country );
            return (Country) cache_today;
        }

        log.info("> [REQUEST] Getting country data", country);

        ResponseEntity<Country[]> response = template.getForEntity("npm-covid-data/country-report-iso-based/" + country + "/" + iso_code, Country[].class);
        Country[] c = response.getBody();

        cache.add(cacheKey, c);
        log.info("> [COUNTRY CACHE SAVED]");
        
        return c[0];
    }

    // ! arrays de countries

    // data from last 6 months from 1 country
    public List<LastSixMonths> getLastSixMonthsData(String iso_code) throws IOException, InterruptedException {        
        
        String cacheKey = iso_code;

        List<LastSixMonths> l_6months = new ArrayList<>();

        Object cache_6months = cache.get( cacheKey );
        if (cache_6months != null) {
            log.info("> [CACHE] Getting data of last six months.", this.getMapCountryISO().get(iso_code));
            return (List<LastSixMonths>) cache_6months;
        }

        log.info("> [REQUEST] Getting data from the last 6 months", iso_code);

        ResponseEntity<LastSixMonths[]> response = template.getForEntity("covid-ovid-data/sixmonth/" + iso_code, LastSixMonths[].class);
        LastSixMonths[] last6MonthsData =response.getBody();
        
        cache.add(cacheKey, last6MonthsData);
        log.info("> [LAST 6 MONTHS CACHE SAVED]");
        
        for (LastSixMonths data : last6MonthsData){
            l_6months.add(data);
        }

        return l_6months;
    }

    // top10 with most total cases in the
    public List<Country> getTop10() throws IOException, InterruptedException {
    
        String cacheKey = "top10";

        Object cache_top10 = cache.get( cacheKey );
        if (cache_top10 != null) {
            log.info(">> [CACHE] Top10 most affected");
            return (List<Country>) cache_top10;
        }

        log.info("> [REQUEST] Top10 most affected");


        Map<String, Integer> map = new HashMap<>();
        List<Country> map10 = new ArrayList<>();


        ResponseEntity<Country[]> response = template.getForEntity("npm-covid-data/", Country[].class);
        Country[] top10 = response.getBody();

        for (Country c : top10){
            String country = c.getName();
            int total_cases = c.getTotal_cases();

            if (!country.equals("World") && !country.equals("Total:")){

                map.put(country, total_cases);
                map10.add(c);
            }
        }

        Map<String, Integer> topTen =
            map.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        
        List<Country> finalList = new ArrayList<>();
        
        int counter = 0;
        for (Country c : map10){
            for (String key : topTen.keySet()){
                if (c.getName().equals(key) && counter<10){
                    finalList.add(c);
                    counter += 1;
                }
            }
        }

        cache.add(cacheKey, finalList);
        log.info("> [TOP 10 CACHE SAVED]");

    return finalList;
        
    }
    

    public String getCacheDetails() { return cache.toString(); }
}
