package cn.puji.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import cn.puji.app.domain.*
import java.util.UUID

internal inline fun <T> ledgerTransaction(db: SQLiteDatabase, block: () -> T): T {
    if (db.inTransaction()) return block()
    db.beginTransaction()
    return try { val result=block(); db.setTransactionSuccessful(); result } finally { db.endTransaction() }
}

class InventoryRepository(
    internal val database: PujiDatabase,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    fun commit(cart: Cart, requestId: String = UUID.randomUUID().toString()): Result<Long> = runCatching {
        val db=database.writableDatabase
        ledgerTransaction(db) {
            existingRequest(db,requestId) ?: run {
                InventoryService.validate(cart,checkStockSnapshot=false).getOrThrow()
                val now=nowProvider()
                val id=newDocument(db,cart.type,cart.totalCents,cart.note,now,requestId)
                cart.lines.forEach { line ->
                    val p=if(line.product.id<=0) {
                        require(cart.type==DocumentType.INBOUND) { "新商品请先入货" }
                        resolveNewProduct(db,line.product,now)
                    } else currentProduct(db,line.product.id)
                    require(p.active) { "${p.name}已停用" }
                    val amount=Math.multiplyExact(line.unitPriceCents,line.quantity.toLong())
                    val isIn=cart.type==DocumentType.INBOUND
                    val cost=if(isIn) amount else outgoingCost(p,line.quantity)
                    appendLine(db,id,p,line.quantity,line.unitPriceCents,cost,if(isIn) line.quantity else -line.quantity,if(isIn) cost else -cost,now)
                }
                id
            }
        }
    }

    internal fun opening(db:SQLiteDatabase, productId:Long, quantity:Int, unitCostCents:Long, now:Long):Long {
        val id=newDocument(db,DocumentType.OPENING,0,"期初库存",now,UUID.randomUUID().toString())
        val cost=Math.multiplyExact(unitCostCents,quantity.toLong())
        appendLine(db,id,currentProduct(db,productId),quantity,unitCostCents,cost,quantity,cost,now)
        return id
    }

    fun count(productId:Long, actualQuantity:Int, expectedQuantity:Int, unitCostCents:Long, note:String):Result<Long> = runCatching {
        require(actualQuantity>=0 && expectedQuantity>=0 && unitCostCents>=0) { "数量和成本不能为负数" }
        val db=database.writableDatabase
        ledgerTransaction(db) {
            val p=currentProduct(db,productId)
            require(p.active) { "商品已停用" }
            require(p.stockQuantity==expectedQuantity) { "库存已变化，请重新核对实点数量" }
            val delta=actualQuantity-expectedQuantity
            val type=if(delta>=0) DocumentType.STOCK_GAIN else DocumentType.STOCK_LOSS
            val now=nowProvider()
            val id=newDocument(db,type,0,note,now,UUID.randomUUID().toString())
            db.execSQL("UPDATE documents SET expected_quantity=?,actual_quantity=? WHERE id=?",arrayOf<Any>(expectedQuantity,actualQuantity,id))
            if(delta!=0) {
                val q=if(delta>0) delta else -delta
                val cost=if(delta>0) Math.multiplyExact(unitCostCents,q.toLong()) else outgoingCost(p,q)
                appendLine(db,id,p,q,if(delta>0) unitCostCents else cost/q,cost,delta,if(delta>0) cost else -cost,now)
            }
            id
        }
    }

    fun writeOff(productId:Long, quantity:Int, note:String):Result<Long> = runCatching {
        require(quantity>0) { "数量必须大于0" }
        require(note.isNotBlank()) { "请填写报损原因" }
        val db=database.writableDatabase
        ledgerTransaction(db) {
            val p=currentProduct(db,productId); require(p.active) { "商品已停用" }
            val cost=outgoingCost(p,quantity)
            val now=nowProvider(); val id=newDocument(db,DocumentType.WRITE_OFF,0,note,now,UUID.randomUUID().toString())
            appendLine(db,id,p,quantity,cost/quantity,cost,-quantity,-cost,now)
            id
        }
    }

    fun returnLines(documentId:Long, quantities:Map<Long,Int>, requestId:String):Result<Long> = runCatching {
        val db=database.writableDatabase
        ledgerTransaction(db) {
            existingRequest(db,requestId) ?: run {
                require(quantities.isNotEmpty() && quantities.values.all { it>0 }) { "请选择退货商品和数量" }
                val original=document(db,documentId) ?: error("原单不存在")
                require(original.type==DocumentType.OUTBOUND || original.type==DocumentType.INBOUND) { "只能从原出货或入货单退货" }
                require(!isReversed(db,documentId)) { "原单已冲正" }
                require(quantities.keys.all { key -> original.lines.any { it.id==key } }) { "退货明细不属于原单" }
                val selected=original.lines.filter { quantities.containsKey(it.id) }
                var total=0L
                selected.forEach { line ->
                    val q=quantities.getValue(line.id)
                    require(q<=line.quantity-line.returnedQuantity) { "${line.productName}退货数量超过可退数量" }
                    total=Math.addExact(total,Math.multiplyExact(line.unitPriceCents,q.toLong()))
                }
                val type=if(original.type==DocumentType.OUTBOUND) DocumentType.SALES_RETURN else DocumentType.PURCHASE_RETURN
                val now=nowProvider()
                val id=newDocument(db,type,total,"关联原单 #$documentId",now,requestId,documentId,original.type)
                var difference=0L
                selected.forEach { line ->
                    val q=quantities.getValue(line.id); val p=currentProduct(db,line.productId)
                    val cost=if(type==DocumentType.SALES_RETURN) InventoryCost.returnCost(line.lineCostCents,line.quantity,line.returnedQuantity,q,line.returnedCostCents)
                        else outgoingCost(p,q)
                    val diff=if(type==DocumentType.PURCHASE_RETURN) Math.subtractExact(cost,Math.multiplyExact(line.unitPriceCents,q.toLong())) else 0L
                    difference=Math.addExact(difference,diff)
                    val sign=if(type==DocumentType.SALES_RETURN) 1 else -1
                    appendLine(db,id,p,q,line.unitPriceCents,cost,q*sign,cost*sign,now,line.id,diff)
                }
                db.execSQL("UPDATE documents SET valuation_difference_cents=? WHERE id=?",arrayOf(difference,id))
                id
            }
        }
    }

    fun reverse(documentId:Long, requestId:String):Result<Long> = runCatching {
        val db=database.writableDatabase
        ledgerTransaction(db) {
            existingRequest(db,requestId) ?: run {
                val original=document(db,documentId) ?: error("原单不存在")
                require(original.type!=DocumentType.REVERSAL && !isReversed(db,documentId)) { "该单不能重复冲正" }
                val dependencies=db.rawQuery("""SELECT COUNT(*) FROM documents d WHERE d.original_document_id=?
                    AND d.type IN ('SALES_RETURN','PURCHASE_RETURN')
                    AND NOT EXISTS(SELECT 1 FROM documents r WHERE r.type='REVERSAL' AND r.original_document_id=d.id)""",arrayOf(documentId.toString())).use { it.moveToFirst();it.getInt(0) }
                require(dependencies==0) { "原单存在退货，请先冲正关联退货" }
                val now=nowProvider()
                val id=newDocument(db,DocumentType.REVERSAL,original.totalCents,"冲正原单 #$documentId",now,requestId,documentId,original.type)
                var difference=0L
                original.lines.forEach { line ->
                    val p=currentProduct(db,line.productId)
                    val delta=-line.quantityDelta
                    val removeAtCurrent=original.type==DocumentType.INBOUND || original.type==DocumentType.OPENING || original.type==DocumentType.STOCK_GAIN
                    val cost=if(removeAtCurrent) outgoingCost(p,line.quantity) else line.lineCostCents
                    val costDelta=if(removeAtCurrent) -cost else -line.stockCostDeltaCents
                    val diff=when(original.type) {
                        DocumentType.INBOUND -> Math.subtractExact(cost,line.lineTotalCents)
                        DocumentType.PURCHASE_RETURN -> -line.valuationDifferenceCents
                        else -> 0L
                    }
                    difference=Math.addExact(difference,diff)
                    appendLine(db,id,p,line.quantity,line.unitPriceCents,cost,delta,costDelta,now,line.id,diff)
                }
                db.execSQL("UPDATE documents SET valuation_difference_cents=? WHERE id=?",arrayOf(difference,id))
                id
            }
        }
    }

    fun documents(type:DocumentType?=null, range:DateRange?=null, query:String="", limit:Int?=null, offset:Int=0):List<StockDocument> {
        require(offset>=0 && (limit==null || limit>0))
        val clauses=mutableListOf<String>();val args=mutableListOf<String>()
        type?.let { clauses+="d.type=?";args+=it.name }
        range?.let { clauses+="d.created_at>=? AND d.created_at<?";args+=it.startInclusive.toString();args+=it.endExclusive.toString() }
        if(query.isNotBlank()) {
            clauses+="(CAST(d.id AS TEXT) LIKE ? OR d.uuid LIKE ? OR EXISTS(SELECT 1 FROM document_lines l WHERE l.document_id=d.id AND (l.product_name_snapshot LIKE ? OR l.barcode_snapshot LIKE ?)))"
            repeat(4) { args+="%${query.trim()}%" }
        }
        val where=if(clauses.isEmpty()) "" else " WHERE "+clauses.joinToString(" AND ")
        val page=if(limit!=null) " LIMIT $limit OFFSET $offset" else if(offset>0) " LIMIT -1 OFFSET $offset" else ""
        val db=database.readableDatabase
        val headers=db.rawQuery("SELECT d.* FROM documents d$where ORDER BY d.created_at DESC,d.id DESC$page",args.toTypedArray()).use { c ->
            buildList { while(c.moveToNext()) add(c.toDocument()) }
        }
        if(headers.isEmpty()) return emptyList()
        // Bound variable counts stay below old Android SQLite's 999-variable limit.
        val byDocument=mutableMapOf<Long,MutableList<DocumentLine>>()
        headers.map { it.id }.chunked(500).forEach { ids ->
            db.rawQuery("""SELECT l.*,COALESCE((SELECT SUM(r.quantity) FROM document_lines r JOIN documents rd ON rd.id=r.document_id
                WHERE r.original_line_id=l.id AND rd.type IN ('SALES_RETURN','PURCHASE_RETURN')
                AND NOT EXISTS(SELECT 1 FROM documents rv WHERE rv.original_document_id=rd.id AND rv.type='REVERSAL')),0) AS returned_quantity,
                COALESCE((SELECT SUM(r.line_cost_cents) FROM document_lines r JOIN documents rd ON rd.id=r.document_id
                    WHERE r.original_line_id=l.id AND rd.type IN ('SALES_RETURN','PURCHASE_RETURN')
                    AND NOT EXISTS(SELECT 1 FROM documents rv WHERE rv.original_document_id=rd.id AND rv.type='REVERSAL')),0) AS returned_cost_cents
                FROM document_lines l WHERE l.document_id IN (${ids.joinToString(",") { "?" }}) ORDER BY l.id""",ids.map { it.toString() }.toTypedArray()).use { c ->
                while(c.moveToNext()) { val line=c.toLine();byDocument.getOrPut(line.documentId) { mutableListOf() }.add(line) }
            }
        }
        return headers.map { it.copy(lines=byDocument[it.id].orEmpty()) }
    }

    fun findDocument(id:Long):StockDocument? = document(database.readableDatabase,id)

    fun documentForRequest(requestId:String):StockDocument? {
        val db=database.readableDatabase
        val id=existingRequest(db,requestId) ?: return null
        return document(db,id)
    }

    fun stockHistory(productId:Long):List<StockHistoryEntry> = database.readableDatabase.rawQuery(
        "SELECT m.*,d.note FROM stock_movements m JOIN documents d ON d.id=m.document_id WHERE m.product_id=? ORDER BY m.created_at DESC,m.id DESC",arrayOf(productId.toString()),
    ).use { c -> buildList { while(c.moveToNext()) add(StockHistoryEntry(c.long("id"),c.long("document_id"),DocumentType.valueOf(c.string("type")),c.int("quantity_delta"),c.int("stock_after"),c.long("stock_cost_delta_cents"),c.long("stock_cost_after_cents"),c.long("created_at"),c.string("note"))) } }

    private fun document(db:SQLiteDatabase,id:Long):StockDocument? {
        val header=db.rawQuery("SELECT * FROM documents WHERE id=?",arrayOf(id.toString())).use { if(it.moveToFirst()) it.toDocument() else null } ?: return null
        val lines=db.rawQuery("""SELECT l.*,COALESCE((SELECT SUM(r.quantity) FROM document_lines r JOIN documents rd ON rd.id=r.document_id
            WHERE r.original_line_id=l.id AND rd.type IN ('SALES_RETURN','PURCHASE_RETURN')
            AND NOT EXISTS(SELECT 1 FROM documents rv WHERE rv.original_document_id=rd.id AND rv.type='REVERSAL')),0) AS returned_quantity,
                COALESCE((SELECT SUM(r.line_cost_cents) FROM document_lines r JOIN documents rd ON rd.id=r.document_id
                    WHERE r.original_line_id=l.id AND rd.type IN ('SALES_RETURN','PURCHASE_RETURN')
                    AND NOT EXISTS(SELECT 1 FROM documents rv WHERE rv.original_document_id=rd.id AND rv.type='REVERSAL')),0) AS returned_cost_cents
            FROM document_lines l WHERE l.document_id=? ORDER BY l.id""",arrayOf(id.toString())).use { c -> buildList { while(c.moveToNext()) add(c.toLine()) } }
        return header.copy(lines=lines)
    }
    private fun existingRequest(db:SQLiteDatabase,request:String):Long? {
        require(request.isNotBlank()) { "请求标识不能为空" }
        return db.rawQuery("SELECT id FROM documents WHERE request_id=?",arrayOf(request)).use { if(it.moveToFirst()) it.getLong(0) else null }
    }
    private fun isReversed(db:SQLiteDatabase,id:Long)=db.rawQuery("SELECT 1 FROM documents WHERE type='REVERSAL' AND original_document_id=? LIMIT 1",arrayOf(id.toString())).use { it.moveToFirst() }
    private fun outgoingCost(p:Product,quantity:Int):Long {
        if(quantity>p.stockQuantity) throw InsufficientStockException(p.name)
        return InventoryCost.allocate(p.stockCostCents,p.stockQuantity,quantity)
    }
    private fun currentProduct(db:SQLiteDatabase,id:Long)=db.query("products",null,"id=?",arrayOf(id.toString()),null,null,null).use { require(it.moveToFirst()) { "商品不存在" };it.toProduct() }
    private fun resolveNewProduct(db:SQLiteDatabase,p:Product,now:Long):Product {
        require(p.name.isNotBlank() && p.costPriceCents>=0 && p.salePriceCents>=0 && p.lowStockThreshold>=0) { "商品资料不完整" }
        if(p.barcode.isNotBlank()) db.query("products",null,"barcode=?",arrayOf(p.barcode.trim()),null,null,null).use { if(it.moveToFirst()) return it.toProduct() }
        val id=db.insertOrThrow("products",null,ContentValues().apply {
            put("name",p.name.trim());if(p.barcode.isBlank()) putNull("barcode") else put("barcode",p.barcode.trim())
            put("category",p.category.trim().ifBlank { "未分类" });put("cost_price_cents",p.costPriceCents);put("sale_price_cents",p.salePriceCents)
            put("low_stock_threshold",p.lowStockThreshold);put("note",p.note.trim());put("created_at",now);put("updated_at",now);put("uuid",UUID.randomUUID().toString())
        })
        return currentProduct(db,id)
    }
    private fun newDocument(db:SQLiteDatabase,type:DocumentType,total:Long,note:String,now:Long,requestId:String,originalId:Long?=null,originalType:DocumentType?=null):Long =
        db.insertOrThrow("documents",null,ContentValues().apply {
            put("type",type.name);put("total_cents",total);put("note",note.trim());put("created_at",now)
            put("uuid",UUID.randomUUID().toString());put("request_id",requestId)
            originalId?.let { put("original_document_id",it) };originalType?.let { put("original_type",it.name) }
        })

    private fun appendLine(db:SQLiteDatabase,documentId:Long,p:Product,q:Int,price:Long,cost:Long,delta:Int,costDelta:Long,now:Long,originalLineId:Long?=null,difference:Long=0) {
        require(q>0 && price>=0 && cost>=0)
        val next=Math.addExact(p.stockQuantity,delta)
        if(next<0) throw InsufficientStockException(p.name)
        val nextCost=Math.addExact(p.stockCostCents,costDelta)
        require(nextCost>=0 && (next!=0 || nextCost==0L)) { "库存成本异常，请先处理依赖单据" }
        val changed=db.update("products",ContentValues().apply {
            put("stock_quantity",next);put("stock_cost_cents",nextCost);put("updated_at",now);put("version",Math.addExact(p.version,1))
        },"id=? AND version=?",arrayOf(p.id.toString(),p.version.toString()))
        check(changed==1) { "库存已变化，请重试" }
        db.insertOrThrow("document_lines",null,ContentValues().apply {
            put("document_id",documentId);put("product_id",p.id);put("product_name_snapshot",p.name);put("barcode_snapshot",p.barcode)
            put("unit_price_cents",price);put("cost_price_snapshot_cents",cost/q);put("quantity",q);put("line_total_cents",Math.multiplyExact(price,q.toLong()))
            put("uuid",UUID.randomUUID().toString());put("line_cost_cents",cost);put("stock_cost_delta_cents",costDelta);put("quantity_delta",delta)
            originalLineId?.let { put("original_line_id",it) };put("valuation_difference_cents",difference)
        })
        db.insertOrThrow("stock_movements",null,ContentValues().apply {
            put("product_id",p.id);put("document_id",documentId)
            val type=db.rawQuery("SELECT type FROM documents WHERE id=?",arrayOf(documentId.toString())).use { it.moveToFirst();it.getString(0) }
            put("type",type);put("quantity_delta",delta);put("stock_after",next);put("created_at",now);put("uuid",UUID.randomUUID().toString())
            put("stock_cost_delta_cents",costDelta);put("stock_cost_after_cents",nextCost)
        })
    }
}

private fun Cursor.long(name:String)=getLong(getColumnIndexOrThrow(name))
private fun Cursor.int(name:String)=getInt(getColumnIndexOrThrow(name))
private fun Cursor.string(name:String)=getString(getColumnIndexOrThrow(name))
private fun Cursor.nullLong(name:String):Long?=getColumnIndexOrThrow(name).let { if(isNull(it)) null else getLong(it) }
private fun Cursor.toDocument()=StockDocument(
    id=long("id"),type=DocumentType.valueOf(string("type")),totalCents=long("total_cents"),note=string("note"),createdAt=long("created_at"),
    uuid=string("uuid"),requestId=string("request_id"),originalDocumentId=nullLong("original_document_id"),
    originalType=string("original_type")?.let(DocumentType::valueOf),valuationDifferenceCents=long("valuation_difference_cents"),costSource=string("cost_source"),
    expectedQuantity=nullLong("expected_quantity")?.toInt(),actualQuantity=nullLong("actual_quantity")?.toInt(),
)
private fun Cursor.toLine()=DocumentLine(
    id=long("id"),documentId=long("document_id"),productId=long("product_id"),productName=string("product_name_snapshot"),barcode=string("barcode_snapshot"),
    unitPriceCents=long("unit_price_cents"),costPriceSnapshotCents=long("cost_price_snapshot_cents"),quantity=int("quantity"),lineCostCents=long("line_cost_cents"),
    stockCostDeltaCents=long("stock_cost_delta_cents"),quantityDelta=int("quantity_delta"),originalLineId=nullLong("original_line_id"),
    valuationDifferenceCents=long("valuation_difference_cents"),costSource=string("cost_source"),uuid=string("uuid"),returnedQuantity=int("returned_quantity"),returnedCostCents=long("returned_cost_cents"),
)
