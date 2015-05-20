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
package com.mrobson.example.search.slave.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.MassIndexer;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.QueryBuilder;

import com.mrobson.example.search.datamodel.Person;

/**
 * @author <a href="mailto:mrobson@redhat.com">Matthew Robson</a>
 * 
 *         May 15, 2015
 */

@Stateless
public class SearchDao {

	@PersistenceContext(unitName = "search-pu")
	private EntityManager em;

	private FullTextEntityManager fullTextEntityManager;

	public void savePeople(int peopleToAdd) throws InterruptedException {
		while (peopleToAdd-- > 0) {
			Person ps = new Person();
			ps.setFirstName("Matt");
			ps.setLastName("Robson");
			em.persist(ps);
		}
	}

	public int readPeople() {
		fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Person.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onField("firstName").matching("Matt").createQuery();
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Person.class);
		List<Person> result = (List<Person>) jpaQuery.getResultList();
		return result.size();
	}

	public void reindexPeople() throws InterruptedException {
		fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(em);
		MassIndexer massidxer = fullTextEntityManager.createIndexer();
		massidxer.typesToIndexInParallel(1);
		massidxer.batchSizeToLoadObjects(10);
		massidxer.threadsToLoadObjects(20);
		massidxer.startAndWait();
	}
}