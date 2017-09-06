package org.gabi.postgresql.orangeproject.service;

import java.sql.Connection;
import org.fluttercode.datafactory.impl.DataFactory;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;


import org.gabi.postgresql.orangeproject.model.Customer;

import redis.clients.jedis.Jedis;
/*
 * TASKS:
 * 
 */
public class CustomerService {
	
	//private CustomerService customerService = new CustomerService();
	private Connection c = null;
	private Random random;
	private DataFactory df = new DataFactory();


	public void close(PreparedStatement preparedStatement, Connection c) {
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (c != null) {
			try {
				c.close();
				System.out.println("Connection closed!");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}
	
	public void close(Statement statement, Connection c) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (c != null) {
			try {
				c.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}
	
	public Connection connect(Connection c) {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			c = DriverManager
					.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "12345");
	         System.out.println("Opened database successfully");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	
		  try {
			c.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		  if(checkDB(c)){
			  System.out.println("DB is already populated");
		  }
		  
			return c;
	}
	
	public boolean checkDB(Connection c){
		  PreparedStatement stmt = null;
	         ResultSet resultSet = null;
             boolean empty = false;
             try {
	         String qry = "SELECT count(*) from CUSTOMERS ";
	       
	             stmt = (PreparedStatement) c.prepareStatement(qry);
	             resultSet =  stmt.executeQuery();
	             resultSet.next();
	             System.out.println(resultSet.getInt(1));
	             if(resultSet.getInt(1) == 0){
	            	 System.out.println("Database is empty! Let's populate it!");
	            	 populateDB(c);
	            	 empty = false;
	             } else {
	            	 empty = true;
	             }
					resultSet.close();
					
	         } catch(SQLException e){
	        	 e.printStackTrace();
	         }
	         
	        
	         return empty;
	}
	
	
	public void populateDB(Connection c){
	
			Date minDate;
			Date maxDate;
			Date start_date ;
			String group_profile;
			Date end_date;
			Date minDateEnd;
			Date maxDateEnd;
			Customer newCustomer;

			try {
				 minDate = new Date();
				 maxDate= new Date();
				 minDateEnd = new Date();
				 maxDateEnd = new Date();
				 start_date = new Date();
				 group_profile = "";
				 end_date = new Date();
					String msisdn = "";
					random = new Random();
					
					for(int index = 1;index <= 2000;index++){
						msisdn = "40";
								for (int i = 0; i < 10; i++) {
							msisdn = msisdn + random.nextInt(10);
								}
								
				minDate = new SimpleDateFormat( "yyyy-mm-dd" ).parse( "2000-05-20" );
				maxDate = new SimpleDateFormat( "yyyy-mm-dd" ).parse( "2013-05-20" );
				start_date = df.getDateBetween(minDate, maxDate);
			 
				minDateEnd = new SimpleDateFormat( "yyyy-mm-dd" ).parse( "2013-05-20" );
				maxDateEnd = new SimpleDateFormat( "yyyy-mm-dd" ).parse( "2020-06-20" );
				end_date = df.getDateBetween(minDateEnd, maxDateEnd);
					
			int profile = random.nextInt(2);
			if(profile == 0) {
				group_profile = "UP";
			} else {
				group_profile = "DOWN";
			}
			 newCustomer = new Customer(msisdn,start_date,end_date,group_profile);
			insertInDB(newCustomer,c);
					
					}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		
	}
	
	
	public List<Customer> getAllCustomers() throws SQLException{
	c = null;
		Statement stmt = null;
		List<Customer> customers = new ArrayList<>();
		try { 
				c= connect(c);
		         stmt = c.createStatement();
		         ResultSet rs = stmt.executeQuery( "SELECT * FROM CUSTOMERS;" );
		         while ( rs.next() ) {
		        	 	String msisdn = rs.getString(1);
		        	 	Date startDate = rs.getDate(2);
		        	 	Date endDate = rs.getDate(3);
		        	 	String groupProfile = rs.getString(4);
		        	 	Customer customer = new Customer(msisdn,startDate,endDate,groupProfile);
		        	 	customers.add(customer);
		        	 	
		         }
		        rs.close();
		         
		         
		} catch ( Exception e ) {
		   e.printStackTrace();
		 } finally { close(stmt,c);

		 }
		
		return customers;
}
	
	
	public Customer searchCustomerBD(String msisdnToGet) throws SQLException{
		PreparedStatement preparedStatement = null;
	 c = null;
		Customer customer  = null;
		String selectSQL = "SELECT * FROM CUSTOMERS WHERE MSISDN = ?";
		
		try {
			c = connect(c);
		         
		         preparedStatement = c.prepareStatement(selectSQL);
		 		preparedStatement.setString(1, msisdnToGet);
		 		
		         ResultSet rs = preparedStatement.executeQuery();
		         if(rs.next()){
		         String msisdn = rs.getString(1);
	        	 	Date startDate = rs.getDate(2);
	        	 	Date endDate = rs.getDate(3);
	        	 	String groupProfile = rs.getString(4);
	        	 	 customer = new Customer(msisdn,startDate,endDate,groupProfile);
		 		}   
		         rs.close();
		} catch ( Exception e ) {
		   e.printStackTrace();
		 } finally {		
			 close(preparedStatement,c);
			}
		
		return customer;
	}
	
	
	public Customer searchCustomerRedis(String msisdnToGet) throws ParseException{
		Customer customer  = null;
	
		  Jedis jedis = new Jedis("localhost"); 
		  System.out.println("Server is running: "+jedis.ping()); 

		
		  if(jedis.exists(msisdnToGet)){
			  Map<String, String> hashed = jedis.hgetAll(msisdnToGet);
		  
		  if(hashed != null) {
		  String msisdn = msisdnToGet;
		  DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
  	 	Date startDate =format.parse(hashed.get("start_date"));
  	 	Date endDate = format.parse(hashed.get("end_date"));
  	 	String groupProfile = hashed.get("group_profile");
  	 	
  	  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  	 hashed.put("timestamp", LocalDateTime.now().format(formatter));
	  jedis.hmset(msisdnToGet, hashed);

	  
		  customer = new Customer(msisdn,startDate,endDate,groupProfile);
		  } else{
			  System.out.println("Searched Customer was found in Redis but has no information");
		  }
	 } else {System.out.println("Searched Customer doesn't exists in Redis!");
	 }
	 
		return customer;
	}
	

	public Customer insertInDB(Customer newCustomer,Connection c) throws SQLException{
	      PreparedStatement stmt = null;
	      try {
	       
	       
	         String sql = "INSERT INTO CUSTOMERS (MSISDN, START_DATE, END_DATE, GROUP_PROFILE) "
	                 + "VALUES (?,?,?,?)";
	         
	         stmt = c.prepareStatement(sql);
	         stmt.setString(1, newCustomer.getMsisdn());
	         stmt.setDate(2,  new java.sql.Date(newCustomer.getStartDate().getTime()));
	         stmt.setDate(3, new java.sql.Date(newCustomer.getEndDate().getTime()));
	         stmt.setString(4, newCustomer.getGroupProfile());
	         
	         stmt.executeUpdate();	
	         c.commit();
    } catch (Exception e) {

    	e.printStackTrace();
     } 
  
   return newCustomer;
}
	
	
	public boolean updateDB(String msisdn, String column, String value){
		c= null;
	      PreparedStatement stmt = null;
	      
	      try{
	    	 c = connect(c);
	    	  String sql = "UPDATE CUSTOMERS SET " + column + " = ? WHERE MSISDN = ?;";
	    	  stmt= c.prepareStatement(sql);
	    	  stmt.setString(2, msisdn);
	    	  if(column.equalsIgnoreCase("START_DATE") || column.equalsIgnoreCase("END_DATE")){
		    	  DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	    		  Date newDate = format.parse(value);
	    		  stmt.setDate(1, new java.sql.Date(newDate.getTime()));
	    		 
	    	  }
	    	  else{
	    		  stmt.setString(1, value);
	    	  }
	    		stmt.executeUpdate();
	    		c.commit();
	    		return true;
	      } catch(Exception e){
	    	  e.printStackTrace();
	    	  return false;
	      }
	      
	   
	}
	
	public void updateRedis(String msisdn, String column, String value){
		
		Jedis jedis = new Jedis("localhost"); 
		  System.out.println("Server is running: "+jedis.ping()); 
		  if(jedis.exists(msisdn)){
			jedis.hset(msisdn, column, value);
			  }
		   else {
			  System.out.println("The modified values from database doesn't exist in redis, yet");
		  }
	}
	
	
	public int deleteUser(String msisdn){
		c= null;
	      PreparedStatement stmt = null;
	      int result = 0;
	      try{
	    	 c = connect(c);
	    	 
	    	 String sql =" DELETE FROM CUSTOMERS WHERE MSISDN = ?;";
	    	  stmt= c.prepareStatement(sql);
	    	  stmt.setString(1,msisdn);
	    	result =  stmt.executeUpdate();
	    		c.commit();
	    		
	    		Jedis jedis = new Jedis("localhost"); 
	  		  System.out.println("Server is running: "+jedis.ping()); 
	  		  if(jedis.exists(msisdn)){
	  			  jedis.del(msisdn);
	  		  }
	  		  else{
	  			  System.out.println("User to delete doesn't exists in cache");
	  		  }
	  			  
	  			
	      }catch(Exception e){
	    	 e.printStackTrace();
	      }
	      return result;		
	}
	
	
	
	public int deleteUsers(){
		c= null;
	      PreparedStatement stmt = null;
	      int result = 0;
	      try{
	    	 c = connect(c);
	    	 
	    	 String sql =" DELETE FROM CUSTOMERS;";
	    	  stmt= c.prepareStatement(sql);
	    	 
	    	result =  stmt.executeUpdate();
	    		c.commit();
	    		
	    		Jedis jedis = new Jedis("localhost"); 
	  		  System.out.println("Server is running: "+jedis.ping()); 
	  		Set<String> keys = jedis.keys("*");
	  		for (String key : keys) {
	  		    jedis.del(key);
	  		} 
	  			
	      }catch(Exception e){
	    	 e.printStackTrace();
	      }
	      return result;
		
	}
}
