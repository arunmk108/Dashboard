package com.capitalone.dashboard.rest;

import com.capitalone.dashboard.auth.access.Admin;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.ApiToken;
import com.capitalone.dashboard.model.ServiceAccount;
import com.capitalone.dashboard.model.UserInfo;
import com.capitalone.dashboard.request.ApiTokenRequest;
import com.capitalone.dashboard.request.ServiceAccountRequest;
import com.capitalone.dashboard.service.ApiTokenService;
import com.capitalone.dashboard.service.ServiceAccountService;
import com.capitalone.dashboard.service.UserInfoService;
import com.capitalone.dashboard.util.EncryptionException;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import javax.validation.Valid;
import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/admin")
@Admin
public class AdminController {
    
    private final UserInfoService userInfoService;

    private final ApiTokenService apiTokenService;

    private final ServiceAccountService serviceAccountService;

    @Autowired
    public AdminController(UserInfoService userInfoService, ApiTokenService apiTokenService,ServiceAccountService serviceAccountService) {
        this.userInfoService = userInfoService;
        this.apiTokenService = apiTokenService;
        this.serviceAccountService = serviceAccountService;
    }
    
    @RequestMapping(path = "/users/addAdmin", method = RequestMethod.POST)
    public ResponseEntity<UserInfo> addAdmin(@RequestBody UserInfo user) {
        UserInfo savedUser = userInfoService.promoteToAdmin(user.getUsername(), user.getAuthType());

        return new ResponseEntity<UserInfo>(savedUser, HttpStatus.OK);
    }
    
    @RequestMapping(path = "/users/removeAdmin", method = RequestMethod.POST)
    public ResponseEntity<UserInfo> removeAuthorityFromUser(@RequestBody UserInfo user) {
        UserInfo savedUser = userInfoService.demoteFromAdmin(user.getUsername(), user.getAuthType());

        return new ResponseEntity<UserInfo>(savedUser, HttpStatus.OK);
    }

    @RequestMapping(value = "/createToken", method = RequestMethod.POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createToken(@Valid @RequestBody ApiTokenRequest apiTokenRequest) {
        try {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(apiTokenService.getApiToken(apiTokenRequest.getApiUser(),
                            apiTokenRequest.getExpirationDt())
                    );
        } catch (EncryptionException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (HygieiaException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
    @RequestMapping(value = "/updateToken/{id}", method = RequestMethod.POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateToken(@Valid @RequestBody ApiTokenRequest apiTokenRequest, @PathVariable ObjectId id) {
        try {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(apiTokenService.updateToken(apiTokenRequest.getExpirationDt(),id));
        } catch (HygieiaException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
    @RequestMapping(value = "/deleteToken/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteToken(@PathVariable ObjectId id) {
        apiTokenService.deleteToken(id);
        return ResponseEntity.noContent().build();
    }
    @RequestMapping(path = "/apitokens", method = RequestMethod.GET)
    public Collection<ApiToken> getApiTokens() {
        Collection<ApiToken> tokens = apiTokenService.getApiTokens();
        return tokens;
    }

    @RequestMapping(value = "/createAccount", method = RequestMethod.POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAccount(@Valid @RequestBody ServiceAccountRequest serviceAccountRequest) {
        try {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(serviceAccountService.createAccount(serviceAccountRequest.getServiceAccount(),
                            serviceAccountRequest.getFileNames()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @RequestMapping(value = "/updateAccount/{id}", method = RequestMethod.POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateAccount(@Valid @RequestBody ServiceAccountRequest serviceAccountRequest, @PathVariable ObjectId id) {
        try {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(serviceAccountService.updateAccount(serviceAccountRequest.getServiceAccount(),
                            serviceAccountRequest.getFileNames(),id));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @RequestMapping(path = "/allServiceAccounts", method = RequestMethod.GET)
    public Collection<ServiceAccount> getAllServiceAccouts() {
        Collection<ServiceAccount> serviceAccounts = serviceAccountService.getAllServiceAccounts();
        return serviceAccounts;
    }


    @RequestMapping(path = "/deleteAccount/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAccount(@PathVariable ObjectId id){
        serviceAccountService.deleteAccount(id);
        return ResponseEntity.<Void>noContent().build();
    }
}
