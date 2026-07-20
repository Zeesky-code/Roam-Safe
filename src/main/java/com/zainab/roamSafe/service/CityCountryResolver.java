package com.zainab.roamSafe.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps a covered destination to its country.
 *
 * Cities were being stored with country "Unknown" because the importers never
 * had the value, which quietly disabled every country filter and forced the API
 * to report country as unknown for all coverage.
 *
 * Lookup is accent- and case-insensitive, so "Kraków" and "Krakow" resolve to
 * the same entry. Anything not listed stays unresolved rather than being
 * guessed: an unknown country is recoverable, a confidently wrong one is not.
 *
 * A handful of entries are not strictly cities (Bali is an island, Fiji a
 * country). They are kept because they are how travellers actually search, and
 * the country is still correct.
 */
@Component
public class CityCountryResolver {

    private static final Map<String, String> COUNTRIES = new LinkedHashMap<>();

    private static void put(String city, String country) {
        COUNTRIES.put(normalize(city), country);
    }

    static {
        // Europe
        put("London", "United Kingdom");
        put("Manchester", "United Kingdom");
        put("Birmingham", "United Kingdom");
        put("Liverpool", "United Kingdom");
        put("Leeds", "United Kingdom");
        put("Bristol", "United Kingdom");
        put("Newcastle", "United Kingdom");
        put("Sheffield", "United Kingdom");
        put("Nottingham", "United Kingdom");
        put("Cardiff", "United Kingdom");
        put("Belfast", "United Kingdom");
        put("Brighton", "United Kingdom");
        put("Oxford", "United Kingdom");
        put("Cambridge", "United Kingdom");
        put("Bath", "United Kingdom");
        put("York", "United Kingdom");
        put("Edinburgh", "United Kingdom");
        put("Glasgow", "United Kingdom");
        put("Paris", "France");
        put("Nice", "France");
        put("Barcelona", "Spain");
        put("Madrid", "Spain");
        put("Rome", "Italy");
        put("Florence", "Italy");
        put("Venice", "Italy");
        put("Milan", "Italy");
        put("Naples", "Italy");
        put("Lisbon", "Portugal");
        put("Porto", "Portugal");
        put("Amsterdam", "Netherlands");
        put("Berlin", "Germany");
        put("Munich", "Germany");
        put("Vienna", "Austria");
        put("Prague", "Czechia");
        put("Budapest", "Hungary");
        put("Kraków", "Poland");
        put("Warsaw", "Poland");
        put("Athens", "Greece");
        put("Santorini", "Greece");
        put("Dublin", "Ireland");
        put("Copenhagen", "Denmark");
        put("Stockholm", "Sweden");
        put("Oslo", "Norway");
        put("Helsinki", "Finland");
        put("Reykjavik", "Iceland");
        put("Brussels", "Belgium");
        put("Bruges", "Belgium");
        put("Zurich", "Switzerland");
        put("Geneva", "Switzerland");
        put("Istanbul", "Türkiye");
        put("Ankara", "Türkiye");
        put("Antalya", "Türkiye");
        put("Moscow", "Russia");
        put("Zagreb", "Croatia");
        put("Dubrovnik", "Croatia");
        put("Split", "Croatia");
        put("Bucharest", "Romania");
        put("Sofia", "Bulgaria");
        put("Kyiv", "Ukraine");
        put("Tallinn", "Estonia");
        put("Riga", "Latvia");
        put("Vilnius", "Lithuania");
        put("Ljubljana", "Slovenia");
        put("Bratislava", "Slovakia");
        put("Valletta", "Malta");

        // Middle East
        put("Dubai", "United Arab Emirates");
        put("Abu Dhabi", "United Arab Emirates");
        put("Doha", "Qatar");
        put("Tel Aviv", "Israel");
        put("Jerusalem", "Israel");
        put("Amman", "Jordan");
        put("Beirut", "Lebanon");
        put("Riyadh", "Saudi Arabia");
        put("Muscat", "Oman");

        // Africa
        put("Cairo", "Egypt");
        put("Marrakech", "Morocco");
        put("Casablanca", "Morocco");
        put("Fes", "Morocco");
        put("Tangier", "Morocco");
        put("Tunis", "Tunisia");
        put("Cape Town", "South Africa");
        put("Johannesburg", "South Africa");
        put("Durban", "South Africa");
        put("Nairobi", "Kenya");
        put("Mombasa", "Kenya");
        put("Zanzibar", "Tanzania");
        put("Dar es Salaam", "Tanzania");
        put("Addis Ababa", "Ethiopia");
        put("Accra", "Ghana");
        put("Lagos", "Nigeria");
        put("Abuja", "Nigeria");
        put("Dakar", "Senegal");
        put("Kampala", "Uganda");
        put("Kigali", "Rwanda");
        put("Victoria Falls", "Zimbabwe");

        // Asia
        put("Tokyo", "Japan");
        put("Osaka", "Japan");
        put("Kyoto", "Japan");
        put("Seoul", "South Korea");
        put("Busan", "South Korea");
        put("Beijing", "China");
        put("Shanghai", "China");
        put("Hong Kong", "Hong Kong");
        put("Macau", "Macau");
        put("Taipei", "Taiwan");
        put("Singapore", "Singapore");
        put("Bangkok", "Thailand");
        put("Chiang Mai", "Thailand");
        put("Phuket", "Thailand");
        put("Krabi", "Thailand");
        put("Hanoi", "Vietnam");
        put("Ho Chi Minh City", "Vietnam");
        put("Da Nang", "Vietnam");
        put("Siem Reap", "Cambodia");
        put("Phnom Penh", "Cambodia");
        put("Vientiane", "Laos");
        put("Kuala Lumpur", "Malaysia");
        put("Penang", "Malaysia");
        put("Bali", "Indonesia");
        put("Jakarta", "Indonesia");
        put("Manila", "Philippines");
        put("Cebu", "Philippines");
        put("Delhi", "India");
        put("Mumbai", "India");
        put("Jaipur", "India");
        put("Goa", "India");
        put("Bengaluru", "India");
        put("Kathmandu", "Nepal");
        put("Colombo", "Sri Lanka");
        put("Dhaka", "Bangladesh");
        put("Karachi", "Pakistan");
        put("Lahore", "Pakistan");
        put("Tashkent", "Uzbekistan");
        put("Almaty", "Kazakhstan");
        put("Tbilisi", "Georgia");
        put("Baku", "Azerbaijan");
        put("Yerevan", "Armenia");
        put("Ulaanbaatar", "Mongolia");

        // Americas
        put("New York City", "United States");
        // Both spellings are present in the data as separate rows. Accent
        // stripping already collapses Kraków/Krakow, Bogotá/Bogota and
        // Cancún/Cancun, but this pair differs by a whole word.
        put("New York", "United States");
        put("Los Angeles", "United States");
        put("San Francisco", "United States");
        put("Chicago", "United States");
        put("Miami", "United States");
        put("Las Vegas", "United States");
        put("New Orleans", "United States");
        put("Seattle", "United States");
        put("Boston", "United States");
        put("Washington DC", "United States");
        put("Honolulu", "United States");
        put("Toronto", "Canada");
        put("Vancouver", "Canada");
        put("Montreal", "Canada");
        put("Mexico City", "Mexico");
        put("Cancún", "Mexico");
        put("Guadalajara", "Mexico");
        put("Oaxaca", "Mexico");
        put("Tulum", "Mexico");
        put("Havana", "Cuba");
        put("Santo Domingo", "Dominican Republic");
        put("Kingston", "Jamaica");
        put("San José", "Costa Rica");
        put("Panama City", "Panama");
        put("Guatemala City", "Guatemala");
        put("Bogotá", "Colombia");
        put("Medellín", "Colombia");
        put("Cartagena", "Colombia");
        put("Cali", "Colombia");
        put("Lima", "Peru");
        put("Cusco", "Peru");
        put("Quito", "Ecuador");
        put("Guayaquil", "Ecuador");
        put("La Paz", "Bolivia");
        put("Santiago", "Chile");
        put("Buenos Aires", "Argentina");
        put("Mendoza", "Argentina");
        put("Montevideo", "Uruguay");
        put("Rio de Janeiro", "Brazil");
        put("São Paulo", "Brazil");
        put("Salvador", "Brazil");
        put("Caracas", "Venezuela");
        put("Asunción", "Paraguay");

        // Oceania
        put("Sydney", "Australia");
        put("Melbourne", "Australia");
        put("Brisbane", "Australia");
        put("Perth", "Australia");
        put("Cairns", "Australia");
        put("Auckland", "New Zealand");
        put("Wellington", "New Zealand");
        put("Queenstown", "New Zealand");
        put("Christchurch", "New Zealand");
        put("Fiji", "Fiji");
        put("Nadi", "Fiji");
        put("Port Moresby", "Papua New Guinea");
    }

    /** The country for a destination, or empty when it isn't known. */
    public Optional<String> countryFor(String city) {
        if (city == null || city.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(COUNTRIES.get(normalize(city)));
    }

    /**
     * Lowercased and stripped of accents, so "Bogotá" and "Bogota" - both of
     * which exist in the data - collapse to one key.
     */
    private static String normalize(String value) {
        String stripped = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.toLowerCase();
    }

    public int knownDestinations() {
        return COUNTRIES.size();
    }
}
