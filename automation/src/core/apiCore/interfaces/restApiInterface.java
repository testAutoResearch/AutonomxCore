package core.apiCore.interfaces;

import static io.restassured.RestAssured.given;

import core.apiCore.helpers.dataHelper;
import core.apiCore.helpers.jsonHelper;
import core.helpers.Helper;
import core.support.configReader.Config;
import core.support.logger.TestLog;
import core.support.objects.ApiObject;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class restApiInterface {

	/*
	 * (String TestSuite, String TestCaseID, String RunFlag, String Description,
	 * String InterfaceType, String UriPath, String ContentType, String Method,
	 * String Option, String RequestHeaders, String TemplateFile, String
	 * RequestBody, String OutputParams, String RespCodeExp, String
	 * ExpectedResponse, String PartialExpectedResponse, String NotExpectedResponse,
	 * String TcComments, String tcName, String tcIndex)
	 */

	/**
	 * interface for restfull api calls
	 * 
	 * @param apiObject
	 * @return
	 */
	public static Response RestfullApiInterface(ApiObject apiObject) {
		
		if(apiObject == null) Helper.assertFalse("apiobject is null");
		
		// replace parameters for request body
		apiObject.RequestBody = dataHelper.replaceParameters(apiObject.RequestBody);

		// set base uri
		setURI(apiObject);

		// send request and receive a response
		Response response = evaluateRequest(apiObject);

		// validate the response
		validateResponse(response, apiObject);

		return response;
	}

	/**
	 * sets base uri for api call
	 */
	public static void setURI(ApiObject apiObject) {

		// replace place holder values for uri
		apiObject.UriPath = dataHelper.replaceParameters(apiObject.UriPath);
		apiObject.UriPath = Helper.stringRemoveLines(apiObject.UriPath);
		// if uri is full path, then set base uri as whats provided in csv file
		// else use baseURI from properties as base uri and extend it with csv file uri
		// path
		if (apiObject.UriPath.startsWith("http")) {
			RestAssured.baseURI = apiObject.UriPath;
			apiObject.UriPath = "";

		} else {
			RestAssured.baseURI = Helper.stringRemoveLines(Config.getValue("UriPath"));
			TestLog.logPass("request URI: " + RestAssured.baseURI + apiObject.UriPath);
		}
	}

	public static void validateResponse(Response response, ApiObject apiObject) {

		// fail test if no response is returned
		if (response == null)
			Helper.assertTrue("no response returned", false);

		// validate status code
		if (!apiObject.RespCodeExp.isEmpty()) {
			TestLog.logPass("expected status code: " + apiObject.RespCodeExp + " response status code: "
					+ response.getStatusCode());
			response.then().statusCode(Integer.valueOf(apiObject.RespCodeExp));
		}

		// saves response values to config object
		jsonHelper.saveOutboundJsonParameters(response, apiObject.OutputParams);

		validateExpectedValues(response, apiObject);
	}

	public static void validateExpectedValues(Response response, ApiObject apiObject) {
		// get response body as string
		String body = response.getBody().asString();
		TestLog.logPass("response: " + body);

		// validate response body against expected json string
		if (!apiObject.PartialExpectedResponse.isEmpty()) {
			apiObject.PartialExpectedResponse = dataHelper.replaceParameters(apiObject.PartialExpectedResponse);

			// separate the expected response by &&
			String[] criteria = apiObject.PartialExpectedResponse.split("&&");
			for (String criterion : criteria) {
				jsonHelper.validateByJsonBody(criterion, response);
				jsonHelper.validateByKeywords(criterion, response);
			}
		}
	}
	
	/**
	 * sets the header, content type and body based on specifications
	 * 
	 * @param apiObject
	 * @return
	 */
	public static RequestSpecification evaluateRequestHeaders(ApiObject apiObject) {
		// set request
		RequestSpecification request = null;

		// if no RequestHeaders specified
		if (apiObject.RequestHeaders.isEmpty()) {
			return given();
		}

		// replace parameters for request body
		apiObject.RequestHeaders = dataHelper.replaceParameters(apiObject.RequestHeaders);

		// if Authorization is set
		if (apiObject.RequestHeaders.contains("Authorization:")) {
			String token = apiObject.RequestHeaders.replace("Authorization:", "");
			request = given().header("Authorization", token);
		}

		// if additional request headers
		switch (apiObject.RequestHeaders) {
		case "INVALID_TOKEN":
			request = given().header("Authorization", "invalid");
			break;
		case "NO_TOKEN":
			request = given().header("Authorization", "");
			break;
		default:
			break;
		}

		return request;
	}
	
	public static RequestSpecification evaluateRequestBody(ApiObject apiObject, RequestSpecification request) {
		if(apiObject.RequestBody.isEmpty()) return request;
		
		// set content type
		request = request.contentType(apiObject.ContentType);
		
		// set form data
		if(apiObject.ContentType.contains("form")) {
			request = request.config(RestAssured.config().encoderConfig(io.restassured.config.EncoderConfig.encoderConfig().encodeContentTypeAs("multipart/form-data", ContentType.TEXT)));
			
			String[] formData = apiObject.RequestBody.split(",");
			for(String data : formData) {
				String[] keyValue = data.split(":");
				request = request.formParam(keyValue[0], keyValue[1]);
			}
			return request;
		}
		
		// if json data type
		return request.body(apiObject.RequestBody);
	}
	
	

	/**
	 * sets the header, content type and body based on specifications
	 * 
	 * @param apiObject
	 * @return
	 */
	public static RequestSpecification evaluateOption(ApiObject apiObject, RequestSpecification request) {

		// if no option specified
		if (apiObject.Option.isEmpty()) {
			return request;
		}

		// replace parameters for request body
		apiObject.Option = dataHelper.replaceParameters(apiObject.Option);

		// if additional options
		switch (apiObject.Option) {
		default:
			break;
		}

		return request;
	}

	public static Response evaluateRequest(ApiObject apiObject) {
		Response response = null;
		
		// set request header
		RequestSpecification request = evaluateRequestHeaders(apiObject);
		
		// set request body
		request = evaluateRequestBody(apiObject, request);

		// set options
	    request = evaluateOption(apiObject, request);

		TestLog.logPass("request body: " + Helper.stringRemoveLines(apiObject.RequestBody));
		TestLog.logPass("request type: " + apiObject.Method);


		switch (apiObject.Method) {
		case "POST":
			response = request.when().post(apiObject.UriPath);
			break;
		case "PUT":
			response = request.when().put(apiObject.UriPath);
			break;
		case "PATCH":
			response = request.when().patch(apiObject.UriPath);
			break;
		case "DELETE":
			response = request.when().delete(apiObject.UriPath);
			break;
		case "GET":
			response = request.when().get(apiObject.UriPath);
			break;
		case "OPTIONS":
			response = request.when().options(apiObject.UriPath);
			break;
		case "HEAD":
			response = request.when().head(apiObject.UriPath);
			break;
		default:
			Helper.assertTrue("request type not found", false);
			break;
		}
		TestLog.logPass("response: " + response.getBody().asString());

		return response.then().extract().response();
	}
}