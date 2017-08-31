package org.gabi.postgresql.orangeproject.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.gabi.postgresql.orangeproject.model.Customer;

import redis.clients.jedis.Jedis;

public class CustomerService {
	
	
	private Connection c = null;


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
		} catch (SQLException e) {
			e.printStackTrace();
		}
	
		  try {
			c.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	         System.out.println("Opened database successfully");
			return c;
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
		    System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		    System.exit(0);
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
	        	 	System.out.println(startDate);
	        	 	Date endDate = rs.getDate(3);
	        	 	String groupProfile = rs.getString(4);
	        	 	 customer = new Customer(msisdn,startDate,endDate,groupProfile);
		 		}   
		         rs.close();
		} catch ( Exception e ) {
		    System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		    System.exit(0);
		 } finally {		close(preparedStatement,c);
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
		  
		  customer = new Customer(msisdn,startDate,endDate,groupProfile);
		  } else{
			  System.out.println("Searched Customer was found in Redis but has no information");
		  }
	 } else {System.out.println("Searched Customer doesn't exists in Redis!");
	 }
	 
		return customer;
	}
	
	
	public Customer insertInDB(Customer newCustomer) throws SQLException{
		 c = null;
	      PreparedStatement stmt = null;
	      try {
	        c = connect(c);
	       
	         String sql = "INSERT INTO CUSTOMERS (MSISDN, START_DATE, END_DATE, GROUP_PROFILE) "
	                 + "VALUES (?,?,?,?)";
	         
	System.out.println(newCustomer);
	         stmt = c.prepareStatement(sql);
	         stmt.setString(1, newCustomer.getMsisdn());
	         stmt.setDate(2,  new java.sql.Date(newCustomer.getStartDate().getTime()));
	         stmt.setDate(3, new java.sql.Date(newCustomer.getEndDate().getTime()));
	         stmt.setString(4, newCustomer.getGroupProfile());
	         
	         stmt.executeUpdate();	
	         c.commit();
    } catch (Exception e) {

    	e.printStackTrace();
     } finally{ 
    	 close(stmt,c);
     }
     System.out.println("Records created successfully");
   return newCustomer;
}
	
	
	public boolean updateDB(String msisdn, String column, String value){
		c= null;
	      PreparedStatement stmt = null;
	      
	      try{
	    	 c = connect(c);
	    	  String sql = "UPDATE CUSTOMERS SET " + column + " = ? WHERE MSISDN = ?;";
	    	  System.out.println("value :" + value);
	    	  stmt= c.prepareStatement(sql);
	    	  System.out.println(msisdn);
	    	  stmt.setString(2, msisdn);
	    	  System.out.println(column);
	    	  if(column.equalsIgnoreCase("START_DATE") || column.equalsIgnoreCase("END_DATE")){
	    		  System.out.println("date");
		    	  DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	    		  Date newDate = format.parse(value);
	    		  System.out.println(newDate);
	    		  System.out.println(newDate.getTime());

	    		  stmt.setDate(1, new java.sql.Date(newDate.getTime()));
	    		 
	    	  }
	    	  else{
	    		  stmt.setString(1, value);
	    		 System.out.println(value);
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
}
