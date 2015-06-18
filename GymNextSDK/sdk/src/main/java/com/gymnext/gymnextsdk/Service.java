package com.gymnext.gymnextsdk;

/**
 * Base interface for all services.
 */
public interface Service
{
    /**
     * All services must have a unique identifier
     * @return
     * The services unique identifier
     */
    public String getId();
}
