package org.gabi.postgresql.orangeproject.model;

import java.util.Date;

public class Subscriber {

	
	private String msisdn;
	private Date startDate;
	private Date endDate;
	private String groupProfile;
	
	public Subscriber() {
		
	}
	
	public Subscriber(String msisdn, Date startDate, Date endDate, String groupProfile) {
		super();
		this.msisdn = msisdn;
		this.startDate = startDate;
		this.endDate = endDate;
		this.groupProfile = groupProfile;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public String getGroupProfile() {
		return groupProfile;
	}
	public void setGroupProfile(String groupProfile) {
		this.groupProfile = groupProfile;
	}
	
	
}
