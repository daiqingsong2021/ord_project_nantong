package com.wisdom.webservice.contract.entity;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 3.2.10
 * 2019-08-28T19:13:07.600+08:00
 * Generated source version: 3.2.10
 *
 */
@WebService(targetNamespace = "http://tempuri.org/", name = "GCServiceSoap")
@XmlSeeAlso({ObjectFactory.class})
public interface GCServiceSoap {

    @WebMethod(operationName = "GetToDoList", action = "http://tempuri.org/GetToDoList")
    @RequestWrapper(localName = "GetToDoList", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetToDoList")
    @ResponseWrapper(localName = "GetToDoListResponse", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetToDoListResponse")
    @WebResult(name = "GetToDoListResult", targetNamespace = "http://tempuri.org/")
    public EntityResponseOfWFTodoList getToDoList(
            @WebParam(name = "PartID", targetNamespace = "http://tempuri.org/") String partID);

    @WebMethod(operationName = "GetDoneList", action = "http://tempuri.org/GetDoneList")
    @RequestWrapper(localName = "GetDoneList", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetDoneList")
    @ResponseWrapper(localName = "GetDoneListResponse", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetDoneListResponse")
    @WebResult(name = "GetDoneListResult", targetNamespace = "http://tempuri.org/")
    public EntityResponseOfWFDoneList getDoneList(
            @WebParam(name = "PartID", targetNamespace = "http://tempuri.org/") String partID);

    @WebMethod(operationName = "GetPaymentByContractCode", action = "http://tempuri.org/GetPaymentByContractCode")
    @RequestWrapper(localName = "GetPaymentByContractCode", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetPaymentByContractCode")
    @ResponseWrapper(localName = "GetPaymentByContractCodeResponse", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetPaymentByContractCodeResponse")
    @WebResult(name = "GetPaymentByContractCodeResult", targetNamespace = "http://tempuri.org/")
    public EntityResponseOfPaymentForGC getPaymentByContractCode(
            @WebParam(name = "contrctCode", targetNamespace = "http://tempuri.org/") String contrctCode);

    @WebMethod(operationName = "GetContractListByCode", action = "http://tempuri.org/GetContractListByCode")
    @RequestWrapper(localName = "GetContractListByCode", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetContractListByCode")
    @ResponseWrapper(localName = "GetContractListByCodeResponse", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetContractListByCodeResponse")
    @WebResult(name = "GetContractListByCodeResult", targetNamespace = "http://tempuri.org/")
    public EntityResponseOfContractListForGC getContractListByCode(
            @WebParam(name = "contrctCode", targetNamespace = "http://tempuri.org/") String contrctCode);

    @WebMethod(operationName = "GetSection", action = "http://tempuri.org/GetSection")
    @RequestWrapper(localName = "GetSection", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetSection")
    @ResponseWrapper(localName = "GetSectionResponse", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetSectionResponse")
    @WebResult(name = "GetSectionResult", targetNamespace = "http://tempuri.org/")
    public EntityResponseOfSectionEntity getSection(
            @WebParam(name = "Request", targetNamespace = "http://tempuri.org/") EntityRequestOfSectionEntity request);

    @WebMethod(operationName = "GetAllContractList", action = "http://tempuri.org/GetAllContractList")
    @RequestWrapper(localName = "GetAllContractList", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetAllContractList")
    @ResponseWrapper(localName = "GetAllContractListResponse", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetAllContractListResponse")
    @WebResult(name = "GetAllContractListResult", targetNamespace = "http://tempuri.org/")
    public EntityResponseOfContractListForGC getAllContractList(
            @WebParam(name = "request", targetNamespace = "http://tempuri.org/")
                    EntityRequestOfContractListForGC request);

    @WebMethod(operationName = "GetContractBySegCode", action = "http://tempuri.org/GetContractBySegCode")
    @RequestWrapper(localName = "GetContractBySegCode", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetContractBySegCode")
    @ResponseWrapper(localName = "GetContractBySegCodeResponse", targetNamespace = "http://tempuri.org/", className = "com.wisdom.acm.szxm.common.webservice.contract.entity.GetContractBySegCodeResponse")
    @WebResult(name = "GetContractBySegCodeResult", targetNamespace = "http://tempuri.org/")
    public EntityResponseOfSegContractEntity getContractBySegCode(
            @WebParam(name = "SegCode", targetNamespace = "http://tempuri.org/") String segCode);
}
