package fi.iki.aeirola.teddyclient;

import android.content.Intent;

import fi.iki.aeirola.teddyclient.fragments.WindowDetailFragment;

public class WindowDetailActivityTest extends TeddyActivityTestCase<WindowDetailActivity> {

    public WindowDetailActivityTest() {
        super(WindowDetailActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        Intent detailIntent = new Intent(getInstrumentation().getTargetContext(), WindowDetailActivity.class);
        detailIntent.putExtra(WindowDetailFragment.ARG_WINDOW, 2L);
        setActivityIntent(detailIntent);

        super.setUp();
    }

    public void testViewLines() throws Exception {
        solo.unlockScreen();

        boolean linesFound = solo.searchText("Hello there");
        solo.takeScreenshot("window_detail");
        assertTrue("Scrollback lines not found", linesFound);
    }
}