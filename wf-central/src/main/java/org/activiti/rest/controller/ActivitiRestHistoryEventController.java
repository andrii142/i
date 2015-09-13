package org.activiti.rest.controller;

import org.apache.log4j.Logger;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wf.dp.dniprorada.base.dao.EntityNotFoundException;
import org.wf.dp.dniprorada.base.dao.GenericEntityDao;
import org.wf.dp.dniprorada.base.util.JsonRestUtils;
import org.wf.dp.dniprorada.constant.HistoryEventMessage;
import org.wf.dp.dniprorada.constant.HistoryEventType;
import org.wf.dp.dniprorada.dao.DocumentDao;
import org.wf.dp.dniprorada.dao.HistoryEventDao;
import org.wf.dp.dniprorada.dao.HistoryEvent_ServiceDao;
import org.wf.dp.dniprorada.model.HistoryEvent;
import org.wf.dp.dniprorada.model.HistoryEvent_Service;
import org.wf.dp.dniprorada.model.Region;
import org.wf.dp.dniprorada.util.luna.AlgorithmLuna;
import org.wf.dp.dniprorada.util.luna.CRCInvalidException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/services")
public class ActivitiRestHistoryEventController {

	private static final Logger log = Logger
			.getLogger(ActivitiRestHistoryEventController.class);

	@Autowired
	private HistoryEvent_ServiceDao historyEventServiceDao;

	@Autowired
	private HistoryEventDao historyEventDao;
	
	@Autowired
	@Qualifier("regionDao")
	private GenericEntityDao<Region> regionDao;

	@Autowired
	private DocumentDao documentDao;
	
	/**
	 * check the correctness of nID_Protected (by algorithm Luna) and return the
	 * object of HistoryEvent_Service
	 * 
	 * @param nID_Protected
	 *            -- string ID of event
	 * @return the object (if nID is correct and record exists) otherwise return
	 *         403. CRC Error (wrong nID_Protected) or 403. "Record not found"
	 */
	@RequestMapping(value = "/getHistoryEvent_Service", method = RequestMethod.GET)
	public @ResponseBody
	ResponseEntity<String> getHistoryEvent_Service(
			@RequestParam(value = "nID_Protected") Long nID_Protected)
			throws ActivitiRestException {

		HistoryEvent_Service event_service = null;
		ResponseEntity<String> result;
		try {
			event_service = historyEventServiceDao
					.getHistoryEvent_ServiceByID_Protected(nID_Protected);
			result = JsonRestUtils.toJsonResponse(event_service);
		} catch (EntityNotFoundException e) {
			ActivitiRestException newErr = new ActivitiRestException(
					"BUSINESS_ERR", "Record not found", e);
			newErr.setHttpStatus(HttpStatus.FORBIDDEN);
			throw newErr;
		} catch (CRCInvalidException e) {
			ActivitiRestException newErr = new ActivitiRestException(
					"BUSINESS_ERR", e.getMessage(), e);
			newErr.setHttpStatus(HttpStatus.FORBIDDEN);
			throw newErr;
		} catch (RuntimeException e) {
			throw new ActivitiRestException(e.getMessage(), e);
		}
		return result;
	}


	/**
	 * add the object of HistoryEvent_Service to db
	 * 
	 * @param nID_Task
	 *            -- ID of Task
	 * @param sStatus
	 *            -- string of status
	 * @param nID_Subject
	 *            -- SubjectID (optional)
	 * @param sID_Status
	 *            -- string-status (optional, for algorithm Luna)
	 * @param response
	 * @return the created object
	 */
	@RequestMapping(value = "/addHistoryEvent_Service", method = RequestMethod.GET)
	public @ResponseBody
	ResponseEntity<String> addHistoryEvent_Service(
			@RequestParam(value = "nID_Process") Long nID_Process,
			@RequestParam(value = "nID_Subject") Long nID_Subject,
			@RequestParam(value = "sID_Status") String sID_Status,
			@RequestParam(value = "sProcessInstanceName") String sProcessInstanceName,
			@RequestParam(value = "nID_Service", required=false) Long nID_Service,
			@RequestParam(value = "nID_Region", required=false) Long nID_Region ,
			@RequestParam(value = "sID_UA", required=false) String sID_UA,
			HttpServletResponse response) {
		
		Map<String, String> mParamMessage = new HashMap<String, String>();
	    mParamMessage.put(HistoryEventMessage.SERVICE_NAME, sProcessInstanceName);
            mParamMessage.put(HistoryEventMessage.SERVICE_STATE, sID_Status);
		setHistoryEvent(HistoryEventType.GET_SERVICE, nID_Subject, mParamMessage);

		return JsonRestUtils.toJsonResponse(historyEventServiceDao
				.addHistoryEvent_Service(nID_Process, sID_Status, nID_Subject,
						sID_Status, nID_Service, nID_Region, sID_UA));
	}

	/**
	 * check the correctness of nID_Protected (by algorithm Luna) and update the
	 * object of HistoryEvent_Service in db
	 * 
	 * @param nID_Protected
	 *            -- nID_Protected of event_service
	 * @param sStatus
	 *            -- string of status
	 * @param sID_Status
	 *            -- string-status (optional)
	 * @param response
	 *            return 200ok or "Record not found"
	 */
	@RequestMapping(value = "/updateHistoryEvent_Service", method = RequestMethod.GET)
	public @ResponseBody
	HistoryEvent_Service updateHistoryEvent_Service(
			@RequestParam(value = "nID_Process", required = false) Long nID_Process,
			@RequestParam(value = "sID_Status") String sID_Status,
			//@RequestParam(value = "nID_Subject", required = false) Long nID_Subject,
			//@RequestParam(value = "nID_Task") Long nID_Task,
			//@RequestParam(value = "sHistoryEventType") HistoryEventType sHistoryEventType,
			HttpServletResponse response) {
		Long nID_Protected = AlgorithmLuna.getProtectedNumber(nID_Process);
		Long nID_Subject = historyEventServiceDao.getHistoryEvent_ServiceBynID_Task(nID_Process).getnID_Subject();
		Map<String, String> mParamMessage = new HashMap<String, String>();
	    mParamMessage.put(HistoryEventMessage.SERVICE_STATE, sID_Status);
	    mParamMessage.put(HistoryEventMessage.TASK_NUMBER, String.valueOf(nID_Protected));
		setHistoryEvent(HistoryEventType.ACTIVITY_STATUS_NEW, nID_Subject, mParamMessage);
		HistoryEvent_Service historyEvent_Service = historyEventServiceDao
				.getHistoryEvent_ServiceBynID_Task(nID_Process);
		if (historyEvent_Service != null) {
			boolean isChanged = false;
			if (!historyEvent_Service.getsStatus().equals(sID_Status)) {
				historyEvent_Service.setsStatus(sID_Status);
				isChanged = true;
			}
			if (sID_Status != null
					&& !sID_Status.equals(historyEvent_Service.getsID_Status())) {
				historyEvent_Service.setsID_Status(sID_Status);
				isChanged = true;
			}
			if (isChanged) {
				historyEventServiceDao
						.updateHistoryEvent_Service(historyEvent_Service);
			}
		}
		return historyEvent_Service;
	}

	
	//################ HistoryEvent services ###################

	@RequestMapping(value = "/setHistoryEvent", method = RequestMethod.POST)
	public @ResponseBody
	Long setHistoryEvent(
			@RequestParam(value = "nID_Subject", required = false) long nID_Subject,
			@RequestParam(value = "nID_HistoryEventType", required = false) Long nID_HistoryEventType,
			@RequestParam(value = "sEventName", required = false) String sEventName_Custom,
			@RequestParam(value = "sMessage") String sMessage,

			HttpServletRequest request, HttpServletResponse httpResponse)
			throws IOException {

		return historyEventDao.setHistoryEvent(nID_Subject,
				  nID_HistoryEventType, sEventName_Custom, sMessage);

	}
	
	@RequestMapping(value = "/getHistoryEvent", method = RequestMethod.GET)
	public @ResponseBody
	HistoryEvent getHistoryEvent(@RequestParam(value = "nID") Long id) {
		return historyEventDao.getHistoryEvent(id);
	}

	@RequestMapping(value = "/getHistoryEvents", method = RequestMethod.GET)
	public @ResponseBody
	List<HistoryEvent> getHistoryEvents(
			@RequestParam(value = "nID_Subject") long nID_Subject) {
		return historyEventDao.getHistoryEvents(nID_Subject);
	}
	
	
	@RequestMapping(value = "/getStatisticServiceCounts", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody
	String getStatisticServiceCounts(@RequestParam(value = "nID_Service") Long nID_Service) {
		
            
            
		List<Map<String, Object>> listOfHistoryEventsWithMeaningfulNames = new LinkedList<Map<String,Object>>();
            
                listOfHistoryEventsWithMeaningfulNames = getListOfHistoryEvents(nID_Service);

                return JSONValue.toJSONString(listOfHistoryEventsWithMeaningfulNames);
	}

	private List<Map<String, Object>> getListOfHistoryEvents(Long nID_Service){
                List<Map<String, Object>> listOfHistoryEventsWithMeaningfulNames = new LinkedList<Map<String,Object>>();
            
            
		List<Map<String, Long>> listOfHistoryEvents = historyEventServiceDao.getHistoryEvent_ServiceBynID_Service(nID_Service);
		
                //Map<String, Long> m = new HashMap();
                //m.put("Київ", nID_Service);
		  //currMap.get("sName")
		for (Map<String, Long> currMap : listOfHistoryEvents){
			  Region region = regionDao.findByIdExpected(currMap.get("sName"));
			  Map<String, Object> currMapWithName = new HashMap<String, Object>();
			  
			  currMapWithName.put("sName", region.getName());
                          
			  //currMapWithName.put("nCount", currMap.get("nCount"));
                          /*
                            https://igov.org.ua/service/661/general - 43
                            https://igov.org.ua/service/655/generall - 75
                            https://igov.org.ua/service/176/general - 546
                            https://igov.org.ua/service/654/general - 307                          
                          */
                          /*


                          */
                          Long nCount = currMap.get("nCount");
                          if(false){
                          }else if(nID_Service==661){
                              if("1200000000".equals(region.getsID_UA()) || "1200000000".equals(region.getsID_UA())){
                                nCount+=43;
                              }
                          }else if(nID_Service==665){
                              if("1200000000".equals(region.getsID_UA()) || "1200000000".equals(region.getsID_UA())){
                                  nCount+=75;
                              }
                          }else if(nID_Service==176){
                              if("1200000000".equals(region.getsID_UA()) || "1200000000".equals(region.getsID_UA())){
                                nCount+=546;
                              }
                          }else if(nID_Service==654){
                              if("1200000000".equals(region.getsID_UA()) || "1200000000".equals(region.getsID_UA())){
                                nCount+=307;
                              }
                          }else if(nID_Service==159){
                            /*
                            https://igov.org.ua/service/159/general
                            Днепропетровская область - 53
                            Киевская область - 69
1;Дніпропетровська;"1200000000"
5;Київ;"8000000000"
16;Київська;"3200000000"
                              
                            */
                              if("1200000000".equals(region.getsID_UA()) || "1200000000".equals(region.getsID_UA())){
                                nCount+=53;
                              }else if("8000000000".equals(region.getsID_UA()) || "3200000000".equals(region.getsID_UA())){
                                nCount+=69;
                              }
                          }else if(nID_Service==1){
                              /*
                            https://igov.org.ua/service/1/general
                            Днепропетровская область - 812
                              */
                              /*if("".equals(region.getsID_UA())){
                                nCount+=53;
                              }else if("".equals(region.getsID_UA())){
                                nCount+=69;
                              }*/
                              if("1200000000".equals(region.getsID_UA()) || "1200000000".equals(region.getsID_UA())){
                                nCount+=812;
                              }
                          }else if(nID_Service==4){
                              /*
                            https://igov.org.ua/service/4/general - 
                            Днепропетровская область - услуга временно приостановлена
                            по иным регионам заявок вне было.                          
                              */
                              nCount+=0;
                          }else if(nID_Service==0){
                              nCount+=0;
                          //region.getsID_UA()
                          }
			  
                          
                          
                          if(nID_Service==159){
                                log.info("[getListOfHistoryEvents]nID_Service="+nID_Service);
                                List<Map<String, Object>> am = new LinkedList<Map<String,Object>>();
                                am = getListOfHistoryEvents(new Long(726));
                                //am.get(0).get("nCount");
                                /*if(am.size()>0){
                                    if(am.get(0).containsKey("nCount")){
                                        String s = (String)am.get(0).get("nCount");
                                        if(s!=null){
                                            Long n = new Long(s);
                                            nCount+=n;
                                        }
                                    }
                                }*/
                                nCount+=getCountFromStatisticArrayMap(am);
                                getListOfHistoryEvents(new Long(727));
                                nCount+=getCountFromStatisticArrayMap(am);
                                getListOfHistoryEvents(new Long(728));
                                nCount+=getCountFromStatisticArrayMap(am);
                                getListOfHistoryEvents(new Long(729));
                                nCount+=getCountFromStatisticArrayMap(am);
                                getListOfHistoryEvents(new Long(730));
                                nCount+=getCountFromStatisticArrayMap(am);
                                getListOfHistoryEvents(new Long(731));
                                nCount+=getCountFromStatisticArrayMap(am);
                                getListOfHistoryEvents(new Long(732));
                                nCount+=getCountFromStatisticArrayMap(am);
                                getListOfHistoryEvents(new Long(733));
                                nCount+=getCountFromStatisticArrayMap(am);
                          }

			  currMapWithName.put("nCount", nCount);
                          
			  listOfHistoryEventsWithMeaningfulNames.add(currMapWithName);
                          
                          
		  }       
                
                return listOfHistoryEventsWithMeaningfulNames;
        }
                
                
	private Long getCountFromStatisticArrayMap(List<Map<String, Object>> am){
            Long n= new Long(0);
            log.info("[getCountFromStatisticArrayMap]am="+am);
            if(am.size()>0){
                if(am.get(0).containsKey("nCount")){
                    String s = (String)am.get(0).get("nCount");
                    if(s!=null){
                        n = new Long(s);
                        log.info("[getCountFromStatisticArrayMap]n="+n);
                        //nCount+=n;
                    }
                }
            }
            return n;
        }
        
        
	private void setHistoryEvent(HistoryEventType eventType,
			Long nID_Subject, Map<String, String> mParamMessage) {
		try {
			String eventMessage = HistoryEventMessage.createJournalMessage(
					eventType, mParamMessage);
			historyEventDao.setHistoryEvent(nID_Subject, eventType.getnID(),
					eventMessage, eventMessage);
		} catch (IOException e) {
			log.error("error during creating HistoryEvent", e);
		}
	}

}
