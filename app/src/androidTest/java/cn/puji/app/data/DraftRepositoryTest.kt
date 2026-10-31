package cn.puji.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.puji.app.domain.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DraftRepositoryTest {
    private lateinit var context: Context
    private lateinit var drafts: DraftRepository
    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        drafts = DraftRepository(context); drafts.clear()
    }
    private fun cart() = Cart.empty(DocumentType.INBOUND).add(Product(id = 17, name = "牛奶", barcode = "1", uuid = UUID.randomUUID().toString(), costPriceCents = 123), 2)
    @Test fun retainsStableIdentityAndRequestAcrossRecreation() {
        val cart = cart(); val request = UUID.randomUUID().toString()
        drafts.save(cart, request)
        val restored = DraftRepository(context).load()!!
        assertEquals(request, restored.requestId)
        assertEquals(cart.lines[0].product.uuid, restored.cart.lines[0].product.uuid)
        assertEquals(246L, restored.cart.totalCents)
    }
    @Test fun interruptedReplacementNeverRestoresStaleNumericIds() {
        drafts.save(cart(), UUID.randomUUID().toString())
        drafts.beginReplacement()
        assertNull(DraftRepository(context).load())
    }
    @Test fun failedReplacementCanRestoreTheOriginalDraft() {
        drafts.save(cart(), UUID.randomUUID().toString())
        drafts.beginReplacement(); drafts.cancelReplacement()
        assertEquals("牛奶", DraftRepository(context).load()!!.cart.lines[0].product.name)
    }
    @Test fun submissionSnapshotIsOnDiskAfterAnAsyncCartEdit() {
        val oldRequest = UUID.randomUUID().toString()
        val request = UUID.randomUUID().toString()
        val original = cart()
        drafts.save(original, oldRequest)
        val edited = original.copy(note = "修改后的清单")
        drafts.save(edited, request)
        drafts.save(edited, request, durable = true)
        val disk = java.io.File(context.applicationInfo.dataDir, "shared_prefs/puji_draft.xml").readText()
        assertTrue(disk.contains(request))
        assertTrue(disk.contains(edited.lines.single().product.uuid))
        assertFalse(disk.contains(oldRequest))
    }
}
