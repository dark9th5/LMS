package com.lmspilot.operations.domain
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class OperationType { BACKUP, RESTORE, UPDATE, ROLLBACK, MAINTENANCE }
enum class OperationStatus { REQUESTED, RUNNING, SUCCEEDED, FAILED, CANCELLED }
@Entity @Table(name="operation_jobs")
class OperationJobEntity(
 @Id var id:UUID=UUID.randomUUID(),
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) var type:OperationType=OperationType.BACKUP,
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) var status:OperationStatus=OperationStatus.REQUESTED,
 @Column(nullable=false) var requestedBy:UUID=UUID.randomUUID(),
 @Column(nullable=false) var requestedAt:Instant=Instant.now(), var startedAt:Instant?=null,var finishedAt:Instant?=null,
 @Column(columnDefinition="text") var parametersJson:String="{}",@Column(columnDefinition="text") var resultJson:String?=null,@Column(columnDefinition="text") var errorMessage:String?=null)
interface OperationJobRepository:org.springframework.data.jpa.repository.JpaRepository<OperationJobEntity,UUID>{fun findAllByOrderByRequestedAtDesc():List<OperationJobEntity>}
