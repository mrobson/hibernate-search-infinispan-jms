/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mrobson.example.search.slave.web;

import java.io.IOException;                                                                                                   

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.mrobson.example.search.slave.dao.SearchDao;

/**
 * @author <a href="mailto:mrobson@redhat.com">Matthew Robson</a>
 * 
 * May 15, 2015
 */

@WebServlet(name = "searchWrite", value = { "/write" }, initParams = {
		@WebInitParam(name = "totalToWrite", value = "10") }, asyncSupported = true)
public class SearchWrite extends HttpServlet {
	
	private static final long serialVersionUID = -6550908286647463562L;
	@EJB
    private SearchDao dao;
    private int peopleToAdd;
    
    @Override
    public void init() throws ServletException {
    	peopleToAdd = Integer.parseInt(getServletConfig().getInitParameter("totalToWrite"));
    }
    
    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        int peopleToAdd = this.peopleToAdd;
        if (request.getParameter("totalToWrite") != null) {
        	peopleToAdd = Integer.parseInt(request.getParameter("totalToWrite"));
        }
        try {
            dao.savePeople(peopleToAdd);
        } catch (InterruptedException e) {
        	response.getWriter().write("There was an issue saving the person: " + e.getMessage());
        }
    }
}