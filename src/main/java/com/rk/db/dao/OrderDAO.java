package com.rk.db.dao;

import com.rk.api.Order;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

public class OrderDAO extends AbstractDAO<Order> {
    public OrderDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public int create(Order order) {
        return persist(order).getId();
    }

    public void update(Order order) {
        persist(order);
    }

    public Order findById(int id) {
        return get(id);
    }
}
