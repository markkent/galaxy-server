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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.proofpoint.galaxy.shared.AgentStatus;
import com.proofpoint.galaxy.shared.Assignment;
import com.proofpoint.galaxy.shared.AssignmentRepresentation;
import com.proofpoint.galaxy.shared.Installation;
import com.proofpoint.galaxy.shared.SlotStatus;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Collections2.transform;
import static com.proofpoint.galaxy.shared.SlotStatusRepresentation.fromSlotStatus;

@Path("/v1/slot")
public class CoordinatorSlotResource
{
    private final Coordinator coordinator;
    private final BinaryRepository binaryRepository;
    private final ConfigRepository configRepository;
    private final LocalConfigRepository localConfigRepository;
    private final GitConfigRepository gitConfigRepository;

    @Inject
    public CoordinatorSlotResource(Coordinator coordinator, BinaryRepository binaryRepository, ConfigRepository configRepository, LocalConfigRepository localConfigRepository, GitConfigRepository gitConfigRepository)
    {
        Preconditions.checkNotNull(coordinator, "coordinator must not be null");
        Preconditions.checkNotNull(configRepository, "repository is null");
        Preconditions.checkNotNull(binaryRepository, "binaryRepository is null");
        Preconditions.checkNotNull(localConfigRepository, "localConfigRepository is null");
        Preconditions.checkNotNull(gitConfigRepository, "gitConfigRepository is null");

        this.coordinator = coordinator;
        this.binaryRepository = binaryRepository;
        this.configRepository = configRepository;
        this.localConfigRepository = localConfigRepository;
        this.gitConfigRepository = gitConfigRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSlots(@Context UriInfo uriInfo)
    {
        Predicate<SlotStatus> slotFilter = SlotFilterBuilder.build(uriInfo, false);
        List<SlotStatus> result = coordinator.getAllSlotsStatus(slotFilter);

        return Response.ok(transform(result, fromSlotStatus())).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response install(
            AssignmentRepresentation assignmentRepresentation,
            @DefaultValue("1") @QueryParam("limit") int limit,
            @Context UriInfo uriInfo)
    {
        Preconditions.checkNotNull(assignmentRepresentation, "assignmentRepresentation must not be null");
        Preconditions.checkArgument(limit > 0, "limit must be at least 1");

        Assignment assignment = assignmentRepresentation.toAssignment();
        Map<String,URI> configMap = localConfigRepository.getConfigMap(assignment.getConfig());
        if (configMap == null) {
            configMap = gitConfigRepository.getConfigMap(assignment.getConfig());
        }
        if (configMap == null) {
            configMap = configRepository.getConfigMap(assignment.getConfig());
        }

        Installation installation = new Installation(assignment, binaryRepository.getBinaryUri(assignment.getBinary()), configMap);

        Predicate<AgentStatus> agentFilter = AgentFilterBuilder.build(uriInfo);
        List<SlotStatus> results = coordinator.install(agentFilter, limit, installation);

        return Response.ok(transform(results, fromSlotStatus())).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response terminateSlots(@Context UriInfo uriInfo)
    {
        Predicate<SlotStatus> slotFilter = SlotFilterBuilder.build(uriInfo, true);
        List<SlotStatus> result = coordinator.terminate(slotFilter);

        return Response.ok(transform(result, fromSlotStatus())).build();
    }
}
