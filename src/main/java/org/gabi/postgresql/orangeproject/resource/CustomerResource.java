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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.gabi.postgresql.orangeproject.model.Customer;
import org.gabi.postgresql.orangeproject.service.CustomerService;

import redis.clients.jedis.Jedis;

@Path("/customers")
public class CustomerResource {


	CustomerService customerService = new CustomerService();
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Customer> getCustomers() throws SQLException {
		return customerService.getAllCustomers();
		
	}
	
	@GET
	@Path("/addSubscribers")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Customer> addCustomers() throws SQLException {
		Connection c = null;
		 c = customerService.connect(c);
		 customerService.populateDB(c);
			c.close();
			return customerService.getAllCustomers();
	}


	
	@GET
	@Path("{searchedMsisdn}")
	@Produces(MediaType.APPLICATION_JSON)
	public Customer getCustomer(@PathParam("searchedMsisdn") String searchedMsisdn) throws SQLException, ParseException{
		Customer foundCustomer = null;
		
		try{
			foundCustomer = customerService.searchCustomerRedis(searchedMsisdn);
		
	if(foundCustomer != null) {
		System.out.println("info found in redis");
	} else{
	foundCustomer = customerService.searchCustomerBD(searchedMsisdn);
		if(foundCustomer != null){
			
			  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			  
			  Jedis jedis = new Jedis("localhost");
			  Long keysCount = jedis.dbSize();
				 Set<String> keys = jedis.keys("*");
				 if(keysCount == 2){
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

			  Map<String,String> customerDetails = new HashMap<String,String>();
			  customerDetails.put("start_date", foundCustomer.getStartDate().toString());
			  customerDetails.put("end_date", foundCustomer.getEndDate().toString());
			  customerDetails.put("group_profile", foundCustomer.getGroupProfile());
			  customerDetails.put("timestamp", LocalDateTime.now().format(formatter));
			  
			  jedis.hmset(searchedMsisdn, customerDetails); 
		      System.out.println("redis new info added");
		}
	}
	if(foundCustomer == null){
		System.out.println("ERROR: MSISDN not found in Redis or DB :" + searchedMsisdn);
	}
		return foundCustomer;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	@GET
	@Path("/{msisdn}/switch")
	public Customer searchCustomer(
			@QueryParam("location") int location,
			@PathParam("msisdn") String msisdn) throws SQLException, ParseException{
		Customer searchedCustomer = new Customer();
		switch(location) {
		case 1: 
			if(customerService.searchCustomerBD(msisdn) != null){
				System.out.println("Customer with msisdn: "+ msisdn + "is in the database");
				searchedCustomer = customerService.searchCustomerBD(msisdn);
			} else {
				System.out.println("Customer with msisdn: "+ msisdn + "it is not in the database");
			}
			
			break;
		
		case 2: 
			if(customerService.searchCustomerRedis(msisdn) != null){
				System.out.println("Customer with msisdn: "+ msisdn + "is in Redis");
				searchedCustomer = customerService.searchCustomerRedis(msisdn);
			} else {
				System.out.println("Customer with msisdn: "+ msisdn + "it is not in Redis");
			}
			break;
		default: 
			System.out.println("Invalid command! use 1 for database or 2 for Redis!");
			break;
		
		}
		
		return searchedCustomer;
	}
	
	
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Customer addCustomer(Customer newCustomer) throws SQLException{
		Connection c = null;
		 c = customerService.connect(c);
		Customer addedCustomer =  customerService.insertInDB(newCustomer,c);
		c.close();
		return addedCustomer;
	}
	
	@PUT
	@Path("/{msisdn}/{column}/{value}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Customer updateMessage(@PathParam("msisdn") String msisdn,
			@PathParam("column") String column,
			@PathParam("value") String value) throws SQLException{
		
		boolean update = customerService.updateDB(msisdn,column,value);
		System.out.println(update + " " + msisdn);
		if(update ==true) {
		customerService.updateRedis(msisdn,column,value);
		}
		
		return customerService.searchCustomerBD(msisdn);
			
	}
	
	@DELETE
	@Path("/delete/{msisdn}")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteUser(@PathParam("msisdn") String msisdn){
		int result = customerService.deleteUser(msisdn);
		
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
		int result = customerService.deleteUsers();
		
			if (result == 1){
			System.out.println("Delete successful");
			return "<result>success</result>";
		}
			return "<result>failed</result>";
	}
	
}
