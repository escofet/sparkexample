package com.mycompany.sparkprj;
import static spark.Spark.get;
import static spark.Spark.stop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;

/*
Get current exchange from fixer.io and convert currency EUR -> USD
Reboot service on a daily basis
Default port 4567. Eg http://localhost:4567/rates
*/
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

@SuppressWarnings("ClassWithoutLogger")
public class MoneyExchange {
    @SuppressWarnings({"PublicField", "NonPublicExported"})
    public static CurrencyExchange exchange = null;
    
    static {
        try {
            // Ger rates against EUR
        	SocketAddress addr = new InetSocketAddress("vale.proxy.corp.sopra", 8080);
    		Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            String json = Jsoup.connect("http://api.fixer.io/latest?base=EUR")
                    .ignoreContentType(true)
                    .proxy(proxy)
                    .execute()
                    .body();
            exchange = new Gson().fromJson(json, CurrencyExchange.class);
        } catch(IOException | JsonParseException ex) {
            // Some logging filtering ex
            stop();
            System.exit(-1);
        }
    }
    
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args){
        /*
        REST API
        GET all rates values against EUR
        GET convert an amount from EUR to an specific currency (parameters)
        GET convert an amount from EUR to an specific currency (query)
        */
        get("/rates", (req, res) -> {
            res.type("application/json");
            return new Gson().toJson(exchange.rates);
        });
        
        get("/convert/:amount/:currency", (req, res) -> {
            res.type("application/json");
            String curr = req.params("currency");
            Double value = Double.parseDouble(req.params("amount")) * 
            exchange.getRate(curr);
            return String.format(Locale.US,
                "{\"%S\":%.2f}", curr, value
            );
        });
        
        get("/convert", (req, res) -> {
            res.type("application/json");
            String curr = req.queryParams("currency");
            Double value = Double.parseDouble(req.queryParams("amount")) * 
            exchange.getRate(curr);
            return String.format(Locale.US,
                "{\"%S\":%.2f}", curr, value
            );
        });
    }
}

@SuppressWarnings({"ClassWithoutLogger", "MultipleTopLevelClassesInFile", "PackageVisibleField"})
class CurrencyExchange {
    public String base;
    public String date;
    public Map<String, Double> rates;
    
    @SuppressWarnings({"PublicConstructorInNonPublicClass", "CollectionWithoutInitialCapacity"})
    public CurrencyExchange() {
        rates = new HashMap<>();
    }
    
    public Double getRate(String curr) {
        return rates.get(curr.toUpperCase());
    }
}
