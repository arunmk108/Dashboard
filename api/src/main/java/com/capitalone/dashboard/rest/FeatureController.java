package com.capitalone.dashboard.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.Feature;
import com.capitalone.dashboard.model.SprintEstimate;
import com.capitalone.dashboard.service.FeatureService;

/**
 * REST service managing all requests to the feature repository.
 *
 * @author KFK884
 *
 */
@RestController
public class FeatureController {
	private final FeatureService featureService;

	@Autowired
	public FeatureController(FeatureService featureService) {
		this.featureService = featureService;
	}

	/**
	 * REST endpoint for retrieving all features for a given sprint and team
	 * (the sprint is derived)
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return A data response list of type Feature containing all features for
	 *         the given team and current sprint
	 */
	@RequestMapping(value = "/feature", method = GET, produces = APPLICATION_JSON_VALUE)
	public DataResponse<List<Feature>> relevantStories(
	        @RequestParam(value = "projectId", required = true) String projectId,
			@RequestParam(value = "agileType", required = false) Optional<String> agileType,
			@RequestParam(value = "component", required = true) String cId,
			@RequestParam(value = "teamId", required = true) String teamId) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getRelevantStories(componentId, teamId, projectId, agileType);
	}

	/**
	 * REST endpoint for retrieving all features for a given sprint and team
	 * (the sprint is derived)
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return A data response list of type Feature containing all features for
	 *         the given team and current sprint
	 */
	@RequestMapping(value = "/feature/{number}", method = GET, produces = APPLICATION_JSON_VALUE)
	public DataResponse<List<Feature>> story(
			@RequestParam(value = "component", required = true) String cId,
			@PathVariable(value = "number") String storyNumber) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getStory(componentId, storyNumber);
	}

	/**
	 * REST endpoint for retrieving the current sprint detail for a team
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return A response list of type Feature containing the done estimate of
	 *         current features
	 */
	@RequestMapping(value = "/iteration", method = GET, produces = APPLICATION_JSON_VALUE)
	public DataResponse<List<Feature>> currentSprintDetail(
	        @RequestParam(value = "projectId", required = true) String projectId,
			@RequestParam(value = "agileType", required = false) Optional<String> agileType,
			@RequestParam(value = "component", required = true) String cId,
			@RequestParam(value = "teamId", required = true) String teamId) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getCurrentSprintDetail(componentId, teamId, projectId, agileType);
	}

	/**
	 * REST endpoint for retrieving only the unique super features for a given
	 * team and sprint and their related estimates
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return A response list of type Feature containing the unique features
	 *         plus their sub features' estimates associated to the current
	 *         sprint and team
	 */
	@RequestMapping(value = "/feature/estimates/super", method = GET, produces = APPLICATION_JSON_VALUE)
	public DataResponse<List<Feature>> featureEpics(
	        @RequestParam(value = "projectId", required = true) String projectId,
			@RequestParam(value = "agileType", required = false) Optional<String> agileType,
			@RequestParam(value = "estimateMetricType", required = false) Optional<String> estimateMetricType,
			@RequestParam(value = "component", required = true) String cId,
			@RequestParam(value = "teamId", required = true) String teamId) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getFeatureEpicEstimates(componentId, teamId, projectId, agileType, estimateMetricType);
	}
	
	/**
	 * REST endpoint for retrieving the current sprint estimates for a team
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return 
	 */
	@RequestMapping(value = "/feature/estimates/aggregatedsprints", method = GET, produces = APPLICATION_JSON_VALUE)
	public DataResponse<SprintEstimate> featureAggregatedSprintEstimates (
	        @RequestParam(value = "projectId", required = true) String projectId,
			@RequestParam(value = "agileType", required = false) Optional<String> agileType,
			@RequestParam(value = "estimateMetricType", required = false) Optional<String> estimateMetricType,
			@RequestParam(value = "component", required = true) String cId,
			@RequestParam(value = "teamId", required = true) String teamId) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getAggregatedSprintEstimates(componentId, teamId, projectId, agileType, estimateMetricType);
	}

	/**
	 * REST endpoint for retrieving the current total estimate for a team and
	 * sprint
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return A response list of type Feature containing the total estimate of
	 *         current features
	 */
	@RequestMapping(value = "/feature/estimates/total/{teamId}", method = GET, produces = APPLICATION_JSON_VALUE)
	@Deprecated
	public DataResponse<List<Feature>> featureTotalEstimate(
			@RequestParam(value = "agileType", required = false) Optional<String> agileType,
			@RequestParam(value = "estimateMetricType", required = false) Optional<String> estimateMetricType,
			@RequestParam(value = "component", required = true) String cId,
			@PathVariable String teamId) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getTotalEstimate(componentId, teamId, agileType, estimateMetricType);
	}

	/**
	 * REST endpoint for retrieving the current in-progress estimate for a team
	 * and sprint
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return A response list of type Feature containing the in-progress
	 *         estimate of current features
	 */
	@RequestMapping(value = "/feature/estimates/wip/{teamId}", method = GET, produces = APPLICATION_JSON_VALUE)
	@Deprecated
	public DataResponse<List<Feature>> featureInProgressEstimate(
			@RequestParam(value = "agileType", required = false) Optional<String> agileType,
			@RequestParam(value = "estimateMetricType", required = false) Optional<String> estimateMetricType,
			@RequestParam(value = "component", required = true) String cId,
			@PathVariable String teamId) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getInProgressEstimate(componentId, teamId, agileType, estimateMetricType);
	}

	/**
	 * REST endpoint for retrieving the current done estimate for a team and
	 * sprint
	 *
	 * @param teamId
	 *            A given scope-owner's source-system ID
	 * @return A response list of type Feature containing the done estimate of
	 *         current features
	 */
	@RequestMapping(value = "/feature/estimates/done/{teamId}", method = GET, produces = APPLICATION_JSON_VALUE)
	@Deprecated
	public DataResponse<List<Feature>> featureDoneEstimate(
			@RequestParam(value = "agileType", required = false) Optional<String> agileType,
			@RequestParam(value = "estimateMetricType", required = false) Optional<String> estimateMetricType,
			@RequestParam(value = "component", required = true) String cId,
			@PathVariable String teamId) {
		ObjectId componentId = new ObjectId(cId);
		return this.featureService.getDoneEstimate(componentId, teamId, agileType, estimateMetricType);
	}
}
