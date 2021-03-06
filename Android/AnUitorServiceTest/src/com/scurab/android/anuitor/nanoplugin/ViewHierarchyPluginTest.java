package com.scurab.android.anuitor.nanoplugin;

import android.view.View;

import com.google.gson.Gson;
import com.scurab.android.anuitor.C;
import com.scurab.android.anuitor.model.ViewNode;
import com.scurab.android.anuitor.reflect.WindowManager;
import com.scurab.android.anuitor.tools.HttpTools;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Created by jbruchanov on 12.6.2014.
 */
@Config(manifest = C.MANIFEST, emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ViewHierarchyPluginTest {

    @Test
    public void testCorrectPath() {
        WindowManager wm = mock(WindowManager.class);
        doReturn(null).when(wm).getCurrentRootView();

        ViewHierarchyPlugin viewHierarchyPlugin = new ViewHierarchyPlugin(wm);
        assertArrayEquals(new String[]{"viewhierarchy.json"}, viewHierarchyPlugin.files());
        assertTrue(viewHierarchyPlugin.canServeUri("/viewhierarchy.json", null));
    }

    @Test
    public void testEmptyResultForNullView() throws IOException {
        WindowManager wm = mock(WindowManager.class);
        doReturn(null).when(wm).getCurrentRootView();

        ViewHierarchyPlugin viewHierarchyPlugin = new ViewHierarchyPlugin(wm);
        NanoHTTPD.Response response = viewHierarchyPlugin.handleRequest(null, null, mock(NanoHTTPD.IHTTPSession.class), null, null);
        assertEquals(HttpTools.MimeType.APP_JSON, response.getMimeType());
        String data = IOUtils.toString(response.getData());
        assertEquals("{}", data);
    }

    @Test
    public void testNonEmptyResultForNullView() throws IOException {
        WindowManager wm = mock(WindowManager.class);
        View inflate = View.inflate(Robolectric.application, android.R.layout.two_line_list_item, null);
        doReturn(inflate).when(wm).getCurrentRootView();

        ViewHierarchyPlugin viewHierarchyPlugin = new ViewHierarchyPlugin(wm);
        NanoHTTPD.Response response = viewHierarchyPlugin.handleRequest(null, null, mock(NanoHTTPD.IHTTPSession.class), null, null);
        assertEquals(HttpTools.MimeType.APP_JSON, response.getMimeType());
        String data = IOUtils.toString(response.getData());
        ViewNode vn = new Gson().fromJson(data, ViewNode.class);

        assertNotNull(vn);
        assertTrue(vn.getData().size() > 0);
        assertEquals(2, vn.getChildCount());
        assertEquals(0, vn.getLevel());
        assertEquals(0, vn.getPosition());

        assertEquals(android.R.id.text1, vn.getChildAt(0).getId());
        assertEquals(android.R.id.text2, vn.getChildAt(1).getId());
        //very simple check
        for (int i = 0, n = vn.getChildCount(); i < n; i++) {
            ViewNode vnc = vn.getChildAt(i);
            assertTrue(vnc.getData().size() > 0);
            assertEquals(0, vnc.getChildCount());
            assertEquals(1, vnc.getLevel());
            assertEquals(i + 1, vnc.getPosition());
        }
    }
}
