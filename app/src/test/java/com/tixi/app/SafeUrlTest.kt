package com.tixi.app

import com.tixi.app.data.remote.SafeUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeUrlTest {

    @Test
    fun `https 公网地址通过`() {
        assertTrue(SafeUrl.isAllowed("https://mineru.net/api/v4/extract-results/batch/abc"))
        assertTrue(SafeUrl.isAllowed("https://cdn.example.com/result/full.zip?sign=abc&expires=1"))
    }

    @Test
    fun `非 https 拒绝`() {
        assertFalse(SafeUrl.isAllowed("http://mineru.net/api/v4/x"))
        assertFalse(SafeUrl.isAllowed("ftp://example.com/a.zip"))
        assertFalse(SafeUrl.isAllowed("file:///data/local/tmp/a.zip"))
    }

    @Test
    fun `内网与环回地址拒绝`() {
        assertFalse(SafeUrl.isAllowed("https://localhost/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://127.0.0.1/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://10.0.0.5/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://172.20.1.1/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://192.168.1.10/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://169.254.169.254/latest/meta-data"))
        assertFalse(SafeUrl.isAllowed("https://[::1]/a.zip"))
    }

    @Test
    fun `可疑形式拒绝`() {
        assertFalse(SafeUrl.isAllowed("https://user:pass@example.com/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://example.com:8080/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://printer.local/a.zip"))
        assertFalse(SafeUrl.isAllowed("https://intranet/a.zip"))
        assertFalse(SafeUrl.isAllowed(""))
        assertFalse(SafeUrl.isAllowed("https://exa mple.com/a.zip"))
    }
}
