package com.lmspilot.operations.api
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.operations.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

data class ServiceHealth(val name:String,val status:String,val version:String?=null,val details:Map<String,Any?> = emptyMap())
data class OperationRequest(val parameters:Map<String,String> = emptyMap())
data class OperationJobResponse(val id:UUID,val type:OperationType,val status:OperationStatus,val requestedBy:UUID,val requestedAt:Instant,val startedAt:Instant?,val finishedAt:Instant?,val resultJson:String?,val errorMessage:String?)
@Service
class OperationsService(private val repository:OperationJobRepository,private val mapper:ObjectMapper,@Value("\${operations.service-urls:}") rawUrls:String){
 private val serviceUrls=rawUrls.split(',').mapNotNull{val p=it.split('=',limit=2);if(p.size==2)p[0] to p[1] else null}.toMap()
 fun health():List<ServiceHealth> = serviceUrls.map{(name,url)->runCatching{val body=RestClient.create().get().uri("$url/actuator/health").retrieve().body(Map::class.java)?:emptyMap<String,Any>();ServiceHealth(name,body["status"]?.toString()?:"UNKNOWN",details=body.entries.associate{it.key.toString() to it.value})}.getOrElse{ServiceHealth(name,"DOWN",details=mapOf("error" to (it.message?:"unavailable")))}}
 @Transactional(readOnly=true) fun jobs()=repository.findAllByOrderByRequestedAtDesc().map{it.response()}
 @Transactional fun request(type:OperationType,input:OperationRequest):OperationJobResponse{
  if(type==OperationType.RESTORE && input.parameters["confirmation"]!="RESTORE")throw ApiException(HttpStatus.BAD_REQUEST,"RESTORE_CONFIRMATION_REQUIRED","Phục hồi yêu cầu confirmation=RESTORE")
  return repository.save(OperationJobEntity(type=type,requestedBy=CurrentUser.id(),parametersJson=mapper.writeValueAsString(input.parameters))).response()
 }
 @Transactional fun complete(id:UUID,success:Boolean,result:Map<String,Any?>):OperationJobResponse{val e=repository.findById(id).orElseThrow{ApiException(HttpStatus.NOT_FOUND,"JOB_NOT_FOUND","Không tìm thấy job")};e.status=if(success)OperationStatus.SUCCEEDED else OperationStatus.FAILED;e.startedAt=e.startedAt?:Instant.now();e.finishedAt=Instant.now();e.resultJson=mapper.writeValueAsString(result);e.errorMessage=if(success)null else result["error"]?.toString();return e.response()}
}
private fun OperationJobEntity.response()=OperationJobResponse(id,type,status,requestedBy,requestedAt,startedAt,finishedAt,resultJson,errorMessage)
@RestController @RequestMapping("/api/v1/operations") @PreAuthorize("hasAuthority('${Permissions.OPERATIONS_MANAGE}')")
class OperationsController(private val service:OperationsService){@GetMapping("/health")fun health()=service.health();@GetMapping("/jobs")fun jobs()=service.jobs();@PostMapping("/jobs/{type}")fun request(@PathVariable type:OperationType,@Valid @RequestBody input:OperationRequest)=service.request(type,input)}
