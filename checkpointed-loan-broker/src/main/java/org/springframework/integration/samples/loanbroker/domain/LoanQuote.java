/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.samples.loanbroker.domain;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * @author Oleg Zhurakousky
 */
public class LoanQuote implements Comparable<LoanQuote>, Serializable{

	private String lender;
	private Date quoteDate;
	private Date expirationDate;
	private double amount;
	private int term;
	private float rate;

	public String getLender() {
		return lender;
	}

	public void setLender(String lender) {
		this.lender = lender;
	}

	public Date getQuoteDate() {
		return quoteDate;
	}

	public void setQuoteDate(Date quoteDate) {
		this.quoteDate = quoteDate;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public int getTerm() {
		return term;
	}

	public void setTerm(int term) {
		this.term = term;
	}

	public float getRate() {
		return rate;
	}

	public void setRate(float rate) {
		this.rate = rate;
	}

	public String toString(){
		return "\n====== Loan Quote =====\n" +
			   "Lender: " + this.lender + "\n" +
			   "Loan amount: " + new DecimalFormat("$###,###.###").format(this.amount) + "\n" + 
			   "Quote Date: " + this.quoteDate + "\n" +
			   "Expiration Date: " + this.expirationDate + "\n" + 
			   "Term: " + this.term + " years" + "\n" +
			   "Rate: " + this.rate + "%\n" + 
			   "=======================\n";
	}

	public int compareTo(LoanQuote other) {
		if (this.rate > other.rate) {
			return 1;
		}
		else if (this.rate < other.rate) {
			return -1;
		}
		return 0;
	}

}
