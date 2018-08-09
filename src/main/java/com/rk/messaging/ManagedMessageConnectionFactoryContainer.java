package com.rk.messaging;

import io.dropwizard.lifecycle.Managed;
import javax.jms.ConnectionFactory;

/**
 * Interface for message connection factory holder. Interface extends managed since connection factory may be pooled or
 * require explicit start and stop operations, so these operations can be called in accordance with application lifecycle.
 */
public interface ManagedMessageConnectionFactoryContainer extends Managed {
    ConnectionFactory getConnectionFactory();
}
