package org.gabi.postgresql.orangeproject.resource;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	@Path("{searchedMsisdn}")
	@Produces(MediaType.APPLICATION_JSON)
	public Customer getCustomer(@PathParam("searchedMsisdn") String searchedMsisdn) throws SQLException, ParseException{
		Customer foundCustomer = null;
		
		try{
		if(customerService.searchCustomerRedis(searchedMsisdn) != null){
	foundCustomer = customerService.searchCustomerRedis(searchedMsisdn);
		}
	if(foundCustomer != null) {
		System.out.println("info found in redis");
	} else{
	foundCustomer = customerService.searchCustomerBD(searchedMsisdn);
		if(foundCustomer != null){
			
			  Jedis jedis = new Jedis("localhost");

			  Map<String,String> customerDetails = new HashMap<String,String>();
			  customerDetails.put("start_date", foundCustomer.getStartDate().toString());
			  customerDetails.put("end_date", foundCustomer.getEndDate().toString());
			  customerDetails.put("group_profile", foundCustomer.getGroupProfile());
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
		return customerService.insertInDB(newCustomer);
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
	
}
