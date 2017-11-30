package com.konkerlabs.platform.registry.api.test.web.controller;

import com.konkerlabs.platform.registry.api.model.DeviceVO;
import com.konkerlabs.platform.registry.api.test.config.GatewayTestConfiguration;
import com.konkerlabs.platform.registry.api.test.config.MongoTestConfig;
import com.konkerlabs.platform.registry.api.test.config.WebTestConfiguration;
import com.konkerlabs.platform.registry.api.web.controller.DeviceRestController;
import com.konkerlabs.platform.registry.api.web.controller.DeviceStatusRestController;
import com.konkerlabs.platform.registry.api.web.wrapper.CrudResponseAdvice;
import com.konkerlabs.platform.registry.business.model.*;
import com.konkerlabs.platform.registry.business.model.Event.EventActor;
import com.konkerlabs.platform.registry.business.model.HealthAlert.Description;
import com.konkerlabs.platform.registry.business.model.HealthAlert.HealthAlertSeverity;
import com.konkerlabs.platform.registry.business.model.HealthAlert.HealthAlertType;
import com.konkerlabs.platform.registry.business.services.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {DeviceRestController.class, DeviceStatusRestController.class})
@AutoConfigureMockMvc(secure = false)
@ContextConfiguration(classes = {
        WebTestConfiguration.class,
        MongoTestConfig.class,
        GatewayTestConfiguration.class,
        CrudResponseAdvice.class
})
public class DeviceByGatewayRestControllerTest extends WebLayerTestContext {

    private static final String NONEXIST_APPLICATION_NANE = "AppLost";

    @Autowired
    private DeviceRegisterService deviceRegisterService;

    @Autowired
    private LocationSearchService locationSearchService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private DeviceEventService deviceEventService;

    @Autowired
    private HealthAlertService healthAlertService;

    @Autowired
    private Tenant tenant;

    @Autowired
    private Application application;

    @Autowired
    private Gateway gateway;

    @Autowired
    private GatewayService gatewayService;

    private Set<String> tags;

    private Device device1;

    private Device device2;

    private HealthAlert health1;

    private HealthAlert health2;

    private List<HealthAlert> healths;

    private List<Event> events;

    private String BASEPATH = "devices";

    private Instant registrationDate = Instant.ofEpochMilli(1495716970000l).minusSeconds(3600l);

    @Before
    public void setUp() {
        final Location locationBR = Location.builder().name("br").build();

        tags = new HashSet<>(Arrays.asList("tag1", "tag2"));
        device1 = Device.builder()
                .deviceId("id1")
                .name("name1")
                .guid("guid1")
                .location(locationBR)
                .application(application)
                .active(true)
                .registrationDate(registrationDate)
                .lastModificationDate(registrationDate)
                .tags(tags)
                .build();
        device2 = Device.builder().deviceId("id2").name("name2").guid("guid2").location(locationBR).application(application).active(false).build();


        health1 = HealthAlert.builder()
                .guid("7d51c242-81db-11e6-a8c2-0746f976f223")
                .severity(HealthAlertSeverity.FAIL)
                .description(Description.NO_MESSAGE_RECEIVED)
                .registrationDate(registrationDate)
                .lastChange(Instant.ofEpochMilli(1495716970000l))
                .type(HealthAlertType.SILENCE)
                .deviceGuid(device1.getGuid())
                .triggerGuid("7d51c242-81db-11e6-a8c2-0746f976f666")
                .build();

        health2 = HealthAlert.builder()
                .guid("7d51c242-81db-11e6-a8c2-0746f976f223")
                .severity(HealthAlertSeverity.OK)
                .description(Description.NO_MESSAGE_RECEIVED)
                .registrationDate(registrationDate)
                .lastChange(Instant.ofEpochMilli(1495716970000l))
                .type(HealthAlertType.SILENCE)
                .deviceGuid(device1.getGuid())
                .triggerGuid("7d51c242-81db-11e6-a8c2-0746f976f666")
                .build();

        healths = Arrays.asList(health1, health2);

        Event event = Event.builder()
                .incoming(EventActor.builder().channel("out").deviceGuid(device1.getGuid()).build())
                .creationTimestamp(registrationDate)
                .build();
        events = Collections.singletonList(event);

        when(locationSearchService.findByName(tenant, application, "br", false))
                .thenReturn(ServiceResponseBuilder.<Location>ok().withResult(locationBR).build());

    }

    @After
    public void tearDown() {
        Mockito.reset(deviceRegisterService);
    }


    @Test
    public void shouldCreateDeviceByGateway() throws Exception {
        Location br =
                Location.builder()
                        .application(application)
                        .guid("f06d9d2d-f5ce-4cc6-8637-348743e8acad")
                        .id("br")
                        .name("br")
                        .description("br")
                        .build();

        Location room1 =
                Location.builder()
                        .application(application)
                        .guid("f06d9d2d-f5ce-4cc6-8637-348743e8acae")
                        .id("sala-101")
                        .name("sala-101")
                        .description("sala-101")
                        .parent(br)
                        .build();

        Location room101Roof = Location.builder()
                .tenant(tenant)
                .application(application)
                .parent(room1)
                .name("sala-101-teto")
                .guid("f06d9d2d-f5ce-4cc6-8637-348743e8acaf")
                .parent(room1)
                .build();

        room1.setChildren(Arrays.asList(room101Roof));
        br.setChildren(Arrays.asList(room1));

        when(deviceRegisterService.register(org.mockito.Matchers.any(Tenant.class), org.mockito.Matchers.any(Application.class), org.mockito.Matchers.any(Device.class)))
                .thenReturn(ServiceResponseBuilder.<Device>ok().withResult(device1).build());

        when(applicationService.getByApplicationName(tenant, application.getName()))
                .thenReturn(ServiceResponseBuilder.<Application>ok().withResult(application).build());

        when(gatewayService.validateGatewayAuthorization(gateway, device1.getLocation()))
                .thenReturn(ServiceResponseBuilder.<Boolean>ok().withResult(Boolean.TRUE).build());

        getMockMvc().perform(MockMvcRequestBuilders.post(
                MessageFormat.format("/{0}/{1}/",
                        application.getName(),
                        BASEPATH
                ))
                .content(getJson(new DeviceVO().apply(device1)))
                .contentType("application/json")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.code", is(HttpStatus.CREATED.value())))
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.timestamp", greaterThan(1400000000)))
                .andExpect(jsonPath("$.result").isMap())
                .andExpect(jsonPath("$.result.id", is("id1")))
                .andExpect(jsonPath("$.result.name", is("name1")))
                .andExpect(jsonPath("$.result.guid", is("guid1")))
                .andExpect(jsonPath("$.result.locationName", is("br")))
                .andExpect(jsonPath("$.result.active", is(true)));

    }


}
