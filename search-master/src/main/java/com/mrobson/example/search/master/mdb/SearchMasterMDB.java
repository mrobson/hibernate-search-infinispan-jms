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
package com.mrobson.example.search.master.mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.MessageListener;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.search.backend.impl.jms.AbstractJMSHibernateSearchController;
import org.jboss.ejb3.annotation.ResourceAdapter;

/**
 * @author <a href="mailto:mrobson@redhat.com">Matthew Robson</a>
 * 
 *         May 15, 2015
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "/queue/indexupdates"),
		@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "24"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
		@ActivationConfigProperty(propertyName = "reconnectAttempts", propertyValue = "-1"),
		@ActivationConfigProperty(propertyName = "retryInterval", propertyValue = "1000"),
		@ActivationConfigProperty(propertyName = "hA", propertyValue = "true") })
@ResourceAdapter("hornetq-ra")
public class SearchMasterMDB extends AbstractJMSHibernateSearchController
		implements MessageListener {

	@PersistenceContext(unitName = "search-pu")
	EntityManager em;

	protected Session getSession() {
		return (Session) em.getDelegate();
	}

	protected void cleanSessionIfNeeded(Session session) {
	}
}