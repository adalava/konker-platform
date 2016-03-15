package com.konkerlabs.platform.registry.business.services.api;


import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.Event;

import java.io.IOException;

public interface EnrichmentExecutor {

    ServiceResponse<Event> enrich(Event incomingEvent, Device device);
}