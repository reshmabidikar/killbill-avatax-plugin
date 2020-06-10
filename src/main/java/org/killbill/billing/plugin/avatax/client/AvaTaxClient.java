/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.avatax.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.killbill.billing.plugin.avatax.client.model.CreateTransactionModel;
import org.killbill.billing.plugin.avatax.client.model.TransactionModel;
import org.killbill.billing.plugin.avatax.core.AvaTaxActivator;
import org.killbill.billing.plugin.util.http.HttpClient;
import org.killbill.billing.plugin.util.http.InvalidRequest;
import org.killbill.billing.plugin.util.http.ResponseFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public class AvaTaxClient extends HttpClient {

    public static final String KILL_BILL_CLIENT_HEADER = "Kill Bill; 2.0; killbill-avatax; 2.0; NA";

    private final String companyCode;
    private final boolean commitDocuments;

    public AvaTaxClient(final Properties properties) throws GeneralSecurityException {
        super(properties.getProperty(AvaTaxActivator.PROPERTY_PREFIX + "url"),
              properties.getProperty(AvaTaxActivator.PROPERTY_PREFIX + "accountId"),
              properties.getProperty(AvaTaxActivator.PROPERTY_PREFIX + "licenseKey"),
              properties.getProperty(AvaTaxActivator.PROPERTY_PREFIX + "proxyHost"),
              ClientUtils.getIntegerProperty(properties, "proxyPort"),
              ClientUtils.getBooleanProperty(properties, "strictSSL"),
              MoreObjects.firstNonNull(ClientUtils.getIntegerProperty(properties, "connectTimeout"), 10000),
              MoreObjects.firstNonNull(ClientUtils.getIntegerProperty(properties, "readTimeout"), 60000));
        this.companyCode = properties.getProperty(AvaTaxActivator.PROPERTY_PREFIX + "companyCode");
        this.commitDocuments = Boolean.parseBoolean(properties.getProperty(AvaTaxActivator.PROPERTY_PREFIX + "commitDocuments"));
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public boolean shouldCommitDocuments() {
        return commitDocuments;
    }

    public boolean isConfigured() {
        return url != null && username != null && password != null;
    }

    @Override
    protected ObjectMapper createObjectMapper() {
        return ClientUtils.createObjectMapper();
    }

    public TransactionModel createTransaction(final CreateTransactionModel createTransactionModel) throws AvaTaxClientException {
        try {
            return doCall(POST,
                          url + "/transactions/create",
                          serialize(createTransactionModel),
                          DEFAULT_OPTIONS,
                          ImmutableMap.<String, String>of("X-Avalara-Client", KILL_BILL_CLIENT_HEADER),
                          TransactionModel.class,
                          ResponseFormat.JSON);
        } catch (final InterruptedException e) {
            throw new AvaTaxClientException(e);
        } catch (final ExecutionException e) {
            throw new AvaTaxClientException(e);
        } catch (final TimeoutException e) {
            throw new AvaTaxClientException(e);
        } catch (final IOException e) {
            throw new AvaTaxClientException(e);
        } catch (final URISyntaxException e) {
            throw new AvaTaxClientException(e);
        } catch (final InvalidRequest e) {
            try {
                return deserializeResponse(e.getResponse(), TransactionModel.class, ResponseFormat.JSON);
            } catch (final IOException e1) {
                throw new AvaTaxClientException(e1);
            }
        }
    }

    private String serialize(final Object o) throws AvaTaxClientException {
        try {
            return mapper.writeValueAsString(o);
        } catch (final JsonProcessingException e) {
            throw new AvaTaxClientException(e);
        }
    }
}
