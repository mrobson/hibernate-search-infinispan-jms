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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import com.mrobson.example.search.slave.dao.SearchDao;

/**
 * @author <a href="mailto:mrobson@redhat.com">Matthew Robson</a>
 * 
 * May 15, 2015
 */

@WebServlet(name = "searchRead", value = { "/read" }, asyncSupported = true)
public class SearchRead extends HttpServlet {

	private static final long serialVersionUID = 8874103300546314746L;
	@EJB
    private SearchDao dao;

	@Override
    public void init() throws ServletException {}

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        int totalPeople = dao.readPeople();
        response.getWriter().print("Reading " + totalPeople  + " entries took (ms): " + (System.currentTimeMillis() - startTime));  
    }
}