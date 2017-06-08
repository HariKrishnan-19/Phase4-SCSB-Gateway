package org.recap.controller;

import org.recap.ReCAPConstants;
import org.recap.model.ScheduleJobRequest;
import org.recap.model.ScheduleJobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Created by rajeshbabuk on 5/4/17.
 */
@RestController
@RequestMapping("/scheduleService")
public class ScheduleJobsController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleJobsController.class);


    /**
     * The Scsb schedule url.
     */
    @Value("${scsb.batch.schedule.url}")
    String scsbScheduleUrl;

    /**
     * Gets scsb schedule url.
     *
     * @return the scsb schedule url
     */
    public String getScsbScheduleUrl() {
        return scsbScheduleUrl;
    }

    /**
     * Sets scsb schedule url.
     *
     * @param scsbScheduleUrl the scsb schedule url
     */
    public void setScsbScheduleUrl(String scsbScheduleUrl) {
        this.scsbScheduleUrl = scsbScheduleUrl;
    }

    /**
     * Gets rest template.
     *
     * @return the rest template
     */
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Schedule job schedule job response.
     *
     * @param scheduleJobRequest the schedule job request
     * @return the schedule job response
     */
    @RequestMapping(value="/scheduleJob", method = RequestMethod.POST)
    public ScheduleJobResponse scheduleJob(@RequestBody ScheduleJobRequest scheduleJobRequest) {
        ScheduleJobResponse scheduleJobResponse = new ScheduleJobResponse();
        try {
            HttpEntity<ScheduleJobRequest> httpEntity = new HttpEntity<>(scheduleJobRequest, getHttpHeaders());

            ResponseEntity<ScheduleJobResponse> responseEntity = getRestTemplate().exchange(getScsbScheduleUrl() + ReCAPConstants.URL_SCHEDULE_JOBS, HttpMethod.POST, httpEntity, ScheduleJobResponse.class);
            scheduleJobResponse = responseEntity.getBody();
        } catch (Exception e) {
            logger.error(ReCAPConstants.LOG_ERROR,e);
            scheduleJobResponse.setMessage(e.getMessage());
        }
        return scheduleJobResponse;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(ReCAPConstants.API_KEY, ReCAPConstants.RECAP);
        return headers;
    }
}
