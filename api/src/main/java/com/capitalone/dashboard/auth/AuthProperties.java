package com.capitalone.dashboard.auth;

import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.capitalone.dashboard.model.AuthType;
import com.google.common.collect.Lists;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthProperties.class);

	private Long expirationTime;
	private String secret;
	private String ldapUserDnPattern;
	private String ldapServerUrl;
	private List<AuthType> authenticationProviders = Lists.newArrayList();

	private String adDomain;
	private String adRootDn;
	private String adUrl;

	private String ldapBindUser;
	private String ldapBindPass;

	private boolean ldapDisableGroupAuthorization = false;

	// -- SSO properties
	private String userEid;
	private String userEmail;
	private String userFirstName;
	private String userLastName;
	private String userMiddelInitials;
	private String userDisplayName;
	//-- end SSO properties

	public Long getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(Long expirationTime) {
		this.expirationTime = expirationTime;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getLdapUserDnPattern() {
		return ldapUserDnPattern;
	}

	public void setLdapUserDnPattern(String ldapUserDnPattern) {
		this.ldapUserDnPattern = ldapUserDnPattern;
	}

	public String getLdapServerUrl() {
		return ldapServerUrl;
	}

	public void setLdapServerUrl(String ldapServerUrl) {
		this.ldapServerUrl = ldapServerUrl;
	}

	public List<AuthType> getAuthenticationProviders() {
		return authenticationProviders;
	}

	public void setAuthenticationProviders(List<AuthType> authenticationProviders) {
		this.authenticationProviders = authenticationProviders;
	}

	public String getAdDomain() {
		return adDomain;
	}

	public void setAdDomain(String adDomain) {
		this.adDomain = adDomain;
	}

	public String getAdRootDn() {
		return adRootDn;
	}

	public void setAdRootDn(String adRootDn) {
		this.adRootDn = adRootDn;
	}

	public String getAdUrl() {
		return adUrl;
	}

	public void setAdUrl(String adUrl) {
		this.adUrl = adUrl;
	}

	public String getLdapBindUser() {
		return ldapBindUser;
	}

	public void setLdapBindUser(String ldapBindUser) {
		this.ldapBindUser = ldapBindUser;
	}

	public String getLdapBindPass() {
		return ldapBindPass;
	}

	public void setLdapBindPass(String ldapBindPass) {
		this.ldapBindPass = ldapBindPass;
	}

	public boolean isLdapDisableGroupAuthorization() {
		return ldapDisableGroupAuthorization;
	}

	public void setLdapDisableGroupAuthorization(boolean ldapDisableGroupAuthorization) {
		this.ldapDisableGroupAuthorization = ldapDisableGroupAuthorization;
	}

	@PostConstruct
	public void applyDefaultsIfNeeded() {
		if (getSecret() == null) {
			LOGGER.info("No JWT secret found in configuration, generating random secret by default.");
			setSecret(UUID.randomUUID().toString().replace("-", ""));
		}

		if (getExpirationTime() == null) {
			LOGGER.info("No JWT expiration time found in configuration, setting to one day.");
			setExpirationTime((long) 1000 * 60 * 60 * 24);
		}

		if (CollectionUtils.isEmpty(authenticationProviders)) {
			authenticationProviders.add(AuthType.STANDARD);
		}
	}

	public String getUserEid() {
		return userEid;
	}

	public void setUserEid(String userEid) {
		this.userEid = userEid;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserFirstName() {
		return userFirstName;
	}

	public void setUserFirstName(String userFirstName) {
		this.userFirstName = userFirstName;
	}

	public String getUserLastName() {
		return userLastName;
	}

	public void setUserLastName(String userLastName) {
		this.userLastName = userLastName;
	}

	public String getUserMiddelInitials() {
		return userMiddelInitials;
	}

	public void setUserMiddelInitials(String userMiddelInitials) {
		this.userMiddelInitials = userMiddelInitials;
	}

	public String getUserDisplayName() {
		return userDisplayName;
	}

	public void setUserDisplayName(String userDisplayName) {
		this.userDisplayName = userDisplayName;
	}

}
