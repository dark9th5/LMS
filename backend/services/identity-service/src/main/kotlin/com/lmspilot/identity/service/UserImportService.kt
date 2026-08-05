package com.lmspilot.identity.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.identity.api.*
import com.lmspilot.identity.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.text.Normalizer
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

private const val MAX_IMPORT_BYTES = 8L * 1024L * 1024L
private const val MAX_IMPORT_ROWS = 5_000
private const val MAX_SAMPLE_ROWS = 20
private const val MAX_XLSX_ENTRY_BYTES = 16 * 1024 * 1024
private const val MAX_XLSX_TOTAL_BYTES = 32 * 1024 * 1024

/**
 * Reads CSV or the first worksheet of an XLSX file without a heavyweight office dependency.
 * The parser intentionally handles the import subset needed by account provisioning: text,
 * numeric and inline/shared-string cells. Formula results are read from their cached value.
 */
@Component
class UserImportFileParser {
    fun parse(file: MultipartFile): TabularImportData {
        if (file.isEmpty) badRequest("Tệp nhập đang trống")
        if (file.size > MAX_IMPORT_BYTES) badRequest("Tệp nhập vượt quá giới hạn 8 MB")
        val name = file.originalFilename?.trim().orEmpty()
        val bytes = file.bytes
        val parsed = when {
            name.endsWith(".csv", ignoreCase = true) || looksLikeText(bytes) -> parseCsv(bytes)
            name.endsWith(".xlsx", ignoreCase = true) || looksLikeZip(bytes) -> parseXlsx(bytes)
            else -> badRequest("Chỉ hỗ trợ tệp CSV hoặc XLSX")
        }
        if (parsed.headers.isEmpty()) badRequest("Tệp nhập không có dòng tiêu đề")
        if (parsed.rows.isEmpty()) badRequest("Tệp nhập không có dữ liệu")
        if (parsed.rows.size > MAX_IMPORT_ROWS) badRequest("Tệp nhập vượt quá $MAX_IMPORT_ROWS dòng dữ liệu")
        return parsed.copy(fileName = name.ifBlank { "users-import" })
    }

    private fun parseCsv(bytes: ByteArray): TabularImportData {
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val delimiter = detectDelimiter(text)
        val matrix = parseDelimited(text, delimiter)
            .map { row -> row.map(String::trim) }
            .filter { row -> row.any(String::isNotBlank) }
        if (matrix.isEmpty()) return TabularImportData("", emptyList(), emptyList())
        val width = matrix.first().size
        val headers = deduplicateHeaders(matrix.first())
        val rows = matrix.drop(1).mapIndexed { index, row ->
            TabularImportRow(index + 2, pad(row, width))
        }
        return TabularImportData("", headers, rows)
    }

    private fun detectDelimiter(text: String): Char {
        val firstLogicalLine = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val candidates = listOf(',', ';', '\t')
        return candidates.maxByOrNull { candidate -> parseDelimited(firstLogicalLine, candidate).firstOrNull()?.size ?: 0 } ?: ','
    }

    private fun parseDelimited(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            when {
                ch == '"' && inQuotes && index + 1 < text.length && text[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    row.add(cell.toString())
                    cell.setLength(0)
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row.add(cell.toString())
                    cell.setLength(0)
                    rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(ch)
            }
            index++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows
    }

    private fun parseXlsx(bytes: ByteArray): TabularImportData {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            var extractedBytes = 0
            while (entry != null) {
                val wanted = !entry.isDirectory && (
                    entry.name in setOf(
                        "xl/sharedStrings.xml",
                        "xl/workbook.xml",
                        "xl/_rels/workbook.xml.rels",
                    ) || (entry.name.startsWith("xl/worksheets/") && entry.name.endsWith(".xml"))
                )
                if (wanted) {
                    val content = readLimited(zip, MAX_XLSX_ENTRY_BYTES)
                    extractedBytes += content.size
                    if (extractedBytes > MAX_XLSX_TOTAL_BYTES) badRequest("Tệp XLSX giải nén vượt quá giới hạn an toàn")
                    entries[entry.name] = content
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val sheetPath = resolveFirstSheetPath(entries)
        val sheetBytes = entries[sheetPath] ?: badRequest("Không đọc được worksheet đầu tiên trong tệp XLSX")
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val document = parseXml(sheetBytes)
        val rowNodes = document.getElementsByTagNameNS("*", "row")
        val matrix = mutableListOf<List<String>>()
        for (i in 0 until rowNodes.length) {
            val rowElement = rowNodes.item(i) as? Element ?: continue
            val cells = rowElement.getElementsByTagNameNS("*", "c")
            val values = sortedMapOf<Int, String>()
            var maxColumn = -1
            for (j in 0 until cells.length) {
                val cell = cells.item(j) as? Element ?: continue
                val reference = cell.getAttribute("r")
                val column = columnIndex(reference)
                maxColumn = maxOf(maxColumn, column)
                values[column] = readXlsxCell(cell, sharedStrings)
            }
            if (maxColumn >= 0) matrix += (0..maxColumn).map { values[it].orEmpty() }
        }
        val nonEmpty = matrix.map { it.map(String::trim) }.filter { row -> row.any(String::isNotBlank) }
        if (nonEmpty.isEmpty()) return TabularImportData("", emptyList(), emptyList())
        val width = nonEmpty.maxOf { it.size }
        val headers = deduplicateHeaders(pad(nonEmpty.first(), width))
        val rows = nonEmpty.drop(1).mapIndexed { index, row -> TabularImportRow(index + 2, pad(row, width)) }
        return TabularImportData("", headers, rows)
    }

    private fun resolveFirstSheetPath(entries: Map<String, ByteArray>): String {
        val workbook = entries["xl/workbook.xml"]
        val relationships = entries["xl/_rels/workbook.xml.rels"]
        if (workbook != null && relationships != null) {
            val workbookDoc = parseXml(workbook)
            val firstSheet = workbookDoc.getElementsByTagNameNS("*", "sheet").item(0) as? Element
            val relationshipId = firstSheet?.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                ?.ifBlank { firstSheet.getAttribute("r:id") }
            if (!relationshipId.isNullOrBlank()) {
                val relDoc = parseXml(relationships)
                val rels = relDoc.getElementsByTagNameNS("*", "Relationship")
                for (i in 0 until rels.length) {
                    val rel = rels.item(i) as? Element ?: continue
                    if (rel.getAttribute("Id") == relationshipId) {
                        val target = rel.getAttribute("Target").removePrefix("/")
                        return if (target.startsWith("xl/")) target else "xl/${target.removePrefix("../")}" 
                    }
                }
            }
        }
        return entries.keys.filter { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }.sorted().firstOrNull()
            ?: badRequest("Tệp XLSX không có worksheet")
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val document = parseXml(bytes)
        val nodes = document.getElementsByTagNameNS("*", "si")
        return (0 until nodes.length).map { index ->
            val element = nodes.item(index) as Element
            val texts = element.getElementsByTagNameNS("*", "t")
            buildString { for (i in 0 until texts.length) append(texts.item(i).textContent) }
        }
    }

    private fun readXlsxCell(cell: Element, sharedStrings: List<String>): String {
        val type = cell.getAttribute("t")
        if (type == "inlineStr") {
            val texts = cell.getElementsByTagNameNS("*", "t")
            return buildString { for (i in 0 until texts.length) append(texts.item(i).textContent) }
        }
        val value = cell.getElementsByTagNameNS("*", "v").item(0)?.textContent.orEmpty()
        return when (type) {
            "s" -> value.toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
            "b" -> if (value == "1") "true" else "false"
            else -> value
        }
    }

    private fun readLimited(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) badRequest("Một thành phần trong tệp XLSX vượt quá giới hạn an toàn")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun parseXml(bytes: ByteArray) = documentBuilderFactory().newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun documentBuilderFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    }

    private fun columnIndex(reference: String): Int {
        val letters = reference.takeWhile(Char::isLetter).uppercase(Locale.ROOT)
        if (letters.isBlank()) return 0
        return letters.fold(0) { acc, char -> acc * 26 + (char.code - 'A'.code + 1) } - 1
    }

    private fun pad(row: List<String>, width: Int): List<String> = row + List((width - row.size).coerceAtLeast(0)) { "" }

    private fun deduplicateHeaders(raw: List<String>): List<String> {
        val counts = mutableMapOf<String, Int>()
        return raw.mapIndexed { index, value ->
            val base = value.trim().ifBlank { "column_${index + 1}" }
            val count = counts.merge(base.lowercase(Locale.ROOT), 1, Int::plus) ?: 1
            if (count == 1) base else "$base ($count)"
        }
    }

    private fun looksLikeZip(bytes: ByteArray) = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
    private fun looksLikeText(bytes: ByteArray) = bytes.take(512).none { it == 0.toByte() }
    private fun badRequest(message: String): Nothing = throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMPORT_FILE", message)
}

data class TabularImportData(val fileName: String, val headers: List<String>, val rows: List<TabularImportRow>)
data class TabularImportRow(val rowNumber: Int, val values: List<String>)

@Service
class UserImportService(
    private val parser: UserImportFileParser,
    private val users: UserAccountRepository,
    private val roles: RoleRepository,
    private val bulkOperations: BulkOperationRepository,
    private val bulkGuard: BulkOperationGuard,
    private val passwordPolicy: PasswordPolicyService,
    private val refreshTokens: RefreshTokenRepository,
    private val capacity: UserCapacityService,
    private val organization: OrganizationScopeClient,
    private val events: DomainEventPublisher,
    private val mapper: ObjectMapper,
    transactionManager: PlatformTransactionManager,
) {
    private val requiresNew = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun inspect(file: MultipartFile): UserImportInspectionResponse {
        val data = parser.parse(file)
        val detected = detectMapping(data.headers)
        return UserImportInspectionResponse(
            fileName = data.fileName,
            headers = data.headers,
            samples = data.rows.take(MAX_SAMPLE_ROWS).map { row ->
                row.values.mapIndexed { index, value -> data.headers[index] to value }.toMap()
            },
            detectedMapping = detected,
        )
    }

    fun preview(file: MultipartFile, mapping: UserImportMappingRequest): UserImportPreviewResponse {
        val data = parser.parse(file)
        val rows = prepareRows(data, mapping)
        return previewResponse(data.fileName, data.headers, rows)
    }

    @org.springframework.transaction.annotation.Transactional
    fun commit(file: MultipartFile, mapping: UserImportMappingRequest, operationId: String): UserImportCommitResponse {
        if (operationId.isBlank() || operationId.length > 120) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPERATION_ID", "operationId không hợp lệ")
        }
        val requestedBy = CurrentUser.id()
        bulkGuard.replay(operationId, "USER_FILE_IMPORT", requestedBy)?.let { saved ->
            return mapper.readValue(saved.resultJson, UserImportCommitResponse::class.java)
        }

        val data = parser.parse(file)
        val prepared = prepareRows(data, mapping)
        val invalid = prepared.filterNot { it.valid }
        if (mapping.failurePolicy == UserImportFailurePolicy.ATOMIC && invalid.isNotEmpty()) {
            val rejected = UserImportCommitResponse(
                operationId = operationId,
                fileName = data.fileName,
                totalRows = prepared.size,
                created = 0,
                updated = 0,
                skipped = 0,
                failed = prepared.size,
                committed = false,
                results = prepared.map { it.toResult(success = false, runtimeErrors = if (it.valid) listOf("Không nhập do chính sách ATOMIC và tệp còn dòng lỗi") else emptyList()) },
            )
            saveOperation(operationId, rejected)
            return rejected
        }

        val validRows = prepared.filter { it.valid }
        val capacityDelta = validRows.sumOf { it.capacityDelta }

        val results = if (mapping.failurePolicy == UserImportFailurePolicy.ATOMIC) {
            runCatching {
                requiresNew.execute<List<UserImportRowResult>> {
                    capacity.requireCapacity(capacityDelta)
                    validRows.map { applyRow(it, mapping, enforceCapacity = false) }
                } ?: emptyList()
            }.fold(
                onSuccess = { atomic -> atomic + invalid.map { it.toResult(success = false) } },
                onFailure = { cause ->
                    val message = userFacingMessage(cause)
                    prepared.map { row ->
                        row.toResult(
                            success = false,
                            runtimeErrors = if (row.valid) listOf("Đã rollback toàn bộ tệp: $message") else emptyList(),
                        )
                    }
                },
            )
        } else {
            prepared.map { row ->
                if (!row.valid) row.toResult(success = false)
                else runCatching {
                    requiresNew.execute<UserImportRowResult> { applyRow(row, mapping) }
                        ?: row.toResult(success = false, runtimeErrors = listOf("Không nhận được kết quả giao dịch"))
                }.getOrElse { cause -> row.toResult(success = false, runtimeErrors = listOf(userFacingMessage(cause))) }
            }
        }.sortedBy { it.rowNumber }

        val created = results.count { it.success && it.action == UserImportAction.CREATE }
        val updated = results.count { it.success && it.action == UserImportAction.UPDATE }
        val response = UserImportCommitResponse(
            operationId = operationId,
            fileName = data.fileName,
            totalRows = prepared.size,
            created = created,
            updated = updated,
            skipped = results.count { it.success && it.action == UserImportAction.SKIP },
            failed = results.count { !it.success },
            committed = created + updated > 0,
            results = results,
        )
        saveOperation(operationId, response)
        return response
    }

    private fun prepareRows(data: TabularImportData, mapping: UserImportMappingRequest): List<PreparedUserImportRow> {
        val indexes = resolveIndexes(data.headers, mapping)
        val roleCatalog = roles.findAll().associateBy { it.code.uppercase(Locale.ROOT) }
        val referencedUnits = data.rows.mapNotNull { raw ->
            raw.value(indexes.organizationUnitId).trim().takeIf(String::isNotBlank)?.let { str -> runCatching { UUID.fromString(str) }.getOrNull() }
        }.toSet()
        val activeUnits = organization.existingActiveUnitIds(referencedUnits)
        val seenCodes = mutableMapOf<String, Int>()
        val seenUsernames = mutableMapOf<String, Int>()
        return data.rows.map { raw ->
            val errors = mutableListOf<String>()
            val code = raw.value(indexes.code).trim()
            val username = raw.value(indexes.username).trim().lowercase(Locale.ROOT)
            val fullName = raw.value(indexes.fullName).trim()
            val email = raw.value(indexes.email).trim().ifBlank { null }
            val password = raw.value(indexes.password).ifBlank { mapping.defaultPassword.orEmpty() }
            val organizationUnitId = raw.value(indexes.organizationUnitId).trim().takeIf(String::isNotBlank)?.let { str ->
                runCatching { UUID.fromString(str) }.getOrElse { errors += "organizationUnitId không phải UUID hợp lệ"; null }
            }
            if (organizationUnitId != null && organizationUnitId !in activeUnits) {
                errors += "Đơn vị không tồn tại hoặc đã ngừng hoạt động"
            }
            val roleCodes = parseRoles(raw.value(indexes.roleCodes), mapping.defaultRoleCodes)
            val status = raw.value(indexes.status).trim().takeIf(String::isNotBlank)?.let {
                runCatching { AccountStatus.valueOf(it.uppercase(Locale.ROOT)) }.getOrElse { errors += "Trạng thái không hợp lệ: $it"; null }
            } ?: AccountStatus.ACTIVE

            if (code.isBlank()) errors += "Thiếu mã người dùng"
            if (code.length > 80) errors += "Mã người dùng vượt 80 ký tự"
            if (username.length !in 3..120) errors += "Tên đăng nhập phải có 3-120 ký tự"
            if (fullName.isBlank() || fullName.length > 180) errors += "Họ tên là bắt buộc và tối đa 180 ký tự"
            if (email != null && !EMAIL_REGEX.matches(email)) errors += "Email không hợp lệ"
            if (password.length !in 12..128 && !(mapping.mode == UserImportMode.UPSERT && password.isBlank() && !mapping.updatePasswordOnUpsert)) {
                errors += "Mật khẩu tạm thời phải có 12-128 ký tự"
            }
            if (roleCodes.size != 1) errors += "Mỗi tài khoản phải có đúng một vai trò"
            val invalidProductRoles = roleCodes.filterNot { it in setOf("ADMIN", "INSTRUCTOR", "STUDENT") }
            if (invalidProductRoles.isNotEmpty()) errors += "Chỉ chấp nhận ADMIN, INSTRUCTOR hoặc STUDENT"
            val unknownRoles = roleCodes.filterNot { roleCatalog.containsKey(it) }
            if (unknownRoles.isNotEmpty()) errors += "Vai trò không tồn tại: ${unknownRoles.joinToString()}"

            val codeKey = code.lowercase(Locale.ROOT)
            val usernameKey = username.lowercase(Locale.ROOT)
            seenCodes.putIfAbsent(codeKey, raw.rowNumber)?.let { first -> errors += "Mã người dùng trùng dòng $first" }
            seenUsernames.putIfAbsent(usernameKey, raw.rowNumber)?.let { first -> errors += "Tên đăng nhập trùng dòng $first" }

            val byCode = code.takeIf(String::isNotBlank)?.let(users::findByCodeIgnoreCase)
            val byUsername = username.takeIf(String::isNotBlank)?.let(users::findByUsernameIgnoreCase)
            if (byCode != null && byUsername != null && byCode.id != byUsername.id) {
                errors += "Mã người dùng và tên đăng nhập đang thuộc hai tài khoản khác nhau"
            }
            val existing = byCode ?: byUsername
            val action = when {
                existing == null -> UserImportAction.CREATE
                mapping.mode == UserImportMode.CREATE_ONLY -> {
                    errors += "Tài khoản đã tồn tại"
                    UserImportAction.SKIP
                }
                existing.protectedAccount -> {
                    errors += "Không thể cập nhật tài khoản quản trị gốc bằng import"
                    UserImportAction.SKIP
                }
                else -> UserImportAction.UPDATE
            }
            val capacityDelta = when {
                action == UserImportAction.CREATE && status != AccountStatus.DISABLED -> 1
                action == UserImportAction.UPDATE && existing?.status == AccountStatus.DISABLED && status != AccountStatus.DISABLED -> 1
                else -> 0
            }
            PreparedUserImportRow(raw.rowNumber, code, username, fullName, email, organizationUnitId, roleCodes, status, password, existing?.id, action, capacityDelta, errors.distinct())
        }
    }

    private fun applyRow(row: PreparedUserImportRow, mapping: UserImportMappingRequest, enforceCapacity: Boolean = true): UserImportRowResult {
        val assignedRoles = row.roleCodes.map { code ->
            roles.findByCodeIgnoreCase(code) ?: throw ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "Vai trò $code không tồn tại")
        }.toMutableSet()
        val entity = when (row.action) {
            UserImportAction.CREATE -> {
                if (enforceCapacity && row.status != AccountStatus.DISABLED) capacity.requireCapacity(1)
                users.save(
                UserAccountEntity(
                    code = row.code,
                    username = row.username,
                    passwordHash = passwordPolicy.encodeInitial(row.password, row.username, row.code),
                    fullName = row.fullName,
                    email = row.email,
                    organizationUnitId = row.organizationUnitId,
                    status = row.status,
                    accountType = AccountType.USER,
                    protectedAccount = false,
                    mustChangePassword = true,
                    passwordChangedAt = Instant.now(),
                    roles = assignedRoles,
                )
            ).also { created ->
                events.publish(
                    com.lmspilot.contracts.EventTypes.USER_CREATED,
                    "identity-service",
                    created.id.toString(),
                    com.lmspilot.contracts.UserCreatedPayload(created.id, created.username, created.fullName, created.organizationUnitId, created.roles.map { it.code }.toSet()),
                )
            }
            }
            UserImportAction.UPDATE -> {
                val existing = row.existingUserId?.let { users.findById(it).orElse(null) }
                    ?: throw ApiException(HttpStatus.CONFLICT, "USER_CHANGED_DURING_IMPORT", "Tài khoản đã thay đổi trong khi nhập")
                if (existing.protectedAccount) throw ApiException(HttpStatus.CONFLICT, "PROTECTED_ACCOUNT", "Không thể cập nhật tài khoản quản trị gốc")
                if (existing.status == AccountStatus.DISABLED && row.status != AccountStatus.DISABLED) capacity.requireCapacity(1)
                existing.code = row.code
                existing.username = row.username
                existing.fullName = row.fullName
                existing.email = row.email
                existing.organizationUnitId = row.organizationUnitId
                val accessChanged = existing.status != row.status || existing.roles.map { it.code }.toSet() != assignedRoles.map { it.code }.toSet()
                existing.status = row.status
                existing.roles = assignedRoles
                if (mapping.updatePasswordOnUpsert && row.password.isNotBlank()) {
                    passwordPolicy.change(existing, row.password, forceChange = true, reason = "IMPORT_PASSWORD_RESET")
                } else if (accessChanged) {
                    refreshTokens.revokeAllByUserId(existing.id, Instant.now(), "IMPORT_ACCESS_CHANGED")
                }
                existing.updatedAt = Instant.now()
                users.save(existing)
            }
            UserImportAction.SKIP -> return row.toResult(success = true)
        }
        return UserImportRowResult(row.rowNumber, entity.id, row.code, row.username, row.action, true, emptyList())
    }

    private fun resolveIndexes(headers: List<String>, mapping: UserImportMappingRequest): ColumnIndexes {
        val normalized = headers.mapIndexed { index, value -> normalizeHeader(value) to index }.toMap()
        fun required(name: String, aliases: Set<String>): Int {
            val configured = normalizeHeader(name)
            return normalized[configured] ?: aliases.asSequence().map(::normalizeHeader).mapNotNull(normalized::get).firstOrNull()
                ?: throw ApiException(HttpStatus.BAD_REQUEST, "MISSING_IMPORT_COLUMN", "Không tìm thấy cột '$name'")
        }
        fun optional(name: String?, aliases: Set<String>): Int? {
            if (name.isNullOrBlank()) return null
            return normalized[normalizeHeader(name)] ?: aliases.asSequence().map(::normalizeHeader).mapNotNull(normalized::get).firstOrNull()
        }
        return ColumnIndexes(
            code = required(mapping.codeColumn, CODE_ALIASES),
            username = required(mapping.usernameColumn, USERNAME_ALIASES),
            fullName = required(mapping.fullNameColumn, FULL_NAME_ALIASES),
            email = optional(mapping.emailColumn, EMAIL_ALIASES),
            organizationUnitId = optional(mapping.organizationUnitIdColumn, UNIT_ALIASES),
            roleCodes = optional(mapping.roleCodesColumn, ROLE_ALIASES),
            password = optional(mapping.passwordColumn, PASSWORD_ALIASES),
            status = optional(mapping.statusColumn, STATUS_ALIASES),
        )
    }

    private fun detectMapping(headers: List<String>): UserImportDetectedMapping {
        val normalized = headers.associateBy(::normalizeHeader)
        fun match(aliases: Set<String>): String? = aliases.asSequence().map(::normalizeHeader).mapNotNull(normalized::get).firstOrNull()
        return UserImportDetectedMapping(match(CODE_ALIASES), match(USERNAME_ALIASES), match(FULL_NAME_ALIASES), match(EMAIL_ALIASES), match(UNIT_ALIASES), match(ROLE_ALIASES), match(PASSWORD_ALIASES), match(STATUS_ALIASES))
    }

    private fun parseRoles(value: String, defaults: Set<String>): Set<String> {
        val parsed = value.split(',', ';', '|').map(String::trim).filter(String::isNotBlank)
        return (if (parsed.isEmpty()) defaults else parsed.toSet()).map { it.uppercase(Locale.ROOT) }.toSet()
    }

    private fun previewResponse(fileName: String, headers: List<String>, rows: List<PreparedUserImportRow>) = UserImportPreviewResponse(
        fileName = fileName,
        headers = headers,
        totalRows = rows.size,
        validRows = rows.count { it.valid },
        invalidRows = rows.count { !it.valid },
        creates = rows.count { it.valid && it.action == UserImportAction.CREATE },
        updates = rows.count { it.valid && it.action == UserImportAction.UPDATE },
        rows = rows.map { it.preview() },
    )

    private fun saveOperation(operationId: String, response: UserImportCommitResponse) {
        bulkOperations.save(
            BulkOperationEntity(
                operationId = operationId,
                operationType = "USER_FILE_IMPORT",
                requestedBy = CurrentUser.id(),
                resultJson = mapper.writeValueAsString(response),
            )
        )
    }

    private fun userFacingMessage(cause: Throwable): String = when (cause) {
        is ApiException -> cause.message ?: "Không thể nhập dòng dữ liệu"
        else -> cause.message?.takeIf(String::isNotBlank)?.take(300) ?: "Không thể nhập dòng dữ liệu"
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
        private val CODE_ALIASES = setOf("code", "user code", "employee code", "ma nguoi dung", "ma nhan vien")
        private val USERNAME_ALIASES = setOf("username", "user name", "login", "ten dang nhap", "tai khoan")
        private val FULL_NAME_ALIASES = setOf("full name", "fullname", "name", "ho ten", "ten day du")
        private val EMAIL_ALIASES = setOf("email", "mail", "thu dien tu")
        private val UNIT_ALIASES = setOf("organization unit id", "organizationunitid", "unit id", "department id", "don vi id", "phong ban id")
        private val ROLE_ALIASES = setOf("roles", "role", "role codes", "vai tro", "quyen mac dinh")
        private val PASSWORD_ALIASES = setOf("password", "temporary password", "initial password", "mat khau", "mat khau tam")
        private val STATUS_ALIASES = setOf("status", "trang thai")
        fun normalizeHeader(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }
}

private data class ColumnIndexes(
    val code: Int,
    val username: Int,
    val fullName: Int,
    val email: Int?,
    val organizationUnitId: Int?,
    val roleCodes: Int?,
    val password: Int?,
    val status: Int?,
)

private fun TabularImportRow.value(index: Int?): String = index?.let { values.getOrNull(it) }.orEmpty()

private data class PreparedUserImportRow(
    val rowNumber: Int,
    val code: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val organizationUnitId: UUID?,
    val roleCodes: Set<String>,
    val status: AccountStatus,
    val password: String,
    val existingUserId: UUID?,
    val action: UserImportAction,
    val capacityDelta: Int,
    val errors: List<String>,
) {
    val valid: Boolean get() = errors.isEmpty()
    fun preview() = UserImportRowPreview(rowNumber, code, username, fullName, email, organizationUnitId, roleCodes, status, action, valid, errors)
    fun toResult(success: Boolean, runtimeErrors: List<String> = emptyList()) = UserImportRowResult(rowNumber, existingUserId, code, username, action, success, (errors + runtimeErrors).distinct())
}
