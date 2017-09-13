package org.gabi.postgresql.orangeproject.resource;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.gabi.postgresql.orangeproject.model.Subscriber;
import org.gabi.postgresql.orangeproject.service.SubscriberService;
import org.jboss.ejb3.annotation.SecurityDomain;

import redis.clients.jedis.Jedis;

@Path("/customers")
@SecurityDomain("keycloak")
public class SubscriberResource {

    @EJB
	SubscriberService subscriberService = new SubscriberService();
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
	public List<Subscriber> getSubscribers(@Context HttpHeaders httpHeaders) throws SQLException {
        List<Subscriber> customers =  subscriberService.getAllCustomers();
		return customers;
	}
	
	@GET
	@Path("/addSubscribers")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Subscriber> addSubscribers() throws SQLException {
		Connection c = null;
		 c = subscriberService.connect(c);
		 subscriberService.populateDB(c);
			c.close();
			return subscriberService.getAllCustomers();
	}

	@GET
	@Path("/cache")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Subscriber> getCustomersCache() throws SQLException {
		
			return subscriberService.getSubscribersCache();
	}

	
	@GET
	@Path("{searchedMsisdn}")
	@Produces(MediaType.APPLICATION_JSON)
	public Subscriber getSubscriber(@PathParam("searchedMsisdn") String searchedMsisdn) throws SQLException, ParseException{
		Subscriber foundSubscriber = null;
		
		try{
			foundSubscriber = subscriberService.searchSubscriberRedis(searchedMsisdn);
		
	if(foundSubscriber != null) {
		System.out.println("info found in redis");
	} else{
	foundSubscriber = subscriberService.searchSubscriberBD(searchedMsisdn);
		if(foundSubscriber != null){
			
			  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			  
			  Jedis jedis = new Jedis("localhost");
			  Long keysCount = jedis.dbSize();
				 Set<String> keys = jedis.keys("*");
				 if(keysCount == 20){
					 String keyToDelete = "";
					LocalDateTime minTime = LocalDateTime.now();
					 for(String key : keys){
						 String timestamp = jedis.hget(key, "timestamp");
						 LocalDateTime keyTime = LocalDateTime.parse(timestamp, formatter);
						 if(keyTime.isBefore(minTime)){
							minTime =  keyTime; 
							keyToDelete = key;
						 }
					 }
					 jedis.del(keyToDelete);
					 System.out.println("Deleted from cache: " + keyToDelete);
				
				 }

			  Map<String,String> subscriberDetails = new HashMap<String,String>();
			  subscriberDetails.put("start_date", foundSubscriber.getStartDate().toString());
			  subscriberDetails.put("end_date", foundSubscriber.getEndDate().toString());
			  subscriberDetails.put("group_profile", foundSubscriber.getGroupProfile());
			  subscriberDetails.put("timestamp", LocalDateTime.now().format(formatter));
			  
			  jedis.hmset(searchedMsisdn, subscriberDetails); 
		      System.out.println("redis new info added");
		}
	}
	if(foundSubscriber == null){
		System.out.println("ERROR: MSISDN not found in Redis or DB :" + searchedMsisdn);
	}
		return foundSubscriber;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	@GET
	@Path("/{msisdn}/switch")
	public Subscriber searchSubscriber(
			@QueryParam("location") int location,
			@PathParam("msisdn") String msisdn) throws SQLException, ParseException{
		Subscriber searchedSubscriber = new Subscriber();
		switch(location) {
		case 1: 
			if(subscriberService.searchSubscriberBD(msisdn) != null){
				System.out.println("Customer with msisdn: "+ msisdn + "is in the database");
				searchedSubscriber = subscriberService.searchSubscriberBD(msisdn);
			} else {
				System.out.println("Customer with msisdn: "+ msisdn + "it is not in the database");
			}
			
			break;
		
		case 2: 
			if(subscriberService.searchSubscriberRedis(msisdn) != null){
				System.out.println("Customer with msisdn: "+ msisdn + "is in Redis");
				searchedSubscriber = subscriberService.searchSubscriberRedis(msisdn);
			} else {
				System.out.println("Customer with msisdn: "+ msisdn + "it is not in Redis");
			}
			break;
		default: 
			System.out.println("Invalid command! use 1 for database or 2 for Redis!");
			break;
		
		}
		
		return searchedSubscriber;
	}
	
	
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Subscriber addCustomer(Subscriber newCustomer) throws SQLException{
		Connection c = null;
		 c = subscriberService.connect(c);
		Subscriber addedCustomer =  subscriberService.insertInDB(newCustomer,c);
		c.close();
		return addedCustomer;
	}
	
	@PUT
	@Path("/{msisdn}/{column}/{value}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Subscriber updateMessage(@PathParam("msisdn") String msisdn,
			@PathParam("column") String column,
			@PathParam("value") String value) throws SQLException{
		
		boolean update = subscriberService.updateDB(msisdn,column,value);
		System.out.println(update + " " + msisdn);
		if(update ==true) {
		subscriberService.updateRedis(msisdn,column,value);
		}
		
		return subscriberService.searchSubscriberBD(msisdn);
			
	}
	
	@DELETE
	@Path("/delete/{msisdn}")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteUser(@PathParam("msisdn") String msisdn){
		int result = subscriberService.deleteSubscriber(msisdn);
		
			if (result == 1){
			System.out.println("Delete successful");
			return "<result>success</result>";
		}
			return "<result>failed</result>";
	}
	
	@DELETE
	@Path("/deleteAll")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteUsers(){
		int result = subscriberService.deleteSubscribers();
		
			if (result == 1){
			System.out.println("Delete successful");
			return "<result>success</result>";
		}
			return "<result>failed</result>";
	}

	
	@DELETE
	@Path("/deleteCache")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteCache(){
		boolean result = subscriberService.deleteCache();
		
			if (result == true){
			return "<result>success</result>";
		}
			return "<result>failed</result>";
	}
}
