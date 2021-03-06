/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.galaxy.coordinator;

import com.google.common.collect.ImmutableList;
import com.proofpoint.galaxy.shared.AgentStatus;
import com.proofpoint.galaxy.shared.AgentStatusRepresentation;
import com.proofpoint.galaxy.shared.SlotStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static com.proofpoint.galaxy.coordinator.RepoHelper.MOCK_BINARY_REPO;
import static com.proofpoint.galaxy.coordinator.RepoHelper.MOCK_CONFIG_REPO;
import static com.proofpoint.galaxy.shared.AgentLifecycleState.OFFLINE;
import static com.proofpoint.galaxy.shared.AgentLifecycleState.ONLINE;
import static com.proofpoint.galaxy.shared.AssignmentHelper.APPLE_ASSIGNMENT;
import static com.proofpoint.galaxy.shared.SlotLifecycleState.STOPPED;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class TestAnnounceResource
{
    private AnnounceResource resource;
    private Coordinator coordinator;
    private AgentStatus agentStatus;

    @BeforeMethod
    public void setup()
            throws Exception
    {
        coordinator = new Coordinator(new MockRemoteAgentFactory(),
                MOCK_BINARY_REPO,
                MOCK_CONFIG_REPO,
                new LocalConfigRepository(new CoordinatorConfig(), null),
                new GitConfigRepository(new GitConfigRepositoryConfig(), null));
        resource = new AnnounceResource(coordinator);
        agentStatus = new AgentStatus(UUID.randomUUID(),
                ONLINE,
                URI.create("fake://foo/"),
                ImmutableList.of(new SlotStatus(UUID.randomUUID(), "foo", URI.create("fake://foo"), STOPPED, APPLE_ASSIGNMENT)));
    }

    @Test
    public void testUpdateAgentStatus()
    {
        coordinator.updateAgentStatus(agentStatus);
        AgentStatus newFooAgent = new AgentStatus(UUID.randomUUID(),
                ONLINE,
                URI.create("fake://foo/"),
                ImmutableList.of(
                        new SlotStatus(UUID.randomUUID(), "foo", URI.create("fake://foo"), STOPPED, APPLE_ASSIGNMENT),
                        new SlotStatus(UUID.randomUUID(), "moo", URI.create("fake://moo"), STOPPED, APPLE_ASSIGNMENT)));

        Response response = resource.updateAgentStatus(newFooAgent.getAgentId(), AgentStatusRepresentation.from(newFooAgent, URI.create("http://localhost/v1/agent")));

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertNull(response.getEntity());
        AgentStatus actualStatus = coordinator.getAgentStatus(agentStatus.getAgentId());
        assertEquals(actualStatus, agentStatus);
        assertEquals(actualStatus.getState(), ONLINE);
    }

    @Test
    public void testAgentOffline()
    {
        coordinator.updateAgentStatus(agentStatus);

        Response response = resource.agentOffline(agentStatus.getAgentId());
        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertNull(response.getEntity());

        AgentStatus actualStatus = coordinator.getAgentStatus(agentStatus.getAgentId());
        assertEquals(actualStatus.getState(), OFFLINE);
    }

    @Test
    public void testRemoveAgentStatusMissing()
    {
        Response response = resource.agentOffline(UUID.randomUUID());
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertNull(response.getEntity());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRemoveAgentStatusNullId()
    {
        resource.agentOffline(null);
    }
}
