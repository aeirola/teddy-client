package fi.iki.aeirola.teddyclient;

/**
 * Created by Axel on 31.1.2015.
 */
public class WindowListActivityTest extends TeddyActivityTestCase<WindowListActivity> {

    public WindowListActivityTest() {
        super(WindowListActivity.class);
    }

    public void testViewList() throws Exception {
        solo.unlockScreen();

        boolean statusFound = solo.searchText("(status)");
        solo.takeScreenshot("window_list");
        assertTrue("Status window not found", statusFound);
    }
}
