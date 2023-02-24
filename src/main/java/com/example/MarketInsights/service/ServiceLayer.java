package com.example.MarketInsights.service;

import com.example.MarketInsights.VO.Data;
import com.example.MarketInsights.VO.Records;
import com.example.MarketInsights.dao.CommodityRepository;
import com.example.MarketInsights.dao.CommodityPriceRepository;
import com.example.MarketInsights.model.Commodity;
import com.example.MarketInsights.model.CommodityPrice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@Service
public class ServiceLayer {
    private final RestTemplate restTemplate;
    String url="https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070";
    String apiKey="579b464db66ec23bdd0000015709af7f45e048694fedae861885947c";
    String format="json";
    int defaultLimit=10000;
    @Autowired
    public ServiceLayer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    @Autowired
    private CommodityRepository commodityRepository;
    @Autowired
    private CommodityPriceRepository commodityPriceRepository;

    /**
     *
     * @param limit
     * @return
     */
    public Records consumeAPILimit(String limit){
        Records response = restTemplate.getForEntity(
                url+"?api-key="+apiKey+"&format="+format+"&limit="+limit,Records.class
        ).getBody();

        return response;
    }

    /**
     *
     * @param state
     * @return
     */
    public Records consumeAPIState(String state){
        Records response = restTemplate.getForEntity(
                url+"?api-key="+apiKey+"&format="+format+"&limit="+defaultLimit+"&filters[state]="+state,Records.class
        ).getBody();

        return response;
    }

    /**
     *
     * @param state
     * @param district
     * @param market
     * @param commodity
     * @param variety
     * @return
     */
    public Records consumeAPIMandi(String state,String district,String market, String commodity, String variety){
        Records response = restTemplate.getForEntity(
                url+"?api-key="+apiKey+"&format="+format+"&limit="+defaultLimit+"&filters[state]="+state+"&filters[district]="+district+"&filters[market]="+market+"&filters[commodity]="+commodity+"&filters[variety]="+variety,Records.class
        ).getBody();

        return response;
    }

    /**
     *
     * @return
     * @throws ParseException
     */
    public Records consumeAPIRef() throws ParseException {
        Records response = restTemplate.getForEntity(
                url+"?api-key="+apiKey+"&format="+format+"&limit="+defaultLimit+"&filters[state]=Kerala",Records.class
        ).getBody();
        ArrayList<Data> records= (ArrayList<Data>) response.getRecords();
        for(Data record: records){
            String id=record.getState()+record.getDistrict()+record.getMarket()+record.getCommodity()+record.getVariety();
            boolean exists = commodityRepository.existsById(id);
            String strDate = record.getArrival_date();
            int price = Integer.parseInt(record.getModal_price());
            int min_price=Integer.parseInt((record.getMin_price().equals("NA"))?record.getModal_price():record.getMin_price());
            int max_price=Integer.parseInt((record.getMax_price().equals("NA"))?record.getModal_price():record.getMax_price());
            Date date = new SimpleDateFormat("dd/mm/yyyy").parse(strDate);
            String commodityPriceId=id+strDate;
            CommodityPrice newPrice=new CommodityPrice(commodityPriceId,date,price,min_price,max_price);
            commodityPriceRepository.save(newPrice);
            if(!exists){
                ArrayList<CommodityPrice> priceList=new ArrayList<>();
                priceList.add(newPrice);

                Commodity commodity=new Commodity(id,record.getState(),record.getDistrict(),record.getMarket(),record.getCommodity(), record.getVariety(),priceList);
                commodityRepository.save(commodity);
            }else{
                Commodity commodity = commodityRepository.findById(id).get();
                ArrayList<CommodityPrice> priceList=commodity.getModal_price();
                int sizeOfList = priceList.size();
                String idPrice = priceList.get(sizeOfList-1).getId();
                if(!idPrice.equals(commodityPriceId)){
                    priceList.add(newPrice);
                    commodity.setModal_price(priceList);
                    commodityRepository.save(commodity);
                }

            }
        }
        return response;
    }

}
