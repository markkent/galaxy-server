package com.proofpoint.galaxy.console;

import com.google.common.collect.ImmutableList;
import com.proofpoint.galaxy.AgentStatus;
import com.proofpoint.galaxy.LifecycleState;
import com.proofpoint.galaxy.MockUriInfo;
import com.proofpoint.galaxy.SlotStatus;
import com.proofpoint.galaxy.SlotStatusRepresentation;
import com.proofpoint.units.Duration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.proofpoint.galaxy.ExtraAssertions.assertEqualsNoOrder;
import static com.proofpoint.galaxy.LifecycleState.UNASSIGNED;
import static com.proofpoint.galaxy.LifecycleState.UNKNOWN;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class TestConsoleSlotResource
{
    private ConsoleSlotResource resource;
    private Console console;

    @BeforeMethod
    public void setUp()
            throws Exception
    {
        console = new Console(new MockRemoteSlotFactory(), new ConsoleConfig().setStatusExpiration(new Duration(100, TimeUnit.DAYS)));
        resource = new ConsoleSlotResource(console);
    }

    @Test
    public void testGetAllSlots()
    {
        SlotStatus slot1 = new SlotStatus(UUID.randomUUID(), "slot1", URI.create("fake://localhost/v1/slot/" + "slot1"));
        SlotStatus slot2 = new SlotStatus(UUID.randomUUID(), "slot2", URI.create("fake://localhost/v1/slot/" + "slot2"));
        AgentStatus agentStatus = new AgentStatus(UUID.randomUUID(), ImmutableList.of(slot1, slot2));
        console.updateAgentStatus(agentStatus);

        URI requestUri = URI.create("http://localhost/v1/slot");
        Response response = resource.getAllSlots(MockUriInfo.from(requestUri));
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEqualsNoOrder((Iterable<?>) response.getEntity(), ImmutableList.of(SlotStatusRepresentation.from(slot1), SlotStatusRepresentation.from(slot2)));
        assertNull(response.getMetadata().get("Content-Type")); // content type is set by jersey based on @Produces
    }

    @Test
    public void testGetAllSlotsWithFilter()
    {
        SlotStatus slot1 = new SlotStatus(UUID.randomUUID(), "slot1", URI.create("fake://foo/v1/slot/" + "slot1"));
        SlotStatus slot2 = new SlotStatus(UUID.randomUUID(), "slot2", URI.create("fake://bar/v1/slot/" + "slot2"));
        AgentStatus agentStatus = new AgentStatus(UUID.randomUUID(), ImmutableList.of(slot1, slot2));
        console.updateAgentStatus(agentStatus);

        URI requestUri = URI.create("http://localhost/v1/slot?host=foo");
        Response response = resource.getAllSlots(MockUriInfo.from(requestUri));
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEqualsNoOrder((Iterable<?>) response.getEntity(), ImmutableList.of(SlotStatusRepresentation.from(slot1)));
        assertNull(response.getMetadata().get("Content-Type")); // content type is set by jersey based on @Produces
    }

    @Test
    public void testGetAllSlotEmpty()
    {
        URI requestUri = URI.create("http://localhost/v1/slot?state=unassigned");
        Response response = resource.getAllSlots(MockUriInfo.from(requestUri));
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEqualsNoOrder((Iterable<?>) response.getEntity(), ImmutableList.of());
        assertNull(response.getMetadata().get("Content-Type")); // content type is set by jersey based on @Produces
    }
}
