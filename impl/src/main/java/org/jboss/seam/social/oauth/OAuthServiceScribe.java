/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.social.oauth;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;


/**
 * @author Antoine Sabot-Durand
 * 
 */

public abstract class OAuthServiceScribe implements OAuthService, Serializable {

    /**
    * 
    */
    private static final long serialVersionUID = -8423894021913341674L;
    private static final String VERIFIER_PARAM_NAME = "oauth_verifier";

    private org.scribe.oauth.OAuthService service;

    protected OAuthTokenScribe requestToken;
    protected OAuthTokenScribe accessToken;

    private Verifier verifier;

    private Boolean connected = Boolean.FALSE;

    protected UserProfile userProfile;

    private OAuthServiceSettings settings;

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    protected org.scribe.oauth.OAuthService getService() {
        if (service == null)
            initService();
        return service;
    }

    private void initService() {
        Class<? extends Api> apiClass = getApiClass();
        ServiceBuilder serviceBuilder = new ServiceBuilder().provider(apiClass).apiKey(getSettings().getApiKey())
                .apiSecret(getSettings().getApiSecret());
        if (getSettings().getCallback() != null && !("".equals(getSettings().getCallback())))
            serviceBuilder.callback(getSettings().getCallback());
        service = serviceBuilder.build();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.seam.social.oauth.OAuthServiceScribe#getSettings()
     */
    @Override
    public OAuthServiceSettings getSettings() {
        return settings;
    }

    public void setSettings(OAuthServiceSettings settings) {
        this.settings = settings;
    }

    protected abstract Class<? extends Api> getApiClass();

    @Override
    public String getAuthorizationUrl() {
        return getService().getAuthorizationUrl(getRequestToken().delegate);
    }

    protected OAuthTokenScribe getRequestToken() {
        if (requestToken == null)
            requestToken = new OAuthTokenScribe(getService().getRequestToken());
        return requestToken;
    }

    @Override
    public void initAccessToken() {
        if (accessToken == null) {
            accessToken = new OAuthTokenScribe(getService().getAccessToken(getRequestToken().delegate, verifier));
            if (accessToken != null) {
                connected = Boolean.TRUE;
                requestToken = null;
            } else {
                // Launch an exception !!
            }
        }

    }

    @Override
    public void resetConnexion() {
        userProfile = null;
        accessToken = null;
        verifier = null;
        connected = Boolean.FALSE;

    }

    protected HttpResponse sendSignedRequest(OAuthRequest request) {
        getService().signRequest(accessToken.delegate, request);
        HttpResponse resp = null;
        try {
            resp = new HttpResponseScribe(request.send());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resp;
    }

    @Override
    public HttpResponse sendSignedRequest(RestVerb verb, String uri) {
        OAuthRequest request = new OAuthRequest(Verb.valueOf(verb.toString()), uri);

        return sendSignedRequest(request);

    }

    @Override
    public HttpResponse sendSignedRequest(RestVerb verb, String uri, String key, Object value) {
        OAuthRequest request = new OAuthRequest(Verb.valueOf(verb.toString()), uri);

        request.addBodyParameter(key, value.toString());

        return sendSignedRequest(request);

    }

    @Override
    public HttpResponse sendSignedXmlRequest(RestVerb verb, String uri, String payload) {
        OAuthRequest request = new OAuthRequest(Verb.valueOf(verb.toString()), uri);
        request.addHeader("Content-Length", Integer.toString(payload.length()));
        request.addHeader("Content-Type", "text/xml");

        request.addPayload(payload);

        return sendSignedRequest(request);

    }

    @Override
    public HttpResponse sendSignedRequest(RestVerb verb, String uri, Map<String, Object> params) {
        OAuthRequest request = new OAuthRequest(Verb.valueOf(verb.toString()), uri);
        for (Entry<String, Object> ent : params.entrySet()) {
            request.addBodyParameter(ent.getKey(), ent.getValue().toString());
        }
        return sendSignedRequest(request);

    }

    @Override
    public void setVerifier(String verifierStr) {
        verifier = new Verifier(verifierStr);
    }

    @Override
    public String getVerifier() {
        return verifier == null ? null : verifier.getValue();
    }

    @Override
    public OAuthToken getAccessToken() {
        return accessToken;
    }

    @Override
    public Boolean isConnected() {
        return connected;
    }

    @Override
    public void setAccessToken(String token, String secret) {
        accessToken = new OAuthTokenScribe(token, secret);

    }

    @Override
    public void setAccessToken(OAuthToken token) {
        accessToken = new OAuthTokenScribe(token.getToken(), token.getSecret());

    }

    @Override
    public String toString() {
        return getType();
    }

    @Override
    public String getVerifierParamName() {
        return VERIFIER_PARAM_NAME;
    }

}
