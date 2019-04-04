package com.capitalone.dashboard.collector;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import com.capitalone.dashboard.model.LibraryPolicyReport;
import com.capitalone.dashboard.model.LibraryPolicyResult;
import com.capitalone.dashboard.model.LibraryPolicyThreatLevel;
import com.capitalone.dashboard.model.LibraryPolicyType;
import com.capitalone.dashboard.model.NexusIQApplication;
import com.capitalone.dashboard.model.PolicyScanMetric;
import com.capitalone.dashboard.util.Supplier;

@Component
public class DefaultNexusIQClient implements NexusIQClient {
    private static final Log LOG = LogFactory.getLog(DefaultNexusIQClient.class);

    private static final String API_V2_APPLICATIONS = "/api/v2/applications";
    private static final String API_V2_REPORTS_LINKS = "/api/v2/reports/applications/%s";
    private static final String API_V2_POLICIES = "/api/v2/policies?format=json";
    private static final String API_V2_POLICY_VIOLATION = "/api/v2/policyViolations?p=%s&format=json";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String PUBLIC_ID = "publicId";

    private static final String THREAT_LEVEL = "threatLevel";

    private final RestOperations rest;
    private final NexusIQSettings nexusIQSettings;

    @Autowired
    public DefaultNexusIQClient(Supplier<RestOperations> restOperationsSupplier, NexusIQSettings settings) {
        this.rest = restOperationsSupplier.get();
        this.nexusIQSettings = settings;
    }

    /**
     * Get all the applications from the nexus IQ server
     * @param instanceUrl instance of nexus iq
     * @return List of applications
     */
    @Override
    public List<NexusIQApplication> getApplications(String instanceUrl) {
        List<NexusIQApplication> nexusIQApplications = new ArrayList<>();
        String url = instanceUrl + API_V2_APPLICATIONS;

        try {
            JSONObject jsonObject = parseAsObject(url);
            JSONArray jsonArray = (JSONArray) jsonObject.get("applications");
            for (Object obj : jsonArray) {
                JSONObject applicationData = (JSONObject) obj;
                NexusIQApplication application = new NexusIQApplication();
                application.setInstanceUrl(instanceUrl);
                application.setApplicationId(str(applicationData, ID));
                application.setApplicationName(str(applicationData, NAME));
                application.setDescription(application.getApplicationName());
                application.setPublicId(str(applicationData, PUBLIC_ID));
                nexusIQApplications.add(application);
            }
        } catch (ParseException e) {
            LOG.error("Could not parse response from: " + url, e);
        } catch (RestClientException rce) {
            LOG.error(rce);
        }
        return nexusIQApplications;
    }

    /**
     * Get report links for a given application.
     * @param application nexus application
     * @return a list of report links
     */
    @Override
    public List<LibraryPolicyReport> getApplicationReport(NexusIQApplication application) {
        List<LibraryPolicyReport> applicationReports = new ArrayList<>();

        String appReportLinkUrl = String.format(
                application.getInstanceUrl() + API_V2_REPORTS_LINKS, application.getApplicationId());

        try {
            JSONArray reports = parseAsArray(appReportLinkUrl);
            if ((reports == null) || (reports.isEmpty())) return null;
            for (Object element : reports) {
                LibraryPolicyReport appReport = new LibraryPolicyReport();
                String stage = str((JSONObject) element, "stage");
                appReport.setStage(stage);
                appReport.setEvaluationDate(timestamp((JSONObject) element, "evaluationDate"));
                appReport.setReportDataUrl(application.getInstanceUrl() + "/" + str((JSONObject) element, "reportDataUrl"));
                appReport.setReportUIUrl(application.getInstanceUrl() + "/" + str((JSONObject) element, "reportHtmlUrl"));
                applicationReports.add(appReport);
            }
        } catch (ParseException e) {
            LOG.error("Could not parse response from: " + appReportLinkUrl, e);
        } catch (RestClientException rce) {
            LOG.error("RestClientException: " + appReportLinkUrl + ". Error code=" + rce.getMessage());
        }
        return applicationReports;
    }
        
    /**
     * Get the report details given a url for the report data.
     * @param url url of the report
     * @return LibraryPolicyResult
     */
    @SuppressWarnings({"PMD.AvoidDeeplyNestedIfStmts","PMD.NPathComplexity"}) // agreed PMD, fixme

    @Override
    public LibraryPolicyResult getDetailedReport(String url) {
        LibraryPolicyResult policyResult = null;
        try {
            JSONObject obj = parseAsObject(url);
            JSONObject matchSummary = (JSONObject) obj.get("matchSummary");
            JSONArray componentArray = (JSONArray) obj.get("components");
            if ((componentArray == null) || (componentArray.isEmpty())) return null;
            for (Object element : componentArray) {
                JSONObject component = (JSONObject) element;
                int licenseLevel = 0;
                JSONArray pathArray = (JSONArray) component.get("pathnames");

                String componentName = !CollectionUtils.isEmpty(pathArray) ? (String) pathArray.get(0) : getComponentNameFromIdentifier((JSONObject) component.get("componentIdentifier"));

                JSONObject licenseData = (JSONObject) component.get("licenseData");

                if (licenseData != null) {
                    //process license data
                    JSONArray effectiveLicenseThreats = (JSONArray) licenseData.get("effectiveLicenseThreats");
                    if (!CollectionUtils.isEmpty(effectiveLicenseThreats)) {
                        for (Object et : effectiveLicenseThreats) {
                            JSONObject etJO = (JSONObject) et;
                            Long longvalue = toLong(etJO, "licenseThreatGroupLevel");
                            if (longvalue != null) {
                                int newlevel = longvalue.intValue();
                                if (licenseLevel == 0) {
                                    licenseLevel = newlevel;
                                } else {
                                    licenseLevel = nexusIQSettings.isSelectStricterLicense() ? Math.max(licenseLevel, newlevel) : Math.min(licenseLevel, newlevel);
                                }
                            }
                        }

                    }
                }

                if (policyResult == null) {
                    policyResult = new LibraryPolicyResult();
                }

                if (licenseLevel > 0) {
                    policyResult.addThreat(LibraryPolicyType.License, LibraryPolicyThreatLevel.fromInt(licenseLevel), componentName);
                }

                JSONObject securityData = (JSONObject) component.get("securityData");
                if (securityData != null) {
                    //process security data
                    JSONArray securityIssues = (JSONArray) securityData.get("securityIssues");
                    if (!CollectionUtils.isEmpty(securityIssues)) {
                        for (Object si : securityIssues) {
                            JSONObject siJO = (JSONObject) si;
                            BigDecimal bigDecimalValue = decimal(siJO, "severity");
                            double securityLevel = (bigDecimalValue == null) ? getSeverityLevel(str(siJO, "threatCategory")) : bigDecimalValue.doubleValue();
                            policyResult.addThreat(LibraryPolicyType.Security, LibraryPolicyThreatLevel.fromDouble(securityLevel), componentName);
                        }
                    } 
                }
            } 
            
            policyResult.setTotalComponentCount(Integer.parseInt(matchSummary.get("totalComponentCount").toString()));
        	policyResult.setKnownComponentCount(Integer.parseInt(matchSummary.get("knownComponentCount").toString()));
            
        } catch (ParseException e) {
            LOG.error("Could not parse response from: " + url, e);
        } catch (RestClientException rce) {
            LOG.error("RestClientException from: " + url + ". Error code=" + rce.getMessage());
        }
                   
        return policyResult;
    }
    
    @Override
    public PolicyScanMetric getPolicyAlerts(NexusIQApplication application)
    {
    	List<String> policyHighViolatedComponents = new ArrayList<>();
		List<String> policyMediumViolatedComponents = new ArrayList<>();
		List<String> policyModerateViolatedComponents = new ArrayList<>();
		Set<String> affectedComponents = new HashSet<>();
		PolicyScanMetric psm = new PolicyScanMetric();
		
    	JSONArray applicationViolations = getPolicyViolations(application.getInstanceUrl());
    	
    	for (Object violatedApplication : applicationViolations) {
			JSONObject applicationArray = (JSONObject) violatedApplication;
			JSONObject app = (JSONObject) applicationArray.get("application");
			if (str(app, PUBLIC_ID).equals(application.getApplicationName())) {
				JSONArray pv = (JSONArray) applicationArray.get("policyViolations");
				for (Object objt : pv) {
					JSONObject violationObject = (JSONObject) objt;

					JSONObject component = (JSONObject) violationObject.get("component");
					String componentHash = (String) component.get("hash");

					if ((8 <= lng(violationObject, THREAT_LEVEL)) && (10 >= lng(violationObject, THREAT_LEVEL))) {
						policyHighViolatedComponents.add(componentHash);
						affectedComponents.add(componentHash);
					} else if ((4 <= lng(violationObject, THREAT_LEVEL))
							&& (7 >= lng(violationObject, THREAT_LEVEL))) {
						policyMediumViolatedComponents.add(componentHash);
						affectedComponents.add(componentHash);
					} else if (3 >= lng(violationObject, THREAT_LEVEL)
							&& (lng(violationObject, THREAT_LEVEL) >= 2)) {
						policyModerateViolatedComponents.add(componentHash);
						affectedComponents.add(componentHash);
					}

				}

			}

		}

	for (String policyhighViolatedComponent : policyHighViolatedComponents) {
		policyMediumViolatedComponents.removeIf(policyhighViolatedComponent::equals);
		policyModerateViolatedComponents.removeIf(policyhighViolatedComponent::equals);
	}
	for (String policyMediumViolatedComponent : policyMediumViolatedComponents) {
		policyModerateViolatedComponents.removeIf(policyMediumViolatedComponent::equals);
	}

	psm.setPolicycriticalCount(policyHighViolatedComponents.size());
	psm.setPolicysevereCount(policyMediumViolatedComponents.size());
	psm.setPolimoderateCount(policyModerateViolatedComponents.size());
	psm.setPolicyAffectedCount(affectedComponents.size());
	return psm;    	
    	
    }
    
	private JSONArray getPolicyViolations(String url) {
		String policyUrl = url + API_V2_POLICIES;
		String policyViolationUrl;

		JSONObject object;
		JSONArray applicationViolations = new JSONArray();
		try {
			object = parseAsObject(policyUrl);
			JSONArray policyArray = (JSONArray) object.get("policies");

			for (Object obj : policyArray) {
				JSONObject policyData = (JSONObject) obj;
				policyViolationUrl = String.format(url + API_V2_POLICY_VIOLATION, str(policyData, ID));
				JSONObject policyObject = parseAsObject(policyViolationUrl);
				applicationViolations.addAll((JSONArray) policyObject.get("applicationViolations"));
			}
		} catch (ParseException e) {
			LOG.error("Error parsing JSON object at getPolicyViolations()" + e);
		}catch (RestClientException e){
			LOG.error("Error fetching polices in getPolicyViolations() "+e);
		}
		return applicationViolations;
	}

    private String getComponentNameFromIdentifier(JSONObject identifier) {
        String unknown = "unknown";
        if (identifier == null) return unknown;
        JSONObject coordinate = (JSONObject) identifier.get("coordinates");
        if (coordinate == null) return unknown;
        String format = str(identifier, "format");
        if (format == null) return unknown;
        String componentName;
        switch (format.toLowerCase(Locale.ENGLISH)) {
            case "maven":
                componentName = String.format("%s:%s-%s.%s",
                        str(coordinate, "groupId"),
                        str(coordinate, "artifactId"),
                        str(coordinate, "version"),
                        str(coordinate, "extension"));
                break;

            case "a-name":
                componentName = String.format("%s-%s",
                        str(identifier, "name"),
                        str(identifier, "version"));
                break;

            default:
                componentName = unknown;
                break;
        }
        return componentName;
    }

    private double getSeverityLevel(String threatCategory) {
        switch (threatCategory) {
            case "critial":
                return 10.0;
            case "severe":
                return 6.9;
            case "moderate":
                return 2.9;
            default:
                return 0.0;
        }
    }


    private JSONArray parseAsArray(String url) throws ParseException {
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, createHeaders(url), String.class);
        return (JSONArray) new JSONParser().parse(response.getBody());
    }

    private JSONObject parseAsObject(String url) throws ParseException {
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, createHeaders(url), String.class);
        return (JSONObject) new JSONParser().parse(response.getBody());
    }

    private long timestamp(JSONObject json, String key) {
        Object obj = json.get(key);
        if (obj != null) {
            try {
                return new SimpleDateFormat(DATE_FORMAT).parse(obj.toString()).getTime();
            } catch (java.text.ParseException e) {
                LOG.error(obj + " is not in expected format " + DATE_FORMAT, e);
            }
        }
        return 0;
    }

    private String str(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString();
    }

    @SuppressWarnings("unused")
    private Integer integer(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : (Integer) obj;
    }

    private Long toLong(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : (Long) obj;
    }

    @SuppressWarnings("unused")
    private BigDecimal decimal(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : new BigDecimal(obj.toString());
    }

    @SuppressWarnings("unused")
    private Boolean bool(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Boolean.valueOf(obj.toString());
    }
    
    @SuppressWarnings("unused")
	private Long lng(JSONObject json, String key) {
		Object obj = json.get(key);
		return obj == null ? null : Long.valueOf(obj.toString());
	}
  
    private HttpEntity<String> createHeaders(String url) {
    	String username = null;
    	String password = null;
        HttpHeaders headers = new HttpHeaders();

    	for(int i=0;i<nexusIQSettings.getServers().size();i++) {
    		if(url.contains(nexusIQSettings.getServers().get(i))){
        		username = nexusIQSettings.getUsernames().get(i);
        		password = nexusIQSettings.getPasswords().get(i);
    		}
    	}
        if (username != null && !username.isEmpty() &&
                password != null && !password.isEmpty()) {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(Charset.forName("US-ASCII"))
            );
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        }
        return new HttpEntity<>(headers);

    }
}
