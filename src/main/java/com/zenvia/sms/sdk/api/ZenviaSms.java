package com.zenvia.sms.sdk.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zenvia.sms.sdk.base.requests.SendSmsRequest;
import com.zenvia.sms.sdk.base.responses.SendSmsResponse;
import com.zenvia.sms.sdk.base.rest.ZenviaSmsModel;
import com.zenvia.sms.sdk.exceptions.ZenviaHTTPExceptionFactory;
import com.zenvia.sms.sdk.exceptions.ZenviaHTTPSmsException;
import com.zenvia.sms.sdk.exceptions.ZenviaSmsInvalidEntityException;
import com.zenvia.sms.sdk.exceptions.ZenviaSmsUnexpectedAPIResponseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.URI;

/**
 *
 * ZenviaSms is an HTTP Client for connecting to Zenvia's API.
 *
 */
public final class ZenviaSms {
        public static final String VERSION = "0.0.1";

        private String base64AuthorizationKey;

        private JsonObject requestBody;
        private JsonObject responseBody;

        private URI endpoint = URI.create("https://api-rest.zenvia360.com.br/services");

        public ZenviaSms(String base64AuthorizationKey) {
            this.base64AuthorizationKey = base64AuthorizationKey;
        }


        /**
         *
         * @param endpoint Zenvia's API endpoint (default is https://api-rest.zenvia360.com.br/services)
         */
        public void setEndpoint(URI endpoint) {
            this.endpoint = endpoint;
        }

        /**
         *
         * @return Endpoint for Zenvia API
         */
        public URI getEndpoint() {
            return this.endpoint;
        }


        /**
         *
         * @param base64AuthorizationKey sets the auth key generated by username and password sent by email, which is required for Zenvia's API authentication.
         * @throws IllegalArgumentException
         */
        public void setBasicAuthorizationKey(String base64AuthorizationKey) throws UnsupportedEncodingException {
            if ( base64AuthorizationKey == null ) {
                throw new IllegalArgumentException("Illegal auth Key:" + base64AuthorizationKey);
            } else {
                this.base64AuthorizationKey = base64AuthorizationKey;
            }

        }

        public String getBasicAuthorizationKey(){
            return this.base64AuthorizationKey;
        }

        /**
         * @return [POST] send sms URI (ENDPOINT/send-sms)
         */
        public URI zenviaSendSmsUrl(){
            return URI.create(endpoint.toString().concat("/send-sms"));
        }

        /**
         * @return [POST] send sms multiple URI (ENDPOINT/send-sms-multiple)
         */
        public URI zenviaSendSmsMultipleUrl(){
            return URI.create(endpoint.toString().concat("/send-sms-multiple"));
        }

        /**
         * @param smsId the of sms to get status
         * @return [POST] order URI (ENDPOINT/get-sms-status/smsId)
         */
        public URI zenviaGetSmsStatusUrl(String smsId){
            return URI.create(endpoint.toString().concat("/get-sms-status/" + smsId));
        }

        /**
         * @return [POST] order URI (ENDPOINT/received/list)
         */
        public URI zenviaListReceivedSmsUrl(){
            return URI.create(endpoint.toString().concat("/received/list"));
        }

        /**
         * @param startDate the start date from the search
         * @param endDate the end date from the search
         * @return [GET] order URI (ENDPOINT/get-sms-status/smsId)
         */
        public URI zenviaListReceivedSmsByPeriodUrl(String startDate, String endDate) {
            return URI.create(endpoint.toString().concat("/received/search/" + startDate + "/" + endDate));
        }

        /**
         * @param smsId the of sms to be canceled
         * @return [POST] order URI (ENDPOINT/cancel-sms/smsId)
         */
        public URI zenviaCancelSmsUrl(String smsId){
            return URI.create(endpoint.toString().concat("/cancel-sms/" + smsId));
        }

        /**
         * Helper method to debug requests made to Konduto's API.
         * @return a String containing API Key, Konduto's API endpoint, request and response bodies.
         */
        public String debug() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("API Key: %s\n", this.base64AuthorizationKey));
            sb.append(String.format("Endpoint: %s\n", this.endpoint.toString()));
            if(this.requestBody != null) {
                sb.append(String.format("Request body: %s\n", this.requestBody));
            }
            if(this.responseBody != null) {
                sb.append(String.format("Response body: %s\n", this.responseBody));
            }
            return sb.toString();
        }

        /**
         * @param inputStream the stream with HTTP response
         * @return the response body as a {@link com.google.gson.JsonObject JsonObject}
         * @throws IOException
         */
        private static JsonObject extractResponse(InputStream inputStream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            JsonParser parser = new JsonParser();
            return (JsonObject) parser.parse(stringBuilder.toString());
        }

        /**
         *
         * @param postRequest the HTTP Post
         * @param requestBody the HTTP request body
         * @return the response coming from Konduto's API
         * @throws ZenviaHTTPSmsException when something goes wrong, i.e a non-200 OK response is answered
         * @throws IOException when something goes wrong in the http connection and could not execute the request correctly
         */
        private JsonObject sendRequest(HttpPost postRequest, JsonObject requestBody) throws ZenviaHTTPSmsException, IOException {


            CloseableHttpClient httpClient = HttpClients.createDefault();

            JsonObject responseBody;

            HttpResponse response;

            checkAuthorizationData();

            postRequest.addHeader("Authorization", "Basic " + this.base64AuthorizationKey);

            postRequest.addHeader("Content-Type", "application/json");

            postRequest.addHeader("Accept", "application/json");

            try {

                this.requestBody = requestBody; // set Zenvia's request body for debugging purposes

                StringEntity requestEntity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);

                postRequest.setEntity(requestEntity);

                response =  httpClient.execute(postRequest);

                int statusCode = response.getStatusLine().getStatusCode();

                HttpEntity responseEntity = response.getEntity();

                responseBody = extractResponse(responseEntity.getContent());

                this.responseBody = responseBody; // set Zenvia's response body for debugging purposes

                if(statusCode != 200) { throw ZenviaHTTPExceptionFactory.buildException(statusCode, responseBody); }

                httpClient.close();

                return responseBody;

            } finally {
                httpClient.close();
            }
        }

    private void checkAuthorizationData(){
        if(this.base64AuthorizationKey == null) {
            throw new NullPointerException("API authorization key cannot be generated, since there is not username and password configured");
        }
    }

    //TODO: Add the methods to send sms and get status and sms
        /**
         * Sends a single sms using Zenvia SMS API
         *
         * @param sendSmsRequest a {@link SendSmsRequest} instance
         * @throws ZenviaHTTPSmsException
         * @throws ZenviaSmsUnexpectedAPIResponseException
         * @throws ZenviaSmsInvalidEntityException
         * @see <a href="http://docs.zenviasms.apiary.io/">Zenvia Sms API Spec</a>
         */

        public SendSmsResponse sendSingleSms(SendSmsRequest sendSmsRequest)
                throws ZenviaHTTPSmsException, ZenviaSmsUnexpectedAPIResponseException, ZenviaSmsInvalidEntityException {

            HttpPost postMethod = new HttpPost(zenviaSendSmsUrl().toString());

            JsonObject responseBody;

            JsonElement elements = ZenviaSmsModel.getGson().toJsonTree(sendSmsRequest);
            JsonObject sendSmsJson = new JsonObject();
            sendSmsJson.add("sendSmsRequest", elements);

            try {
                responseBody = sendRequest(postMethod, sendSmsJson);

                if(responseBody == null) {
                    throw new ZenviaSmsUnexpectedAPIResponseException(null);
                }

                return (SendSmsResponse)ZenviaSmsModel.fromJSON(responseBody.getAsJsonObject("sendSmsResponse"), SendSmsResponse.class);

            } catch (IOException e) {
                throw new ZenviaSmsInvalidEntityException(sendSmsRequest);
            }
        }

}
