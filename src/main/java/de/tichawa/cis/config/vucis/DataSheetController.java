package de.tichawa.cis.config.vucis;

/**
 * The datasheet controller for VUCIS. Needed subclass as a few portions do not follow all other CIS types.
 */
public class DataSheetController extends de.tichawa.cis.config.DataSheetController {

    @Override
    protected String getProfileImageUrlString() {
        return super.getProfileImageUrlString();
        //TODO select different image depending on current selection
    }
}
