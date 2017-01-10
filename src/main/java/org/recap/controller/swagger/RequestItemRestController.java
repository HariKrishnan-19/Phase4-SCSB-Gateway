package org.recap.controller.swagger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.apache.camel.ProducerTemplate;
import org.recap.ReCAPConstants;
import org.recap.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by hemalathas on 1/11/16.
 */
@RestController
@RequestMapping("/requestItem")
@Api(value = "requestItem", description = "Request Item", position = 1)
public class RequestItemRestController {

    private Logger logger = LoggerFactory.getLogger(RequestItemRestController.class);

    @Value("${server.protocol}")
    String serverProtocol;

    @Value("${scsb.circ.url}")
    String scsbCircUrl;

    @Autowired
    private ProducerTemplate producer;

    @RequestMapping(value = ReCAPConstants.REST_URL_REQUEST_ITEM, method = RequestMethod.POST)
    @ApiOperation(value = "Request Item", notes = "Item Request from Owning institution", nickname = "requestItem")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public ItemResponseInformation itemRequest(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemRequestInformation itemRequestInfo){
        ItemResponseInformation itemResponseInformation = new ItemResponseInformation();
        List itemBarcodes =null;
        try {
            ObjectMapper objectMapper= new ObjectMapper();
            String json ="";
            if(itemRequestInfo.getItemBarcodes() !=null) {
                itemBarcodes = itemRequestInfo.getItemBarcodes();
                itemRequestInfo.setItemBarcodes(null);
                if (itemBarcodes.size() > 1) {
                    for (int i=0;i<itemBarcodes.size();i++){
                        itemRequestInfo.setItemBarcodes(Arrays.asList(itemBarcodes.get(i).toString()));
                        json = objectMapper.writeValueAsString(itemRequestInfo);
                        producer.sendBodyAndHeader(ReCAPConstants.REQUEST_ITEM_QUEUE, json, ReCAPConstants.REQUEST_TYPE_QUEUE_HEADER, itemRequestInfo.getRequestType());
                    }
                } else if (itemBarcodes.size() == 1) {
                    itemRequestInfo.setItemBarcodes(Arrays.asList(itemBarcodes.get(0).toString()));
                    json = objectMapper.writeValueAsString(itemRequestInfo);
                    producer.sendBodyAndHeader(ReCAPConstants.REQUEST_ITEM_QUEUE, json, ReCAPConstants.REQUEST_TYPE_QUEUE_HEADER, itemRequestInfo.getRequestType());
                }
            }
            itemResponseInformation.setSuccess(true);
            itemResponseInformation.setScreenMessage("Message recevied, your request will be processed");
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        logger.info("Message In Queue");
        return itemResponseInformation;
    }

    @RequestMapping(value =     ReCAPConstants.REST_URL_VALIDATE_REQUEST_ITEM , method = RequestMethod.POST)
    @ApiOperation(value = "validateItemRequestInformations",
            notes = "Validate Item Request Informations", nickname = "validateItemRequestInformation", position = 0)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public ResponseEntity validateItemRequest(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemRequestInformation itemRequestInfo){
        ResponseEntity responseEntity = null;
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        try {
            response = restTemplate.postForEntity(serverProtocol + scsbCircUrl + "requestItem/validateItemRequestInformations", itemRequestInfo, String.class).getBody();
        }catch (HttpClientErrorException httpEx){
            HttpStatus statusCode = httpEx.getStatusCode();
            String responseBodyAsString = httpEx.getResponseBodyAsString();
            return new ResponseEntity(responseBodyAsString,getHttpHeaders(),statusCode);
        }catch(Exception ex){
            logger.debug("scsbCircUrl : "+scsbCircUrl);
            responseEntity = new ResponseEntity("Scsb circ Service is Unavailable.", getHttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE);
            return responseEntity;
        }
        responseEntity =  new ResponseEntity(response,getHttpHeaders(), HttpStatus.OK);
        return responseEntity;
    }

    @RequestMapping(value = "/checkoutItem" , method = RequestMethod.POST)
    @ApiOperation(value = "checkoutItem",
            notes = "Checkout Item Request from Owning institution", nickname = "checkoutItem")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem checkoutItemRequest(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemCheckOutRequest itemCheckOutRequest){
        ItemCheckoutResponse itemCheckoutResponse= null;
        ItemRequestInformation itemRequestInfo= new ItemRequestInformation();
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setPatronBarcode(itemCheckOutRequest.getPatronIdentifier());
            itemRequestInfo.setItemBarcodes(itemCheckOutRequest.getItemBarcodes());
            itemRequestInfo.setItemOwningInstitution(itemCheckOutRequest.getItemOwningInstitution());
            itemRequestInfo.setRequestingInstitution(itemCheckOutRequest.getItemOwningInstitution());
            response = restTemplate.postForEntity(serverProtocol + scsbCircUrl + "requestItem/checkoutItem", itemRequestInfo, String.class).getBody();
            ObjectMapper om = new ObjectMapper();
            itemCheckoutResponse = om.readValue(response, ItemCheckoutResponse.class);
        }catch(RestClientException ex){
            logger.error("RestClient : ", ex);
            itemCheckoutResponse.setScreenMessage(ex.getMessage());
        }catch(Exception ex){
            logger.error("Exception : ",ex);
            itemCheckoutResponse.setScreenMessage(ex.getMessage());
        }
        return itemCheckoutResponse;
    }

    @RequestMapping(value = "/checkinItem" , method = RequestMethod.POST)
    @ApiOperation(value = "checkinItem",
            notes = "Checkin Item Request from Owning institution", nickname = "checkinItem")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem checkinItemRequest(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemCheckInRequest itemCheckInRequest){
        ItemCheckinResponse itemCheckinResponse= null;
        ItemRequestInformation itemRequestInfo = new ItemRequestInformation();
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setPatronBarcode(itemCheckInRequest.getPatronIdentifier());
            itemRequestInfo.setItemBarcodes(itemCheckInRequest.getItemBarcodes());
            itemRequestInfo.setItemOwningInstitution(itemCheckInRequest.getItemOwningInstitution());
            itemRequestInfo.setRequestingInstitution(itemCheckInRequest.getItemOwningInstitution());
            response = restTemplate.postForEntity(serverProtocol + scsbCircUrl + "requestItem/checkinItem", itemRequestInfo, String.class).getBody();
            ObjectMapper om = new ObjectMapper();
            itemCheckinResponse = om.readValue(response, ItemCheckinResponse.class);
        }catch(RestClientException ex){
            logger.error("RestClient : ", ex);
        }catch(Exception ex){
            logger.error("Exception : ", ex);
        }
        return itemCheckinResponse;
    }

    @RequestMapping(value = "/holdItem" , method = RequestMethod.POST)
    @ApiOperation(value = "holdItem",
            notes = "hold Item Request from Owning institution", nickname = "holdItem")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem holdItemRequest(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemHoldRequest itemHoldRequest){
        ItemHoldResponse itemHoldResponse= null;
        ItemRequestInformation itemRequestInfo=new ItemRequestInformation();
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setItemBarcodes(itemHoldRequest.getItemBarcodes());
            itemRequestInfo.setItemOwningInstitution(itemHoldRequest.getItemOwningInstitution());
            itemRequestInfo.setRequestingInstitution(itemHoldRequest.getItemOwningInstitution());
            itemRequestInfo.setPatronBarcode(itemHoldRequest.getPatronIdentifier());
            itemRequestInfo.setExpirationDate(itemHoldRequest.getExpirationDate());
            itemRequestInfo.setBibId(itemHoldRequest.getBibId());
            itemRequestInfo.setDeliveryLocation(itemHoldRequest.getPickupLocation());
            itemRequestInfo.setTrackingId(itemHoldRequest.getTrackingId());
            itemRequestInfo.setTitle(itemHoldRequest.getTitle());
            itemRequestInfo.setAuthor(itemHoldRequest.getAuthor());
            itemRequestInfo.setCallNumber(itemHoldRequest.getCallNumber());

            response = restTemplate.postForEntity(serverProtocol + scsbCircUrl + ReCAPConstants.URL_REQUEST_ITEM_HOLD, itemRequestInfo, String.class).getBody();
            ObjectMapper om = new ObjectMapper();
            itemHoldResponse = om.readValue(response, ItemHoldResponse.class);
        }catch(RestClientException ex){
            logger.error("RestClient : "+ ex.getMessage());
            itemHoldResponse.setScreenMessage(ex.getMessage());
        }catch(Exception ex){
            logger.error("Exception : "+ex.getMessage());
            itemHoldResponse.setScreenMessage(ex.getMessage());
        }
        return itemHoldResponse;
    }

    @RequestMapping(value = "/cancelHoldItem" , method = RequestMethod.POST)
    @ApiOperation(value = "cancelHoldItem",
            notes = "Cancel hold Item Request from Owning institution", nickname = "cancelHoldItem")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem cancelHoldItemRequest(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemHoldCancelRequest itemHoldCancelRequest){
        ItemHoldResponse itemHoldResponse= null;
        ItemRequestInformation itemRequestInfo=new ItemRequestInformation();
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setItemBarcodes(itemHoldCancelRequest.getItemBarcodes());
            itemRequestInfo.setItemOwningInstitution(itemHoldCancelRequest.getItemOwningInstitution());
            itemRequestInfo.setRequestingInstitution(itemHoldCancelRequest.getItemOwningInstitution());
            itemRequestInfo.setPatronBarcode(itemHoldCancelRequest.getPatronIdentifier());
            itemRequestInfo.setExpirationDate(itemHoldCancelRequest.getExpirationDate());
            itemRequestInfo.setBibId(itemHoldCancelRequest.getBibId());
            itemRequestInfo.setDeliveryLocation(itemHoldCancelRequest.getPickupLocation());
            itemRequestInfo.setTrackingId(itemHoldCancelRequest.getTrackingId());

            response = restTemplate.postForEntity(serverProtocol + scsbCircUrl + "requestItem/cancelHoldItem", itemRequestInfo, String.class).getBody();
            ObjectMapper om = new ObjectMapper();
            itemHoldResponse = om.readValue(response, ItemHoldResponse.class);
        }catch(RestClientException ex){
            logger.error("RestClient : "+ ex.getMessage());
            itemHoldResponse.setScreenMessage(ex.getMessage());
        }catch(Exception ex){
            logger.error("Exception : "+ex.getMessage());
            itemHoldResponse.setScreenMessage(ex.getMessage());
        }
        return itemHoldResponse;
    }

    @RequestMapping(value = "/createBib" , method = RequestMethod.POST)
    @ApiOperation(value = "createBib",
            notes = "Create Bibliographic Request from Owning institution", nickname = "createBib")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem createBibRequest(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemCreateBibRequest itemCreateBibRequest){
        ItemCreateBibResponse itemCreateBibResponse = new ItemCreateBibResponse();
        ItemRequestInformation itemRequestInfo = new ItemRequestInformation();
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setItemBarcodes(itemCreateBibRequest.getItemBarcodes());
            itemRequestInfo.setPatronBarcode(itemCreateBibRequest.getPatronIdentifier());
            itemRequestInfo.setItemOwningInstitution(itemCreateBibRequest.getItemOwningInstitution());
            itemRequestInfo.setRequestingInstitution (itemCreateBibRequest.getItemOwningInstitution());
            itemRequestInfo.setTitleIdentifier(itemCreateBibRequest.getTitleIdentifier());

            response = restTemplate.postForEntity(serverProtocol + scsbCircUrl + ReCAPConstants.URL_REQUEST_ITEM_CREATEBIB, itemRequestInfo, String.class).getBody();
            ObjectMapper om = new ObjectMapper();
            itemCreateBibResponse = om.readValue(response, ItemCreateBibResponse.class);
        }catch(RestClientException ex){
            logger.error("RestClient : "+ ex.getMessage());
            itemCreateBibResponse.setScreenMessage(ex.getMessage());
        }catch(Exception ex){
            logger.error("Exception : "+ex.getMessage());
            itemCreateBibResponse.setScreenMessage(ex.getMessage());
        }
        return itemCreateBibResponse;
    }

    @RequestMapping(value = "/itemInformation"  , method = RequestMethod.POST)
    @ApiOperation(value = "itemInformation"     , notes = "item Information and status of Circulation", nickname = "itemInformation")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem itemInformation(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson") @RequestBody ItemInformationRequest itemRequestInfo){
        HttpEntity<ItemInformationResponse> responseEntity = null;
        ItemInformationResponse itemInformationResponse =null;
        ItemInformationRequest itemInformationRequest = new ItemInformationRequest();
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemInformationRequest.setItemBarcodes(itemRequestInfo.getItemBarcodes());
            itemInformationRequest.setItemOwningInstitution(itemRequestInfo.getItemOwningInstitution());
            itemInformationRequest.setSource(itemRequestInfo.getSource());
            HttpEntity request = new HttpEntity(itemInformationRequest);
            responseEntity = restTemplate.exchange(serverProtocol + scsbCircUrl +   ReCAPConstants.URL_REQUEST_ITEM_INFORMATION, HttpMethod.POST, request, ItemInformationResponse.class);
            itemInformationResponse = responseEntity.getBody();
        }catch(RestClientException ex){
            logger.error("RestClient : ",ex);
            itemInformationResponse.setScreenMessage(ex.getMessage());
        }catch(Exception ex){
            logger.error("Exception : ",ex);
            itemInformationResponse.setScreenMessage(ex.getMessage());
        }
        return itemInformationResponse;
    }

    @RequestMapping(value = "/recall" , method = RequestMethod.POST)
    @ApiOperation(value = "recall",
            notes = "Recall Item Request from Owning institution", nickname = "RecallItem")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem recallItem(@ApiParam(value = "Parameters for requesting an item" , required = true , name = "requestItemJson")@RequestBody ItemRecalRequest itemRecalRequest){
        ItemRecallResponse itemRecallResponse = new ItemRecallResponse();
        ItemRequestInformation itemRequestInfo=new ItemRequestInformation();
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInfo.setItemBarcodes(itemRecalRequest.getItemBarcodes());
            itemRequestInfo.setItemOwningInstitution(itemRecalRequest.getItemOwningInstitution());
            itemRequestInfo.setRequestingInstitution(itemRecalRequest.getItemOwningInstitution());
            itemRequestInfo.setPatronBarcode(itemRecalRequest.getPatronIdentifier());
            itemRequestInfo.setExpirationDate(itemRecalRequest.getExpirationDate());
            itemRequestInfo.setBibId(itemRecalRequest.getBibId());
            itemRequestInfo.setDeliveryLocation(itemRecalRequest.getPickupLocation());

            response = restTemplate.postForEntity(serverProtocol + scsbCircUrl + ReCAPConstants.URL_REQUEST_ITEM_RECALL, itemRequestInfo, String.class).getBody();
            ObjectMapper om = new ObjectMapper();
            itemRecallResponse = om.readValue(response, ItemRecallResponse.class);
        }catch(RestClientException ex){
            logger.error("RestClient : "+ ex.getMessage());
            itemRecallResponse.setScreenMessage(ex.getMessage());
        }catch(Exception ex){
            logger.error("Exception : "+ex.getMessage());
            itemRecallResponse.setScreenMessage(ex.getMessage());
        }
        return itemRecallResponse;
    }

    @RequestMapping(value = "/patronInformation"  , method = RequestMethod.POST)
    @ApiOperation(value = "patronInformation"     , notes = "Patron Information", nickname = "patronInformation")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    @ResponseBody
    public AbstractResponseItem patronInformation(@ApiParam(value = "Parameters for requesting an patron" , required = true , name = "requestpatron") @RequestBody PatronInformationRequest patronInformationRequest){
        HttpEntity<PatronInformationResponse> responseEntity = null;
        PatronInformationResponse patronInformation =null;
        ItemRequestInformation itemRequestInformation = new ItemRequestInformation();
        RestTemplate restTemplate = new RestTemplate();
        try {
            itemRequestInformation.setPatronBarcode (patronInformationRequest.getPatronIdentifier());
            itemRequestInformation.setItemOwningInstitution(patronInformationRequest.getItemOwningInstitution());
            HttpEntity request = new HttpEntity(itemRequestInformation);
            responseEntity = restTemplate.exchange(serverProtocol + scsbCircUrl +   ReCAPConstants.URL_REQUEST_PATRON_INFORMATION, HttpMethod.POST, request, PatronInformationResponse.class);
            patronInformation = responseEntity.getBody();
        }catch(RestClientException ex){
            logger.error("RestClient : ",ex);
            patronInformation.setScreenMessage(ex.getMessage());
        }catch(Exception ex){
            logger.error("Exception : ",ex);
            patronInformation.setScreenMessage(ex.getMessage());
        }
        return patronInformation;
    }

//    @RequestMapping(value = "/refile"  , method = RequestMethod.POST)
//    @ApiOperation(value = "refile"     , notes = "Re-File", nickname = "patronInformation")
//    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
//    @ResponseBody
    public void refileItem(@ApiParam(value = "Parameters for requesting re-file" , required = true , name = "itemBarcode") @RequestBody ItemRefileRequest itemRefileRequest){

    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(ReCAPConstants.RESPONSE_DATE, new Date().toString());
        return responseHeaders;
    }

}